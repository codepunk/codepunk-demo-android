package com.codepunk.demo.support.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.codepunk.demo.R;

@SuppressWarnings("unused")
public class AppCompatSeekBar extends android.support.v7.widget.AppCompatSeekBar {
    //region Fields
    private final AppCompatProgressBarHelper mAppCompatProgressBarHelper;
    //endregion Fields

    //region Constructors
    public AppCompatSeekBar(Context context) {
        super(context);
        mAppCompatProgressBarHelper = AppCompatProgressBarHelper.newInstance(this);
        initializeAppCompatSeekBar(
                context,
                null,
                R.attr.appCompatProgressBarStyle,
                0);
    }

    public AppCompatSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAppCompatProgressBarHelper = AppCompatProgressBarHelper.newInstance(this);
        initializeAppCompatSeekBar(
                context,
                attrs,
                R.attr.appCompatProgressBarStyle,
                0);
    }

    public AppCompatSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mAppCompatProgressBarHelper = AppCompatProgressBarHelper.newInstance(this);
        initializeAppCompatSeekBar(
                context,
                attrs,
                defStyleAttr,
                0);
    }

    /*
    @TargetApi(LOLLIPOP)
    public AppCompatSeekBar(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mAppCompatProgressBarHelper = AppCompatProgressBarHelper.newInstance(this);
        initializeAppCompatSeekBar(
                context,
                attrs,
                defStyleAttr,
                defStyleRes);
    }
    */
    //endregion Constructors

    //region Methods
    public synchronized int getSupportMax() {
        return mAppCompatProgressBarHelper.getMax();
    }

    public synchronized int getSupportMin() {
        return mAppCompatProgressBarHelper.getMin();
    }

    public synchronized int getSupportProgress() {
        return mAppCompatProgressBarHelper.getProgress();
    }

    public synchronized int getSupportSecondaryProgress() {
        return mAppCompatProgressBarHelper.getSecondaryProgress();
    }

    public synchronized void setSupportMax(int max) {
        mAppCompatProgressBarHelper.setMax(max);
    }

    public synchronized void setSupportMin(int min) {
        mAppCompatProgressBarHelper.setMin(min);
    }

    public synchronized void setSupportProgress(int progress) {
        mAppCompatProgressBarHelper.setProgress(progress);
    }

    public void setSupportProgress(int progress, boolean animate) {
        mAppCompatProgressBarHelper.setProgress(progress, animate);
    }

    public synchronized void setSupportSecondaryProgress(int secondaryProgress) {
        mAppCompatProgressBarHelper.setSecondaryProgress(secondaryProgress);
    }
    //endregion Methods

    //region Private methods
    private void initializeAppCompatSeekBar(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.AppCompatSeekBar, defStyleAttr, defStyleRes);
        final int min = a.getInt(R.styleable.AppCompatSeekBar_supportMin, 0);
        final int max = a.getInt(R.styleable.AppCompatSeekBar_supportMax, 0);
        final int progress = a.getInt(R.styleable.AppCompatSeekBar_supportProgress, 0);
        final int secondaryProgress =
                a.getInt(R.styleable.AppCompatSeekBar_supportSecondaryProgress, 0);
        setSupportMin(min);
        setSupportMax(max);
        setSupportProgress(progress);
        setSupportSecondaryProgress(secondaryProgress);
        a.recycle();
    }
    //endregion Private methods
}
