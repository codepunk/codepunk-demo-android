package com.codepunk.demo.support;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.codepunk.demo.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static android.os.Build.VERSION_CODES.BASE;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O;

public class AppCompatProgressBarVersionOne extends ProgressBar {
    //region Nested classes
    private interface AppCompatProgressBarImpl {
        int getMax();
        int getMin();
        int getProgress();
        int getSecondaryProgress();
        void setMax(int max);
        void setMin(int min);
        void setProgress(int progress);
        void setProgress(int progress, boolean animate);
        void setSecondaryProgress(int secondaryProgress);
    }

    @TargetApi(BASE)
    private class BaseAppCompatProgressBarImpl implements AppCompatProgressBarImpl {
        int mMax;
        int mMin;
        int mProgress;
        boolean mMaxInitialized;
        boolean mMinInitialized;
        boolean mMaxSet;

        @Override
        public int getMax() {
            return mMax;
        }

        @Override
        public int getMin() {
            return mMin;
        }

        @Override
        public int getProgress() {
            return AppCompatProgressBarVersionOne.super.getProgress() + mMin;
        }

        @Override
        public int getSecondaryProgress() {
            return AppCompatProgressBarVersionOne.super.getSecondaryProgress() + mMin;
        }

        @Override
        public void setMax(int max) {
            if (mMinInitialized) {
                if (max < mMin) {
                    max = mMin;
                }
            }
            mMaxInitialized = true;
            if (mMinInitialized && max != getMax()) {
                mMax = max;
                setMaxInternal(max - mMin);
                postInvalidate();

                if (mProgress > max) {
                    mProgress = max;
                }
                refreshProgress(android.R.id.progress, mProgress - mMin, false, false);
            } else {
                mMax = max;
            }
        }

        @Override
        public void setMin(int min) {
            if (mMaxInitialized) {
                if (min > mMax) {
                    min = mMax;
                }
            }
            mMinInitialized = true;
            if (mMaxInitialized && min != mMin) {
                mMin = min;
                if (!mMaxSet) {
                    setMaxInternal(mMax - min);
                }
                postInvalidate();

                if (mProgress < min) {
                    mProgress = min;
                }
                refreshProgress(android.R.id.progress, mProgress - min, false, false);
            } else {
                mMin = min;
            }
        }

        @Override
        public void setProgress(int progress) {
            AppCompatProgressBarVersionOne.super.setProgress(progress - mMin);
        }

        @Override
        public void setProgress(int progress, boolean animate) {
            // Should not be reachable
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSecondaryProgress(int secondaryProgress) {
            AppCompatProgressBarVersionOne.super.setSecondaryProgress(secondaryProgress - mMin);
        }

        protected void refreshProgress(int id, int progress, boolean fromUser, boolean animate) {
            try {
                final Method method =
                        ProgressBar.class.getDeclaredMethod(
                                "refreshProgress",
                                int.class,
                                int.class,
                                boolean.class);
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                method.invoke(AppCompatProgressBarVersionOne.this, id, progress, fromUser);
            } catch (Exception e) {
                // NOOP
            }
        }

        private void setMaxInternal(int max) {
            try {
                final Field field = ProgressBar.class.getDeclaredField("mMax");
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                field.setInt(AppCompatProgressBarVersionOne.this, max);
                mMaxSet = true;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @TargetApi(N)
    private class NougatAppCompatProgressBarImpl extends BaseAppCompatProgressBarImpl {
        @Override
        public void setProgress(int progress, boolean animate) {
            AppCompatProgressBarVersionOne.super.setProgress(progress - mMin, animate);
        }

        @Override
        protected void refreshProgress(int id, int progress, boolean fromUser, boolean animate) {
            try {
                final Method method =
                        ProgressBar.class.getDeclaredMethod(
                                "refreshProgress",
                                int.class,
                                int.class,
                                boolean.class,
                                boolean.class);
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                method.invoke(AppCompatProgressBarVersionOne.this, id, progress, fromUser, animate);
            } catch (Exception e) {
                // NOOP
            }
        }
    }

    @TargetApi(O)
    private class OreoAppCompatProgressBarImpl implements AppCompatProgressBarImpl {
        @Override
        public int getMax() {
            return AppCompatProgressBarVersionOne.super.getMax();
        }

        @Override
        public int getMin() {
            return AppCompatProgressBarVersionOne.super.getMin();
        }

        @Override
        public int getProgress() {
            return AppCompatProgressBarVersionOne.super.getProgress();
        }

        @Override
        public int getSecondaryProgress() {
            return AppCompatProgressBarVersionOne.super.getSecondaryProgress();
        }

        @Override
        public void setMax(int max) {
            AppCompatProgressBarVersionOne.super.setMax(max);
        }

        @Override
        public void setMin(int min) {
            AppCompatProgressBarVersionOne.super.setMin(min);
        }

        @Override
        public void setProgress(int progress) {
            AppCompatProgressBarVersionOne.super.setProgress(progress);
        }

        @Override
        public void setProgress(int progress, boolean animate) {
            AppCompatProgressBarVersionOne.super.setProgress(progress, animate);
        }

        @Override
        public void setSecondaryProgress(int secondaryProgress) {
            AppCompatProgressBarVersionOne.super.setSecondaryProgress(secondaryProgress);
        }
    }
    //endregion Nested classes

    //region Fields
    private AppCompatProgressBarImpl mImpl;
    //endregion Fields

    //region Constructors
    public AppCompatProgressBarVersionOne(Context context) {
        super(context);
        initializeAppCompatProgressBar(context, null, R.attr.appCompatProgressBarStyle, 0);
    }

    public AppCompatProgressBarVersionOne(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeAppCompatProgressBar(context, attrs, R.attr.appCompatProgressBarStyle, 0);
    }

    public AppCompatProgressBarVersionOne(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeAppCompatProgressBar(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(LOLLIPOP)
    public AppCompatProgressBarVersionOne(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initializeAppCompatProgressBar(context, attrs, defStyleAttr, defStyleRes);
    }
    //endregion Constructors

    //region Inherited methods
    @Override
    public synchronized int getMax() {
        return isO() ? super.getMax() : getImpl().getMax();
    }

    /*
    @Override
    public synchronized int getMin() {
        return isO() ? super.getMin() : getImpl().getMin();
    }
    */

    @Override
    public synchronized int getProgress() {
        return isO() ? super.getProgress() : getImpl().getProgress();
    }

    @Override
    public synchronized int getSecondaryProgress() {
        return isO() ? super.getSecondaryProgress() : getImpl().getSecondaryProgress();
    }

    @Override
    public synchronized void setMax(int max) {
        if (isO()) {
            super.setMax(max);
        } else {
            getImpl().setMax(max);
        }
    }

    /*
    @Override
    public synchronized void setMin(int min) {
        if (isO()) {
            super.setMin(min);
        } else {
            getImpl().setMin(min);
        }
    }
    */

    @Override
    public synchronized void setProgress(int progress) {
        if (isO()) {
            super.setProgress(progress);
        } else {
            getImpl().setProgress(progress);
        }
    }

    @Override
    public void setProgress(int progress, boolean animate) {
        if (isO()) {
            super.setProgress(progress, animate);
        } else {
            getImpl().setProgress(progress, animate);
        }
    }

    @Override
    public synchronized void setSecondaryProgress(int secondaryProgress) {
        if (isO()) {
            super.setSecondaryProgress(secondaryProgress);
        } else {
            getImpl().setSecondaryProgress(secondaryProgress);
        }
    }
    //endregion Inherited methods

    //region Methods
    public synchronized int getSupportMin() {
        if (isO()) {
            return super.getMin();
        } else {
            return getImpl().getMin();
        }
    }

    public synchronized void setSupportMin(int min) {
        if (isO()) {
            super.setMin(min);
        } else {
            getImpl().setMin(min);
        }
    }
    //endregion Methods

    //region Private methods
    private AppCompatProgressBarImpl getImpl() {
        if (mImpl == null) {
            if (Build.VERSION.SDK_INT >= O) {
                mImpl = new OreoAppCompatProgressBarImpl();
            } else if (Build.VERSION.SDK_INT >= N) {
                mImpl = new NougatAppCompatProgressBarImpl();
            } else {
                mImpl = new BaseAppCompatProgressBarImpl();
            }
        }
        return mImpl;
    }

    private boolean isO() {
        return Build.VERSION.SDK_INT >= O;
    }

    private void initializeAppCompatProgressBar(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.AppCompatProgressBar, defStyleAttr, defStyleRes);
        final int min = a.getInt(R.styleable.AppCompatProgressBar_supportMin, getImpl().getMin());
        final int progress = 0; //a.getInt(R.styleable.AppCompatProgressBar_android_progress, min);
        final int secondaryProgress =
                0; //a.getInt(R.styleable.AppCompatProgressBar_android_secondaryProgress, min);
        setSupportMin(min);
        setProgress(progress);
        setSecondaryProgress(secondaryProgress);
        a.recycle();
    }
    //endregion Private methods
}
