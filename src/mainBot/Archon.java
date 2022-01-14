package mainBot;

import battlecode.common.*;

import java.awt.*;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.Map;

public class Archon extends RobotPlayer {

    static int minersBuilt = 0;
    static int soldiersBuilt = 0;
    static int buildersBuilt = 0;
    static int sagesBuilt = 0;
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
        int addMaxBuilder = rc.getTeamLeadAmount(rc.getTeam())/10000 * 10;
        if (rc.getTeamLeadAmount(rc.getTeam()) < 400) {
            maxBuilderCount = 0;
        }
        maxBuilderCount += addMaxBuilder;
        int builderCount = rc.readSharedArray(54);
        int currentIncome = rc.readSharedArray(49);
        totalIncomeGathered += currentIncome;
        //System.out.println("income: " + currentIncome);
        int minerCount = rc.readSharedArray(50);
        int soldierCount = rc.readSharedArray(51);
        int wtCount = rc.readSharedArray(52);
        int sageCount = rc.readSharedArray(53);
        int enemyCount = 0;
        int scoutedResources = 0;
        int combatSector = (rc.readSharedArray(55) & 0b111111)-1;
        boolean enemyArchonNearby = false;
        int mapArea = rc.getMapHeight() * rc.getMapWidth();

        if (combatSector == -1) {
            combatSector = 50;
        } else {
            enemyCount++;
        }
        int[] friendlyArchonSectors = new int[rc.getArchonCount()];
        for (int i = friendlyArchonSectors.length-1; i >=0; i--) {
            friendlyArchonSectors[i] = 50;
        }

        for (int i = 48; i >= 0; i--) {
            int[] sector = Comms.readSectorInfo(rc, i);
            if (sector[0] == 1) {
                for (int j = friendlyArchonSectors.length-1; j >=0; j--) {
                    if (friendlyArchonSectors[j] == 50) {
                        friendlyArchonSectors[j] = i;
                        break;
                    }
                }
            }
        }

        int closestDistToAnyArchon = Integer.MAX_VALUE;
        for (int i = 48; i >= 0; i--) {
            int[] sector = Comms.readSectorInfo(rc, i);
            if (sector[3] > 0) {
                for (int j = friendlyArchonSectors.length-1; j >=0; j--) {
                    if (Comms.sectorMidpt(rc, friendlyArchonSectors[j]).distanceSquaredTo(Comms.sectorMidpt(rc, i)) < closestDistToAnyArchon) {
                        combatSector = i;
                    }
                }
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
                } else if (unit.getType() == RobotType.ARCHON) {
                    enemyArchonNearby = true;
                }
            }
        }
        if (enemyCount > 0) {
            firstEnemySeen = true;
        }
        // Building
        Direction center = rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
        Direction dir = center;
        if (center == Direction.CENTER) {
            dir = Direction.SOUTH;
        }
        Direction[] directions = new Direction[8];
        for (int i = 7; i>=0;i--) {
            if (i == 7) {
                directions[7-i] = center;
            } else {
                directions[7-i] = directions[7-i - 1].rotateLeft();
            }
        }
        double rubble = 200;
        MapLocation src = rc.getLocation();
        for (Direction dire : directions) {
            MapLocation loc = src.add(dire);
            if (rc.onTheMap(loc) && rc.senseRobotAtLocation(loc) == null) {
                if (rc.senseRubble(loc) < rubble && mapArea > 900) {
                    rubble = rc.senseRubble(loc);
                    dir = dire;
                } else if (mapArea <= 900){
                    dir = dire;
                    break;
                }
            }
        }

        int soldiersCanBuild = rc.getTeamLeadAmount(rc.getTeam()) / 75;
        if (soldiersCanBuild > rc.getArchonCount()) {
            soldiersCanBuild = rc.getArchonCount();
        }

        boolean shouldBuildSoldier = false;
        if (combatSector < 50 && soldiersCanBuild < rc.getArchonCount()) {
            // friendlyArchonSector -> int[] that has the sector num of each friendly archon
            // closest enemy -> int that has the sector num of the closest enemy
            // sort friendlyArchonSector based on distance to closest enemy.
            int[] friendlyArchonSectorsDists = new int[friendlyArchonSectors.length];
            MapLocation combatMdpt = Comms.sectorMidpt(rc, combatSector);
            for (int i = friendlyArchonSectors.length - 1; i >= 0; i--) {
                friendlyArchonSectorsDists[i] = Comms.sectorMidpt(rc, friendlyArchonSectors[i]).distanceSquaredTo(combatMdpt);
            }

            int n = friendlyArchonSectorsDists.length;
            for (int i = 1; i < n; ++i) {
                int key = friendlyArchonSectorsDists[i];
                int j = i - 1;

                /*
                 * Move elements of arr[0..i-1], that are greater than key, to one
                 * position ahead of their current position
                 */
                while (j >= 0 && friendlyArchonSectorsDists[j] > key) {
                    friendlyArchonSectorsDists[j + 1] = friendlyArchonSectorsDists[j];
                    j = j - 1;
                }
                friendlyArchonSectorsDists[j + 1] = key;
            }
            if (soldiersCanBuild > 0 && (soldierCount % 4 < 3 ||  Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt) == 0)) {
                for (int i = soldiersCanBuild - 1; i >= 0; i--) {
                    if (friendlyArchonSectorsDists[(soldiersCanBuild - 1)-i] == Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt)) {
                        shouldBuildSoldier = true;
                    }
                }
            } else if (soldiersCanBuild > 0) {
                for (int i = soldiersCanBuild - 1; i >= 0; i--) {
                    if (friendlyArchonSectorsDists[rc.getArchonCount()-1 - i] == Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt)) {
                        shouldBuildSoldier = true;
                    }
                }
            }
        } else if (combatSector == 50 && soldiersCanBuild < rc.getArchonCount()) {

            // friendlyArchonSector -> int[] that has the sector num of each friendly archon
            // closest enemy -> int that has the sector num of the closest enemy
            // sort friendlyArchonSector based on distance to closest enemy.
            int[] friendlyArchonSectorsDists = new int[friendlyArchonSectors.length];
            MapLocation combatMdpt = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
            for (int i = friendlyArchonSectors.length - 1; i >= 0; i--) {
                friendlyArchonSectorsDists[i] = Comms.sectorMidpt(rc, friendlyArchonSectors[i]).distanceSquaredTo(combatMdpt);
            }

            int n = friendlyArchonSectorsDists.length;
            for (int i = 1; i < n; ++i) {
                int key = friendlyArchonSectorsDists[i];
                int j = i - 1;

                /*
                 * Move elements of arr[0..i-1], that are greater than key, to one
                 * position ahead of their current position
                 */
                while (j >= 0 && friendlyArchonSectorsDists[j] > key) {
                    friendlyArchonSectorsDists[j + 1] = friendlyArchonSectorsDists[j];
                    j = j - 1;
                }
                friendlyArchonSectorsDists[j + 1] = key;
            }
            if (soldiersCanBuild > 0 && (soldierCount % 4 < 3 ||  Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt) == 0)) {
                for (int i = soldiersCanBuild - 1; i >= 0; i--) {
                    if (friendlyArchonSectorsDists[(soldiersCanBuild - 1)-i] == Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt)) {
                        shouldBuildSoldier = true;
                    }
                }
            } else if (soldiersCanBuild > 0) {
                for (int i = soldiersCanBuild - 1; i >= 0; i--) {
                    if (friendlyArchonSectorsDists[rc.getArchonCount()-1 - i] == Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt)) {
                        shouldBuildSoldier = true;
                    }
                }
            }
        }
        else {
            shouldBuildSoldier = true;
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
        int targetMinerCount = (int) (20*(1/(1+.02*(100+turnCount))+.15) * friendlyToEnemyRatio * friendlyToEnemyRatio);
        //System.out.println(targetMinerCount);
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
        spreadCooldown -= minerDiff;
        //System.out.println(combatSector);
        if (spreadCooldown < 0) {
            spreadCooldown = 0;
        }
        if (roundStartLead >= (rc.getArchonCount() - minerDiff) * 50) {
            spreadCooldown = 0;
        }

        MapLocation[] nearbyLead = rc.senseNearbyLocationsWithLead(RobotType.ARCHON.visionRadiusSquared);
        int totalNearbyLead = 0;
        for (int i = nearbyLead.length-1; i >=0 ; i--) {
            totalNearbyLead += rc.senseLead(nearbyLead[i]);
        }
        System.out.println(scoutedResources);
        if (!enemyArchonNearby && (minerCount < 4 || (scoutedResources/2 > minerCount && soldierCount >= 2 && (sageCount + soldierCount) * (1.5 - soldierToMinerRatioAdj) > minerCount))) { //|| (soldiersBuiltInARow > 2 && minerCount < 15)
            if (rc.canBuildRobot(RobotType.MINER, dir)) {
                rc.buildRobot(RobotType.MINER,dir);
                soldiersBuiltInARow = 0;
                rc.writeSharedArray(50, rc.readSharedArray(50) + 1);
            }
        }
        else if (minerCount == 4 && soldierCount < 2) {
            if (shouldBuildSoldier && rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                rc.buildRobot(RobotType.SOLDIER, dir);
                rc.writeSharedArray(51, rc.readSharedArray(51) + 1);
            }
        }
//        else if (!enemyArchonNearby && (soldierCount + 2 >= minerCount && minerCount < 15)) {
//            if (rc.canBuildRobot(RobotType.MINER, dir)) {
//                rc.buildRobot(RobotType.MINER,dir);
//                soldiersBuiltInARow = 0;
//                rc.writeSharedArray(50, rc.readSharedArray(50) + 1);
//            }
//        }
        else if (!enemyArchonNearby && (!firstEnemySeen || (totalNearbyLead > 50 && totalNearbyLead < 100)) && rc.canBuildRobot(RobotType.MINER, dir)) {
            if (spreadCooldown == 0) {
                rc.buildRobot(RobotType.MINER, dir);
                minersBuilt++;
                spreadCooldown+= rc.getArchonCount() -1;
                soldiersBuiltInARow = 0;
                rc.writeSharedArray(50, rc.readSharedArray(50) + 1);
            }
        }
        else if ((targetMinerCount < minerCount || (sageCount + soldierCount) * (1.5 - soldierToMinerRatioAdj) < minerCount) &&
                rc.canBuildRobot(RobotType.SAGE, dir) && !(rc.getTeamLeadAmount(rc.getTeam())>400 && builderCount < maxBuilderCount && buildersBuiltInARow < 1)) {
            rc.buildRobot(RobotType.SAGE, dir);
            minersBuiltInARow = 0;
            buildersBuiltInARow = 0;
            rc.writeSharedArray(51, rc.readSharedArray(51) + 1);
            sagesBuilt++;
        }
        else if (((((targetMinerCount < minerCount || (sageCount + soldierCount) * (1.5 - soldierToMinerRatioAdj) < minerCount)
                && !(rc.getTeamLeadAmount(rc.getTeam())>400 && builderCount < maxBuilderCount && buildersBuiltInARow < 1) )
                || enemyArchonNearby)) && rc.canBuildRobot(RobotType.SOLDIER, dir)) {
            if (shouldBuildSoldier) {
                rc.buildRobot(RobotType.SOLDIER, dir);
                soldiersBuilt++;
                soldiersBuiltInARow++;
                minersBuiltInARow = 0;
                buildersBuiltInARow = 0;
                rc.writeSharedArray(51, rc.readSharedArray(51) + 1);
            }
        } else if (!enemyArchonNearby && soldiersBuilt>0 && rc.canBuildRobot(RobotType.MINER, dir) && (targetMinerCount > minerCount) &&
                !(rc.getTeamLeadAmount(rc.getTeam())>400 && builderCount < maxBuilderCount && buildersBuiltInARow < 1)) { //&& currentIncome > minerCount * 5
            rc.buildRobot(RobotType.MINER, dir);
            minersBuilt++;
            soldiersBuiltInARow = 0;
            minersBuiltInARow++;
            buildersBuiltInARow = 0;
            lastTurnMiners++;
            rc.writeSharedArray(50, rc.readSharedArray(50) + 1);
            //Soldiers are built when none of the above conditions are satisfied.
        }
        else if (!enemyArchonNearby && ((targetMinerCount
                < minerCount && friendlyToEnemyRatio > 3) ||
                rc.getTeamLeadAmount(rc.getTeam())>400) && rc.canBuildRobot(RobotType.BUILDER, dir) && builderCount < maxBuilderCount && buildersBuiltInARow < 1) {
            rc.buildRobot(RobotType.BUILDER, dir);
            buildersBuilt++;
            soldiersBuiltInARow = 0;
            minersBuiltInARow = 0;
            buildersBuiltInARow++;
            rc.writeSharedArray(54, rc.readSharedArray(54) + 1);
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

        lastTurnMiners = rc.readSharedArray(50);
        //Comms stuff
        Comms.updateSector(rc, turnCount);
//        System.out.println("turnsAFK:" +turnsNotActioning);

        turnsAlive++;

        if (roundStartLead >= (rc.getArchonCount()-minerDiff) * 50) {
            spreadCooldown = 0;
        }
    }
}
