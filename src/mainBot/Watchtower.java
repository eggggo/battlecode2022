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
  static int turnsHaveBeenTwo = 0;
  static MapLocation bestTgtSector = null;

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
      return src.directionTo(minRubbleLoc);
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
    int rubbleThreshold = rc.senseRubble(rc.getLocation()) + 20;
    int soldierCount = rc.readSharedArray(51);

    if (rc.getLevel() == 2 && rc.getArchonCount() ==4) {
        turnsHaveBeenTwo++;
    }

    if (turnsAlive == 0) {
        for (int i = 48; i >= 0; i --) {
            sectorMdpts[i] = Comms.sectorMidpt(rc, i);
        }
    }

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

    int turnsNotKilledStuffMove = 30;

    Direction stallDir = stallOnGoodRubble(rc);

    if (attackTgt != null && (isHostile(attackTgt) || attackTgt.type == RobotType.ARCHON || enemies.length >= 4) 
        && rc.getMode() == RobotMode.PORTABLE && rc.canTransform()
        && stallDir == Direction.CENTER && rc.senseRubble(src) <= rubbleThreshold) {
        rc.transform();
    } else if (turnsNotKilledStuff > turnsNotKilledStuffMove && rc.getMode() == RobotMode.TURRET && rc.canTransform()
            && rc.getLevel() > 1) {
        rc.transform();
    }

    if (rc.isMovementReady()) {
        Direction dir = null;
        RobotInfo nearestBuilder = null;
        for (int i = friendlies.length - 1; i >= 0; i --) {
          RobotInfo friend = friendlies[i];
          if (friend.getType() == RobotType.BUILDER && (nearestBuilder == null 
          || friend.location.distanceSquaredTo(src) < nearestBuilder.location.distanceSquaredTo(src))) {
            nearestBuilder = friend;
          }
        }
        if (attackTgt != null) {
            dir = stallDir;
        } else {
          if (turnCount % 2 == 0) {
            bestTgtSector = null;
            double bestTgtSectorScore = 0;
              for (int i = 48; i >= 0; i--) {
                  int[] sector = Comms.readSectorInfo(rc, i);
                  MapLocation loc = sectorMdpts[i];
                  double sectorScore = (sector[3] + 50*sector[1])/Math.sqrt(src.distanceSquaredTo(loc));
                  if ((sector[1] > 0 || sector[3] > 0) && (bestTgtSector == null || sectorScore > bestTgtSectorScore)) {
                      bestTgtSector = loc;
                      bestTgtSectorScore = sectorScore;
                  }
              }
            }
            if (bestTgtSector != null && !(nearestBuilder != null && nearestBuilder.location.distanceSquaredTo(src) > 10)) {
                dir = Pathfinder.getMoveDir(rc, bestTgtSector);
            }
        }
        if (dir != null && rc.canMove(dir) && rc.getLevel() > 1) {
            if (rc.getArchonCount()==4) {
                if (turnsHaveBeenTwo > 0) { //tweaking this value to see if not moving for a certain amount of turns when having 4 archons to incentivize defense helps .
                    rc.move(dir);
                }
            } else {
                rc.move(dir);
            }

        }
    }

    turnsNotKilledStuff++;
    turnsAlive++;
    Comms.updateSector(rc, turnCount);

    if (rc.getLevel() > 1) {
        boolean currentHpThresh = (double)rc.getHealth()/rc.getType().getMaxHealth(1) > 0.2;
        if (!currentHpThresh && aboveHpThresh) {
            rc.writeSharedArray(52, rc.readSharedArray(52) - 1);
        } else if (currentHpThresh && !aboveHpThresh) {
            rc.writeSharedArray(52, rc.readSharedArray(52) + 1);
        }
        aboveHpThresh = currentHpThresh;
    }
  }
}
