package mainBot;

import battlecode.common.*;
import mainBot.betterJavaUtil.*;

public class Pathfinder {

    static boolean onMap(RobotController rc, MapLocation test) {
        return (test.x >= 0 && test.x < rc.getMapWidth() && test.y >= 0 && test.y < rc.getMapHeight());
    }
    static Direction getMoveDir(RobotController bot, MapLocation tgt) throws GameActionException {
        MapLocation src = bot.getLocation();
        int range = 8;

        /*
        //simple heuristic using only nearby 8
        Direction optimalDir = Direction.CENTER;
        double optimalCost = 9999;
        for (Direction dir : Direction.allDirections()) {
            MapLocation loc = src.add(dir);
            if (!onMap(bot, loc) || bot.isLocationOccupied(loc)) {
                continue;
            }
            double currCost = Math.sqrt(loc.distanceSquaredTo(tgt)) + (bot.senseRubble(loc));
            if (currCost < optimalCost) {
                optimalDir = dir;
                optimalCost = currCost;
            }
        }
        return optimalDir;*/

        //sweep dijkstras/bellman ford from immediate tgt to src
        MapLocation immTgt;

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

        HashMap<MapLocation, Double> costs = new HashMap<>(10);
        MapLocation moveTo = src.add(src.directionTo(immTgt));
        LinkedList<MapLocation> processQ = new LinkedList<>();
        processQ.add(immTgt);
        bot.setIndicatorString(immTgt.toString());

        Node<MapLocation> h = processQ.head;

        while (h != null) {
            MapLocation current = h.val;

            if (current.equals(src)) {
                break;
            }

            Direction straightDir = current.directionTo(src);
            double currCds = 1 + bot.senseRubble(current)/10.0;
            
            MapLocation towards = current.add(straightDir);
            if (onMap(bot, towards) && towards.isWithinDistanceSquared(src, range) && (towards.equals(src) || !bot.isLocationOccupied(towards))) {
                double towardsCds = 1 + bot.senseRubble(towards)/10.0;
                if (!costs.contains(towards) || costs.get(towards) > towardsCds + currCds) {
                    costs.put(towards, towardsCds + currCds);
                    if (towards.equals(src)) {
                        moveTo = current;
                    }
                    if (!processQ.contains(towards)) {
                        processQ.add(towards);
                    }
                }
            }

            MapLocation left = current.add(straightDir.rotateLeft());
            if (onMap(bot, left) && left.isWithinDistanceSquared(src, range) && (left.equals(src) || !bot.isLocationOccupied(left))) {
                double leftCds = 1 + bot.senseRubble(left)/10.0;
                if (!costs.contains(left) || costs.get(left) > leftCds + currCds) {
                    costs.put(left, leftCds + currCds);
                    if (left.equals(src)) {
                        moveTo = current;
                    }
                    if (!processQ.contains(left)) {
                        processQ.add(left);
                    }
                }
            }

            MapLocation right = current.add(straightDir.rotateRight());
            if (onMap(bot, right) && right.isWithinDistanceSquared(src, range) && (right.equals(src) || !bot.isLocationOccupied(right))) {
                double rightCds = 1 + bot.senseRubble(right)/10.0;
                if (!costs.contains(right) || costs.get(right) > rightCds + currCds) {
                    costs.put(right, rightCds + currCds);
                    if (right.equals(src)) {
                        break;
                    }
                    if (!processQ.contains(right)) {
                        processQ.add(right);
                    }
                }
            }
            h = h.next;
        }
        return src.directionTo(moveTo);
    }
}
