package com.codepunk.demo.support;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.codepunk.demo.R;
import com.codepunk.demo.support.AppCompatProgressBarHelper.AppCompatProgressBarImpl;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.O;

public class AppCompatProgressBar extends ProgressBar {
    //region Fields
    private AppCompatProgressBarImpl mImpl;
    //endregion Fields

    //region Constructors
    public AppCompatProgressBar(Context context) {
        super(context);
        initializeAppCompatProgressBar(context, null, R.attr.appCompatProgressBarStyle, 0);
    }

    public AppCompatProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeAppCompatProgressBar(context, attrs, R.attr.appCompatProgressBarStyle, 0);
    }

    public AppCompatProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeAppCompatProgressBar(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(LOLLIPOP)
    public AppCompatProgressBar(
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

    @Override
    public synchronized int getProgress() {
        return super.getProgress() + (isO() ? 0 : getImpl().getMin());
    }

    @Override
    public synchronized int getSecondaryProgress() {
        return super.getSecondaryProgress() + (isO() ? 0 : getImpl().getMin());
    }

    @Override
    public synchronized void setMax(int max) {
        if (isO()) {
            super.setMax(max);
        } else {
            getImpl().setMax(max);
        }
    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress - (isO() ? 0 : getImpl().getMin()));
    }

    @Override
    public void setProgress(int progress, boolean animate) {
        super.setProgress(progress - (isO() ? 0 : getImpl().getMin()), animate);
    }

    @Override
    public synchronized void setSecondaryProgress(int secondaryProgress) {
        super.setSecondaryProgress(secondaryProgress - (isO() ? 0 : getImpl().getMin()));
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
            mImpl = AppCompatProgressBarHelper.newInstance(this);
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
        final int progress = a.getInt(R.styleable.AppCompatProgressBar_android_progress, min);
        final int secondaryProgress =
                a.getInt(R.styleable.AppCompatProgressBar_android_secondaryProgress, min);
        setSupportMin(min);
        setProgress(progress);
        setSecondaryProgress(secondaryProgress);
        a.recycle();
    }
    //endregion Private methods
}
