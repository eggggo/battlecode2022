package mainBot;

import battlecode.common.*;


public class Sage extends RobotPlayer{
    static int turnsAlive = 0;
    static int attackOffset = 0;
    static MapLocation[] enemyArchons = null;
    static int attackLocation = 0;
    //Role: 2 for defense, 1 for attack, 0 for scout
    static int role;
    static MapLocation[] sectorMdpts = new MapLocation[49];
    static MapLocation bestTgtSector = null;
    static MapLocation closestFriendlyArchon = null;
    static MapLocation scoutTgt = null;
    static boolean notRepaired = false;
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

    static boolean isBuilding(RobotInfo unit) {
        return (unit.getType() == RobotType.WATCHTOWER || unit.getType() == RobotType.LABORATORY || unit.getType() == RobotType.ARCHON);
    }

    public static void runSage(RobotController rc) throws GameActionException {
        MapLocation src = rc.getLocation();
        int senseRadius = rc.getType().visionRadiusSquared;
        int radius = rc.getType().actionRadiusSquared;
        Team friendly = rc.getTeam();
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(senseRadius, opponent);
        RobotInfo[] friendlies = rc.senseNearbyRobots(senseRadius, friendly);
        RobotInfo attackTgt = null;
        RobotInfo inVisionTgt = null;
        int unitHP = 0;
        int buildingHP = 0;
        int enemiesKilled = 0;
        int rubbleThreshold = rc.senseRubble(rc.getLocation()) + 20;
        boolean frontline = true;
        int nearbyDamage = 0;
        int nearbyDamageHealth = 0;

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

        int lowestHPTgt = 9999;
        int lowestInVisionHPTgt = 9999;
        if (enemies.length > 0) {
            for (int i = enemies.length - 1; i >= 0; i --) {
                RobotInfo enemy = enemies[i];
                if (enemy.getLocation().distanceSquaredTo(src) <= radius) {
                    if (attackTgt == null ||
                            isHostile(enemy) && !isHostile(attackTgt) ||
                            isHostile(enemy) == isHostile(attackTgt) && enemy.getHealth() < lowestHPTgt) {
                        //note: && has higher precedence than ||

                        lowestHPTgt = enemy.getHealth();
                        attackTgt = enemy;
                    }

                    if ((enemy.getType() == RobotType.ARCHON || enemy.getType() == RobotType.WATCHTOWER || enemy.getType() == RobotType.LABORATORY)) {
                        if (enemy.getMode() == RobotMode.TURRET) {
                            buildingHP += enemy.getType().getMaxHealth(enemy.getLevel());
                        }
                    } else {
                        unitHP += enemy.getType().getMaxHealth(1);
                        if (enemy.getHealth() <= enemy.getType().getMaxHealth(1)*0.22) {
                            enemiesKilled ++;
                        }
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
            if (attackTgt != null && inVisionTgt == null) {
                inVisionTgt = attackTgt;
            }

        }

        //assess teammates
        for (int i = friendlies.length - 1; i >= 0; i --) {
            RobotInfo friend = friendlies[i];
            if (isHostile(friend)) {
                nearbyDamage ++;
                nearbyDamageHealth += friend.getHealth();
                if (frontline && attackTgt != null 
                && friend.location.distanceSquaredTo(attackTgt.location) < src.distanceSquaredTo(attackTgt.location)) {
                    frontline = false;
                }
            }
            if (src.distanceSquaredTo(friend.getLocation()) <= RobotType.SAGE.actionRadiusSquared && isBuilding(friend)) {
                buildingHP -= friend.getType().getMaxHealth(friend.level);
            }
        }
        double averageHealth = (double)nearbyDamageHealth/nearbyDamage;

            //comms access every 3rd turn for bytecode reduction
        if (turnCount % 3 == 0 || turnsAlive == 0) {
            bestTgtSector = null;
            closestFriendlyArchon = null;
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

        //if we reach full health then we are repaired
        if (rc.getHealth() >= RobotType.SAGE.getMaxHealth(rc.getLevel())/2) {
            notRepaired = false;
        }

        //if you can see the scout target sector mdpt, randomize and go to somewhere else
        if (src.distanceSquaredTo(scoutTgt) <= senseRadius) {
            scoutTgt = sectorMdpts[rng.nextInt(49)];
        }

        Direction dir = null;
        //int repairThresh = (int)(15 + (-1.0*(Math.abs(src.x - closestFriendlyArchon.x) + Math.abs(src.y - closestFriendlyArchon.y)))/12);
        if ((notRepaired || rc.getHealth() <= 22) && src.distanceSquaredTo(closestFriendlyArchon) >= 3) {
            dir = Pathfinder.getMoveDir(rc, closestFriendlyArchon, prev5Spots);
            notRepaired = true;
        //if mid hp comparatively or no cd, shuffle
        } else if (inVisionTgt != null && isHostile(inVisionTgt) && ((frontline && rc.getHealth() < averageHealth && rc.getHealth() <= 45) 
                || !rc.isActionReady())) {
            int dx = inVisionTgt.location.x - src.x;
            int dy = inVisionTgt.location.y - src.y;
            MapLocation runawayTgt = new MapLocation(Math.min(Math.max(0, src.x - dx), rc.getMapWidth() - 1), 
            Math.min(Math.max(0, src.y - dy), rc.getMapHeight() - 1));
            prev5Spots = new MapLocation[5];
            dir = Pathfinder.getMoveDir(rc, runawayTgt, prev5Spots);
            MapLocation kitingTgt = src.add(dir);
            if (rc.senseRubble(kitingTgt) > rubbleThreshold) {
                dir = stallOnGoodRubble(rc);
            }
        //if enough soldiers nearby advance to make space
        } else if (inVisionTgt != null && (nearbyDamage >= 4 || src.distanceSquaredTo(inVisionTgt.location) > 14)) {
            dir = Pathfinder.getMoveDir(rc, inVisionTgt.location, prev5Spots);
            MapLocation kitingTgt = src.add(dir);
            if (rc.senseRubble(kitingTgt) > rubbleThreshold) {
                dir = stallOnGoodRubble(rc);
            }
        //otherwise if there is an enemy sector go to best scored one
        } else if (bestTgtSector != null) {
            dir = Pathfinder.getMoveDir(rc, bestTgtSector, prev5Spots);
            if (src.distanceSquaredTo(bestTgtSector) < 50 && rc.senseRubble(src.add(dir)) > rubbleThreshold) {
                dir = stallOnGoodRubble(rc);
            }
          //otherwise scout same as miner
        } else {
            dir = Pathfinder.getMoveDir(rc, scoutTgt, prev5Spots);
        }

        //maximize damage done
        if ( (AnomalyType.CHARGE.sagePercentage * unitHP >= RobotType.SAGE.getDamage(1) || enemiesKilled > 1)
          && rc.canEnvision(AnomalyType.CHARGE)) {
            rc.envision(AnomalyType.CHARGE);
        } else if ((AnomalyType.FURY.sagePercentage * buildingHP >= 60)
          && rc.canEnvision(AnomalyType.FURY)) {
            rc.envision(AnomalyType.FURY);
        } else if (attackTgt != null && rc.canAttack(attackTgt.location)) {
            MapLocation toAttack = attackTgt.location;
            rc.attack(toAttack);
        }

        //determine whether to attack here or after moving
        if (dir != null && rc.canMove(dir)) {
            rc.move(dir);
            prev5Spots[currentOverrideIndex] = rc.getLocation();
            currentOverrideIndex  = (currentOverrideIndex + 1) % 5;
        }

        //post move attack if available
        if (rc.isActionReady() ) { //&& rc.senseRubble(rc.getLocation()) < rubbleThreshold) {
            attackTgt = null;
            src = rc.getLocation();
            enemies = rc.senseNearbyRobots(senseRadius, opponent);
            lowestHPTgt = 9999;
            buildingHP = 0;
            unitHP = 0;
            enemiesKilled = 0;
            if (enemies.length > 0) {
            for (int i = enemies.length - 1; i >= 0; i --) {
                RobotInfo enemy = enemies[i];
                if (enemy.getLocation().distanceSquaredTo(src) <= radius) {
                    if (attackTgt == null ||
                            isHostile(enemy) && !isHostile(attackTgt) ||
                            isHostile(enemy) == isHostile(attackTgt) && enemy.getHealth() < lowestHPTgt) {
                        //note: && has higher precedence than ||

                        lowestHPTgt = enemy.getHealth();
                        attackTgt = enemy;
                    }

                    if ((enemy.getType() == RobotType.ARCHON || enemy.getType() == RobotType.WATCHTOWER || enemy.getType() == RobotType.LABORATORY)) {
                        if (enemy.getMode() == RobotMode.TURRET) {
                            buildingHP += enemy.getType().getMaxHealth(enemy.getLevel());
                        }
                    } else {
                        unitHP += enemy.getType().getMaxHealth(1);
                        if (enemy.getHealth() <= enemy.getType().getMaxHealth(1)*0.22) {
                            enemiesKilled ++;
                        }
                    }
                }
            }
            //maximize damage done
            if ( (AnomalyType.CHARGE.sagePercentage * unitHP >= RobotType.SAGE.getDamage(1) || enemiesKilled > 1)
                    && rc.canEnvision(AnomalyType.CHARGE)) {
                rc.envision(AnomalyType.CHARGE);
            } else if ((AnomalyType.FURY.sagePercentage * buildingHP >= 60)
                    && rc.canEnvision(AnomalyType.FURY)) {
                rc.envision(AnomalyType.FURY);
            } else if (attackTgt != null && rc.canAttack(attackTgt.location)) {
                MapLocation toAttack = attackTgt.location;
                rc.attack(toAttack);
            }
            }
        }

        Comms.updateSector(rc);
        Comms.updateTypeCount(rc);
        turnsAlive ++;
    }
}
