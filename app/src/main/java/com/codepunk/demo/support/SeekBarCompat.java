package com.codepunk.demo.support;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.SeekBar;

import static android.os.Build.VERSION_CODES.N;

public class SeekBarCompat {

    //region Nested classes
    private interface SeekBarCompatImpl {
        void setProgress(SeekBar seekBar, int progress, boolean animate);
    }

    private static final class BaseSeekBarCompatImpl implements SeekBarCompatImpl {
        @Override
        public void setProgress(SeekBar seekBar, int progress, boolean animate) {
            seekBar.setProgress(progress);
        }
    }

    @TargetApi(N)
    private static final class NSeekBarCompatImpl implements SeekBarCompatImpl {
        @Override
        public void setProgress(SeekBar seekBar, int progress, boolean animate) {
            seekBar.setProgress(progress, animate);
        }
    }
    //endregion Nested classes

    //region Constants
    private static final SeekBarCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= N) {
            IMPL = new NSeekBarCompatImpl();
        } else {
            IMPL = new BaseSeekBarCompatImpl();
        }
    }
    //endregion Constants

    //region Public methods
    public static void setProgress(SeekBar seekBar, int progress, boolean animate) {
        IMPL.setProgress(seekBar, progress, animate);
    }
    //endregion Public methods
}
