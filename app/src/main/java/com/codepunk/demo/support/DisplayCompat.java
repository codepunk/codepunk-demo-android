package com.codepunk.demo.support;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR2;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

@SuppressLint({"ObsoleteSdkInt"})
public class DisplayCompat {
    //region Nested classes
    private interface DisplayCompatImpl {
        void getRealMetrics(Display display, DisplayMetrics outMetrics);
        void getRealSize(Display display, Point outSize);
        void getSize(Display display, Point outSize);
    }

    private static class BaseDisplayCompatImpl implements DisplayCompatImpl {
        @Override
        public void getRealMetrics(Display display, DisplayMetrics outMetrics) {
            display.getMetrics(outMetrics);
        }

        @Override
        public void getRealSize(Display display, Point outSize) {
            getSize(display, outSize);
        }

        @Override
        public void getSize(Display display, Point outSize) {
            outSize.set(display.getWidth(), display.getHeight());
        }
    }

    @TargetApi(HONEYCOMB_MR2)
    private static class HoneycombMr2DisplayCompatImpl extends BaseDisplayCompatImpl {
        @Override
        public void getSize(Display display, Point outSize) {
            display.getSize(outSize);
        }
    }

    @TargetApi(JELLY_BEAN_MR1)
    private static class JellyBeanMr1DisplayCompatImpl extends HoneycombMr2DisplayCompatImpl {
        @Override
        public void getRealMetrics(Display display, DisplayMetrics outMetrics) {
            display.getRealMetrics(outMetrics);
        }

        @Override
        public void getRealSize(Display display, Point outSize) {
            display.getRealSize(outSize);
        }
    }
    //endregion Nested classes

    //region Constants
    private static final DisplayCompatImpl IMPL;
    static {
        if (SDK_INT >= JELLY_BEAN_MR1) {
            IMPL = new JellyBeanMr1DisplayCompatImpl();
        } else if (SDK_INT >= HONEYCOMB_MR2) {
            IMPL = new HoneycombMr2DisplayCompatImpl();
        } else {
            IMPL = new BaseDisplayCompatImpl();
        }
    }
    //endregion Constants

    //region Methods
    public static void getRealMetrics(Display display, DisplayMetrics outMetrics) {
        IMPL.getRealMetrics(display, outMetrics);
    }

    public static void getRealSize(Display display, Point outSize) {
        IMPL.getRealSize(display, outSize);
    }

    public static void getSize(Display display, Point outSize) {
        IMPL.getSize(display, outSize);
    }
    //endregion Methods
}
