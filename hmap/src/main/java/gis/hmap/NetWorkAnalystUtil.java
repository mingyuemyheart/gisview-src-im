package gis.hmap;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

//import com.supermap.android.commons.EventStatus;
//import com.supermap.android.maps.Point2D;
//import com.supermap.android.networkAnalyst.FindPathParameters;
//import com.supermap.android.networkAnalyst.FindPathResult;
//import com.supermap.android.networkAnalyst.FindPathService;
//import com.supermap.android.networkAnalyst.TransportationAnalystParameter;
//import com.supermap.android.networkAnalyst.TransportationAnalystResultSetting;
//import com.supermap.services.components.commontypes.Path;
//import com.supermap.services.components.commontypes.Route;

import com.supermap.analyst.networkanalyst.TransportationAnalyst;
import com.supermap.analyst.networkanalyst.TransportationAnalystParameter;
import com.supermap.analyst.networkanalyst.TransportationAnalystResult;
import com.supermap.analyst.networkanalyst.TransportationAnalystSetting;
import com.supermap.analyst.networkanalyst.WeightFieldInfo;
import com.supermap.analyst.networkanalyst.WeightFieldInfos;
import com.supermap.data.CursorType;
import com.supermap.data.DatasetType;
import com.supermap.data.DatasetVector;
import com.supermap.data.DatasetVectorInfo;
import com.supermap.data.Datasource;
import com.supermap.data.EncodeType;
import com.supermap.data.GeoLine;
import com.supermap.data.GeoLineM;
import com.supermap.data.Point2D;
import com.supermap.data.Point2Ds;
import com.supermap.data.Recordset;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ryan on 2018/10/17.
 */

class NetWorkAnalystUtil {
    protected Handler handler;
    protected Datasource tempDatasource;
    protected Datasource dataDatasource;
    protected RoutePoint start;
    protected RoutePoint end;
    protected RoutePoint[] way;
    protected WeightFieldInfos weightFieldInfos;
    protected TransportationAnalystSetting transportationAnalystSetting;

    public NetWorkAnalystUtil(RoutePoint start, RoutePoint end, RoutePoint[] way,
                              Datasource tempDatasource, Datasource dataDatasource, Handler handler) {
        this.handler = handler;
        this.tempDatasource = tempDatasource;
        this.dataDatasource = dataDatasource;
        this.start = start;
        this.end = end;
        this.way = way;

        weightFieldInfos = new WeightFieldInfos();
        WeightFieldInfo weightFieldInfo = new WeightFieldInfo();
        weightFieldInfo.setFTWeightField("smLength");
        weightFieldInfo.setTFWeightField("smLength");
        weightFieldInfo.setName("length");
        weightFieldInfos.add(weightFieldInfo);

        transportationAnalystSetting = new TransportationAnalystSetting();
        transportationAnalystSetting.setEdgeIDField("SmEdgeID");
        transportationAnalystSetting.setNodeIDField("SmNodeID");
        transportationAnalystSetting.setEdgeNameField("roadName");
        transportationAnalystSetting.setTolerance(89);
        transportationAnalystSetting.setWeightFieldInfos(weightFieldInfos);
        transportationAnalystSetting.setFNodeIDField("SmFNode");
        transportationAnalystSetting.setTNodeIDField("SmTNode");
    }

    public void close() {
        weightFieldInfos.clear();
    }

    public static class CalculatedRoute {
        public String buildingId;
        public String floorId;
        public RoutePoint start;
        public RoutePoint end;
        public RoutePoint[] way;
        public GeoLineM[] routes;

        public CalculatedRoute(String buildingId, String floorId, RoutePoint start, RoutePoint end, RoutePoint[] way, GeoLineM[] routes) {
            this.buildingId = buildingId;
            this.floorId = floorId;
            this.start = start;
            this.end = end;
            this.way = way;
            this.routes = routes;
        }
    }

    public void excutePathService(String buildingId, String floorId) {
        Common.fixedThreadPool.execute(new PathServiceRunnable(this, buildingId, floorId));
    }

    private class PathServiceRunnable implements Runnable {
        protected NetWorkAnalystUtil util;
        protected String buildingId;
        protected String floorId;

        public PathServiceRunnable(NetWorkAnalystUtil util, String buildingId, String floorId) {
            this.util = util;
            this.buildingId = buildingId;
            this.floorId = floorId;
        }

        @Override
        public void run() {
            Point2Ds point2Ds = new Point2Ds();
            if (start != null && start.buildingId == buildingId && start.floorid == floorId)
                point2Ds.add(new Point2D(start.coords[1], start.coords[0]));
            for (RoutePoint p : way) {
                if (p.buildingId == buildingId && p.floorid == floorId)
                    point2Ds.add(new Point2D(p.coords[1], p.coords[0]));
            }
            if (end != null && end.buildingId == buildingId && end.floorid == floorId)
                point2Ds.add(new Point2D(end.coords[1], end.coords[0]));

            if (point2Ds.getCount() <= 0)
                return;

            DatasetVector network;
            if (TextUtils.isEmpty(floorId))
                network = (DatasetVector) dataDatasource.getDatasets().get("yuanqu_Network");
            else
                network = (DatasetVector) dataDatasource.getDatasets().get(floorId + "_Network");
            if (network != null) {
                transportationAnalystSetting.setNetworkDataset(network);
                TransportationAnalyst analyst = new TransportationAnalyst();
                analyst.setAnalystSetting(transportationAnalystSetting);
                analyst.load();

                TransportationAnalystParameter parameter = new TransportationAnalystParameter();
                parameter.setWeightName("length");
                //设置最佳路径分析的返回对象
                parameter.setPoints(point2Ds);
                parameter.setNodesReturn(true);
                parameter.setEdgesReturn(true);
                parameter.setPathGuidesReturn(true);
                parameter.setRoutesReturn(true);

                TransportationAnalystResult transportationAnalystResult;
                try {
                    transportationAnalystResult = analyst.findPath(parameter, false);
                    GeoLineM[] routes = transportationAnalystResult.getRoutes();

                    CalculatedRoute result = new CalculatedRoute(buildingId, floorId, start, end, way, routes);
                    Message msg = new Message();
                    msg.obj = result;
                    msg.what = Common.ANALYST_ROUTE;
                    handler.sendMessage(msg);

                    parameter.dispose();
                    point2Ds.clear();
                    analyst.dispose();
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.obj = null;
                    msg.what = Common.ANALYST_ROUTE;
                    handler.sendMessage(msg);
                }
            }
        }
    }
}
