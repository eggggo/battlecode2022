package mainBot;

import battlecode.common.*;

public class Sage extends RobotPlayer{
    static int turnsAlive = 0;
    static boolean aboveHpThresh = true;
    static int attackOffset = 0;
    static MapLocation home = null;
    static MapLocation[] enemyArchons = null;
    static int attackLocation = 0;
    //Role: 2 for defense, 1 for attack, 0 for scout
    static int role;
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

    public static void runSage(RobotController rc) throws GameActionException {
        MapLocation src = rc.getLocation();
        int senseRadius = rc.getType().visionRadiusSquared;
        int radius = rc.getType().actionRadiusSquared;
        Team friendly = rc.getTeam();
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(senseRadius, opponent);
        RobotInfo attackTgt = null;
        RobotInfo inVisionTgt = null;
        int unitHP = 0;
        int buildingHP = 0;
        int rubbleThreshold = rc.senseRubble(rc.getLocation()) + 20;

        if (turnsAlive == 0) {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(senseRadius, friendly);
            for (int i = 48; i >= 0; i --) {
                sectorMdpts[i] = Comms.sectorMidpt(rc, i);
            }
        }

        int lowestHPTgt = 9999;
        int lowestInVisionHPTgt = 9999;
        if (enemies.length > 0) {
            for (int i = enemies.length - 1; i >= 0; i --) {
                RobotInfo enemy = enemies[i];
                if (enemy.getLocation().distanceSquaredTo(src) <= radius) {
                    if (attackTgt == null ||
                        isHostile(enemy) && !isHostile(attackTgt) ||
                        isHostile(enemy) == isHostile(attackTgt) && enemy.getHealth() < lowestHPTgt) {
                        //note: && has higher precedence than ||

                        lowestHPTgt = enemy.getHealth();
                        attackTgt = enemy;
                    }

                    if (enemy.getType() == RobotType.ARCHON || enemy.getType() == RobotType.WATCHTOWER || enemy.getType() == RobotType.LABORATORY) {
                        buildingHP += enemy.getType().getMaxHealth(enemy.getLevel());
                    } else {
                        unitHP += enemy.getType().getMaxHealth(1);
                    }
                } else {
                    if (inVisionTgt == null ||
                        isHostile(enemy) && !isHostile(inVisionTgt) ||
                        isHostile(enemy) == isHostile(inVisionTgt) && enemy.getHealth() < lowestInVisionHPTgt) {

                        lowestInVisionHPTgt = enemy.getHealth();
                        inVisionTgt = enemy;
                    }
                }
            }
            if (attackTgt != null && inVisionTgt == null) {
                inVisionTgt = attackTgt;
            }

            //maximize damage done
            if ( AnomalyType.CHARGE.sagePercentage * unitHP >= RobotType.SAGE.getDamage(1)
                    && rc.canEnvision(AnomalyType.CHARGE)) {
                rc.envision(AnomalyType.CHARGE);
            } else if ((AnomalyType.FURY.sagePercentage * buildingHP >= 60)
                    && rc.canEnvision(AnomalyType.FURY)) {
                rc.envision(AnomalyType.FURY);
            } else if (attackTgt != null && rc.canAttack(attackTgt.location)) {
                MapLocation toAttack = attackTgt.location;
                rc.attack(toAttack);
            }
        }

        Direction dir = null;
        if (rc.getHealth() < RobotType.SAGE.getMaxHealth(rc.getLevel()) / 10 && home != null) { // If low health run home
            dir = Pathfinder.getMoveDir(rc, home);
        //if cant attack and see enemies run
        } else if (inVisionTgt != null && isHostile(inVisionTgt) && !rc.isActionReady()) {
            Direction opposite = src.directionTo(inVisionTgt.location).opposite();
            MapLocation runawayTgt = src.add(opposite).add(opposite);
            runawayTgt = new MapLocation(Math.min(Math.max(0, runawayTgt.x), rc.getMapWidth() - 1),
                    Math.min(Math.max(0, runawayTgt.y), rc.getMapHeight() - 1));
            dir = Pathfinder.getMoveDir(rc, runawayTgt);
        } else if (inVisionTgt != null){
            //far, follow if we have cd up
            if (src.distanceSquaredTo(inVisionTgt.location) > 14) {
                dir = Pathfinder.getMoveDir(rc, inVisionTgt.location);
                int chaseSpotRubble = rc.senseRubble(src.add(dir));
                if (chaseSpotRubble > rubbleThreshold && isHostile(inVisionTgt)) {
                    dir = stallOnGoodRubble(rc);
                }
            } else {
              dir = stallOnGoodRubble(rc);
            }
        } else {
            MapLocation closestEnemies = null;
            MapLocation closestEnemyArchon = null;
            int archonDistance = 9999;
            MapLocation closestHomeArchon = null;
            for (int i = 48; i >= 0; i--) {
              int[] sector = Comms.readSectorInfo(rc, i);
              MapLocation loc = sectorMdpts[i];
              if (sector[3] > 0 && (closestEnemies == null || closestEnemies.distanceSquaredTo(src) > src.distanceSquaredTo(loc))) {
                closestEnemies = loc;
              }
              if (sector[1] == 1 && (closestEnemyArchon == null || closestEnemyArchon.distanceSquaredTo(src) > src.distanceSquaredTo(loc))) {
                closestEnemyArchon = loc;
              }
              if (sector[0] == 1 && sectorMdpts[i].distanceSquaredTo(src) < archonDistance) {
                archonDistance = sectorMdpts[i].distanceSquaredTo(src);
                closestHomeArchon = sectorMdpts[i];
                home = closestEnemyArchon;
              }
            }
            if (closestEnemies != null) {
              dir = Pathfinder.getMoveDir(rc, closestEnemies);
              MapLocation togo = src.add(dir);
              int rubble = rc.senseRubble(togo);
              if (closestEnemies.distanceSquaredTo(src) < 100 && rubble > rubbleThreshold) {
                dir = stallOnGoodRubble(rc);
              }
            } else if (closestEnemyArchon != null) {
              dir = Pathfinder.getMoveDir(rc, closestEnemyArchon);
              MapLocation togo = src.add(dir);
              int rubble = rc.senseRubble(togo);
              if (closestEnemyArchon.distanceSquaredTo(src) < 100 && rubble > rubbleThreshold) {
                dir = stallOnGoodRubble(rc);
              }
            }
          }

        if (dir != null && rc.canMove(dir)) {
            rc.move(dir);
        }

        if (turnsAlive == 0) {
            rc.writeSharedArray(53, rc.readSharedArray(53) + 1);
        }
        boolean currentHpThresh = (double)rc.getHealth()/rc.getType().getMaxHealth(1) > 0.2;
        if (!currentHpThresh && aboveHpThresh) {
            rc.writeSharedArray(53, rc.readSharedArray(53) - 1);
        } else if (currentHpThresh && !aboveHpThresh) {
            rc.writeSharedArray(53, rc.readSharedArray(53) + 1);
        }
        Comms.updateSector(rc, turnCount);
        aboveHpThresh = currentHpThresh;
        turnsAlive ++;
    }
}
