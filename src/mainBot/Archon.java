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
    static int unitsAfterEnemySeen = 0;

    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runArchon(RobotController rc) throws GameActionException {

        int maxBuilderCount = (int) (Math.sqrt(rc.getMapHeight() * rc.getMapWidth())/20) * 15;
        int addMaxBuilder = rc.getTeamLeadAmount(rc.getTeam())/10000 * 10;
        maxBuilderCount += addMaxBuilder;
        int builderCount = rc.readSharedArray(54);
        int currentIncome = rc.readSharedArray(49);
        totalIncomeGathered += currentIncome;
        int minerCount = rc.readSharedArray(50);
        int soldierCount = rc.readSharedArray(51);
        int wtCount = rc.readSharedArray(52);
        int sageCount = rc.readSharedArray(53);
        int enemyCount = 0;
        int scoutedResources = 0;
        int combatSector = (rc.readSharedArray(55) & 0b111111)-1;
        boolean enemyArchonNearby = false;
        int mapArea = rc.getMapHeight() * rc.getMapWidth();
        boolean archonSpotted = false;
        int leadNearArchons = 0;
        MapLocation[] nearbyLead = rc.senseNearbyLocationsWithLead(RobotType.ARCHON.visionRadiusSquared);
        //Initialize combat sector.  If we see an enemy, enemyCount++
        if (combatSector == -1) {
            combatSector = 50;
        } else {
            enemyCount++;
        }

        //Set all the default friendlyArchonSectors to 50 for initialization
        int[] friendlyArchonSectors = new int[rc.getArchonCount()];
        for (int i = friendlyArchonSectors.length-1; i >=0; i--) {
            friendlyArchonSectors[i] = 50;
        }

        //Find all nearby friendlyArchonSectors
        for (int i = 48; i >= 0; i--) {
            int[] sector = Comms.readSectorInfo(rc, i);
            if (sector[0] == 1) {
                for (int j = friendlyArchonSectors.length-1; j >=0; j--) {
                    if (friendlyArchonSectors[j] == 50) {
                        friendlyArchonSectors[j] = i;
                        break;
                    }
                }
                leadNearArchons += sector[2];
            }
        }

        //Find the closest enemy to any friendlySector
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
            if (sector[1] != 0) {
                archonSpotted = true;
            }
            enemyCount += sector[3];
            scoutedResources += sector[2];
        }

        //Scan for nearby Soldiers and Archons of the enemy
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

        //Initialize firstEnemySeen as true if enemyCount > 0
        if (enemyCount > 0) {
            firstEnemySeen = true;
        }

        // Setting the building direction dir
        Direction center = rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
        Direction dir = center;
        // If the direction to the center of the map is Direction.Center(means you're in the center), set your default
        //direction to south, otherwise keep it center.
        if (center == Direction.CENTER) {
            dir = Direction.SOUTH;
        }
        //Populate directions with all possible directions starting from direction to center rotating left
        Direction[] directions = new Direction[8];
        for (int i = 7; i>=0;i--) {
            if (i == 7) {
                directions[7-i] = center;
            } else {
                directions[7-i] = directions[7-i - 1].rotateLeft();
            }
        }
        int nearestLead = Integer.MAX_VALUE;
        MapLocation nearestPatch = null;
        for (int i = nearbyLead.length-1; i>=0; i--) {
            int dist = nearbyLead[i].distanceSquaredTo(rc.getLocation());
            if (dist < nearestLead && rc.senseLead(nearbyLead[i]) > 10) {
                nearestLead = dist;
                nearestPatch = nearbyLead[i];
            }
        }

        //Accounting for rubble when creating a unit.  Don't care about rubble if mapArea > 900
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

        if (nearestPatch != null && rc.onTheMap(nearestPatch) && rc.senseRobotAtLocation(nearestPatch) == null) {
            dir = rc.getLocation().directionTo(nearestPatch);
        }

        //The number of soldiers you can build is equivalent to the amount of lead you have/75
        int soldiersCanBuild = rc.getTeamLeadAmount(rc.getTeam()) / 75;
        if (soldiersCanBuild > rc.getArchonCount()) {
            soldiersCanBuild = rc.getArchonCount();
        }

        //Initiallizing shouldBuildSoldier as false.  The following code ensures that the closest archon to combat
        // produces soldiers.  The exception is that every 4 soldiers, the farthest archon to combat creates a soldier
        // to potentially mix it up and create other fronts of attack.
        boolean shouldBuildSoldier = false;
        if (enemyArchonNearby) {
            shouldBuildSoldier = true;
        } else if (combatSector < 50 && soldiersCanBuild < rc.getArchonCount()) {

            //initialize friendlyArchonSectorsDists which is the distance of each of the archons to the closest enemy
            // sector.
            int[] friendlyArchonSectorsDists = new int[friendlyArchonSectors.length];
            MapLocation combatMdpt = Comms.sectorMidpt(rc, combatSector);
            for (int i = friendlyArchonSectors.length - 1; i >= 0; i--) {
                friendlyArchonSectorsDists[i] = Comms.sectorMidpt(rc, friendlyArchonSectors[i]).distanceSquaredTo(combatMdpt);
            }

            //sorting algorithm
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

            //If the archon can build and it has enough lead and has priority over other archons because it is close to the nearest enemy enable it to build a soldier.
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
        //if we are producing before we see an enemy, the closest archon to the center builds.
        else if (combatSector == 50 && soldiersCanBuild < rc.getArchonCount()) {


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
        //if you can build 4 soldiers, shouldBuildSoldier should be true.
        else {
            shouldBuildSoldier = true;
        }

        double friendlyToEnemyRatio = ((double) 1+soldierCount + wtCount + sageCount)/ (double) (1+enemyCount);
        //cap friendlyToEnemyRatio at 5 for building alg purposes.
        if (friendlyToEnemyRatio > 5) {
            friendlyToEnemyRatio = 5;
        }
        //This adjusts the soldier to miner ratio based on the friendlyToEnemy ratio
        if (friendlyToEnemyRatio <= 3) {
            soldierToMinerRatioAdj = -3*friendlyToEnemyRatio + 3;
        }
        //formula to calculate the current target miner count.
        int targetMinerCount = (int) (20*(1/(1+.02*(100+turnCount))+.15) * friendlyToEnemyRatio + scoutedResources - currentIncome);
        if (archonSpotted) {
            targetMinerCount -= 5;
        }
        if (archonSpotted && turnsAlive < 20) {
            targetMinerCount = 0;
        }
        //System.out.println("targetMinerCount " + targetMinerCount);
        //System.out.println("income: " + currentIncome);
        //Repair Logic
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

        //If we can't build a miner and we can repair a soldier, do it.
        if (woundedWarrior != null && rc.canRepair(woundedWarrior) && !rc.canBuildRobot(RobotType.MINER, dir)) {
            rc.repair(woundedWarrior);
        }

        // Logic for miner production spread.
        int roundStartLead = rc.getTeamLeadAmount(rc.getTeam());
        int minerDiff = minerCount - lastTurnMiners;
        spreadCooldown -= minerDiff;
        if (spreadCooldown < 0) {
            spreadCooldown = 0;
        }
        if (roundStartLead >= (rc.getArchonCount() - minerDiff) * 50) {
            spreadCooldown = 0;
        }

        //Sensing total lead nearby to signal miner building.
        int totalNearbyLead = 0;
        for (int i = nearbyLead.length-1; i >=0 ; i--) {
            totalNearbyLead += rc.senseLead(nearbyLead[i]);
        }

        //UNIT BUILDING:
        boolean shouldBuildBuilder = rc.getTeamLeadAmount(rc.getTeam())>220 && builderCount < maxBuilderCount && buildersBuiltInARow < 1;
        boolean shouldBuildMiner = targetMinerCount > minerCount;
        boolean shouldBuildSoldierConds = (sageCount + soldierCount) * (1.5 - soldierToMinerRatioAdj) < minerCount && leadNearArchons < 75;

        int initialMiners = 3;
        if (mapArea <= 900) {
            initialMiners = 2;
        }

        //If there is no enemyArchonNearby and the first enemy hasn't been seen or there is nearby lead between 50 and 100, build a miner
        if (mapArea <=900 && rc.getArchonCount() == 1) {
            if (soldierCount < minerCount) {
                if (rc.canBuildRobot(RobotType.SOLDIER,dir)) {
                    rc.buildRobot(RobotType.SOLDIER, dir);
                    soldiersBuilt++;
                    rc.writeSharedArray(51, rc.readSharedArray(51) + 1);
                    unitsAfterEnemySeen++;
                }
            } else if (rc.canBuildRobot(RobotType.MINER, dir)) {
                rc.buildRobot(RobotType.MINER, dir);
                minersBuilt++;
                rc.writeSharedArray(50, rc.readSharedArray(50) + 1);
                unitsAfterEnemySeen++;
            }
        }
        else if (!enemyArchonNearby && firstEnemySeen && soldierCount < minerCount) {
            if (unitsAfterEnemySeen % 3 == 2) {
                if (shouldBuildSoldier && rc.canBuildRobot(RobotType.MINER, dir)) {
                    rc.buildRobot(RobotType.MINER, dir);
                    minersBuilt++;
                    rc.writeSharedArray(50, rc.readSharedArray(50) + 1);
                    unitsAfterEnemySeen++;
                }
            } else {
                if (shouldBuildSoldier && rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                    rc.buildRobot(RobotType.SOLDIER, dir);
                    soldiersBuilt++;
                    rc.writeSharedArray(51, rc.readSharedArray(51) + 1);
                    unitsAfterEnemySeen++;
                }
            }
        }
        else if (!enemyArchonNearby && minersBuilt < initialMiners) {
            if (rc.canBuildRobot(RobotType.MINER,dir)) {
                rc.buildRobot(RobotType.MINER, dir);
                minersBuilt++;
                rc.writeSharedArray(50, rc.readSharedArray(50) + 1);
            }
        }
        else if (!enemyArchonNearby && soldierCount < rc.getArchonCount()) {
            if (rc.canBuildRobot(RobotType.SOLDIER,dir) && soldiersBuilt < 1) {
                rc.buildRobot(RobotType.SOLDIER, dir);
                soldiersBuilt++;
                rc.writeSharedArray(51, rc.readSharedArray(51) + 1);
            }
        }
        else if (!enemyArchonNearby && (!firstEnemySeen || (totalNearbyLead > 50 && totalNearbyLead < 100)) && rc.canBuildRobot(RobotType.MINER, dir)) {
            if (spreadCooldown == 0) {
                rc.buildRobot(RobotType.MINER, dir);
                minersBuilt++;
                spreadCooldown+= rc.getArchonCount() -1;
                soldiersBuiltInARow = 0;
                rc.writeSharedArray(50, rc.readSharedArray(50) + 1);
            }
        }
        //If our minerCount is less than the target or our miner count is greater than our attacker count times a ratio and we shouldn't build a builder or theres an enemyArchonNearby, build a soldier.
        else if (rc.canBuildRobot(RobotType.SAGE, dir) &&
                (((!shouldBuildMiner || shouldBuildSoldierConds) && !(shouldBuildBuilder)) || enemyArchonNearby)) {
            rc.buildRobot(RobotType.SAGE, dir);
            minersBuiltInARow = 0;
            buildersBuiltInARow = 0;
            rc.writeSharedArray(51, rc.readSharedArray(51) + 1);
            sagesBuilt++;
        }
        //If our minerCount is less than the target or our miner count is greater than our attacker count times a ratio and we shouldn't build a builder or theres an enemyArchonNearby, build a soldier.
        else if (rc.canBuildRobot(RobotType.SOLDIER, dir) &&
                (((!shouldBuildMiner || shouldBuildSoldierConds) && !(shouldBuildBuilder)) || enemyArchonNearby)) {
            if (shouldBuildSoldier) {
                rc.buildRobot(RobotType.SOLDIER, dir);
                soldiersBuilt++;
                soldiersBuiltInARow++;
                minersBuiltInARow = 0;
                buildersBuiltInARow = 0;
                rc.writeSharedArray(51, rc.readSharedArray(51) + 1);
            }
        }
        //If we can build a miner and our minerCount is less than the target and we shouldn't build a builder and theres no enemyArchonNearby
        else if (rc.canBuildRobot(RobotType.MINER, dir) &&
                !enemyArchonNearby && shouldBuildMiner && !(shouldBuildBuilder)) {
            rc.buildRobot(RobotType.MINER, dir);
            minersBuilt++;
            soldiersBuiltInARow = 0;
            minersBuiltInARow++;
            buildersBuiltInARow = 0;
            lastTurnMiners++;
            rc.writeSharedArray(50, rc.readSharedArray(50) + 1);
            //Soldiers are built when none of the above conditions are satisfied.
        }
        //If there is no enemyArchonNearby and we should build a builder or our minerCount is greater than our target and friendlyToEnemy is greater than 3 build a builder.
        else if (rc.canBuildRobot(RobotType.BUILDER, dir) &&
                !enemyArchonNearby && shouldBuildBuilder) {
            rc.buildRobot(RobotType.BUILDER, dir);
            buildersBuilt++;
            soldiersBuiltInARow = 0;
            minersBuiltInARow = 0;
            buildersBuiltInARow++;
            rc.writeSharedArray(54, rc.readSharedArray(54) + 1);
        }
        else if (rc.isActionReady()) {
            turnsNotActioning++;
        }

        //Updating Stuff
        lastTurnMiners = rc.readSharedArray(50);
        Comms.updateSector(rc, turnCount);
        turnsAlive++;

        if (roundStartLead >= (rc.getArchonCount()-minerDiff) * 50) {
            spreadCooldown = 0;
        }


    }
}
