package v3;

import battlecode.common.*;

public class Sage extends RobotPlayer{
    static int turnsAlive = 0;
    static boolean aboveHpThresh = true;
    static int turnsNotKilledStuff = 0;
    static int attackOffset = 0;
    static MapLocation home = null;
    static MapLocation[] enemyArchons = null;
    static int attackLocation = 0;
    //Role: 2 for defense, 1 for attack, 0 for scout
    static int role;
    static int castNum = 0;

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

    public static void runSage(RobotController rc) throws GameActionException {
        MapLocation src = rc.getLocation();
        int senseRadius = rc.getType().visionRadiusSquared;
        Team friendly = rc.getTeam();
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(senseRadius, opponent);

        if (turnsAlive == 0) {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(senseRadius, friendly);
            initializeSage(rc, nearbyRobots);
        }

        MapLocation closestEnemy = null;
        MapLocation closestAttackingEnemy = null;
        MapLocation closestAttackingEnemySeen = null;
        if (enemies.length > 0) {
            int buildings = 0;
            int units = 0;
            int archons = 0;
            for (int i = enemies.length - 1; i >= 0; i --) {
                RobotInfo enemy = enemies[i];
                if (rc.getLocation().distanceSquaredTo(enemy.getLocation()) <= RobotType.SAGE.actionRadiusSquared) {
                    if (closestEnemy == null || enemy.location.distanceSquaredTo(src) < closestEnemy.distanceSquaredTo(src)) {
                        closestEnemy = enemy.location;
                    } else if ((enemy.getType() == RobotType.SOLDIER || enemy.getType() == RobotType.SAGE
                            || enemy.getType() == RobotType.WATCHTOWER) && (closestAttackingEnemy == null
                            || enemy.location.distanceSquaredTo(src) < closestAttackingEnemy.distanceSquaredTo(src))) {
                        closestAttackingEnemy = enemy.location;
                        closestAttackingEnemySeen = enemy.location;
                    }
                    if (enemy.getType() == RobotType.WATCHTOWER) {
                        buildings++;
                    } else if (enemy.getType() == RobotType.ARCHON) {
                        archons++;
                    } else if (enemy.getType() == RobotType.SAGE || enemy.getType() == RobotType.SOLDIER) {
                        units++;
                    }
                } else {
                    if ((enemy.getType() == RobotType.SOLDIER || enemy.getType() == RobotType.SAGE
                            || enemy.getType() == RobotType.WATCHTOWER) && (closestAttackingEnemy == null
                            || enemy.location.distanceSquaredTo(src) < closestAttackingEnemy.distanceSquaredTo(src))) {
                        closestAttackingEnemySeen = enemy.location;
                    }
                }
            }

            MapLocation toAttack = closestEnemy;
            if (closestAttackingEnemy != null) {
                toAttack = closestAttackingEnemy;
            }

            if (units >= 5 && rc.canEnvision(AnomalyType.CHARGE)) {
                rc.envision(AnomalyType.CHARGE);
                turnsNotKilledStuff = 0;
                System.out.println("Charged");
                castNum++;
            } else if ((archons >= 1 || buildings >= 3) && rc.canEnvision(AnomalyType.FURY)) {
                rc.envision(AnomalyType.FURY);
                turnsNotKilledStuff = 0;
                System.out.println("Envisioned");
                castNum++;
            } else if (toAttack != null && rc.canAttack(toAttack)) {
                rc.attack(toAttack);
                turnsNotKilledStuff = 0;
            }
        }
        if (castNum > 2) {
            System.out.println(castNum);
        }

        Direction dir = null;
        if (rc.getHealth() < RobotType.SOLDIER.getMaxHealth(rc.getLevel()) / 10 && home != null) { // If low health run home
            dir = Pathfinder.getMoveDir(rc, home);
        } else if (closestAttackingEnemySeen != null && !rc.isActionReady()) {
            Direction opposite = src.directionTo(closestAttackingEnemySeen).opposite();
            MapLocation runawayTgt = src.add(opposite).add(opposite);
            runawayTgt = new MapLocation(Math.min(Math.max(0, runawayTgt.x), rc.getMapWidth() - 1),
                    Math.min(Math.max(0, runawayTgt.y), rc.getMapHeight() - 1));
            dir = Pathfinder.getMoveDir(rc, runawayTgt);
        } else if (turnsNotKilledStuff < 2) {
            int currRubble = rc.senseRubble(src);
            int minRubble = currRubble;
            MapLocation minRubbleLoc = src;
            if (currRubble > 0) {
                for (Direction d : Direction.allDirections()) {
                    MapLocation test = src.add(d);
                    if (rc.onTheMap(test) && !rc.isLocationOccupied(test) && rc.canSenseLocation(test)) {
                        int rubbleHere = rc.senseRubble(test);
                        if (rubbleHere < minRubble) {
                            minRubble = rubbleHere;
                            minRubbleLoc = test;
                        }
                    }
                }
            }
            dir = Pathfinder.getMoveDir(rc, minRubbleLoc);
        } else {
            int distance = Integer.MAX_VALUE;
            MapLocation actualArchonsTarget = null;
            int enemySectorDistance = 9999;
            MapLocation closestEnemies = null;
            for (int i = 48; i >= 0; i--) {
                int[] sector = Comms.readSectorInfo(rc, i);
                MapLocation loc = Comms.sectorMidpt(rc, i);
                if (sector[3] > 5 && enemySectorDistance > rc.getLocation().distanceSquaredTo(loc)) {
                    closestEnemies = loc;
                    enemySectorDistance = rc.getLocation().distanceSquaredTo(loc);
                }
                if (sector[1] == 1 && distance > rc.getLocation().distanceSquaredTo(loc)) {
                    actualArchonsTarget = loc;
                    distance = rc.getLocation().distanceSquaredTo(loc);
                }
            }
            if (actualArchonsTarget != null) {
                dir = Pathfinder.getMoveDir(rc, actualArchonsTarget);
            } else {
                //Attacks at one of the random spots of a potential enemy base

                MapLocation attackTarget = enemyArchons[(rc.getID() + attackOffset) % (enemyArchons.length)];

                //Change target if theres nothing at the target
                if (rc.canSenseLocation(attackTarget)) {
                    RobotInfo rb = rc.senseRobotAtLocation(attackTarget);
                    if (rb == null || rb.getType() != RobotType.ARCHON) {
                        attackOffset += 1;

                    }
                }
                dir = Pathfinder.getMoveDir(rc, attackTarget);
            }
        }

        if (dir != null && rc.canMove(dir)) {
            rc.move(dir);
        }

        if (turnsAlive == 0) {
            rc.writeSharedArray(53, rc.readSharedArray(53) + 1);
        }
        boolean currentHpThresh = (double)rc.getHealth()/rc.getType().getMaxHealth(1) > 0.7;
        if (!currentHpThresh && aboveHpThresh) {
            rc.writeSharedArray(53, rc.readSharedArray(53) - 1);
        } else if (currentHpThresh && !aboveHpThresh) {
            rc.writeSharedArray(53, rc.readSharedArray(53) + 1);
        }
        turnsNotKilledStuff++;
        Comms.updateSector(rc);
        aboveHpThresh = currentHpThresh;
        turnsAlive ++;
    }

    static void initializeSage(RobotController rc, RobotInfo[] nearbyRobots) throws GameActionException{
        role = 1;
        int archonCount = 4;
        for (int i = nearbyRobots.length - 1; i >= 0; i--) {
            if (nearbyRobots[i].getType() == RobotType.ARCHON) {
                home = nearbyRobots[i].getLocation();
                break;
            }
        }


        //if all of the archons have written to the comms
        boolean quad1 = false;
        boolean quad2 = false;
        boolean quad3 = false;
        boolean quad4 = false;

        //Create an array for the quads each archon is contained in and another 2D array for each of the archons' coords
        int currentArchonIndex = 0;
        int[] quads = new int[archonCount];
        MapLocation[] coords = new MapLocation[archonCount];
        for (int i = 48; i >= 0; i--) {
            int[] sector = Comms.readSectorInfo(rc, i);
            //System.out.println("sector " + i + ": " + Arrays.toString(sector));
            if (sector[0] == 1) {
                MapLocation mdpt = Comms.sectorMidpt(rc, i);
                quads[currentArchonIndex] = getQuadrant(rc, mdpt.x, mdpt.y);
                coords[currentArchonIndex] = mdpt;
                currentArchonIndex++;
            }
        }
        archonCount = currentArchonIndex;
        int[] tempQuads = new int[currentArchonIndex];
        MapLocation[] tempCoords = new MapLocation[currentArchonIndex];
        for (int i = currentArchonIndex - 1; i >= 0; i --) {
            tempQuads[i] = quads[i];
            tempCoords[i] = coords[i];
        }
        quads = tempQuads;
        coords = tempCoords;

        enemyArchons = new MapLocation[archonCount * 3];

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
                enemyArchons[3 * i] = new MapLocation(coords[i].x, rc.getMapHeight() - 1 - coords[i].y); //vert flip
                enemyArchons[3 * i + 1] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, coords[i].y); // horz flip
                enemyArchons[3 * i + 2] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, rc.getMapHeight() - 1 - coords[i].y); // 180 rotate
            }

        } else if (quad1 && quad2 || quad3 && quad4) {
            //System.out.println("Horizontal Symmetry or Rotational Symmetry");
            //thought that it was unecessary to check horizontal when its likely horizontally symmetric, so i just copied another vertical fip instead of using horz
            for (int i = archonCount - 1; i >= 0; i--) {
                enemyArchons[3 * i] = new MapLocation(coords[i].x, rc.getMapHeight() - 1 - coords[i].y); //vert flip
                enemyArchons[3 * i + 1] = new MapLocation(coords[i].x, rc.getMapHeight() - 1 - coords[i].y); // vert flip
                enemyArchons[3 * i + 2] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, rc.getMapHeight() - 1 - coords[i].y); // 180 rotate
            }
        } else if (quad1 && quad4 || quad2 && quad3) {
            //System.out.println("Vertical Symmetry or Rotational Symmetry");
            //thought that it was unecessary to check vertical when its likely vertically symmetric, so i just copied another horiz fip instead of using vert
            for (int i = archonCount - 1; i >= 0; i--) {
                enemyArchons[3 * i] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, coords[i].y); //horz flip
                enemyArchons[3 * i + 1] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, coords[i].y); // horz flip
                enemyArchons[3 * i + 2] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, rc.getMapHeight() - 1 - coords[i].y); // 180 rotate
            }
        } else {
            //System.out.println("only in one quad so cannot tell");
            for (int i = archonCount - 1; i >= 0; i--) {
                enemyArchons[3 * i] = new MapLocation(coords[i].x, rc.getMapHeight() - 1 - coords[i].y); //vert flip
                enemyArchons[3 * i + 1] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, coords[i].y); // horz flip
                enemyArchons[3 * i + 2] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, rc.getMapHeight() - 1 - coords[i].y); // 180 rotate
            }
        }
        rc.writeSharedArray(51, rc.readSharedArray(51) + 1);
    }
}
