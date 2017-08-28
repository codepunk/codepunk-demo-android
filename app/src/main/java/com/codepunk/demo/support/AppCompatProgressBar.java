package com.codepunk.demo.support;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.math.MathUtils;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.O;

public class AppCompatProgressBar extends ProgressBar {
    //region Nested classes
    private interface AppCompatProgressBarImpl {
        int getMin();
        int getProgress();
        int getSecondaryProgress();
        void setMax(int max);
        void setMin(int min);
        void setProgress(int progress);
        void setProgress(int progress, boolean animate);
        void setSecondaryProgress(int secondaryProgress);
    }

    private static class BaseAppCompatProgressBarImpl implements AppCompatProgressBarImpl {
        private final ProgressBar mOwner;
        private boolean mMaxInitialized;
        private boolean mMinInitialized;
        private int mMax;
        private int mMin;
        private int mProgress;
        private int mSecondaryProgress;

        public BaseAppCompatProgressBarImpl(ProgressBar owner) {
            super();
            mOwner = owner;
            mMin = 0;
            mMax = owner.getMax();
        }

        @Override
        public int getMin() {
            return mMin;
        }

        @Override
        public int getProgress() {
            return mOwner.isIndeterminate() ? 0 : mProgress;
        }

        @Override
        public int getSecondaryProgress() {
            return mOwner.isIndeterminate() ? 0 : mSecondaryProgress;
        }

        @Override
        public void setMax(int max) {

        }

        @Override
        public void setMin(int min) {

        }

        @Override
        public void setProgress(int progress) {
            resolveProgress(mMin, mMax, progress, false);
        }

        @Override
        public void setProgress(int progress, boolean animate) {
            resolveProgress(mMin, mMax, progress, animate);
        }

        @Override
        public void setSecondaryProgress(int secondaryProgress) {

        }

        private void setProgressInternal(int progress, boolean animate) {
            mOwner.setProgress(progress); // TODO animate in Android N
        }

        private void resolveProgress(
                int min,
                int max,
                int progress,
                boolean animate) {
            if (mOwner.isIndeterminate()) {
                return;
            }

            mProgress = MathUtils.clamp(progress, min, max);
            final int range = max - min;
            final int actualProgress =
                    (range == 0 ? 0 : (int) ((mProgress - min) / (float) range));
            setProgressInternal(actualProgress, animate);
        }
    }

    @TargetApi(O)
    private static class OAppCompatProgressBarImpl implements AppCompatProgressBarImpl {
        private final ProgressBar mOwner;

        public OAppCompatProgressBarImpl(ProgressBar owner) {
            super();
            mOwner = owner;
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

    private static class Initializer {
        final AppCompatProgressBarImpl mImpl;

        public Initializer(AppCompatProgressBarImpl impl) {
            super();
            mImpl = impl;
        }
    }
    //endregion Nested classes

    //region Fields
    private final AppCompatProgressBarImpl mImpl;
    //endregion Fields

    //region Constructors
    public AppCompatProgressBar(Context context) {
        super(context);
        final Initializer initializer = initializeAppCompatProgressBar();
        mImpl = initializer.mImpl;
    }

    public AppCompatProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        final Initializer initializer = initializeAppCompatProgressBar();
        mImpl = initializer.mImpl;
    }

    public AppCompatProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final Initializer initializer = initializeAppCompatProgressBar();
        mImpl = initializer.mImpl;
    }

    @TargetApi(LOLLIPOP)
    public AppCompatProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        final Initializer initializer = initializeAppCompatProgressBar();
        mImpl = initializer.mImpl;
    }
    //endregion Constructors

    //region Inherited methods
    @Override
    public synchronized int getMin() {
        return mImpl.getMin();
    }

    @Override
    public synchronized int getProgress() {
        return mImpl.getProgress();
    }

    @Override
    public synchronized int getSecondaryProgress() {
        return mImpl.getSecondaryProgress();
    }

    @Override
    public synchronized void setMax(int max) {
        mImpl.setMax(max);
    }

    @Override
    public synchronized void setMin(int min) {
        mImpl.setMin(min);
    }

    @Override
    public synchronized void setProgress(int progress) {
        mImpl.setProgress(progress);
    }

    @Override
    public void setProgress(int progress, boolean animate) {
        mImpl.setProgress(progress, animate);
    }

    @Override
    public synchronized void setSecondaryProgress(int secondaryProgress) {
        super.setSecondaryProgress(secondaryProgress);
    }
    //endregion Inherited methods

    //region Methods
    public synchronized int getSupportMin() {
        return mImpl.getMin();
    }

    public synchronized void setSupportMin(int min) {
        mImpl.setMin(min);
    }
    //endregion Methods

    //region Private methods
    private Initializer initializeAppCompatProgressBar() {
        final AppCompatProgressBarImpl impl;
        if (Build.VERSION.SDK_INT >= O) {
            impl = new OAppCompatProgressBarImpl(this);
        } else {
            impl = new BaseAppCompatProgressBarImpl(this);
        }
        return new Initializer(impl);
    }
    //endregion Private methods
}
