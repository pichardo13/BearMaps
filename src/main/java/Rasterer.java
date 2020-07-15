import java.util.TreeMap;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    /** The max image depth level. */
    public static final int MAX_DEPTH = 7;
    public static final TreeMap<Double, Integer> DEPTHLONDPPS = new TreeMap<>();


    public double depthLonDPP(int depth) {
        double lonDelta = MapServer.ROOT_LON_DELTA / Math.pow(2, depth);
        return lonDelta / MapServer.TILE_SIZE;
    }

    public void updateTreeMap() {
        for (int depth = 0; depth <= MAX_DEPTH; depth++) {
            DEPTHLONDPPS.put(depthLonDPP(depth), depth);
        }
    }


    /**
     * Takes a user query and finds the grid of images that best matches the query. These images
     * will be combined into one big image (rastered) by the front end. The grid of images must obey
     * the following properties, where image in the grid is referred to as a "tile".
     * <ul>
     *     <li>The tiles collected must cover the most longitudinal distance per pixel (LonDPP)
     *     possible, while still covering less than or equal to the amount of longitudinal distance
     *     per pixel in the query box for the user viewport size.</li>
     *     <li>Contains all tiles that intersect the query bounding box that fulfill the above
     *     condition.</li>
     *     <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     * </ul>
     * @param params The RasterRequestParams containing coordinates of the query box and the browser
     *               viewport width and height.
     * @return A valid RasterResultParams containing the computed results.
     */
    public RasterResultParams getMapRaster(RasterRequestParams params) {
        updateTreeMap();
        double desiredLonDPP = lonDPP(params.lrlon, params.ullon, params.w);
        double desiredLeft = params.ullon;
        double desiredUp = params.ullat;
        double desiredRight = params.lrlon;
        double desiredDown = params.lrlat;
        double rootLeft = MapServer.ROOT_ULLON;
        double rootRight = MapServer.ROOT_LRLON;
        double rootUp = MapServer.ROOT_ULLAT;
        double rootDown = MapServer.ROOT_LRLAT;

        if (desiredLeft > rootRight || desiredRight < rootLeft
                || desiredDown > rootUp || desiredUp < rootDown) {
            return RasterResultParams.queryFailed();
        }

        if (desiredLeft > desiredRight || desiredRight < desiredLeft
                || desiredUp < desiredDown || desiredDown > desiredUp) {
            return RasterResultParams.queryFailed();
        }

        int resultDepth;

        if (DEPTHLONDPPS.floorEntry(desiredLonDPP) == null) {
            resultDepth = 7;
        } else {
            resultDepth = DEPTHLONDPPS.floorEntry(desiredLonDPP).getValue();
        }

        TreeMap<Double, Integer> imageLeft = new TreeMap<Double, Integer>();
        TreeMap<Double, Integer> imageRight = new TreeMap<Double, Integer>();
        TreeMap<Double, Integer> imageUp = new TreeMap<Double, Integer>();
        TreeMap<Double, Integer> imageDown = new TreeMap<Double, Integer>();
        double lonDelta = MapServer.ROOT_LON_DELTA / Math.pow(2, resultDepth);
        double latDelta = MapServer.ROOT_LAT_DELTA / Math.pow(2, resultDepth);
        double k = Math.pow(2, resultDepth) - 1;

        for (int x = 0; x <= k; x++) {
            imageLeft.put(MapServer.ROOT_ULLON + x * lonDelta, x);
        }
        for (int x = 0; x <= k; x++) {
            imageRight.put(MapServer.ROOT_LRLON - (k - x) * lonDelta, x);
        }
        for (int y = 0; y <= k; y++) {
            imageUp.put(MapServer.ROOT_ULLAT - (y * latDelta), y);
        }
        for (int y = 0; y <= k; y++) {
            imageDown.put(MapServer.ROOT_LRLAT + (k - y) * latDelta, y);
        }

        double resultUlLon = imageLeft.floorEntry(desiredLeft).getKey();
        double resultUlLat = imageUp.ceilingEntry(desiredUp).getKey();
        double resultLrLon = imageRight.ceilingEntry(desiredRight).getKey();
        double resultLrLat = imageDown.floorEntry(desiredDown).getKey();
        int resultLeft = imageLeft.floorEntry(desiredLeft).getValue();
        int resultRight = imageRight.ceilingEntry(desiredRight).getValue();
        int resultUp = imageUp.ceilingEntry(desiredUp).getValue();
        int resultDown = imageDown.floorEntry(desiredDown).getValue();
        String[][] resultGrid = new String[resultDown - resultUp + 1][resultRight - resultLeft + 1];

        int yIdx = 0;
        for (int y = resultUp; y <= resultDown; y++) {
            int xIdx = 0;
            for (int x = resultLeft; x <= resultRight; x++) {
                resultGrid[yIdx][xIdx] = "d" + resultDepth + "_x" + x + "_y" + y + ".png";
                xIdx += 1;
            }
            yIdx += 1;
        }

        RasterResultParams resultParams = new RasterResultParams.Builder()
                .setRenderGrid(resultGrid).setRasterUlLon(resultUlLon)
                .setRasterUlLat(resultUlLat).setRasterLrLon(resultLrLon)
                .setRasterLrLat(resultLrLat).setDepth(resultDepth)
                .setQuerySuccess(true).create();

        return resultParams;
    }

    /**
     * Calculates the lonDPP of an image or query box
     * @param lrlon Lower right longitudinal value of the image or query box
     * @param ullon Upper left longitudinal value of the image or query box
     * @param width Width of the query box or image
     * @return lonDPP
     */
    private double lonDPP(double lrlon, double ullon, double width) {
        return (lrlon - ullon) / width;
    }
}
