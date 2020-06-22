package gis.hmap;

/**
 * Created by Ryan on 2018/11/12.
 */

class BestLocation {
    private static BestLocation _instance = null;
    public long updateIndoorTime;
    public long updateGlobalTime;
    public double lat;
    public double lng;
    public String address;
    public String buildingId;
    public String floorId;

    private BestLocation() {
        updateIndoorTime = 0L;
        updateGlobalTime = 0L;
        lat = lng = 0.0;
        buildingId = floorId = "";
    }

    public static BestLocation getInstance() {
        if (_instance == null)
            _instance = new BestLocation();

        return _instance;
    }

    public static void updateIndoor(double lat, double lng, String buildingId, String floorId) {
        if (_instance == null)
            _instance = new BestLocation();

        _instance.lat = lat;
        _instance.lng = lng;
        _instance.buildingId = buildingId;
        _instance.floorId = floorId;
        _instance.updateIndoorTime = System.currentTimeMillis();
    }

    public static void updateGlobal(double lat, double lng, String addr) {
        if (_instance == null)
            _instance = new BestLocation();

        _instance.lat = lat;
        _instance.lng = lng;
        _instance.address = addr;
        _instance.updateGlobalTime = System.currentTimeMillis();
    }

    public static boolean isIndoorValidate() {
        boolean ret = false;

        if (_instance == null)
            _instance = new BestLocation();

        if (_instance.updateIndoorTime > 0) {

            long curr = System.currentTimeMillis();

            if (curr - _instance.updateIndoorTime < 60*1000)
                ret = true;
        }

        return ret;
    }

    public static boolean isGlobalValidate() {
        boolean ret = false;

        if (_instance == null)
            _instance = new BestLocation();

        if (_instance.updateGlobalTime > 0) {

            long curr = System.currentTimeMillis();

            if (curr - _instance.updateGlobalTime < 60*1000)
                ret = true;
        }

        return ret;
    }

    long getUpdateTime() {
        return Math.max(updateGlobalTime, updateIndoorTime);
    }
}
