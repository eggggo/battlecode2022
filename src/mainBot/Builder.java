package mainBot;

import battlecode.common.*;

public class Builder extends RobotPlayer {

    static int laboratoriesBuilt = 0;
    static int watchtowersBuilt = 0;
    static int turnsAlive = 0;
    static MapLocation[] sectorMdpts = new MapLocation[49];
    static boolean firstEnemySeen = false;
    static Direction awayFromEnemies = null;
    static int sageCount = 0;
    static int wtCount = 0;
    static int minerCount = 0;
    static int labCount = 0;
    static int currentIncome = 0;
    static MapLocation bestTgtSector = null;
    static MapLocation nearestFriendlyArchon = null;
    static MapLocation[] prev5Spots = new MapLocation[5];
    static int currentOverrideIndex = 0;

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

      static boolean isHostile(RobotInfo enemy) {
        return (enemy.getType() == RobotType.SOLDIER || enemy.getType() == RobotType.SAGE 
        || enemy.getType() == RobotType.WATCHTOWER);
      }

    public static void runBuilder(RobotController rc) throws GameActionException {
        MapLocation src = rc.getLocation();
        int senseRadius = rc.getType().visionRadiusSquared;
        Team friendly = rc.getTeam();
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(senseRadius, friendly);
        RobotInfo[] enemies = rc.senseNearbyRobots(senseRadius, friendly.opponent());
        MapLocation closestAttacker = null;
        RobotInfo nearestWt = null;
        RobotInfo nearestLab = null;
        RobotInfo nearestArchon = null;
        RobotInfo nearestPrototype = null;
        int awayFromLabX = 0;
        int awayFromLabY = 0;

        if (!firstEnemySeen) {
            for (int i = 48; i >= 0; i--) {
                if (Comms.readSectorInfo(rc, i, 3) > 0) {
                    firstEnemySeen = true;
                    break;
                }
            }
        }

        int initLabCount = 1;

        //on odd read turns get relevant info for building
        if (rc.getRoundNum() % 2 == 1) {
            sageCount = rc.readSharedArray(53);
            wtCount = rc.readSharedArray(52);
            minerCount = rc.readSharedArray(50);
            labCount = rc.readSharedArray(56);
            currentIncome = rc.readSharedArray(49);
        }

        //initial setup
        if (turnsAlive == 0) {
            int xVector = 0;
            int yVector = 0;
            for (int i = 48; i >= 0; i --) {
                sectorMdpts[i] = Comms.sectorMidpt(rc, i);
                if (Comms.readSectorInfo(rc, i, 1) == 1) {
                    xVector += src.directionTo(sectorMdpts[i]).opposite().dx;
                    yVector += src.directionTo(sectorMdpts[i]).opposite().dy;
                }
            }
            awayFromEnemies = src.directionTo(src.translate(xVector, yVector));         
        }

        //finding closest enemy dps
        for (int i = enemies.length - 1; i >= 0; i --) {
            RobotInfo enemy = enemies[i];
            if ((isHostile(enemy)) && (closestAttacker == null
            || enemy.location.distanceSquaredTo(src) < closestAttacker.distanceSquaredTo(src))) {
                closestAttacker = enemy.location;
            }
        }

        //comms access every other turn for bytecode reduction
        if (turnCount % 3 == 0 || turnsAlive == 0) {
            bestTgtSector = null;
            nearestFriendlyArchon = null;
            double highScore = 0;
            for (int i = 48; i >= 0; i --) {
                int enemyArchon = Comms.readSectorInfo(rc, i, 1);
                int enemyScore = Comms.readSectorInfo(rc, i, 3);
                int homeArchon = Comms.readSectorInfo(rc, i, 0);
                MapLocation mdpt = sectorMdpts[i];
                if (enemyArchon > 0 || enemyScore > 0) {
                    double currentScore = (10.0*enemyArchon + enemyScore)/(src.distanceSquaredTo(mdpt));
                    if (currentScore > highScore) {
                        bestTgtSector = mdpt;
                    }
                }
                if (homeArchon > 0) {
                    if (nearestFriendlyArchon == null || 
                    mdpt.distanceSquaredTo(src) < src.distanceSquaredTo(nearestFriendlyArchon)) {
                        nearestFriendlyArchon = mdpt;
                    }
                }
            }
        }

        //Assess buildings in surroundings
        for (int i = nearbyRobots.length - 1; i >= 0; i--) {
            RobotInfo unit = nearbyRobots[i];
            if (unit.getMode() == RobotMode.PROTOTYPE && (nearestPrototype == null 
            || nearestPrototype.getHealth() > unit.getHealth())) {
                nearestPrototype = unit;
            } else if (unit.getType() == RobotType.WATCHTOWER 
            && (nearestWt == null || unit.getHealth() < nearestWt.getHealth())) {
                nearestWt = unit;
            } else if (unit.getType() == RobotType.LABORATORY) { 
                if (nearestLab == null || unit.getHealth() < nearestLab.getHealth()) {
                    nearestLab = unit;
                }
                awayFromLabX += src.directionTo(unit.location).opposite().dx;
                awayFromLabY += src.directionTo(unit.location).opposite().dy;
            } else if (unit.getType() == RobotType.ARCHON && (nearestArchon == null 
            || nearestArchon.getHealth() > unit.getHealth())) {
                nearestArchon = unit;
            }
        }

        Direction dir = null;
        Direction stallDir = stallOnGoodRubble(rc);

        Direction center = rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
        Direction builddir = center;

        double rubble = 200;
        for (Direction dire : Direction.allDirections()) {
            MapLocation loc = src.add(dire);
            if (rc.onTheMap(loc) && rc.senseRobotAtLocation(loc) == null) {
                if (rc.senseRubble(loc) < rubble) {
                    rubble = rc.senseRubble(loc);
                    builddir = dire;
                }
            }
        }

        boolean buildLab = laboratoriesBuilt == 0 && ((minerCount / 10 + initLabCount > labCount)
                || (rc.getTeamLeadAmount(rc.getTeam()) >= 180 && !(sageCount > (15 * Math.max(1, wtCount)))));
        boolean buildWt = (sageCount > (15 * (wtCount+1))) && (watchtowersBuilt < 3);
        buildWt = false;

        //movement flow
        //if unfinished building nearby finish it
        if (nearestPrototype != null) {
            if (!(src.distanceSquaredTo(nearestPrototype.location) <= 5 && stallDir == Direction.CENTER)) {
                dir = Pathfinder.getMoveDir(rc, nearestPrototype.location, prev5Spots);
            } else {
                dir = stallDir;
            }
        //if watchtower nearby follow it to battle
        } else if (nearestWt != null && src.distanceSquaredTo(nearestWt.location) > 5) {
            dir = Pathfinder.getMoveDir(rc, nearestWt.location, prev5Spots);
        //if enemies nearby run
        } else if (closestAttacker != null) {
            int dx = closestAttacker.x - src.x;
            int dy = closestAttacker.y - src.y;
            MapLocation runawayTgt = new MapLocation(Math.min(Math.max(0, src.x - dx), rc.getMapWidth() - 1), 
            Math.min(Math.max(0, src.y - dy), rc.getMapHeight() - 1));
            prev5Spots = new MapLocation[5];
            dir = Pathfinder.getMoveDir(rc, runawayTgt, prev5Spots);
        //if archon nearby low go heal it(TODO: add mutating conditions here as well)
        } else if (nearestArchon != null && nearestArchon.getHealth() < nearestArchon.getType().getMaxHealth(nearestArchon.level)) {
            if (!(src.distanceSquaredTo(nearestArchon.location) <= 5 && stallDir == Direction.CENTER)) {
                dir = Pathfinder.getMoveDir(rc, nearestArchon.location, prev5Spots);
            } else {
                dir = stallDir;
            }
        //if we want to build a lab run away from civilization
        } else if (buildLab) {
            MapLocation tgt = null;
            if (awayFromLabX == 0 && awayFromLabY == 0) {
                tgt = src.translate(3*awayFromEnemies.dx, 3*awayFromEnemies.dy);
            } else {
                tgt = src.translate(3*awayFromLabX, 3*awayFromLabY);
            }
            MapLocation inBounds = new MapLocation(Math.min(Math.max(0, tgt.x), rc.getMapWidth() - 1), 
                Math.min(Math.max(0, tgt.y), rc.getMapHeight() - 1));
            dir = Pathfinder.getMoveDir(rc, inBounds, prev5Spots);
        //if we want to build a wt run towards enemies
        } else if (buildWt && bestTgtSector != null) {
            rc.setIndicatorString("build wt");
            if (src.distanceSquaredTo(bestTgtSector) < 150) {
                dir = stallDir;
            } else {
                dir = Pathfinder.getMoveDir(rc, bestTgtSector, prev5Spots);
            }
        }
//        //If you've built your lab, start following the nearest friendly archon
//        else if (nearestFriendlyArchon != null && src.distanceSquaredTo(nearestFriendlyArchon) > 15 && laboratoriesBuilt > 0) {
//            dir = Pathfinder.getMoveDir(rc, nearestFriendlyArchon, prev5Spots);
//        }
        //else, default behavior, currently same as running from civilization to build labs
        else {
            MapLocation tgt = null;
            if (awayFromLabX == 0 && awayFromLabY == 0) {
                tgt = src.translate(3*awayFromEnemies.dx, 3*awayFromEnemies.dy);
            } else {
                tgt = src.translate(3*awayFromLabX, 3*awayFromLabY);
            }
            MapLocation inBounds = new MapLocation(Math.min(Math.max(0, tgt.x), rc.getMapWidth() - 1),
                    Math.min(Math.max(0, tgt.y), rc.getMapHeight() - 1));
            if (inBounds.equals(src)) {
                dir = Direction.values()[rng.nextInt(9)];
            } else {
                dir = Pathfinder.getMoveDir(rc, inBounds, prev5Spots);
            }
        }


        //action flow. note that these are not else ifs since we can have a nearby building of higher prio but do nothing to it
        //and instead do something to another lower prio building
        //repair prototypes
        if (rc.isActionReady() && (nearestPrototype != null) && (rc.canRepair(nearestPrototype.location))) {
            rc.repair(nearestPrototype.location);
        }

        //mutate/repair a watchtower
        if (nearestWt != null && rc.canRepair(nearestWt.location) && nearestWt.getHealth() < RobotType.WATCHTOWER.getMaxHealth(nearestWt.level)) {
            rc.repair(nearestWt.location);
        }
        //mutate/repair a lab
        else if (nearestLab != null && rc.canRepair(nearestLab.location)
                && nearestLab.getHealth() < RobotType.LABORATORY.getMaxHealth(nearestLab.level)) {
                rc.repair(nearestLab.location);
        }
        //mutate/repair an archon
        else if (nearestArchon != null && rc.canRepair(nearestArchon.location)
                && nearestArchon.getHealth() < RobotType.ARCHON.getMaxHealth(nearestArchon.level)) {
                rc.repair(nearestArchon.location);
        }
        else if (nearestWt != null && rc.canMutate(nearestWt.location)) { //TODO: conditions for should mutate
            rc.mutate(nearestWt.location);
            rc.writeSharedArray(55, (rc.readSharedArray(55) & 0b1111111));
        }
        else if (buildWt) {
            if (rc.canBuildRobot(RobotType.WATCHTOWER, builddir)) {
                rc.buildRobot(RobotType.WATCHTOWER, builddir);
                watchtowersBuilt++;
            }
        }
        //build a lab
        else if (buildLab) {
            if (rc.canBuildRobot(RobotType.LABORATORY, builddir)) {
                rc.buildRobot(RobotType.LABORATORY, builddir);
                laboratoriesBuilt++;
                rc.writeSharedArray(55, (rc.readSharedArray(55) & 0b1111111));
            }
        }
        else if (nearestLab != null && rc.canMutate(nearestLab.location) && labCount > (rc.getArchonCount() + 1)) { //TODO: conditions for should mutate
            rc.mutate(nearestLab.location);
            rc.writeSharedArray(55, (rc.readSharedArray(55) & 0b1111111));
        }
        else if (nearestArchon != null && rc.canMutate(nearestArchon.location)) { //TODO: conditions for should mutate
            rc.mutate(nearestArchon.location);
            rc.writeSharedArray(55, (rc.readSharedArray(55) & 0b1111111));
        }

        if (dir != null && rc.canMove(dir)) {
            rc.move(dir);
            prev5Spots[currentOverrideIndex] = rc.getLocation();
            currentOverrideIndex  = (currentOverrideIndex + 1) % 5;
        }

        //Comms stuff
        Comms.updateSector(rc);
        Comms.updateTypeCount(rc);
        turnsAlive ++;
    }
}
