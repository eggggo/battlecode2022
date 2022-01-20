package stratBot;

import java.util.Arrays;

import battlecode.common.*;

public class Comms {
  //revamped comms agane, each index corresponds to a sector on the map where 0 is bottom left and 48 is top right
  //each sector info contains 1 bit home archon presence, 1 bit enemy archon presence,
  //8 bit resource count(capped at 255), 6 bit enemy count(capped at 63)
  //indices 49 through 63 for other info:
  //49: global income
  //50: healthy miner count(above 20% hp)
  //51: healthy soldier count(above 70%)
  //52: healthy watchtower count(above 70%)
  //53: healthy sage count(above 70%)
  //54: healthy builder count(above 20% hp)
  static int locationToSector(RobotController rc, MapLocation loc) {
    int width = rc.getMapWidth();
    int height = rc.getMapHeight();
    double xSize = width / 7.0;
    double ySize = height / 7.0;
    int xSector = (int) (loc.x / xSize);
    int ySector = (int) (loc.y / ySize);
    return ySector * 7 + xSector;
  }

  static MapLocation sectorMidpt(RobotController rc, int sector) {
    int width = rc.getMapWidth();
    int height = rc.getMapHeight();
    double xSize = width / 7.0;
    double ySize = height / 7.0;
    int xCoord = (int) ((sector % 7) * xSize + xSize / 2);
    int yCoord = (int) ((sector / 7) * ySize + ySize / 2);
    return new MapLocation(xCoord, yCoord);
  }

  static int[] readSectorInfo(RobotController rc, int sector) throws GameActionException {
    int[] res = new int[4];
    int entry = rc.readSharedArray(sector);
    res[0] = (entry >> 15);
    res[1] = (entry >> 14) & 0b1;
    res[2] = (entry >> 6) & 0b11111111;
    res[3] = entry & 0b111111;
    return res;
  }

  static boolean withinSector(RobotController rc, MapLocation loc, int sector) throws GameActionException {
    int width = rc.getMapWidth();
    int height = rc.getMapHeight();
    double xSize = width / 7.0;
    double ySize = height / 7.0;
    MapLocation bottomLeft = new MapLocation((int) ((sector % 7) * xSize), (int) ((sector / 7) * ySize));
    MapLocation topRight = new MapLocation((int) Math.ceil(((sector % 7) + 1) * xSize), (int) Math.ceil(((sector / 7) + 1) * ySize));
    return (loc.x >= bottomLeft.x && loc.x < topRight.x && loc.y >= bottomLeft.y && loc.y < topRight.y);
  }

  static void updateSector(RobotController rc) throws GameActionException {
    int sector = locationToSector(rc, rc.getLocation());
    int homeArchon = 0;
    int enemyArchon = 0;
    int resourceCount = 0;
    int enemyCount = 0;
    int range = rc.getType().visionRadiusSquared;
    int[] entry = readSectorInfo(rc, sector);

    if (rc.getType() == RobotType.ARCHON) {
      if ((double) rc.getHealth() / rc.getType().getMaxHealth(1) > 0.1) {
        homeArchon = 1;
      } else {
        homeArchon = 0;
      }
    } else {/*
            MapLocation mdpt = sectorMidpt(rc, sector);
            boolean trueVision = rc.getLocation().distanceSquaredTo(mdpt) <= 2;
            if (entry[0] == 0 || trueVision) {
                RobotInfo[] friendlies = rc.senseNearbyRobots(range, rc.getTeam());
                for (int i = friendlies.length - 1; i >= 0; i --) {
                    RobotInfo r = friendlies[i];
                    if (r.getType() == RobotType.ARCHON && withinSector(rc, r.getLocation(), sector)) {
                        homeArchon = 1;
                        break;
                    }
                }
            } else {*/
      homeArchon = entry[0];
      //}
    }

    RobotInfo[] enemies = rc.senseNearbyRobots(range, rc.getTeam().opponent());

    for (int i = enemies.length - 1; i >= 0; i--) {
      RobotInfo r = enemies[i];
      if (r.getType() == RobotType.ARCHON && withinSector(rc, r.getLocation(), sector)) {
        enemyArchon = 1;
      } else if ((r.getType() == RobotType.SOLDIER
              || r.getType() == RobotType.SAGE
              || r.getType() == RobotType.WATCHTOWER) && withinSector(rc, r.getLocation(), sector)) {
        enemyCount++;
      }
    }
    enemyCount = Math.min(63, enemyCount);

    MapLocation[] nearbyLead = rc.senseNearbyLocationsWithLead(range);
    MapLocation[] nearbyGold = rc.senseNearbyLocationsWithGold(range);
    if (nearbyGold.length > 0) {
      resourceCount = 255;
    } else {
      int leadCount = 0;
      for (int i = nearbyLead.length - 1; i >= 0; i--) {
        if (withinSector(rc, nearbyLead[i], sector)) {
          leadCount += rc.senseLead(nearbyLead[i]);
        }
      }
      resourceCount = Math.min(leadCount, 255);
    }

    int msg = (homeArchon << 15) | (enemyArchon << 14) | (resourceCount << 6) | (enemyCount);
    rc.writeSharedArray(sector, msg);
  }

  static void initHomeArchon(RobotController rc, MapLocation loc) throws GameActionException {
    for (int i = 3; i >= 0; i -= 1) {
      int entry = rc.readSharedArray(55 + i);
      int code = 0b1111 & entry;
      if (code == 0) {
        entry = (loc.x << 10) | (loc.y << 4) | 1;

        rc.writeSharedArray(55 + i, entry);
        break;
      }
    }

  }

  static MapLocation[] readHomeArchonLocations(RobotController rc) throws GameActionException {
    MapLocation[] homes = new MapLocation[rc.getArchonCount()];

    int index = 0;
    for (int i = 3; i >= 0; i -= 1) {
      int entry = rc.readSharedArray(55 + i);
      int code = 0b1111 & entry;

      if (code != 0) {
        int x = entry >> 10;
        int y = entry >> 4 & 0b111111;
        homes[index] = new MapLocation(x, y);
        index++;

      }
    }
    return homes;
  }

  static void printComms(RobotController rc) throws GameActionException {
    for (int i = 48; i >= 0; i--) {
      //System.out.println("sector: " + i + ", " + Arrays.toString(readSectorInfo(rc, i)));
    }
  }
}
