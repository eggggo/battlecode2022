package v9;

import battlecode.common.*;

public class Builder extends RobotPlayer {

    static int laboratoriesBuilt = 0;
    static int watchtowersBuilt = 0;
    static int turnsAlive = 0;
    static MapLocation home = null;
    static MapLocation[] sectorMdpts = new MapLocation[49];
    static boolean firstEnemySeen = false;
    static Direction awayFromEnemies = null;
    static int sageCount = 0;
    static int wtCount = 0;
    static int minerCount = 0;
    static int labCount = 0;

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

    public static void runBuilder(RobotController rc) throws GameActionException {
        MapLocation src = rc.getLocation();
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
        if (firstEnemySeen) {
            initLabCount = 1;
        }
        if (rc.getRoundNum() % 2 == 1) {
            sageCount = rc.readSharedArray(53);
            wtCount = rc.readSharedArray(52);
            minerCount = rc.readSharedArray(50);
            labCount = rc.readSharedArray(56);
        }
        if (turnsAlive == 0) {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
            for (int i = nearbyRobots.length - 1; i >= 0; i--) {
                if (nearbyRobots[i].getType() == RobotType.ARCHON) {
                    home = nearbyRobots[i].getLocation();
                    break;
                }
            }
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
        int currentIncome = rc.readSharedArray(49);
        int senseRadius = rc.getType().visionRadiusSquared;
        Team friendly = rc.getTeam();
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(senseRadius, friendly);
        RobotInfo[] enemies = rc.senseNearbyRobots(senseRadius, friendly.opponent());
        MapLocation closestAttacker = null;
        MapLocation nearbySoldier = null;
        MapLocation nearbyBuilding = null;
        int numNearbyWatchtowers = 0;
        int mapArea = rc.getMapHeight() * rc.getMapWidth();

        for (int i = enemies.length - 1; i >= 0; i --) {
            RobotInfo enemy = enemies[i];
            if ((enemy.getType() == RobotType.SOLDIER || enemy.getType() == RobotType.SAGE 
            || enemy.getType() == RobotType.WATCHTOWER) && (closestAttacker == null
            || enemy.location.distanceSquaredTo(src) < closestAttacker.distanceSquaredTo(src))) {
                closestAttacker = enemy.location;
            }
        }

        //Sensing Important information like nearby soldiers:
        int dist = Integer.MAX_VALUE;
        MapLocation nearestFriend = null;
        for (int i = nearbyRobots.length - 1; i >= 0; i--) {
            MapLocation tempLoc = nearbyRobots[i].getLocation();
            int tempDist = src.distanceSquaredTo(nearbyRobots[i].getLocation());
            if (tempDist < dist) {
                dist = tempDist;
                nearestFriend = tempLoc;
            }
        }

        int distanceFromBuilding = 9999;
        //If theres a nearby watchtower or laboratory that needs to be repaired, set the nearbyBulding to it.
        for (int i = nearbyRobots.length - 1; i >= 0; i--) {
            if (nearbyRobots[i].getType() == RobotType.WATCHTOWER && rc.getLocation().distanceSquaredTo(nearbyRobots[i].getLocation()) < distanceFromBuilding) {
                nearbyBuilding = nearbyRobots[i].getLocation();
                distanceFromBuilding = rc.getLocation().distanceSquaredTo(nearbyRobots[i].getLocation());
            } else if (nearbyRobots[i].getType() == RobotType.LABORATORY && rc.getLocation().distanceSquaredTo(nearbyRobots[i].getLocation()) < distanceFromBuilding
            && nearbyRobots[i].getHealth() < RobotType.LABORATORY.getMaxHealth(nearbyRobots[i].getLevel())) {
                nearbyBuilding = nearbyRobots[i].getLocation();
                distanceFromBuilding = rc.getLocation().distanceSquaredTo(nearbyRobots[i].getLocation());
            }
        }
        //System.out.println(distanceFromBuilding);
        //Tracking nearby Watchtower amount
        for (int i = nearbyRobots.length - 1; i >= 0; i--) {
            if (nearbyRobots[i].getType() == RobotType.WATCHTOWER) {
                numNearbyWatchtowers++;
            }
        }
        //System.out.println(nearbyBuilding);
        Direction dir;

        boolean labOverWt = false;
        if (mapArea >= 2500 && rc.getTeamGoldAmount(rc.getTeam()) ==0) {
            labOverWt = true;
        }

        Direction center = rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
        Direction builddir = center;
        // If the direction to the center of the map is Direction.Center(means you're in the center), set your default
        //direction to south, otherwise keep it center.
        if (center == Direction.CENTER) {
            builddir = Direction.SOUTH;
        }
        //Populate directions with all possible directions starting from direction to center rotating left
        Direction[] directions = new Direction[8];
        for (int i = 7; i>=0;i--) {
            if (i == 7) {
                directions[7-i] = center;
            } else {
                directions[7-i] = directions[7-i - 1].rotateLeft();
            }
        }

        double rubble = 200;
        for (Direction dire : directions) {
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
        boolean buildWt = rc.canBuildRobot(RobotType.WATCHTOWER, builddir) && (sageCount > 15)
                && (watchtowersBuilt < 3 || numNearbyWatchtowers == 9);

        //If there is no nearby repariable building, follow a nearby non-crowded soldier, otherwise move randomly
        if (nearbyBuilding != null && src.distanceSquaredTo(nearbyBuilding) > 5) {
            dir = Pathfinder.getMoveDir(rc, nearbyBuilding);
        } else if (closestAttacker != null) {
            Direction opposite = src.directionTo(closestAttacker).opposite();
            MapLocation runawayTgt = src.add(opposite).add(opposite);
            runawayTgt = new MapLocation(Math.min(Math.max(0, runawayTgt.x), rc.getMapWidth() - 1), 
            Math.min(Math.max(0, runawayTgt.y), rc.getMapHeight() - 1));
            dir = Pathfinder.getMoveDir(rc, runawayTgt);
        }
        else if (buildLab) {
            MapLocation awayTgt = src.add(awayFromEnemies).add(awayFromEnemies);
            MapLocation inBounds = new MapLocation(Math.min(Math.max(0, awayTgt.x), rc.getMapWidth() - 1), 
                Math.min(Math.max(0, awayTgt.y), rc.getMapHeight() - 1));
            dir = Pathfinder.getMoveDir(rc, inBounds);
        } else {
            dir = stallOnGoodRubble(rc);
        }

        //System.out.println(nearbyBuilding != null && rc.canMutate(nearbyBuilding) && rc.getTeamLeadAmount(rc.getTeam()) >= 200);
        //If there is a nearby building that can be repaired, repair it, otherwise go to the nearest repariable buidling and repair it.
        if (nearbyBuilding != null && rc.canMutate(nearbyBuilding)) {
//            rc.setIndicatorString("1");
            rc.mutate(nearbyBuilding);
            rc.writeSharedArray(55, (rc.readSharedArray(55) & 0b1111111));
            //System.out.println("hello");
        }
        else if (nearbyBuilding != null && rc.canRepair(nearbyBuilding)) {
//            rc.setIndicatorString("2");
            rc.repair(nearbyBuilding);
        } else if (buildLab) {
//            rc.setIndicatorString("3");
            if (rc.canBuildRobot(RobotType.LABORATORY, builddir)) {
                rc.buildRobot(RobotType.LABORATORY, builddir);
                laboratoriesBuilt++;
                rc.writeSharedArray(55, (rc.readSharedArray(55) & 0b1111111));
            }
        } else if (buildWt) {
//            rc.setIndicatorString("4");
            rc.buildRobot(RobotType.WATCHTOWER, builddir);
            watchtowersBuilt++;
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
