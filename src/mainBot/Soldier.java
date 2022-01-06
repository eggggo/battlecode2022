package mainBot;

import battlecode.common.*;


import java.util.*;

public class Soldier extends RobotPlayer {

  static int turnsAlive = 0;
  static int turnsNotKilledStuff = 0;
  static int attackOffset = 0;
  static MapLocation home = null;
  static MapLocation[] enemyArchons = null;
  static int attackLocation = 0;
  //Role: 2 for defense, 1 for attack, 0 for scout
  static int role;

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

  /**
   * Run a single turn for a Soldier.
   * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
   */
  static void runSoldier(RobotController rc) throws GameActionException {

    int radius = rc.getType().actionRadiusSquared;
    int senseRadius = rc.getType().visionRadiusSquared;
    Team friendly = rc.getTeam();
    Team opponent = rc.getTeam().opponent();
    RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
    int archonCount = 4;
    if (enemies.length > 0) {
      MapLocation toAttack = enemies[0].location;
      if (rc.canAttack(toAttack)) {
        rc.attack(toAttack);
        turnsNotKilledStuff = 0;
      }
    }
    if (turnsAlive == 0) {
      RobotInfo[] nearbyRobots = rc.senseNearbyRobots(senseRadius, friendly);
      initializeSoldier(rc, nearbyRobots);
    }
    //Soldier rules order
    //0: If turn 0, setup home
    //1: If low health, run home
    //2: If enemy, attack
    //3: If an archon is under attack (defense or offense) go help
    //4: Defense pathing/Scout pathing/Attack pathing


    Direction dir = null;
    if (rc.getHealth() < RobotType.SOLDIER.getMaxHealth(rc.getLevel()) / 2 && home != null) { // If low health run home
      dir = Pathfinder.getMoveDir(rc, home);
    } else if (enemies.length > 0) { //If enemy, attack
      MapLocation toAttack = enemies[0].location;
      if (rc.canAttack(toAttack)) {
        rc.attack(toAttack);
        turnsNotKilledStuff = 0;
      }
    }// else if Communicated something is under attack do stuff
    else if (role == 2) {
      //Idk i think defense sucks but maybe we should setup to defend
    } else if (role == 1) { //If attacker, go attack
      //Attacks at one of the random spots of a potential enemy base

      MapLocation attackTarget = enemyArchons[attackLocation];


      //Change target if theres nothing at the target
      if (rc.canSenseLocation(attackTarget)) {
        RobotInfo rb = rc.senseRobotAtLocation(attackTarget);
        if (rb == null || rb.getType() != RobotType.ARCHON) {
          attackOffset += 1;
          attackLocation = (rc.getID() + attackOffset) % (enemyArchons.length);

        }
      }
      dir = Pathfinder.getMoveDir(rc, attackTarget);
    } else if (role == 0) {
      //Same copy of random pathing code that miners have
      //Needs to be fixed cause its random and garbage
      dir = directions[rng.nextInt(directions.length)];


    }
    if (dir != null && rc.canMove(dir)) {
      rc.move(dir);
    }

    turnsNotKilledStuff++;
    turnsAlive++;
    Comms.updateSector(rc);
  }

  static void initializeSoldier(RobotController rc, RobotInfo[] nearbyRobots) throws GameActionException{
    role = rc.getID() % 2;
    int archonCount = 4;
    for (int i = nearbyRobots.length - 1; i >= 0; i--) {
      if (nearbyRobots[i].getType() == RobotType.ARCHON) {
        home = nearbyRobots[i].getLocation();
        break;
      }
    }


    enemyArchons = new MapLocation[archonCount * 3];
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
      int[] sector = Comms.readSectorInfo(rc, i);
      if (sector[0] == 1) {
        MapLocation mdpt = Comms.sectorMidpt(rc, i);
        quads[currentArchonIndex] = getQuadrant(rc, mdpt.x, mdpt.y);
        coords[currentArchonIndex] = mdpt;
        currentArchonIndex++;
      }
    }
    if (coords[1] == null) {
      archonCount = 1;
      int[] tempQuads = new int[1];
      MapLocation[] tempCoords = new MapLocation[1];
      tempQuads[0] = quads[0];
      quads = tempQuads;
      tempCoords[0] = coords[0];
      coords = tempCoords;
    } else if (coords[2] == null) {
      archonCount = 2;
      int[] tempQuads = new int[2];
      MapLocation[] tempCoords = new MapLocation[2];
      tempQuads[0] = quads[0];
      tempQuads[1] = quads[1];
      quads = tempQuads;
      tempCoords[0] = coords[0];
      tempCoords[1] = coords[1];
      coords = tempCoords;
    } else if (coords[3] == null) {
      archonCount = 3;
      int[] tempQuads = new int[3];
      MapLocation[] tempCoords = new MapLocation[3];
      tempQuads[0] = quads[0];
      tempQuads[1] = quads[1];
      tempQuads[2] = quads[2];
      quads = tempQuads;
      tempCoords[0] = coords[0];
      tempCoords[1] = coords[1];
      tempCoords[2] = coords[2];
      coords = tempCoords;
    }

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
      //System.out.println("Rotational Symmetry");
      for (int i = archonCount - 1; i >= 0; i--) {
        enemyArchons[3 * i] = new MapLocation(coords[i].x, rc.getMapHeight() - 1 - coords[i].y); //vert flip
        enemyArchons[3 * i + 1] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, coords[i].y); // horz flip
        enemyArchons[3 * i + 2] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, rc.getMapHeight() - 1 - coords[i].y); // 180 rotate
      }

    } else if (quad1 && quad2 || quad3 && quad4) {
      //System.out.println("Horizontal Symmetry or Rotational Symmetry");
      //thought that it was unecessary to check horizontal when its likely horizontally symmetric, so i just copied another vertical fip instead of using horz
      for (int i = archonCount - 1; i >= 0; i--) {
        enemyArchons[3 * i] = new MapLocation(coords[i].x, rc.getMapHeight() - 1 - coords[i].y); //vert flip
        enemyArchons[3 * i + 1] = new MapLocation(coords[i].x, rc.getMapHeight() - 1 - coords[i].y); // vert flip
        enemyArchons[3 * i + 2] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, rc.getMapHeight() - 1 - coords[i].y); // 180 rotate
      }
    } else if (quad1 && quad4 || quad2 && quad3) {
      //System.out.println("Vertical Symmetry or Rotational Symmetry");
      //thought that it was unecessary to check vertical when its likely vertically symmetric, so i just copied another horiz fip instead of using vert
      for (int i = archonCount - 1; i >= 0; i--) {
        enemyArchons[3 * i] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, coords[i].y); //horz flip
        enemyArchons[3 * i + 1] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, coords[i].y); // horz flip
        enemyArchons[3 * i + 2] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, rc.getMapHeight() - 1 - coords[i].y); // 180 rotate
      }
    } else {
      //System.out.println("only in one quad so cannot tell");
      for (int i = archonCount - 1; i >= 0; i--) {
        enemyArchons[3 * i] = new MapLocation(coords[i].x, rc.getMapHeight() - 1 - coords[i].y); //vert flip
        enemyArchons[3 * i + 1] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, coords[i].y); // horz flip
        enemyArchons[3 * i + 2] = new MapLocation(rc.getMapWidth() - 1 - coords[i].x, rc.getMapHeight() - 1 - coords[i].y); // 180 rotate
      }
    }

  }
}
