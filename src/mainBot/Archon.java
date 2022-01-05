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

        //if all of the archons have written to the comms
        if (rc.readSharedArray(64 - rc.getArchonCount()) != 0) {

            boolean quad1 = false;
            boolean quad2 = false;
            boolean quad3 = false;
            boolean quad4 = false;

            //Create an array for the quads each archon is contained in and another 2D array for each of the archons' coords
            Integer[] quads = new Integer[rc.getArchonCount()];
            Integer[][] coords = new Integer[rc.getArchonCount()][2];
            for (int i = rc.getArchonCount() - 1; i >= 0; i--) {
                quads[i] = rc.readSharedArray(63 - i) / 10000;
                coords[i][0] = (rc.readSharedArray(63 - i) % 10000) / 100;
                coords[i][1] = (rc.readSharedArray(63 - i) % 100);
            }

            //initialize whether there's a friendly archon in each quad
            for (int a = rc.getArchonCount() - 1; a >= 0; a--) {
                if (quads[a] == 1) {
                    quad1 = true;
                } else if (quads[a] == 2) {
                    quad2 = true;
                } else if (quads[a] == 3) {
                    quad3 = true;
                } else if (quads[a] == 4) {
                    quad4 = true;
                }
            }

            //Predict location of enemy archons based on potential symmetry.  For each archon, I check the 180 rotation,
            // horizontal flip, and vertical flip.  These are only potential locations, as roataion can occur at other degrees of rotation.
            Integer[][] enemyArchons = new Integer[rc.getArchonCount() * 3][2];
            if (quad1 && quad3 || quad2 && quad4) {
                System.out.println("Rotational Symmetry");
                for (int i = rc.getArchonCount() - 1; i >= 0; i--) {
                    enemyArchons[3 * i][0] = coords[i][0]; //vert flip
                    enemyArchons[3 * i][1] = rc.getMapHeight() - 1 - coords[i][1]; //vert flip
                    enemyArchons[3 * i + 1][0] = rc.getMapWidth() - 1 - coords[i][0]; // horz flip
                    enemyArchons[3 * i + 1][1] = coords[i][1]; // horz flip
                    enemyArchons[3 * i + 2][0] = rc.getMapWidth() - 1 - coords[i][0]; // 180 rotate
                    enemyArchons[3 * i + 2][1] = rc.getMapHeight() - 1 - coords[i][1]; // 180 rotate
                }

            } else if (quad1 && quad2 || quad3 && quad4) {
                System.out.println("Horizontal Symmetry or Rotational Symmetry");
                //thought that it was unecessary to check horizontal when its likely horizontally symmetric, so i just copied another vertical fip instead of using horz
                for (int i = rc.getArchonCount() - 1; i >= 0; i--) {
                    enemyArchons[3 * i][0] = coords[i][0]; //vert flip
                    enemyArchons[3 * i][1] = rc.getMapHeight() - 1 - coords[i][1]; //vert flip
                    enemyArchons[3 * i + 1][0] = coords[i][0]; //vert flip
                    enemyArchons[3 * i + 1][1] = rc.getMapHeight() - 1 - coords[i][1]; //vert flip
                    enemyArchons[3 * i + 2][0] = rc.getMapWidth() - 1 - coords[i][0]; // 180 rotate
                    enemyArchons[3 * i + 2][1] = rc.getMapHeight() - 1 - coords[i][1]; // 180 rotate
                }
            } else if (quad1 && quad4 || quad2 && quad3) {
                System.out.println("Vertical Symmetry or Rotational Symmetry");
                //thought that it was unecessary to check vertical when its likely vertically symmetric, so i just copied another horiz fip instead of using vert
                for (int i = rc.getArchonCount() - 1; i >= 0; i--) {
                    enemyArchons[3 * i][0] = rc.getMapWidth() - 1 - coords[i][0]; // horz flip
                    enemyArchons[3 * i][1] = coords[i][1]; // horz flip
                    enemyArchons[3 * i + 1][0] = rc.getMapWidth() - 1 - coords[i][0]; // horz flip
                    enemyArchons[3 * i + 1][1] = coords[i][1]; // horz flip
                    enemyArchons[3 * i + 2][0] = rc.getMapWidth() - 1 - coords[i][0]; // 180 rotate
                    enemyArchons[3 * i + 2][1] = rc.getMapHeight() - 1 - coords[i][1]; // 180 rotate
                }
            } else {
                System.out.println("only in one quad so cannot tell");
                for (int i = rc.getArchonCount() - 1; i >= 0; i--) {
                    enemyArchons[3 * i][0] = coords[i][0]; //vert flip
                    enemyArchons[3 * i][1] = rc.getMapHeight() - 1 - coords[i][1]; //vert flip
                    enemyArchons[3 * i + 1][0] = rc.getMapWidth() - 1 - coords[i][0]; // horz flip
                    enemyArchons[3 * i + 1][1] = coords[i][1]; // horz flip
                    enemyArchons[3 * i + 2][0] = rc.getMapWidth() - 1 - coords[i][0]; // 180 rotate
                    enemyArchons[3 * i + 2][1] = rc.getMapHeight() - 1 - coords[i][1]; // 180 rotate
                }
            }
            System.out.println(Arrays.deepToString(enemyArchons));
        }
    }
}
