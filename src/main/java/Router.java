import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.ArrayList;

/**
 * This class provides a <code>shortestPath</code> method and <code>routeDirections</code> for
 * finding routes between two points on the map.
 */
public class Router {
    /**
     * Return a <code>List</code> of vertex IDs corresponding to the shortest path from a given
     * starting coordinate and destination coordinate.
     *
     * @param g       <code>GraphDB</code> data source.
     * @param stlon   The longitude of the starting coordinate.
     * @param stlat   The latitude of the starting coordinate.
     * @param destlon The longitude of the destination coordinate.
     * @param destlat The latitude of the destination coordinate.
     * @return The <code>List</code> of vertex IDs corresponding to the shortest paths
     */

    public static List<Long> shortestPath(GraphDB g,
                                          double stlon, double stlat,
                                          double destlon, double destlat) {
        long startID = g.closest(stlon, stlat);
        PriorityNode startNode = new PriorityNode(startID, 0.0);
        long destID = g.closest(destlon, destlat);
        PriorityQueue<PriorityNode> fringe = new PriorityQueue<>();
        fringe.add(startNode);
        HashMap<Long, Double> best = new HashMap<>();
        HashMap<Long, Long> prev = new HashMap<>();
        best.put(startID, 0.0);
        prev.put(startID, null);
        ArrayList<Long> visited = new ArrayList<>();
        ArrayList<Long> reversedShortPath = new ArrayList<>();
        ArrayList<Long> shortPath = new ArrayList<>();
        long currID = startID;
        while (!fringe.isEmpty() && currID != destID) {
            PriorityNode currNode = fringe.poll();
            currID = currNode.iD;
            if (!visited.contains(currID)) {
                for (long neighborID : g.adjacent(currID)) {
                    if (visited.contains(neighborID)) {
                        continue;
                    }
                    double startIDtoCurrID = best.get(currID);
                    double currIDtoNeighborID = g.distance(currID, neighborID);
                    double neighborIDtoDestID = g.distance(neighborID, destID);
                    double ourStartIDToNeighborID = startIDtoCurrID + currIDtoNeighborID;
                    if (best.containsKey(neighborID)) {
                        if (ourStartIDToNeighborID < best.get(neighborID)) {
                            best.put(neighborID, ourStartIDToNeighborID);
                            prev.put(neighborID, currID);
                            fringe.add(new PriorityNode(neighborID,
                                    ourStartIDToNeighborID + neighborIDtoDestID));
                        }
                    } else {
                        best.put(neighborID, ourStartIDToNeighborID);
                        prev.put(neighborID, currID);
                        fringe.add(new PriorityNode(neighborID,
                                ourStartIDToNeighborID + neighborIDtoDestID));
                    }
                }
//                try {
////                if (!visited.contains(currNode.iD)) {
////                    visited.add(currNode.iD);
////                    currID = currNode.iD;
////                }
//                    visited.add(currNode.iD);
//                    currID = currNode.iD;
//                } catch (NullPointerException e) {
//                    return Collections.emptyList();
//                }
                //PriorityNode currNode = fringe.poll();
            }
            try {
                if (!visited.contains(currNode.iD)) {
                    visited.add(currNode.iD);
                    currID = currNode.iD;
                }
                visited.add(currNode.iD);
                currID = currNode.iD;
            } catch (NullPointerException e) {
                return Collections.emptyList();
            }
        }
        while (currID != startID) {
            reversedShortPath.add(currID);
            currID = prev.get(currID);
        }
        reversedShortPath.add(startID);
        for (int i = 1; i < reversedShortPath.size() + 1; i++) {
            shortPath.add(reversedShortPath.get(reversedShortPath.size() - i));
        }
        return shortPath;
    }
    public static class PriorityNode implements Comparable<PriorityNode> {
        private long iD;
        private double priority;
        private PriorityNode(long iD, double priority) {
            this.iD = iD;
            this.priority = priority;
        }
        @Override
        public int compareTo(PriorityNode o) {
            double diff = this.priority - o.priority;
            if (diff > 0) {
                return 1;
            } else if (diff < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }

//    public static List<Long> shortestPath(GraphDB g,
//                                          double stlon, double stlat,
//                                          double destlon, double destlat) {
//        long startID = g.closest(stlon, stlat);
//        PriorityNode startNode = new PriorityNode(startID, 0.0);
//        long destID = g.closest(destlon, destlat);
//        //data structures: fringe priority queue
//        PriorityQueue<PriorityNode> fringe = new PriorityQueue<>();
//        //fringe.add(startNode);
//        HashMap<Long, Double> best = new HashMap<>();
//        HashMap<Long, Long> prev = new HashMap<>();
//        best.put(startID, 0.0);
//        prev.put(startID, null);
//        ArrayList<Long> visited = new ArrayList<>();
//        ArrayList<Long> reversedShortPath = new ArrayList<>();
//        ArrayList<Long> shortPath = new ArrayList<>();
//        long currID = startID;
//        while (currID != destID) {
//            for (long neighborID : g.adjacent(currID)) {
//                if (visited.contains(neighborID)) {
//                    continue;
//                }
//                double startIDtoCurrID = best.get(currID);
//                double currIDtoNeighborID = g.distance(currID, neighborID);
//                double neighborIDtoDestID = g.distance(neighborID, destID);
//                double ourStartIDToNeighborID = startIDtoCurrID + currIDtoNeighborID;
//                if (best.containsKey(neighborID)) {
//                    if (ourStartIDToNeighborID < best.get(neighborID)) {
//                        best.put(neighborID, ourStartIDToNeighborID);
//                        prev.put(neighborID, currID);
//                        fringe.add(new PriorityNode(neighborID,
//                                ourStartIDToNeighborID + neighborIDtoDestID));
//                    }
//                } else {
//                    best.put(neighborID, ourStartIDToNeighborID);
//                    prev.put(neighborID, currID);
//                    fringe.add(new PriorityNode(neighborID,
//                            ourStartIDToNeighborID + neighborIDtoDestID));
//                }
//            }
//            PriorityNode currNode = fringe.poll();
//            try {
//                if (!visited.contains(currNode.iD)) {
//                    visited.add(currNode.iD);
//                    currID = currNode.iD;
//                }
//            } catch (NullPointerException e) {
//                return Collections.emptyList();
//            }
//        }
//        while (currID != startID) {
//            reversedShortPath.add(currID);
//            currID = prev.get(currID);
//        }
//        reversedShortPath.add(startID);
//        for (int i = 1; i < reversedShortPath.size() + 1; i++) {
//            shortPath.add(reversedShortPath.get(reversedShortPath.size() - i));
//        }
//        return shortPath;
//    }
//
//    public static class PriorityNode implements Comparable<PriorityNode> {
//        private long iD;
//        private double priority;
//        private PriorityNode(long iD, double priority) {
//            this.iD = iD;
//            this.priority = priority;
//        }
//        @Override
//        public int compareTo(PriorityNode o) {
//            double diff = this.priority - o.priority;
//            if (diff > 0) {
//                return 1;
//            } else if (diff < 0) {
//                return -1;
//            } else {
//                return 0;
//            }
//        }
//    }

    /**
     * Given a <code>route</code> of vertex IDs, return a <code>List</code> of
     * <code>NavigationDirection</code> objects representing the travel directions in order.
     * @param g <code>GraphDB</code> data source.
     * @param route The shortest-path route of vertex IDs.
     * @return A new <code>List</code> of <code>NavigationDirection</code> objects.
     */
    public static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {
        // TODO
        return Collections.emptyList();
    }
    /**
     * Class to represent a navigation direction, which consists of 3 attributes:
     * a direction to go, a way, and the distance to travel for.
     */
    public static class NavigationDirection {
        /** Integer constants representing directions. */
        public static final int START = 0, STRAIGHT = 1, SLIGHT_LEFT = 2, SLIGHT_RIGHT = 3,
                RIGHT = 4, LEFT = 5, SHARP_LEFT = 6, SHARP_RIGHT = 7;
        /** Number of directions supported. */
        public static final int NUM_DIRECTIONS = 8;
        /** A mapping of integer values to directions.*/
        public static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];
        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }
        /** The direction represented.*/
        int direction;
        /** The name of this way. */
        String way;
        /** The distance along this way. */
        double distance = 0.0;
        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }
        /**
         * Returns a new <code>NavigationDirection</code> from a string representation.
         * @param dirAsString <code>String</code> instructions for a navigation direction.
         * @return A new <code>NavigationDirection</code> based on the string, or <code>null</code>
         * if unable to parse.
         */
        public static NavigationDirection fromString(String dirAsString) {
            String regex = "([a-zA-Z\\s]+) on ([\\w\\s]*) and continue for ([0-9\\.]+) miles\\.";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(dirAsString);
            NavigationDirection nd = new NavigationDirection();
            if (m.matches()) {
                String direction = m.group(1);
                if (direction.equals("Start")) {
                    nd.direction = NavigationDirection.START;
                } else if (direction.equals("Go straight")) {
                    nd.direction = NavigationDirection.STRAIGHT;
                } else if (direction.equals("Slight left")) {
                    nd.direction = NavigationDirection.SLIGHT_LEFT;
                } else if (direction.equals("Slight right")) {
                    nd.direction = NavigationDirection.SLIGHT_RIGHT;
                } else if (direction.equals("Turn right")) {
                    nd.direction = NavigationDirection.RIGHT;
                } else if (direction.equals("Turn left")) {
                    nd.direction = NavigationDirection.LEFT;
                } else if (direction.equals("Sharp left")) {
                    nd.direction = NavigationDirection.SHARP_LEFT;
                } else if (direction.equals("Sharp right")) {
                    nd.direction = NavigationDirection.SHARP_RIGHT;
                } else {
                    return null;
                }
                nd.way = m.group(2);
                try {
                    nd.distance = Double.parseDouble(m.group(3));
                } catch (NumberFormatException e) {
                    return null;
                }
                return nd;
            } else {
                // Not a valid nd
                return null;
            }
        }
        @Override
        public boolean equals(Object o) {
            if (o instanceof NavigationDirection) {
                return direction == ((NavigationDirection) o).direction
                        && way.equals(((NavigationDirection) o).way)
                        && distance == ((NavigationDirection) o).distance;
            }
            return false;
        }
        @Override
        public int hashCode() {
            return Objects.hash(direction, way, distance);
        }
    }
}
