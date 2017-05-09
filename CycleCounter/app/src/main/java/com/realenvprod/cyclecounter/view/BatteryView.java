package com.realenvprod.cyclecounter.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import com.realenvprod.cyclecounter.R;

/**
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */

public class BatteryView extends android.support.v7.widget.AppCompatImageView {

    private int batteryLevel = -1;

    private Drawable unknown;
    private Drawable alert;
    private Drawable twenty;
    private Drawable thirty;
    private Drawable fifty;
    private Drawable sixty;
    private Drawable eighty;
    private Drawable ninety;
    private Drawable full;

    public BatteryView(Context context) {
        super(context);
        initialize();
    }

    public BatteryView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public BatteryView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        unknown = getResources().getDrawable(R.drawable.ic_battery_unknown_black_24dp);
        alert = getResources().getDrawable(R.drawable.ic_battery_alert_black_24dp);
        twenty = getResources().getDrawable(R.drawable.ic_battery_20_black_24dp);
        thirty = getResources().getDrawable(R.drawable.ic_battery_30_black_24dp);
        fifty = getResources().getDrawable(R.drawable.ic_battery_50_black_24dp);
        sixty = getResources().getDrawable(R.drawable.ic_battery_60_black_24dp);
        eighty = getResources().getDrawable(R.drawable.ic_battery_80_black_24dp);
        ninety = getResources().getDrawable(R.drawable.ic_battery_90_black_24dp);
        full = getResources().getDrawable(R.drawable.ic_battery_full_black_24dp);

        setImageDrawable(unknown);
    }

    public void setBatteryLevel(int level) {
        Drawable drawable = unknown;
        if (level > 0 && level <= 20) {
            drawable = alert;
        } else if (level > 20 && level <= 30) {
            drawable = thirty;
        } else if (level > 30 && level <= 50) {
            drawable = fifty;
        } else if (level > 50 && level <= 60) {
            drawable = sixty;
        } else if (level > 60 && level <= 80) {
            drawable = eighty;
        } else if (level > 80 && level <= 90) {
            drawable = ninety;
        } else if (level > 90 && level <= 100) {
            drawable = full;
        }

        setImageDrawable(drawable);
    }
}
