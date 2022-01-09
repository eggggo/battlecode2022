package mainBot;

import battlecode.common.*;

public class Builder extends RobotPlayer {

    static int laboratoriesBuilt = 0;
    static int watchtowersBuilt = 0;
    static int turnsAlive = 0;
    static boolean aboveHpThresh = true;
    static MapLocation home = null;

    public static void runBuilder(RobotController rc) throws GameActionException {
        if (turnsAlive == 0) {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
            for (int i = nearbyRobots.length - 1; i >= 0; i--) {
                if (nearbyRobots[i].getType() == RobotType.ARCHON) {
                    home = nearbyRobots[i].getLocation();
                    break;
                }
            }
        }
        int currentIncome = rc.readSharedArray(49);
        int senseRadius = rc.getType().visionRadiusSquared;
        Team friendly = rc.getTeam();
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(senseRadius, friendly);
        MapLocation nearbySoldier = null;
        MapLocation nearbyBuilding = null;
        int numNearbyWatchtowers = 0;

        //Sensing Important information like nearby soldiers:
        for (int i = nearbyRobots.length - 1; i >= 0; i--) {
            if (nearbyRobots[i].getType() == RobotType.SOLDIER) {
                nearbySoldier = nearbyRobots[i].getLocation();
                break;
            }
        }

        int distanceFromBuilding = Integer.MAX_VALUE;
        //If theres a nearby watchtower or laboratory that needs to be repaired, set the nearbyBulding to it.
        for (int i = nearbyRobots.length - 1; i >= 0; i--) {
            if (nearbyRobots[i].getType() == RobotType.WATCHTOWER && nearbyRobots[i].getHealth() !=
                    RobotType.WATCHTOWER.getMaxHealth(nearbyRobots[i].getLevel()) && rc.getLocation().distanceSquaredTo(nearbyRobots[i].getLocation()) < distanceFromBuilding) {
                nearbyBuilding = nearbyRobots[i].getLocation();
                distanceFromBuilding = rc.getLocation().distanceSquaredTo(nearbyRobots[i].getLocation());
            } else if (nearbyRobots[i].getType() == RobotType.LABORATORY && nearbyRobots[i].getHealth() !=
                    RobotType.LABORATORY.getMaxHealth(nearbyRobots[i].getLevel()) && rc.getLocation().distanceSquaredTo(nearbyRobots[i].getLocation()) < distanceFromBuilding) {
                nearbyBuilding = nearbyRobots[i].getLocation();
                distanceFromBuilding = rc.getLocation().distanceSquaredTo(nearbyRobots[i].getLocation());
            }
        }
        System.out.println(distanceFromBuilding);
        //Tracking nearby Watchtower amount
        for (int i = nearbyRobots.length - 1; i >= 0; i--) {
            if (nearbyRobots[i].getType() == RobotType.WATCHTOWER) {
                numNearbyWatchtowers++;
            }
        }

        Direction dir;

        //If there is no nearby repariable building, follow a nearby non-crowded soldier, otherwise move randomly
        if (nearbyBuilding != null) {
            dir = Pathfinder.getMoveDir(rc, nearbyBuilding);
        } else if (nearbySoldier != null && nearbyRobots.length < 9) {
            dir = Pathfinder.getMoveDir(rc, nearbySoldier);
        } else if (home != null) {
            dir = rc.getLocation().directionTo(home).opposite();
        } else {
            dir = directions[rng.nextInt(directions.length)];
        }

        MapLocation src = rc.getLocation();
        Direction builddir = directions[rng.nextInt(directions.length)];
        for (Direction dire : Direction.allDirections()) {
            MapLocation loc = src.add(dire);
            if (rc.onTheMap(loc) && rc.senseRobotAtLocation(loc) == null) {
                builddir = dire;
                break;
            }
        }

        //If there is a nearby building that can be repaired, repair it, otherwise go to the nearest repariable buidling and repair it.
        if (nearbyBuilding != null && rc.canRepair(nearbyBuilding)) {
            rc.repair(nearbyBuilding);
        } else if (rc.getID() % 10 == 1 && laboratoriesBuilt == 0 && rc.canBuildRobot(RobotType.LABORATORY, builddir)) {
            if (home != null && rc.getLocation().distanceSquaredTo(home) > 9) {
                rc.buildRobot(RobotType.LABORATORY, builddir);
                laboratoriesBuilt++;
            } else if (home == null){
                rc.buildRobot(RobotType.LABORATORY, builddir);
                laboratoriesBuilt++;
            }
        } else if (rc.canBuildRobot(RobotType.WATCHTOWER, builddir) && currentIncome > 30 && rc.getTeamLeadAmount(rc.getTeam()) > 200) {
            if (home != null && rc.getLocation().distanceSquaredTo(home) > 9) {
                rc.buildRobot(RobotType.WATCHTOWER, builddir);
            } else if (home == null){
                rc.buildRobot(RobotType.WATCHTOWER, builddir);
            }
        }

        if (rc.canMove(dir)) {
            rc.move(dir);
        }

        //Comms stuff
        Comms.updateSector(rc, turnCount);

        boolean currentHpThresh = (double)rc.getHealth()/rc.getType().getMaxHealth(1) > 0.2;
        if (!currentHpThresh && aboveHpThresh) {
            rc.writeSharedArray(54, rc.readSharedArray(54) - 1);
        } else if (currentHpThresh && !aboveHpThresh) {
            rc.writeSharedArray(54, rc.readSharedArray(54) + 1);
        }
        aboveHpThresh = currentHpThresh;
        turnsAlive ++;
    }
}
