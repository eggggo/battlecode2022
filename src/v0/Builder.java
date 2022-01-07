package v0;

import battlecode.common.*;

public class Builder extends RobotPlayer {

    static int laboratoriesBuilt = 0;
    static int watchtowersBuilt = 0;

    public static void runBuilder(RobotController rc) throws GameActionException {

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

        //If theres a nearby watchtower or laboratory that needs to be repaired, set the nearbyBulding to it.
        for (int i = nearbyRobots.length - 1; i >= 0; i--) {
            if (nearbyRobots[i].getType() == RobotType.WATCHTOWER && nearbyRobots[i].getHealth() != RobotType.WATCHTOWER.getMaxHealth(nearbyRobots[i].getLevel())) {
                nearbyBuilding = nearbyRobots[i].getLocation();
                break;
            } else if (nearbyRobots[i].getType() == RobotType.LABORATORY && nearbyRobots[i].getHealth() != RobotType.LABORATORY.getMaxHealth(nearbyRobots[i].getLevel())) {
                nearbyBuilding = nearbyRobots[i].getLocation();
                break;
            }
        }

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
        } else if (nearbySoldier == null || nearbyRobots.length > 9) {
            dir = directions[rng.nextInt(directions.length)];
        } else {
            dir = Pathfinder.getMoveDir(rc, nearbySoldier);
        }

        //If there is a nearby building that can be repaired, repair it, otherwise go to the nearest repariable buidling and repair it.
        if (nearbyBuilding != null && rc.canRepair(nearbyBuilding)) {
            rc.repair(nearbyBuilding);
        } else if (rc.canBuildRobot(RobotType.LABORATORY, Direction.SOUTH)) {
            rc.buildRobot(RobotType.LABORATORY, Direction.SOUTH);
        } else if (rc.canBuildRobot(RobotType.WATCHTOWER, Direction.SOUTH) && 3*laboratoriesBuilt >= watchtowersBuilt && numNearbyWatchtowers < 3) {
            rc.buildRobot(RobotType.WATCHTOWER, Direction.SOUTH);
        }

        if (rc.canMove(dir)) {
            rc.move(dir);
        }

        //Comms stuff
        Comms.updateSector(rc);
    }
}
