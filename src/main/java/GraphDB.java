import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Kevin Lowe, Antares Chen, Kevin Lin
 */
public class GraphDB {

    HashMap<Long, Vertex> vertexHashMap = new HashMap<>();
    HashMap<Long, LinkedList<Vertex>> wayHashMap = new HashMap<>();
    KDTree kd;

    /**
     * This constructor creates and starts an XML parser, cleans the nodes, and prepares the
     * data structures for processing. Modify this constructor to initialize your data structures.
     *
     * @param dbPath Path to the XML file to be parsed.
     */

    public GraphDB(String dbPath) {
        File inputFile = new File(dbPath);
        try (FileInputStream inputStream = new FileInputStream(inputFile)) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            saxParser.parse(inputStream, new GraphBuildingHandler(this));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
        kd = new KDTree(this);
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     *
     * @param s Input string.
     * @return Cleaned string.
     */
    private static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     * Remove nodes with no connections from the graph.
     * While this does not guarantee that any two nodes in the remaining graph are connected,
     * we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        ArrayList<Long> toRemove = new ArrayList<>();
        for (Long iD : vertexHashMap.keySet()) {
            if (vertexHashMap.get(iD).adjacents.isEmpty()) {
                toRemove.add(iD);
            }
        }
        vertexHashMap.keySet().removeAll(toRemove);
    }

    /**
     * Returns the longitude of vertex <code>v</code>.
     *
     * @param v The ID of a vertex in the graph.
     * @return The longitude of that vertex, or 0.0 if the vertex is not in the graph.
     */
    double lon(long v) {
        return vertexHashMap.get(v).lon;

    }

    /**
     * Returns the latitude of vertex <code>v</code>.
     *
     * @param v The ID of a vertex in the graph.
     * @return The latitude of that vertex, or 0.0 if the vertex is not in the graph.
     */
    double lat(long v) {
        return vertexHashMap.get(v).lat;
    }

    double x(long v) {
        return projectToX(lon(v), lat(v));
    }

    double y(long v) {
        return projectToY(lon(v), lat(v));
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     *
     * @return An iterable of all vertex IDs in the graph.
     */
    Iterable<Long> vertices() {
        return vertexHashMap.keySet();
    }

    /**
     * Returns an iterable over the IDs of all vertices adjacent to <code>v</code>.
     *
     * @param v The ID for any vertex in the graph.
     * @return An iterable over the IDs of all vertices adjacent to <code>v</code>, or an empty
     * iterable if the vertex is not in the graph.
     */
    Iterable<Long> adjacent(long v) {
        return vertexHashMap.get(v).adjacents;
    }

    /**
     * Returns the great-circle distance between two vertices, v and w, in miles.
     * Assumes the lon/lat methods are implemented properly.
     *
     * @param v The ID for the first vertex.
     * @param w The ID for the second vertex.
     * @return The great-circle distance between vertices and w.
     * @source https://www.movable-type.co.uk/scripts/latlong.html
     */
    public double distance(long v, long w) {
        double phi1 = Math.toRadians(lat(v));
        double phi2 = Math.toRadians(lat(w));
        double dphi = Math.toRadians(lat(w) - lat(v));
        double dlambda = Math.toRadians(lon(w) - lon(v));

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    static double euclidean(double x1, double x2, double y1, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    /**
     * Returns the ID of the vertex closest to the given longitude and latitude.
     *
     * @param lon The given longitude.
     * @param lat The given latitude.
     * @return The ID for the vertex closest to the <code>lon</code> and <code>lat</code>.
     */
    public long closest(double lon, double lat) {
        return kd.nearestNeighborSearch(kd.root, projectToX(lon, lat),
                projectToY(lon, lat));
    }

    /**
     * Return the Euclidean x-value for some point, p, in Berkeley. Found by computing the
     * Transverse Mercator projection centered at Berkeley.
     *
     * @param lon The longitude for p.
     * @param lat The latitude for p.
     * @return The flattened, Euclidean x-value for p.
     * @source https://en.wikipedia.org/wiki/Transverse_Mercator_projection
     */
    static double projectToX(double lon, double lat) {
        double dlon = Math.toRadians(lon - ROOT_LON);
        double phi = Math.toRadians(lat);
        double b = Math.sin(dlon) * Math.cos(phi);
        return (K0 / 2) * Math.log((1 + b) / (1 - b));
    }

    /**
     * Return the Euclidean y-value for some point, p, in Berkeley. Found by computing the
     * Transverse Mercator projection centered at Berkeley.
     *
     * @param lon The longitude for p.
     * @param lat The latitude for p.
     * @return The flattened, Euclidean y-value for p.
     * @source https://en.wikipedia.org/wiki/Transverse_Mercator_projection
     */
    static double projectToY(double lon, double lat) {
        double dlon = Math.toRadians(lon - ROOT_LON);
        double phi = Math.toRadians(lat);
        double con = Math.atan(Math.tan(phi) / Math.cos(dlon));
        return K0 * (con - Math.toRadians(ROOT_LAT));
    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     *
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public List<String> getLocationsByPrefix(String prefix) {
        return Collections.emptyList();
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     *
     * @param locationName A full name of a location searched for.
     * @return A <code>List</code> of <code>LocationParams</code> whose cleaned name matches the
     * cleaned <code>locationName</code>
     */
    public List<LocationParams> getLocations(String locationName) {
        return Collections.emptyList();
    }

    /**
     * Returns the initial bearing between vertices <code>v</code> and <code>w</code> in degrees.
     * The initial bearing is the angle that, if followed in a straight line along a great-circle
     * arc from the starting point, would take you to the end point.
     * Assumes the lon/lat methods are implemented properly.
     *
     * @param v The ID for the first vertex.
     * @param w The ID for the second vertex.
     * @return The bearing between <code>v</code> and <code>w</code> in degrees.
     * @source https://www.movable-type.co.uk/scripts/latlong.html
     */
    double bearing(long v, long w) {
        double phi1 = Math.toRadians(lat(v));
        double phi2 = Math.toRadians(lat(w));
        double lambda1 = Math.toRadians(lon(v));
        double lambda2 = Math.toRadians(lon(w));

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    public Vertex makeVertex(long id, String name, double lon, double lat) {
        return new Vertex(id, name, lon, lat);
    }


    /**
     * Radius of the Earth in miles.
     */
    private static final int R = 3963;
    /**
     * Latitude centered on Berkeley.
     */
    private static final double ROOT_LAT = (MapServer.ROOT_ULLAT + MapServer.ROOT_LRLAT) / 2;
    /**
     * Longitude centered on Berkeley.
     */
    private static final double ROOT_LON = (MapServer.ROOT_ULLON + MapServer.ROOT_LRLON) / 2;
    /**
     * Scale factor at the natural origin, Berkeley. Prefer to use 1 instead of 0.9996 as in UTM.
     *
     * @source https://gis.stackexchange.com/a/7298
     */
    private static final double K0 = 1.0;

    public class Vertex {
        long id;
        double lon;
        double lat;
        double x;
        double y;
        String name;
        List<Long> adjacents = new ArrayList<>();

        public Vertex(long id, String name, double lon, double lat) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
            this.x = projectToX(this.lon, this.lat);
            this.y = projectToY(this.lon, this.lat);
            this.name = name;

        }
    }

    public class KDTree {
        Node root;
        Long currentBest;
        Double currentBestDistance;
        ArrayList<Vertex> allX;
        //HashSet<Long> pruned = new HashSet<>();

        KDTree(GraphDB g) {
            allX = new ArrayList<>();
            allX.addAll(vertexHashMap.values());
            Collections.sort(allX, (v1, v2) -> Double.compare(v1.x, v2.x));

            root = buildTree(allX, true);
        }

        public Node buildTree(ArrayList<Vertex> lst, boolean xBool) {
            // ArrayList<Vertex> temp = new ArrayList<>();
            // temp.addAll(lst);

            int size = lst.size();
            if (lst.isEmpty()) {
                return null;
            }
            if (xBool) {
                Collections.sort(lst, (v1, v2) -> Double.compare(v1.x, v2.x));
                Vertex xMidVert = lst.get(size / 2);
                Integer xMidIdx = lst.indexOf(xMidVert);
                if (xMidIdx >= size) {
                    return new Node(xMidVert.id, null, null);
                } else {
                    return new Node(xMidVert.id,
                            buildTree(new ArrayList<>(lst.subList(0, xMidIdx)), !xBool),
                            buildTree(new ArrayList<>(lst.subList(xMidIdx + 1, size)), !xBool));
                }
            } else {
                Collections.sort(lst, (v1, v2) -> Double.compare(v1.y, v2.y));
                Vertex yMidVert = lst.get(size / 2);
                Integer yMidIdx = lst.indexOf(yMidVert);
                if (yMidIdx >= size) {
                    return new Node(yMidVert.id, null, null);
                } else {
                    return new Node(yMidVert.id,
                            buildTree(new ArrayList<>(lst.subList(0, yMidIdx)), !xBool),
                            buildTree(new ArrayList<>(lst.subList(yMidIdx + 1, size)), !xBool));
                }
            }
        }

        public class Node {

            Node left;
            Node right;
            Long iD;
            double x;
            double y;


            Node(Long iD, Node left, Node right) {
                this.left = left;
                this.right = right;
                this.iD = iD;
                this.x = x(iD);
                this.y = y(iD);
            }
        }

        Long nearestNeighborSearch(Node root2, double x, double y) {
            currentBest = null;
            currentBestDistance = Double.POSITIVE_INFINITY;
            nearestNeighborSearchHelper(root2, x, y, true);
            return currentBest;
        }

        void nearestNeighborSearchHelper(Node root3, double x, double y, boolean lonBoolean) {
            if (root3 != null) {

                double currentX = root3.x;
                double currentY = root3.y;
                double euclid = euclidean(x, currentX, y, currentY);

                if (euclid == 0) {
                    currentBestDistance = 0.0;
                    currentBest = root3.iD;
                    return;
                }

                if (euclid < currentBestDistance) {
                    currentBest = root3.iD;
                    currentBestDistance = euclid;
                }

                if (lonBoolean) {
                    double deltaX = Math.abs(currentX - x);
                    if (x < currentX) {
                        nearestNeighborSearchHelper(root3.left, x, y, !lonBoolean);

                        if (deltaX < currentBestDistance) {
                            nearestNeighborSearchHelper(root3.right, x, y, !lonBoolean);
                        }
                    } else {
                        nearestNeighborSearchHelper(root3.right, x, y, !lonBoolean);

                        if (deltaX < currentBestDistance) {
                            nearestNeighborSearchHelper(root3.left, x, y, !lonBoolean);
                        }
                    }
                } else {
                    double deltaY = Math.abs(currentY - y);
                    if (y < currentY) {
                        nearestNeighborSearchHelper(root3.left, x, y, !lonBoolean);

                        if (deltaY < currentBestDistance) {
                            nearestNeighborSearchHelper(root3.right, x, y, !lonBoolean);
                        }
                    } else {
                        nearestNeighborSearchHelper(root3.right, x, y, !lonBoolean);

                        if (deltaY < currentBestDistance) {
                            nearestNeighborSearchHelper(root3.left, x, y, !lonBoolean);
                        }
                    }
                }
            }
        }
    }
}
