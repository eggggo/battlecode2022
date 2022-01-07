package v0;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Sage extends RobotPlayer{
    static int turnsAlive = 0;
    static boolean aboveHpThresh = true;

    public static void runSage(RobotController rc) throws GameActionException {
        if (turnsAlive == 0) {
            rc.writeSharedArray(53, rc.readSharedArray(53) + 1);
        }
        boolean currentHpThresh = (double)rc.getHealth()/rc.getType().getMaxHealth(1) > 0.7;
        if (!currentHpThresh && aboveHpThresh) {
            rc.writeSharedArray(53, rc.readSharedArray(53) - 1);
        } else if (currentHpThresh && !aboveHpThresh) {
            rc.writeSharedArray(53, rc.readSharedArray(53) + 1);
        }
        aboveHpThresh = currentHpThresh;
        turnsAlive ++;
    }
}
