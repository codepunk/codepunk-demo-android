package com.codepunk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.math.MathUtils;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ProgressBar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings({"unused"})
public class AppCompatSeekBarEx extends AppCompatSeekBar {
    //region Nested classes
    private static abstract class AppCompatSeekBarExImpl {
        void setProgressInternal(
                Class<?>[] parameterTypes,
                AppCompatSeekBarEx seekBar,
                Object... objects) {
            try {
                Method method = ProgressBar.class.getDeclaredMethod(
                        "setProgressInternal",
                        parameterTypes);
                if (method != null) {
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    method.invoke(seekBar, objects);
                }
            } catch (NoSuchMethodException e) {
                Log.w(TAG, "NoSuchMethodException encountered calling setProgressInternal", e);
            } catch (IllegalAccessException e) {
                Log.w(TAG, "IllegalAccessException encountered calling setProgressInternal", e);
            } catch (InvocationTargetException e) {
                Log.w(TAG, "InvocationTargetException encountered calling setProgressInternal", e);
            }
        }

        protected abstract void setProgressInternal(
                AppCompatSeekBarEx seekBar,
                int progress,
                boolean fromUser,
                boolean animate);
    }

    private static class BaseAppCompatSeekBarExImpl extends AppCompatSeekBarExImpl {
        private static final Class<?>[] SET_PROGRESS_INTERNAL_PARAMETER_TYPES =
                new Class[] {int.class, boolean.class};
        @Override
        protected void setProgressInternal(
                AppCompatSeekBarEx seekBar,
                int progress,
                boolean fromUser,
                boolean animate) {
            setProgressInternal(
                    SET_PROGRESS_INTERNAL_PARAMETER_TYPES,
                    seekBar,
                    progress,
                    fromUser);
        }
    }

    private static class NougatAppCompatSeekBarExImpl extends AppCompatSeekBarExImpl {
        private static final Class<?>[] SET_PROGRESS_INTERNAL_PARAMETER_TYPES =
                new Class[] {int.class, boolean.class, boolean.class};
        @Override
        protected void setProgressInternal(
                AppCompatSeekBarEx seekBar,
                int progress,
                boolean fromUser,
                boolean animate) {
            setProgressInternal(
                    SET_PROGRESS_INTERNAL_PARAMETER_TYPES,
                    seekBar,
                    progress,
                    fromUser,
                    animate);
        }
    }
    //endregion Nested classes

    //region Constants
    private static final String TAG = AppCompatSeekBarEx.class.getSimpleName();

    private static final AppCompatSeekBarExImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            IMPL = new NougatAppCompatSeekBarExImpl();
        } else {
            IMPL = new BaseAppCompatSeekBarExImpl();
        }
    }
    //endregion Constants

    //region Fields
    private int mInnerMin = Integer.MIN_VALUE;
    private int mInnerMax = Integer.MAX_VALUE;
    //endregion Fields

    //region Constructors
    public AppCompatSeekBarEx(Context context) {
        super(context);
    }

    public AppCompatSeekBarEx(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AppCompatSeekBarEx(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    //endregion Constructors

    //region Inherited methods
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final boolean retVal = super.onTouchEvent(event);
        if (retVal) {
            clampProgress(true, false);
        }
        return retVal;
    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);
        clampProgress(false, false);
    }

    @Override
    public void setProgress(int progress, boolean animate) {
        super.setProgress(progress, animate);
        clampProgress(false, animate);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect r = new Rect();
    }

    //endregion Inherited methods

    //region Methods
    private void clampProgress(boolean fromUser, boolean animate) {
        final int progress = getProgress();
        final int clampedProgress = MathUtils.clamp(progress, mInnerMin, mInnerMax);
        if (progress != clampedProgress) {
            IMPL.setProgressInternal(this, clampedProgress, fromUser, animate);
            invalidate();
        }
    }

    public int getInnerMin() {
        return mInnerMin;
    }

    public void setInnerMin(int innerMin) {
        mInnerMin = innerMin;
    }

    public int getInnerMax() {
        return mInnerMax;
    }

    public void setInnerMax(int innerMax) {
        mInnerMax = innerMax;
    }
    //region Methods
}
