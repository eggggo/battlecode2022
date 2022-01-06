package mainBot;
import battlecode.common.*;

public class Comms {
    static void updateCommArray(RobotController rc) throws GameActionException {
        MapLocation src = rc.getLocation();
        for (int i = 63; i >= 0; i --) {
            int entry = rc.readSharedArray(i);
            if (entry == 0) {
                continue;
            }
            int code = entry << 12;
            MapLocation tgt = new MapLocation((entry >> 6) & 0b111111, entry & 0b111111);
            if (!rc.canSenseLocation(tgt)) {
                continue;
            } else {
                switch (code) {
                    case 1:
                        //if theres no longer a friendly archon here then clear the entry
                        Team friendly = rc.getTeam();
                        if (!rc.canSenseRobotAtLocation(tgt)) {
                            rc.writeSharedArray(i, 0);
                        } else {
                            RobotInfo robot = rc.senseRobotAtLocation(tgt);
                            if (!(robot.getType() == RobotType.ARCHON && robot.getTeam() == friendly)) {
                                rc.writeSharedArray(i, 0);
                            }
                        }
                    case 2:
                        //if theres no longer an enemy archon here then clear the entry
                        Team enemy = rc.getTeam().opponent();
                        if (!rc.canSenseRobotAtLocation(tgt)) {
                            rc.writeSharedArray(i, 0);
                        } else {
                            RobotInfo robot = rc.senseRobotAtLocation(tgt);
                            if (!(robot.getType() == RobotType.ARCHON && robot.getTeam() == enemy)) {
                                rc.writeSharedArray(i, 0);
                            }
                        }
                    break;
                    case 3:
                        //if theres no longer resource above a threshold here then clear the entry
                        //replace with rc.senseNearbyLocationsWithLead after it works
                        /*
                        MapLocation[] nearbyLead = rc.senseNearbyLocationsWithLead(rc.getType().visionRadiusSquared);
                        int leadCount = 0;
                        for (int j = nearbyLead.length - 1; j >= 0; j --) {
                            leadCount += rc.senseLead(nearbyLead[j]);
                        }
                        if (!(rc.senseNearbyLocationsWithGold(rc.getType().visionRadiusSquared).length > 0 || leadCount > 100)) {
                            rc.writeSharedArray(i, 0);
                        }*/
                        int lead = rc.senseLead(tgt);
                        int gold = rc.senseGold(tgt);
                        if (!(gold > 0 || lead > 50)) { //single patch 50, whole area probably at least 100+
                            rc.writeSharedArray(i, 0);
                        }
                    break;
                    case 4:
                        //if theres no longer an attacking enemy here then clear entry
                        enemy = rc.getTeam().opponent();
                        if (!rc.canSenseRobotAtLocation(tgt)) {
                            rc.writeSharedArray(i, 0);
                        } else {
                            RobotInfo robot = rc.senseRobotAtLocation(tgt);
                            if (!((robot.getType() == RobotType.SOLDIER 
                                || robot.getType() == RobotType.SAGE 
                                || robot.getType() == RobotType.WATCHTOWER) && robot.getTeam() == enemy)) {
                                rc.writeSharedArray(i, 0);
                            }
                        }
                    break;
                }
            }
        }
    }

    static void writeToCommArray(RobotController rc, int code, int x, int y) throws GameActionException {
        int msg = (code << 12) | ((x << 6) | y);
        MapLocation current = new MapLocation(x, y);
        //if this is an archon sighting it is IMPORTANT
        if (code <= 2) {
            int firstFreeIndex = -1;
            //1 pass to both find 1st free index to put this sighting in(non archon sighting msg) and 
            //if the sighting already exists dont do anything
            for (int i = 63; i >= 0; i --) {
                int entry = rc.readSharedArray(i);
                if ((entry >> 12 > 2 || entry == 0) && firstFreeIndex == -1) {
                    firstFreeIndex = i;
                } else if (entry >> 12 == code) {
                    MapLocation entryLoc = new MapLocation((entry >> 6) & 0b111111, entry & 0b111111);
                    if (entryLoc.distanceSquaredTo(current) <= 40) {
                        firstFreeIndex = -2;
                        break;
                    }
                }
            }
            //write to first free non priority index if new sighting
            if (firstFreeIndex >= 0) {
                rc.writeSharedArray(firstFreeIndex, msg);
            }
        //if this is a resource/enemy sighting we only insert it if there is a free spot or dont insert it if
        //it is the same sighting
        } else {
            int firstFreeIndex = -1;
            for (int i = 63; i >= 0; i --) {
                int entry = rc.readSharedArray(i);
                if (entry == 0 && firstFreeIndex == -1) {
                    firstFreeIndex = i;
                } else if (entry >> 12 == code) {
                    MapLocation entryLoc = new MapLocation((entry >> 6) & 0b111111, entry & 0b111111);
                    if (entryLoc.distanceSquaredTo(current) <= 40) {
                        firstFreeIndex = -2;
                        break;
                    } 
                }
            }
            //write to first free index if new sighting
            if (firstFreeIndex >= 0) {
                rc.writeSharedArray(firstFreeIndex, msg);
            }
        }
    }

    static int[] readFromCommsArray(RobotController rc, int index) throws GameActionException {
        int msg = rc.readSharedArray(index);
        int[] msgContents = new int[3];
        msgContents[0] = msg >> 12;
        msgContents[1] = (msg >> 6) & 0b111111;
        msgContents[2] = msg & 0b111111;
        return msgContents;
    }
}
