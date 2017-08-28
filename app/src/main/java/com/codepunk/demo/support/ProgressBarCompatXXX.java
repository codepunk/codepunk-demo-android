package com.codepunk.demo.support;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v4.math.MathUtils;
import android.widget.ProgressBar;

import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O;

public class ProgressBarCompatXXX {

    //region Nested classes
    private interface ProgressBarCompatImpl {
        void setMax(ProgressBar progressBar, int max);
        void setMin(ProgressBar progressBar, int min);
        int getMin(ProgressBar progressBar);
        int getProgress(ProgressBar progressBar);
        void setProgress(ProgressBar progressBar, int progress);
        void setProgress(ProgressBar progressBar, int progress, boolean animate);
    }

    private static class BaseProgressBarCompatImpl
            implements ProgressBarCompatXXX.ProgressBarCompatImpl {
        private boolean mMaxInitialized;
        private boolean mMinInitialized;
        private int mMax;
        private int mMin;
        private int mProgress;

        @Override
        public void setMax(ProgressBar progressBar, int max) {
            if (mMinInitialized) {
                if (max < mMin) {
                    max = mMin;
                }
            }

            mMaxInitialized = true;
            if (max != mMax) {
                mMax = max;
                if (mMinInitialized) {
                    resolveProgress(progressBar, mMin, mMax, mProgress, false);
                }
            }
        }

        @Override
        public void setMin(ProgressBar progressBar, int min) {
            if (mMaxInitialized) {
                if (min > mMax) {
                    min = mMax;
                }
            }

            mMinInitialized = true;
            if (min != mMin) {
                mMin = min;
                if (mMaxInitialized) {
                    resolveProgress(progressBar, mMin, mMax, mProgress, false);
                }
            }
        }

        @Override
        public int getMin(ProgressBar progressBar) {
            return mMin;
        }

        @Override
        public int getProgress(ProgressBar progressBar) {
            return progressBar.isIndeterminate() ? 0 : mProgress;
        }

        @Override
        public void setProgress(ProgressBar progressBar, int progress) {
            resolveProgress(progressBar, mMin, mMax, progress, false);
        }

        @Override
        public void setProgress(ProgressBar progressBar, int progress, boolean animate) {
            resolveProgress(progressBar, mMin, mMax, progress, animate);
        }

        protected void setProgressInternal(ProgressBar progressBar, int progress, boolean animate) {
            progressBar.setProgress(progress);
        }

        private void resolveProgress(
                ProgressBar progressBar,
                int min,
                int max,
                int progress,
                boolean animate) {
            if (progressBar.isIndeterminate()) {
                return;
            }

            mProgress = MathUtils.clamp(progress, min, max);
            final int range = max - min;
            final int actualProgress =
                    (range == 0 ? 0 : (int) ((mProgress - min) / (float) range));
            setProgressInternal(progressBar, actualProgress, animate);
        }
    }

    @TargetApi(N)
    private static final class NProgressBarCompatImpl extends BaseProgressBarCompatImpl {
        @Override
        protected void setProgressInternal(ProgressBar progressBar, int progress, boolean animate) {
            progressBar.setProgress(progress, animate);
        }
    }

    @TargetApi(O)
    private static final class OProgressBarCompatImpl implements ProgressBarCompatImpl {
        @Override
        public void setMax(ProgressBar progressBar, int max) {
            progressBar.setMax(max);
        }

        @Override
        public void setMin(ProgressBar progressBar, int min) {
            progressBar.setMin(min);
        }

        @Override
        public int getMin(ProgressBar progressBar) {
            return progressBar.getMin();
        }

        @Override
        public int getProgress(ProgressBar progressBar) {
            return progressBar.getProgress();
        }

        @Override
        public void setProgress(ProgressBar progressBar, int progress) {
            progressBar.setProgress(progress);
        }

        @Override
        public void setProgress(ProgressBar progressBar, int progress, boolean animate) {
            progressBar.setProgress(progress, animate);
        }
    }
    //endregion Nested classes

    //region Constants
    private static final ProgressBarCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= O) {
            IMPL = new OProgressBarCompatImpl();
        } else if (Build.VERSION.SDK_INT >= N) {
            IMPL = new NProgressBarCompatImpl();
        } else {
            IMPL = new BaseProgressBarCompatImpl();
        }
    }
    //endregion Constants

    //region Public methods
    public static void setMax(ProgressBar progressBar, int max) {
        IMPL.setMax(progressBar, max);
    }

    public static void setMin(ProgressBar progressBar, int min) {
        IMPL.setMin(progressBar, min);
    }

    public static int getMin(ProgressBar progressBar) {
        return IMPL.getMin(progressBar);
    }

    public static int getProgress(ProgressBar progressBar) {
        return IMPL.getProgress(progressBar);
    }

    public static void setProgress(ProgressBar progressBar, int progress) {
        IMPL.setProgress(progressBar, progress);
    }

    public static void setProgress(ProgressBar progressBar, int progress, boolean animate) {
        IMPL.setProgress(progressBar, progress, animate);
    }
    //endregion Public methods
}
