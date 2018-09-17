import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 * @Author Jinchao Yin
 */

public class Rasterer {
    private double mapWidth;
    private double mapHeight;
    public Rasterer() {
        // YOUR CODE HERE
        mapWidth = MapServer.ROOT_LRLON - MapServer.ROOT_ULLON;
        mapHeight = MapServer.ROOT_ULLAT - MapServer.ROOT_LRLAT;
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    private static final String[] MapRasterKey = {"render_grid", "raster_ul_lon", "raster_ul_lat",
            "raster_lr_lon", "raster_lr_lat" , "depth" , "query_success" };
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        //System.out.println(params);
        Map<String, Object> results = new HashMap<>();
        double lrlon = params.get("lrlon");
        double ullon = params.get("ullon");
        double lrlat = params.get("lrlat");
        double ullat = params.get("ullat");
        //System.out.println(lrlon + " " + ullon + " " + lrlat + " " + ullat);
        //System.out.println(MapServer.ROOT_LRLON + " " + MapServer.ROOT_ULLON + " " + MapServer.ROOT_LRLAT + " " + MapServer.ROOT_ULLAT);
        if (ullon < MapServer.ROOT_LRLON && ullat > MapServer.ROOT_LRLAT && lrlon > MapServer.ROOT_ULLON && lrlat < MapServer.ROOT_ULLAT  ) {
            results.put("query_success", true);
            //System.out.println("true");
        } else {
            results.put("query_success", false);
            //System.out.println("false");
            return results;
        }
        final double LonDPP = (lrlon - ullon) / (params.get("w"));
        //System.out.println("Since you haven't implemented getMapRaster, nothing is displayed in "
        //                  + "your browser.");
        int depth;
        for (depth = 0; imgLonDPP(depth) > LonDPP && depth < 7; depth++ );
        results.put("depth", depth);
        double tilex = ullon > MapServer.ROOT_ULLON ? findTile(ullon - MapServer.ROOT_ULLON, mapWidth, depth) : 0;
        double tiley = ullat <= MapServer.ROOT_ULLAT ? findTile(MapServer.ROOT_ULLAT - ullat, mapHeight, depth) : 0;
        double tilexEnd = lrlon < MapServer.ROOT_LRLON ? findTile(lrlon - MapServer.ROOT_ULLON, mapWidth, depth) : Math.pow(2, depth) - 1;
        double tileyEnd = lrlat >= MapServer.ROOT_LRLAT ? findTile(MapServer.ROOT_ULLAT - lrlat, mapHeight, depth) : Math.pow(2, depth) - 1;
        //System.out.println (tilex + " " + tiley);

        int xTileNum = (int)tilexEnd - (int)tilex + 1;
        int yTileNum = (int)tileyEnd - (int)tiley + 1;
        //System.out.println("depth:" + depth + " xTileNum:" + xTileNum + " yTileNum:" + yTileNum);
        String[][] grid = new String[yTileNum][xTileNum];
        String head =  "d" + String.valueOf(depth) + "_";
        for (int j = 0; j < yTileNum; j++) {
            String tail = "_" + "y" + String.valueOf((int)tiley + j) + ".png";
            for (int i = 0; i < xTileNum; i++) {
                grid[j][i] = head + "x" + String.valueOf((int)tilex + i) + tail;
                //System.out.println(grid[j][i]);
            }
        }
        results.put("render_grid", grid);
        double raster_ul_lon = MapServer.ROOT_ULLON + (mapWidth/Math.pow(2, depth))*(int)(tilex);
        double raster_ul_lat = MapServer.ROOT_ULLAT - (mapHeight/Math.pow(2, depth))*(int)(tiley);
        //double raster_lr_lon = Math.min(MapServer.ROOT_ULLON + (mapWidth/Math.pow(2, depth))*((int)(tilex) + xTileNum), MapServer.ROOT_LRLON);
        double raster_lr_lon = MapServer.ROOT_ULLON + (mapWidth/Math.pow(2, depth))*(int)(tilexEnd + 1);
        //double raster_lr_lat = Math.max(MapServer.ROOT_ULLAT - (mapHeight/Math.pow(2, depth))*((int)(tiley) + yTileNum), MapServer.ROOT_LRLAT);
        double raster_lr_lat = MapServer.ROOT_ULLAT - (mapHeight/Math.pow(2, depth))*(int)(tileyEnd + 1);
        results.put("raster_lr_lon", raster_lr_lon);
        results.put("raster_ul_lon", raster_ul_lon);
        results.put("raster_lr_lat", raster_lr_lat);
        results.put("raster_ul_lat", raster_ul_lat);
        //System.out.println(raster_ul_lon + " " + raster_ul_lat + " " + raster_lr_lon + " " + raster_lr_lat);
        return results;
    }

    private double imgLonDPP (int depth) {
        if (depth < 0 || depth > 7) {
            throw new IllegalArgumentException("depth not valid");
        }
        double lonDPP = mapWidth / MapServer.TILE_SIZE;
        lonDPP = lonDPP / Math.pow(2, depth);
        return lonDPP;
    }

    private double findTile (double coord, double limit, int depth) {
        return (coord/limit) * Math.pow(2, depth);
    }

}
