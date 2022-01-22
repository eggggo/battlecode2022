package mainBot;

import battlecode.common.*;

public class Sage extends RobotPlayer{
    static int turnsAlive = 0;
    static int attackOffset = 0;
    static MapLocation home = null;
    static MapLocation[] enemyArchons = null;
    static int attackLocation = 0;
    //Role: 2 for defense, 1 for attack, 0 for scout
    static int role;
    static MapLocation[] sectorMdpts = new MapLocation[49];
    static MapLocation scoutTgt = null;
    static int turnsSinceSeenHostile = 0;
    static boolean notRepaired = false;
    static MapLocation bestTgtSector = null;
    static MapLocation closestFriendlyArchon = null;

    //set upon initialization
    static int senseRadius;
    static int actionRadius;
    static Team friendly;
    static Team opponent;

    /**
     * @return the direction of the least rubble to move
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

    public static void runSage(RobotController rc) throws GameActionException {
        MapLocation src = rc.getLocation();
        int rubbleThreshold = rc.senseRubble(rc.getLocation()) + 20;

        //initialization
        if (turnsAlive == 0) {
            senseRadius = rc.getType().visionRadiusSquared;
            actionRadius = rc.getType().actionRadiusSquared;
            friendly = rc.getTeam();
            opponent = rc.getTeam().opponent();

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

        //determined every turn
        RobotInfo[] enemies = rc.senseNearbyRobots(senseRadius, opponent);
        RobotInfo attackTgt = null; //lowest health enemy within actionRadius
        RobotInfo inVisionTgt = null; //lowest health enemy within visionRadius
        int unitHP = 0;
        int buildingHP = 0;
        boolean sageOrWTSeen = false;

        //attack/vision targeting code
        int lowestHPTgt = 9999;
        int lowestInVisionHPTgt = 9999;
        if (enemies.length > 0) {
            for (int i = enemies.length - 1; i >= 0; i --) {
                RobotInfo enemy = enemies[i];
                sageOrWTSeen = (enemy.getType() == RobotType.SAGE || enemy.getType() == RobotType.WATCHTOWER) ? true:false;
                if (enemy.getLocation().distanceSquaredTo(src) <= actionRadius) {
                    if (attackTgt == null ||
                        isHostile(enemy) && !isHostile(attackTgt) ||
                        isHostile(enemy) == isHostile(attackTgt) && enemy.getHealth() < lowestHPTgt) {
                        //note: && has higher precedence than ||

                        lowestHPTgt = enemy.getHealth();
                        attackTgt = enemy;
                    }

                    if (enemy.getType() == RobotType.ARCHON || enemy.getType() == RobotType.WATCHTOWER || enemy.getType() == RobotType.LABORATORY) {
                        buildingHP += enemy.getType().getMaxHealth(enemy.getLevel());
                    } else {
                        unitHP += enemy.getType().getMaxHealth(1);
                    }
                } else {
                    if (inVisionTgt == null ||
                        isHostile(enemy) && !isHostile(inVisionTgt) ||
                        isHostile(enemy) == isHostile(inVisionTgt) && enemy.getHealth() < lowestInVisionHPTgt) {

                        lowestInVisionHPTgt = enemy.getHealth();
                        inVisionTgt = enemy;
                    }
                }
            }
            if (attackTgt != null) {
                inVisionTgt = attackTgt;
            }

        }

        //comms access every 3rd turn for bytecode reduction
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
                    double currentScore = (50.0*enemyArchon + enemyInSector)/Math.sqrt(src.distanceSquaredTo(sectorMdpts[i]));
                    if (currentScore > highScore) {
                        bestTgtSector = sectorMdpts[i];
                    }
                }
            }
        }

        //if you can see the scout target sector mdpt, randomize and go to somewhere else
        if (src.distanceSquaredTo(scoutTgt) <= senseRadius) {
            scoutTgt = sectorMdpts[rng.nextInt(49)];
        }

        //if we reach full health then we are repaired
        if (rc.getHealth() >= RobotType.SAGE.getMaxHealth(rc.getLevel()) * 0.75) {
            notRepaired = false;
        }

        //Movement Code
        Direction dir = null;
        int repairThresh = (int)(15 + (-1.0*(Math.abs(src.x - closestFriendlyArchon.x) + Math.abs(src.y - closestFriendlyArchon.y)))/12);
        if ((notRepaired || rc.getHealth() < repairThresh && closestFriendlyArchon != null)) {
            // If low health run home (for now its go suicide)
            dir = Pathfinder.getMoveDir(rc, closestFriendlyArchon);
            notRepaired = true;
        } else if (inVisionTgt != null && attackTgt != null && !rc.isActionReady()) {
            //enemy is in action radius, but no cooldown to attack
            if(isHostile(inVisionTgt) || rc.getActionCooldownTurns() > 20){
                //if we see hostile enemies or our action cooldown is high, run
                Direction opposite = src.directionTo(inVisionTgt.location).opposite();
                MapLocation runawayTgt = src.add(opposite).add(opposite);
                runawayTgt = new MapLocation(Math.min(Math.max(0, runawayTgt.x), rc.getMapWidth() - 1),
                        Math.min(Math.max(0, runawayTgt.y), rc.getMapHeight() - 1));
                dir = Pathfinder.getMoveDir(rc, runawayTgt);
            }
            else {
                //not hostile and we have low action cooldown, so chase
                dir = Pathfinder.getMoveDir(rc, inVisionTgt.location);
                int chaseSpotRubble = rc.senseRubble(src.add(dir));
                if (chaseSpotRubble > rubbleThreshold && isHostile(inVisionTgt)) {
                    dir = stallOnGoodRubble(rc);
                }
            }
        } else if (inVisionTgt != null && attackTgt != null && rc.isActionReady()) {
            //enemy is in action radius, and we do have cooldown to attack

            if(!sageOrWTSeen && src.distanceSquaredTo(attackTgt.location) > 15){
                dir = stallOnGoodRubble(rc);
            }
            else{
                //maximize damage done
                if ( AnomalyType.CHARGE.sagePercentage * unitHP >= RobotType.SAGE.getDamage(1)
                        && rc.canEnvision(AnomalyType.CHARGE)) {
                    rc.envision(AnomalyType.CHARGE);
                } else if ((AnomalyType.FURY.sagePercentage * buildingHP >= 60)
                        && rc.canEnvision(AnomalyType.FURY)) {
                    rc.envision(AnomalyType.FURY);
                } else if (attackTgt != null && rc.canAttack(attackTgt.location)) {
                    MapLocation toAttack = attackTgt.location;
                    rc.attack(toAttack);
                }

                //run!
                Direction opposite = src.directionTo(inVisionTgt.location).opposite();
                MapLocation runawayTgt = src.add(opposite).add(opposite);
                runawayTgt = new MapLocation(Math.min(Math.max(0, runawayTgt.x), rc.getMapWidth() - 1),
                        Math.min(Math.max(0, runawayTgt.y), rc.getMapHeight() - 1));
                dir = Pathfinder.getMoveDir(rc, runawayTgt);
            }

        } else if (inVisionTgt != null && attackTgt == null && rc.getActionCooldownTurns() <= 20){
            //enemy is only in vision radius, and can attack by next turn, find good rubble

            if(!sageOrWTSeen){
                dir = Pathfinder.getMoveDir(rc, inVisionTgt.location);
                int chaseSpotRubble = rc.senseRubble(src.add(dir));
                if (chaseSpotRubble > rubbleThreshold && isHostile(inVisionTgt)) {
                    dir = stallOnGoodRubble(rc);
                }
            }
            else{
                dir = stallOnGoodRubble(rc);
            }

        } else if(inVisionTgt != null && attackTgt == null && rc.getActionCooldownTurns() > 20){
            //enemy is only in vision radius, and we cant attack by next turn, run!
            Direction opposite = src.directionTo(inVisionTgt.location).opposite();
            MapLocation runawayTgt = src.add(opposite).add(opposite);
            runawayTgt = new MapLocation(Math.min(Math.max(0, runawayTgt.x), rc.getMapWidth() - 1),
                    Math.min(Math.max(0, runawayTgt.y), rc.getMapHeight() - 1));
            dir = Pathfinder.getMoveDir(rc, runawayTgt);

        } else if (bestTgtSector != null) {
            dir = Pathfinder.getMoveDir(rc, bestTgtSector);
          //otherwise scout same as miner
        } else {
            dir = Pathfinder.getMoveDir(rc, scoutTgt);
        }

        if (inVisionTgt != null && isHostile(inVisionTgt)) {
            turnsSinceSeenHostile = 0;
        } else{
            turnsSinceSeenHostile++;
        }

        if(turnsSinceSeenHostile < 2 && rc.senseRubble(src.add(dir)) > 75){
            dir = stallOnGoodRubble(rc);
        }

        if (dir != null && rc.canMove(dir)) {
            rc.move(dir);
        }

        Comms.updateSector(rc);
        Comms.updateTypeCount(rc);
        turnsAlive ++;
    }
}
