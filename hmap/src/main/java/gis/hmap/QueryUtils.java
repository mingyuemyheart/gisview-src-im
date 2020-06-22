package gis.hmap;

import android.os.Handler;
import android.os.Message;
import android.os.WorkSource;

//import com.supermap.android.commons.EventStatus;
//import com.supermap.android.data.GetFeaturesByGeometryService;
//import com.supermap.android.data.GetFeaturesBySQLParameters;
//import com.supermap.android.data.GetFeaturesBySQLService;
//import com.supermap.android.data.GetFeaturesResult;
//import com.supermap.android.maps.Point2D;
//import com.supermap.services.components.commontypes.Feature;
//import com.supermap.services.components.commontypes.Geometry;
//import com.supermap.services.components.commontypes.QueryParameter;

import com.supermap.data.CursorType;
import com.supermap.data.Dataset;
import com.supermap.data.DatasetType;
import com.supermap.data.DatasetVector;
import com.supermap.data.DatasetVectorInfo;
import com.supermap.data.Datasets;
import com.supermap.data.Datasource;
import com.supermap.data.DatasourceConnectionInfo;
import com.supermap.data.EncodeType;
import com.supermap.data.EngineType;
import com.supermap.data.GeoArc;
import com.supermap.data.Geometry;
import com.supermap.data.GeometryType;
import com.supermap.data.Point2D;
import com.supermap.data.Point2Ds;
import com.supermap.data.QueryParameter;
import com.supermap.data.Recordset;
import com.supermap.data.Workspace;
import com.supermap.services.FeatureSet;
import com.supermap.services.QueryMode;
import com.supermap.services.QueryOption;
import com.supermap.services.QueryService;
import com.supermap.services.ResponseCallback;
import com.supermap.services.ServiceQueryParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;


/**
 * Created by Ryan on 2018/10/16.
 */

 class QueryUtils {

    public static class BuildingResult {
        public DatasetVector buildings;

        public BuildingResult(DatasetVector buildings) {
            this.buildings = buildings;
        }
    }

    public static void queryAllBuildings(String name, Datasource tempDatasource, Datasource dataDatasource, Handler handler) {
        new Thread(new QueryAllBuildingsRunnable(name, tempDatasource, dataDatasource, handler)).start();
    }

    private static class QueryAllBuildingsRunnable implements Runnable {
        private String name;
        private Handler handler;
        private Datasource tempDatasource;
        private Datasource dataDatasource;

        public QueryAllBuildingsRunnable(String name, Datasource tempDatasource, Datasource dataDatasource, Handler handler) {
            this.name = name;
            this.handler = handler;
            this.tempDatasource = tempDatasource;
            this.dataDatasource = dataDatasource;
        }

        @Override
        public void run() {
            if (dataDatasource == null) {
                return;
            }
            DatasetVector buildings = (DatasetVector) dataDatasource.getDatasets().get("Buildings");
            if (dataDatasource != null && buildings != null) {
                BuildingResult br = new BuildingResult(buildings);
                Message msg = new Message();
                msg.obj = br;
                msg.what = Common.QUERY_BUILDINGS;
                handler.sendMessage(msg);
            }
        }
    }

    public static class IndoorMapResult {
        public String buildingId;
        public String floorId;
        public DatasetVector buildings;
        public DatasetVector floor;

        public IndoorMapResult(String buildingId, String floorId, DatasetVector buildings, DatasetVector floor) {
            this.buildingId = buildingId;
            this.floorId = floorId;
            this.buildings = buildings;
            this.floor = floor;
        }
    }

    public static void queryIndoorMap(String buildingId, String florid,
                                      Datasource tempDatasource, Datasource dataDatasource, Handler handler) {
        new Thread(new QueryIndoorMapRunnable(buildingId, florid, tempDatasource, dataDatasource, handler)).start();
    }

    private static class QueryIndoorMapRunnable implements Runnable {
        private String buildingId;
        private String florid;
        private Handler handler;
        private Datasource tempDatasource;
        private Datasource dataDatasource;

        public QueryIndoorMapRunnable(String buildingId, String florid,
                                      Datasource tempDatasource, Datasource dataDatasource, Handler handler) {
            this.buildingId = buildingId;
            this.florid = florid;
            this.tempDatasource = tempDatasource;
            this.dataDatasource = dataDatasource;
            this.handler = handler;
        }

        @Override
        public void run() {
            DatasetVector building = null, floor = null;
            try {
                building = (DatasetVector) dataDatasource.getDatasets().get("Buildings");
                if (building != null) {
                    QueryParameter queryParameter = new QueryParameter();
                    queryParameter.setAttributeFilter(String.format("SMID=%s", buildingId));
                    queryParameter.setHasGeometry(true);
                    queryParameter.setCursorType(CursorType.STATIC);
                    Recordset queryRecordset = building.query(queryParameter);
                    DatasetVectorInfo dvi = new DatasetVectorInfo();
                    dvi.setType(DatasetType.REGION);
                    dvi.setEncodeType(EncodeType.NONE);
                    dvi.setName("building");
                    tempDatasource.getDatasets().delete("building");
                    building = tempDatasource.getDatasets().create(dvi);
                    building.append(queryRecordset);
                    queryRecordset.dispose();
                }
                floor = (DatasetVector) dataDatasource.getDatasets().get(florid);
                if (floor != null) {
                    QueryParameter queryParameter = new QueryParameter();
                    queryParameter.setAttributeFilter(String.format("BUILDINGID=%s", buildingId));
                    queryParameter.setHasGeometry(true);
                    queryParameter.setCursorType(CursorType.STATIC);
                    Recordset queryRecordset = floor.query(queryParameter);
                    DatasetVectorInfo dvi = new DatasetVectorInfo();
                    dvi.setType(DatasetType.REGION);
                    dvi.setEncodeType(EncodeType.NONE);
                    dvi.setName("floor");
                    tempDatasource.getDatasets().delete("floor");
                    floor = tempDatasource.getDatasets().create(dvi);
                    floor.append(queryRecordset);
                    queryRecordset.dispose();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (building != null || floor != null) {
                IndoorMapResult data = new IndoorMapResult(buildingId, florid, building, floor);
                Message msg = new Message();
                msg.obj = data;
                msg.what = Common.QUERY_INDOOR_MAP;
                handler.sendMessage(msg);
            }
        }
    }

    public static class PerimeterResult {
        public String parkId;
        public PerimeterStyle alarm;
        public PerimeterStyle normal;
        public DatasetVector alarmDataset;
        public DatasetVector normalDataset;

        public PerimeterResult(String parkId, PerimeterStyle alarm, PerimeterStyle normal, DatasetVector alarmDataset, DatasetVector normalDataset) {
            this.parkId = parkId;
            this.alarm = alarm;
            this.normal = normal;
            this.normalDataset = normalDataset;
            this.alarmDataset = alarmDataset;
        }
    }

    public static void queryPerimeter(String parkId, PerimeterStyle alarm, PerimeterStyle normal, int[] alarmList,
                                      Datasource tempDatasource, Datasource dataDatasource, Handler handler) {
        new Thread(new QueryPerimeterRunnable(parkId, alarm, normal, alarmList, tempDatasource, dataDatasource, handler)).start();
    }

    private static class QueryPerimeterRunnable implements Runnable {
        private String parkId;
        private PerimeterStyle alarm;
        private PerimeterStyle normal;
        private int[] alarmList;
        private Handler handler;
        private Datasource tempDatasource;
        private Datasource dataDatasource;

        public QueryPerimeterRunnable(String parkId, PerimeterStyle alarm, PerimeterStyle normal, int[] alarmList,
                                      Datasource tempDatasource, Datasource dataDatasource, Handler handler) {
            this.parkId = parkId;
            this.alarm = alarm;
            this.normal = normal;
            this.alarmList = alarmList;
            this.handler = handler;
            this.tempDatasource = tempDatasource;
            this.dataDatasource = dataDatasource;
        }

        @Override
        public void run() {
            try {
                DatasetVector normalDataset = (DatasetVector) dataDatasource.getDatasets().get("周界");
                DatasetVector alarmDataset = null;
                if (normalDataset != null) {
                    QueryParameter queryParameter = new QueryParameter();
                    if (alarmList != null && alarmList.length > 0) {
                        String cond = String.format("SMID=%d", alarmList[0]);
                        for (int i = 1; i < alarmList.length; i++)
                            cond += String.format(" OR SMID=%d", alarmList[i]);
                        queryParameter.setAttributeFilter(cond);
                    }
                    queryParameter.setHasGeometry(true);
                    queryParameter.setCursorType(CursorType.STATIC);
                    Recordset queryRecordset = normalDataset.query(queryParameter);
                    DatasetVectorInfo dvi = new DatasetVectorInfo();
                    dvi.setType(DatasetType.LINE);
                    dvi.setEncodeType(EncodeType.NONE);
                    dvi.setName("alarmPerimeter");
                    tempDatasource.getDatasets().delete("alarmPerimeter");
                    alarmDataset = tempDatasource.getDatasets().create(dvi);
                    alarmDataset.append(queryRecordset);
                    queryRecordset.dispose();
                }

                if (alarmDataset != null || normalDataset != null) {
                    PerimeterResult data = new PerimeterResult(parkId, alarm, normal, alarmDataset, normalDataset);
                    Message msg = new Message();
                    msg.obj = data;
                    msg.what = Common.QUERY_PERIMETER;
                    handler.sendMessage(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class ModelResult {
        public String parkId;
        public DatasetVector highlightGeometry;
        public DatasetVector normalGeometry;

        public ModelResult(String parkId, DatasetVector highlightGeometry, DatasetVector normalGeometry) {
            this.parkId = parkId;
            this.highlightGeometry = highlightGeometry;
            this.normalGeometry = normalGeometry;
        }
    }

    public static void queryModel(String parkId, int[] modId, Datasource tempDatasource, Datasource dataDatasource, Handler handler) {
        new Thread(new QueryModelRunnable(parkId, modId, tempDatasource, dataDatasource, handler)).start();
    }

    private static class QueryModelRunnable implements Runnable {
        public String parkId;
        private int[] modIds;
        private Handler handler;
        private Datasource tempDatasource;
        private Datasource dataDatasource;

        public QueryModelRunnable(String parkId, int[] modIds, Datasource tempDatasource, Datasource dataDatasource, Handler handler) {
            this.parkId = parkId;
            this.modIds = modIds;
            this.tempDatasource = tempDatasource;
            this.dataDatasource = dataDatasource;
            this.handler = handler;
        }

        @Override
        public void run() {
            if (dataDatasource != null) {
                DatasetVector datasetVector = (DatasetVector) dataDatasource.getDatasets().get("模型");
                DatasetVector highlightGeometry = null;
                if (modIds != null && modIds.length > 0) {
                    QueryParameter queryParameter = new QueryParameter();
                    String cond = String.format("SMID=%d", modIds[0]);
                    for (int i = 1; i < modIds.length; i++)
                        cond += String.format(" OR SMID=%d", modIds[i]);
                    queryParameter.setAttributeFilter(cond);
                    queryParameter.setHasGeometry(true);
                    queryParameter.setCursorType(CursorType.STATIC);
                    Recordset queryRecordset = datasetVector.query(queryParameter);
                    DatasetVectorInfo dvi = new DatasetVectorInfo();
                    dvi.setType(DatasetType.REGION);
                    dvi.setEncodeType(EncodeType.NONE);
                    dvi.setName("highlightModel");
                    tempDatasource.getDatasets().delete("highlightModel");
                    highlightGeometry = tempDatasource.getDatasets().create(dvi);
                    highlightGeometry.append(queryRecordset);
                    queryRecordset.dispose();
                }

                if (highlightGeometry != null || datasetVector != null) {
                    ModelResult data = new ModelResult(parkId, highlightGeometry, datasetVector);
                    Message msg = new Message();
                    msg.obj = data;
                    msg.what = Common.QUERY_MODEL;
                    handler.sendMessage(msg);
                }
            }
        }
    }
}
