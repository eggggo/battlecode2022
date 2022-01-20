package mainBot;

import java.util.Random;
import battlecode.common.*;

public class BetterMiner extends RobotPlayer {

    static int prevIncome = 0;
    static boolean aboveHpThresh = true;
    static int turnsAlive = 0;
    static MapLocation[] sectorMdpts = new MapLocation[49];
    static int maxTravelDistance = 100;
    static MapLocation scoutTgt = null;
    static Random rng = new Random();
    static MapLocation nearestFriendlyArchon = null;
    static MapLocation bestOOVResource = null;

    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */

    //looks in a 3x3 square around unit to find the best rubble square, then returns direction to it
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
                    if (rubbleHere <= minRubble) {
                        minRubble = rubbleHere;
                        minRubbleLoc = test;
                    }
                }
            }
        }
        return src.directionTo(minRubbleLoc);
    }

    //tests whether a unit is hostile(does damage) or not
    static boolean isHostile(RobotInfo unit) {
        return (unit.getType() == RobotType.SOLDIER || unit.getType() == RobotType.SAGE 
        || unit.getType() == RobotType.WATCHTOWER);
    }

    static void runMiner(RobotController rc) throws GameActionException {
        int income = 0;
        int senseRadius = rc.getType().visionRadiusSquared;
        Team friendly = rc.getTeam();
        Team opponent = rc.getTeam().opponent();
        MapLocation src = rc.getLocation();
        Direction dir = null;
        MapLocation resources = null;
        RobotInfo[] enemies = rc.senseNearbyRobots(senseRadius, opponent);

        //setup loop, populates sector mdpts array for cheaper access, gets scout location
        if (turnsAlive == 0) {
            for (int i = 48; i >= 0; i --) {
                sectorMdpts[i] = Comms.sectorMidpt(rc, i);
            }
            int scoutPattern = rc.readSharedArray(50) % 2;
            if (scoutPattern == 0) {
                scoutTgt = sectorMdpts[rng.nextInt(49)];
            } else {
                int designatedLoc = rng.nextInt(5);
                switch (designatedLoc) {
                    case 0:
                        scoutTgt = new MapLocation(0, 0);
                        break;
                    case 1:
                        scoutTgt = new MapLocation(rc.getMapWidth() - 1, 0);
                        break;
                    case 2:
                        scoutTgt = new MapLocation(0, rc.getMapHeight() - 1);
                        break;
                    case 3:
                        scoutTgt = new MapLocation(rc.getMapWidth() - 1, rc.getMapHeight() - 1);
                        break;
                    case 4:
                        scoutTgt = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
                        break;
                }
            }
        }

        //main comms loop, do all comms stuff here
        //finds mdpt of closest friendly archon, best resources in travel distance
        //refreshes every 2 turns to reduce bytecode load
        if (turnCount % 2 == 0) {
            nearestFriendlyArchon = null;
            bestOOVResource = null;
            int bestResource = 0;
            for (int i = 48; i >= 0; i --) {
                int[] sectorInfo = Comms.readSectorInfo(rc, i);
                MapLocation sectorMdpt = sectorMdpts[i];
                if (sectorInfo[0] == 1 && (nearestFriendlyArchon == null 
                || sectorMdpt.distanceSquaredTo(src) < nearestFriendlyArchon.distanceSquaredTo(src))) {
                    nearestFriendlyArchon = sectorMdpt;
                }
                if (sectorInfo[2] > bestResource && maxTravelDistance >= sectorMdpt.distanceSquaredTo(src)) {
                    bestResource = sectorInfo[2];
                    bestOOVResource = sectorMdpt;
                }
            }
        }

        // Try to mine on squares around us.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(src.x + dx, src.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                    income += 5;
                }
                //if we are farther than a certain distance from nearest friendly archon, let's mine out the area
                int friendlyResourceRadius = (int)Math.pow((rc.getMapWidth()/2 + rc.getMapHeight()/2)/2, 2);
                boolean shouldContinue = nearestFriendlyArchon != null 
                && src.distanceSquaredTo(nearestFriendlyArchon) > friendlyResourceRadius;
                int stopMiningThres = 1;
                if (shouldContinue) {
                    stopMiningThres = 0;
                }
                while (rc.canMineLead(mineLocation) && rc.senseLead(mineLocation) > stopMiningThres) {
                    rc.mineLead(mineLocation);
                    income += 1;
                }
            }
        }

        //finding closest enemy hostile to run away if exists
        MapLocation closestAttacker = null;
        for (int i = enemies.length - 1; i >= 0; i --) {
            RobotInfo enemy = enemies[i];
            if (isHostile(enemy) && (closestAttacker == null
            || enemy.location.distanceSquaredTo(src) < closestAttacker.distanceSquaredTo(src))) {
                closestAttacker = enemy.location;
            }
        }

        //finding best mining spot in vision
        MapLocation[] nearbyLead = rc.senseNearbyLocationsWithLead(senseRadius, 5);
        MapLocation[] nearbyGold = rc.senseNearbyLocationsWithGold(senseRadius);
        double highLead = 0;
        for (int i = nearbyLead.length - 1; i >= 0; i --) {
            double leadCount = (double)rc.senseLead(nearbyLead[i])/(1 + rc.senseRubble(nearbyLead[i])/10.0);
            if (leadCount > highLead) {
                highLead = leadCount;
                resources = nearbyLead[i];
            }
        }
        if (nearbyGold.length > 0) {
            resources = nearbyGold[0];
        }

        //if you can see the scout target sector mdpt, randomize and go to somewhere else
        if (src.distanceSquaredTo(scoutTgt) <= senseRadius) {
            scoutTgt = sectorMdpts[rng.nextInt(49)];
        }

        //movement action loop, vectors for avoiding enemies
        int xVector = 0;
        int yVector = 0;
        //if good nearby resources go there
        if (resources != null) {
            dir = src.directionTo(resources);
        //if good comms nearby resources go there
        } else if (bestOOVResource != null) {
            dir = src.directionTo(bestOOVResource);
        //else, go to scout location
        } else {
            dir = src.directionTo(scoutTgt);
        }
        xVector += dir.dx;
        yVector += dir.dy;
        
        //adding away from attacker vector
        if (closestAttacker != null) {
            Direction awayAttacker = src.directionTo(closestAttacker).opposite();
            xVector += 2*awayAttacker.dx;
            yVector += 2*awayAttacker.dy;
        }

        MapLocation vectorTgt = src.translate(4*xVector, 4*yVector);
        MapLocation inBounds = new MapLocation(Math.min(Math.max(0, vectorTgt.x), rc.getMapWidth() - 1), 
            Math.min(Math.max(0, vectorTgt.y), rc.getMapHeight() - 1));
        dir = Pathfinder.getMoveDir(rc, inBounds);
        
        if (rc.canMove(dir)) {
            rc.move(dir);
        }

        //Comms stuff
        Comms.updateSector(rc, turnCount);
        int deltaIncome = income - prevIncome;
        //index 49 is global income
        int currentIncome = rc.readSharedArray(49);
        rc.writeSharedArray(49, Math.max(0, currentIncome + deltaIncome));
        prevIncome = income;

        //self report alive or ded
        boolean currentHpThresh = (double)rc.getHealth()/rc.getType().getMaxHealth(1) > 0.2;
        if (!currentHpThresh && aboveHpThresh) {
            rc.writeSharedArray(50, rc.readSharedArray(50) - 1);
        } else if (currentHpThresh && !aboveHpThresh) {
            rc.writeSharedArray(50, rc.readSharedArray(50) + 1);
        }
        aboveHpThresh = currentHpThresh;
        turnsAlive++;
    }
}
