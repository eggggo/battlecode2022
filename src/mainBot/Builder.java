package mainBot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Builder extends RobotPlayer {

    static int laboratoriesBuilt = 0;
    static int watchtowersBuilt = 0;
    public static void runBuilder(RobotController rc) throws GameActionException {
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
        if (rc.canBuildRobot(RobotType.LABORATORY, Direction.SOUTH)) {
            rc.buildRobot(RobotType.LABORATORY, Direction.SOUTH);
        }
        if (rc.canBuildRobot(RobotType.WATCHTOWER, Direction.SOUTH) && 3*laboratoriesBuilt >= watchtowersBuilt) {
            rc.buildRobot(RobotType.WATCHTOWER, Direction.SOUTH);
        }
    }
}
