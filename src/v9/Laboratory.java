package v9;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Laboratory extends RobotPlayer {
    static int minerCount = 0;
    static int sageCount = 0;
    public static void runLaboratory(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() % 2 == 1) {
            minerCount = rc.readSharedArray(50);
            sageCount = rc.readSharedArray(53);
        }

        int minMiners = sageCount/2;
        int mapArea = rc.getMapHeight() * rc.getMapWidth();

        //If you can make gold, make it
        if (rc.canTransmute() && minerCount >= minMiners) {
            rc.setIndicatorString("1");
            rc.transmute();
        }

        Comms.updateSector(rc);
        Comms.updateTypeCount(rc);
    }
}
