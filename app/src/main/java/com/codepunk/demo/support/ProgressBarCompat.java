package com.codepunk.demo.support;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.ProgressBar;

public class ProgressBarCompat {
    //region Nested classes
    private interface ProgressBarCompatImpl {
        void setProgress(ProgressBar progressBar, int progress, boolean animate);
    }

    private static class BaseProgressBarCompatImpl implements ProgressBarCompatImpl {
        @Override
        public void setProgress(ProgressBar progressBar, int progress, boolean animate) {
            progressBar.setProgress(progress);
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static class NougatProgressBarCompatImpl implements ProgressBarCompatImpl {
        @Override
        public void setProgress(ProgressBar progressBar, int progress, boolean animate) {
            progressBar.setProgress(progress, animate);
        }
    }
    //endregion Nested classes

    //region Constants
    private static final ProgressBarCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            IMPL = new NougatProgressBarCompatImpl();
        } else {
            IMPL = new BaseProgressBarCompatImpl();
        }
    }
    //endregion Constants

    //region Public methods
    public static void setProgress(ProgressBar progressBar, int progress, boolean animate) {
        IMPL.setProgress(progressBar, progress, animate);
    }
    //endregion Public methods
}
