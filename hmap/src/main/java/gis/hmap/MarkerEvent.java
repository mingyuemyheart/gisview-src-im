package gis.hmap;

/**
 * Created by Ryan on 2018/9/22.
 */

public class MarkerEvent {
    public TargetEvent eventType;
    public double[] pos;
    public Marker marker;
    public String markerId;

    public MarkerEvent() {

    }

    public MarkerEvent(TargetEvent eventType, double[] pos, Marker marker, String markerId) {
        this.eventType = eventType;
        this.pos = pos;
        this.marker = marker;
        this.markerId = markerId;
    }
}
