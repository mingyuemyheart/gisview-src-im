package gis.hmap;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.supermap.data.CoordSysTransMethod;
import com.supermap.data.CoordSysTransParameter;
import com.supermap.data.CoordSysTranslator;
import com.supermap.data.CursorType;
import com.supermap.data.DatasetType;
import com.supermap.data.DatasetVector;
import com.supermap.data.DatasetVectorInfo;
import com.supermap.data.Datasource;
import com.supermap.data.DatasourceConnectionInfo;
import com.supermap.data.EncodeType;
import com.supermap.data.EngineType;
import com.supermap.data.Environment;
import com.supermap.data.FieldInfo;
import com.supermap.data.GeoLine;
import com.supermap.data.GeoLineM;
import com.supermap.data.GeoStyle;
import com.supermap.data.Point2D;
import com.supermap.data.Point2Ds;
import com.supermap.data.PrjCoordSys;
import com.supermap.data.PrjCoordSysType;
import com.supermap.data.QueryParameter;
import com.supermap.data.Recordset;
import com.supermap.data.Workspace;
import com.supermap.mapping.CallOut;
import com.supermap.mapping.CalloutAlignment;
import com.supermap.mapping.Layer;
import com.supermap.mapping.LayerSettingVector;
import com.supermap.mapping.MapControl;
import com.supermap.mapping.MapView;
import com.supermap.mapping.TrackingLayer;
import com.supermap.mapping.imChart.ChartPoint;
import com.supermap.mapping.imChart.HeatMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//import org.apache.http.HttpResponse;
//import org.apache.http.NameValuePair;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.util.EntityUtils;

/**
 * TODO: document your custom view class.
 */
public class GisView extends RelativeLayout implements AMapLocationListener {
    public static  String TAG = "GISView";

    private static final String calculatdRouteKey = "[calculatdRoute]";
    private static final String indoorKeyTemplate = "indoor[building:%s]";
    private static final String perimeterKey = "[perimeter]";
    private static final String modelsKey = "[models]";
    private static final String buildingTouchKey = "[buildingTouch]";
    private static final double[] scaleDenominators = {1.0/1250000, 1.0/625000, 1.0/312500, 1.0/150000, 1.0/75000, 1.0/38000, 1.0/19000, 1.0/9000, 1.0/4500, 1.0/2200, 1.0/1100, 1.0/500, 1.0/250, 1.0/100, 1.0/50, 1.0/25, 1.0/12, 1.0/6, 1.0/3, 1.0};

    protected MapView mapView;
    protected MapControl mapControl;
    protected Workspace workspace;
    protected Datasource dataDatasource;
    protected Datasource tempDatasource;
    protected NetWorkAnalystUtil netWorkAnalystUtil;
//    protected AbstractTileLayerView darkLayer;
//    protected AbstractTileLayerView lightLayer;
//    protected AbstractTileLayerView darkLayerWorld;
//    protected AbstractTileLayerView lightLayerWorld;
    protected Layer perimeterLayer;
    protected Layer perimeterAlarmLayer;
    protected Layer buildingsLayer;
    protected Layer[] floorLayer = new Layer[2];
    protected Layer highlightModelLayer;
    protected Layer pathLayer;
    protected TrackingLayer trackingLayer;
    protected String theme = "dark";
    protected List<ImageView> mMarkerList;
    protected List<String> mPopupList;
    protected DatasetVector mBuildings;
    protected List<MarkerListener> mMarkerListener;
    protected List<BuildingListener> mBuildingListener;
    protected List<ZoomListener> mZoomListener;
    protected List<MapListener> mMapListener;
    protected List<LocationListener> mPosListener;
    protected MarkerEventListener markerEventListener;
    protected int mHideLevel = 1;
    protected int mZoomLevel = 16;
    protected double[] mCenter;
    protected Handler handler;
    protected HeatMap mHeatMap;
    protected ArrayList<ChartPoint> mHotDatas;

    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;

    protected boolean isLocatorRunning = false;
    private double location_x_coordinate = 90006.388816089151;
    private double location_y_coordinate = 151146.58611877781;
    private double location_angle = 1.239410;
    //foe huawei  shenzhen coordinate eg2:62544.441693463683,157214.57982009905
    //foe huawei  shenzhen coordinate eg3:74122.787646144207,180396.54017803265

    public GisView(Context context) {
        super(context);
        init(context,null, 0,0);
    }

    public GisView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0,0);
    }

    public GisView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle,0);
    }

    public GisView(Context context, AttributeSet attrs, int defStyle, int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
        init(context, attrs, defStyle, defStyleRes);
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation.getErrorCode() == 0) {
            if (!BestLocation.isIndoorValidate()) {
                BestLocation.updateGlobal(
                        amapLocation.getLatitude(),
                        amapLocation.getLongitude(),
                        amapLocation.getAddress());
                GeoLocation p = getMyLocation();
                if (p != null && mPosListener.size() > 0 && isLocatorRunning) {
                    LocationEvent le = new LocationEvent(
                            BestLocation.getInstance().address,
                            BestLocation.getInstance().lat,
                            BestLocation.getInstance().lng,
                            "",
                            "");
                    for (LocationListener listener : mPosListener) {
                        try {
                            listener.onLocation(le);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }else {
            //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
            Log.e("AmapError","location Error, ErrCode:"
                    + amapLocation.getErrorCode() + ", errInfo:"
                    + amapLocation.getErrorInfo());
        }
    }

    /**
     * 采用最好的方式获取定位信息
     */
    public GeoLocation getMyLocation() {
        GeoLocation p = null;
        if (BestLocation.isIndoorValidate()) {
            p = new GeoLocation();
            p.address = "实时位置";
            p.lng = BestLocation.getInstance().lng;
            p.lat = BestLocation.getInstance().lat;
            p.buildingId = BestLocation.getInstance().buildingId;
            p.floorId = BestLocation.getInstance().floorId;
            p.time = new Date(BestLocation.getInstance().getUpdateTime());

            return p;
        } else if (BestLocation.isGlobalValidate()) {
            p = new GeoLocation();
            p.address = BestLocation.getInstance().address;
            p.lng = BestLocation.getInstance().lng;
            p.lat = BestLocation.getInstance().lat;
            p.buildingId = "";
            p.floorId = "";
            p.time = new Date(BestLocation.getInstance().getUpdateTime());

            return p;
        }

        Criteria c = new Criteria();//Criteria类是设置定位的标准信息（系统会根据你的要求，匹配最适合你的定位供应商），一个定位的辅助信息的类
        c.setPowerRequirement(Criteria.POWER_LOW);//设置低耗电
        c.setAltitudeRequired(true);//设置需要海拔
        c.setBearingAccuracy(Criteria.ACCURACY_COARSE);//设置COARSE精度标准
        c.setAccuracy(Criteria.ACCURACY_LOW);//设置低精度
        //... Criteria 还有其他属性，就不一一介绍了
        Location best = LocationUtils.getBestLocation(getContext(), c);
        if (best == null) {
            Location net = LocationUtils.getNetWorkLocation(getContext());
            if (net == null) {
                return null;
            } else {
                p = new GeoLocation();
                p.address = "实时位置";
                p.lng = net.getLongitude();
                p.lat = net.getLatitude();;
                p.time = new Date(net.getTime());

                return p;
            }
        } else {
            p = new GeoLocation();
            p.address = "实时位置";
            p.lng = best.getLongitude();
            p.lat = best.getLatitude();;
            p.time = new Date(best.getTime());

            return p;
        }
    }

    private void init(Context context, AttributeSet attrs, int defStyle, int defStyleRes) {
        mMarkerList = new ArrayList<>();
        mPopupList = new ArrayList<>();
        mMarkerListener = new ArrayList<>();
        mBuildingListener = new ArrayList<>();
        mZoomListener = new ArrayList<>();
        mMapListener = new ArrayList<>();
        mPosListener = new ArrayList<>();
        handler = new ExecuteFinished(this);
        mHotDatas = new ArrayList<>();

        markerEventListener = new MarkerEventListener();
    }

    public void setGisServer(String url) {
        Common.CreateInstance(url);
    }

    public void setRTLSServer(String url) {
        Common.initRtlsLicenseHost(url);
    }

    public void initRtlsMapdataSrv(String ip, int port) {
        Common.initRtlsMapdataHost(ip, port);
}

    public void initTarget(String targetId) {
        Common.initCurrentTarget(targetId);
    }

    public void initEngine(Context context){
        //初始化定位
        mLocationClient = new AMapLocationClient(context);
        //设置定位回调监听
        mLocationClient.setLocationListener(this);
        AMapLocationClientOption option = new AMapLocationClientOption();
        option.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.Transport);
        option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        option.setOnceLocation(false);
        option.setNeedAddress(true);
        option.setHttpTimeOut(10000);
        if(mLocationClient != null){
            mLocationClient.setLocationOption(option);
            //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
            mLocationClient.stopLocation();
            mLocationClient.startLocation();
        }

        isLocatorRunning = true;
    }

    public void deinitEngine(){
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
        }

    }

    public void getLocationOfAddress(final String address,final int count, final GeoServiceCallback callback){

        Common.fixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                String ret = "[]";
                try {
                    String enc_addr = URLEncoder.encode(address, "UTF-8");
                    ret =  GisView.getStringFromURL(Common.getHost() + Common.GEO_CODE + "?address="+enc_addr+"&fromIndex=0&toIndex=10&maxReturn=" + count);
                }
                catch (Exception ex){
                    Log.d(TAG, "getLocationOfAddress: "+ex.getMessage());
                }

                JSONArray arr = new JSONArray();
                try {
                    arr = new JSONArray(ret);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                GeoLocation [] res = new GeoLocation [arr.length()];


                for (int i = 0;i < arr.length(); i ++){
                    res[i] = new GeoLocation();
                    try {
                        JSONObject obj = arr.getJSONObject(i);
                        res[i].address = obj.getString("address");
                        res[i].lng = obj.getJSONObject("location").getDouble("x");
                        res[i].lat = obj.getJSONObject("location").getDouble("y") ;
                        res[i].score = obj.getDouble("score");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if(callback != null)
                    callback.onQueryAddressFinished(res);
            }
        });
    }

    public void getAddressOfLocation(final double lng, final double lat,final double radius, final int count, final GeoServiceCallback callback){

        Common.fixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                String ret = "[]";
                try {
                    String url = Common.getHost() + Common.GEO_DECODE + "?x=" + lng+ "&y=" + lat+ "&geoDecodingRadius="+radius+"&fromIndex=0&toIndex=10&maxReturn=" + count;
                    ret =  GisView.getStringFromURL(url);

                }
                catch (Exception ex){
                    Log.d(TAG, "getLocationOfAddress: "+ex.getMessage());
                }

                JSONArray arr = new JSONArray();
                try {
                    arr = new JSONArray(ret);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                GeoLocation [] res = new GeoLocation [arr.length()];


                for (int i = 0;i < arr.length(); i ++){
                    res[i] = new GeoLocation();
                    try {
                        JSONObject obj = arr.getJSONObject(i);
                        res[i].address = obj.getString("address");
                        res[i].lng = obj.getJSONObject("location").getDouble("x");
                        res[i].lat = obj.getJSONObject("location").getDouble("y") ;
                        res[i].score = obj.getDouble("score");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if(callback != null)
                    callback.onQueryAddressFinished(res);
            }
        });
    }

    public static String getStringFromURL(String urlString) {
        HttpURLConnection conn = null;
        String server_response = "[]";
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");// 设置URL请求方法
            //可设置请求头
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Charset", "UTF-8");
            InputStream is = conn.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int v;
            while ((v = is.read()) != -1) {
                baos.write(v);
            }
            server_response = baos.toString();
            baos.close();
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null)
                conn.disconnect();
        }

        if(!TextUtils.isEmpty(server_response)){
            Log.i("Server response", server_response );
            return server_response;
        } else {
            Log.i("Server response", "Failed to get server response" );
            return "[]";
        }
    }

    public void queryObject(final String parkId, final String address, final QueryCallback callback) {
        Common.fixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                String addr = null;
                double lng = 0, lat = 0;
                try {
                    String ret = "[]";
                    try {
                        String enc_addr = URLEncoder.encode(address, "UTF-8");
                        ret =  GisView.getStringFromURL(Common.getHost() + Common.GEO_CODE + "?address="+enc_addr+"&fromIndex=0&toIndex=10&maxReturn=1");
                    }
                    catch (Exception ex){
                        Log.d(TAG, "getLocationOfAddress: "+ex.getMessage());
                    }

                    JSONArray arr = new JSONArray();
                    try {
                        arr = new JSONArray(ret);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (arr.length() > 0){
                        try {
                            JSONObject obj = arr.getJSONObject(0);
                            addr = obj.getString("address");
                            lng = obj.getJSONObject("location").getDouble("x");
                            lat = obj.getJSONObject("location").getDouble("y") ;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!TextUtils.isEmpty(addr) && mBuildings != null) {
                    Point2D pt = new Point2D(lng, lat);
                    Recordset recordset = mBuildings.getRecordset(false, CursorType.STATIC);
                    do {
                        if (recordset.isEOF())
                            break;
                        if (recordset.getGeometry().hitTest(pt, 0)) {
                            FieldInfo[] fi = recordset.getFieldInfos().toArray();
                            String[] fields = new String[fi.length];
                            String[] values = new String[fi.length];
                            for (int i = 0; i < fi.length; i++) {
                                fields[i] = fi[i].getName();
                                values[i] = recordset.getFieldValue(fields[i]).toString();
                            }

                            ObjectInfo info = new ObjectInfo(fields, values);
                            info.address = addr;
                            info.lng = lng;
                            info.lat = lat;
                            if (callback != null)
                                callback.onQueryFinished(info);
                            break;
                        }
                    } while (recordset.moveNext());
                }
            }
        });
    }

    public void getBuldingInfo(final String parkId, final String buildingId, final QueryCallback callback) {
        Common.fixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                if (mBuildings != null) {
                    Recordset recordset = mBuildings.getRecordset(false, CursorType.STATIC);
                    QueryParameter queryParameter = new QueryParameter();
                    queryParameter.setAttributeFilter(String.format("SMID=%s AND PARKID=%s", buildingId, parkId));
                    queryParameter.setHasGeometry(true);
                    queryParameter.setCursorType(CursorType.STATIC);
                    Recordset queryRecordset = mBuildings.query(queryParameter);

                    if (queryRecordset != null) {
                        FieldInfo[] fi = recordset.getFieldInfos().toArray();
                        String[] fields = new String[fi.length];
                        String[] values = new String[fi.length];
                        for (int i = 0; i < fi.length; i++) {
                            fields[i] = fi[i].getName();
                            values[i] = recordset.getFieldValue(fields[i]).toString();
                        }
                        ObjectInfo info = new ObjectInfo(fields, values);
                        try {
                            String ret = "[]";
                            try {
                                String enc_addr = URLEncoder.encode(info.getStrParam("NAME"), "UTF-8");
                                ret =  GisView.getStringFromURL(Common.getHost() + Common.GEO_CODE + "?address="+enc_addr+"&fromIndex=0&toIndex=10&maxReturn=1");
                            }
                            catch (Exception ex){
                                Log.d(TAG, "getLocationOfAddress: "+ex.getMessage());
                            }

                            JSONArray arr = new JSONArray();
                            try {
                                arr = new JSONArray(ret);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if (arr.length() > 0){
                                try {
                                    JSONObject obj = arr.getJSONObject(0);
                                    info.address = obj.getString("address");
                                    info.lng = obj.getJSONObject("location").getDouble("x");
                                    info.lat = obj.getJSONObject("location").getDouble("y") ;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (callback != null)
                            callback.onQueryFinished(info);
                    }
                }
            }
        });
    }


    private class MarkerEventListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            ImageView iv = (ImageView) v;
            if (iv != null) {
                for (ImageView imageView : mMarkerList) {
                    if (iv.equals(imageView)) {
                        Marker mk = (Marker) iv.getTag();
                        if (mMarkerListener.size() > 0) {
                            MarkerEvent me = new MarkerEvent(TargetEvent.Press,
                                    mk.position, mk, mk.markerId);
                            for (MarkerListener listener : mMarkerListener)
                                listener.markerEvent(me);
                        }
                        break;
                    }
                }
            }
        }
    }

    private GestureDetector.SimpleOnGestureListener mGestrueListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            final int x = (int) e.getX();
            final int y = (int) e.getY();
            Point2D pt = mapControl.getMap().pixelToMap(new com.supermap.data.Point(x, y));
            //当投影不是经纬坐标系时，则对起始点进行投影转换
            if(mapControl.getMap().getPrjCoordSys().getType() != PrjCoordSysType.PCS_EARTH_LONGITUDE_LATITUDE){
                Point2Ds points = new Point2Ds();
                points.add(pt);
                PrjCoordSys desPrjCoorSys = new PrjCoordSys();
                desPrjCoorSys.setType(PrjCoordSysType.PCS_EARTH_LONGITUDE_LATITUDE);
                CoordSysTranslator.convert(points, mapControl.getMap().getPrjCoordSys(), desPrjCoorSys, new CoordSysTransParameter(), CoordSysTransMethod.MTH_GEOCENTRIC_TRANSLATION);
                pt = points.getItem(0);
            }
            final Point2D point = pt;
            if (mBuildings != null) {
                Recordset recordset = mBuildings.getRecordset(false, CursorType.STATIC);
                do {
                    if (recordset.isEOF())
                        break;
                    if (recordset.getGeometry().hitTest(pt, 0)) {
                        if (mBuildingListener.size() > 0) {
                            double[] pos = new double[4];
                            FieldInfo[] fi = recordset.getFieldInfos().toArray();
                            String[] fields = new String[fi.length];
                            String[] values = new String[fi.length];
                            for (int i = 0; i < fi.length; i++) {
                                fields[i] = fi[i].getName();
                                values[i] = recordset.getFieldValue(fields[i]).toString();
                                if (fields[i].equalsIgnoreCase("SMSDRIW"))
                                    pos[0] = Double.parseDouble(values[i]);
                                else if (fields[i].equalsIgnoreCase("SMSDRIN"))
                                    pos[1] = Double.parseDouble(values[i]);
                                else if (fields[i].equalsIgnoreCase("SMSDRIE"))
                                    pos[2] = Double.parseDouble(values[i]);
                                else if (fields[i].equalsIgnoreCase("SMSDRIS"))
                                    pos[3] = Double.parseDouble(values[i]);
                            }
                            BuildingEvent be = new BuildingEvent(TargetEvent.Press, pos, fields, values);
                            for (BuildingListener listener : mBuildingListener)
                                listener.buildingEvent(be);
                        }
                        break;
                    }
                } while (recordset.moveNext());
            }
            if (mMapListener.size() > 0) {
                // 获取地理信息
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String ret = "[]";
                        try {
                            String url = Common.getHost() + Common.GEO_DECODE + "?x=" + point.getX() + "&y=" + point.getY() + "&geoDecodingRadius=0.0005&fromIndex=0&toIndex=10&maxReturn=5";
                            ret = GisView.getStringFromURL(url);
                        } catch (Exception ex) {
                            Log.d(TAG, "getLocationOfAddress: " + ex.getMessage());
                        }

                        JSONArray arr = new JSONArray();
                        try {
                            arr = new JSONArray(ret);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        String[] addrs = new String[arr.length()];

                        for (int i = 0; i < arr.length(); i++) {
                            try {
                                JSONObject obj = arr.getJSONObject(i);
                                addrs[i] = obj.getString("address");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        // 引发事件
                        MapEvent me = new MapEvent(TargetEvent.Press,
                                new int[] { x, y },
                                new double[] { point.getX(), point.getY() },
                                addrs);
                        Message msg = new Message();
                        msg.obj = me;
                        msg.what = Common.EVENT_MAP_TAP;
                        handler.sendMessage(msg);
                    }
                }).start();
            }
            return super.onSingleTapConfirmed(e);
        }
    };

//    @Override
//    public void zoomStart(MapView mapView) {
//        fitHeatmapToView(true);
//        ZoomEvent ze = new ZoomEvent(Zoom.ZoomStart, mapView.getZoomLevel());
//        for (ZoomListener listener : mZoomListener) {
//            listener.zoomEvent(ze);
//        }
//    }

//    @Override
//    public void zoomEnd(MapView mapView) {
//        fitHeatmapToView(true);
//        switchMarkerHide();
//        ZoomEvent ze = new ZoomEvent(Zoom.ZoomEnd, mapView.getZoomLevel());
//        for (ZoomListener listener : mZoomListener) {
//            listener.zoomEvent(ze);
//        }
//    }

    public void loadMap(int zoom, double[] center) {
        String rootPath = getContext().getExternalFilesDir("supermap").getAbsolutePath();
        File dir = new File(rootPath + "/license/SuperMap iMobile Trial.slm");
        if (!dir.exists())
            return;

        Environment.setLicensePath(rootPath + "/license/");
        Environment.setTemporaryPath(rootPath + "/temp/");
        Environment.setWebCacheDirectory(rootPath + "/WebCatch");
        Environment.initialization(getContext().getApplicationContext());
        View inflate = inflate(getContext(), R.layout.gisview, this);
        mapView = inflate.findViewById(R.id.mapview);
        mapControl = mapView.getMapControl();
        workspace = new Workspace();

        mapControl.setGestureDetector(new GestureDetector(getContext(), mGestrueListener));
        LoadWorkSpace loader = new LoadWorkSpace(null);
        loader.startLoad();
        final com.supermap.mapping.Map map = mapControl.getMap();
        map.setWorkspace(workspace);
//        map.setMapLoadedListener(this);

        DatasourceConnectionInfo datasourceconnection = new DatasourceConnectionInfo();
        datasourceconnection.setEngineType(EngineType.Rest);
        if (theme.equalsIgnoreCase("dark")) {
            datasourceconnection.setServer(Common.getHost() + Common.DARK_WORLDMAP);
            datasourceconnection.setAlias(Common.DARK_WORLDMAP_ALIAS);
        } else {
            datasourceconnection.setServer(Common.getHost() + Common.LIGHT_WORLDMAP);
            datasourceconnection.setAlias(Common.LIGHT_WORLDMAP_ALIAS);
        }
        Datasource datasource = workspace.getDatasources().open(datasourceconnection);
        if (datasource != null) {
            map.getLayers().add(datasource.getDatasets().get(0), false);
//            map.setViewBounds(datasource.getDatasets().get(0).getBounds());
//            map.setLockedViewBounds(datasource.getDatasets().get(0).getBounds());
        }
        datasourceconnection = new DatasourceConnectionInfo();
        datasourceconnection.setEngineType(EngineType.Rest);
        if (theme.equalsIgnoreCase("dark")) {
            datasourceconnection.setServer(Common.getHost() + Common.DARK_MAP);
            datasourceconnection.setAlias(Common.DARK_MAP_ALIAS);
        } else {
            datasourceconnection.setServer(Common.getHost() + Common.LIGHT_MAP);
            datasourceconnection.setAlias(Common.LIGHT_MAP_ALIAS);
        }
        datasource = workspace.getDatasources().open(datasourceconnection);
        if (datasource != null) {
            map.getLayers().add(datasource.getDatasets().get(0), true);
//            map.setViewBounds(datasource.getDatasets().get(0).getBounds());
//            map.setLockedViewBounds(datasource.getDatasets().get(0).getBounds());
        }

        map.setAntialias(true);
        mZoomLevel = zoom;
        mCenter = center;
//        mapControl.getMap().setVisibleScalesEnabled(false);
        map.setVisibleScales(scaleDenominators);
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) { }
                map.setScale(scaleDenominators[mZoomLevel]);
                map.setCenter(new Point2D(mCenter[1], mCenter[0]));
                map.refresh();
            }
        });

        datasourceconnection = new DatasourceConnectionInfo();
        datasourceconnection.setEngineType(EngineType.UDB);
        datasourceconnection.setServer(rootPath + "/oem/workspace/yuanqu.udb");
        datasourceconnection.setAlias("yuanqu");
        dataDatasource = workspace.getDatasources().open(datasourceconnection);
        datasourceconnection = new DatasourceConnectionInfo();
        datasourceconnection.setEngineType(EngineType.UDB);
        datasourceconnection.setAlias("temp");
        tempDatasource = workspace.getDatasources().create(datasourceconnection);
        QueryUtils.queryModel(null, null, tempDatasource, dataDatasource, handler);
        QueryUtils.queryAllBuildings(null, tempDatasource, dataDatasource, handler);
    }

    public void setTheme(String mapStyle) {
//        if (!theme.equalsIgnoreCase(mapStyle)) {
//            theme = mapStyle;
//            if (theme.equalsIgnoreCase("light")) {
//                mapView.removeLayer(darkLayer);
//                mapView.addLayer(lightLayer);
//            } else {
//                mapView.removeLayer(lightLayer);
//                mapView.addLayer(darkLayer);
//            }
//        }
    }

    public void setCenter(double lat, double lng) {
        mapControl.getMap().setCenter(new Point2D(lng, lat));
        mapControl.getMap().refresh();
    }

    public void setZoom(double[] center, int zoom) {
        mZoomLevel = zoom;
        if (zoom >= 0 && zoom < scaleDenominators.length)
            mapControl.getMap().setScale(scaleDenominators[mZoomLevel]);
        mapControl.getMap().setCenter(new Point2D(center[1], center[0]));
        mapControl.getMap().refresh();
    }

    public void zoomInMap() {
        mZoomLevel++;
        if (mZoomLevel >= scaleDenominators.length)
            mZoomLevel = scaleDenominators.length - 1;
        mapControl.getMap().setScale(scaleDenominators[mZoomLevel]);
        mapControl.getMap().refresh();
    }

    public void zoomOutMap() {
        mZoomLevel--;
        if (mZoomLevel < 0)
            mZoomLevel = 0;
        mapControl.getMap().setScale(scaleDenominators[mZoomLevel]);
        mapControl.getMap().refresh();
    }

    public int getZoom() {
        return mZoomLevel;
    }

    public void destroyMap() {
        mapControl.getMap().getLayers().clear();
        workspace.getDatasources().closeAll();
//        workspace.getMaps().clear();
    }

    public void changeMarkerPosition(String markerId, double lat, double lng) {
//        List<Overlay> ovls = mapView.getOverlays();
//        for (Overlay ov : ovls) {
//            if (ov instanceof DefaultItemizedOverlay) {
//                DefaultItemizedOverlay overlay = (DefaultItemizedOverlay) ov;
//                for (int i = 0; i < overlay.size(); i++) {
//                    OverlayItemEx item = (OverlayItemEx) overlay.getItem(i);
//                    if (item.getTitle().equalsIgnoreCase(markerId)) {
//                        item.setPoint(new Point2D(lng, lat));
//                        mapView.invalidate();
//                        return;
//                    }
//                }
//            }
//        }
    }

    public void addMarker(String layerId, int layerIndex, GeneralMarker[] markers) {
        List<GeneralMarker> drawableMarkers = new ArrayList<>();
        List<GeneralMarker> urlMarkers = new ArrayList<>();
        for (GeneralMarker marker : markers) {
            if (marker.image == null) {
                if (!TextUtils.isEmpty(marker.imagePath))
                    urlMarkers.add(marker);
            } else
                drawableMarkers.add(marker);
        }

        if (drawableMarkers.size() > 0) {
            for (GeneralMarker marker : drawableMarkers) {
                MarkerDrawable drawable = new MarkerDrawable(marker.image, marker.width, marker.height);
                ImageView image = new ImageView(getContext());
                image.setOnClickListener(markerEventListener);
                image.setImageDrawable(drawable);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setTag(marker);
                CallOut callout = new CallOut(getContext());
                callout.setStyle(CalloutAlignment.BOTTOM);
                callout.setCustomize(true);
                callout.setLocation(marker.position[1], marker.position[0]);
                callout.setContentView(image);
                mapView.addCallout(callout, marker.markerId);
                mMarkerList.add(image);
            }
            mapView.invalidate();
        }
        if (urlMarkers.size() > 0) {
            UrlMarkerMaker.makeUrlMarker(
                    getContext(),
                    layerId,
                    layerIndex,
                    urlMarkers.toArray(new GeneralMarker[urlMarkers.size()]),
                    handler);
        }
    }

    public void addMarker(String layerId, int layerIndex, FlashMarker[] markers) {
        List<FlashMarker> drawableMarkers = new ArrayList<>();
        List<FlashMarker> urlMarkers = new ArrayList<>();
        for (FlashMarker marker : markers) {
            if (marker.images == null) {
                if (marker.imagesPath != null)
                    urlMarkers.add(marker);
            } else
                drawableMarkers.add(marker);
        }

        if (drawableMarkers.size() > 0) {
            final List<AnimationDrawable> anilst = new ArrayList<>();
            for (FlashMarker marker : markers) {
                AnimationDrawable animation = new AnimationDrawable();
                for (Drawable d : marker.images) {
                    MarkerDrawable md = new MarkerDrawable(d, marker.width, marker.height);
                    animation.addFrame(md, marker.duration);
                }
                animation.setOneShot(false);
                ImageView image = new ImageView(getContext());
                image.setOnClickListener(markerEventListener);
                image.setImageDrawable(animation);
                image.setTag(marker);
                CallOut callout = new CallOut(getContext());
                callout.setStyle(CalloutAlignment.CENTER);
                callout.setCustomize(true);
                callout.setLocation(marker.position[1], marker.position[0]);
                callout.setContentView(image);
                mapView.addCallout(callout, marker.markerId);
                anilst.add(animation);
                mMarkerList.add(image);
            }
            mapView.invalidate();
            mapView.post(new Runnable() {
                @Override
                public void run() {
                    for (AnimationDrawable ani : anilst) {
                        ani.start();
                    }
                }
            });
        }
        if (urlMarkers.size() > 0) {
            UrlMarkerMaker.makeUrlMarker(
                    layerId,
                    layerIndex,
                    urlMarkers.toArray(new FlashMarker[urlMarkers.size()]),
                    handler);
        }
    }

    public void addFlashMarker(String layerId, int layerIndex, FlashMarker[] markers) {
        addMarker(layerId, layerIndex, markers);
    }

    public void deleteMarker(String markerId) {
        mapView.removeCallOut(markerId);
        mapView.invalidate();
    }

    public void deleteLayer(String layerId) {
        mapView.removeAllCallOut();
        mapView.invalidate();
    }

    public void addMarkerListener(MarkerListener listener) {
        if (!mMarkerListener.contains(listener))
            mMarkerListener.add(listener);
    }

    public void removeMarkerListener(MarkerListener listener) {
        mMarkerListener.remove(listener);
    }

    public void addPopup(double[] position, String content, int width, int height, Object tag) {
        DefaultPopupView popupView = new DefaultPopupView(getContext());
        popupView.setText(content);
        popupView.setTag(tag);
        popupView.setSize(width, height);
        CallOut callout = new CallOut(getContext());
        callout.setStyle(CalloutAlignment.BOTTOM);
        callout.setCustomize(true);
        callout.setLocation(position[1], position[0]);
        callout.setContentView(popupView);
        String popup = String.format("popup_%d", mPopupList.size());
        mPopupList.add(popup);
        mapView.addCallout(callout, popup);
        mapView.invalidate();
    }

    public void addPopup(double[] position, View popupView, Object tag) {
        popupView.setTag(tag);
        CallOut callout = new CallOut(getContext());
        callout.setStyle(CalloutAlignment.BOTTOM);
        callout.setCustomize(true);
        callout.setLocation(position[1], position[0]);
        callout.setContentView(popupView);
        String popup = String.format("popup_%d", mPopupList.size());
        mPopupList.add(popup);
        mapView.addCallout(callout, popup);
        mapView.invalidate();
    }

    public void closePopup() {
        for (String popup : mPopupList)
            mapView.removeCallOut(popup);
        mPopupList.clear();
        mapView.invalidate();
    }

    public void showIndoorMap(String buildingId, String floorid) {
        if (TextUtils.isEmpty(floorid)) {
            for (int i = 0; i < floorLayer.length; i++) {
                if (floorLayer[i] != null) {
                    floorLayer[i].getDataset().close();
                    mapControl.getMap().getLayers().remove(floorLayer[i]);
                    floorLayer[i] = null;
                }
            }
            clearPath();
            if (netWorkAnalystUtil != null)
                netWorkAnalystUtil.excutePathService("", "");
            mapControl.getMap().refresh();
        } else
            QueryUtils.queryIndoorMap(buildingId, floorid, tempDatasource, dataDatasource, handler);
    }

    public void switchOutdoor() {
        showIndoorMap(null, null);
    }

    public void drawCustomPath(RoutePoint[] points) {
        if (points == null || points.length == 0)
            return;

        Point2Ds point2Ds = new Point2Ds();
        for (RoutePoint p : points) {
            point2Ds.add(new Point2D(p.coords[1], p.coords[0]));
        }
        GeoLine line = new GeoLine(point2Ds);
        DatasetVectorInfo dvi = new DatasetVectorInfo();
        dvi.setType(DatasetType.LINE);
        dvi.setEncodeType(EncodeType.NONE);
        dvi.setName("customPath");
        tempDatasource.getDatasets().delete("customPath");
        DatasetVector customPath = tempDatasource.getDatasets().create(dvi);
        Recordset recordset = customPath.getRecordset(true, CursorType.DYNAMIC);
        recordset.addNew(line);
        customPath.append(recordset);
        pathLayer = mapControl.getMap().getLayers().add(customPath , true);
        pathLayer.setSelectable(false);
        LayerSettingVector lineSetting = (LayerSettingVector)pathLayer.getAdditionalSetting();
        GeoStyle lineStyle = new GeoStyle();
        lineStyle.setLineColor(new com.supermap.data.Color(points[0].color));
        lineStyle.setLineWidth(points[0].width);
        lineSetting.setStyle(lineStyle);
        mapControl.getMap().refresh();
    }

    public void calcRoutePath(RoutePoint start, RoutePoint end, RoutePoint[] way) {
        if (netWorkAnalystUtil == null)
            netWorkAnalystUtil = new NetWorkAnalystUtil(start, end, way, tempDatasource, dataDatasource, handler);
        netWorkAnalystUtil.excutePathService("", "");
    }

    public void clearPath() {
        clearAllRoute();
        if (netWorkAnalystUtil != null) {
            netWorkAnalystUtil.close();
            netWorkAnalystUtil = null;
        }
    }

    public void showHeatMap(HeatPoint[] points, int radius, double opacity) {
        for (HeatPoint hp : points) {
            mHotDatas.add(new ChartPoint(new Point2D(hp.lng, hp.lat), hp.value));
        }
        mHeatMap = new HeatMap(getContext(), mapView);
        mHeatMap.setRadious(radius);
        mHeatMap.setSmoothTransColor(true);
        mHeatMap.addChartDatas(mHotDatas);
        mHeatMap.setTitle("人员热力图");
        mapView.invalidate();
    }

    public void clearHeatMap() {
        if (mHeatMap != null) {
            mHeatMap.removeAllData();
            mHeatMap.dispose();
            mapView.invalidate();
        }
    }

    public void displayPerimeter(String parkId,
                                 String normalColor, int normalWidth, int normalOpacity,
                                 String alarmColor, int alarmWidth, int alarmOpacity,
                                 int[] alarmList) {
        PerimeterStyle alarm = new PerimeterStyle(Color.parseColor(alarmColor), alarmWidth, alarmOpacity);
        PerimeterStyle normal = new PerimeterStyle(Color.parseColor(normalColor), normalWidth, normalOpacity);
        displayPerimeter(parkId, alarm, normal, alarmList);
    }

    public void displayPerimeter(String parkId, PerimeterStyle alarm, PerimeterStyle normal, int[] alarmList) {

        removePerimeter();
        QueryUtils.queryPerimeter(parkId, alarm, normal, alarmList, tempDatasource, dataDatasource, handler);
    }

    public void removePerimeter() {
        if (perimeterLayer != null) {
            perimeterLayer.getDataset().close();
            mapControl.getMap().getLayers().remove(perimeterLayer);
            perimeterLayer = null;
        }
        if (perimeterAlarmLayer != null) {
            perimeterAlarmLayer.getDataset().close();
            mapControl.getMap().getLayers().remove(perimeterAlarmLayer);
            perimeterAlarmLayer = null;
        }
    }

    public void showModelHighlight(String parkId, int[] modId) {
        QueryUtils.queryModel(parkId, modId, tempDatasource, dataDatasource, handler);
    }

    public void removeModelhighlighting() {
        if (highlightModelLayer != null) {
            highlightModelLayer.getDataset().close();
            mapControl.getMap().getLayers().remove(highlightModelLayer);
            highlightModelLayer = null;
        }
    }

    public void addBuildingListener(BuildingListener listener) {
        if (!mBuildingListener.contains(listener))
            mBuildingListener.add(listener);
    }

    public void removeBuildingListener(BuildingListener listener) {
//        mBuildingListener.remove(listener);
//        List<Overlay> overlays = mapView.getOverlays();
//        for (Overlay ov : overlays) {
//            String key = ov.getKey();
//            if (!TextUtils.isEmpty(key) && key.startsWith("building:")) {
//                PolygonOverlay unselect = (PolygonOverlay) ov;
//                if (unselect != null)
//                    unselect.setLinePaint(getBuildingSelectPaint(false));
//            }
//        }
//        mapView.invalidate();
    }

    public void addZoomListener(ZoomListener listener) {
        if (!mZoomListener.contains(listener))
            mZoomListener.add(listener);
    }

    public void removeZoomListener(ZoomListener listener) {
        mZoomListener.remove(listener);
    }

    public void addMapListener(MapListener listener) {
        if (!mMapListener.contains(listener))
            mMapListener.add(listener);
    }

    public void removeMapListener(MapListener listener) {
        mMapListener.remove(listener);
    }

    public void setHideLevel(int level) {
        mHideLevel = level;
        switchMarkerHide();
    }

    private void switchMarkerHide() {
//        boolean show;
//        if (mapView.getZoomLevel() > mHideLevel)
//            show = true;
//        else
//            show = false;
//        List<Overlay> overlays = mapView.getOverlays();
//        for (Overlay ov : overlays) {
//            if (ov instanceof DefaultItemizedOverlay) {
//                DefaultItemizedOverlay overlay = (DefaultItemizedOverlay) ov;
//                for (int i = 0, cnt = overlay.size(); i < cnt; i++) {
//                    Drawable d = overlay.getItem(i).getMarker(0);
//                    d.setVisible(show, true);
//                    d.invalidateSelf();
//                }
//            }
//        }
//        mapView.invalidate();
    }

    public void addLocateListener(LocationListener listener) {
        if (!mPosListener.contains(listener))
            mPosListener.add(listener);
    }

    public void removeLocateListener(LocationListener listener) {
        mPosListener.remove(listener);
    }

    public void startLocate() {
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.startLocation();
        }
        isLocatorRunning = true;
    }

    public void stopLocate() {
        isLocatorRunning = false;
        if (mLocationClient != null)
            mLocationClient.stopLocation();
    }

    static class ExecuteFinished extends Handler {
        private GisView host;
        ExecuteFinished(GisView host) {
            this.host = host;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Common.ADD_GENERAL_MARKER:
                    UrlMarkerMaker.UrlGeneralMarker gmarker = (UrlMarkerMaker.UrlGeneralMarker) msg.obj;
                    host.addMarker(gmarker.layerId, gmarker.layerIndex, gmarker.markers);
                    break;
                case Common.ADD_FLASH_MARKER:
                    UrlMarkerMaker.UrlFlashMarker fmarker = (UrlMarkerMaker.UrlFlashMarker) msg.obj;
                    host.addMarker(fmarker.layerId, fmarker.layerIndex, fmarker.markers);
                    break;
                case Common.QUERY_BUILDINGS:
                    QueryUtils.BuildingResult buildingResult = (QueryUtils.BuildingResult) msg.obj;
                    host.initBuildingEvent(buildingResult);
                    break;
                case Common.QUERY_INDOOR_MAP:
                    QueryUtils.IndoorMapResult indoor = (QueryUtils.IndoorMapResult) msg.obj;
                    host.renderIndoorMap(indoor);
                    if (host.netWorkAnalystUtil != null)
                        host.netWorkAnalystUtil.excutePathService(indoor.buildingId, indoor.floorId);
                    break;
                case Common.QUERY_PERIMETER:
                    QueryUtils.PerimeterResult perimeter = (QueryUtils.PerimeterResult) msg.obj;
                    host.renderPerimeter(perimeter);
                    break;
                case Common.QUERY_MODEL:
                    QueryUtils.ModelResult model = (QueryUtils.ModelResult) msg.obj;
                    host.renderModel(model);
                    break;
                case Common.ANALYST_ROUTE:
                    NetWorkAnalystUtil.CalculatedRoute route = (NetWorkAnalystUtil.CalculatedRoute) msg.obj;
                    host.renderCalculatdRoute(route);
                    break;
                case Common.HEAT_MAP_CALC_END:
                    host.mapView.invalidate();
                case Common.EVENT_MAP_TAP:
                    if (host.mMapListener.size() > 0) {
                        for (MapListener listener : host.mMapListener)
                            listener.mapEvent((MapEvent) msg.obj);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void initBuildingEvent(QueryUtils.BuildingResult buildingResult) {
        if (buildingResult != null && buildingResult.buildings != null) {
            mBuildings = buildingResult.buildings;
//            buildingsLayer = mapControl.getMap().getLayers().add(buildingResult.buildings, true);
//            buildingsLayer.setSelectable(true);
//            LayerSettingVector rgnSetting = (LayerSettingVector) buildingsLayer.getAdditionalSetting();
//            GeoStyle rgnStyle = new GeoStyle();
//            rgnStyle.setLineWidth(0);
//            rgnStyle.setLineColor(new com.supermap.data.Color(255, 255, 255, 255));
//            rgnStyle.setFillBackColor(new com.supermap.data.Color(255, 255, 255, 0));
//            rgnStyle.setFillForeColor(new com.supermap.data.Color(255, 255, 255, 0));
//            rgnStyle.setFillBackOpaque(false);
//            rgnStyle.setFillOpaqueRate(0);
//            rgnSetting.setStyle(rgnStyle);
        }
    }

    private void renderIndoorMap(QueryUtils.IndoorMapResult indoor) {
        for (int i = 0; i < floorLayer.length; i++) {
            if (floorLayer[i] != null) {
//                floorLayer[i].getDataset().close();
                mapControl.getMap().getLayers().remove(floorLayer[i]);
                floorLayer[i] = null;
            }
        }
        if (indoor.buildings != null) {
            floorLayer[0] = mapControl.getMap().getLayers().add(indoor.buildings, true);
            floorLayer[0].setSelectable(false);
            LayerSettingVector rgnSetting = (LayerSettingVector) floorLayer[0].getAdditionalSetting();
            GeoStyle rgnStyle = new GeoStyle();
            rgnStyle.setLineWidth(0.2);
            rgnStyle.setFillForeColor(new com.supermap.data.Color(192, 192, 192));
            rgnSetting.setStyle(rgnStyle);
        }
        if (indoor.floor != null) {
            floorLayer[1] = mapControl.getMap().getLayers().add(indoor.floor, true);
            floorLayer[1].setSelectable(false);
            LayerSettingVector lineSetting = (LayerSettingVector) floorLayer[1].getAdditionalSetting();
            GeoStyle lineStyle = new GeoStyle();
            lineStyle.setLineColor(new com.supermap.data.Color(0, 0, 0));
            lineStyle.setLineWidth(0.2);
            lineStyle.setFillForeColor(new com.supermap.data.Color(128, 128, 128));
            lineSetting.setStyle(lineStyle);
        }
    }

    private void clearAllRoute() {
        if (pathLayer != null) {
            pathLayer.getDataset().close();
            mapControl.getMap().getLayers().remove(pathLayer);
            pathLayer = null;
        }
        tempDatasource.getDatasets().delete("customPath");
        trackingLayer = mapControl.getMap().getTrackingLayer();
        int count = trackingLayer.getCount();
        for (int i = 0; i < count; i++)
        {
            int index = trackingLayer.indexOf("result");
            if (index != -1)
                trackingLayer.remove(index);
        }
        mapView.removeAllCallOut();
    }

    private void renderCalculatdRoute(NetWorkAnalystUtil.CalculatedRoute route) {
        clearAllRoute();

        if (route.routes == null || route.routes.length <= 0) {
            mapControl.getMap().refresh();
            return;
        }

        for (int i = 0; i < route.routes.length; i++)
        {
            GeoLineM geoLineM = route.routes[i];
            GeoStyle style = new GeoStyle();
            style.setLineColor(new com.supermap.data.Color(255, 80, 0));
            style.setLineWidth(1);
            geoLineM.setStyle(style);
            trackingLayer.add(geoLineM, "result");
        }

        mapControl.getMap().refresh();

//        Paint paint = new Paint();
//        paint.setAntiAlias(true);
////        if (route.start != null)
////            paint.setColor(route.start.color);
////        else if (route.end != null)
////            paint.setColor(route.end.color);
////        else if (route.way != null && route.way.length > 0)
////            paint.setColor(route.way[0].color);
////        else
//            paint.setColor(Color.parseColor("#00F20216"));
//        paint.setAlpha(150);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeCap(Paint.Cap.ROUND);
//        paint.setStrokeJoin(Paint.Join.ROUND);
//        paint.setPathEffect(new CornerPathEffect(20));
//        paint.setStrokeWidth(10);
//
//        for (List<Point2D> point2DS : route.route) {
//            if (point2DS.size() > 0) {
//                if (route.start != null)
//                    point2DS.add(0, new Point2D(route.start.coords[1], route.start.coords[0]));
//                if (route.end != null)
//                    point2DS.add(new Point2D(route.end.coords[1], route.end.coords[0]));
//                LineOverlay overlay = new LineOverlay(paint);
//                overlay.setLinePaint(paint);
//                overlay.setPointPaint(paint);
//                overlay.setShowPoints(false);
//                overlay.setData(point2DS);
//                ovls.add(overlay);
//                mapView.getOverlays().add(overlay);
//            }
//        }
//
//        DefaultItemizedOverlay overlay = new DefaultItemizedOverlay(null);
//        Marker marker;
//        if (route.start != null) {
//            marker = new Marker(route.start.coords, "起", 40, 65, route.start);
//            OverlayItemEx item = new OverlayItemEx(
//                    new Point2D(marker.position[1], marker.position[0]),
//                    marker.title, marker.title, marker);
//            MarkerDrawable drawable = new MarkerDrawable(getContext().getResources().getDrawable(R.drawable.green_marker, null), marker.width, marker.height);
//            item.setMarker(drawable);
//            overlay.addItem(item);
//        }
//        if (route.end != null) {
//            marker = new Marker(route.end.coords, "终", 40, 65, route.end);
//            OverlayItemEx item = new OverlayItemEx(
//                    new Point2D(marker.position[1], marker.position[0]),
//                    marker.title, marker.title, marker);
//            MarkerDrawable drawable = new MarkerDrawable(getContext().getResources().getDrawable(R.drawable.red_marker, null), marker.width, marker.height);
//            item.setMarker(drawable);
//            overlay.addItem(item);
//        }
//        ovls.add(overlay);
//        mapView.getOverlays().add(overlay);
//        mapView.invalidate();
    }

    private void renderPerimeter(QueryUtils.PerimeterResult perimeter) {
        removePerimeter();
        perimeterLayer = mapControl.getMap().getLayers().add(perimeter.normalDataset , true);
        perimeterLayer.setSelectable(false);
        LayerSettingVector lineSetting = (LayerSettingVector)perimeterLayer.getAdditionalSetting();
        GeoStyle lineStyle = new GeoStyle();
        lineStyle.setLineColor(new com.supermap.data.Color(perimeter.normal.color));
        lineStyle.setLineWidth(perimeter.normal.width);
        lineSetting.setStyle(lineStyle);
        perimeterAlarmLayer = mapControl.getMap().getLayers().add(perimeter.alarmDataset , true);
        perimeterAlarmLayer.setSelectable(false);
        LayerSettingVector alarmlineSetting = (LayerSettingVector)perimeterAlarmLayer.getAdditionalSetting();
        lineStyle = new GeoStyle();
        lineStyle.setLineColor(new com.supermap.data.Color(perimeter.alarm.color));
        lineStyle.setLineWidth(perimeter.alarm.width);
        alarmlineSetting.setStyle(lineStyle);
        mapControl.getMap().refresh();
    }

    private void renderModel(QueryUtils.ModelResult model) {
        if (model.highlightGeometry == null)
            mapControl.getMap().getLayers().add(model.normalGeometry , true);
        else {
            removeModelhighlighting();
            highlightModelLayer = mapControl.getMap().getLayers().add(model.highlightGeometry , true);
            highlightModelLayer.setSelectable(false);
            LayerSettingVector lineSetting = (LayerSettingVector)highlightModelLayer.getAdditionalSetting();
            GeoStyle lineStyle = new GeoStyle();
            lineStyle.setLineColor(new com.supermap.data.Color(Color.BLUE));
            lineStyle.setLineWidth(0.5);
            lineSetting.setStyle(lineStyle);
        }

        mapControl.getMap().refresh();
    }

    private Paint getBuildingSelectPaint(boolean select) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#2262CC"));
        if (select)
            paint.setAlpha(128);
        else
            paint.setAlpha(0);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(10);

        return paint;
    }
}
