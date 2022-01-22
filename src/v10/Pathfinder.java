package v10;

import battlecode.common.*;
import mainBot.betterJavaUtil.*;

public class Pathfinder {

    static boolean arrayContains(MapLocation[] arr, MapLocation loc) {
        for (int i = arr.length - 1; i >= 0; i --) {
            if (arr[i] == null) {
                continue;
            } else if (arr[i].equals(loc)) {
                return true;
            }
        }
        return false;
    }

    static Direction getMoveDir(RobotController rc, MapLocation tgt, MapLocation[] prev5Locs) throws GameActionException {
        MapLocation src = rc.getLocation();
        int range = 8;
        //rc.setIndicatorString("tgt: " + tgt);
        
        //simple heuristic using only nearby 8
        MapLocation immTgt;
        if (src.equals(tgt)) {
            return Direction.CENTER;
        } else {
            Direction optimalDir = Direction.CENTER;
            double optimalCost = 9999;
            for (Direction dir : Direction.allDirections()) {
                MapLocation loc = src.add(dir);
                if (!rc.onTheMap(loc) || rc.isLocationOccupied(loc) || arrayContains(prev5Locs, loc)) {
                    continue;
                }
                double currCost = loc.distanceSquaredTo(tgt) + (rc.senseRubble(loc));
                if (currCost < optimalCost) {
                    optimalDir = dir;
                    optimalCost = currCost;
                }
            }
            return optimalDir;

            //sweep relaxing edges from immediate tgt to src
        } /*
        else {
            if (tgt.isWithinDistanceSquared(src, range)) {
                immTgt = tgt;
            } else {
                Direction toTgt = src.directionTo(tgt);
                double radMultiplier = 1;
                if (toTgt.dx != 0 && toTgt.dy != 0) radMultiplier = 1.0/2;

                //finding translation deltas
                int deltaX = toTgt.dx*(int)Math.floor(Math.sqrt(range*radMultiplier));
                int deltaY = toTgt.dy*(int)Math.floor(Math.sqrt(range*radMultiplier));

                //dijkstras in vision target for this turn
                immTgt = src.translate(deltaX, deltaY);
            }

            int[][] costs = new int[5][5];
            for (int i = 4; i >= 0; i --) {
                for (int j = 4; j >= 0; j --) {
                    costs[i][j] = -1;
                }
            }
            MapLocation moveTo = null;
            LinkedList<MapLocation> processQ = new LinkedList<>();
            int tgtRubble = rc.senseRubble(immTgt);
            costs[immTgt.x - src.x + 2][immTgt.y - src.y + 2] = tgtRubble;
            processQ.add(immTgt);

            Node<MapLocation> h = processQ.head;

            while (h != null) {
                MapLocation current = h.val;

                if (current.equals(src)) {
                    break;
                }

                Direction straightDir = current.directionTo(src);
                int currRubble = costs[current.x - src.x + 2][current.y - src.y + 2];
                int currCds = currRubble;

                MapLocation left = current.add(straightDir.rotateLeft());
                if (rc.onTheMap(left) && left.isWithinDistanceSquared(src, range) && (left.equals(src) || !rc.isLocationOccupied(left))) {
                    int leftRubble = rc.senseRubble(left);
                    int leftCds = leftRubble;
                    if (costs[left.x - src.x + 2][left.y - src.y + 2] == -1 || costs[left.x - src.x + 2][left.y - src.y + 2] > leftCds + currCds) {
                        costs[left.x - src.x + 2][left.y - src.y + 2] = leftCds + currCds;
                        if (left.equals(src)) {
                            moveTo = current;
                        }
                        if (!processQ.contains(left)) {
                            processQ.add(left);
                        }
                    }
                }

                MapLocation right = current.add(straightDir.rotateRight());
                if (rc.onTheMap(right) && right.isWithinDistanceSquared(src, range) && (right.equals(src) || !rc.isLocationOccupied(right))) {
                    int rightRubble = rc.senseRubble(right);
                    int rightCds = rightRubble;
                    if (costs[right.x - src.x + 2][right.y - src.y + 2] == -1 || costs[right.x - src.x + 2][right.y - src.y + 2] > rightCds + currCds) {
                        costs[right.x - src.x + 2][right.y - src.y + 2] = rightCds + currCds;
                        if (right.equals(src)) {
                            break;
                        }
                        if (!processQ.contains(right)) {
                            processQ.add(right);
                        }
                    }
                }

                MapLocation towards = current.add(straightDir);
                if (rc.onTheMap(towards) && towards.isWithinDistanceSquared(src, range) && (towards.equals(src) || !rc.isLocationOccupied(towards))) {
                    int toRubble = rc.senseRubble(towards);
                    int towardsCds = toRubble;
                    if (costs[towards.x - src.x + 2][towards.y - src.y + 2] == -1 || costs[towards.x - src.x + 2][towards.y - src.y + 2] > towardsCds + currCds) {
                        costs[towards.x - src.x + 2][towards.y - src.y + 2] = towardsCds + currCds;
                        if (towards.equals(src)) {
                            moveTo = current;
                        }
                        if (!processQ.contains(towards)) {
                            processQ.add(towards);
                        }
                    }
                }
                h = h.next;
            }
            if (moveTo == null) {
                Direction optimalDir = Direction.CENTER;
                double optimalCost = 9999;
                for (Direction dir : Direction.allDirections()) {
                    MapLocation loc = src.add(dir);
                    if (!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                        continue;
                    }
                    double currCost = Math.sqrt(loc.distanceSquaredTo(tgt))*20 + (rc.senseRubble(loc));
                    if (currCost < optimalCost) {
                        optimalDir = dir;
                        optimalCost = currCost;
                    }
                }
                moveTo = src.add(optimalDir);
            }
            return src.directionTo(moveTo);
        } */
    }
}
