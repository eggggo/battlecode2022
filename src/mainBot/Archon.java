package mainBot;

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

        double friendlyToEnemyRatio = 0.0;
        int scoutedResources = 0;
        int targetMinerCount = (int) (1/50 * scoutedResources * (1/(1+.02*turnCount)+.7) * friendlyToEnemyRatio);
        int minerCount = rc.readSharedArray(50);
        int soldierCount = rc.readSharedArray(51);
        int wtCount = rc.readSharedArray(52);
        int sageCount = rc.readSharedArray(53);

        int senseRadius = rc.getType().visionRadiusSquared;
        Team friendly = rc.getTeam();
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] opponentUnits = rc.senseNearbyRobots(senseRadius, opponent);
        RobotInfo[] alliedUnits = rc.senseNearbyRobots(senseRadius, friendly);

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
        Direction dir = directions[rng.nextInt(directions.length)];

//        if (turnCount % 500 == 0 && rc.canBuildRobot(RobotType.BUILDER, dir)) {
//            rc.buildRobot(RobotType.BUILDER, dir);
//        } else if (rc.canBuildRobot(RobotType.MINER, dir) && targetMinerCount < minerCount) { //&& currentIncome > minerCount * 5
//            rc.buildRobot(RobotType.MINER, dir);
//            minersBuilt++;
//            //Soldiers are built when none of the above conditions are satisfied.
//        } else if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
//            rc.buildRobot(RobotType.SOLDIER, dir);
//            soldiersBuilt++;
//        }

        //Bulders are built with an about 1:20 ratio to the number of total units owned
        if (rc.canBuildRobot(RobotType.MINER, dir) && minersBuilt < -130 + 64 * Math.log(rc.getRoundNum()) && opponentUnits.length == 0) {
            rc.buildRobot(RobotType.MINER, dir);
            minersBuilt++;

            //Soldiers are built when none of the above conditions are satisfied.
        } else if (rc.canBuildRobot(RobotType.SOLDIER, dir) && rc.getRoundNum() > 100) {
            rc.buildRobot(RobotType.SOLDIER, dir);
            soldiersBuilt++;
        }

        //Comms stuff
        Comms.updateSector(rc);
        //int currentIncome = rc.readSharedArray(49);
        //System.out.println("income: " + currentIncome);

        System.out.println("miners: " + minerCount + ", soldiers:" + soldierCount + ", wt:" + wtCount + ", " + sageCount);
        turnsAlive++;
    }
}
