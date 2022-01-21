package mainBot;

import battlecode.common.*;

public class Soldier extends RobotPlayer {

  static int turnsAlive = 0;
  static boolean notRepaired = false;
  static MapLocation[] sectorMdpts = new MapLocation[49];
  static MapLocation bestTgtSector = null;
  static MapLocation closestFriendlyArchon = null;
  static MapLocation scoutTgt = null;

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

  static boolean isHostile(RobotInfo unit) {
    return (unit.getType() == RobotType.SOLDIER || unit.getType() == RobotType.SAGE 
    || unit.getType() == RobotType.WATCHTOWER);
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
    int nearbyDamage = 0;
    int nearbyDamageHealth = 0;
    int rubbleThreshold = rc.senseRubble(rc.getLocation()) + 20;
    Direction dir = null;
    boolean frontline = true;

    if (turnsAlive == 0) {
      for (int i = 48; i >= 0; i --) {
          sectorMdpts[i] = Comms.sectorMidpt(rc, i);
      }
      int scoutPattern = rc.readSharedArray(50) % 2;
      if (scoutPattern == 0) {
        scoutTgt = sectorMdpts[rng.nextInt(49)];
      } else {
        int designatedLoc = rng.nextInt(5);
        switch (designatedLoc) {
            case 0:
                scoutTgt = new MapLocation(0, 0);
                break;
            case 1:
                scoutTgt = new MapLocation(rc.getMapWidth() - 1, 0);
                break;
            case 2:
                scoutTgt = new MapLocation(0, rc.getMapHeight() - 1);
                break;
            case 3:
                scoutTgt = new MapLocation(rc.getMapWidth() - 1, rc.getMapHeight() - 1);
                break;
            case 4:
                scoutTgt = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
                break;
            }
        }
    }

    //if we reach full health then we are repaired
    if (rc.getHealth() == RobotType.SOLDIER.getMaxHealth(rc.getLevel())) {
      notRepaired = false;
    }

    //pre move attack and enemy assessment
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
      }
      if (attackTgt != null && inVisionTgt == null) {
        inVisionTgt = attackTgt;
      }

      if (attackTgt != null && rc.canAttack(attackTgt.location)) {
        MapLocation toAttack = attackTgt.location;
        rc.attack(toAttack);
      }
    }

    //assess teammates
    for (int i = friendlies.length - 1; i >= 0; i --) {
        RobotInfo friend = friendlies[i];
        if (isHostile(friend)) {
            nearbyDamage ++;
            nearbyDamageHealth += friend.getHealth();
            if (frontline && attackTgt != null 
            && friend.location.distanceSquaredTo(attackTgt.location) < src.distanceSquaredTo(attackTgt.location)) {
                frontline = false;
            }
        }
    }
    double averageHealth = (double)nearbyDamageHealth/nearbyDamage;

    //comms access every 3rd turn for bytecode reduction
    if (turnCount % 3 == 0 || turnsAlive == 0) {
        bestTgtSector = null;
        double highScore = 0;
        for (int i = 48; i >= 0; i --) {
            int[] sector = Comms.readSectorInfo(rc, i);
            if (sector[0] == 1 && (closestFriendlyArchon == null 
            || sectorMdpts[i].distanceSquaredTo(src) < closestFriendlyArchon.distanceSquaredTo(src))) {
                closestFriendlyArchon = sectorMdpts[i];
            }
            if (sector[1] == 1 || sector[3] > 0) {
                double currentScore = (50.0*sector[1] + sector[3])/Math.sqrt(src.distanceSquaredTo(sectorMdpts[i]));
                if (currentScore > highScore) {
                    bestTgtSector = sectorMdpts[i];
                }
            }
        }
    }

    //if you can see the scout target sector mdpt, randomize and go to somewhere else
    if (src.distanceSquaredTo(scoutTgt) <= senseRadius) {
      scoutTgt = sectorMdpts[rng.nextInt(49)];
    }

    int repairThresh = (int)(15 + (-1.0*(Math.abs(src.x - closestFriendlyArchon.x) + Math.abs(src.y - closestFriendlyArchon.y)))/12);
    //main movement loop
    //if low enough hp run back to heal
    if ((notRepaired || rc.getHealth() <= repairThresh) && src.distanceSquaredTo(closestFriendlyArchon) >= 3) {
      dir = Pathfinder.getMoveDir(rc, closestFriendlyArchon);
      notRepaired = true;
    //if mid hp comparatively or no cd, shuffle
    } else if (attackTgt != null && ((frontline && rc.getHealth() < averageHealth && rc.getHealth() < 15 + repairThresh) 
              || !rc.isActionReady())) {
      Direction opposite = src.directionTo(inVisionTgt.location).opposite();
      MapLocation runawayTgt = src.add(opposite).add(opposite);
      runawayTgt = new MapLocation(Math.min(Math.max(0, runawayTgt.x), rc.getMapWidth() - 1), 
      Math.min(Math.max(0, runawayTgt.y), rc.getMapHeight() - 1));
      dir = Pathfinder.getMoveDir(rc, runawayTgt);
      MapLocation kitingTgt = src.add(dir);
      if (rc.senseRubble(kitingTgt) > rubbleThreshold) {
          dir = stallOnGoodRubble(rc);
      }
    //if enough soldiers nearby advance to make space
    } else if (nearbyDamage > 4 && attackTgt != null) {
      dir = Pathfinder.getMoveDir(rc, attackTgt.location);
      MapLocation kitingTgt = src.add(dir);
      if (rc.senseRubble(kitingTgt) > rubbleThreshold) {
        dir = stallOnGoodRubble(rc);
      }
    //otherwise if there is an enemy sector go to best scored one
    } else if (bestTgtSector != null) {
      dir = Pathfinder.getMoveDir(rc, bestTgtSector);
    //otherwise scout same as miner
    } else {
      dir = Pathfinder.getMoveDir(rc, scoutTgt);
    }

    //move
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
        }
      }
    }

    turnsAlive++;

    //update comms
    Comms.updateSector(rc);
    Comms.updateTypeCount(rc);
  }
}
