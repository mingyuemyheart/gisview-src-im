package gis.gisdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import gis.hmap.GeoLocation;
import gis.hmap.GeoServiceCallback;
import gis.hmap.GisView;
import gis.hmap.HeatPoint;
import gis.hmap.BuildingEvent;
import gis.hmap.BuildingListener;
import gis.hmap.FlashMarker;
import gis.hmap.GeneralMarker;
import gis.hmap.LocationEvent;
import gis.hmap.LocationListener;
import gis.hmap.MapEvent;
import gis.hmap.MapListener;
import gis.hmap.Marker;
import gis.hmap.MarkerEvent;
import gis.hmap.MarkerListener;
import gis.hmap.ObjectInfo;
import gis.hmap.QueryCallback;
import gis.hmap.RoutePoint;
import gis.hmap.ZoomEvent;
import gis.hmap.ZoomListener;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        MarkerListener, BuildingListener, ZoomListener, MapListener, LocationListener,
        GeoServiceCallback, QueryCallback {

    private String[] mPerms = {
            "android.permission.INTERNET",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_INTERNAL_STORAGE",
            "android.permission.READ_INTERNAL_STORAGE",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.CHANGE_WIFI_STATE",
            "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_PHONE_STATE",
            "android.permission.MOUNT_UNMOUNT_FILESYSTEMS"};

    private int cnt = 0;
    private String markerId;
    private boolean permissionflag = false; //
    private Handler mainHandler = new Handler();

    GisView gisView = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        gisView = (GisView) findViewById(R.id.gisView);
//        gisView.setGisServer("http://10.240.209.28:8090");
        gisView.setGisServer("http://47.97.169.150:8090");
        gisView.setRTLSServer("https://10.240.155.52:18889");
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        initPermission();
        checkPermission();
    }

    private void checkPermission(){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if (!PermissionDetect.hasPermissions(this, mPerms))
                ActivityCompat.requestPermissions(this, mPerms,1);
            else
                initGisView();
//        else
//            initGisView();
    }

    private void initPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //检查权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //请求权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                permissionflag = true;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //请求权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
//        } else {
//            permissionflag = true;
//        }
    }

    /**
     * 权限的结果回调函数
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            permissionflag = grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED;
        }
        initGisView();
    }

    private void initGisView() {
        try {
            byte[] buf = new byte[1024];
            final String listence = "SuperMap iMobile Trial.slm";
            String rootPath = getExternalFilesDir("supermap").getAbsolutePath();
            File dir = new File(rootPath + "/license/");
            if (!dir.exists())
                dir.mkdirs();
            dir = new File(rootPath + "/oem/workspace/");
            if (!dir.exists())
                dir.mkdirs();

            File listenceFile = new File(rootPath + "/license/" + listence);
            if (!listenceFile.exists()) {
                int nRead = 0;
                InputStream in = getResources().getAssets().open(listence);
                FileOutputStream out = new FileOutputStream(listenceFile);
                while ((nRead = in.read(buf, 0 ,1024)) > 0) {
                    out.write(buf, 0, nRead);
                }
                out.flush();
                out.close();
                in.close();
            }

            File dataFile = new File(rootPath + "/oem/data.zip");
            if (!dataFile.exists()) {
                int nRead = 0;
                InputStream in = getResources().getAssets().open("data.zip");
                FileOutputStream out = new FileOutputStream(dataFile);
                while ((nRead = in.read(buf, 0 ,1024)) > 0) {
                    out.write(buf, 0, nRead);
                }
                out.flush();
                out.close();
                in.close();
                Unzip(rootPath + "/oem/data.zip", rootPath + "/oem/workspace/");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {}
                gisView.loadMap(9,new double[] {22.972860320987436, 113.35606992244722});
            }
        });
        gisView.initEngine(getApplicationContext());
    }
    private static void Unzip(String zipFile, String targetDir) {
        int BUFFER = 4096; //这里缓冲区我们使用4KB，
        String strEntry; //保存每个zip的条目名称

        try {
            BufferedOutputStream dest = null; //缓冲输出流
            FileInputStream fis = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry; //每个zip条目的实例


            while ((entry = zis.getNextEntry()) != null) {


                try {
                    Log.i("Unzip: ","="+ entry);
                    int count;
                    byte data[] = new byte[BUFFER];
                    strEntry = entry.getName();


                    File entryFile = new File(targetDir + strEntry);
                    File entryDir = new File(entryFile.getParent());
                    if (!entryDir.exists()) {
                        entryDir.mkdirs();
                    }


                    FileOutputStream fos = new FileOutputStream(entryFile);
                    dest = new BufferedOutputStream(fos, BUFFER);
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            zis.close();
        } catch (Exception cwj) {
            cwj.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gisView.deinitEngine();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        //加载地图
        if (id == R.id.loadMap) {
            gisView.loadMap(9,new double[] {22.972860820987436, 113.35606990244722});
//        } else  if (id == R.id.setThemeLight) {
//            gisView.setTheme("light");
//        }  else  if (id == R.id.setThemeDark) {
//            gisView.setTheme("dark");
        } else if(id == R.id.encodeAddress){  //查询经纬度对应地名
            gisView.getAddressOfLocation(113.356064264385,22.9728369011972,0.0005,5,this);
            Log.d("GisView", "location ok");

        } else if(id == R.id.decodeAddress){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("搜索地址");
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            builder.setPositiveButton("搜索", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String data = input.getText().toString();
                    gisView.getLocationOfAddress(data,5,MainActivity.this);  //位置搜索（模糊匹配）
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();

            //gisView.getLocationOfAddress("2号楼",5,this);  //位置搜索（模糊匹配）
        } else if(id == R.id.qureyObject){
            gisView.queryObject("1", "2号楼", this);
        } else if(id == R.id.buildingInfo) {
            gisView.getBuldingInfo("1", "1", this);
        } else if(id == R.id.menuGPS){
            GeoLocation loc = gisView.getMyLocation();  //获取我的定位
            if(loc == null) //定位失败
                Toast.makeText(this, "定位失败", Toast.LENGTH_SHORT).show();
            else {
                String str = String.format("位置: lng:%f, lat:%f, addr:%s", loc.lng, loc.lat, loc.address);
                Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
                GeneralMarker[] markers = new GeneralMarker[]{
                        new GeneralMarker(
                                new double[]{loc.lng, loc.lat},
                                "位置",
                                getResources().getDrawable(R.drawable.green_marker, null),
                                64, 64, null)
                };
                gisView.addMarker("lm01", 999, markers);
                gisView.setCenter(loc.lng, loc.lat);
            }

        } else if (id == R.id.unloadMap) {
            gisView.destroyMap();
            //加载楼层
        } else if (id == R.id.loadF1) {
            gisView.showIndoorMap("1","F1");
        } else if (id == R.id.loadF2) {
            gisView.showIndoorMap("1","F2");
        } else if (id == R.id.loadF3) {
            gisView.showIndoorMap("1","F3");
            //退出
        } else if (id == R.id.loadOutdoor) {
            gisView.switchOutdoor();
        }
        else if (id == R.id.addMarker) {

            GeneralMarker[] markers = new GeneralMarker[] {
                    new GeneralMarker(
                            new double[] { 22.972860320987436 + (Math.random()-0.5) / 1000, 113.35606992244722 + (Math.random()-0.5) / 1000 },
                            String.format("marker%d", cnt++),
                            getResources().getDrawable(R.drawable.tag_pin, null),
                            64, 64, null),
                    new GeneralMarker(
                            new double[] { 22.972860320987436 + (Math.random()-0.5) / 1000, 113.35606992244722 + (Math.random()-0.5) / 1000 },
                            String.format("marker%d", cnt++),
                            getResources().getDrawable(R.drawable.tag_pin, null),
                            64, 64, null)
            };
            gisView.addMarker("lm01", 999, markers);
        }
        else if (id == R.id.addMarkerUrl) {

            GeneralMarker[] markers = new GeneralMarker[] {
                    new GeneralMarker(
                            new double[] { 22.972860320987436 + (Math.random()-0.5) / 1000, 113.35606992244722 + (Math.random()-0.5) / 1000 },
                            String.format("marker%d", cnt++), "./images/pic1.png", 64, 64, null),
                    new GeneralMarker(
                            new double[] { 22.972860320987436 + (Math.random()-0.5) / 1000, 113.35606992244722 + (Math.random()-0.5) / 1000 },
                            String.format("marker%d", cnt++), "./images/pic2.png", 64, 64, null)
            };
            gisView.addMarker("lm01", 999, markers);

        }
        else if (id == R.id.addFlashMarker) {
            Drawable[] ani = new Drawable[] {
                    getResources().getDrawable(R.drawable.flash_1, null),
                    getResources().getDrawable(R.drawable.flash_2, null),
                    getResources().getDrawable(R.drawable.flash_3, null)
            };
            FlashMarker[] markers = new FlashMarker[] {
                    new FlashMarker(
                            new double[] { 22.972860320987436 + (Math.random()-0.5) / 1000, 113.35606992244722 + (Math.random()-0.5) / 1000 },
                            String.format("marker%d", cnt++), ani, 500, 192, 192, null),
                    new FlashMarker(
                            new double[] { 22.972860320987436 + (Math.random()-0.5) / 1000, 113.35606992244722 + (Math.random()-0.5) / 1000 },
                            String.format("marker%d", cnt++), ani, 500, 192, 192, null),
            };
            //gisView.addMarker("lm02", 999, markers);
            gisView.addFlashMarker("lm02", 999, markers);
        }
        else if (id == R.id.addFlashMarkerUrl) {
            String[] ani = new String[] { "./images/1.png", "./images/2.png", "./images/3.png", "./images/4.png", "./images/5.png" };
            FlashMarker[] markers = new FlashMarker[] {
                    new FlashMarker(
                            new double[] { 22.972860320987436 + (Math.random()-0.5) / 1000, 113.35606992244722 + (Math.random()-0.5) / 1000 },
                            String.format("marker%d", cnt++), ani, 500, 64, 64, null),
                    new FlashMarker(
                            new double[] { 22.972860320987436 + (Math.random()-0.5) / 1000, 113.35606992244722 + (Math.random()-0.5) / 1000 },
                            String.format("marker%d", cnt++), ani, 500, 64, 64, null),
            };
            gisView.addFlashMarker("lm02", 999, markers);
        }
        else if (id == R.id.zoom1) {
            gisView.setZoom(new double []{22.97286, 113.35606}, 1);
        }
        else if (id == R.id.zoom7) {
            gisView.setZoom(new double []{22.97286, 113.35606}, 6);
        }
        else if(id == R.id.zoomIn){
            gisView.zoomInMap();
        }
        else if(id == R.id.zoomOut){
            gisView.zoomOutMap();
        }
        //添加信息框
        else if(id == R.id.addpopup){
            gisView.addPopup(
                    new double []{22.97286 + (Math.random()-0.5) / 1000, 113.35606 + (Math.random()-0.5) / 1000},
                    "信息框",
                    300,
                    100,
                    "hello marker"
            );
        }

        //关闭信息框
        else if(id == R.id.closepopup){
            gisView.closePopup();
        }
        //显示周界
        else if(id == R.id.displayPerimeter){
            gisView.displayPerimeter(
                    "1",
                    "#0000FF",
                    2,
                    50,
                    "#FF00FF",
                    2,
                    50,
                    new int [] {10, 12, 14, 16});
        }
        //显示周界
        else if(id == R.id.displayPerimeter2){
            gisView.displayPerimeter(
                    "1",
                    "#0000FF",
                    2,
                    50,
                    "#FF0000",
                    2,
                    50,
                    new int [] {10, 12});
        }
        //移除周界
        else if(id == R.id.removePerimeter){
            gisView.removePerimeter();
        }
        else if(id == R.id.drawRoute){
            RoutePoint[] routePoints = new RoutePoint[5];
            for (int i = 0; i < 5; i++) {
                RoutePoint routePoint = new RoutePoint(
                        new double[] {
                                22.972860320987436 + (Math.random()-0.5) / 1000.0,
                                113.35606992244722 + (Math.random()-0.5) / 1000.0
                        },
                        Color.YELLOW, "none", "none", 1, 5);
                routePoints[i] = routePoint;
            }
            gisView.drawCustomPath(routePoints);
        }
        else if(id == R.id.caclRoute){
            gisView.calcRoutePath(
                    new RoutePoint(new double[] { 22.972897436243493, 113.35581243077175 },
                            Color.parseColor("#F20216"),
                            "", "", 20, 100),
                    new RoutePoint(new double[] { 22.972386774354412, 113.35689159098212 },
                            Color.parseColor("#F20216"),
                            "", "", 20, 100),
                    new RoutePoint[] {
                            new RoutePoint(new double[] { 22.972724848576696, 113.35585224707808 },
                                    Color.parseColor("#F20216"),
                                    "", "", 20, 100),
                            new RoutePoint(new double[] { 22.972630664983477, 113.35606172043653 },
                                    Color.parseColor("#F20216"),
                                    "", "", 20, 100),
                            new RoutePoint(new double[] { 22.97276682508405, 113.3562745404476 },
                                    Color.parseColor("#F20216"),
                                    "", "", 20, 100),
                            new RoutePoint(new double[] { 22.972969445613277, 113.35655969681977 },
                                    Color.parseColor("#F20216"),
                                    "", "", 20, 100),
                            new RoutePoint(new double[] { 22.9724934566077, 113.3569276414497 },
                                    Color.parseColor("#F20216"),
                                    "", "", 20, 100),
                    });
            GeneralMarker[] markers = new GeneralMarker[] {
                    new GeneralMarker(
                            new double[] { 22.972897436243493, 113.35581243077175 },
                            String.format("s1", cnt++),
                            getResources().getDrawable(R.drawable.green_marker, null),
                            40, 65, null),
                    new GeneralMarker(
                            new double[] { 22.972386774354412, 113.35689159098212 },
                            String.format("e1", cnt++),
                            getResources().getDrawable(R.drawable.red_marker, null),
                            40, 65, null)
            };
            gisView.addMarker("lm01", 999, markers);
        }
        else if(id == R.id.caclRouteinner){
            gisView.calcRoutePath(
                    new RoutePoint(new double[] { 22.97294157090782, 113.35612287918616 },
                            Color.parseColor("#F20216"),
                            "1", "F3", 20, 100),
                    new RoutePoint(new double[] { 22.972871099009524, 113.35618158596151 },
                            Color.parseColor("#F20216"),
                            "1", "F3", 20, 100),
                    new RoutePoint[] {
                            new RoutePoint(new double[] { 22.97284138910299, 113.35606036954283 },
                                    Color.parseColor("#F20216"),
                                    "1", "F3", 20, 100)
                    });
            GeneralMarker[] markers = new GeneralMarker[] {
                    new GeneralMarker(
                            new double[] { 22.97294157090782, 113.35612287918616 },
                            String.format("s1", cnt++),
                            getResources().getDrawable(R.drawable.green_marker, null),
                            40, 65, null),
                    new GeneralMarker(
                            new double[] { 22.972871099009524, 113.35618158596151 },
                            String.format("e1", cnt++),
                            getResources().getDrawable(R.drawable.red_marker, null),
                            40, 65, null)
            };
            gisView.addMarker("lm01", 999, markers);
        }
        else if(id == R.id.clearRoute){
            gisView.clearPath();
            gisView.deleteMarker("s1");
            gisView.deleteMarker("e1");
        }
        //生成热力图
        else if(id == R.id.showHeatmap){

            //22.972860320987436, 113.35606992244722
            int heatNumbers = 100;
            int radius = 30;
            HeatPoint[] heatPoints  = new HeatPoint[heatNumbers];

            for(int i = 0; i < heatNumbers; i++) {
                heatPoints[i] = new HeatPoint();
                heatPoints[i].lat = (Math.random() - 0.5) * 0.00028 + 22.972860320987436;
                heatPoints[i].lng =  (Math.random() - 0.5) * 0.0005 + 113.35606992244722;
                heatPoints[i].value = (int)(Math.random() * 100);
                heatPoints[i].tag = null;
            }

            gisView.showHeatMap(heatPoints, radius, 0.3);
        }
        //移除热力图
        else if(id == R.id.clearHeatmap){
            gisView.clearHeatMap();
        }
        //车位/模型高亮
        else if(id == R.id.modalHighlight){
            gisView.showModelHighlight("1",new int[]{1, 2, 3});
        }
        //车位模型关闭高亮
        else if(id == R.id.disableHighlight){
            gisView.removeModelhighlighting();
        }
        //设置地图中心
        else if(id == R.id.setCenter){
            gisView.setCenter(22.972860820987436, 113.35606990244722);
        }
        //设置地图中心
        else if(id == R.id.markerPos){
            double lat = (Math.random() - 0.5) * 0.00028 + 22.972860320987436;
            double lng = (Math.random() - 0.5) * 0.0005 + 113.35606992244722;

            gisView.changeMarkerPosition("lm01", lat, lng); //设置marker位置
            //gisView.showCurrentPosition();
            gisView.setCenter(lat, lng); //移动地图
        }
        //删除指定marker
        else if (id == R.id.deleteMarker) {
            if (markerId != null)
                gisView.deleteMarker(markerId);
        }
        else if (id == R.id.deleteLayer) {
            gisView.deleteLayer("lm01");
            gisView.deleteLayer("lm02");
        }
        else if (id == R.id.addmkrevent) {
            gisView.addMarkerListener(this);
        }
        else if (id == R.id.delmkrevent) {
            gisView.removeMarkerListener(this);
        }

        else if (id == R.id.addbudevent) {
            gisView.addBuildingListener(this);
        }
        else if (id == R.id.delbudevent) {
            gisView.removeBuildingListener(this);
        }
//        else if (id == R.id.addzmevent) {
//            gisView.addZoomListener(this);
//        }
//        else if (id == R.id.delzmevent) {
//            gisView.removeZoomListener(this);
//        }
        else if (id == R.id.getZoom) {
            int l = gisView.getZoom();
            Toast.makeText(this, String.format("缩放级别：%d", l), Toast.LENGTH_LONG).show();
        }
//        else if (id == R.id.hidelevel) {
//            gisView.setHideLevel(2);
//        }
        else if (id == R.id.addmaptap) {
            gisView.addMapListener(this);
        }
        else if (id == R.id.delmaptap) {
            gisView.removeMapListener(this);
        }
        else if (id == R.id.addloc) {
            gisView.addLocateListener(this);
        }
        else if (id == R.id.delloc) {
            gisView.removeLocateListener(this);
        }
        else if (id == R.id.startloc) {
            gisView.startLocate();
        }
        else if (id == R.id.stoploc) {
            gisView.stopLocate();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void markerEvent(MarkerEvent me) {
        String msg = String.format("%s, %s, %s", me.eventType.toString(), me.markerId, me.marker.markerId);
        markerId = me.markerId;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void zoomEvent(ZoomEvent ze) {
        String msg = String.format("%s, %d", ze.eventType.toString(), ze.level);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void buildingEvent(BuildingEvent be) {
        String msg = String.format("%s, %s, %s", be.eventType.toString(), be.buildingId, be.getStrParam("NAME"));
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void mapEvent(MapEvent me) {
        String msg = String.format("%s, (%d, %d), latlng(%f, %f)", me.eventType.toString(), me.screenPos[0], me.screenPos[1],
                me.geoPos[0], me.geoPos[1]);
        if (me.addrs != null && me.addrs.length > 0)
            msg += "\r\naddr: " + me.addrs[0];
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocation(final LocationEvent le) {
        final String msg = String.format("%s, latlng(%f, %f), building:%s, floor:%s",
                le.address, le.lat, le.lng, le.buildingId, le.floorId);
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                GeneralMarker[] markers = new GeneralMarker[]{
                        new GeneralMarker(
                                new double[]{le.lat, le.lng},
                                "位置",
                                getResources().getDrawable(gis.hmap.R.drawable.red_marker, null),
                                40, 65, null)
                };
                gisView.deleteLayer("lm01");
                gisView.addMarker("lm01", 999, markers);
            }
        }, 100);
    }

    class MyRunnable implements Runnable{
        public GeoLocation [] locations;
        public MyRunnable(GeoLocation [] loc){
            locations = loc;
        }
        @Override
        public void run() {


        }
    }

    @Override
    public void onQueryAddressFinished(GeoLocation[] loc) { //获取地址匹配结束
        Log.d("GisView", "onQueryAddressFinished: ");
        if(loc.length > 0){

            Looper.prepare();
            runOnUiThread(new MyRunnable(loc) {

                @Override
                public void run() {
                    for (GeoLocation c:this.locations ) {
                        GeoLocation loc = c;
                        GeneralMarker[] markers = new GeneralMarker[] {
                                new GeneralMarker(
                                        new double[] { loc.lat,loc.lng },
                                        loc.address,
                                        getResources().getDrawable(R.drawable.tag_pin, null),
                                        72, 72, null),
                        };
                        gisView.addMarker("lm01", 999, markers);
                        gisView.addPopup(
                                new double[] { loc.lat,loc.lng },
                                loc.address,
                                300,
                                100,
                                "hello marker"
                        );
                    }
                }
            });
            Looper.loop();
            Toast.makeText(this, loc[0].address +" is x=" + loc[0].lng + ",y=" + loc[0].lat, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onQueryFinished(final ObjectInfo info) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                String str = String.format("%s, (lng,lat)=%f, %f, NAME=%s", info.address, info.lng, info.lat, info.getStrParam("NAME"));
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
            }
        });
    }
}
