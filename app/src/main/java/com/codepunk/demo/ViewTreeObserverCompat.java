package com.codepunk.demo;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;

public class ViewTreeObserverCompat {

    //region Nested classes
    private interface ViewTreeObserverCompatImpl {
        void removeOnGlobalLayoutListener(
                ViewTreeObserver observer,
                OnGlobalLayoutListener victim);
    }

    @SuppressWarnings("deprecation")
    private static class BaseViewTreeObserverCompatImpl
            implements ViewTreeObserverCompatImpl {
        @Override
        public void removeOnGlobalLayoutListener(
                ViewTreeObserver observer,
                OnGlobalLayoutListener victim) {
            observer.removeGlobalOnLayoutListener(victim);
        }
    }

    @TargetApi(JELLY_BEAN)
    private static class JellyBeanViewTreeObserverCompatImpl
            implements ViewTreeObserverCompatImpl {
        @Override
        public void removeOnGlobalLayoutListener(
                ViewTreeObserver observer,
                OnGlobalLayoutListener victim) {
            observer.removeOnGlobalLayoutListener(victim);
        }
    }
    //endregion Nested classes

    //region Constants
    private static final ViewTreeObserverCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= JELLY_BEAN) {
            IMPL = new JellyBeanViewTreeObserverCompatImpl();
        } else {
            IMPL = new BaseViewTreeObserverCompatImpl();
        }
    }
    //endregion Constants

    //region Public methods
    public static void removeOnGlobalLayoutListener(
            ViewTreeObserver observer,
            OnGlobalLayoutListener victim) {
        IMPL.removeOnGlobalLayoutListener(observer, victim);
    }
    //endregion Public methods
}
