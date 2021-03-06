package v8;

import battlecode.common.*;

public class Soldier extends RobotPlayer {

  static int turnsAlive = 0;
  static int turnsNotKilledStuff = 0;
  static int attackOffset = 0;
  static MapLocation home = null;
  static MapLocation[] enemyArchons = null;
  static MapLocation[] copyEnemyArchons = null;
  static int attackLocation = 0;
  static boolean aboveHpThresh = true;
  static boolean notRepaired = false;
  static MapLocation[] sectorMdpts = new MapLocation[49];
  static RobotInfo priorTarget = null;
  static int priorTargetResetTimer = 0;
  static boolean stall = false;
  static MapLocation tenTurnUpdate = null;
  static int tenTurnHealthUpdate = 50;
  static MapLocation opposite = null;
  static MapLocation bestTgtSector = null;
  static MapLocation closestEnemyArchon = null;
  static MapLocation closestHomeArchon = null;

  static int getQuadrant(RobotController rc, int x, int y) {
    int quad = 0;
    if (x >= rc.getMapWidth() / 2 && y >= rc.getMapHeight() / 2) {
      quad = 1;
    } else if (x >= rc.getMapWidth() / 2 && y < rc.getMapHeight() / 2) {
      quad = 4;
    } else if (x < rc.getMapWidth() / 2 && y >= rc.getMapHeight() / 2) {
      quad = 2;
    } else if (x < rc.getMapWidth() / 2 && y < rc.getMapHeight() / 2) {
      quad = 3;
    } else {
      System.out.println("Error: not in a quadrant");
    }
    return quad;
  }

  //specify hostile = true if we prefer hostile enemies, but any enemy is fine. hostile = false means any enemy.
  //can return null no enemies we can see
  static MapLocation closestEnemy (RobotController rc, boolean hostile) {
    MapLocation src = rc.getLocation();
    int radius = rc.getType().actionRadiusSquared;
    //int senseRadius = rc.getType().visionRadiusSquared;
    //Team friendly = rc.getTeam();
    Team opponent = rc.getTeam().opponent();
    RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);

    if (enemies.length > 0) {
      MapLocation closestEnemy = null;
      MapLocation closestAttackingEnemy = null;
      for (int i = enemies.length - 1; i >= 0; i --) {
        RobotInfo enemy = enemies[i];
        if (closestEnemy == null || enemy.location.distanceSquaredTo(src) < closestEnemy.distanceSquaredTo(src)) {
          closestEnemy = enemy.location;
        }
        if ((enemy.getType() == RobotType.SOLDIER || enemy.getType() == RobotType.SAGE
                || enemy.getType() == RobotType.WATCHTOWER) && (closestAttackingEnemy == null
                || enemy.location.distanceSquaredTo(src) < closestAttackingEnemy.distanceSquaredTo(src))) {
          closestAttackingEnemy = enemy.location;
        }
      }
      MapLocation toAttack = closestEnemy;
      if (hostile) {
        if(closestAttackingEnemy != null){
          return closestAttackingEnemy;
        }
        return closestEnemy;
      }
      else {
        return closestEnemy;
      }
    }
    return null;
  }

  //Stay within vision radius, but out of action radius
  //enemy can be called with null, signifying no enemy seen. this robot will try to path towards last seen enemy
  //assume enemy bot location, if specified is within vision radius
  //Only call when we cannot win a trade but want to retain map/space control
  static MapLocation lastSeenClosestEnemy = null;
  static void kiteVision(RobotController rc, MapLocation src, MapLocation enemy) throws GameActionException {
    if(enemy == null && lastSeenClosestEnemy == null){
      //do default action
    }
    else if (enemy == null){
      //move towards lastSeenEnemy
      Direction dir = Pathfinder.getMoveDir(rc, lastSeenClosestEnemy);
      if (dir != null && rc.canMove(dir)) {
        rc.move(dir);
      }

      //if we see enemies, update lastSeenEnemy to the closest, and attack it.
      MapLocation closestEnemy = closestEnemy(rc,true);
      if (closestEnemy != null) {
        lastSeenClosestEnemy = closestEnemy;
        if(rc.canAttack(closestEnemy)){
          rc.attack(closestEnemy);
        }
      }
    } else {

        int distance = enemy.distanceSquaredTo(src);

        if(distance > rc.getType().actionRadiusSquared && distance < rc.getType().visionRadiusSquared){
          //if distance is between vision and action, maintain distance (don't move)
          lastSeenClosestEnemy = enemy;
        }
        else if(distance < rc.getType().actionRadiusSquared){
          lastSeenClosestEnemy = enemy;
        if (rc.canAttack(enemy)) {
          rc.attack(enemy);
        }
        //move away from enemy, update lastSeenEnemy to the closest
        Direction dir = Pathfinder.getAwayDir(rc, lastSeenClosestEnemy);
        if (dir != null && rc.canMove(dir)) {
          rc.move(dir);
        }
      }
    }
  }

  //Same as previous method KiteVision, but looking to trade (will combine with previous method, adding a boolean parameter)
  //eventually put into DroidActions Class, since kiting (for vision) is applicable to miners as well
  //Only call when determined we can win a trade
  static MapLocation lastSeenClosestEnemyF = null;
  static void kiteFight(RobotController rc, MapLocation src, MapLocation enemy) throws GameActionException{
    if(enemy == null && lastSeenClosestEnemy == null){
      //do default action
    }
    else if (enemy == null){
      //move towards lastSeenEnemy
      Direction dir = Pathfinder.getMoveDir(rc, lastSeenClosestEnemy);
      if (dir != null && rc.canMove(dir)) {
        rc.move(dir);
      }

      //if we see enemies, update lastSeenEnemy to the closest, and attack it.
      // out of vision -> in action will only happen if enemy is 25 away and after moving we get to 13 away, but it is possible
      MapLocation closestEnemy = closestEnemy(rc,true);
      if (closestEnemy != null) {
        lastSeenClosestEnemy = closestEnemy;
        if(rc.canAttack(closestEnemy)){
          rc.attack(closestEnemy);
        }
      }

    } else {

      int distance = enemy.distanceSquaredTo(src);

      if(distance > rc.getType().actionRadiusSquared && distance < rc.getType().visionRadiusSquared){

        //if distance is between vision and action, move and attack enemy
        // can always get into action range if in vision range, provided we have enough cooldown
        lastSeenClosestEnemyF = enemy;

        Direction dir = Pathfinder.getMoveDir(rc, enemy);
        if (dir != null && rc.canMove(dir)) {
          rc.move(dir);
        }

        if(rc.canAttack(enemy)){
          rc.attack(enemy);
        }


      }
      else if(distance < rc.getType().actionRadiusSquared){
      //ok we are assuming we want to fight here so the below comments will be in decision making
        //here, we will either attack and move or move and attack depending on rubble

        //detect how many enemy bots are within action radius vs vision radius.
        // We want only one enemy bot in action radius at a time, so the action can be either
        // attack -> not moving or attack -> moving away


        lastSeenClosestEnemyF = enemy;

        Direction dir = Pathfinder.getMoveDir(rc, enemy);
        if( rc.senseRubble(src) < rc.senseRubble(src.add(dir)) ){
          //shoot then move
          if(rc.canAttack(enemy)){
            rc.attack(enemy);
          }
          if (dir != null && rc.canMove(dir)) {
            rc.move(dir);
          }

        }
        else{
          //move then shoot
          if (dir != null && rc.canMove(dir)) {
            rc.move(dir);
          }
          if(rc.canAttack(enemy)){
            rc.attack(enemy);
          }

        }
      }
    }
  }

  static Direction stallOnGoodRubble(RobotController rc) throws GameActionException {
    MapLocation src = rc.getLocation();
    int currRubble = rc.senseRubble(src);
      int minRubble = currRubble;
      MapLocation minRubbleLoc = src;
      if (currRubble > 0) {
        for (Direction d : Direction.allDirections()) {
          MapLocation test = src.add(d);
          if (rc.onTheMap(test) && !rc.isLocationOccupied(test) && rc.canSenseLocation(test)) {
            int rubbleHere = rc.senseRubble(test);
            if (rubbleHere <= minRubble) {
              minRubble = rubbleHere;
              minRubbleLoc = test;
            }
          }
        }
      }
      return src.directionTo(minRubbleLoc);
  }

  static boolean isHostile(RobotInfo enemy) {
    return (enemy.getType() == RobotType.SOLDIER || enemy.getType() == RobotType.SAGE 
    || enemy.getType() == RobotType.WATCHTOWER);
  }

  /**
   * Run a single turn for a Soldier.
   * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
   */
  static void runSoldier(RobotController rc) throws GameActionException {
    MapLocation src = rc.getLocation();
    int radius = rc.getType().actionRadiusSquared;
    int senseRadius = rc.getType().visionRadiusSquared;
    Team friendly = rc.getTeam();
    Team opponent = rc.getTeam().opponent();
    RobotInfo[] enemies = rc.senseNearbyRobots(senseRadius, opponent);
    RobotInfo[] friendlies = rc.senseNearbyRobots(senseRadius, friendly);
    RobotInfo attackTgt = null;
    RobotInfo inVisionTgt = null;
    int enemyHealth = 0;
    double enemyDamage = 0;
    int friendlyHealth = 0;
    double friendlyDamage = 0;
    int rubbleThreshold = rc.senseRubble(rc.getLocation()) + 20;
    int soldierCount = rc.readSharedArray(51);
    if (turnsAlive == 0) {
      initializeSoldier(rc, friendlies);
    }

    if (turnsAlive % 10 == 0) {
      stall = tenTurnUpdate != null && tenTurnUpdate.distanceSquaredTo(rc.getLocation()) < 16 && rc.getHealth() == tenTurnHealthUpdate;
      tenTurnUpdate = rc.getLocation();
      tenTurnHealthUpdate = rc.getHealth();
    }

    if (rc.getHealth() == RobotType.SOLDIER.getMaxHealth(rc.getLevel())) {
      notRepaired = false;
    }
    //Soldier rules order
    //0: If turn 0, setup home
    //1: If low health, run home
    //2: If enemy, attack
    //3: If an archon is under attack (defense or offense) go help
    //4: Defense pathing/Scout pathing/Attack pathing

    //focus fire nearest attacker with lowest hp, if no attacker just nearest unit with lowest hp
    int lowestHPTgt = 9999;
    int lowestInVisionHPTgt = 9999;
    if (enemies.length > 0) {
      for (int i = enemies.length - 1; i >= 0; i --) {
        RobotInfo enemy = enemies[i];
        if (enemy.getLocation().distanceSquaredTo(src) <= radius) {
          if (attackTgt == null) {
            lowestHPTgt = enemy.getHealth();
            attackTgt = enemy;
          } else if (isHostile(enemy) && !isHostile(attackTgt)) {
            lowestHPTgt = enemy.getHealth();
            attackTgt = enemy;
          } else if (enemy.getHealth() < lowestHPTgt 
          && ((isHostile(enemy) && isHostile(attackTgt)) || (!isHostile(enemy) && !isHostile(attackTgt)))) {
            lowestHPTgt = enemy.getHealth();
            attackTgt = enemy;
          }
        } else {
          if (inVisionTgt == null) {
            lowestInVisionHPTgt = enemy.getHealth();
            inVisionTgt = enemy;
          } else if (isHostile(enemy) && !isHostile(inVisionTgt)) {
            lowestInVisionHPTgt = enemy.getHealth();
            inVisionTgt = enemy;
          } else if (enemy.getHealth() < lowestInVisionHPTgt 
            && ((isHostile(enemy) && isHostile(inVisionTgt)) || (!isHostile(enemy) && !isHostile(inVisionTgt)))) {
            lowestInVisionHPTgt = enemy.getHealth();
            inVisionTgt = enemy;
          }
        }
        if (isHostile(enemy)) {
            enemyHealth += enemy.getHealth();
            enemyDamage += enemy.getType().damage/(1.0 + rc.senseRubble(enemy.location)/10.0);
        }
      }
      if (attackTgt != null && inVisionTgt == null) {
        inVisionTgt = attackTgt;
      }

      if (attackTgt != null && rc.canAttack(attackTgt.location)) {
        MapLocation toAttack = attackTgt.location;
        rc.attack(toAttack);
        turnsNotKilledStuff = 0;
      }
    }

    double repairThreshold = -.0444* soldierCount + .64444;
    double moveDirDx = 0;
    double moveDirDy = 0;
    if (friendlies.length > 0) {
      for (int i = friendlies.length - 1; i >= 0; i --) {
        RobotInfo robot = friendlies[i];
        if ((robot.getType() == RobotType.SOLDIER || robot.getType() == RobotType.SAGE || robot.getType() == RobotType.WATCHTOWER)) {
          if (robot.getLocation().distanceSquaredTo(src) < radius) {
            friendlyHealth += robot.getHealth();
            friendlyDamage += robot.getType().damage/(1.0 + rc.senseRubble(robot.location)/10.0);
          } else {
            moveDirDx += src.directionTo(robot.location).dx;
            moveDirDy += src.directionTo(robot.location).dy;
          }
        }
      }
    }

    if (enemies.length > 0 || priorTargetResetTimer > 5) {
      priorTarget = inVisionTgt;
      priorTargetResetTimer = 0;
    } else {
      priorTargetResetTimer++;
    }
    boolean chase = rc.getHealth() > 3*rc.getType().getMaxHealth(rc.getLevel())/4 && priorTarget != null && inVisionTgt == null;
    Direction dir = null;
    Direction a = null;
    if (chase) {
      a = Pathfinder.getMoveDir(rc, priorTarget.location);
      if (rc.senseRubble(rc.getLocation().add(a)) > rubbleThreshold && isHostile(priorTarget)) {
        chase = false;
      }
    }
    //1: if less than 50/3 hp go back and repair
    if ((notRepaired || (rc.getHealth() < RobotType.SOLDIER.getMaxHealth(rc.getLevel())/5)) && home != null
            && !stall && rc.getLocation().distanceSquaredTo(home) > 9 && (rc.getArchonCount() > 2 || rc.getTeamLeadAmount(rc.getTeam()) < 75 * rc.getArchonCount())) { // If low health run home
      dir = Pathfinder.getMoveDir(rc, home);
      notRepaired = true;

    } else if (chase) {
      dir = a;
    }
    //2: if we cannot win the fight we are going into kite and run
    else if ((inVisionTgt != null) && isHostile(inVisionTgt) && Math.ceil(friendlyHealth/(double)enemyDamage) < Math.ceil(enemyHealth/(double)friendlyDamage)) {
      Direction opposite = src.directionTo(inVisionTgt.location).opposite();
      MapLocation runawayTgt = src.add(opposite).add(opposite);
      runawayTgt = new MapLocation(Math.min(Math.max(0, runawayTgt.x), rc.getMapWidth() - 1), 
      Math.min(Math.max(0, runawayTgt.y), rc.getMapHeight() - 1));
      //maybe don't go if tgt has bad rubble? definitely need to revise this
      dir = Pathfinder.getMoveDir(rc, runawayTgt);
      MapLocation kitingTgt = src.add(dir);
      if (rc.senseRubble(kitingTgt) > rubbleThreshold) {
        dir = stallOnGoodRubble(rc);
      }
    //3: if we are fighting go to nearest best rubble spot to max damage
    //swap out with others if low hp
    } else if (attackTgt != null && isHostile(attackTgt)) {
      //shuffle low health behind high health
      if (rc.getHealth() < 20 && attackTgt != null) {
        Direction opposite = src.directionTo(attackTgt.location).opposite();
        MapLocation runawayTgt = src.add(opposite).add(opposite);
        runawayTgt = new MapLocation(Math.min(Math.max(0, runawayTgt.x), rc.getMapWidth() - 1), 
        Math.min(Math.max(0, runawayTgt.y), rc.getMapHeight() - 1));
        //maybe don't go if tgt has bad rubble? definitely need to revise this
        dir = Pathfinder.getMoveDir(rc, runawayTgt);
        MapLocation kitingTgt = src.add(dir);
        if (rc.senseRubble(kitingTgt) > rubbleThreshold) {
          dir = stallOnGoodRubble(rc);
        }
      } else {
        dir = stallOnGoodRubble(rc);
      }
    //If a target in vision, chase
    } else if (inVisionTgt != null){
      //far, follow
      if (src.distanceSquaredTo(inVisionTgt.location) > 5) {
        dir = Pathfinder.getMoveDir(rc, inVisionTgt.location);
        int chaseSpotRubble = rc.senseRubble(src.add(dir));
      if (chaseSpotRubble > rubbleThreshold && isHostile(inVisionTgt)) {
        dir = stallOnGoodRubble(rc);
      }
      //close enough to engage, go to low rubble to fight
      } else {
        dir = stallOnGoodRubble(rc);
      }
    //4: if there are enemies in a nearby sector go there
    //otherwise, go to nearest scouted enemy archon or guess if no scouted
    } else {
      if (turnCount % 2 == 0) {
        bestTgtSector = null;
        double bestTgtSectorScore = 0;
        closestEnemyArchon = null;
        int archonDistance = 9999;
        closestHomeArchon = null;
        for (int i = 48; i >= 0; i--) {
          int[] sector = Comms.readSectorInfo(rc, i);
          MapLocation loc = sectorMdpts[i];
          double sectorScore = sector[3]/Math.sqrt(src.distanceSquaredTo(loc));
          if (sector[3] > 0 && (bestTgtSector == null || sectorScore > bestTgtSectorScore)) {
            bestTgtSector = loc;
            bestTgtSectorScore = sectorScore;
          }
          if (sector[1] == 1 && (closestEnemyArchon == null || closestEnemyArchon.distanceSquaredTo(src) > src.distanceSquaredTo(loc))) {
            closestEnemyArchon = loc;
          }
          if (sector[0] == 1 && sectorMdpts[i].distanceSquaredTo(src) < archonDistance) {
            archonDistance = sectorMdpts[i].distanceSquaredTo(src);
            closestHomeArchon = sectorMdpts[i];
          }
        }
      }
      if (bestTgtSector != null) {
        dir = Pathfinder.getMoveDir(rc, bestTgtSector);
        MapLocation togo = src.add(dir);
        int rubble = rc.senseRubble(togo);
        //if an enemy present sector is within 40 r^2 and the spot pathfinder returns is not great rubble, wait on good rubble squares
        //to prevent getting pushed in bad position, need to revise this as well
        if (bestTgtSector.distanceSquaredTo(src) < 100 && rubble > rubbleThreshold && !stall) {
          dir = stallOnGoodRubble(rc);
        }
      } else if (closestEnemyArchon != null) {
        dir = Pathfinder.getMoveDir(rc, closestEnemyArchon);
        MapLocation togo = src.add(dir);
        int rubble = rc.senseRubble(togo);
        //if an enemy archon present sector is within 40 r^2 and the spot pathfinder returns is not great rubble, wait on good rubble squares
        //to prevent getting pushed in bad position, need to revise this as well
        if (closestEnemyArchon.distanceSquaredTo(src) < 100 && rubble > rubbleThreshold && !stall) {
          dir = stallOnGoodRubble(rc);
        }
      } else {
            double xVector = 0;
            double yVector = 0;
            for (int i = friendlies.length - 1; i >= 0; i --) {
                MapLocation friendlyLoc = friendlies[i].getLocation();
                double d = Math.sqrt(src.distanceSquaredTo(friendlyLoc));
                Direction opposite = src.directionTo(friendlyLoc).opposite();
                xVector += opposite.dx*(2.0/d);
                yVector += opposite.dy*(2.0/d);
            }
            if (closestHomeArchon != null) {
              Direction oppositeClosestHomeArchon = src.directionTo(closestHomeArchon).opposite();
              xVector += oppositeClosestHomeArchon.dx/1.5;
              yVector += oppositeClosestHomeArchon.dy/1.5;
            }
            MapLocation vectorTgt = src.translate((int)xVector, (int)yVector);
            MapLocation inBounds = new MapLocation(Math.min(Math.max(0, vectorTgt.x), rc.getMapWidth() - 1), 
            Math.min(Math.max(0, vectorTgt.y), rc.getMapHeight() - 1));
            dir = Pathfinder.getMoveDir(rc, inBounds);
      }
    }

    if (dir != null && rc.canMove(dir)) {
      rc.move(dir);
    }

    //post move attack if available
    if (rc.isActionReady()) {
      attackTgt = null;
      enemies = rc.senseNearbyRobots(senseRadius, opponent);
      lowestHPTgt = 9999;
      if (enemies.length > 0) {
        for (int i = enemies.length - 1; i >= 0; i --) {
          RobotInfo enemy = enemies[i];
          if (enemy.getLocation().distanceSquaredTo(src) <= radius) {
            if (attackTgt == null) {
              lowestHPTgt = enemy.getHealth();
              attackTgt = enemy;
            } else if (isHostile(enemy) && !isHostile(attackTgt)) {
              lowestHPTgt = enemy.getHealth();
              attackTgt = enemy;
            } else if (enemy.getHealth() < lowestHPTgt 
            && ((isHostile(enemy) && isHostile(attackTgt)) || (!isHostile(enemy) && !isHostile(attackTgt)))) {
              lowestHPTgt = enemy.getHealth();
              attackTgt = enemy;
            }
          }
        }
        if (attackTgt != null && rc.canAttack(attackTgt.location)) {
          MapLocation toAttack = attackTgt.location;
          rc.attack(toAttack);
          turnsNotKilledStuff = 0;
        }
      }
    }

    turnsNotKilledStuff++;
    turnsAlive++;
    Comms.updateSector(rc, turnCount);

    boolean currentHpThresh = (double)rc.getHealth()/rc.getType().getMaxHealth(1) > 0.2;
    if (!currentHpThresh && aboveHpThresh) {
        rc.writeSharedArray(51, rc.readSharedArray(51) - 1);
    } else if (currentHpThresh && !aboveHpThresh) {
        rc.writeSharedArray(51, rc.readSharedArray(51) + 1);
    }
    aboveHpThresh = currentHpThresh;
  }


  static void initializeSoldier(RobotController rc, RobotInfo[] nearbyRobots) throws GameActionException {
    for (int i = nearbyRobots.length - 1; i >= 0; i--) {
      if (nearbyRobots[i].getType() == RobotType.ARCHON) {
        home = nearbyRobots[i].getLocation();
        break;
      }
    }
  }
}
