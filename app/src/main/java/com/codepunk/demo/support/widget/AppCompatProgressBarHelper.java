package com.codepunk.demo.support.widget;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.math.MathUtils;
import android.util.Log;
import android.widget.ProgressBar;

import java.util.Locale;

import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class AppCompatProgressBarHelper {
    //region Nested classes
    private static class BaseAppCompatProgressBarHelper
            extends AppCompatProgressBarHelper {
        @NonNull protected final ProgressBar mOwner;

        private boolean mMaxInitialized;
        private boolean mMinInitialized;
        private int mMin;

        private BaseAppCompatProgressBarHelper(@NonNull ProgressBar owner) {
            mOwner = owner;
        }

        @Override
        public int getMax() {
            return mOwner.getMax() + mMin;
        }

        @Override
        public int getMin() {
            return mMin;
        }

        @Override
        public int getProgress() {
            return mOwner.getProgress() + mMin;
        }

        @Override
        public int getSecondaryProgress() {
            return mOwner.getSecondaryProgress() + mMin;
        }

        @Override
        public void setMax(int max) {
            if (mMinInitialized) {
                if (max < mMin) {
                    max = mMin;
                }
            }
            mMaxInitialized = true;
            final int currentMax = getMax();
            if (mMinInitialized && max != currentMax) {
                mOwner.setMax(max - mMin);
            } else {
                mOwner.setMax(max);
            }
        }

        @Override
        public void setMin(int min) {
            if (mMaxInitialized) {
                final int max = getMax();
                if (min > max) {
                    min = max;
                }
            }
            mMinInitialized = true;
            if (mMaxInitialized && min != mMin) {
                final int max = getMax();
                final int progress = getProgress();
                final int secondaryProgress = getSecondaryProgress();
                final int clampedProgress = MathUtils.clamp(progress, min, max);
                final int clampedSecondaryProgress = MathUtils.clamp(secondaryProgress, min, max);
                mMin = min;
                mOwner.setMax(max - min);
                mOwner.setProgress(clampedProgress - min);
                mOwner.setSecondaryProgress(clampedSecondaryProgress - min);
            } else {
                mMin = min;
            }
        }

        @Override
        public void setProgress(int progress) {
            mOwner.setProgress(progress - mMin);
        }

        @Override
        public void setProgress(int progress, boolean animate) {
           setProgress(progress);
        }

        @Override
        public void setSecondaryProgress(int secondaryProgress) {
            mOwner.setSecondaryProgress(secondaryProgress - mMin);
        }
    }

    @TargetApi(N)
    private static final class NougatAppCompatProgressBarHelper
            extends BaseAppCompatProgressBarHelper {
        private NougatAppCompatProgressBarHelper(@NonNull ProgressBar owner) {
            super(owner);
        }

        @Override
        public void setProgress(int progress, boolean animate) {
            mOwner.setProgress(progress, animate);
        }
    }

    @TargetApi(O)
    private static final class OreoAppCompatProgressBarHelper
            extends AppCompatProgressBarHelper {
        @NonNull private final ProgressBar mOwner;

        private OreoAppCompatProgressBarHelper(@NonNull ProgressBar owner) {
            mOwner = owner;
        }

        @Override
        public int getMax() {
            return mOwner.getMax();
        }

        @Override
        public int getMin() {
            return mOwner.getMin();
        }

        @Override
        public int getProgress() {
            return mOwner.getProgress();
        }

        @Override
        public int getSecondaryProgress() {
            return mOwner.getSecondaryProgress();
        }

        @Override
        public void setMax(int max) {
            mOwner.setMax(max);
        }

        @Override
        public void setMin(int min) {
            mOwner.setMin(min);
        }

        @Override
        public void setProgress(int progress) {
            mOwner.setProgress(progress);
        }

        @Override
        public void setProgress(int progress, boolean animate) {
            mOwner.setProgress(progress, animate);
        }

        @Override
        public void setSecondaryProgress(int secondaryProgress) {
            mOwner.setSecondaryProgress(secondaryProgress);
        }
    }
    //endregion Nested classes

    //region Methods
    public abstract int getMax();
    public abstract int getMin();
    public abstract int getProgress();
    public abstract int getSecondaryProgress();
    public abstract void setMax(int max);
    public abstract void setMin(int min);
    public abstract void setProgress(int progress);
    public abstract void setProgress(int progress, boolean animate);
    public abstract void setSecondaryProgress(int secondaryProgress);
    //endregion

    //region Private methods
    static AppCompatProgressBarHelper newInstance(@NonNull ProgressBar owner) {
        // NOTE: Oreo SeekBar seems to have an issue when min != 0. So for now we'll
        // use our own, pre-Oreo implementation.
        /*
        if (Build.VERSION.SDK_INT >= O) {
            return new OreoAppCompatProgressBarHelper(owner);
        } else */
        if (Build.VERSION.SDK_INT >= N) {
            return new NougatAppCompatProgressBarHelper(owner);
        } else {
            return new BaseAppCompatProgressBarHelper(owner);
        }
    }
    //endregion Private methods
}
