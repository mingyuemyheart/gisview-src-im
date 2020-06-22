package gis.hmap;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Ryan on 2018/11/28.
 */

class DefaultPopupView extends LinearLayout {
    protected TextView textView;
    protected Object tag;

    public DefaultPopupView(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public DefaultPopupView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0,0);
    }

    public DefaultPopupView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle,0);
    }

    public DefaultPopupView(Context context, AttributeSet attrs, int defStyle, int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
        init(context, attrs, defStyle, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyle, int defStyleRes) {
        View inflate = LayoutInflater.from(context).inflate(R.layout.gisview_popup_default, this, true);

        textView = inflate.findViewById(R.id.tv_popup_default);
        textView.setText("");
    }

    public void setText(CharSequence text) {
        textView.setText(text);
    }

    public void setSize(int width, int height) {
        textView.setWidth(width);
        textView.setHeight(height);
    }

    public void setTag(Object obj) {
        tag = obj;
    }

    public Object getTag() {
        return tag;
    }
}
