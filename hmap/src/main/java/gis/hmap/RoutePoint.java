package gis.hmap;

/**
 * Created by Ryan on 2018/9/29.
 */

public class RoutePoint {
    public double[] coords;
    public int color;
    public int width;
    public int opacity;
    public String buildingId;
    public String floorid;

    public RoutePoint(double[] coords, int color, String buildingId,
                      String floorid, int width, int opacity) {
        this.coords = coords;
        this.color = color;
        this.buildingId = buildingId;
        this.floorid = floorid;
        this.width = width;
        this.opacity = opacity;
    }
}
