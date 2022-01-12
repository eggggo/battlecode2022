package mainBot;

import battlecode.common.*;
import mainBot.Pathfinder;

import java.util.ArrayList;
import java.util.List;

public class Miner extends RobotPlayer {

    static int prevIncome = 0;
    static boolean aboveHpThresh = true;
    static int turnsAlive = 0;
    static MapLocation[] sectorMdpts = new MapLocation[49];
    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    //action flow:
    //1. mine
    //2. if close enemy run away
    //3. if close to resource go
    //4. if close to other miners spread
    //5. goto nearest sector with resource
    static void runMiner(RobotController rc) throws GameActionException {
        int income = 0;
        int senseRadius = rc.getType().visionRadiusSquared;
        Team friendly = rc.getTeam();
        MapLocation src = rc.getLocation();
        Direction dir = null;
        MapLocation resources = null;

        if (turnsAlive == 0) {
            for (int i = 48; i >= 0; i --) {
                sectorMdpts[i] = Comms.sectorMidpt(rc, i);
            }
            //scouting miners
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

        //run away from enemy attackers if we see them
        RobotInfo[] enemies = rc.senseNearbyRobots(senseRadius, friendly.opponent());
        RobotInfo[] friendlies = rc.senseNearbyRobots(senseRadius, friendly);
        MapLocation closestAttacker = null;
        MapLocation[] nearbyLead = rc.senseNearbyLocationsWithLead(senseRadius);
        MapLocation[] nearbyGold = rc.senseNearbyLocationsWithGold(senseRadius);
        double highLead = 0;
        for (int i = enemies.length - 1; i >= 0; i --) {
            RobotInfo enemy = enemies[i];
            if ((enemy.getType() == RobotType.SOLDIER || enemy.getType() == RobotType.SAGE 
            || enemy.getType() == RobotType.WATCHTOWER) && (closestAttacker == null
            || enemy.location.distanceSquaredTo(src) < closestAttacker.distanceSquaredTo(src))) {
                closestAttacker = enemy.location;
            }
        }
        for (int i = nearbyLead.length - 1; i >= 0; i --) {
            double leadCount = (double)rc.senseLead(nearbyLead[i])/(1 + rc.senseRubble(nearbyLead[i])/10.0);
            if (leadCount > 5 && leadCount > highLead) {
                highLead = leadCount;
                resources = nearbyLead[i];
            }
        }
        if (nearbyGold.length > 0) {
            resources = nearbyGold[0];
        }

        //main control structure
        //run if enemy attacker in vision
        if (closestAttacker != null) {
            Direction opposite = src.directionTo(closestAttacker).opposite();
            MapLocation runawayTgt = src.add(opposite).add(opposite);
            runawayTgt = new MapLocation(Math.min(Math.max(0, runawayTgt.x), rc.getMapWidth() - 1), 
            Math.min(Math.max(0, runawayTgt.y), rc.getMapHeight() - 1));
            dir = Pathfinder.getMoveDir(rc, runawayTgt);
        //if good resources nearby go there
        } else if (resources != null) {
            MapLocation tgtResource = resources;
            dir = Pathfinder.getMoveDir(rc, tgtResource);
        //else go to sector with reported resources + spread vector + away from closest archon vector
        } else {
            int sectorNumber = (int) (Math.random() * 48) + 1;
            int distance = 9999;
            int archonDistance = 9999;
            MapLocation closestHomeArchon = null;
            for (int i = 48; i >= 0; i--) {
                int[] sector = Comms.readSectorInfo(rc, i);
                if (sector[2] > 30 && rc.getLocation().distanceSquaredTo(sectorMdpts[i]) < distance) {
                    sectorNumber = i;
                    distance = rc.getLocation().distanceSquaredTo(sectorMdpts[i]);
                }
                if (sector[0] == 1 && sectorMdpts[i].distanceSquaredTo(src) < archonDistance) {
                    archonDistance = sectorMdpts[i].distanceSquaredTo(src);
                    closestHomeArchon = sectorMdpts[i];
                }
            }
            dir = Pathfinder.getMoveDir(rc, sectorMdpts[sectorNumber]);
            double xVector = dir.dx;
            double yVector = dir.dy;
            for (int i = friendlies.length - 1; i >= 0; i --) {
                MapLocation friendlyLoc = friendlies[i].getLocation();
                double d = Math.sqrt(src.distanceSquaredTo(friendlyLoc));
                Direction opposite = src.directionTo(friendlyLoc).opposite();
                xVector += opposite.dx*(2.0/d);
                yVector += opposite.dy*(2.0/d);
            }
            Direction oppositeClosestHomeArchon = src.directionTo(closestHomeArchon).opposite();
            xVector += oppositeClosestHomeArchon.dx;
            yVector += oppositeClosestHomeArchon.dy;
            MapLocation vectorTgt = src.translate((int)xVector, (int)yVector);
            MapLocation inBounds = new MapLocation(Math.min(Math.max(0, vectorTgt.x), rc.getMapWidth() - 1), 
            Math.min(Math.max(0, vectorTgt.y), rc.getMapHeight() - 1));
            dir = Pathfinder.getMoveDir(rc, inBounds);
        }

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
