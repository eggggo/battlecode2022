package mainBot;

import battlecode.common.*;
import mainBot.Pathfinder;

import java.util.ArrayList;
import java.util.List;

public class Miner extends RobotPlayer {

    static int prevIncome = 0;
    static boolean aboveHpThresh = true;
    static int turnsAlive = 0;
    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runMiner(RobotController rc) throws GameActionException {
        int income = 0;
        int senseRadius = rc.getType().visionRadiusSquared;
        Team friendly = rc.getTeam();

        if (turnsAlive == 0) {
            rc.writeSharedArray(50, rc.readSharedArray(50) + 1);
        }

        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                    income += 5;
                }
                while (rc.canMineLead(mineLocation) && rc.senseLead(mineLocation) > 1) {
                    rc.mineLead(mineLocation);
                    income += 1;
                }
            }
        }

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(senseRadius, friendly);
        MapLocation nearbySoldier = null;
        //Sensing Important information like a nearby friendly soldier
        for (int i = nearbyRobots.length - 1; i >= 0; i--) {
            if (nearbyRobots[i].getType() == RobotType.SOLDIER) {
                nearbySoldier = nearbyRobots[i].getLocation();
                break;
            }
        }

        //Instruction order:
        //1: Follow nearby soldier
        //2: Walk to resource
        //3: Random direction
        Direction dir;
        //Is there a nearby soldier without a huge number of followers?

        if (nearbySoldier != null && nearbyRobots.length < 9) {
            dir = Pathfinder.getMoveDir(rc, nearbySoldier);
        } else {
            MapLocation resources = null;
            MapLocation[] allLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), rc.getType().visionRadiusSquared); // Second parameter defaults to robot sensing radius if too large
            int highLead = 0;
            int highGold = 0;
            for (int i = allLocs.length - 1; i >= 0; i--) {
                int leadAmount = rc.senseLead(allLocs[i]);
                int goldAmount = rc.senseGold(allLocs[i]);
                if (leadAmount > 5 || goldAmount != 0) {
                    if (leadAmount > highLead || goldAmount > highGold) {
                        resources = allLocs[i];
                        highLead = leadAmount;
                        highGold = goldAmount;
                    }
                }
            }
            //Are there nearby resources?
            if (resources != null) {
                MapLocation tgtResource = resources;
                dir = Pathfinder.getMoveDir(rc, tgtResource);
                MapLocation newLocation = new MapLocation(me.x + dir.dx, me.y + dir.dy);
                int newNeighbors = 0;
                for (int i = 1; i >= -1; i -= 1) {
                    for (int j = 1; j >= -1; j -= 1) {
                        MapLocation loc = new MapLocation(me.x + dir.dx + i, me.y + dir.dy + j);
                        if (rc.onTheMap(loc) && rc.senseRobotAtLocation(loc) != null) {
                            newNeighbors += 1;
                        }
                    }
                }
                if (newNeighbors >= 3) {
                    dir = directions[rng.nextInt(directions.length)];
                }
                //Else random direction
            } else {
                dir = directions[rng.nextInt(directions.length)];
            }
        }

        if (rc.canMove(dir)) {
            rc.move(dir);
        }

        //Comms stuff
        Comms.updateSector(rc);
        /*
        int deltaIncome = income - prevIncome;
        //index 49 is global income
        int currentIncome = rc.readSharedArray(49);
        rc.writeSharedArray(49, currentIncome + deltaIncome);
        prevIncome = income;*/

        boolean currentHpThresh = rc.getHealth()/rc.getType().getMaxHealth(1) > 0.2;
        if (!currentHpThresh && aboveHpThresh) {
            rc.writeSharedArray(50, rc.readSharedArray(50) - 1);
        } else if (currentHpThresh && !aboveHpThresh) {
            rc.writeSharedArray(50, rc.readSharedArray(50) + 1);
        }
        aboveHpThresh = currentHpThresh;
        turnsAlive++;
    }
}
