package com.codepunk.demo.support;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.ProgressBar;

import static android.os.Build.VERSION_CODES.N;

public class ProgressBarCompat {

    //region Nested classes
    private interface ProgressBarCompatImpl {
        void setProgress(ProgressBar progressBar, int progress, boolean animate);
    }

    private static final class BaseProgressBarCompatImpl implements ProgressBarCompatImpl {
        @Override
        public void setProgress(ProgressBar progressBar, int progress, boolean animate) {
            progressBar.setProgress(progress);
        }
    }

    @TargetApi(N)
    private static final class NProgressBarCompatImpl implements ProgressBarCompatImpl {
        @Override
        public void setProgress(ProgressBar progressBar, int progress, boolean animate) {
            progressBar.setProgress(progress, animate);
        }
    }
    //endregion Nested classes

    //region Constants
    private static final ProgressBarCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= N) {
            IMPL = new NProgressBarCompatImpl();
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
