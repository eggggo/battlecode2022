package mainBot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.ArrayList;
import java.util.List;

public class Miner extends RobotPlayer {
    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runMiner(RobotController rc) throws GameActionException {
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

        //Sensing Important information:
        List<MapLocation> resources = new ArrayList<MapLocation>();
        MapLocation[] allLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), Integer.MAX_VALUE); // Second parameter defaults to robot sensing radius if too large
        for (int i = allLocs.length-1; i >= 0; i--) {
            if (rc.senseLead(allLocs[i]) > 20 || rc.senseGold(allLocs[i]) != 0) {
                resources.add(allLocs[i]);
            }
        }

        Direction dir = directions[rng.nextInt(directions.length)];
        if (!resources.isEmpty()) {
            MapLocation tgtResource = resources.get(0);
            dir = Pathfinder.getMoveDir(rc, tgtResource);
        }
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
