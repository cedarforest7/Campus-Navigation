import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.PriorityQueue;

/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map. Start by using Dijkstra's, and if your code isn't fast enough for your
 * satisfaction (or the autograder), upgrade your implementation by switching it to A*.
 * Your code will probably not be fast enough to pass the autograder unless you use A*.
 * The difference between A* and Dijkstra's is only a couple of lines of code, and boils
 * down to the priority you use to order your vertices.
 */
public class Router {
    /**
     * Return a List of longs representing the shortest path from the node
     * closest to a start location and the node closest to the destination
     * location.
     * @param g The graph to use.
     * @param stlon The longitude of the start location.
     * @param stlat The latitude of the start location.
     * @param destlon The longitude of the destination location.
     * @param destlat The latitude of the destination location.
     * @return A list of node id's in the order visited on the shortest path.
     */




    public static List<Long> shortestPath(GraphDB g, double stlon, double stlat,
                                          double destlon, double destlat) {
        LinkedList<Long> path = new LinkedList<>();
        Set<Long> marked = new HashSet<>();
        //Best known distance from source to every vertex w, i.e.  d(s, w)
        Map<Long, Double> best = new HashMap<>();
        //“Parent” of every vertex (similar to edgeTo in lecture, i.e. edgeTo[w]).
        Map<Long, Long> edgeTo = new HashMap<>();

        class distanceComp implements Comparator<Long> {
            double lon;
            double lat;
            public distanceComp (double lon, double lat) {
                this.lon = lon;
                this.lat = lat;
            }
            @Override
            public int compare (Long v1, Long v2) {
                double d1 = best.get(v1) + g.distance(g.lon(v1), g.lat(v1), lon, lat);
                double d2 = best.get(v2) + g.distance(g.lon(v2), g.lat(v2), lon, lat);
                if (d1 < d2) {
                   return -1;
                } else if (d1 == d2) {
                    return 0;
                } else {
                    return 1;
                }
            }
        }
        distanceComp comp = new distanceComp(destlon, destlat);
        PriorityQueue<Long> fringe = new PriorityQueue<>(11, comp);

//        double[] stcoord = {stlon, stlat};
//       Long st = g.getNode(stcoord);
        long st = g.closest(stlon, stlat);
        fringe.add(st);
        long goal = g.closest(destlon, destlat);

        for (long v : g.vertices()) {
            if (v == st) {
                best.put(v, 0.0);
            } else {
                best.put(v, Double.POSITIVE_INFINITY);
            }
        }

        while(true) {
            Long head = fringe.poll();
            marked.add(head);
            if (head == null) {
                return null;
            }
            if (head == goal) {
                for (long v = head; v != st; v = edgeTo.get(v)) {
                    path.addFirst(v);
                }
                path.addFirst(st);
                return path;
            }
            for (long v : g.adjacent(head)) {
                if (!marked.contains(v)) {
                    double dist = best.get(head) + g.distance(head, v);
                    if (dist < best.get(v)) {
                        best.put(v, dist);
                        edgeTo.put(v, head);
                        //System.out.println(v + " " + head);     //for test
                        fringe.add(v);
                    }
                }
            }
        }
    }




    /**
     * Create the list of directions corresponding to a route on the graph.
     * @param g The graph to use.
     * @param route The route to translate into directions. Each element
     *              corresponds to a node from the graph in the route.
     * @return A list of NavigatiionDirection objects corresponding to the input
     * route.
     */
    public static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {
        List<NavigationDirection> nav = new LinkedList<>();
        for (int i = 0, j = 1; j < route.size(); i = j, j++) {
            NavigationDirection nd = new NavigationDirection();
            long cur = route.get(i);
            long next = route.get(j);
            nd.way = g.edgeName(cur, next);
            nd.distance = g.distance(cur, next);
            if (i == 0) {
                nd.direction = NavigationDirection.START;
            } else {
                double anglePre = angleConvert(g.bearing(route.get(i - 1), cur));
                double angleNext = angleConvert(g.bearing(cur, next));
                double bearing = directionConvert(angleNext - anglePre);
                //System.out.println(bearing + " " + nd.way);        //for test
                if (Math.abs(bearing) <= 15) {
                    nd.direction = NavigationDirection.STRAIGHT;
                } else if (bearing > 0) {
                    if (bearing < 30) {
                        nd.direction = NavigationDirection.SLIGHT_RIGHT;
                    } else if (bearing < 100) {
                        nd.direction = NavigationDirection.RIGHT;
                    } else {
                        nd.direction = NavigationDirection.SHARP_RIGHT;
                    }
                } else if (bearing < 0) {
                    if (bearing > -30) {
                        nd.direction = NavigationDirection.SLIGHT_LEFT;
                    } else if (bearing > -100) {
                        nd.direction = NavigationDirection.LEFT;
                    } else {
                        nd.direction = NavigationDirection.SHARP_LEFT;
                    }
                }
            }
            for (;j < route.size() - 1 && g.edgeName(route.get(j), route.get(j + 1)).equals(nd.way) ; j++) {
                nd.distance += g.distance(route.get(j), route.get(j + 1));
            }
            next = route.get(j);
            nav.add(nd);
        }
        return nav; // FIXME
    }

    private static double angleConvert (double a) {
        if (a > 0) {
            a -= 360;
        }
        return a;
    }

    private static double directionConvert (double a) {
        if (a > 180) {
            a -= 360;
        } else if (a < - 180) {
            a += 360;
        }
        return a;
    }

    /**
     * Class to represent a navigation direction, which consists of 3 attributes:
     * a direction to go, a way, and the distance to travel for.
     */
    public static class NavigationDirection {

        /** Integer constants representing directions. */
        public static final int START = 0;
        public static final int STRAIGHT = 1;
        public static final int SLIGHT_LEFT = 2;
        public static final int SLIGHT_RIGHT = 3;
        public static final int RIGHT = 4;
        public static final int LEFT = 5;
        public static final int SHARP_LEFT = 6;
        public static final int SHARP_RIGHT = 7;

        /** Number of directions supported. */
        public static final int NUM_DIRECTIONS = 8;

        /** A mapping of integer values to directions.*/
        public static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];

        /** Default name for an unknown way. */
        public static final String UNKNOWN_ROAD = "unknown road";
        
        /** Static initializer. */
        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }

        /** The direction a given NavigationDirection represents.*/
        int direction;
        /** The name of the way I represent. */
        String way;
        /** The distance along this way I represent. */
        double distance;

        /**
         * Create a default, anonymous NavigationDirection.
         */
        public NavigationDirection() {
            this.direction = STRAIGHT;
            this.way = UNKNOWN_ROAD;
            this.distance = 0.0;
        }

        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }

        /**
         * Takes the string representation of a navigation direction and converts it into
         * a Navigation Direction object.
         * @param dirAsString The string representation of the NavigationDirection.
         * @return A NavigationDirection object representing the input string.
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
                // not a valid nd
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
