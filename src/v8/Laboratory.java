package v8;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Laboratory extends RobotPlayer {
    static boolean aboveHpThresh = true;
    public static void runLaboratory(RobotController rc) throws GameActionException {
        //If you can make gold, make it
        if (rc.canTransmute()) {
            rc.setIndicatorString("1");
            rc.transmute();
        }

        Comms.updateSector(rc, turnCount);
        boolean currentHpThresh = (double)rc.getHealth()/rc.getType().getMaxHealth(rc.getLevel()) > 0.2;
        if (!currentHpThresh && aboveHpThresh) {
            rc.writeSharedArray(56, rc.readSharedArray(56) - 1);
        } else if (currentHpThresh && !aboveHpThresh) {
            rc.writeSharedArray(56, rc.readSharedArray(56) + 1);
        }
        aboveHpThresh = currentHpThresh;
    }
}
