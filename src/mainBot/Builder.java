package mainBot;

import battlecode.common.*;

public class Builder extends RobotPlayer {

    static int laboratoriesBuilt = 0;
    static int watchtowersBuilt = 0;
    static int turnsAlive = 0;
    static MapLocation[] sectorMdpts = new MapLocation[49];
    static boolean firstEnemySeen = false;
    static Direction awayFromEnemies = null;
    static int sageCount = 0;
    static int wtCount = 0;
    static int minerCount = 0;
    static int labCount = 0;
    static int currentIncome = 0;
    static MapLocation bestTgtSector = null;
    static RobotInfo lastBuilt = null;

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

    public static void runBuilder(RobotController rc) throws GameActionException {
        MapLocation src = rc.getLocation();
        int senseRadius = rc.getType().visionRadiusSquared;
        Team friendly = rc.getTeam();
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(senseRadius, friendly);
        RobotInfo[] enemies = rc.senseNearbyRobots(senseRadius, friendly.opponent());
        MapLocation closestAttacker = null;
        MapLocation nearestWt = null;
        MapLocation nearestLab = null;
        int awayFromLabX = 0;
        int awayFromLabY = 0;

        if (!firstEnemySeen) {
            for (int i = 48; i >= 0; i--) {
                int[] sector = Comms.readSectorInfo(rc, i);
                if (sector[3] > 0) {
                    firstEnemySeen = true;
                    break;
                }
            }
        }

        int initLabCount = 1;

        //on odd read turns get relevant info for building
        if (rc.getRoundNum() % 2 == 1) {
            sageCount = rc.readSharedArray(53);
            wtCount = rc.readSharedArray(52);
            minerCount = rc.readSharedArray(50);
            labCount = rc.readSharedArray(56);
            currentIncome = rc.readSharedArray(49);
        }

        //initial setup
        if (turnsAlive == 0) {
            int xVector = 0;
            int yVector = 0;
            for (int i = 48; i >= 0; i --) {
                sectorMdpts[i] = Comms.sectorMidpt(rc, i);
                int[] sector = Comms.readSectorInfo(rc, i);
                if (sector[1] == 1) {
                    xVector += src.directionTo(sectorMdpts[i]).opposite().dx;
                    yVector += src.directionTo(sectorMdpts[i]).opposite().dy;
                }
            }
            awayFromEnemies = src.directionTo(src.translate(xVector, yVector));         
        }

        //finding closest enemy dps
        for (int i = enemies.length - 1; i >= 0; i --) {
            RobotInfo enemy = enemies[i];
            if ((isHostile(enemy)) && (closestAttacker == null
            || enemy.location.distanceSquaredTo(src) < closestAttacker.distanceSquaredTo(src))) {
                closestAttacker = enemy.location;
            }
        }

        //comms access every other turn for bytecode reduction
        if (turnCount % 3 == 0 || turnsAlive == 0) {
            bestTgtSector = null;
            double highScore = 0;
            for (int i = 48; i >= 0; i --) {
                int[] sector = Comms.readSectorInfo(rc, i);
                if (sector[1] == 1 || sector[3] > 0) {
                    double currentScore = (50.0*sector[1] + sector[3])/Math.sqrt(src.distanceSquaredTo(sectorMdpts[i]));
                    if (currentScore > highScore) {
                        bestTgtSector = sectorMdpts[i];
                    }
                }
            }
        }

        int distanceFromBuilding = 9999;
        //If theres a nearby watchtower, or laboratory that needs to be repaired, set the nearbyBulding to it.
        for (int i = nearbyRobots.length - 1; i >= 0; i--) {
            RobotInfo unit = nearbyRobots[i];
            if (unit.getType() == RobotType.WATCHTOWER && rc.getLocation().distanceSquaredTo(unit.getLocation()) < distanceFromBuilding) {
                nearestWt = unit.getLocation();
                distanceFromBuilding = rc.getLocation().distanceSquaredTo(unit.getLocation());
            } else if (unit.getType() == RobotType.LABORATORY) { 
                if (rc.getLocation().distanceSquaredTo(unit.getLocation()) < distanceFromBuilding) {
                    nearestLab = unit.getLocation();
                    distanceFromBuilding = rc.getLocation().distanceSquaredTo(unit.getLocation());
                }
                awayFromLabX += src.directionTo(unit.location).opposite().dx;
                awayFromLabY += src.directionTo(unit.location).opposite().dy;
            }
        }

        Direction dir = null;

        Direction center = rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
        Direction builddir = center;

        double rubble = 200;
        for (Direction dire : Direction.allDirections()) {
            MapLocation loc = src.add(dire);
            if (rc.onTheMap(loc) && rc.senseRobotAtLocation(loc) == null) {
                if (rc.senseRubble(loc) < rubble) {
                    rubble = rc.senseRubble(loc);
                    builddir = dire;
                }
            }
        }

        boolean buildLab = (minerCount / 10 + initLabCount > labCount)
                || (rc.getTeamLeadAmount(rc.getTeam()) >= 180 && !(sageCount > 3 * rc.getArchonCount()));
        boolean buildWt = (sageCount > 15)&& (watchtowersBuilt < 3);

        if (lastBuilt != null && lastBuilt.getHealth() == lastBuilt.type.getMaxHealth(lastBuilt.level)) {
            lastBuilt = null;
        }

        //If there is no nearby repariable building, follow a nearby non-crowded soldier, otherwise move randomly
        if (lastBuilt != null) {
            dir = Pathfinder.getMoveDir(rc, lastBuilt.location);
            lastBuilt = rc.senseRobotAtLocation(lastBuilt.location);
        }
        else if (nearestWt != null && src.distanceSquaredTo(nearestWt) > 5) {
            dir = Pathfinder.getMoveDir(rc, nearestWt);
        } else if (closestAttacker != null) {
            Direction opposite = src.directionTo(closestAttacker).opposite();
            MapLocation runawayTgt = src.add(opposite).add(opposite);
            runawayTgt = new MapLocation(Math.min(Math.max(0, runawayTgt.x), rc.getMapWidth() - 1), 
            Math.min(Math.max(0, runawayTgt.y), rc.getMapHeight() - 1));
            dir = Pathfinder.getMoveDir(rc, runawayTgt);
        }
        else if (buildLab) {
            MapLocation tgt = src.translate(awayFromLabX + 2*awayFromEnemies.dx, awayFromLabY + 2*awayFromEnemies.dy);
            MapLocation inBounds = new MapLocation(Math.min(Math.max(0, tgt.x), rc.getMapWidth() - 1), 
                Math.min(Math.max(0, tgt.y), rc.getMapHeight() - 1));
            dir = Pathfinder.getMoveDir(rc, inBounds);
        } else if (buildWt) {
            if (src.distanceSquaredTo(bestTgtSector) < 150) {
                dir = stallOnGoodRubble(rc);
            } else {
                dir = Pathfinder.getMoveDir(rc, bestTgtSector);
            }
        } else {
            MapLocation tgt = src.translate(awayFromLabX, awayFromLabY);
            MapLocation inBounds = new MapLocation(Math.min(Math.max(0, tgt.x), rc.getMapWidth() - 1), 
                Math.min(Math.max(0, tgt.y), rc.getMapHeight() - 1));
            dir = Pathfinder.getMoveDir(rc, inBounds);
        }

        MapLocation actionTgt = null;
        if (nearestWt != null) {
            actionTgt = nearestWt;
        } else if (nearestLab != null) {
            actionTgt = nearestLab;
        }

        boolean needHealing = false;
        if (actionTgt != null) {
            RobotInfo actionTgtRobot = rc.senseRobotAtLocation(actionTgt);
            needHealing = actionTgtRobot.getHealth() < actionTgtRobot.getType().getMaxHealth(actionTgtRobot.level);
        }

        //System.out.println(nearbyBuilding != null && rc.canMutate(nearbyBuilding) && rc.getTeamLeadAmount(rc.getTeam()) >= 200);
        //If there is a nearby building that can be repaired, repair it, otherwise go to the nearest repariable buidling and repair it.
        if ((actionTgt != null) && needHealing && (rc.canRepair(actionTgt))) {
//            rc.setIndicatorString("2");
            rc.repair(actionTgt);
        } else if (buildLab) {
//            rc.setIndicatorString("3");
            if (rc.canBuildRobot(RobotType.LABORATORY, builddir)) {
                rc.buildRobot(RobotType.LABORATORY, builddir);
                lastBuilt = rc.senseRobotAtLocation(src.add(builddir));
                laboratoriesBuilt++;
                rc.writeSharedArray(55, (rc.readSharedArray(55) & 0b1111111));
            }
        } else if ((actionTgt != null) && (rc.canMutate(actionTgt))) {
            rc.mutate(actionTgt);
            rc.writeSharedArray(55, (rc.readSharedArray(55) & 0b1111111));
            //System.out.println("hello");
        }else if (buildWt) {
//            rc.setIndicatorString("4");
            if (rc.canBuildRobot(RobotType.WATCHTOWER, builddir)) {
                rc.buildRobot(RobotType.WATCHTOWER, builddir);
                lastBuilt = rc.senseRobotAtLocation(src.add(builddir));
                watchtowersBuilt++;
            }
        }

        if (dir != null && rc.canMove(dir)) {
            rc.move(dir);
        }

        //Comms stuff
        Comms.updateSector(rc);
        Comms.updateTypeCount(rc);
        turnsAlive ++;
    }
}
