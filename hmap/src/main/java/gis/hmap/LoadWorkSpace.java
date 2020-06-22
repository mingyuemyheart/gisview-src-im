package gis.hmap;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Ryan on 2018/11/29.
 */

class LoadWorkSpace {
    private CommonEventListener eventListener;
    private int blockCnt;
    private Object syncObj;

    public LoadWorkSpace(CommonEventListener eventListener) {
        this.eventListener = eventListener;
        blockCnt = 0;
        syncObj = new Object();
    }

    public void startLoad() {
        Common.fixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(Common.getHost() + Common.DATA_SOURCEINFO);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);// 使用 URL 连接进行输出
                    conn.setDoInput(true);// 使用 URL 连接进行输入
                    conn.setUseCaches(false);// 忽略缓存
                    conn.setRequestMethod("GET");// 设置URL请求方法
                    //可设置请求头
                    conn.setRequestProperty("Content-Type", "application/octet-stream");
                    conn.setRequestProperty("Charset", "UTF-8");
                    InputStream is = conn.getInputStream();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int v;
                    while ((v = is.read()) != -1) {
                        baos.write(v);
                    }
                    String str = baos.toString();
                    baos.close();
                    conn.disconnect();
                    JSONObject json = new JSONObject(str);
                    String onlinever = json.getString("version");
                    if (checkNeedUpdate(onlinever)) {
                        final String rootPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                        final JSONArray arr = json.getJSONArray("sourcefile");
                        int cnt = arr.length();
                        blockCnt = 0;
                        for (int i = 0; i < cnt; i++) {
                            final int index = i;
                            Common.downloadThreadTool.execute(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        String fn = arr.getString(index);
                                        URL url = new URL(Common.getHost() + Common.DATA_SOURCE + fn);
                                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                        conn.setDoOutput(true);// 使用 URL 连接进行输出
                                        conn.setDoInput(true);// 使用 URL 连接进行输入
                                        conn.setUseCaches(false);// 忽略缓存
                                        conn.setRequestMethod("GET");// 设置URL请求方法
                                        //可设置请求头
                                        conn.setRequestProperty("Content-Type", "application/octet-stream");
                                        conn.setRequestProperty("Connection", "Keep-Alive");
                                        conn.setRequestProperty("Charset", "UTF-8");
                                        InputStream is = conn.getInputStream();
                                        String wsfolder = rootPath + "/SuperMap/oem/workspace";
                                        FileOutputStream fo = new FileOutputStream(rootPath + "/SuperMap/oem/" + fn);
                                        byte[] buf = new byte[4096];
                                        int cnt;
                                        while ((cnt = is.read(buf)) != -1) {
                                            fo.write(buf, 0, cnt);
                                        }
                                        fo.close();
                                        conn.disconnect();
                                        increaseBlockCount();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                        int timeout = 5*60*10;
                        while (blockCnt < cnt && timeout > 0) {
                            Thread.sleep(100);
                            timeout--;
                        }
                        if (timeout > 0) {
//                        String updatefile = rootPath + "/SuperMap/oem/update.zip";
//                        ZipInputStream zip = new ZipInputStream(new FileInputStream(updatefile));
//                        ZipEntry zipEntry;
//                        String szName = "";
//                        while ((zipEntry = zip.getNextEntry()) != null) {
//                            szName = zipEntry.getName();
//                            if (zipEntry.isDirectory()) {
//                                //获取部件的文件夹名
//                                szName = szName.substring(0, szName.length() - 1);
//                                File folder = new File(wsfolder + File.separator + szName);
//                                folder.mkdirs();
//                            } else {
//                                Log.e("gisview",wsfolder + File.separator + szName);
//                                File file = new File(wsfolder + File.separator + szName);
//                                if (!file.exists()){
//                                    Log.e("gisview", "Create the file:" + wsfolder + File.separator + szName);
//                                    file.getParentFile().mkdirs();
//                                    file.createNewFile();
//                                }
//                                // 获取文件的输出流
//                                FileOutputStream out = new FileOutputStream(file);
//                                int len;
//                                byte[] buffer = new byte[1024];
//                                // 读取（字节）字节到缓冲区
//                                while ((len = zip.read(buffer)) != -1) {
//                                    // 从缓冲区（0）位置写入（字节）字节
//                                    out.write(buffer, 0, len);
//                                    out.flush();
//                                }
//                                out.close();
//                            }
//                        }
//                        zip.close();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private boolean checkNeedUpdate(String ver) {
        String rootPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        rootPath += "/SuperMap/oem/";
        boolean ret = true;
        File dir = new File(rootPath);
        if (!dir.exists()) {
            dir.mkdirs();
            return ret;
        }
        rootPath += "datasource";
        File f = new File(rootPath);
        if (f.exists()) {
            try {
                FileInputStream fi = new FileInputStream(f);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int i;
                while ((i = fi.read()) != -1) {
                    baos.write(i);
                }
                String str = baos.toString();
                JSONObject json = new JSONObject(str);
                String localver = json.getString("version");
                baos.close();
                fi.close();
                if (localver.equalsIgnoreCase(ver))
                    ret = false;
                else
                    ret = true;
            } catch (Exception e) {
                ret = true;
                e.printStackTrace();
            }
        }

        return ret;
    }

    private void increaseBlockCount() {
        synchronized(syncObj) {
            blockCnt++;
        }
    }
}
