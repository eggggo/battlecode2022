package mainBot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

import java.sql.SQLOutput;
import java.util.Arrays;

public class Archon extends RobotPlayer {
    static int minersBuilt = 0;
    static int soldiersBuilt = 0;
    static int buildersBuilt = 0;
    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runArchon(RobotController rc) throws GameActionException {

        //Countin nearby soldier and miner counts.
        int nearbySoldiers = 0;
        int nearbyMiners = 0;
        for (int i = rc.senseNearbyRobots().length - 1; i >= 0; i--) {
            if (rc.senseNearbyRobots()[i].getType() == RobotType.SOLDIER) {
                nearbySoldiers++;
            } else if (rc.senseNearbyRobots()[i].getType() == RobotType.MINER) {
                nearbyMiners++;
            }
        }

        // Building
        Direction dir = directions[rng.nextInt(directions.length)];

        //Bulders are built with an about 1:20 ratio to the number of total units owned
        System.out.println(rc.getRobotCount());
        if (rc.getRobotCount() > (buildersBuilt + 1) * 30 && buildersBuilt < 5) {
            if (rc.canBuildRobot(RobotType.BUILDER, dir)) {
                rc.buildRobot(RobotType.BUILDER, dir);
                buildersBuilt++;
            }
            //miners are built when the neaby Miner count is less than 1.5 times the nearby Soldier count
        } else if (rc.canBuildRobot(RobotType.MINER, dir) && nearbyMiners < 1.5 * nearbySoldiers) {
            rc.buildRobot(RobotType.MINER, dir);
            minersBuilt++;
            //Soldiers are built when none of the above conditions are satisfied.
        } else if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
            rc.buildRobot(RobotType.SOLDIER, dir);
            soldiersBuilt++;
        }

        //Encoding location of friendly archons in the last 4 indicies of our comms array.  First number is the quadrant
        // number the archon is housed in and the last four digits is the x and y coordinates of the archon.
        for (int i = 63; i >= 0; i--) {
            if (rc.readSharedArray(i) == 0) {
                int quad = 0;
                if (rc.getLocation().x >= rc.getMapWidth() / 2 && rc.getLocation().y >= rc.getMapHeight() / 2) {
                    quad = 1;
                } else if (rc.getLocation().x >= rc.getMapWidth() / 2 && rc.getLocation().y < rc.getMapHeight() / 2) {
                    quad = 4;
                } else if (rc.getLocation().x < rc.getMapWidth() / 2 && rc.getLocation().y >= rc.getMapHeight() / 2) {
                    quad = 2;
                } else if (rc.getLocation().x < rc.getMapWidth() / 2 && rc.getLocation().y < rc.getMapHeight() / 2) {
                    quad = 3;
                } else {
                    System.out.println("Error: not in a quadrant");
                }
                rc.writeSharedArray(i, quad * 10000 + rc.getLocation().x * 100 + rc.getLocation().y);
                break;
            }
        }
    }
}
