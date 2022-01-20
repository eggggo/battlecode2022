package v8;

import battlecode.common.*;

public class Miner extends RobotPlayer {

    static int prevIncome = 0;
    static boolean aboveHpThresh = true;
    static int turnsAlive = 0;
    static MapLocation[] sectorMdpts = new MapLocation[49];
    static Direction bounceDir = null;
    static int maxTravelDistance = 100;
    static Direction spawnDir = null;
    static int sectorNumber = -1;
    static MapLocation home = null;
    static MapLocation closestEnemyArchon = null;
    static int scoutPattern = 0;
    static MapLocation scoutTgt = null;
    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */

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

    static boolean isHostile(RobotInfo enemy) {
        return (enemy.getType() == RobotType.SOLDIER || enemy.getType() == RobotType.SAGE 
        || enemy.getType() == RobotType.WATCHTOWER);
    }

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
        Team opponent = rc.getTeam().opponent();
        MapLocation src = rc.getLocation();
        Direction dir = null;
        MapLocation resources = null;
        int mapArea = rc.getMapHeight() * rc.getMapWidth();

        if (turnsAlive == 0) {
            for (int i = 48; i >= 0; i --) {
                sectorMdpts[i] = Comms.sectorMidpt(rc, i);
            }
            RobotInfo[] spawn = rc.senseNearbyRobots(2, friendly);
            for (int i = spawn.length - 1; i >= 0; i --) {
                RobotInfo robot = spawn[i];
                if (robot.getType() == RobotType.ARCHON) {
                    spawnDir = rc.getLocation().directionTo(robot.location).opposite();
                    home = robot.getLocation();
                    break;
                }
            }
            scoutPattern = rc.readSharedArray(50) % 3;
            bounceDir = spawnDir;
            scoutTgt = new MapLocation(rc.getMapWidth() - 1 - src.x, rc.getMapHeight() - 1 - src.y);
        }

        int distanceFromSpawn = 0;
        boolean nearbyEnemy = false;
        boolean nearbyFriend = true;
        if (home != null) {
            distanceFromSpawn = rc.getLocation().distanceSquaredTo(home);
        }
        RobotInfo[] enemies = rc.senseNearbyRobots(senseRadius, opponent);
        if (enemies.length > 0) {
            nearbyEnemy = true;
        }
        RobotInfo[] friendlies = rc.senseNearbyRobots(senseRadius, friendly);
        if (friendlies.length == 0) {
            nearbyFriend = false;
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
                boolean shouldContinue = nearbyEnemy && distanceFromSpawn > (.75 * Math.sqrt(mapArea)) * (.75 * Math.sqrt(mapArea));
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

        //run away from enemy attackers if we see them
        MapLocation closestAttacker = null;
        MapLocation[] nearbyLead = rc.senseNearbyLocationsWithLead(senseRadius, 5);
        MapLocation[] nearbyGold = rc.senseNearbyLocationsWithGold(senseRadius);
        int nearbyFriendlySoldierCount = 0;
        MapLocation nearestFriendlySoldier = null;
        int nearbyFriendlyMinerCount = 0;
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
            if (leadCount > highLead) {
                highLead = leadCount;
                resources = nearbyLead[i];
            }
        }
        for (int i = friendlies.length - 1; i >= 0; i --) {
            RobotInfo friend = friendlies[i];
            if (isHostile(friend)) {
                nearbyFriendlySoldierCount ++;
                if (nearestFriendlySoldier == null || friend.location.distanceSquaredTo(src) < nearestFriendlySoldier.distanceSquaredTo(src)) {
                    nearestFriendlySoldier = friend.location;
                }
            } else if (friend.getType() == RobotType.MINER && (nearestFriendlySoldier == null 
            || friend.location.distanceSquaredTo(nearestFriendlySoldier) < src.distanceSquaredTo(nearestFriendlySoldier))) {
                nearbyFriendlyMinerCount ++;
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
            bounceDir = dir;
        //if good resources nearby go there
        } else if (resources != null && ((nearbyLead.length + nearbyGold.length) > 2*nearbyFriendlyMinerCount || highLead > 100)) {
            dir = Pathfinder.getMoveDir(rc, resources);
        //else go to sector with reported resources + spread vector + away from closest archon vector
        } else if (nearbyFriendlySoldierCount > nearbyFriendlyMinerCount) {
            if (src.distanceSquaredTo(nearestFriendlySoldier) <= 8) {
                dir = stallOnGoodRubble(rc);
            } else {
                dir = Pathfinder.getMoveDir(rc, nearestFriendlySoldier);
            }
        } else {
            if (turnCount % 2 == 0) {
                sectorNumber = -1;
                int bestResource = 0;
                for (int i = 48; i >= 0; i--) {
                    int[] sector = Comms.readSectorInfo(rc, i);
                    int distanceToSector = rc.getLocation().distanceSquaredTo(sectorMdpts[i]);
                    if (sector[2] > bestResource
                        && maxTravelDistance >= distanceToSector) {
                        sectorNumber = i;
                        bestResource = sector[2];
                    }
                    if (sector[1] > 0 && (closestEnemyArchon == null 
                    || src.distanceSquaredTo(closestEnemyArchon) > src.distanceSquaredTo(sectorMdpts[i]))) {
                        closestEnemyArchon = sectorMdpts[i];
                    }
                }
            }
            if (sectorNumber != -1 || nearbyFriendlyMinerCount > 1) {
                double xVector = 0;
                double yVector = 0;
                if (sectorNumber != -1) {
                    dir = rc.getLocation().directionTo(sectorMdpts[sectorNumber]);
                    xVector = 2*dir.dx;
                    yVector = 2*dir.dy;
                }
                for (int i = friendlies.length - 1; i >= 0; i --) {
                    MapLocation friendlyLoc = friendlies[i].getLocation();
                    double d = Math.sqrt(src.distanceSquaredTo(friendlyLoc));
                    Direction opposite = src.directionTo(friendlyLoc).opposite();
                    xVector += opposite.dx*(4.0/d);
                    yVector += opposite.dy*(4.0/d);
                }
                MapLocation vectorTgt = src.translate((int)Math.ceil(xVector), (int)Math.ceil(yVector));
                MapLocation inBounds = new MapLocation(Math.min(Math.max(0, vectorTgt.x), rc.getMapWidth() - 1), 
                Math.min(Math.max(0, vectorTgt.y), rc.getMapHeight() - 1));
                dir = Pathfinder.getMoveDir(rc, inBounds);
                bounceDir = dir;
            } else {
                if (scoutPattern == 0) {
                    if (!rc.onTheMap(src.add(src.directionTo(scoutTgt)))) {
                        scoutTgt = new MapLocation(rc.getMapWidth() - 1 - src.x, rc.getMapHeight() - 1 - src.y);
                    }
                    dir = Pathfinder.getMoveDir(rc, scoutTgt);
                } else if (scoutPattern == 1) {
                    if (closestEnemyArchon != null) {
                        dir = Pathfinder.getMoveDir(rc, closestEnemyArchon);
                    } else {
                        dir = Pathfinder.getMoveDir(rc, scoutTgt);
                    }
                } else {
                    if (!rc.onTheMap(src.add(bounceDir))) {
                        bounceDir = bounceDir.opposite();
                    }
                    MapLocation tgt = src.add(bounceDir).add(bounceDir);
                    MapLocation inBounds = new MapLocation(Math.min(Math.max(0, tgt.x), rc.getMapWidth() - 1), 
                    Math.min(Math.max(0, tgt.y), rc.getMapHeight() - 1));
                    dir = Pathfinder.getMoveDir(rc, inBounds);
                }
            }
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
