package com.codepunk.demo.support;

import android.annotation.TargetApi;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;

import java.lang.reflect.Method;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR2;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

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

        @SuppressWarnings("deprecation")
        @Override
        public void getRealSize(Display display, Point outSize) {
            outSize.set(display.getWidth(), display.getHeight());
        }

        @SuppressWarnings("deprecation")
        @Override
        public void getSize(Display display, Point outSize) {
            outSize.set(display.getWidth(), display.getHeight());
        }
    }

    @TargetApi(HONEYCOMB_MR2)
    private static class HoneycombMr2DisplayCompatImpl implements DisplayCompatImpl {
        @Override
        public void getRealMetrics(Display display, DisplayMetrics outMetrics) {
            try {
                final Method getRealMetrics =
                        Display.class.getDeclaredMethod("getRealMetrics", DisplayMetrics.class);
                getRealMetrics.invoke(display, outMetrics);
            } catch (Exception e) {
                display.getMetrics(outMetrics);
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public void getRealSize(Display display, Point outSize) {
            try {
                final Method getRealWidth = Display.class.getDeclaredMethod("getRealWidth");
                final Method getRealHeight = Display.class.getDeclaredMethod("getRealHeight");
                outSize.set(
                        (Integer) getRealWidth.invoke(display),
                        (Integer) getRealHeight.invoke(display));
            } catch (Exception e) {
                display.getSize(outSize);
            }
        }

        @Override
        public void getSize(Display display, Point outSize) {
            display.getSize(outSize);
        }
    }

    @TargetApi(ICE_CREAM_SANDWICH)
    private static class IceCreamSandwichDisplayCompatImpl extends HoneycombMr2DisplayCompatImpl {
        @Override
        public void getRealSize(Display display, Point outSize) {
            try {
                Method getRealSize =
                        Display.class.getDeclaredMethod("getRealSize", Point.class);
                if (!getRealSize.isAccessible()) {
                    getRealSize.setAccessible(true);
                }
                getRealSize.invoke(display, outSize);
            } catch (Exception e) {
                display.getSize(outSize);
            }
        }
    }

    @TargetApi(JELLY_BEAN_MR1)
    private static class JellyBeanMr1DisplayCompatImpl extends IceCreamSandwichDisplayCompatImpl {
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
        } else if (SDK_INT >= ICE_CREAM_SANDWICH) {
            IMPL = new IceCreamSandwichDisplayCompatImpl();
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
