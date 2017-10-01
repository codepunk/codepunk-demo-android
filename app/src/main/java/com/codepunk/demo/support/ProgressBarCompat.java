package com.codepunk.demo.support;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.ProgressBar;

@SuppressWarnings({"unused"})
public class ProgressBarCompat {
    //region Nested classes
    private interface ProgressBarCompatImpl {
        int getMin(ProgressBar progressBar);
        void setProgress(ProgressBar progressBar, int progress, boolean animate);
    }

    private static class BaseProgressBarCompatImpl implements ProgressBarCompatImpl {
        @Override
        public int getMin(ProgressBar progressBar) {
            return 0;
        }

        @Override
        public void setProgress(ProgressBar progressBar, int progress, boolean animate) {
            progressBar.setProgress(progress);
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static class NougatProgressBarCompatImpl extends BaseProgressBarCompatImpl {
        @Override
        public void setProgress(ProgressBar progressBar, int progress, boolean animate) {
            progressBar.setProgress(progress, animate);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static class OreoProgressBarCompatImpl extends NougatProgressBarCompatImpl {
        @Override
        public int getMin(ProgressBar progressBar) {
            return progressBar.getMin();
        }
    }
    //endregion Nested classes

    //region Constants
    private static final ProgressBarCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            IMPL = new OreoProgressBarCompatImpl();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            IMPL = new NougatProgressBarCompatImpl();
        } else {
            IMPL = new BaseProgressBarCompatImpl();
        }
    }
    //endregion Constants

    //region Public methods
    public static int getMin(ProgressBar progressBar) {
        return IMPL.getMin(progressBar);
    }

    public static void setProgress(ProgressBar progressBar, int progress, boolean animate) {
        IMPL.setProgress(progressBar, progress, animate);
    }
    //endregion Public methods
}
