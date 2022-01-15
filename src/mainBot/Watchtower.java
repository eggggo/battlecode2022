package mainBot;

import battlecode.common.*;

public class Watchtower extends RobotPlayer {

  static int turnsAlive = 0;
  static int turnsNotKilledStuff = 0;
  static int attackOffset = 0;
  static MapLocation home = null;
  static MapLocation[] enemyArchons = null;
  static MapLocation[] copyEnemyArchons = null;
  static int attackLocation = 0;
  static boolean aboveHpThresh = true;
  static MapLocation[] sectorMdpts = new MapLocation[49];

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
            if (rubbleHere < minRubble) {
              minRubble = rubbleHere;
              minRubbleLoc = test;
            }
          }
        }
      }
      return Pathfinder.getMoveDir(rc, minRubbleLoc);
  }

  static boolean isHostile(RobotInfo enemy) {
    return (enemy.getType() == RobotType.SOLDIER || enemy.getType() == RobotType.SAGE 
    || enemy.getType() == RobotType.WATCHTOWER);
  }

  /**
   * Run a single turn for a watchtower.
   * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
   */
  static void runWatchtower(RobotController rc) throws GameActionException {
    MapLocation src = rc.getLocation();
    int radius = rc.getType().actionRadiusSquared;
    int senseRadius = rc.getType().visionRadiusSquared;
    Team friendly = rc.getTeam();
    Team opponent = rc.getTeam().opponent();
    RobotInfo[] enemies = rc.senseNearbyRobots(senseRadius, opponent);
    RobotInfo[] friendlies = rc.senseNearbyRobots(senseRadius, friendly);
    RobotInfo attackTgt = null;
    RobotInfo inVisionTgt = null;

    //focus fire nearest attacker with lowest hp, if no attacker just nearest unit with lowest hp
    int lowestHPTgt = 9999;
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
          } else if (enemy.getHealth() < lowestHPTgt) {
            lowestHPTgt = enemy.getHealth();
            attackTgt = enemy;
          }
        } else {
          if (inVisionTgt == null) {
            lowestHPTgt = enemy.getHealth();
            inVisionTgt = enemy;
          } else if (isHostile(enemy) && !isHostile(inVisionTgt)) {
            lowestHPTgt = enemy.getHealth();
            inVisionTgt = enemy;
          } else if (enemy.getHealth() < lowestHPTgt) {
            lowestHPTgt = enemy.getHealth();
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
        turnsNotKilledStuff = 0;
      }
    }

    if (enemies.length > 0 && rc.getMode() == RobotMode.PORTABLE && rc.canTransform()) {
        rc.transform();
    } else if (turnsNotKilledStuff > 30 && rc.getMode() == RobotMode.TURRET && rc.canTransform()) {
        rc.transform();
    }

    MapLocation closestEnemies = null;
    for (int i = 48; i >= 0; i--) {
      int[] sector = Comms.readSectorInfo(rc, i);
      MapLocation loc = sectorMdpts[i];
      if ((sector[3] > 0 || sector[1] > 0) && (closestEnemies == null || closestEnemies.distanceSquaredTo(src) > src.distanceSquaredTo(loc))) {
        closestEnemies = loc;
      }
    }
    
    Direction dir = null;
    if (closestEnemies != null) {
        dir = Pathfinder.getMoveDir(rc, closestEnemies);
    }
    System.out.println(dir);
    
    if (dir != null && rc.canMove(dir)) {
      rc.move(dir);
    }

    turnsNotKilledStuff++;
    turnsAlive++;
    Comms.updateSector(rc, turnCount);

    boolean currentHpThresh = (double)rc.getHealth()/rc.getType().getMaxHealth(1) > 0.7;
    if (!currentHpThresh && aboveHpThresh) {
        rc.writeSharedArray(52, rc.readSharedArray(52) - 1);
    } else if (currentHpThresh && !aboveHpThresh) {
        rc.writeSharedArray(52, rc.readSharedArray(52) + 1);
    }
    aboveHpThresh = currentHpThresh;
  }
}
