package mainBot;

import battlecode.common.*;

import java.awt.*;
import java.sql.SQLOutput;
import java.util.Arrays;

public class Archon extends RobotPlayer {

    static int minersBuilt = 0;
    static int soldiersBuilt = 0;
    static int buildersBuilt = 0;
    static int turnsAlive = 0;
    static int totalIncomeGathered = 0;
    static int turnsNotActioning = 0;
    static int soldiersBuiltInARow = 0;
    static int turnsUntilFirstEnemy = 0;
    static int minersBuiltInARow = 0;
    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runArchon(RobotController rc) throws GameActionException {

        int targetBuilderCount = turnCount/50;
        int currentIncome = rc.readSharedArray(49);
        totalIncomeGathered += currentIncome;
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
        if (enemyCount > 1 && turnsUntilFirstEnemy != 0) {
            turnsUntilFirstEnemy = turnCount;
        }
        double friendlyToEnemyRatio = ((double) soldierCount + wtCount + sageCount)/ (double) enemyCount;
        int targetMinerCount = (int) (.02 * scoutedResources * (1/(1+.02*turnCount)+.7) * friendlyToEnemyRatio * .5);
        //System.out.println(currentIncome);
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
        MapLocation src = rc.getLocation();
        Direction dir = directions[rng.nextInt(directions.length)];
        for (Direction dire : Direction.allDirections()) {
            MapLocation loc = src.add(dire);
            if (rc.onTheMap(loc) && rc.senseRobotAtLocation(loc) == null) {
                dir = dire;
                break;
            }
        }
        System.out.println(turnsUntilFirstEnemy);
        if (minersBuilt < 2 && rc.canBuildRobot(RobotType.MINER, dir)) {
            rc.buildRobot(RobotType.MINER,dir);
            minersBuilt++;
        } else if (soldiersBuilt < 1 && rc.canBuildRobot(RobotType.SOLDIER, dir)) {
            rc.buildRobot(RobotType.SOLDIER,dir);
            soldiersBuilt++;
        } else if ((friendlyToEnemyRatio < 5 || (turnCount % 2*turnsUntilFirstEnemy >
                turnsUntilFirstEnemy && totalIncomeGathered*.0075 > soldierCount)) && rc.canBuildRobot(RobotType.SOLDIER, dir)) {
            rc.buildRobot(RobotType.SOLDIER, dir);
            soldiersBuilt++;
            soldiersBuiltInARow++;
            minersBuiltInARow = 0;
        } else if (rc.canBuildRobot(RobotType.MINER, dir) && targetMinerCount > minerCount) { //&& currentIncome > minerCount * 5
            rc.buildRobot(RobotType.MINER, dir);
            minersBuilt++;
            soldiersBuiltInARow = 0;
            minersBuiltInARow++;
            //Soldiers are built when none of the above conditions are satisfied.
        } else if (targetBuilderCount > buildersBuilt && currentIncome > 30 && rc.canBuildRobot(RobotType.BUILDER, dir)) {
            rc.buildRobot(RobotType.BUILDER, dir);
            buildersBuilt++;
            soldiersBuiltInARow = 0;
            minersBuiltInARow = 0;
        } else if (rc.isActionReady()) {
            turnsNotActioning++;
        }

        //Comms stuff
        Comms.updateSector(rc);
        System.out.println(turnsNotActioning);
        //System.out.println("miners: " + minerCount + ", soldiers:" + soldierCount + ", wt:" + wtCount + ", " + sageCount);
        turnsAlive++;
    }
}
