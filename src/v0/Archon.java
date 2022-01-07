package v0;

import battlecode.common.*;

import java.awt.*;
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

        int targetBuilderCount = turnCount/50;
        int currentIncome = rc.readSharedArray(49);
        int minerCount = rc.readSharedArray(50);
        int soldierCount = rc.readSharedArray(51);
        int wtCount = rc.readSharedArray(52);
        int sageCount = rc.readSharedArray(53);
        int enemyCount = 1;
        int scoutedResources = 0;
        for (int i = 48; i >= 0; i--) {
            int[] sector = Comms.readSectorInfo(rc, i);
            enemyCount += sector[3];
            scoutedResources += sector[2];
        }
        double friendlyToEnemyRatio = ((double) soldierCount + wtCount + sageCount)/ (double) enemyCount;
        int targetMinerCount = (int) (.02 * scoutedResources * (1/(1+.02*turnCount)+.7) * friendlyToEnemyRatio*.5);
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

        if (minersBuilt < 2 && rc.canBuildRobot(RobotType.MINER, dir)) {
            rc.buildRobot(RobotType.MINER,dir);
            minersBuilt++;
        } else if (friendlyToEnemyRatio < 5 && rc.canBuildRobot(RobotType.SOLDIER, dir)) {
            rc.buildRobot(RobotType.SOLDIER, dir);
            soldiersBuilt++;
        } else if (targetBuilderCount > buildersBuilt && currentIncome > 30 && rc.canBuildRobot(RobotType.BUILDER, dir)) {
            rc.buildRobot(RobotType.BUILDER, dir);
            buildersBuilt++;
        } else if (rc.canBuildRobot(RobotType.MINER, dir) && targetMinerCount > minerCount) { //&& currentIncome > minerCount * 5
            rc.buildRobot(RobotType.MINER, dir);
            minersBuilt++;
            //Soldiers are built when none of the above conditions are satisfied.
        } else if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
            rc.buildRobot(RobotType.SOLDIER, dir);
            soldiersBuilt++;
        }

        //Comms stuff
        Comms.updateSector(rc);

        //System.out.println("miners: " + minerCount + ", soldiers:" + soldierCount + ", wt:" + wtCount + ", " + sageCount);
        turnsAlive++;
    }
}
