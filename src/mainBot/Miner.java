package mainBot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;

public class Miner extends RobotPlayer {
    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runMiner(RobotController rc) throws GameActionException {

        int senseRadius = rc.getType().visionRadiusSquared;
        Team friendly = rc.getTeam();
        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation) && rc.senseLead(mineLocation) > 1) {
                    rc.mineLead(mineLocation);
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

        //If there's a nearby soldier, follow it, otherwise go in a random direction
        Direction dir;
        if (nearbySoldier == null || nearbyRobots.length > 9) {
            dir = directions[rng.nextInt(directions.length)];
        } else {
            dir = Pathfinder.getMoveDir(rc, nearbySoldier);
        }

        //Sensing Important information:
        MapLocation resources = null;
        MapLocation[] allLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), rc.getType().visionRadiusSquared); // Second parameter defaults to robot sensing radius if too large
        for (int i = allLocs.length - 1; i >= 0; i--) {
            if (rc.senseLead(allLocs[i]) > 50 || rc.senseGold(allLocs[i]) != 0) {
                resources = allLocs[i];
            }
        }

        //If there are resources nearby go to it
        if (resources != null) {
            MapLocation tgtResource = resources;
            dir = Pathfinder.getMoveDir(rc, tgtResource);
        }

        if (rc.canMove(dir)) {
            rc.move(dir);
        }

        //Comms stuff
        Comms.updateSector(rc);
    }
}
