package gis.hmap;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Ryan on 2018/10/18.
 */

class Common {
    private static Common _instance = null;
    private String host = "http://47.97.169.150:8090";
    private String rtlsLicenseHost = "https://10.240.155.52:18889";
    private String rtlsMapDataHost = "http://10.240.155.35:8090";
    private String targetId = "yuanqu";

    private Common() { _instance = this; }

    public static void CreateInstance(String url) {
        if (_instance == null)
            _instance = new Common();

        _instance.host = url;
    }

    public static void initRtlsLicenseHost(String url) {
        if (_instance == null)
            _instance = new Common();

        _instance.rtlsLicenseHost = url;
    }

    public static void initRtlsMapdataHost(String ip, int port) {
        if (_instance == null)
            _instance = new Common();

        _instance.rtlsMapDataHost = String.format("http://%s:%d", ip, port);
    }

    public static void initCurrentTarget(String targetId) {
        if (_instance == null)
            _instance = new Common();

        _instance.targetId = targetId;
    }

    public static String getHost() {
        if (_instance == null)
            _instance = new Common();

        return _instance.host;
    }

    public static String getRtlsLicenseSrv() {
        if (_instance == null)
            _instance = new Common();

        return _instance.rtlsLicenseHost + LicenseServer;
    }

    // 基本数据源
    public static final String DATA_SOURCE = "/maps/";
    public static final String DATA_SOURCEINFO = "/maps/datasource";
    // SuperMap iServer提供的地图采用固定地址传递
    public static final String DARK_MAP = "/iserver/services/map-tt/rest/maps/yuanqu_black";
    public static final String DARK_MAP_ALIAS = "DARK_MAP";
    public static final String LIGHT_MAP = "/iserver/services/map-tt/rest/maps/yuanqu_white";
    public static final String LIGHT_MAP_ALIAS = "LIGHT_MAP";
    public static final String DARK_WORLDMAP = "/iserver/services/map-tt/rest/maps/WorldMap";
    public static final String DARK_WORLDMAP_ALIAS = "DARK_WORLDMAP";
    public static final String LIGHT_WORLDMAP = "/iserver/services/map-tt/rest/maps/WorldMap_white";
    public static final String LIGHT_WORLDMAP_ALIAS = "LIGHT_WORLDMAP";
    public static final String DATA = "/iserver/services/data-tt/rest/data";
    public static final String SERVICE = "/iserver/services/transportationAnalyst-tt/rest/networkanalyst/yuanqu_Network@yuanqu";
    public static final String GEO_CODE = "/iserver/services/addressmatch-tt/restjsr/v1/address/geocoding.json";
    public static final String GEO_DECODE = "/iserver/services/addressmatch-tt/restjsr/v1/address/geodecoding.json";
    //Rtls
    private static  String LicenseServer  = "/garden.guide/guide/requestguide";

    public static final int ADD_GENERAL_MARKER = 1;
    public static final int ADD_FLASH_MARKER = 2;
    public static final int QUERY_BUILDINGS = 10;
    public static final int QUERY_INDOOR_MAP = 11;
    public static final int QUERY_PERIMETER = 12;
    public static final int QUERY_MODEL = 13;
    public static final int ANALYST_ROUTE = 101;
    public static final int HEAT_MAP_CALC_END = 201;
    public static final int EVENT_MAP_TAP = 301;

    public static ExecutorService fixedThreadPool = Executors.newCachedThreadPool();
    public static ExecutorService downloadThreadTool = Executors.newFixedThreadPool(5);
}
