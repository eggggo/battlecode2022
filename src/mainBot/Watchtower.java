package mainBot;

import battlecode.common.*;

public class Watchtower extends RobotPlayer{

    static int turnsNotKilledStuff = 0;
    static Integer[][] enemyArchons = null;
    static int turnsAlive = 0;
    static int attackOffset = 0;

    static int getQuadrant(RobotController rc, int x, int y) {
        int quad = 0;
        if (x >= rc.getMapWidth() / 2 && y >= rc.getMapHeight() / 2) {
            quad = 1;
        } else if (x >= rc.getMapWidth() / 2 && y < rc.getMapHeight() / 2) {
            quad = 4;
        } else if (x < rc.getMapWidth() / 2 && y >= rc.getMapHeight() / 2) {
            quad = 2;
        } else if (x < rc.getMapWidth() / 2 && y < rc.getMapHeight() / 2) {
            quad = 3;
        } else {
            System.out.println("Error: not in a quadrant");
        }
        return quad;
    }

    public static void runWatchtower(RobotController rc) throws GameActionException {
        int archonCount = rc.getArchonCount();
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        //Transforms to turret if there are enemies nearby and it can transform
        if (enemies.length > 0 && rc.getMode() == RobotMode.PORTABLE && rc.canTransform()) {
            rc.transform();
        }
        //Attacks nearby enemies if it can attack
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
                turnsNotKilledStuff = 0;
            }
        }
        //Transforms to protable if it hasn't killed anything in 30 turns.
        if (turnsNotKilledStuff > 30 && rc.getMode() == RobotMode.TURRET && rc.canTransform()) {
            rc.transform();
        }

        //Establishing potential enemy archon locations - same as soldier AI
        if (turnsAlive == 0) {
            enemyArchons = new Integer[archonCount * 3][2];
            //if all of the archons have written to the comms
            boolean quad1 = false;
            boolean quad2 = false;
            boolean quad3 = false;
            boolean quad4 = false;

            //Create an array for the quads each archon is contained in and another 2D array for each of the archons' coords
            Integer[] quads = new Integer[archonCount];
            Integer[][] coords = new Integer[archonCount][2];
            for (int i = archonCount - 1; i >= 0; i--) {
                int[] msgContents = Comms.readFromCommsArray(rc, 63-i);
                if (msgContents[0] == 0) {
                    quads[i] = getQuadrant(rc, msgContents[1], msgContents[2]);
                    coords[i][0] = msgContents[1];
                    coords[i][1] = msgContents[2];
                }
            }

            //initialize whether there's a friendly archon in each quad
            for (int a = archonCount - 1; a >= 0; a--) {
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
            if (quad1 && quad3 || quad2 && quad4) {
                //System.out.println("Rotational Symmetry");
                for (int i = archonCount - 1; i >= 0; i--) {
                    enemyArchons[3 * i][0] = coords[i][0]; //vert flip
                    enemyArchons[3 * i][1] = rc.getMapHeight() - 1 - coords[i][1]; //vert flip
                    enemyArchons[3 * i + 1][0] = rc.getMapWidth() - 1 - coords[i][0]; // horz flip
                    enemyArchons[3 * i + 1][1] = coords[i][1]; // horz flip
                    enemyArchons[3 * i + 2][0] = rc.getMapWidth() - 1 - coords[i][0]; // 180 rotate
                    enemyArchons[3 * i + 2][1] = rc.getMapHeight() - 1 - coords[i][1]; // 180 rotate
                }

            } else if (quad1 && quad2 || quad3 && quad4) {
                //System.out.println("Horizontal Symmetry or Rotational Symmetry");
                //thought that it was unecessary to check horizontal when its likely horizontally symmetric, so i just copied another vertical fip instead of using horz
                for (int i = archonCount - 1; i >= 0; i--) {
                    enemyArchons[3 * i][0] = coords[i][0]; //vert flip
                    enemyArchons[3 * i][1] = rc.getMapHeight() - 1 - coords[i][1]; //vert flip
                    enemyArchons[3 * i + 1][0] = coords[i][0]; //vert flip
                    enemyArchons[3 * i + 1][1] = rc.getMapHeight() - 1 - coords[i][1]; //vert flip
                    enemyArchons[3 * i + 2][0] = rc.getMapWidth() - 1 - coords[i][0]; // 180 rotate
                    enemyArchons[3 * i + 2][1] = rc.getMapHeight() - 1 - coords[i][1]; // 180 rotate
                }
            } else if (quad1 && quad4 || quad2 && quad3) {
                //System.out.println("Vertical Symmetry or Rotational Symmetry");
                //thought that it was unecessary to check vertical when its likely vertically symmetric, so i just copied another horiz fip instead of using vert
                for (int i = archonCount - 1; i >= 0; i--) {
                    enemyArchons[3 * i][0] = rc.getMapWidth() - 1 - coords[i][0]; // horz flip
                    enemyArchons[3 * i][1] = coords[i][1]; // horz flip
                    enemyArchons[3 * i + 1][0] = rc.getMapWidth() - 1 - coords[i][0]; // horz flip
                    enemyArchons[3 * i + 1][1] = coords[i][1]; // horz flip
                    enemyArchons[3 * i + 2][0] = rc.getMapWidth() - 1 - coords[i][0]; // 180 rotate
                    enemyArchons[3 * i + 2][1] = rc.getMapHeight() - 1 - coords[i][1]; // 180 rotate
                }
            } else {
                //System.out.println("only in one quad so cannot tell");
                for (int i = archonCount - 1; i >= 0; i--) {
                    enemyArchons[3 * i][0] = coords[i][0]; //vert flip
                    enemyArchons[3 * i][1] = rc.getMapHeight() - 1 - coords[i][1]; //vert flip
                    enemyArchons[3 * i + 1][0] = rc.getMapWidth() - 1 - coords[i][0]; // horz flip
                    enemyArchons[3 * i + 1][1] = coords[i][1]; // horz flip
                    enemyArchons[3 * i + 2][0] = rc.getMapWidth() - 1 - coords[i][0]; // 180 rotate
                    enemyArchons[3 * i + 2][1] = rc.getMapHeight() - 1 - coords[i][1]; // 180 rotate
                }
            }
        }

        //Attacks at one of the random spots of a potential enemy base after spending 100 turns home with no enemies attacking
        int randomAttackLoc = (rc.getID() + attackOffset) % (archonCount * 3);
        Direction dir = Pathfinder.getMoveDir(rc, new MapLocation(enemyArchons[randomAttackLoc][0],
                enemyArchons[randomAttackLoc][1]));

        //Changes targets after reaching the target and not killing things for 30 turns
        if (dir == Direction.CENTER && turnsNotKilledStuff > 30) {
            attackOffset++;
        }

        turnsNotKilledStuff++;

        if (rc.canMove(dir)) {
            rc.move(dir);
        }
        turnsAlive++;
    }
}
