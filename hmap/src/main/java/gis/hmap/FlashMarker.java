package gis.hmap;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Ryan on 2018/9/21.
 */

public class FlashMarker extends Marker {
    public Drawable[] images;
    public String[] imagesPath;
    public int duration;

    public FlashMarker(double[] position, String markerId, String[] images, int duration, int width, int height, Object tag) {
        super(position, markerId, width, height, tag);

        this.duration = duration;
        this.images = null;
        imagesPath = null;
        if (images != null) {
            List<String> lst = new ArrayList<>();
            for (String image : images) {
                if (image.startsWith("./")) {
                    String path = Common.getHost() + "/gis/" + image.substring(2);
                    lst.add(path);
                }
                else
                    lst.add(image);
            }
            imagesPath = new String[lst.size()];
            lst.toArray(imagesPath);
        }
    }

    public FlashMarker(double[] position, String markerId, Drawable[] images, int duration, int width, int height, Object tag) {
        super(position, markerId, width, height, tag);

        this.images = images;
        this.duration = duration;
        imagesPath = null;
    }
}
