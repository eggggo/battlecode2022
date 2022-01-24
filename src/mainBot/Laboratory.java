package mainBot;

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
//        if (sageCount > 35) {
//            minMiners = (sageCount * 3) / 4;
//        }
        int mapArea = rc.getMapHeight() * rc.getMapWidth();
        double thresh = Math.sqrt(mapArea);

        //If you can make gold, make it
        if (rc.canTransmute() && (minerCount >= minMiners || minerCount >= 90)) {
            rc.setIndicatorString("1");
            rc.transmute();
        }

        Comms.updateSector(rc);
        Comms.updateTypeCount(rc);
    }
}
