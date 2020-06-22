package gis.hmap;

import android.text.TextUtils;

import java.util.Map;

/**
 * Created by Ryan on 2018/11/12.
 */

public class LocationEvent {
    public boolean indoor;
    public String address;
    public double lng;
    public double lat;
    public String buildingId;
    public String floorId;
    public Map extend;

    public LocationEvent(String address, double lng, double lat, String buildingId, String floorId) {
        this.address = address;
        this.lng = lng;
        this.lat = lat;
        this.buildingId = buildingId;
        this.floorId = floorId;
        this.indoor = !TextUtils.isEmpty(buildingId);
    }
}
