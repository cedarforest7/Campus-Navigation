import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.*;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Jinchao Yin, Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    //private Set<Long> verticesID = new HashSet<>();
//    private Set<vertex> vertices = new HashSet<>();
//    private Set<edge> edges;
    private Map<Long, Set<edge>> adj = new HashMap<>();
    private Map<Long, double[]> vertexMap = new HashMap<>();
    private Map<Long, String> nodeName = new HashMap<>();  //names of location

    //static final String ur = "unknown road";
    static final String ur = "";        //to be modified

//    class vertex {
//        long id;
//        double lon;
//        double lat;
//        public vertex (long id, double lon, double lat) {
//            this.id = id;
//            this.lon = lon;
//            this.lat = lat;
//        }
//    }

    class edge {
        long v1, v2;
        //do not consider weight (road length)?
        String name = ur;
        public edge (long v1,  long v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
        public edge (long v1,  long v2, String name) {
            this.v1 = v1;
            this.v2 = v2;
            this.name = name;
        }
    }

    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        // TODO: Your code here.
        // Hint: look at the adjacency list
        //Set<Long> toRemove = new HashSet<>();
        for (long v : vertices()) {
            if (!adj.containsKey(v)) {
                removeNode(v);
            }
        }
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        //YOUR CODE HERE.
        Set<Long> verticesID = new HashSet<>();
        for (Long id : vertexMap.keySet()) {        //test
            verticesID.add(id);
        }
        return verticesID;
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        ArrayList<Long> adjacent = new ArrayList<>();
        for (edge e : adj.get(v)) {
            adjacent.add(e.v1 == v ? e.v2 : e.v1);
        }
        return adjacent;
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        double min = Double.POSITIVE_INFINITY;
        long closest = 0;
        for (long v : vertexMap.keySet()) {
            double dist = distance(lon, lat, lon(v), lat(v));
            if (dist < min) {
                min = dist;
                closest = v;
            }
        }
        return closest;
    }

    long closest (long v) {
        return closest(lon(v), lat(v));
    }


    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        return vertexMap.get(v)[0];
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        return vertexMap.get(v)[1];
    }

    //addNode, addEdge, reemoveNode, addWay(add many edges)  etc.
    void addNode (long v, double lon, double lat) {
        double[] coord = {lon, lat};
        vertexMap.put(v, coord);
    }
    //ways are all two-way
    void addEdge (edge e) {
        adj.putIfAbsent(e.v1, new HashSet<>());
        adj.putIfAbsent(e.v2, new HashSet<>());
        adj.get(e.v1).add(e);
        adj.get(e.v2).add(e);
    }

    void addEdge(long v1, long v2) {
        edge e = new edge(v1, v2);
        addEdge(e);
    }
    //remove a vertex but not yet all the edges that contains it
    long removeNode(long v) {
        vertexMap.remove(v);
        adj.remove(v);
        return v;
    }

    void addWay(Iterable<edge> way) {
        for (edge e : way) {
            addEdge(e);
        }
    }

    void addWay(Iterable<edge> way, String wayName) {
        for (edge e : way) {
            e.name = wayName;
            addEdge(e);
        }
    }

    void addWay (List<Long> way) {
        for (int i = 0; i < way.size() - 1; i++ ) {
            addEdge(way.get(i), way.get(i + 1));
        }
    }



    void addNodeName (long v, String name) {
        nodeName.put(v, name);
    }

    Iterable<edge> nodesToEdge (List<Long> way) {
        Set<edge> s = new HashSet<>();
        for (int i = 0; i < way.size() - 1; i++ ) {
            s.add(new edge(way.get(i), way.get(i + 1)));
        }
        return s;       //all edges do not have names at this point
    }

    long getNode (double[] coord) {
        for (long v : vertexMap.keySet()) {
            if (Arrays.equals(vertexMap.get(v), coord)) {
                return v;
            }
        }
        return 0;
    }

    Set<edge> getEdge (long v) {
        return adj.get(v);
    }

    edge edgeBetween (long va, long vb) {
        for (edge e : adj.get(va)) {
            long v = (e.v1 == va ? e.v2 : e.v1);
            if (v == vb) {
                return e;
            }
        }
        return null;
    }

    String edgeName (long va, long vb) {
        return edgeBetween(va, vb).name;
    }
}
