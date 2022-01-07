package v2;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Laboratory extends RobotPlayer {
    public static void runLaboratory(RobotController rc) throws GameActionException {
        //If you can make gold, make it
        if (rc.canTransmute()) {
            rc.transmute();
        }
    }
}
