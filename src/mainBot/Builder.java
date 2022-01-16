package mainBot;

import java.util.Arrays;

import battlecode.common.*;

public class Builder extends RobotPlayer {

    static int laboratoriesBuilt = 0;
    static int watchtowersBuilt = 0;
    static int turnsAlive = 0;
    static boolean aboveHpThresh = true;
    static MapLocation home = null;
    static MapLocation[] sectorMdpts = new MapLocation[49];

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
        if (turnsAlive == 0) {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
            for (int i = nearbyRobots.length - 1; i >= 0; i--) {
                if (nearbyRobots[i].getType() == RobotType.ARCHON) {
                    home = nearbyRobots[i].getLocation();
                    break;
                }
            }
            for (int i = 48; i >= 0; i --) {
                sectorMdpts[i] = Comms.sectorMidpt(rc, i);
            }
        }
        MapLocation src = rc.getLocation();
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
            } else if (nearbyRobots[i].getType() == RobotType.LABORATORY && rc.getLocation().distanceSquaredTo(nearbyRobots[i].getLocation()) < distanceFromBuilding) {
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
        else if (labOverWt && nearestFriend != null && laboratoriesBuilt == 0) {
            dir = Pathfinder.getAwayDir(rc, nearestFriend);
        }
        else if (rc.getTeamLeadAmount(rc.getTeam())>100 && watchtowersBuilt == 0) {
            Direction center = rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
            dir = center;
            // If the direction to the center of the map is Direction.Center(means you're in the center), set your default
            //direction to south, otherwise keep it center.
            if (center == Direction.CENTER) {
                dir = Direction.SOUTH;
            }
            int leastRubble = Integer.MAX_VALUE;
                for (int i = directions.length - 1; i >= 0; i--) {
                    MapLocation loc = src.add(directions[i]);
                    if (rc.onTheMap(loc) && rc.senseRobotAtLocation(loc) == null && rc.senseRubble(loc) < leastRubble) {
                        dir = directions[i];
                        leastRubble = rc.senseRubble(loc);
                    }
                }
        } else {
            dir = stallOnGoodRubble(rc);
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

        //System.out.println(nearbyBuilding != null && rc.canMutate(nearbyBuilding) && rc.getTeamLeadAmount(rc.getTeam()) >= 200);
        //If there is a nearby building that can be repaired, repair it, otherwise go to the nearest repariable buidling and repair it.
        if (nearbyBuilding != null && rc.canMutate(nearbyBuilding) && rc.getTeamLeadAmount(rc.getTeam()) >= 200) {
            rc.mutate(nearbyBuilding);
            rc.writeSharedArray(52, rc.readSharedArray(52) + 1);
            rc.writeSharedArray(55, (rc.readSharedArray(55) & 0b1111111));
            //System.out.println("hello");
        }
        else if (nearbyBuilding != null && rc.canRepair(nearbyBuilding)) {
            rc.repair(nearbyBuilding);
        } else if (labOverWt && laboratoriesBuilt == 0) {
            if (rc.canBuildRobot(RobotType.LABORATORY, builddir)) {
                rc.buildRobot(RobotType.LABORATORY, builddir);
                laboratoriesBuilt++;
                rc.writeSharedArray(55, (rc.readSharedArray(55) & 0b1111111));
            }
        } else if (rc.canBuildRobot(RobotType.WATCHTOWER, builddir) && (watchtowersBuilt == 0 || numNearbyWatchtowers == 9)) {
            rc.buildRobot(RobotType.WATCHTOWER, builddir);
            watchtowersBuilt++;
        }

        if (dir != null && rc.canMove(dir)) {
            rc.move(dir);
        }

        //Comms stuff
        Comms.updateSector(rc, turnCount);

        boolean currentHpThresh = (double)rc.getHealth()/rc.getType().getMaxHealth(1) > 0.2;
        if (!currentHpThresh && aboveHpThresh) {
            rc.writeSharedArray(54, rc.readSharedArray(54) - 1);
            rc.writeSharedArray(55, (rc.readSharedArray(55) & 0b1111111));
        } else if (currentHpThresh && !aboveHpThresh) {
            rc.writeSharedArray(54, rc.readSharedArray(54) + 1);
        }
        aboveHpThresh = currentHpThresh;
        turnsAlive ++;
    }
}
