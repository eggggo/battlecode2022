package mainBot;

import battlecode.common.*;

import java.awt.*;

public class Archon extends RobotPlayer {

    static int minersBuilt = 0;
    static int soldiersBuilt = 0;
    static int buildersBuilt = 0;
    static int sagesBuilt = 0;
    static int turnsAlive = 0;
    static int turnsNotActioning = 0;
    static int soldiersBuiltInARow = 0;
    static int minersBuiltInARow = 0;
    static int buildersBuiltInARow = 0;
    static boolean firstEnemySeen = false;
    static int spreadCooldown = 0;
    static int lastTurnMiners = 0;
    static int unitsAfterEnemySeen = 0;
    static int mapThres = 900;
    static int startNumArchons = 0;
    static int previousSector = 50;
    static boolean builtBuilderRecently = false;
    static MapLocation bestTgtSector = null;
    static MapLocation closestFriendlyArchon = null;
    static MapLocation[] sectorMdpts = new MapLocation[49];
    static MapLocation[] prev5Spots = new MapLocation[5];
    static int currentOverrideIndex = 0;
    static int turnsOutOfRange = 31;
    static int transitionTurn = 0;

    static Direction stallOnGoodRubble(RobotController rc) throws GameActionException {
        MapLocation src = rc.getLocation();
        int currRubble = rc.senseRubble(src);
        int minRubble = currRubble;
        MapLocation minRubbleLoc = src;
        if (currRubble > 0) {
            for (Direction d : Direction.allDirections()) {
                MapLocation test = src.add(d);
                if (rc.onTheMap(test) && !rc.isLocationOccupied(test) && rc.canSenseLocation(test)) {
                    int rubbleHere = rc.senseRubble(test);
                    if (rubbleHere < minRubble) {
                        minRubble = rubbleHere;
                        minRubbleLoc = test;
                    }
                }
            }
        }
        return src.directionTo(minRubbleLoc);
    }

    static int getQuadrant(RobotController rc, int x, int y) {
        int quad = 0;
        if (x >= rc.getMapWidth() / 2 && y >= rc.getMapHeight() / 2) {
          quad = 1;
        } else if (x >= rc.getMapWidth() / 2 && y < rc.getMapHeight() / 2) {
          quad = 4;
        } else if (x < rc.getMapWidth() / 2 && y >= rc.getMapHeight() / 2) {
          quad = 2;
        } else if (x < rc.getMapWidth() / 2 && y < rc.getMapHeight() / 2) {
          quad = 3;
        } else {
          System.out.println("Error: not in a quadrant");
        }
        return quad;
      }

    static void updateGuesses(RobotController rc) throws GameActionException {
        int archonCount = 4;
        //if all of the archons have written to the comms
        boolean quad1 = false;
        boolean quad2 = false;
        boolean quad3 = false;
        boolean quad4 = false;
    
        //Create an array for the quads each archon is contained in and another 2D array for each of the archons' coords
        int currentArchonIndex = 0;
        int[] quads = new int[archonCount];
        MapLocation[] coords = new MapLocation[archonCount];
        for (int i = 48; i >= 0; i--) {
            MapLocation mdpt = Comms.sectorMidpt(rc, i);
            if (Comms.readSectorInfo(rc, i, 0) == 1) {
            quads[currentArchonIndex] = getQuadrant(rc, mdpt.x, mdpt.y);
            coords[currentArchonIndex] = mdpt;
            currentArchonIndex++;
            }
        }
        archonCount = currentArchonIndex;
        int[] tempQuads = new int[currentArchonIndex];
        MapLocation[] tempCoords = new MapLocation[currentArchonIndex];
        for (int i = currentArchonIndex - 1; i >= 0; i --) {
            tempQuads[i] = quads[i];
            tempCoords[i] = coords[i];
        }
        quads = tempQuads;
        coords = tempCoords;
    
        MapLocation[] enemyArchons = new MapLocation[archonCount * 3];
    
        //initialize whether there's a friendly archon in each quad
        for (int a = archonCount - 1; a >= 0; a--) {
            if (quads[a] == 1) {
            quad1 = true;
            } else if (quads[a] == 2) {
            quad2 = true;
            } else if (quads[a] == 3) {
            quad3 = true;
            } else if (quads[a] == 4) {
            quad4 = true;
            }
        }
    
        //Predict location of enemy archons based on potential symmetry.  For each archon, I check the 180 rotation,
        // horizontal flip, and vertical flip.  These are only potential locations, as roataion can occur at other degrees of rotation.
        if (quad1 && quad3 || quad2 && quad4) {
            for (int i = archonCount - 1; i >= 0; i--) {
            enemyArchons[3 * i] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, rc.getMapHeight() - 1 - coords[i].y); // 180 rotate
            }
    
        } else if (quad1 && quad2 || quad3 && quad4 || rc.getMapWidth()*2.5 < rc.getMapHeight()) {
            //thought that it was unecessary to check horizontal when its likely horizontally symmetric, so i just copied another vertical fip instead of using horz
            for (int i = archonCount - 1; i >= 0; i--) {
            enemyArchons[3 * i] = new MapLocation(coords[i].x, rc.getMapHeight() - 1 - coords[i].y); //vert flip
            enemyArchons[3 * i + 2] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, rc.getMapHeight() - 1 - coords[i].y); // 180 rotate
            //if map width times 2.5 is less than the height, its pretty likely to be vertical flip coords
            if (rc.getMapWidth()*2.5 < rc.getMapHeight()) {
                enemyArchons[3 * i + 2] = null;
            }
            }
        } else if (quad1 && quad4 || quad2 && quad3 || rc.getMapHeight()*2.5 < rc.getMapWidth()) {
            //thought that it was unecessary to check vertical when its likely vertically symmetric, so i just copied another horiz fip instead of using vert
            for (int i = archonCount - 1; i >= 0; i--) {
            enemyArchons[3 * i] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, coords[i].y); //horz flip
            enemyArchons[3 * i + 2] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, rc.getMapHeight() - 1 - coords[i].y); // 180 rotate
            //if map height times 2.5 is less than the width, its pretty likely to be horizontal flip coords
            if (rc.getMapHeight()*2.5 < rc.getMapWidth()) {
                enemyArchons[3 * i + 2] = null;
            }
            }
        } else {
            for (int i = archonCount - 1; i >= 0; i--) {
            enemyArchons[3 * i] = new MapLocation(coords[i].x, rc.getMapHeight() - 1 - coords[i].y); //vert flip
            enemyArchons[3 * i + 1] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, coords[i].y); // horz flip
            enemyArchons[3 * i + 2] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, rc.getMapHeight() - 1 - coords[i].y); // 180 rotate
            }
        }

        for (int i = enemyArchons.length - 1; i >= 0; i --) {
            MapLocation archon = enemyArchons[i];
            if (archon != null) {
                int sector = Comms.locationToSector(rc, archon);
                rc.writeSharedArray(sector, rc.readSharedArray(sector) | 0b0100000000000000);
            }
        }
        rc.writeSharedArray(55, rc.readSharedArray(55) | 0b1000000);
    }

    static boolean isHostile(RobotInfo enemy) {
        return (enemy.getType() == RobotType.SOLDIER || enemy.getType() == RobotType.SAGE 
        || enemy.getType() == RobotType.WATCHTOWER);
    }

    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runArchon(RobotController rc) throws GameActionException {

        int builderCount = rc.readSharedArray(54);
        int minerCount = rc.readSharedArray(50);
        int soldierCount = rc.readSharedArray(51);
        int wtCount = rc.readSharedArray(52);
        int sageCount = rc.readSharedArray(53);
        int combatSector = (rc.readSharedArray(55) & 0b111111)-1;
        int mapArea = rc.getMapHeight() * rc.getMapWidth();
        MapLocation[] nearbyLead = rc.senseNearbyLocationsWithLead(RobotType.ARCHON.visionRadiusSquared);
        int rubbleThreshold = rc.senseRubble(rc.getLocation()) + 20;
        MapLocation src = rc.getLocation();
        int actionRadius = rc.getType().actionRadiusSquared;
        if (turnCount % 2 == 1 && builderCount % rc.getArchonCount() == 0) {
            builtBuilderRecently = false;
        }

        if (turnsAlive == 0) {
            startNumArchons = rc.getArchonCount();
            for (int i = 48; i >= 0; i --) {
                sectorMdpts[i] = Comms.sectorMidpt(rc, i);
            }
            if (startNumArchons == 1) {
                mapThres = 0;
            } else {
                mapThres = 900;
            }
        }

        //Initialize combat sector.  If we see an enemy, enemyCount++
        if (combatSector == -1) {
            combatSector = 50;
        } else if (!firstEnemySeen) {
            firstEnemySeen = true;
        }

        if (minerCount == 0) {
            minersBuilt = 0;
        }

        //Set all the default friendlyArchonSectors to 50 for initialization
        int[] friendlyArchonSectors = new int[rc.getArchonCount()];
        for (int i = friendlyArchonSectors.length-1; i >=0; i--) {
            friendlyArchonSectors[i] = 50;
        }

        int numUniqueArchonSectors = 0;
        //Find all nearby friendlyArchonSectors
        for (int i = 48; i >= 0; i--) {
            if (Comms.readSectorInfo(rc, i, 0) == 1) {
                for (int j = friendlyArchonSectors.length-1; j >=0; j--) {
                    if (friendlyArchonSectors[j] == 50) {
                        friendlyArchonSectors[j] = i;
                        numUniqueArchonSectors++;
                        break;
                    }
                }
            }
        }

        //Find the closest enemy to any friendlySector
        int closestDistToAnyArchon = Integer.MAX_VALUE;
        for (int i = 48; i >= 0; i--) {
            if (Comms.readSectorInfo(rc, i, 3) > 0) {
                for (int j = friendlyArchonSectors.length-1; j >=0; j--) {
                    if (Comms.sectorMidpt(rc, friendlyArchonSectors[j]).distanceSquaredTo(Comms.sectorMidpt(rc, i)) < closestDistToAnyArchon) {
                        combatSector = i;
                    }
                }
            }
            if (!firstEnemySeen && Comms.readSectorInfo(rc, i, 3) > 0) {
                firstEnemySeen = true;
            }
        }

        if (turnCount % 3 == 0 || turnsAlive == 0) {
            bestTgtSector = null;
            double highScore = 0;
            for (int i = 48; i >= 0; i --) {
                int homeArchon = Comms.readSectorInfo(rc, i, 0);
                int enemyArchon = Comms.readSectorInfo(rc, i, 1);
                int enemyInSector = Comms.readSectorInfo(rc, i, 3);
                if (homeArchon == 1 && (closestFriendlyArchon == null
                        || sectorMdpts[i].distanceSquaredTo(src) < closestFriendlyArchon.distanceSquaredTo(src))) {
                    closestFriendlyArchon = sectorMdpts[i];
                }
                if (enemyArchon == 1 || enemyInSector > 0) {
                    double currentScore = (10.0*enemyArchon + enemyInSector)/(src.distanceSquaredTo(sectorMdpts[i]));
                    if (currentScore > highScore) {
                        bestTgtSector = sectorMdpts[i];
                        highScore = currentScore;
                    }
                }
            }
        }

        Direction stallDir = stallOnGoodRubble(rc);

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

        //Accounting for rubble when creating a unit.  Don't care about rubble if mapArea > mapThres
        double rubble = 200;
        for (Direction dire : directions) {
            MapLocation loc = src.add(dire);
            if (rc.onTheMap(loc) && rc.senseRobotAtLocation(loc) == null) {
                if (rc.senseRubble(loc) < rubble && mapArea > mapThres) {
                    rubble = rc.senseRubble(loc);
                    dir = dire;
                } else if (mapArea <= mapThres){
                    dir = dire;
                    break;
                }
            }
        }

//        if (nearestPatch != null) {
//            Direction[] possDirections = new Direction[3];
//            possDirections[2] = rc.getLocation().directionTo(nearestPatch);
//            possDirections[1] = possDirections[2].rotateLeft();
//            possDirections[0] = possDirections[2].rotateRight();
//            int minRubble = Integer.MAX_VALUE;
//            for (int i = possDirections.length - 1; i>= 0; i--) {
//                MapLocation tempLoc = src.add(possDirections[i]);
//                int rubbleAtDir = rc.senseRubble(tempLoc);
//                if (rc.onTheMap(tempLoc) && rc.senseRobotAtLocation(tempLoc) == null && rubbleAtDir < minRubble) {
//                    minRubble = rubbleAtDir;
//                    dir = possDirections[i];
//                }
//            }
//        }
        if (nearestPatch != null && rc.onTheMap(nearestPatch) && rc.senseRobotAtLocation(nearestPatch) == null) {
            dir = rc.getLocation().directionTo(nearestPatch);
        }

        //Initiallizing shouldBuildSoldier as false.  The following code ensures that the closest archon to combat
        // produces soldiers.  The exception is that every 4 soldiers, the farthest archon to combat creates a soldier
        // to potentially mix it up and create other fronts of attack.
        boolean shouldBuildLab = false;
        boolean shouldBuildSage = false;
        boolean shouldBuildMinerOrd = false;
        boolean shouldBuildSoldier = false;

        //initialize friendlyArchonSectorsDists which is the distance of each of the archons to the closest enemy
        // sector.
        int[] friendlyArchonSectorsDists = new int[friendlyArchonSectors.length];
        MapLocation combatMdpt = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
        if (combatSector < 50) {
            combatMdpt = Comms.sectorMidpt(rc, combatSector);
        }
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

        if (numUniqueArchonSectors < rc.getArchonCount() && numUniqueArchonSectors > 0) {
            for (int i= rc.getArchonCount()-numUniqueArchonSectors-1;i>=0;i--) {
                friendlyArchonSectorsDists[i+1] = friendlyArchonSectorsDists[0];
            }
        }

        if (rc.readSharedArray(58) == 1) {
            numUniqueArchonSectors--;
        }

        if (numUniqueArchonSectors < 1) {
            numUniqueArchonSectors = 1;
        }

        int minersCanBuild = rc.getTeamLeadAmount(rc.getTeam()) / 50;
        if (minersCanBuild > rc.getArchonCount()) {
            minersCanBuild = rc.getArchonCount();
        }

        if (minersCanBuild > 0) {
            if ((turnCount % 4 < 3 || Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt) == 0)) {
                for (int i = minersCanBuild - 1; i >= 0; i--) {
                    if (friendlyArchonSectorsDists[(minersCanBuild - 1) - i] == Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt)) {
                        shouldBuildMinerOrd = true;
                    }
                }
            } else {
                for (int i = minersCanBuild - 1; i >= 0; i--) {
                    if (friendlyArchonSectorsDists[rc.getArchonCount() - 1 - i] == Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt)) {
                        shouldBuildMinerOrd = true;
                    }
                }
            }
        }

        int sagesCanBuild = rc.getTeamGoldAmount(rc.getTeam()) / 20;
        if (sagesCanBuild > rc.getArchonCount()) {
            sagesCanBuild = rc.getArchonCount();
        }

        int soldiersCanBuild = rc.getTeamLeadAmount(rc.getTeam()) / 75;
        if (soldiersCanBuild > rc.getArchonCount()) {
            soldiersCanBuild = rc.getArchonCount();
        }

        if (soldiersCanBuild > 0) {
            if (((sageCount + soldierCount) % 2 < 1 || Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt) == 0)) {
                for (int i = soldiersCanBuild - 1; i >= 0; i--) {
                    if (friendlyArchonSectorsDists[(soldiersCanBuild - 1) - i] == Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt)) {
                        shouldBuildSoldier = true;
                    }
                }
            } else { //potentially look at using numUniqueArchonSectors hers instead of rc.getArchon count as sometimes not all archons will have unique sectors
                for (int i = soldiersCanBuild - 1; i >= 0; i--) {
                    if (friendlyArchonSectorsDists[rc.getArchonCount() - 1 - i] == Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt)) {
                        shouldBuildSoldier = true;
                    }
                }
            }
        }

        if (sagesCanBuild > 0) {
            if (((sageCount + soldierCount) % 5 < 4 || Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt) == 0)) {
                for (int i = sagesCanBuild - 1; i >= 0; i--) {
                    if (friendlyArchonSectorsDists[(sagesCanBuild - 1) - i] == Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt)) {
                        shouldBuildSage = true;
                    }
                }
            } else { //potentially look at using numUniqueArchonSectors hers instead of rc.getArchon count as sometimes not all archons will have unique sectors
                for (int i = sagesCanBuild - 1; i >= 0; i--) {
                    if (friendlyArchonSectorsDists[rc.getArchonCount() - 1 - i] == Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt)) {
                        shouldBuildSage = true;
                    }
                }
            }
        }

        if (friendlyArchonSectorsDists[numUniqueArchonSectors-1] == Comms.sectorMidpt(rc, Comms.locationToSector(rc, rc.getLocation())).distanceSquaredTo(combatMdpt)) {
            shouldBuildLab = true;
        }

        //Repair Logic
        Team friendly = rc.getTeam();
        RobotInfo[] alliedUnits = rc.senseNearbyRobots(actionRadius, friendly);
        int nearbyBuilders = 0;

        //if theres a wounded soldier nearby, repair it
        RobotInfo woundedWarrior = null;
        for (int i = alliedUnits.length - 1; i >= 0; i--) {
            RobotInfo unit = alliedUnits[i];
            int unitHealth = unit.getHealth();
            if (unit.getType() == RobotType.BUILDER) {
                nearbyBuilders++;
            }
            if (unitHealth < unit.getType().getMaxHealth(1)) {
                if (woundedWarrior == null) {
                    woundedWarrior = unit;
                } else if (isHostile(woundedWarrior))  {
                    //note: woundedWarrior could be a watchtower which would be a bug if we built watchtowers
                    if (isHostile(unit) && unit.getHealth() < woundedWarrior.health) {
                        woundedWarrior = unit;
                    }
                } else {
                    if (isHostile(unit) || unit.getHealth() < woundedWarrior.health) {
                        woundedWarrior = unit;
                    }
                }
            }
        }

        //If we can't build a miner and we can repair a soldier, do it.
        if (woundedWarrior != null && rc.canRepair(woundedWarrior.location) && !rc.canBuildRobot(RobotType.MINER, dir) && !rc.canBuildRobot(RobotType.SAGE, dir)) {
            rc.setIndicatorString("repair");
            rc.repair(woundedWarrior.location);
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
        //UNIT BUILDING:

        int initialMiners = 3;
        if (mapArea <= mapThres) {
            initialMiners = 2;
        }

        int minerBuilderRatio = 3;
        if (mapArea < 1200) {
            minerBuilderRatio = 5;
        }

        int initLabCount = 1;
        if (firstEnemySeen) {
            initLabCount = 1;
        }

        int distAwayFromEnemy = 100;
        int minerLabRatio = 10;

        if (bestTgtSector != null && rc.getLocation().distanceSquaredTo(bestTgtSector) >= distAwayFromEnemy) {
            turnsOutOfRange++;
        } else if (bestTgtSector != null && rc.getLocation().distanceSquaredTo(bestTgtSector) < distAwayFromEnemy) {
            turnsOutOfRange = 0;
        }

        //If there is no enemyArchonNearby and the first enemy hasn't been seen or there is nearby lead between 50 and 100, build a miner
        //If our minerCount is less than the target or our miner count is greater than our attacker count times a ratio and we shouldn't build a builder or theres an enemyArchonNearby, build a soldier.
        if (rc.readSharedArray(55) >> 7 == 1 && (rc.getTeamLeadAmount(rc.getTeam()) >= 350)) {
            rc.writeSharedArray(55, (rc.readSharedArray(55) & 0b1111111));
        }

        if (false && (rc.getMapHeight() * 1.75 <= rc.getMapWidth() || rc.getMapWidth() * 1.75 <= rc.getMapHeight())) {
            transitionTurn = 375;
            if (soldierCount > 2 * rc.getArchonCount()) {
                transitionTurn = 0;
            }
        } else if (mapArea < 1156 && rc.getArchonCount() < 3) {
            transitionTurn = 375;
        }

        System.out.println(transitionTurn);
        if (turnCount < transitionTurn) {
            int initialSoldiers = rc.getArchonCount() * 2;
            if (firstEnemySeen && rc.readSharedArray(58) == 0 && rc.getMode() == RobotMode.TURRET && bestTgtSector != null &&
                    rc.getLocation().distanceSquaredTo(bestTgtSector) >= distAwayFromEnemy && rc.getTeamLeadAmount(rc.getTeam()) < 350 && shouldBuildLab && turnsOutOfRange > 30) {
                if (rc.canTransform()) {
                    rc.setIndicatorString("Turret to Port");
                    rc.transform();
                    rc.writeSharedArray(58, 1);
                }
            } else if (rc.getMode() == RobotMode.PORTABLE && rc.canTransform() &&
                    (rc.getLocation().distanceSquaredTo(bestTgtSector) < distAwayFromEnemy || rc.getTeamLeadAmount(rc.getTeam()) >= 350)
                    && stallDir == Direction.CENTER && rc.senseRubble(src) <= rubbleThreshold) {
                if (rc.canTransform()) {
                    rc.setIndicatorString("Port to Turrent");
                    rc.transform();
                    rc.writeSharedArray(58, 0);
                }
            }
            else if (rc.canBuildRobot(RobotType.SAGE, dir) && shouldBuildSage) {
                rc.setIndicatorString("2");
                rc.buildRobot(RobotType.SAGE, dir);
                minersBuiltInARow = 0;
                buildersBuiltInARow = 0;
                sagesBuilt++;
            }
            else if (rc.readSharedArray(55) >> 7 == 0) {
                if (minerCount < initialMiners && rc.canBuildRobot(RobotType.MINER, dir)) {
                    rc.setIndicatorString("3");
                    if (shouldBuildMinerOrd) {
                        rc.buildRobot(RobotType.MINER, dir);
                        minersBuilt++;
                    }
                } else if (soldierCount < initialSoldiers) {
                    rc.setIndicatorString("soldier");
                    if (shouldBuildSoldier && rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                        rc.setIndicatorString("1");
                        rc.buildRobot(RobotType.SOLDIER, dir);
                        soldiersBuilt++;
                    }
                } else if ((builderCount == 0 || (minerCount / minerLabRatio + initLabCount > builderCount &&
                        (minerCount / minerBuilderRatio > builderCount && rc.getTeamGoldAmount(rc.getTeam()) > 0)) ||
                        rc.getTeamLeadAmount(rc.getTeam()) >= 350) && shouldBuildLab) {
                    rc.setIndicatorString("4");
                    if (rc.canBuildRobot(RobotType.BUILDER, dir) && !builtBuilderRecently) {
                        rc.buildRobot(RobotType.BUILDER, dir);
                        buildersBuilt++;
                        soldiersBuiltInARow = 0;
                        minersBuiltInARow = 0;
                        buildersBuiltInARow++;
                        builtBuilderRecently = true;
                        if ((minerCount / minerLabRatio + initLabCount > builderCount || builderCount == 0)) {
                            rc.writeSharedArray(55, (rc.readSharedArray(55) | 0b10000000));
                        }
                    }
                } else if (soldierCount <= minerCount) {
                    rc.setIndicatorString("soldier over miner");
                    if (shouldBuildSoldier && rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                        rc.setIndicatorString("1");
                        rc.buildRobot(RobotType.SOLDIER, dir);
                        soldiersBuilt++;
                    }
                } else if (rc.canBuildRobot(RobotType.MINER, dir)) {
                    rc.setIndicatorString("5");
                    rc.buildRobot(RobotType.MINER, dir);
                    minersBuilt++;
                    unitsAfterEnemySeen++;
                }
                else {
                    rc.setIndicatorString("afk");
                }
            } else {
                rc.setIndicatorString("saving");
            }
        } else {
            if (firstEnemySeen && rc.readSharedArray(58) == 0 && rc.getMode() == RobotMode.TURRET && bestTgtSector != null &&
                    rc.getLocation().distanceSquaredTo(bestTgtSector) >= distAwayFromEnemy && rc.getTeamLeadAmount(rc.getTeam()) < 350 && shouldBuildLab && turnsOutOfRange > 30) {
                if (rc.canTransform()) {
                    rc.setIndicatorString("Turret to Port");
                    rc.transform();
                    rc.writeSharedArray(58, 1);
                }
            } else if (rc.getMode() == RobotMode.PORTABLE && rc.canTransform() &&
                    (rc.getLocation().distanceSquaredTo(bestTgtSector) < distAwayFromEnemy || rc.getTeamLeadAmount(rc.getTeam()) >= 350)
                    && stallDir == Direction.CENTER && rc.senseRubble(src) <= rubbleThreshold) {
                if (rc.canTransform()) {
                    rc.setIndicatorString("Port to Turrent");
                    rc.transform();
                    rc.writeSharedArray(58, 0);
                }
            } else if (rc.canBuildRobot(RobotType.SAGE, dir) && shouldBuildSage) {
                rc.setIndicatorString("2");
                rc.buildRobot(RobotType.SAGE, dir);
                minersBuiltInARow = 0;
                buildersBuiltInARow = 0;
                sagesBuilt++;
            }
//        else if (rc.getHealth() < rc.getType().getMaxHealth(rc.getLevel())/3.0 && nearbyBuilders < 1) {
//            if (rc.canBuildRobot(RobotType.BUILDER, dir)) {
//                rc.setIndicatorString("8");
//                rc.buildRobot(RobotType.BUILDER, dir);
//                buildersBuilt++;
//                soldiersBuiltInARow = 0;
//                minersBuiltInARow = 0;
//                buildersBuiltInARow++;
//                builtBuilderRecently = true;
//            }
//        }
            else if (rc.readSharedArray(55) >> 7 == 0) {
                rc.setIndicatorString("not saving");
//            if (sageCount > 15 && 2 * sageCount < minerCount) {
//                rc.setIndicatorString("soldier");
//                if (shouldBuildSage && rc.canBuildRobot(RobotType.SOLDIER, dir)) {
//                                rc.setIndicatorString("1");
//                                rc.buildRobot(RobotType.SOLDIER, dir);
//                                soldiersBuilt++;
//                            }
//            }
                if (minerCount < initialMiners && rc.canBuildRobot(RobotType.MINER, dir) && minersBuilt < (5 / rc.getArchonCount())) {
                    rc.setIndicatorString("3");
                    if (shouldBuildMinerOrd) {
                        rc.buildRobot(RobotType.MINER, dir);
                        minersBuilt++;
                    }
                } else if ((builderCount == 0 || (minerCount / minerLabRatio + initLabCount > builderCount &&
                        (minerCount / minerBuilderRatio > builderCount && rc.getTeamGoldAmount(rc.getTeam()) > 0)) ||
                        rc.getTeamLeadAmount(rc.getTeam()) >= 350) && shouldBuildLab) {
                    rc.setIndicatorString("4");
                    if (rc.canBuildRobot(RobotType.BUILDER, dir) && !builtBuilderRecently) {
                        rc.buildRobot(RobotType.BUILDER, dir);
                        buildersBuilt++;
                        soldiersBuiltInARow = 0;
                        minersBuiltInARow = 0;
                        buildersBuiltInARow++;
                        builtBuilderRecently = true;
                        if ((minerCount / minerLabRatio + initLabCount > builderCount || builderCount == 0)) {
                            rc.writeSharedArray(55, (rc.readSharedArray(55) | 0b10000000));
                        }
                    }
                }
                //    else if (!firstEnemySeen) {
                //        if (shouldBuildSoldier && rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                //            rc.setIndicatorString("3");
                //            rc.buildRobot(RobotType.SOLDIER, dir);
                //            soldiersBuilt++;
                //        }
                //    }
                //    else if ((unitsAfterEnemySeen) % 3 < 2) {
                //        rc.setIndicatorString("soldier?");
                //        if (rc.canBuildRobot(RobotType.SOLDIER, dir) && shouldBuildSoldier) {
                //            rc.buildRobot(RobotType.SOLDIER, dir);
                //            soldiersBuilt++;
                //            rc.writeSharedArray(51, rc.readSharedArray(51) + 1);
                //            unitsAfterEnemySeen++;
                //        }
                //    }
                else if (rc.canBuildRobot(RobotType.MINER, dir)) {
                    rc.setIndicatorString("5");
                    rc.buildRobot(RobotType.MINER, dir);
                    minersBuilt++;
                    unitsAfterEnemySeen++;

                } else {
                    rc.setIndicatorString("6");
                }
            } else {
                rc.setIndicatorString("7");
            }
        }
        //Updating Stuff
        lastTurnMiners = rc.readSharedArray(50);
        turnsAlive++;

        if (roundStartLead >= (rc.getArchonCount() - minerDiff) * 50) {
            spreadCooldown = 0;
        }
        if (rc.isActionReady()) {
            turnsNotActioning++;
        }
        Direction moveDir = null;
        if (rc.getMode() == RobotMode.PORTABLE && bestTgtSector != null) {
            moveDir = Pathfinder.getMoveDir(rc, bestTgtSector, prev5Spots);
        }
        if (moveDir != null && rc.canMove(moveDir)) {
            rc.move(moveDir);
            prev5Spots[currentOverrideIndex] = rc.getLocation();
            currentOverrideIndex  = (currentOverrideIndex + 1) % 5;
        }

        if (previousSector != 50 && Comms.locationToSector(rc, src) != previousSector) {
            rc.writeSharedArray(previousSector, rc.readSharedArray(previousSector) & 0x7FFF);
        }

        Comms.updateSector(rc);
        Comms.clearCounts(rc);
        if (((rc.readSharedArray(55) >> 6) & 0b1) == 0) {
            updateGuesses(rc);
        }
        previousSector = Comms.locationToSector(rc, src);
    }
}
