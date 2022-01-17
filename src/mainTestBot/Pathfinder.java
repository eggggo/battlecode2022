package mainTestBot;

import battlecode.common.*;
import mainBot.betterJavaUtil.*;

//The memoization of cell costs allows subsequent paths in the same object instance to be calculated faster, if the tested routes overlap.
//For r2=53, max cost around 2800. For r2=20, max cost around 1400
public class Pathfinder {
    private final RobotController rc;
    private final MapLocation origin; // position of the robot. cell lat/lon offsets are calculated relative to this.

    /*
     * Create a new instance each turn, as obstacles (other bots and rubble) can move even if you don't.
     */
    public Pathfinder(RobotController robotController) {
        this.rc = robotController;
        this.origin = rc.getLocation();
    }

    /**
     * Provides the initial direction to move in, if you're trying to get to the given location.
     *
     * @param tgt The intended destination.
     * @return The direction to move, or CENTER if no can be determined
     * @throws GameActionException
     */
    public Direction getMoveDir(MapLocation tgt) throws GameActionException {
        rc.setIndicatorString("tgt: " + tgt);

        if (Clock.getBytecodesLeft() < 5500) {
            Direction optimalDir = Direction.CENTER;
            double optimalCost = 9999;
            for (Direction dir : Direction.allDirections()) {
                MapLocation loc = origin.add(dir);
                if (!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                    continue;
                }
                double currCost = Math.sqrt(loc.distanceSquaredTo(tgt)) * 20 + (rc.senseRubble(loc));
                if (currCost < optimalCost) {
                    optimalDir = dir;
                    optimalCost = currCost;
                }
            }
            return optimalDir;

        }

        //can't see tgt; go to point in direction of tgt on edge of vision
        if(!rc.canSenseLocation(tgt) || !rc.onTheMap(tgt)){//15,15 - 5, 5
            double R = Math.sqrt(rc.getType().visionRadiusSquared); //4.47
            double vX = tgt.x - origin.x; //10
            double vY = tgt.y - origin.y; //10
            double magV = Math.sqrt(vX*vX + vY*vY); //14.1
            int aX = (int) (vX / magV * R); //.709*4.47 = 3.170
            int aY = (int) (vY / magV * R);
            tgt = origin.translate(aX, aY);
        }

        if(!rc.canSenseLocation(tgt)  || !rc.onTheMap(tgt) || origin.equals(tgt)) {
            Direction optimalDir = Direction.CENTER;
            double optimalCost = 9999;
            for (Direction dir : Direction.allDirections()) {
                MapLocation loc = origin.add(dir);
                if (!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                    continue;
                }
                double currCost = Math.sqrt(loc.distanceSquaredTo(tgt))*20 + (rc.senseRubble(loc));
                if (currCost < optimalCost) {
                    optimalDir = dir;
                    optimalCost = currCost;
                }
            }
            return optimalDir;
        }
        // pathing works in terms of offset from the bot's current location
        int dx = tgt.x-origin.x;
        int dy = tgt.y-origin.y;
        Clock.getBytecodeNum();
        int result = cell(dx, dy);
        Clock.getBytecodeNum();
        // decompose the result back into its component parts
        int pathCost = result/100;
        int directionOrdinal = pathCost%10;
        Direction direction = Direction.values()[directionOrdinal];
        return direction;
    }

    private int cell(int dx, int dy) throws GameActionException {

        switch(dy) {
            case -7:
                switch(dx) {
                    case -2: return cell7S2W();
                    case -1: return cell7S1W();
                    case 0: return cell7S0E();
                    case 1: return cell7S1E();
                    case 2: return cell7S2E();
                }
            case -6:
                switch(dx) {
                    case -4: return cell6S4W();
                    case -3: return cell6S3W();
                    case -2: return cell6S2W();
                    case -1: return cell6S1W();
                    case 0: return cell6S0E();
                    case 1: return cell6S1E();
                    case 2: return cell6S2E();
                    case 3: return cell6S3E();
                    case 4: return cell6S4E();
                }
            case -5:
                switch(dx) {
                    case -5: return cell5S5W();
                    case -4: return cell5S4W();
                    case -3: return cell5S3W();
                    case -2: return cell5S2W();
                    case -1: return cell5S1W();
                    case 0: return cell5S0E();
                    case 1: return cell5S1E();
                    case 2: return cell5S2E();
                    case 3: return cell5S3E();
                    case 4: return cell5S4E();
                    case 5: return cell5S5E();
                }
            case -4:
                switch(dx) {
                    case -6: return cell4S6W();
                    case -5: return cell4S5W();
                    case -4: return cell4S4W();
                    case -3: return cell4S3W();
                    case -2: return cell4S2W();
                    case -1: return cell4S1W();
                    case 0: return cell4S0E();
                    case 1: return cell4S1E();
                    case 2: return cell4S2E();
                    case 3: return cell4S3E();
                    case 4: return cell4S4E();
                    case 5: return cell4S5E();
                    case 6: return cell4S6E();
                }
            case -3:
                switch(dx) {
                    case -6: return cell3S6W();
                    case -5: return cell3S5W();
                    case -4: return cell3S4W();
                    case -3: return cell3S3W();
                    case -2: return cell3S2W();
                    case -1: return cell3S1W();
                    case 0: return cell3S0E();
                    case 1: return cell3S1E();
                    case 2: return cell3S2E();
                    case 3: return cell3S3E();
                    case 4: return cell3S4E();
                    case 5: return cell3S5E();
                    case 6: return cell3S6E();
                }
            case -2:
                switch(dx) {
                    case -7: return cell2S7W();
                    case -6: return cell2S6W();
                    case -5: return cell2S5W();
                    case -4: return cell2S4W();
                    case -3: return cell2S3W();
                    case -2: return cell2S2W();
                    case -1: return cell2S1W();
                    case 0: return cell2S0E();
                    case 1: return cell2S1E();
                    case 2: return cell2S2E();
                    case 3: return cell2S3E();
                    case 4: return cell2S4E();
                    case 5: return cell2S5E();
                    case 6: return cell2S6E();
                    case 7: return cell2S7E();
                }
            case -1:
                switch(dx) {
                    case -7: return cell1S7W();
                    case -6: return cell1S6W();
                    case -5: return cell1S5W();
                    case -4: return cell1S4W();
                    case -3: return cell1S3W();
                    case -2: return cell1S2W();
                    case -1: return cell1S1W();
                    case 0: return cell1S0E();
                    case 1: return cell1S1E();
                    case 2: return cell1S2E();
                    case 3: return cell1S3E();
                    case 4: return cell1S4E();
                    case 5: return cell1S5E();
                    case 6: return cell1S6E();
                    case 7: return cell1S7E();
                }
            case 0:
                switch(dx) {
                    case -7: return cell0N7W();
                    case -6: return cell0N6W();
                    case -5: return cell0N5W();
                    case -4: return cell0N4W();
                    case -3: return cell0N3W();
                    case -2: return cell0N2W();
                    case -1: return cell0N1W();
                    case 0: return cell0N0E();
                    case 1: return cell0N1E();
                    case 2: return cell0N2E();
                    case 3: return cell0N3E();
                    case 4: return cell0N4E();
                    case 5: return cell0N5E();
                    case 6: return cell0N6E();
                    case 7: return cell0N7E();
                }
            case 1:
                switch(dx) {
                    case -7: return cell1N7W();
                    case -6: return cell1N6W();
                    case -5: return cell1N5W();
                    case -4: return cell1N4W();
                    case -3: return cell1N3W();
                    case -2: return cell1N2W();
                    case -1: return cell1N1W();
                    case 0: return cell1N0E();
                    case 1: return cell1N1E();
                    case 2: return cell1N2E();
                    case 3: return cell1N3E();
                    case 4: return cell1N4E();
                    case 5: return cell1N5E();
                    case 6: return cell1N6E();
                    case 7: return cell1N7E();
                }
            case 2:
                switch(dx) {
                    case -7: return cell2N7W();
                    case -6: return cell2N6W();
                    case -5: return cell2N5W();
                    case -4: return cell2N4W();
                    case -3: return cell2N3W();
                    case -2: return cell2N2W();
                    case -1: return cell2N1W();
                    case 0: return cell2N0E();
                    case 1: return cell2N1E();
                    case 2: return cell2N2E();
                    case 3: return cell2N3E();
                    case 4: return cell2N4E();
                    case 5: return cell2N5E();
                    case 6: return cell2N6E();
                    case 7: return cell2N7E();
                }
            case 3:
                switch(dx) {
                    case -6: return cell3N6W();
                    case -5: return cell3N5W();
                    case -4: return cell3N4W();
                    case -3: return cell3N3W();
                    case -2: return cell3N2W();
                    case -1: return cell3N1W();
                    case 0: return cell3N0E();
                    case 1: return cell3N1E();
                    case 2: return cell3N2E();
                    case 3: return cell3N3E();
                    case 4: return cell3N4E();
                    case 5: return cell3N5E();
                    case 6: return cell3N6E();
                }
            case 4:
                switch(dx) {
                    case -6: return cell4N6W();
                    case -5: return cell4N5W();
                    case -4: return cell4N4W();
                    case -3: return cell4N3W();
                    case -2: return cell4N2W();
                    case -1: return cell4N1W();
                    case 0: return cell4N0E();
                    case 1: return cell4N1E();
                    case 2: return cell4N2E();
                    case 3: return cell4N3E();
                    case 4: return cell4N4E();
                    case 5: return cell4N5E();
                    case 6: return cell4N6E();
                }
            case 5:
                switch(dx) {
                    case -5: return cell5N5W();
                    case -4: return cell5N4W();
                    case -3: return cell5N3W();
                    case -2: return cell5N2W();
                    case -1: return cell5N1W();
                    case 0: return cell5N0E();
                    case 1: return cell5N1E();
                    case 2: return cell5N2E();
                    case 3: return cell5N3E();
                    case 4: return cell5N4E();
                    case 5: return cell5N5E();
                }
            case 6:
                switch(dx) {
                    case -4: return cell6N4W();
                    case -3: return cell6N3W();
                    case -2: return cell6N2W();
                    case -1: return cell6N1W();
                    case 0: return cell6N0E();
                    case 1: return cell6N1E();
                    case 2: return cell6N2E();
                    case 3: return cell6N3E();
                    case 4: return cell6N4E();
                }
            case 7:
                switch(dx) {
                    case -2: return cell7N2W();
                    case -1: return cell7N1W();
                    case 0: return cell7N0E();
                    case 1: return cell7N1E();
                    case 2: return cell7N2E();
                }
        }

        throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "should never get here");
    }

    private int cell7S2W;
    private int cell7S2W() throws GameActionException {
        if(cell7S2W == 0) {
            MapLocation loc = origin.translate(-2, -7);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell7S2W = 1_000_000;
            } else {
                cell7S2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell6S2W(), cell6S1W()), cell7S1W()), cell6S3W());
            }
        }
        return cell7S2W;
    }

    private int cell7S1W;
    private int cell7S1W() throws GameActionException {
        if(cell7S1W == 0) {
            MapLocation loc = origin.translate(-1, -7);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell7S1W = 1_000_000;
            } else {
                cell7S1W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell6S1W(), cell6S0E()), cell7S0E()), cell6S2W());
            }
        }
        return cell7S1W;
    }

    private int cell7S0E;
    private int cell7S0E() throws GameActionException {
        if(cell7S0E == 0) {
            MapLocation loc = origin.translate(0, -7);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell7S0E = 1_000_000;
            } else {
                cell7S0E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell6S0E(), cell6S1E()), cell6S1W());
            }
        }
        return cell7S0E;
    }

    private int cell7S1E;
    private int cell7S1E() throws GameActionException {
        if(cell7S1E == 0) {
            MapLocation loc = origin.translate(1, -7);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell7S1E = 1_000_000;
            } else {
                cell7S1E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell6S1E(), cell6S2E()), cell7S0E()), cell6S0E());
            }
        }
        return cell7S1E;
    }

    private int cell7S2E;
    private int cell7S2E() throws GameActionException {
        if(cell7S2E == 0) {
            MapLocation loc = origin.translate(2, -7);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell7S2E = 1_000_000;
            } else {
                cell7S2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell6S2E(), cell6S3E()), cell7S1E()), cell6S1E());
            }
        }
        return cell7S2E;
    }

    private int cell6S4W;
    private int cell6S4W() throws GameActionException {
        if(cell6S4W == 0) {
            MapLocation loc = origin.translate(-4, -6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6S4W = 1_000_000;
            } else {
                cell6S4W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5S4W(), cell5S3W()), cell6S3W()), cell5S5W());
            }
        }
        return cell6S4W;
    }

    private int cell6S3W;
    private int cell6S3W() throws GameActionException {
        if(cell6S3W == 0) {
            MapLocation loc = origin.translate(-3, -6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6S3W = 1_000_000;
            } else {
                cell6S3W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5S3W(), cell5S2W()), cell6S2W()), cell5S4W());
            }
        }
        return cell6S3W;
    }

    private int cell6S2W;
    private int cell6S2W() throws GameActionException {
        if(cell6S2W == 0) {
            MapLocation loc = origin.translate(-2, -6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6S2W = 1_000_000;
            } else {
                cell6S2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5S2W(), cell5S1W()), cell6S1W()), cell5S3W());
            }
        }
        return cell6S2W;
    }

    private int cell6S1W;
    private int cell6S1W() throws GameActionException {
        if(cell6S1W == 0) {
            MapLocation loc = origin.translate(-1, -6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6S1W = 1_000_000;
            } else {
                cell6S1W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5S1W(), cell5S0E()), cell6S0E()), cell5S2W());
            }
        }
        return cell6S1W;
    }

    private int cell6S0E;
    private int cell6S0E() throws GameActionException {
        if(cell6S0E == 0) {
            MapLocation loc = origin.translate(0, -6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6S0E = 1_000_000;
            } else {
                cell6S0E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell5S0E(), cell5S1E()), cell5S1W());
            }
        }
        return cell6S0E;
    }

    private int cell6S1E;
    private int cell6S1E() throws GameActionException {
        if(cell6S1E == 0) {
            MapLocation loc = origin.translate(1, -6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6S1E = 1_000_000;
            } else {
                cell6S1E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5S1E(), cell5S2E()), cell6S0E()), cell5S0E());
            }
        }
        return cell6S1E;
    }

    private int cell6S2E;
    private int cell6S2E() throws GameActionException {
        if(cell6S2E == 0) {
            MapLocation loc = origin.translate(2, -6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6S2E = 1_000_000;
            } else {
                cell6S2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5S2E(), cell5S3E()), cell6S1E()), cell5S1E());
            }
        }
        return cell6S2E;
    }

    private int cell6S3E;
    private int cell6S3E() throws GameActionException {
        if(cell6S3E == 0) {
            MapLocation loc = origin.translate(3, -6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6S3E = 1_000_000;
            } else {
                cell6S3E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5S3E(), cell5S4E()), cell6S2E()), cell5S2E());
            }
        }
        return cell6S3E;
    }

    private int cell6S4E;
    private int cell6S4E() throws GameActionException {
        if(cell6S4E == 0) {
            MapLocation loc = origin.translate(4, -6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6S4E = 1_000_000;
            } else {
                cell6S4E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5S4E(), cell5S5E()), cell6S3E()), cell5S3E());
            }
        }
        return cell6S4E;
    }

    private int cell5S5W;
    private int cell5S5W() throws GameActionException {
        if(cell5S5W == 0) {
            MapLocation loc = origin.translate(-5, -5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5S5W = 1_000_000;
            } else {
                cell5S5W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell4S5W(), cell4S4W()), cell5S4W());
            }
        }
        return cell5S5W;
    }

    private int cell5S4W;
    private int cell5S4W() throws GameActionException {
        if(cell5S4W == 0) {
            MapLocation loc = origin.translate(-4, -5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5S4W = 1_000_000;
            } else {
                cell5S4W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell4S4W(), cell4S3W()), cell5S3W());
            }
        }
        return cell5S4W;
    }

    private int cell5S3W;
    private int cell5S3W() throws GameActionException {
        if(cell5S3W == 0) {
            MapLocation loc = origin.translate(-3, -5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5S3W = 1_000_000;
            } else {
                cell5S3W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell4S3W(), cell4S2W()), cell5S2W()), cell4S4W());
            }
        }
        return cell5S3W;
    }

    private int cell5S2W;
    private int cell5S2W() throws GameActionException {
        if(cell5S2W == 0) {
            MapLocation loc = origin.translate(-2, -5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5S2W = 1_000_000;
            } else {
                cell5S2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell4S2W(), cell4S1W()), cell5S1W()), cell4S3W());
            }
        }
        return cell5S2W;
    }

    private int cell5S1W;
    private int cell5S1W() throws GameActionException {
        if(cell5S1W == 0) {
            MapLocation loc = origin.translate(-1, -5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5S1W = 1_000_000;
            } else {
                cell5S1W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell4S1W(), cell4S0E()), cell5S0E()), cell4S2W());
            }
        }
        return cell5S1W;
    }

    private int cell5S0E;
    private int cell5S0E() throws GameActionException {
        if(cell5S0E == 0) {
            MapLocation loc = origin.translate(0, -5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5S0E = 1_000_000;
            } else {
                cell5S0E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell4S0E(), cell4S1E()), cell4S1W());
            }
        }
        return cell5S0E;
    }

    private int cell5S1E;
    private int cell5S1E() throws GameActionException {
        if(cell5S1E == 0) {
            MapLocation loc = origin.translate(1, -5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5S1E = 1_000_000;
            } else {
                cell5S1E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell4S1E(), cell4S2E()), cell5S0E()), cell4S0E());
            }
        }
        return cell5S1E;
    }

    private int cell5S2E;
    private int cell5S2E() throws GameActionException {
        if(cell5S2E == 0) {
            MapLocation loc = origin.translate(2, -5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5S2E = 1_000_000;
            } else {
                cell5S2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell4S2E(), cell4S3E()), cell5S1E()), cell4S1E());
            }
        }
        return cell5S2E;
    }

    private int cell5S3E;
    private int cell5S3E() throws GameActionException {
        if(cell5S3E == 0) {
            MapLocation loc = origin.translate(3, -5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5S3E = 1_000_000;
            } else {
                cell5S3E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell4S3E(), cell4S4E()), cell5S2E()), cell4S2E());
            }
        }
        return cell5S3E;
    }

    private int cell5S4E;
    private int cell5S4E() throws GameActionException {
        if(cell5S4E == 0) {
            MapLocation loc = origin.translate(4, -5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5S4E = 1_000_000;
            } else {
                cell5S4E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell4S4E(), cell5S3E()), cell4S3E());
            }
        }
        return cell5S4E;
    }

    private int cell5S5E;
    private int cell5S5E() throws GameActionException {
        if(cell5S5E == 0) {
            MapLocation loc = origin.translate(5, -5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5S5E = 1_000_000;
            } else {
                cell5S5E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell4S5E(), cell5S4E()), cell4S4E());
            }
        }
        return cell5S5E;
    }

    private int cell4S6W;
    private int cell4S6W() throws GameActionException {
        if(cell4S6W == 0) {
            MapLocation loc = origin.translate(-6, -4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4S6W = 1_000_000;
            } else {
                cell4S6W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell3S6W(), cell3S5W()), cell4S5W()), cell5S5W());
            }
        }
        return cell4S6W;
    }

    private int cell4S5W;
    private int cell4S5W() throws GameActionException {
        if(cell4S5W == 0) {
            MapLocation loc = origin.translate(-5, -4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4S5W = 1_000_000;
            } else {
                cell4S5W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell3S5W(), cell3S4W()), cell4S4W());
            }
        }
        return cell4S5W;
    }

    private int cell4S4W;
    private int cell4S4W() throws GameActionException {
        if(cell4S4W == 0) {
            MapLocation loc = origin.translate(-4, -4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4S4W = 1_000_000;
            } else {
                cell4S4W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell3S4W(), cell3S3W()), cell4S3W());
            }
        }
        return cell4S4W;
    }

    private int cell4S3W;
    private int cell4S3W() throws GameActionException {
        if(cell4S3W == 0) {
            MapLocation loc = origin.translate(-3, -4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4S3W = 1_000_000;
            } else {
                cell4S3W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell3S3W(), cell3S2W()), cell4S2W());
            }
        }
        return cell4S3W;
    }

    private int cell4S2W;
    private int cell4S2W() throws GameActionException {
        if(cell4S2W == 0) {
            MapLocation loc = origin.translate(-2, -4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4S2W = 1_000_000;
            } else {
                cell4S2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell3S2W(), cell3S1W()), cell4S1W()), cell3S3W());
            }
        }
        return cell4S2W;
    }

    private int cell4S1W;
    private int cell4S1W() throws GameActionException {
        if(cell4S1W == 0) {
            MapLocation loc = origin.translate(-1, -4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4S1W = 1_000_000;
            } else {
                cell4S1W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell3S1W(), cell3S0E()), cell4S0E()), cell3S2W());
            }
        }
        return cell4S1W;
    }

    private int cell4S0E;
    private int cell4S0E() throws GameActionException {
        if(cell4S0E == 0) {
            MapLocation loc = origin.translate(0, -4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4S0E = 1_000_000;
            } else {
                cell4S0E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell3S0E(), cell3S1E()), cell3S1W());
            }
        }
        return cell4S0E;
    }

    private int cell4S1E;
    private int cell4S1E() throws GameActionException {
        if(cell4S1E == 0) {
            MapLocation loc = origin.translate(1, -4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4S1E = 1_000_000;
            } else {
                cell4S1E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell3S1E(), cell3S2E()), cell4S0E()), cell3S0E());
            }
        }
        return cell4S1E;
    }

    private int cell4S2E;
    private int cell4S2E() throws GameActionException {
        if(cell4S2E == 0) {
            MapLocation loc = origin.translate(2, -4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4S2E = 1_000_000;
            } else {
                cell4S2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell3S2E(), cell3S3E()), cell4S1E()), cell3S1E());
            }
        }
        return cell4S2E;
    }

    private int cell4S3E;
    private int cell4S3E() throws GameActionException {
        if(cell4S3E == 0) {
            MapLocation loc = origin.translate(3, -4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4S3E = 1_000_000;
            } else {
                cell4S3E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell3S3E(), cell4S2E()), cell3S2E());
            }
        }
        return cell4S3E;
    }

    private int cell4S4E;
    private int cell4S4E() throws GameActionException {
        if(cell4S4E == 0) {
            MapLocation loc = origin.translate(4, -4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4S4E = 1_000_000;
            } else {
                cell4S4E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell3S4E(), cell4S3E()), cell3S3E());
            }
        }
        return cell4S4E;
    }

    private int cell4S5E;
    private int cell4S5E() throws GameActionException {
        if(cell4S5E == 0) {
            MapLocation loc = origin.translate(5, -4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4S5E = 1_000_000;
            } else {
                cell4S5E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell3S5E(), cell4S4E()), cell3S4E());
            }
        }
        return cell4S5E;
    }

    private int cell4S6E;
    private int cell4S6E() throws GameActionException {
        if(cell4S6E == 0) {
            MapLocation loc = origin.translate(6, -4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4S6E = 1_000_000;
            } else {
                cell4S6E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell3S6E(), cell5S5E()), cell4S5E()), cell3S5E());
            }
        }
        return cell4S6E;
    }

    private int cell3S6W;
    private int cell3S6W() throws GameActionException {
        if(cell3S6W == 0) {
            MapLocation loc = origin.translate(-6, -3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3S6W = 1_000_000;
            } else {
                cell3S6W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell2S6W(), cell2S5W()), cell3S5W()), cell4S5W());
            }
        }
        return cell3S6W;
    }

    private int cell3S5W;
    private int cell3S5W() throws GameActionException {
        if(cell3S5W == 0) {
            MapLocation loc = origin.translate(-5, -3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3S5W = 1_000_000;
            } else {
                cell3S5W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell2S5W(), cell2S4W()), cell3S4W()), cell4S4W());
            }
        }
        return cell3S5W;
    }

    private int cell3S4W;
    private int cell3S4W() throws GameActionException {
        if(cell3S4W == 0) {
            MapLocation loc = origin.translate(-4, -3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3S4W = 1_000_000;
            } else {
                cell3S4W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell2S4W(), cell2S3W()), cell3S3W());
            }
        }
        return cell3S4W;
    }

    private int cell3S3W;
    private int cell3S3W() throws GameActionException {
        if(cell3S3W == 0) {
            MapLocation loc = origin.translate(-3, -3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3S3W = 1_000_000;
            } else {
                cell3S3W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell2S3W(), cell2S2W()), cell3S2W());
            }
        }
        return cell3S3W;
    }

    private int cell3S2W;
    private int cell3S2W() throws GameActionException {
        if(cell3S2W == 0) {
            MapLocation loc = origin.translate(-2, -3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3S2W = 1_000_000;
            } else {
                cell3S2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell2S2W(), cell2S1W()), cell3S1W());
            }
        }
        return cell3S2W;
    }

    private int cell3S1W;
    private int cell3S1W() throws GameActionException {
        if(cell3S1W == 0) {
            MapLocation loc = origin.translate(-1, -3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3S1W = 1_000_000;
            } else {
                cell3S1W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell2S1W(), cell2S0E()), cell3S0E()), cell2S2W());
            }
        }
        return cell3S1W;
    }

    private int cell3S0E;
    private int cell3S0E() throws GameActionException {
        if(cell3S0E == 0) {
            MapLocation loc = origin.translate(0, -3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3S0E = 1_000_000;
            } else {
                cell3S0E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell2S0E(), cell2S1E()), cell2S1W());
            }
        }
        return cell3S0E;
    }

    private int cell3S1E;
    private int cell3S1E() throws GameActionException {
        if(cell3S1E == 0) {
            MapLocation loc = origin.translate(1, -3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3S1E = 1_000_000;
            } else {
                cell3S1E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell2S1E(), cell2S2E()), cell3S0E()), cell2S0E());
            }
        }
        return cell3S1E;
    }

    private int cell3S2E;
    private int cell3S2E() throws GameActionException {
        if(cell3S2E == 0) {
            MapLocation loc = origin.translate(2, -3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3S2E = 1_000_000;
            } else {
                cell3S2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell2S2E(), cell3S1E()), cell2S1E());
            }
        }
        return cell3S2E;
    }

    private int cell3S3E;
    private int cell3S3E() throws GameActionException {
        if(cell3S3E == 0) {
            MapLocation loc = origin.translate(3, -3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3S3E = 1_000_000;
            } else {
                cell3S3E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell2S3E(), cell3S2E()), cell2S2E());
            }
        }
        return cell3S3E;
    }

    private int cell3S4E;
    private int cell3S4E() throws GameActionException {
        if(cell3S4E == 0) {
            MapLocation loc = origin.translate(4, -3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3S4E = 1_000_000;
            } else {
                cell3S4E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell2S4E(), cell3S3E()), cell2S3E());
            }
        }
        return cell3S4E;
    }

    private int cell3S5E;
    private int cell3S5E() throws GameActionException {
        if(cell3S5E == 0) {
            MapLocation loc = origin.translate(5, -3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3S5E = 1_000_000;
            } else {
                cell3S5E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell2S5E(), cell4S4E()), cell3S4E()), cell2S4E());
            }
        }
        return cell3S5E;
    }

    private int cell3S6E;
    private int cell3S6E() throws GameActionException {
        if(cell3S6E == 0) {
            MapLocation loc = origin.translate(6, -3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3S6E = 1_000_000;
            } else {
                cell3S6E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell2S6E(), cell4S5E()), cell3S5E()), cell2S5E());
            }
        }
        return cell3S6E;
    }

    private int cell2S7W;
    private int cell2S7W() throws GameActionException {
        if(cell2S7W == 0) {
            MapLocation loc = origin.translate(-7, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S7W = 1_000_000;
            } else {
                cell2S7W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell1S7W(), cell1S6W()), cell2S6W()), cell3S6W());
            }
        }
        return cell2S7W;
    }

    private int cell2S6W;
    private int cell2S6W() throws GameActionException {
        if(cell2S6W == 0) {
            MapLocation loc = origin.translate(-6, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S6W = 1_000_000;
            } else {
                cell2S6W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell1S6W(), cell1S5W()), cell2S5W()), cell3S5W());
            }
        }
        return cell2S6W;
    }

    private int cell2S5W;
    private int cell2S5W() throws GameActionException {
        if(cell2S5W == 0) {
            MapLocation loc = origin.translate(-5, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S5W = 1_000_000;
            } else {
                cell2S5W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell1S5W(), cell1S4W()), cell2S4W()), cell3S4W());
            }
        }
        return cell2S5W;
    }

    private int cell2S4W;
    private int cell2S4W() throws GameActionException {
        if(cell2S4W == 0) {
            MapLocation loc = origin.translate(-4, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S4W = 1_000_000;
            } else {
                cell2S4W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell1S4W(), cell1S3W()), cell2S3W()), cell3S3W());
            }
        }
        return cell2S4W;
    }

    private int cell2S3W;
    private int cell2S3W() throws GameActionException {
        if(cell2S3W == 0) {
            MapLocation loc = origin.translate(-3, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S3W = 1_000_000;
            } else {
                cell2S3W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1S3W(), cell1S2W()), cell2S2W());
            }
        }
        return cell2S3W;
    }

    private int cell2S2W;
    private int cell2S2W() throws GameActionException {
        if(cell2S2W == 0) {
            MapLocation loc = origin.translate(-2, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S2W = 1_000_000;
            } else {
                cell2S2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1S2W(), cell1S1W()), cell2S1W());
            }
        }
        return cell2S2W;
    }

    private int cell2S1W;
    private int cell2S1W() throws GameActionException {
        if(cell2S1W == 0) {
            MapLocation loc = origin.translate(-1, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S1W = 1_000_000;
            } else {
                cell2S1W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1S1W(), cell1S0E()), cell2S0E());
            }
        }
        return cell2S1W;
    }

    private int cell2S0E;
    private int cell2S0E() throws GameActionException {
        if(cell2S0E == 0) {
            MapLocation loc = origin.translate(0, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S0E = 1_000_000;
            } else {
                cell2S0E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1S0E(), cell1S1E()), cell1S1W());
            }
        }
        return cell2S0E;
    }

    private int cell2S1E;
    private int cell2S1E() throws GameActionException {
        if(cell2S1E == 0) {
            MapLocation loc = origin.translate(1, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S1E = 1_000_000;
            } else {
                cell2S1E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1S1E(), cell2S0E()), cell1S0E());
            }
        }
        return cell2S1E;
    }

    private int cell2S2E;
    private int cell2S2E() throws GameActionException {
        if(cell2S2E == 0) {
            MapLocation loc = origin.translate(2, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S2E = 1_000_000;
            } else {
                cell2S2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1S2E(), cell2S1E()), cell1S1E());
            }
        }
        return cell2S2E;
    }

    private int cell2S3E;
    private int cell2S3E() throws GameActionException {
        if(cell2S3E == 0) {
            MapLocation loc = origin.translate(3, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S3E = 1_000_000;
            } else {
                cell2S3E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1S3E(), cell2S2E()), cell1S2E());
            }
        }
        return cell2S3E;
    }

    private int cell2S4E;
    private int cell2S4E() throws GameActionException {
        if(cell2S4E == 0) {
            MapLocation loc = origin.translate(4, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S4E = 1_000_000;
            } else {
                cell2S4E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell1S4E(), cell3S3E()), cell2S3E()), cell1S3E());
            }
        }
        return cell2S4E;
    }

    private int cell2S5E;
    private int cell2S5E() throws GameActionException {
        if(cell2S5E == 0) {
            MapLocation loc = origin.translate(5, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S5E = 1_000_000;
            } else {
                cell2S5E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell1S5E(), cell3S4E()), cell2S4E()), cell1S4E());
            }
        }
        return cell2S5E;
    }

    private int cell2S6E;
    private int cell2S6E() throws GameActionException {
        if(cell2S6E == 0) {
            MapLocation loc = origin.translate(6, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S6E = 1_000_000;
            } else {
                cell2S6E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell1S6E(), cell3S5E()), cell2S5E()), cell1S5E());
            }
        }
        return cell2S6E;
    }

    private int cell2S7E;
    private int cell2S7E() throws GameActionException {
        if(cell2S7E == 0) {
            MapLocation loc = origin.translate(7, -2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2S7E = 1_000_000;
            } else {
                cell2S7E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell1S7E(), cell3S6E()), cell2S6E()), cell1S6E());
            }
        }
        return cell2S7E;
    }

    private int cell1S7W;
    private int cell1S7W() throws GameActionException {
        if(cell1S7W == 0) {
            MapLocation loc = origin.translate(-7, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S7W = 1_000_000;
            } else {
                cell1S7W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N7W(), cell0N6W()), cell1S6W()), cell2S6W());
            }
        }
        return cell1S7W;
    }

    private int cell1S6W;
    private int cell1S6W() throws GameActionException {
        if(cell1S6W == 0) {
            MapLocation loc = origin.translate(-6, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S6W = 1_000_000;
            } else {
                cell1S6W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N6W(), cell0N5W()), cell1S5W()), cell2S5W());
            }
        }
        return cell1S6W;
    }

    private int cell1S5W;
    private int cell1S5W() throws GameActionException {
        if(cell1S5W == 0) {
            MapLocation loc = origin.translate(-5, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S5W = 1_000_000;
            } else {
                cell1S5W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N5W(), cell0N4W()), cell1S4W()), cell2S4W());
            }
        }
        return cell1S5W;
    }

    private int cell1S4W;
    private int cell1S4W() throws GameActionException {
        if(cell1S4W == 0) {
            MapLocation loc = origin.translate(-4, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S4W = 1_000_000;
            } else {
                cell1S4W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N4W(), cell0N3W()), cell1S3W()), cell2S3W());
            }
        }
        return cell1S4W;
    }

    private int cell1S3W;
    private int cell1S3W() throws GameActionException {
        if(cell1S3W == 0) {
            MapLocation loc = origin.translate(-3, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S3W = 1_000_000;
            } else {
                cell1S3W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N3W(), cell0N2W()), cell1S2W()), cell2S2W());
            }
        }
        return cell1S3W;
    }

    private int cell1S2W;
    private int cell1S2W() throws GameActionException {
        if(cell1S2W == 0) {
            MapLocation loc = origin.translate(-2, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S2W = 1_000_000;
            } else {
                cell1S2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell0N2W(), cell0N1W()), cell1S1W());
            }
        }
        return cell1S2W;
    }

    private int cell1S1W;
    private int cell1S1W() throws GameActionException {
        if(cell1S1W == 0) {
            MapLocation loc = origin.translate(-1, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S1W = 1_000_000;
            } else {
                cell1S1W = ((100*rc.senseRubble(loc))+Direction.SOUTHWEST.ordinal()) + cell0N1W();
                cell1S1W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell0N1W(), cell0N0E()), cell1S0E());
            }
        }
        return cell1S1W;
    }

    private int cell1S0E;
    private int cell1S0E() throws GameActionException {
        if(cell1S0E == 0) {
            MapLocation loc = origin.translate(0, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S0E = 1_000_000;
            } else {
                cell1S0E = ((100*rc.senseRubble(loc))+Direction.SOUTH.ordinal()) + cell0N0E();
            }
        }
        return cell1S0E;
    }

    private int cell1S1E;
    private int cell1S1E() throws GameActionException {
        if(cell1S1E == 0) {
            MapLocation loc = origin.translate(1, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S1E = 1_000_000;
            } else {
                cell1S1E = ((100*rc.senseRubble(loc))+Direction.SOUTHEAST.ordinal()) + cell0N1E();
                cell1S1E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell0N1E(), cell1S0E()), cell0N0E());
            }
        }
        return cell1S1E;
    }

    private int cell1S2E;
    private int cell1S2E() throws GameActionException {
        if(cell1S2E == 0) {
            MapLocation loc = origin.translate(2, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S2E = 1_000_000;
            } else {
                cell1S2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell0N2E(), cell1S1E()), cell0N1E());
            }
        }
        return cell1S2E;
    }

    private int cell1S3E;
    private int cell1S3E() throws GameActionException {
        if(cell1S3E == 0) {
            MapLocation loc = origin.translate(3, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S3E = 1_000_000;
            } else {
                cell1S3E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N3E(), cell2S2E()), cell1S2E()), cell0N2E());
            }
        }
        return cell1S3E;
    }

    private int cell1S4E;
    private int cell1S4E() throws GameActionException {
        if(cell1S4E == 0) {
            MapLocation loc = origin.translate(4, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S4E = 1_000_000;
            } else {
                cell1S4E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N4E(), cell2S3E()), cell1S3E()), cell0N3E());
            }
        }
        return cell1S4E;
    }

    private int cell1S5E;
    private int cell1S5E() throws GameActionException {
        if(cell1S5E == 0) {
            MapLocation loc = origin.translate(5, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S5E = 1_000_000;
            } else {
                cell1S5E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N5E(), cell2S4E()), cell1S4E()), cell0N4E());
            }
        }
        return cell1S5E;
    }

    private int cell1S6E;
    private int cell1S6E() throws GameActionException {
        if(cell1S6E == 0) {
            MapLocation loc = origin.translate(6, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S6E = 1_000_000;
            } else {
                cell1S6E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N6E(), cell2S5E()), cell1S5E()), cell0N5E());
            }
        }
        return cell1S6E;
    }

    private int cell1S7E;
    private int cell1S7E() throws GameActionException {
        if(cell1S7E == 0) {
            MapLocation loc = origin.translate(7, -1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1S7E = 1_000_000;
            } else {
                cell1S7E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N7E(), cell2S6E()), cell1S6E()), cell0N6E());
            }
        }
        return cell1S7E;
    }

    private int cell0N7W;
    private int cell0N7W() throws GameActionException {
        if(cell0N7W == 0) {
            MapLocation loc = origin.translate(-7, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N7W = 1_000_000;
            } else {
                cell0N7W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1N6W(), cell0N6W()), cell1S6W());
            }
        }
        return cell0N7W;
    }

    private int cell0N6W;
    private int cell0N6W() throws GameActionException {
        if(cell0N6W == 0) {
            MapLocation loc = origin.translate(-6, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N6W = 1_000_000;
            } else {
                cell0N6W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1N5W(), cell0N5W()), cell1S5W());
            }
        }
        return cell0N6W;
    }

    private int cell0N5W;
    private int cell0N5W() throws GameActionException {
        if(cell0N5W == 0) {
            MapLocation loc = origin.translate(-5, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N5W = 1_000_000;
            } else {
                cell0N5W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1N4W(), cell0N4W()), cell1S4W());
            }
        }
        return cell0N5W;
    }

    private int cell0N4W;
    private int cell0N4W() throws GameActionException {
        if(cell0N4W == 0) {
            MapLocation loc = origin.translate(-4, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N4W = 1_000_000;
            } else {
                cell0N4W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1N3W(), cell0N3W()), cell1S3W());
            }
        }
        return cell0N4W;
    }

    private int cell0N3W;
    private int cell0N3W() throws GameActionException {
        if(cell0N3W == 0) {
            MapLocation loc = origin.translate(-3, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N3W = 1_000_000;
            } else {
                cell0N3W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1N2W(), cell0N2W()), cell1S2W());
            }
        }
        return cell0N3W;
    }

    private int cell0N2W;
    private int cell0N2W() throws GameActionException {
        if(cell0N2W == 0) {
            MapLocation loc = origin.translate(-2, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N2W = 1_000_000;
            } else {
                cell0N2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1N1W(), cell0N1W()), cell1S1W());
            }
        }
        return cell0N2W;
    }

    private int cell0N1W;
    private int cell0N1W() throws GameActionException {
        if(cell0N1W == 0) {
            MapLocation loc = origin.translate(-1, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N1W = 1_000_000;
            } else {
                cell0N1W = ((100*rc.senseRubble(loc))+Direction.WEST.ordinal()) + cell0N0E();
            }
        }
        return cell0N1W;
    }

    private int cell0N0E;
    private int cell0N0E() throws GameActionException {
        if(cell0N0E == 0) {
            MapLocation loc = origin.translate(0, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N0E = 1_000_000;
            } else {
                cell0N0E = 100+Direction.CENTER.ordinal();
            }
        }
        return cell0N0E;
    }

    private int cell0N1E;
    private int cell0N1E() throws GameActionException {
        if(cell0N1E == 0) {
            MapLocation loc = origin.translate(1, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N1E = 1_000_000;
            } else {
                cell0N1E = ((100*rc.senseRubble(loc))+Direction.EAST.ordinal()) + cell0N0E();
            }
        }
        return cell0N1E;
    }

    private int cell0N2E;
    private int cell0N2E() throws GameActionException {
        if(cell0N2E == 0) {
            MapLocation loc = origin.translate(2, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N2E = 1_000_000;
            } else {
                cell0N2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1S1E(), cell0N1E()), cell1N1E());
            }
        }
        return cell0N2E;
    }

    private int cell0N3E;
    private int cell0N3E() throws GameActionException {
        if(cell0N3E == 0) {
            MapLocation loc = origin.translate(3, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N3E = 1_000_000;
            } else {
                cell0N3E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1S2E(), cell0N2E()), cell1N2E());
            }
        }
        return cell0N3E;
    }

    private int cell0N4E;
    private int cell0N4E() throws GameActionException {
        if(cell0N4E == 0) {
            MapLocation loc = origin.translate(4, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N4E = 1_000_000;
            } else {
                cell0N4E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1S3E(), cell0N3E()), cell1N3E());
            }
        }
        return cell0N4E;
    }

    private int cell0N5E;
    private int cell0N5E() throws GameActionException {
        if(cell0N5E == 0) {
            MapLocation loc = origin.translate(5, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N5E = 1_000_000;
            } else {
                cell0N5E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1S4E(), cell0N4E()), cell1N4E());
            }
        }
        return cell0N5E;
    }

    private int cell0N6E;
    private int cell0N6E() throws GameActionException {
        if(cell0N6E == 0) {
            MapLocation loc = origin.translate(6, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N6E = 1_000_000;
            } else {
                cell0N6E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1S5E(), cell0N5E()), cell1N5E());
            }
        }
        return cell0N6E;
    }

    private int cell0N7E;
    private int cell0N7E() throws GameActionException {
        if(cell0N7E == 0) {
            MapLocation loc = origin.translate(7, 0);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell0N7E = 1_000_000;
            } else {
                cell0N7E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1S6E(), cell0N6E()), cell1N6E());
            }
        }
        return cell0N7E;
    }

    private int cell1N7W;
    private int cell1N7W() throws GameActionException {
        if(cell1N7W == 0) {
            MapLocation loc = origin.translate(-7, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N7W = 1_000_000;
            } else {
                cell1N7W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell2N6W(), cell1N6W()), cell0N6W()), cell0N7W());
            }
        }
        return cell1N7W;
    }

    private int cell1N6W;
    private int cell1N6W() throws GameActionException {
        if(cell1N6W == 0) {
            MapLocation loc = origin.translate(-6, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N6W = 1_000_000;
            } else {
                cell1N6W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell2N5W(), cell1N5W()), cell0N5W()), cell0N6W());
            }
        }
        return cell1N6W;
    }

    private int cell1N5W;
    private int cell1N5W() throws GameActionException {
        if(cell1N5W == 0) {
            MapLocation loc = origin.translate(-5, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N5W = 1_000_000;
            } else {
                cell1N5W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell2N4W(), cell1N4W()), cell0N4W()), cell0N5W());
            }
        }
        return cell1N5W;
    }

    private int cell1N4W;
    private int cell1N4W() throws GameActionException {
        if(cell1N4W == 0) {
            MapLocation loc = origin.translate(-4, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N4W = 1_000_000;
            } else {
                cell1N4W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell2N3W(), cell1N3W()), cell0N3W()), cell0N4W());
            }
        }
        return cell1N4W;
    }

    private int cell1N3W;
    private int cell1N3W() throws GameActionException {
        if(cell1N3W == 0) {
            MapLocation loc = origin.translate(-3, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N3W = 1_000_000;
            } else {
                cell1N3W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell2N2W(), cell1N2W()), cell0N2W()), cell0N3W());
            }
        }
        return cell1N3W;
    }

    private int cell1N2W;
    private int cell1N2W() throws GameActionException {
        if(cell1N2W == 0) {
            MapLocation loc = origin.translate(-2, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N2W = 1_000_000;
            } else {
                cell1N2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1N1W(), cell0N1W()), cell0N2W());
            }
        }
        return cell1N2W;
    }

    private int cell1N1W;
    private int cell1N1W() throws GameActionException {
        if(cell1N1W == 0) {
            MapLocation loc = origin.translate(-1, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N1W = 1_000_000;
            } else {
                cell1N1W = ((100*rc.senseRubble(loc))+Direction.NORTHWEST.ordinal()) + cell1N0E();
                cell1N1W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1N0E(), cell0N0E()), cell0N1W());
            }
        }
        return cell1N1W;
    }

    private int cell1N0E;
    private int cell1N0E() throws GameActionException {
        if(cell1N0E == 0) {
            MapLocation loc = origin.translate(0, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N0E = 1_000_000;
            } else {
                cell1N0E = ((100*rc.senseRubble(loc))+Direction.NORTH.ordinal()) + cell0N0E();
            }
        }
        return cell1N0E;
    }

    private int cell1N1E;
    private int cell1N1E() throws GameActionException {
        if(cell1N1E == 0) {
            MapLocation loc = origin.translate(1, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N1E = 1_000_000;
            } else {
                cell1N1E = ((100*rc.senseRubble(loc))+Direction.NORTHEAST.ordinal()) + cell0N1E();
                cell1N1E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell0N1E(), cell0N0E()), cell1N0E());
            }
        }
        return cell1N1E;
    }

    private int cell1N2E;
    private int cell1N2E() throws GameActionException {
        if(cell1N2E == 0) {
            MapLocation loc = origin.translate(2, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N2E = 1_000_000;
            } else {
                cell1N2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell0N2E(), cell0N1E()), cell1N1E());
            }
        }
        return cell1N2E;
    }

    private int cell1N3E;
    private int cell1N3E() throws GameActionException {
        if(cell1N3E == 0) {
            MapLocation loc = origin.translate(3, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N3E = 1_000_000;
            } else {
                cell1N3E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N3E(), cell0N2E()), cell1N2E()), cell2N2E());
            }
        }
        return cell1N3E;
    }

    private int cell1N4E;
    private int cell1N4E() throws GameActionException {
        if(cell1N4E == 0) {
            MapLocation loc = origin.translate(4, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N4E = 1_000_000;
            } else {
                cell1N4E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N4E(), cell0N3E()), cell1N3E()), cell2N3E());
            }
        }
        return cell1N4E;
    }

    private int cell1N5E;
    private int cell1N5E() throws GameActionException {
        if(cell1N5E == 0) {
            MapLocation loc = origin.translate(5, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N5E = 1_000_000;
            } else {
                cell1N5E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N5E(), cell0N4E()), cell1N4E()), cell2N4E());
            }
        }
        return cell1N5E;
    }

    private int cell1N6E;
    private int cell1N6E() throws GameActionException {
        if(cell1N6E == 0) {
            MapLocation loc = origin.translate(6, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N6E = 1_000_000;
            } else {
                cell1N6E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N6E(), cell0N5E()), cell1N5E()), cell2N5E());
            }
        }
        return cell1N6E;
    }

    private int cell1N7E;
    private int cell1N7E() throws GameActionException {
        if(cell1N7E == 0) {
            MapLocation loc = origin.translate(7, 1);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell1N7E = 1_000_000;
            } else {
                cell1N7E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell0N7E(), cell0N6E()), cell1N6E()), cell2N6E());
            }
        }
        return cell1N7E;
    }

    private int cell2N7W;
    private int cell2N7W() throws GameActionException {
        if(cell2N7W == 0) {
            MapLocation loc = origin.translate(-7, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N7W = 1_000_000;
            } else {
                cell2N7W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell3N6W(), cell2N6W()), cell1N6W()), cell1N7W());
            }
        }
        return cell2N7W;
    }

    private int cell2N6W;
    private int cell2N6W() throws GameActionException {
        if(cell2N6W == 0) {
            MapLocation loc = origin.translate(-6, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N6W = 1_000_000;
            } else {
                cell2N6W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell3N5W(), cell2N5W()), cell1N5W()), cell1N6W());
            }
        }
        return cell2N6W;
    }

    private int cell2N5W;
    private int cell2N5W() throws GameActionException {
        if(cell2N5W == 0) {
            MapLocation loc = origin.translate(-5, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N5W = 1_000_000;
            } else {
                cell2N5W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell3N4W(), cell2N4W()), cell1N4W()), cell1N5W());
            }
        }
        return cell2N5W;
    }

    private int cell2N4W;
    private int cell2N4W() throws GameActionException {
        if(cell2N4W == 0) {
            MapLocation loc = origin.translate(-4, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N4W = 1_000_000;
            } else {
                cell2N4W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell3N3W(), cell2N3W()), cell1N3W()), cell1N4W());
            }
        }
        return cell2N4W;
    }

    private int cell2N3W;
    private int cell2N3W() throws GameActionException {
        if(cell2N3W == 0) {
            MapLocation loc = origin.translate(-3, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N3W = 1_000_000;
            } else {
                cell2N3W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell2N2W(), cell1N2W()), cell1N3W());
            }
        }
        return cell2N3W;
    }

    private int cell2N2W;
    private int cell2N2W() throws GameActionException {
        if(cell2N2W == 0) {
            MapLocation loc = origin.translate(-2, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N2W = 1_000_000;
            } else {
                cell2N2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell2N1W(), cell1N1W()), cell1N2W());
            }
        }
        return cell2N2W;
    }

    private int cell2N1W;
    private int cell2N1W() throws GameActionException {
        if(cell2N1W == 0) {
            MapLocation loc = origin.translate(-1, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N1W = 1_000_000;
            } else {
                cell2N1W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell2N0E(), cell1N0E()), cell1N1W());
            }
        }
        return cell2N1W;
    }

    private int cell2N0E;
    private int cell2N0E() throws GameActionException {
        if(cell2N0E == 0) {
            MapLocation loc = origin.translate(0, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N0E = 1_000_000;
            } else {
                cell2N0E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1N1E(), cell1N0E()), cell1N1W());
            }
        }
        return cell2N0E;
    }

    private int cell2N1E;
    private int cell2N1E() throws GameActionException {
        if(cell2N1E == 0) {
            MapLocation loc = origin.translate(1, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N1E = 1_000_000;
            } else {
                cell2N1E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1N1E(), cell1N0E()), cell2N0E());
            }
        }
        return cell2N1E;
    }

    private int cell2N2E;
    private int cell2N2E() throws GameActionException {
        if(cell2N2E == 0) {
            MapLocation loc = origin.translate(2, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N2E = 1_000_000;
            } else {
                cell2N2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1N2E(), cell1N1E()), cell2N1E());
            }
        }
        return cell2N2E;
    }

    private int cell2N3E;
    private int cell2N3E() throws GameActionException {
        if(cell2N3E == 0) {
            MapLocation loc = origin.translate(3, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N3E = 1_000_000;
            } else {
                cell2N3E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell1N3E(), cell1N2E()), cell2N2E());
            }
        }
        return cell2N3E;
    }

    private int cell2N4E;
    private int cell2N4E() throws GameActionException {
        if(cell2N4E == 0) {
            MapLocation loc = origin.translate(4, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N4E = 1_000_000;
            } else {
                cell2N4E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell1N4E(), cell1N3E()), cell2N3E()), cell3N3E());
            }
        }
        return cell2N4E;
    }

    private int cell2N5E;
    private int cell2N5E() throws GameActionException {
        if(cell2N5E == 0) {
            MapLocation loc = origin.translate(5, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N5E = 1_000_000;
            } else {
                cell2N5E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell1N5E(), cell1N4E()), cell2N4E()), cell3N4E());
            }
        }
        return cell2N5E;
    }

    private int cell2N6E;
    private int cell2N6E() throws GameActionException {
        if(cell2N6E == 0) {
            MapLocation loc = origin.translate(6, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N6E = 1_000_000;
            } else {
                cell2N6E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell1N6E(), cell1N5E()), cell2N5E()), cell3N5E());
            }
        }
        return cell2N6E;
    }

    private int cell2N7E;
    private int cell2N7E() throws GameActionException {
        if(cell2N7E == 0) {
            MapLocation loc = origin.translate(7, 2);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell2N7E = 1_000_000;
            } else {
                cell2N7E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell1N7E(), cell1N6E()), cell2N6E()), cell3N6E());
            }
        }
        return cell2N7E;
    }

    private int cell3N6W;
    private int cell3N6W() throws GameActionException {
        if(cell3N6W == 0) {
            MapLocation loc = origin.translate(-6, 3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3N6W = 1_000_000;
            } else {
                cell3N6W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell4N5W(), cell3N5W()), cell2N5W()), cell2N6W());
            }
        }
        return cell3N6W;
    }

    private int cell3N5W;
    private int cell3N5W() throws GameActionException {
        if(cell3N5W == 0) {
            MapLocation loc = origin.translate(-5, 3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3N5W = 1_000_000;
            } else {
                cell3N5W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell4N4W(), cell3N4W()), cell2N4W()), cell2N5W());
            }
        }
        return cell3N5W;
    }

    private int cell3N4W;
    private int cell3N4W() throws GameActionException {
        if(cell3N4W == 0) {
            MapLocation loc = origin.translate(-4, 3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3N4W = 1_000_000;
            } else {
                cell3N4W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell3N3W(), cell2N3W()), cell2N4W());
            }
        }
        return cell3N4W;
    }

    private int cell3N3W;
    private int cell3N3W() throws GameActionException {
        if(cell3N3W == 0) {
            MapLocation loc = origin.translate(-3, 3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3N3W = 1_000_000;
            } else {
                cell3N3W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell3N2W(), cell2N2W()), cell2N3W());
            }
        }
        return cell3N3W;
    }

    private int cell3N2W;
    private int cell3N2W() throws GameActionException {
        if(cell3N2W == 0) {
            MapLocation loc = origin.translate(-2, 3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3N2W = 1_000_000;
            } else {
                cell3N2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell3N1W(), cell2N1W()), cell2N2W());
            }
        }
        return cell3N2W;
    }

    private int cell3N1W;
    private int cell3N1W() throws GameActionException {
        if(cell3N1W == 0) {
            MapLocation loc = origin.translate(-1, 3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3N1W = 1_000_000;
            } else {
                cell3N1W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell3N0E(), cell2N0E()), cell2N1W()), cell2N2W());
            }
        }
        return cell3N1W;
    }

    private int cell3N0E;
    private int cell3N0E() throws GameActionException {
        if(cell3N0E == 0) {
            MapLocation loc = origin.translate(0, 3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3N0E = 1_000_000;
            } else {
                cell3N0E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell2N1E(), cell2N0E()), cell2N1W());
            }
        }
        return cell3N0E;
    }

    private int cell3N1E;
    private int cell3N1E() throws GameActionException {
        if(cell3N1E == 0) {
            MapLocation loc = origin.translate(1, 3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3N1E = 1_000_000;
            } else {
                cell3N1E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell2N2E(), cell2N1E()), cell2N0E()), cell3N0E());
            }
        }
        return cell3N1E;
    }

    private int cell3N2E;
    private int cell3N2E() throws GameActionException {
        if(cell3N2E == 0) {
            MapLocation loc = origin.translate(2, 3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3N2E = 1_000_000;
            } else {
                cell3N2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell2N2E(), cell2N1E()), cell3N1E());
            }
        }
        return cell3N2E;
    }

    private int cell3N3E;
    private int cell3N3E() throws GameActionException {
        if(cell3N3E == 0) {
            MapLocation loc = origin.translate(3, 3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3N3E = 1_000_000;
            } else {
                cell3N3E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell2N3E(), cell2N2E()), cell3N2E());
            }
        }
        return cell3N3E;
    }

    private int cell3N4E;
    private int cell3N4E() throws GameActionException {
        if(cell3N4E == 0) {
            MapLocation loc = origin.translate(4, 3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3N4E = 1_000_000;
            } else {
                cell3N4E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell2N4E(), cell2N3E()), cell3N3E());
            }
        }
        return cell3N4E;
    }

    private int cell3N5E;
    private int cell3N5E() throws GameActionException {
        if(cell3N5E == 0) {
            MapLocation loc = origin.translate(5, 3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3N5E = 1_000_000;
            } else {
                cell3N5E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell2N5E(), cell2N4E()), cell3N4E()), cell4N4E());
            }
        }
        return cell3N5E;
    }

    private int cell3N6E;
    private int cell3N6E() throws GameActionException {
        if(cell3N6E == 0) {
            MapLocation loc = origin.translate(6, 3);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell3N6E = 1_000_000;
            } else {
                cell3N6E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell2N6E(), cell2N5E()), cell3N5E()), cell4N5E());
            }
        }
        return cell3N6E;
    }

    private int cell4N6W;
    private int cell4N6W() throws GameActionException {
        if(cell4N6W == 0) {
            MapLocation loc = origin.translate(-6, 4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4N6W = 1_000_000;
            } else {
                cell4N6W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5N5W(), cell4N5W()), cell3N5W()), cell3N6W());
            }
        }
        return cell4N6W;
    }

    private int cell4N5W;
    private int cell4N5W() throws GameActionException {
        if(cell4N5W == 0) {
            MapLocation loc = origin.translate(-5, 4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4N5W = 1_000_000;
            } else {
                cell4N5W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell4N4W(), cell3N4W()), cell3N5W());
            }
        }
        return cell4N5W;
    }

    private int cell4N4W;
    private int cell4N4W() throws GameActionException {
        if(cell4N4W == 0) {
            MapLocation loc = origin.translate(-4, 4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4N4W = 1_000_000;
            } else {
                cell4N4W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell4N3W(), cell3N3W()), cell3N4W());
            }
        }
        return cell4N4W;
    }

    private int cell4N3W;
    private int cell4N3W() throws GameActionException {
        if(cell4N3W == 0) {
            MapLocation loc = origin.translate(-3, 4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4N3W = 1_000_000;
            } else {
                cell4N3W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell4N2W(), cell3N2W()), cell3N3W());
            }
        }
        return cell4N3W;
    }

    private int cell4N2W;
    private int cell4N2W() throws GameActionException {
        if(cell4N2W == 0) {
            MapLocation loc = origin.translate(-2, 4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4N2W = 1_000_000;
            } else {
                cell4N2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell4N1W(), cell3N1W()), cell3N2W()), cell3N3W());
            }
        }
        return cell4N2W;
    }

    private int cell4N1W;
    private int cell4N1W() throws GameActionException {
        if(cell4N1W == 0) {
            MapLocation loc = origin.translate(-1, 4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4N1W = 1_000_000;
            } else {
                cell4N1W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell4N0E(), cell3N0E()), cell3N1W()), cell3N2W());
            }
        }
        return cell4N1W;
    }

    private int cell4N0E;
    private int cell4N0E() throws GameActionException {
        if(cell4N0E == 0) {
            MapLocation loc = origin.translate(0, 4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4N0E = 1_000_000;
            } else {
                cell4N0E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell3N1E(), cell3N0E()), cell3N1W());
            }
        }
        return cell4N0E;
    }

    private int cell4N1E;
    private int cell4N1E() throws GameActionException {
        if(cell4N1E == 0) {
            MapLocation loc = origin.translate(1, 4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4N1E = 1_000_000;
            } else {
                cell4N1E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell3N2E(), cell3N1E()), cell3N0E()), cell4N0E());
            }
        }
        return cell4N1E;
    }

    private int cell4N2E;
    private int cell4N2E() throws GameActionException {
        if(cell4N2E == 0) {
            MapLocation loc = origin.translate(2, 4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4N2E = 1_000_000;
            } else {
                cell4N2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell3N3E(), cell3N2E()), cell3N1E()), cell4N1E());
            }
        }
        return cell4N2E;
    }

    private int cell4N3E;
    private int cell4N3E() throws GameActionException {
        if(cell4N3E == 0) {
            MapLocation loc = origin.translate(3, 4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4N3E = 1_000_000;
            } else {
                cell4N3E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell3N3E(), cell3N2E()), cell4N2E());
            }
        }
        return cell4N3E;
    }

    private int cell4N4E;
    private int cell4N4E() throws GameActionException {
        if(cell4N4E == 0) {
            MapLocation loc = origin.translate(4, 4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4N4E = 1_000_000;
            } else {
                cell4N4E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell3N4E(), cell3N3E()), cell4N3E());
            }
        }
        return cell4N4E;
    }

    private int cell4N5E;
    private int cell4N5E() throws GameActionException {
        if(cell4N5E == 0) {
            MapLocation loc = origin.translate(5, 4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4N5E = 1_000_000;
            } else {
                cell4N5E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell3N5E(), cell3N4E()), cell4N4E());
            }
        }
        return cell4N5E;
    }

    private int cell4N6E;
    private int cell4N6E() throws GameActionException {
        if(cell4N6E == 0) {
            MapLocation loc = origin.translate(6, 4);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell4N6E = 1_000_000;
            } else {
                cell4N6E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell3N6E(), cell3N5E()), cell4N5E()), cell5N5E());
            }
        }
        return cell4N6E;
    }

    private int cell5N5W;
    private int cell5N5W() throws GameActionException {
        if(cell5N5W == 0) {
            MapLocation loc = origin.translate(-5, 5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5N5W = 1_000_000;
            } else {
                cell5N5W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell5N4W(), cell4N4W()), cell4N5W());
            }
        }
        return cell5N5W;
    }

    private int cell5N4W;
    private int cell5N4W() throws GameActionException {
        if(cell5N4W == 0) {
            MapLocation loc = origin.translate(-4, 5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5N4W = 1_000_000;
            } else {
                cell5N4W = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell5N3W(), cell4N3W()), cell4N4W());
            }
        }
        return cell5N4W;
    }

    private int cell5N3W;
    private int cell5N3W() throws GameActionException {
        if(cell5N3W == 0) {
            MapLocation loc = origin.translate(-3, 5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5N3W = 1_000_000;
            } else {
                cell5N3W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5N2W(), cell4N2W()), cell4N3W()), cell4N4W());
            }
        }
        return cell5N3W;
    }

    private int cell5N2W;
    private int cell5N2W() throws GameActionException {
        if(cell5N2W == 0) {
            MapLocation loc = origin.translate(-2, 5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5N2W = 1_000_000;
            } else {
                cell5N2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5N1W(), cell4N1W()), cell4N2W()), cell4N3W());
            }
        }
        return cell5N2W;
    }

    private int cell5N1W;
    private int cell5N1W() throws GameActionException {
        if(cell5N1W == 0) {
            MapLocation loc = origin.translate(-1, 5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5N1W = 1_000_000;
            } else {
                cell5N1W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5N0E(), cell4N0E()), cell4N1W()), cell4N2W());
            }
        }
        return cell5N1W;
    }

    private int cell5N0E;
    private int cell5N0E() throws GameActionException {
        if(cell5N0E == 0) {
            MapLocation loc = origin.translate(0, 5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5N0E = 1_000_000;
            } else {
                cell5N0E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell4N1E(), cell4N0E()), cell4N1W());
            }
        }
        return cell5N0E;
    }

    private int cell5N1E;
    private int cell5N1E() throws GameActionException {
        if(cell5N1E == 0) {
            MapLocation loc = origin.translate(1, 5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5N1E = 1_000_000;
            } else {
                cell5N1E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell4N2E(), cell4N1E()), cell4N0E()), cell5N0E());
            }
        }
        return cell5N1E;
    }

    private int cell5N2E;
    private int cell5N2E() throws GameActionException {
        if(cell5N2E == 0) {
            MapLocation loc = origin.translate(2, 5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5N2E = 1_000_000;
            } else {
                cell5N2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell4N3E(), cell4N2E()), cell4N1E()), cell5N1E());
            }
        }
        return cell5N2E;
    }

    private int cell5N3E;
    private int cell5N3E() throws GameActionException {
        if(cell5N3E == 0) {
            MapLocation loc = origin.translate(3, 5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5N3E = 1_000_000;
            } else {
                cell5N3E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell4N4E(), cell4N3E()), cell4N2E()), cell5N2E());
            }
        }
        return cell5N3E;
    }

    private int cell5N4E;
    private int cell5N4E() throws GameActionException {
        if(cell5N4E == 0) {
            MapLocation loc = origin.translate(4, 5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5N4E = 1_000_000;
            } else {
                cell5N4E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell4N4E(), cell4N3E()), cell5N3E());
            }
        }
        return cell5N4E;
    }

    private int cell5N5E;
    private int cell5N5E() throws GameActionException {
        if(cell5N5E == 0) {
            MapLocation loc = origin.translate(5, 5);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell5N5E = 1_000_000;
            } else {
                cell5N5E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell4N5E(), cell4N4E()), cell5N4E());
            }
        }
        return cell5N5E;
    }

    private int cell6N4W;
    private int cell6N4W() throws GameActionException {
        if(cell6N4W == 0) {
            MapLocation loc = origin.translate(-4, 6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6N4W = 1_000_000;
            } else {
                cell6N4W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell6N3W(), cell5N3W()), cell5N4W()), cell5N5W());
            }
        }
        return cell6N4W;
    }

    private int cell6N3W;
    private int cell6N3W() throws GameActionException {
        if(cell6N3W == 0) {
            MapLocation loc = origin.translate(-3, 6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6N3W = 1_000_000;
            } else {
                cell6N3W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell6N2W(), cell5N2W()), cell5N3W()), cell5N4W());
            }
        }
        return cell6N3W;
    }

    private int cell6N2W;
    private int cell6N2W() throws GameActionException {
        if(cell6N2W == 0) {
            MapLocation loc = origin.translate(-2, 6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6N2W = 1_000_000;
            } else {
                cell6N2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell6N1W(), cell5N1W()), cell5N2W()), cell5N3W());
            }
        }
        return cell6N2W;
    }

    private int cell6N1W;
    private int cell6N1W() throws GameActionException {
        if(cell6N1W == 0) {
            MapLocation loc = origin.translate(-1, 6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6N1W = 1_000_000;
            } else {
                cell6N1W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell6N0E(), cell5N0E()), cell5N1W()), cell5N2W());
            }
        }
        return cell6N1W;
    }

    private int cell6N0E;
    private int cell6N0E() throws GameActionException {
        if(cell6N0E == 0) {
            MapLocation loc = origin.translate(0, 6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6N0E = 1_000_000;
            } else {
                cell6N0E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell5N1E(), cell5N0E()), cell5N1W());
            }
        }
        return cell6N0E;
    }

    private int cell6N1E;
    private int cell6N1E() throws GameActionException {
        if(cell6N1E == 0) {
            MapLocation loc = origin.translate(1, 6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6N1E = 1_000_000;
            } else {
                cell6N1E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5N2E(), cell5N1E()), cell5N0E()), cell6N0E());
            }
        }
        return cell6N1E;
    }

    private int cell6N2E;
    private int cell6N2E() throws GameActionException {
        if(cell6N2E == 0) {
            MapLocation loc = origin.translate(2, 6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6N2E = 1_000_000;
            } else {
                cell6N2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5N3E(), cell5N2E()), cell5N1E()), cell6N1E());
            }
        }
        return cell6N2E;
    }

    private int cell6N3E;
    private int cell6N3E() throws GameActionException {
        if(cell6N3E == 0) {
            MapLocation loc = origin.translate(3, 6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6N3E = 1_000_000;
            } else {
                cell6N3E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5N4E(), cell5N3E()), cell5N2E()), cell6N2E());
            }
        }
        return cell6N3E;
    }

    private int cell6N4E;
    private int cell6N4E() throws GameActionException {
        if(cell6N4E == 0) {
            MapLocation loc = origin.translate(4, 6);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell6N4E = 1_000_000;
            } else {
                cell6N4E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell5N5E(), cell5N4E()), cell5N3E()), cell6N3E());
            }
        }
        return cell6N4E;
    }

    private int cell7N2W;
    private int cell7N2W() throws GameActionException {
        if(cell7N2W == 0) {
            MapLocation loc = origin.translate(-2, 7);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell7N2W = 1_000_000;
            } else {
                cell7N2W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell7N1W(), cell6N1W()), cell6N2W()), cell6N3W());
            }
        }
        return cell7N2W;
    }

    private int cell7N1W;
    private int cell7N1W() throws GameActionException {
        if(cell7N1W == 0) {
            MapLocation loc = origin.translate(-1, 7);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell7N1W = 1_000_000;
            } else {
                cell7N1W = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell7N0E(), cell6N0E()), cell6N1W()), cell6N2W());
            }
        }
        return cell7N1W;
    }

    private int cell7N0E;
    private int cell7N0E() throws GameActionException {
        if(cell7N0E == 0) {
            MapLocation loc = origin.translate(0, 7);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell7N0E = 1_000_000;
            } else {
                cell7N0E = (100*rc.senseRubble(loc)) + Math.min(Math.min(cell6N1E(), cell6N0E()), cell6N1W());
            }
        }
        return cell7N0E;
    }

    private int cell7N1E;
    private int cell7N1E() throws GameActionException {
        if(cell7N1E == 0) {
            MapLocation loc = origin.translate(1, 7);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell7N1E = 1_000_000;
            } else {
                cell7N1E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell6N2E(), cell6N1E()), cell6N0E()), cell7N0E());
            }
        }
        return cell7N1E;
    }

    private int cell7N2E;
    private int cell7N2E() throws GameActionException {
        if(cell7N2E == 0) {
            MapLocation loc = origin.translate(2, 7);
            if(!rc.onTheMap(loc) || rc.isLocationOccupied(loc)) {
                cell7N2E = 1_000_000;
            } else {
                cell7N2E = (100*rc.senseRubble(loc)) + Math.min(Math.min(Math.min(cell6N3E(), cell6N2E()), cell6N1E()), cell7N1E());
            }
        }
        return cell7N2E;
    }



    //return direction away from a location
    //will try to find a way to maximize distance even if it's not directly away
    Direction getAwayDir(MapLocation loc) throws GameActionException {
        MapLocation src = rc.getLocation();

        Direction opposite = src.directionTo(loc).opposite();

        MapLocation runawayTgt = src.add(opposite).add(opposite);
        runawayTgt = new MapLocation(Math.min(Math.max(0, runawayTgt.x), rc.getMapWidth() - 1),
                Math.min(Math.max(0, runawayTgt.y), rc.getMapHeight() - 1));
        Direction toReturn = getMoveDir(runawayTgt);

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
