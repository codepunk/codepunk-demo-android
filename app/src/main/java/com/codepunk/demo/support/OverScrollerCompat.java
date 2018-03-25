package com.codepunk.demo.support;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.OverScroller;

/**
 * A utility class for using {@link android.widget.OverScroller} in a backward-compatible fashion.
 */
public class OverScrollerCompat {

    //region Nested classes

    private interface OverScrollerCompatImpl {
        float getCurrVelocity(OverScroller overScroller);
    }

    private static class BaseOverScrollerCompatImpl implements OverScrollerCompatImpl {
        @Override
        public float getCurrVelocity(OverScroller overScroller) {
            return 0.0f;
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static class IcsOverScrollerCompatImpl implements OverScrollerCompatImpl {
        @Override
        public float getCurrVelocity(OverScroller overScroller) {
            return overScroller.getCurrVelocity();
        }
    }

    //endregion Nested classes

    //region Constants

    private static final OverScrollerCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            IMPL = new IcsOverScrollerCompatImpl();
        } else {
            IMPL = new BaseOverScrollerCompatImpl();
        }
    }

    //endregion Constants

    //region Constructors

    /**
     * Disallow instantiation.
     */
    private OverScrollerCompat() {
    }

    //endregion Constructors

    //region Methods

    /**
     * @see android.view.ScaleGestureDetector#getCurrentSpanY()
     */
    public static float getCurrVelocity(OverScroller overScroller) {
        return IMPL.getCurrVelocity(overScroller);
    }

    //endregion Methods
}
