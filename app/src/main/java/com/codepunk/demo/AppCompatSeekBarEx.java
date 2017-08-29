package com.codepunk.demo;

import android.content.Context;
import android.support.v4.math.MathUtils;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ProgressBar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings({"SpellCheckingInspection", "TryWithIdenticalCatches", "unused"})
public class AppCompatSeekBarEx extends AppCompatSeekBar {
    private static final String TAG = AppCompatSeekBarEx.class.getSimpleName();

    private int mInnerMin = Integer.MIN_VALUE;
    private int mInnerMax = Integer.MAX_VALUE;

    public AppCompatSeekBarEx(Context context) {
        super(context);
    }

    public AppCompatSeekBarEx(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AppCompatSeekBarEx(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

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

    private void callSetProgressInternal(int progress, boolean fromUser, boolean animate) {
        try {
            Method method = ProgressBar.class.getDeclaredMethod(
                    "refreshProgress",
                    int.class,
                    boolean.class,
                    boolean.class);
            if (method != null) {
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                method.invoke(this, progress, fromUser, animate);
            }
        } catch (NoSuchMethodException e) {
            // NOOP
        } catch (IllegalAccessException e) {
            // NOOP
        } catch (InvocationTargetException e) {
            // NOOP
        }
    }

    private void clampProgress(boolean fromUser, boolean animate) {
        final int progress = getProgress();
        final int clampedProgress = MathUtils.clamp(progress, mInnerMin, mInnerMax);
        if (progress != clampedProgress) {
            callSetProgressInternal(clampedProgress, fromUser, animate);
        }
    }
}
