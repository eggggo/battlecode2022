package mainBot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Laboratory extends RobotPlayer {
    public static void runLaboratory(RobotController rc) throws GameActionException {
        int minerCount = rc.readSharedArray(50);
        System.out.println(minerCount);
        //If you can make gold, make it
        if (rc.canTransmute() && minerCount >= 5) {
            rc.setIndicatorString("1");
            rc.transmute();
        }

        Comms.updateSector(rc);
        Comms.updateTypeCount(rc);
    }
}
