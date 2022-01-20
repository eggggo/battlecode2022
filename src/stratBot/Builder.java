package stratBot;

import battlecode.common.*;

public class Builder extends RobotPlayer {

  static int laboratoriesBuilt = 0;
  static int watchtowersBuilt = 0;
  static int turnsAlive = 0;
  static boolean aboveHpThresh = true;
  static MapLocation home = null;
  //Role: Attack Builders 0, Laboratory Builder 1
  static int role;


  public static void runBuilder(RobotController rc) throws GameActionException {
    int currentIncome = rc.readSharedArray(49);
    int senseRadius = rc.getType().visionRadiusSquared;
    Team friendly = rc.getTeam();
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(senseRadius, friendly);
    RobotInfo[] enemies = rc.senseNearbyRobots(senseRadius, rc.getTeam().opponent());
    if (turnsAlive == 0) {
      MapLocation[] homes = Comms.readHomeArchonLocations(rc);
      int minDistance = 60;
      for (int i = homes.length - 1; i >= 0; i--) {
        if (homes[i] == null) {
          continue;
        }
        int d = homes[i].distanceSquaredTo(rc.getLocation());
        if (d < minDistance) {
          minDistance = d;
          home = homes[i];
        }
      }
      if (home == null) {
        home = rc.getLocation();
      }
      if (enemies.length > 4 || rc.getRoundNum() < 750) {
        role = 0;
      } else {
        role = rc.getID() % 2;
      }

    }
    //If can repair repair
    //If see repairable walk
    //if role is attack do attack, if role is lab build lab

    Direction dir = Direction.CENTER;
    MapLocation me = rc.getLocation();
    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        MapLocation repairLocation = new MapLocation(me.x + dx, me.y + dy);
        // Notice that the Miner's action cooldown is very low.
        // You can mine multiple times per turn!
        while (rc.canRepair(repairLocation)) {
          rc.repair(repairLocation);
        }

      }
    }

    for (int i = nearbyRobots.length - 1; i >= 0; i--) {
      RobotType rT = nearbyRobots[i].getType();
      int rH = nearbyRobots[i].getHealth();
      int lvl = nearbyRobots[i].getLevel();
      int maxH = rT.getMaxHealth(lvl);
      if ((rT == RobotType.WATCHTOWER || rT == RobotType.LABORATORY) && rH != maxH) {
        dir = Pathfinder.getMoveDir(rc, nearbyRobots[i].getLocation());
      }


    }
    if (dir != Direction.CENTER) {
      if (enemies.length > 4) {
        role = 0;
      }
      if (role == 0) {
        int nearbyTowers = 0;
        for (int i = nearbyRobots.length - 1; i >= 0; i--) {
          if (nearbyRobots[i].getType() == RobotType.WATCHTOWER) {
            nearbyTowers++;
          }
        }
        Direction opposite = rc.getLocation().directionTo(home).opposite();
        if (nearbyTowers >= 8) {
          dir = opposite;
        } else {
          if (rc.canBuildRobot(RobotType.WATCHTOWER, opposite)) {
            rc.buildRobot(RobotType.WATCHTOWER, opposite);
          } else {
            dir = opposite;
          }
        }

      } else if (role == 1) {
        int nearbyLabs = 0;
        for (int i = nearbyRobots.length - 1; i >= 0; i--) {
          if (nearbyRobots[i].getType() == RobotType.LABORATORY) {
            nearbyLabs++;
          }
        }
        MapLocation wall = closestWall(rc);
        if ((wall == null || wall == me) && nearbyLabs < 3) {
          for (int i = directions.length; i >= 0; i--) {
            if (rc.canBuildRobot(RobotType.LABORATORY, directions[i])) {
              rc.buildRobot(RobotType.LABORATORY, directions[i]);
            }
          }
        } else {
          role = 0;
        }
      }
    }


    if (rc.canMove(dir)) {
      rc.move(dir);
    }

    //Comms stuff
    Comms.updateSector(rc);

    if (turnsAlive == 0) {
      rc.writeSharedArray(54, rc.readSharedArray(54) + 1);
    }
    boolean currentHpThresh = (double) rc.getHealth() / rc.getType().getMaxHealth(1) > 0.2;
    if (!currentHpThresh && aboveHpThresh) {
      rc.writeSharedArray(54, rc.readSharedArray(54) - 1);
    } else if (currentHpThresh && !aboveHpThresh) {
      rc.writeSharedArray(54, rc.readSharedArray(54) + 1);
    }
    aboveHpThresh = currentHpThresh;
    turnsAlive++;
  }

  static MapLocation closestWall(RobotController rc) {
    MapLocation me = rc.getLocation();
    int topRow = rc.getMapHeight() - 2;
    int ritCol = rc.getMapWidth() - 2;
    int botRow = 1;
    int lefCol = 1;
    int minDist = 60;
    MapLocation soln = null;
    //top
    int distTop = me.distanceSquaredTo(new MapLocation(me.x, topRow));
    if (me.y != topRow && distTop < minDist) {
      minDist = distTop;
      soln = new MapLocation(me.x, topRow);
    }

    int distBot = me.distanceSquaredTo(new MapLocation(me.x, botRow));
    if (me.y != botRow && distBot < minDist) {
      minDist = distBot;
      soln = new MapLocation(me.x, botRow);
    }
    int distRit = me.distanceSquaredTo(new MapLocation(ritCol, me.y));
    if (me.x != ritCol && distRit < minDist) {
      minDist = distRit;
      soln = new MapLocation(ritCol, me.y);
    }

    int distLef = me.distanceSquaredTo(new MapLocation(lefCol, me.y));
    if (me.x != lefCol && distLef < minDist) {
      minDist = distLef;
      soln = new MapLocation(lefCol, me.y);
    }

    return soln;
  }
}
