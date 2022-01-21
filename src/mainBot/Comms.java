package mainBot;

import java.util.Arrays;

import battlecode.common.*;

public class Comms {
    //revamped comms agane, each index corresponds to a sector on the map where 0 is bottom left and 48 is top right
    //each sector info contains 1 bit home archon presence, 1 bit enemy archon presence, 
    //6 bit resource patch count(capped at 61), 5 bit enemy count(capped at 31), 1 bit turnMod, (2 bits free)
    //indices 49 through 63 for other info:
    //49: global income - 16 bit income count
    //50: miner count - 16 bit miner count
    //51: soldier count - 16 bit soldier count
    //52: watchtower count - 16 bit watchtower count
    //53: sage count - 16 bit sage count
    //54: builder count - 16 bit builder count
    //55: 1 bit resource saving mode, 1 bit first soldier, last 6 bits sector of first seen enemy
    //56: lab count - 16 bit lab count
    //57: archon cycler for clearing
    static int locationToSector(RobotController rc, MapLocation loc) {
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        double xSize = width/7.0;
        double ySize = height/7.0;
        int xSector = (int)(loc.x/xSize);
        int ySector = (int)(loc.y/ySize);
        return ySector * 7 + xSector;
    }

    static MapLocation sectorMidpt(RobotController rc, int sector) {
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        double xSize = width/7.0;
        double ySize = height/7.0;
        int xCoord = (int)((sector % 7) * xSize + xSize/2);
        int yCoord = (int)((sector / 7) * ySize + ySize/2);
        return new MapLocation(xCoord, yCoord);
    }

    static int[] readSectorInfo(RobotController rc, int sector) throws GameActionException {
        int[] res = new int[5];
        int entry = rc.readSharedArray(sector);
        res[0] = (entry >> 15); //home archon presence
        res[1] = (entry >> 14) & 0b1; //enemy archon presence
        res[2] = (entry >> 8) & 0b111111; //resource count max 63
        res[3] = (entry >> 3) & 0b11111; //enemy attacker count max 31
        res[4] = (entry >> 2) & 0b1; //turnMod
        return res;
    }

    static boolean withinSector(RobotController rc, MapLocation loc, int sector) throws GameActionException {
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        double xSize = width/7.0;
        double ySize = height/7.0;
        int lowerX = (int)((sector%7)*xSize);
        int lowerY = (int)((sector / 7)*ySize);
        return (loc.x >= lowerX && loc.x < lowerX + xSize && loc.y >= lowerY && loc.y < lowerY + ySize);
    }

    static void updateSector(RobotController rc) throws GameActionException {
        int sector = locationToSector(rc, rc.getLocation());
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        double xSize = width/7.0;
        double ySize = height/7.0;
        int homeArchon = 0;
        int enemyArchon = 0;
        int resourceCount = 0;
        int enemyCount = 0;
        int range = rc.getType().visionRadiusSquared;
        int[] entry = readSectorInfo(rc, sector);
        int lowerX = (int)((sector%7)*xSize);
        int lowerY = (int)((sector/7)*xSize);
        int turnMod = rc.getRoundNum() % 2;
        boolean firstSeenEnemy = (rc.readSharedArray(55) & 0b111111) == 0;

        if (rc.getType() == RobotType.ARCHON) {
            if ((double)rc.getHealth()/rc.getType().getMaxHealth(1) > 0.1) {
                homeArchon = 1;
            } else {
                homeArchon = 0;
            }
        } else {
            homeArchon = entry[0];
        }

        RobotInfo[] enemies = rc.senseNearbyRobots(range, rc.getTeam().opponent());

        if (enemies.length > 0 && firstSeenEnemy) {
            rc.writeSharedArray(55, (rc.readSharedArray(55) & 0b1111111111000000) | (sector+1));
        }

        for (int i = enemies.length - 1; i >= 0; i --) {
            if (enemyArchon == 1 && enemyCount >= 31) {
                break;
            }
            RobotInfo r = enemies[i];
            if (withinSector(rc, r.getLocation(), sector)) {
                if (r.getType() == RobotType.ARCHON) {
                    enemyArchon = 1;
                }
                enemyCount ++;
                if ((r.getType() == RobotType.SOLDIER
                        || r.getType() == RobotType.SAGE
                        || r.getType() == RobotType.WATCHTOWER)) {
                            enemyCount += 4;
                }
            }
        }
        enemyCount = Math.min(31, enemyCount);

        MapLocation[] nearbyLead = rc.senseNearbyLocationsWithLead(range, 10);
        MapLocation[] nearbyGold = rc.senseNearbyLocationsWithGold(range);
        if (nearbyGold.length > 0) {
            resourceCount = 63;
        } else {
            resourceCount = Math.min(63, nearbyLead.length);
        }

        if (entry[4] == turnMod) {
            enemyArchon = Math.max(enemyArchon, entry[1]);
            enemyCount = Math.max(enemyCount, entry[3]);
            resourceCount = Math.max(resourceCount, entry[2]);
        }

        int msg = (homeArchon << 15) | (enemyArchon << 14) | (resourceCount << 8) | (enemyCount << 3) | (turnMod << 2);
        rc.writeSharedArray(sector, msg);
    }

    static void printComms(RobotController rc) throws GameActionException {
        for (int i = 48; i >= 0; i --) {
            System.out.println("sector: " + i + ", " + Arrays.toString(readSectorInfo(rc, i)));
        }
    }

    static void updateTypeCount(RobotController rc) throws GameActionException {
        int index = -1;
        switch (rc.getType()) {
            case MINER:
                index = 50;
                break;
            case SOLDIER:
                index = 51;
                break;
            case WATCHTOWER:
                index = 52;
                break;
            case SAGE:
                index = 53;
                break;
            case BUILDER:
                index = 54;
                break;
            case LABORATORY:
                index = 56;
                break;
            default:
                index = -1;
        }
        rc.writeSharedArray(index, rc.readSharedArray(index) + 1);
    }

    static void clearCounts(RobotController rc) throws GameActionException {
        int numberArchon = ((rc.readSharedArray(57)) + 1) % rc.getArchonCount();
        if (numberArchon == 0) {
            if (rc.readSharedArray(49) != 0) {
                rc.writeSharedArray(49, 0);
            }
            if (rc.readSharedArray(50) != 0) {
                rc.writeSharedArray(50, 0);
            }
            if (rc.readSharedArray(51) != 0) {
                rc.writeSharedArray(51, 0);
            }
            if (rc.readSharedArray(52) != 0) {
                rc.writeSharedArray(52, 0);
            }
            if (rc.readSharedArray(53) != 0) {
                rc.writeSharedArray(53, 0);
            }
            if (rc.readSharedArray(54) != 0) {
                rc.writeSharedArray(54, 0);
            }
            if (rc.readSharedArray(56) != 0) {
                rc.writeSharedArray(56, 0);
            }
        }
        rc.writeSharedArray(57, numberArchon);
    }
}