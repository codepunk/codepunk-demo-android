package com.codepunk.demo.support;

import android.annotation.TargetApi;
import android.os.Build;

public class IntegerCompat {

    //region Nested classes
    private interface IntegerCompatImpl {
        int compare(int x, int y);
    }

    private static class BaseIntegerCompatImpl implements IntegerCompatImpl {
        @Override
        public int compare(int x, int y) {
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static class KitKatIntegerCompatImpl implements IntegerCompatImpl {
        @Override
        public int compare(int x, int y) {
            return Integer.compare(x, y);
        }
    }
    //endregion Nested classes

    //region Constants
    private static final IntegerCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            IMPL = new KitKatIntegerCompatImpl();
        } else {
            IMPL = new BaseIntegerCompatImpl();
        }
    }
    //endregion Constants

    //region Public methods
    public static int compare(int x, int y) {
        return IMPL.compare(x, y);
    }
    //endregion Public methods
}
