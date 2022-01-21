package mainBot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Laboratory extends RobotPlayer {
    static int minerCount = 0;
    public static void runLaboratory(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() % 2 == 1) {
            minerCount = rc.readSharedArray(50);
        }
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
