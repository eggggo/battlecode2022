package stratBot;

import battlecode.common.*;

import java.sql.SQLOutput;
import java.util.Arrays;

public class Archon extends RobotPlayer {

  static int minersBuilt = 0;
  static int soldiersBuilt = 0;
  static int buildersBuilt = 0;
  static int turnsAlive = 0;

  /**
   * Run a single turn for an Archon.
   * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
   */
  static void runArchon(RobotController rc) throws GameActionException {
    MapLocation centerMap = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
    Team opponent = rc.getTeam().opponent();
    //Printing anomaly schedule
    AnomalyScheduleEntry[] anomalySchedule = rc.getAnomalySchedule();
    for (int i = anomalySchedule.length - 1; i >= 0; i--) {
      //   System.out.println(anomalySchedule[anomalySchedule.length-1-i].anomalyType.toString());
    }

    //Countin nearby soldier and miner counts.
    int nearbySoldiers = 0;
    int nearbyMiners = 0;
    int senseRadius = rc.getType().visionRadiusSquared;
    Team friendly = rc.getTeam();
    RobotInfo[] alliedUnits = rc.senseNearbyRobots(senseRadius, friendly);
    for (int i = alliedUnits.length - 1; i >= 0; i--) {
      if (alliedUnits[i].getType() == RobotType.SOLDIER) {
        nearbySoldiers++;
      } else if (alliedUnits[i].getType() == RobotType.MINER) {
        nearbyMiners++;
      }
    }
    RobotInfo[] opponentUnits = rc.senseNearbyRobots(senseRadius, opponent);
    Direction dir = directions[rng.nextInt(directions.length)];
    if (opponentUnits.length > 0 && rc.canBuildRobot(RobotType.SOLDIER, dir)) {

      rc.buildRobot(RobotType.SOLDIER, dir);
      soldiersBuilt++;
    }
    //if theres a wounded soldier nearby, repair it
    MapLocation woundedWarrior = null;
    for (int i = alliedUnits.length - 1; i >= 0; i--) {
      RobotInfo unit = alliedUnits[i];
      if (unit.getType() == RobotType.SOLDIER && unit.getHealth() != RobotType.SOLDIER.getMaxHealth(unit.getLevel())) {
        woundedWarrior = unit.getLocation();
        break;
      }
    }

    if (woundedWarrior != null && rc.canRepair(woundedWarrior)) {
      rc.repair(woundedWarrior);
    }


    // Building
    dir = directions[rng.nextInt(directions.length)];

    //Bulders are built with an about 1:20 ratio to the number of total units owned
    if (rc.getRobotCount() > (buildersBuilt + 1) * 30 && buildersBuilt < 5 && nearbySoldiers > 3 && opponentUnits.length == 0) {
      if (rc.canBuildRobot(RobotType.BUILDER, dir)) {
        rc.buildRobot(RobotType.BUILDER, dir);
        buildersBuilt++;
      }
      //miners are built when the neaby Miner count is less than 1.5 times the nearby Soldier count
    } else if (rc.canBuildRobot(RobotType.MINER, dir) && minersBuilt < -130 + 64 * Math.log(rc.getRoundNum()) && opponentUnits.length == 0) {
      rc.buildRobot(RobotType.MINER, dir);
      minersBuilt++;

      //Soldiers are built when none of the above conditions are satisfied.
    } else if (rc.canBuildRobot(RobotType.SOLDIER, dir) && rc.getRoundNum() > 100) {
      rc.buildRobot(RobotType.SOLDIER, dir);
      soldiersBuilt++;
    }

    //Encoding location of friendly archons in the last 4 indicies of our comms array.
    Comms.writeToCommArray(rc, 0, rc.getLocation().x, rc.getLocation().y);

    turnsAlive++;
  }
}
