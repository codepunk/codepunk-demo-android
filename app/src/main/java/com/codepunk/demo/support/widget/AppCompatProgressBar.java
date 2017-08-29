package com.codepunk.demo.support.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.codepunk.demo.R;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

@SuppressWarnings("unused")
public class AppCompatProgressBar extends ProgressBar {
    //region Fields
    private final AppCompatProgressBarHelper mAppCompatProgressBarHelper;
    //endregion Fields

    //region Constructors
    public AppCompatProgressBar(Context context) {
        super(context);
        mAppCompatProgressBarHelper = AppCompatProgressBarHelper.newInstance(this);
        initializeAppCompatProgressBar(
                context,
                null,
                R.attr.appCompatProgressBarStyle,
                0);
    }

    public AppCompatProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAppCompatProgressBarHelper = AppCompatProgressBarHelper.newInstance(this);
        initializeAppCompatProgressBar(
                context,
                attrs,
                R.attr.appCompatProgressBarStyle,
                0);
    }

    public AppCompatProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mAppCompatProgressBarHelper = AppCompatProgressBarHelper.newInstance(this);
        initializeAppCompatProgressBar(
                context,
                attrs,
                defStyleAttr,
                0);
    }

    @TargetApi(LOLLIPOP)
    public AppCompatProgressBar(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mAppCompatProgressBarHelper = AppCompatProgressBarHelper.newInstance(this);
        initializeAppCompatProgressBar(
                context,
                attrs,
                defStyleAttr,
                defStyleRes);
    }
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
    private void initializeAppCompatProgressBar(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.AppCompatProgressBar, defStyleAttr, defStyleRes);
        final int max = a.getInt(R.styleable.AppCompatProgressBar_supportMax, 0);
        final int min = a.getInt(R.styleable.AppCompatProgressBar_supportMin, 0);
        final int progress = a.getInt(R.styleable.AppCompatProgressBar_supportProgress, 0);
        final int secondaryProgress =
                a.getInt(R.styleable.AppCompatProgressBar_supportSecondaryProgress, 0);
        setSupportMin(min);
        setSupportMax(max);
        setSupportProgress(progress);
        setSupportSecondaryProgress(secondaryProgress);
        a.recycle();
    }
    //endregion Private methods
}
