package mainBot;
import battlecode.common.*;

public class Comms {
    static void writeToCommArray(RobotController rc, int code, int x, int y) throws GameActionException {
        int msg = (code << 12) | ((x << 6) | y);
        int firstFreeIndex = 63;
        for (int i = 63; i >= 0; i --) {
            if (rc.readSharedArray(i) == 0) {
                firstFreeIndex = i;
                break;
            }
        }
        rc.writeSharedArray(firstFreeIndex, msg);
    }

    static int[] readFromCommsArray(RobotController rc, int index) throws GameActionException {
        int msg = rc.readSharedArray(index);
        int[] msgContents = new int[3];
        msgContents[0] = msg >> 12;
        msgContents[1] = (msg >> 6) & 0b111111;
        msgContents[2] = msg & 0b111111;
        return msgContents;
    }
}
