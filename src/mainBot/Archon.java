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
    static int buildersBuiltInARow = 0;
    static boolean firstEnemySeen = false;
    static int spreadCooldown = 0;
    static int lastTurnMiners = 0;
    static double soldierToMinerRatioAdj = 0;
    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runArchon(RobotController rc) throws GameActionException {

        int maxBuilderCount = (int) (Math.sqrt(rc.getMapHeight() * rc.getMapWidth())/20) * 15;
        int builderCount = rc.readSharedArray(54);
        int currentIncome = rc.readSharedArray(49);
        totalIncomeGathered += currentIncome;
        System.out.println("income: " + currentIncome);
        int minerCount = rc.readSharedArray(50);
        int soldierCount = rc.readSharedArray(51);
        int wtCount = rc.readSharedArray(52);
        int sageCount = rc.readSharedArray(53);
        int enemyCount = 0;
        int scoutedResources = 0;
        int combatSector = 50;
        for (int i = 48; i >= 0; i--) {
            int[] sector = Comms.readSectorInfo(rc, i);
            if (sector[3] > 0) {
                combatSector = i;
            }
            enemyCount += sector[3];
            scoutedResources += sector[2];
        }
        if (scoutedResources > 2000) {
            scoutedResources = 2000;
        }
        int senseRadius = rc.getType().visionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] opponentUnits = rc.senseNearbyRobots(senseRadius, opponent);
        if (opponentUnits.length > 0) {
            for (int i = opponentUnits.length - 1; i >= 0; i--) {
                RobotInfo unit = opponentUnits[i];
                if (unit.getType() == RobotType.SOLDIER) {
                    enemyCount++;
                    break;
                }
            }
        }
        if (enemyCount > 0) {
            firstEnemySeen = true;
        }
        // Building
        Direction center = rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
        Direction dir = center;
        Direction[] directions = new Direction[8];
        for (int i = 7; i>=0;i--) {
            if (i == 7) {
                directions[7-i] = center;
            } else {
                directions[7-i] = directions[7-i - 1].rotateLeft();
            }
        }
        MapLocation src = rc.getLocation();
        for (Direction dire : directions) {
            MapLocation loc = src.add(dire);
            if (rc.onTheMap(loc) && rc.senseRobotAtLocation(loc) == null) {
                dir = dire;
                break;
            }
        }
        boolean shouldBuildSoldier = true;
//        System.out.println(combatSector);
//        System.out.println(rc.getID() + " " + rc.canBuildRobot(RobotType.SOLDIER, dir));
        if (combatSector < 50 && rc.canBuildRobot(RobotType.SOLDIER, dir)) {
            MapLocation combatSectorLoc = Comms.sectorMidpt(rc, combatSector);
            int distToCombatSector = rc.getLocation().distanceSquaredTo(combatSectorLoc);
            if (rc.getTeamLeadAmount(rc.getTeam()) < 150) {
                for (int i = 48; i >= 0; i--) {
                    int[] sector = Comms.readSectorInfo(rc, i);
                    if (!Comms.withinSector(rc, rc.getLocation(), i) && sector[0] == 1 && Comms.sectorMidpt(rc, i).distanceSquaredTo(combatSectorLoc) < distToCombatSector) {
                        shouldBuildSoldier = false;
                    }
                }
            }
        }

        if (soldierCount == 0) {
            soldierCount = 1;
        }
        if (enemyCount == 0) {
            enemyCount = 1;
        }
        double friendlyToEnemyRatio = ((double) soldierCount + wtCount + sageCount)/ (double) enemyCount;
        if (friendlyToEnemyRatio > 5) {
            friendlyToEnemyRatio = 5;
        }
        if (friendlyToEnemyRatio <= 3) {
            soldierToMinerRatioAdj = -3*friendlyToEnemyRatio + 3;
        }
        int targetMinerCount = (int) (.02 * scoutedResources * (1/(1+.02*turnCount)+.15) * friendlyToEnemyRatio * friendlyToEnemyRatio * .5);
//        System.out.println("scouted Resources:" + scoutedResources);
//        System.out.println("feratio:" + friendlyToEnemyRatio);
//        System.out.println("current Income:" + currentIncome);
        Team friendly = rc.getTeam();
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

        if (woundedWarrior != null && rc.canRepair(woundedWarrior) && !rc.canBuildRobot(RobotType.MINER, dir)) {
            rc.repair(woundedWarrior);
        }
        int roundStartLead = rc.getTeamLeadAmount(rc.getTeam());
        int minerDiff = minerCount - lastTurnMiners;
        if (roundStartLead >= (rc.getArchonCount() - minerDiff) * 50) {
            spreadCooldown = 0;
        }
//        System.out.println(rc.getID() + " has " + spreadCooldown);
//        System.out.println(rc.getID() + " has " + minerDiff + " diff");
//        System.out.println(rc.getID() + " has " + minerCount + " miner");
//        System.out.println(friendlyToEnemyRatio);
//        System.out.println(firstEnemySeen);
//        System.out.println(rc.canBuildRobot(RobotType.MINER, dir));
        if (!firstEnemySeen && rc.canBuildRobot(RobotType.MINER, dir)) {
            if (spreadCooldown == 0) {
                rc.buildRobot(RobotType.MINER, dir);
                minersBuilt++;
                spreadCooldown+= rc.getArchonCount() -1;
            }
//            rc.buildRobot(RobotType.MINER, dir);
//            minersBuilt++;
        }
        else if ((targetMinerCount < minerCount || (sageCount + soldierCount) * (1.5 + soldierToMinerRatioAdj) < minerCount) &&
                rc.canBuildRobot(RobotType.SAGE, dir) && !(rc.getTeamLeadAmount(rc.getTeam())>400 && builderCount < maxBuilderCount && buildersBuiltInARow < 1)) {
            rc.buildRobot(RobotType.SAGE, dir);
            minersBuiltInARow = 0;
            buildersBuiltInARow = 0;
        }
        else if ((targetMinerCount < minerCount || (sageCount + soldierCount) * (1.5 + soldierToMinerRatioAdj) < minerCount) &&
                rc.canBuildRobot(RobotType.SOLDIER, dir) && !(rc.getTeamLeadAmount(rc.getTeam())>400 && builderCount < maxBuilderCount && buildersBuiltInARow < 1)) {
            if (shouldBuildSoldier) {
                rc.buildRobot(RobotType.SOLDIER, dir);
                soldiersBuilt++;
                soldiersBuiltInARow++;
                minersBuiltInARow = 0;
                buildersBuiltInARow = 0;
            }
        } else if (soldiersBuilt>0 && rc.canBuildRobot(RobotType.MINER, dir) && (targetMinerCount > minerCount) &&
                !(rc.getTeamLeadAmount(rc.getTeam())>400 && builderCount < maxBuilderCount && buildersBuiltInARow < 1)) { //&& currentIncome > minerCount * 5
            rc.buildRobot(RobotType.MINER, dir);
            minersBuilt++;
            soldiersBuiltInARow = 0;
            minersBuiltInARow++;
            buildersBuiltInARow = 0;
            //Soldiers are built when none of the above conditions are satisfied.
        }
        else if (((soldiersBuilt>0 && currentIncome > 50 && targetMinerCount
                < minerCount && friendlyToEnemyRatio > 2.5) ||
                rc.getTeamLeadAmount(rc.getTeam())>400) && rc.canBuildRobot(RobotType.BUILDER, dir) && builderCount < maxBuilderCount && buildersBuiltInARow < 1) {
            rc.buildRobot(RobotType.BUILDER, dir);
            buildersBuilt++;
            soldiersBuiltInARow = 0;
            minersBuiltInARow = 0;
            buildersBuiltInARow++;
        }
//        else if (soldiersBuilt>1 && rc.canBuildRobot(RobotType.MINER, dir)) { //&& currentIncome > minerCount * 5
//            rc.buildRobot(RobotType.MINER, dir);
//            minersBuilt++;
//            soldiersBuiltInARow = 0;
//            minersBuiltInARow++;
//            //Soldiers are built when none of the above conditions are satisfied.
//        }
        else if (rc.isActionReady()) {
            turnsNotActioning++;
        }

        //Comms stuff
        Comms.updateSector(rc);
//        System.out.println("turnsAFK:" +turnsNotActioning);
        turnsAlive++;
        spreadCooldown -= minerDiff;
        lastTurnMiners = minerCount;
        if (spreadCooldown < 0 || roundStartLead >= (rc.getArchonCount()-minerDiff) * 50) {
            spreadCooldown = 0;
        }
    }
}
