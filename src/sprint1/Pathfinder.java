package sprint1;

import battlecode.common.*;
import mainBot.betterJavaUtil.*;

public class Pathfinder {

    static Direction getMoveDir(RobotController bot, MapLocation tgt) throws GameActionException {
        MapLocation src = bot.getLocation();
        int range = 8;
        
        //simple heuristic using only nearby 8
        MapLocation immTgt;
        if (tgt.isWithinDistanceSquared(src, range) && Clock.getBytecodesLeft() < 4000
                || Clock.getBytecodesLeft() < 5500) {
            Direction optimalDir = Direction.CENTER;
            double optimalCost = 9999;
            for (Direction dir : Direction.allDirections()) {
                MapLocation loc = src.add(dir);
                if (!bot.onTheMap(loc) || bot.isLocationOccupied(loc)) {
                    continue;
                }
                double currCost = Math.sqrt(loc.distanceSquaredTo(tgt))*20 + (bot.senseRubble(loc));
                if (currCost < optimalCost) {
                    optimalDir = dir;
                    optimalCost = currCost;
                }
            }
            return optimalDir;

            //sweep relaxing edges from immediate tgt to src
        } else {
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
            MapLocation moveTo = src.add(src.directionTo(immTgt));
            LinkedList<MapLocation> processQ = new LinkedList<>();
            int tgtRubble = bot.senseRubble(immTgt);
            costs[immTgt.x - src.x + 2][immTgt.y - src.y + 2] = tgtRubble;
            processQ.add(immTgt);
            bot.setIndicatorString(tgt.toString());

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
                if (bot.onTheMap(left) && left.isWithinDistanceSquared(src, range) && (left.equals(src) || !bot.isLocationOccupied(left))) {
                    int leftRubble = bot.senseRubble(left);
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
                if (bot.onTheMap(right) && right.isWithinDistanceSquared(src, range) && (right.equals(src) || !bot.isLocationOccupied(right))) {
                    int rightRubble = bot.senseRubble(right);
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
                if (bot.onTheMap(towards) && towards.isWithinDistanceSquared(src, range) && (towards.equals(src) || !bot.isLocationOccupied(towards))) {
                    int toRubble = bot.senseRubble(towards);
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
            return src.directionTo(moveTo);
        }
    }

    //return direction away from a location
    //will try to find a way to maximize distance even if it's not directly away
    static Direction getAwayDir(RobotController rc, MapLocation loc) throws GameActionException {
        MapLocation src = rc.getLocation();

        Direction opposite = src.directionTo(loc).opposite();

        MapLocation runawayTgt = src.add(opposite).add(opposite);
        runawayTgt = new MapLocation(Math.min(Math.max(0, runawayTgt.x), rc.getMapWidth() - 1),
                Math.min(Math.max(0, runawayTgt.y), rc.getMapHeight() - 1));
        Direction toReturn = Pathfinder.getMoveDir(rc, runawayTgt);

        if(rc.canMove(toReturn)){
            return toReturn;
        }
        else if(rc.canMove(toReturn.rotateRight() )){
            return toReturn.rotateRight();
        }
        else if(rc.canMove(toReturn.rotateLeft() )){
            return toReturn.rotateLeft();
        }
        else if(rc.canMove(toReturn.rotateLeft().rotateLeft() )){
            return toReturn.rotateLeft().rotateLeft();
        }
        else{
            return toReturn.rotateRight().rotateRight();
        }
    }
}
