package gis.hmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ryan on 2018/10/18.
 */
class UrlMarkerMaker {

    public static class UrlGeneralMarker {
        public String layerId;
        public int layerIndex;
        public GeneralMarker[] markers;

        public UrlGeneralMarker(String layerId, int layerIndex, GeneralMarker[] markers) {
            this.layerId = layerId;
            this.layerIndex = layerIndex;
            this.markers = markers;
        }
    }

    public static void makeUrlMarker(Context context, String layerId, int layerIndex, GeneralMarker[] markers, Handler handler) {

        new Thread(new UrlGeneralMarkerRunnable(context, layerId, layerIndex, markers, handler)).start();
    }

    private static class UrlGeneralMarkerRunnable implements Runnable {
        private Context context;
        private String layerId;
        private int layerIndex;
        private GeneralMarker[] markers;
        private Handler handler;

        public UrlGeneralMarkerRunnable(Context context, String layerId, int layerIndex, GeneralMarker[] markers, Handler handler) {
            this.context = context;
            this.layerId = layerId;
            this.layerIndex = layerIndex;
            this.markers = markers;
            this.handler = handler;
        }

        @Override
        public void run() {
            if (markers == null) return;
            if (markers.length <= 0) return;

            List<GeneralMarker> ready = new ArrayList<>();
            try {
                for (GeneralMarker marker : markers) {
                    URL url = new URL(marker.imagePath);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setRequestMethod("GET");
                    if (conn.getResponseCode() == 200){
                        InputStream inputStream = conn.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        marker.image = new BitmapDrawable(context.getResources(), bitmap);
                        inputStream.close();
                        if (marker.image != null)
                            ready.add(marker);
                    }
//                    marker.image = Drawable.createFromStream(url.openStream(), null);
//                    if (marker.image != null)
//                        ready.add(marker);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (ready.size() > 0) {
                GeneralMarker[] updatedMarker = new GeneralMarker[ready.size()];
                ready.toArray(updatedMarker);
                UrlGeneralMarker marker = new UrlGeneralMarker(layerId, layerIndex, updatedMarker);

                Message msg = new Message();
                msg.obj = marker;
                msg.what = Common.ADD_GENERAL_MARKER;
                handler.sendMessage(msg);
            }
        }
    }

    public static class UrlFlashMarker {
        public String layerId;
        public int layerIndex;
        public FlashMarker[] markers;

        public UrlFlashMarker(String layerId, int layerIndex, FlashMarker[] markers) {
            this.layerId = layerId;
            this.layerIndex = layerIndex;
            this.markers = markers;
        }
    }

    public static void makeUrlMarker(String layerId, int layerIndex, FlashMarker[] markers, Handler handler) {

        new Thread(new UrlFlashMarkerRunnable(layerId, layerIndex, markers, handler)).start();
    }

    private static class UrlFlashMarkerRunnable implements Runnable {
        private String layerId;
        private int layerIndex;
        private FlashMarker[] markers;
        private Handler handler;

        public UrlFlashMarkerRunnable(String layerId, int layerIndex, FlashMarker[] markers, Handler handler) {
            this.layerId = layerId;
            this.layerIndex = layerIndex;
            this.markers = markers;
            this.handler = handler;
        }

        @Override
        public void run() {
            if (markers == null) return;
            if (markers.length <= 0) return;

            List<FlashMarker> ready = new ArrayList<>();
            try {
                for (FlashMarker marker : markers) {
                    List<Drawable> images = new ArrayList<>();
                    for (String path : marker.imagesPath) {
                        URL url = new URL(path);
                        Drawable image = Drawable.createFromStream(url.openStream(), null);
                        if (image != null)
                            images.add(image);
                    }
                    if (images.size() > 0) {
                        marker.images = new Drawable[images.size()];
                        marker.images = images.toArray(marker.images);
                        ready.add(marker);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (ready.size() > 0) {
                FlashMarker[] updatedMarker = new FlashMarker[ready.size()];
                ready.toArray(updatedMarker);
                UrlFlashMarker marker = new UrlFlashMarker(layerId, layerIndex, updatedMarker);

                Message msg = new Message();
                msg.obj = marker;
                msg.what = Common.ADD_FLASH_MARKER;
                handler.sendMessage(msg);
            }
        }
    }
}
