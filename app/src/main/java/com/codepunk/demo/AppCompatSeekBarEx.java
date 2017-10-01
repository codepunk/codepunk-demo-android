package com.codepunk.demo;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.math.MathUtils;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.widget.SeekBar;

@SuppressWarnings({"unused"})
public class AppCompatSeekBarEx extends AppCompatSeekBar implements SeekBar.OnSeekBarChangeListener {
    //region Fields
    private boolean mClampedMaxInitialized;
    private boolean mClampedMinInitialized;
    private int mClampedMax = Integer.MAX_VALUE;
    private int mClampedMin = Integer.MIN_VALUE;
    private OnSeekBarChangeListener mOnSeekBarChangeListener;
    //endregion Fields

    //region Constructors
    public AppCompatSeekBarEx(Context context) {
        super(context);
        initAppCompatSeekBarEx(context, null, 0);
    }

    public AppCompatSeekBarEx(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAppCompatSeekBarEx(context, attrs, 0);
    }

    public AppCompatSeekBarEx(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAppCompatSeekBarEx(context, attrs, defStyleAttr);
    }
    //endregion Constructors

    //region Inherited methods
    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        mOnSeekBarChangeListener = listener;
    }
    //endregion Inherited methods

    //region Interface methods
    @Override // SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        final int clampedProgress = MathUtils.clamp(progress, mClampedMin, mClampedMax);
        if (progress != clampedProgress) {
            seekBar.setProgress(clampedProgress);
        }

        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onProgressChanged(seekBar, progress, fromUser);
        }
    }

    @Override // SeekBar.OnSeekBarChangeListener
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStartTrackingTouch(seekBar);
        }
    }

    @Override // SeekBar.OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStopTrackingTouch(seekBar);
        }
    }
    //endregion Interface methods

    //region Methods
    public int getClampedMax() {
        return mClampedMax;
    }

    public void setClampedMax(int clampedMax) {
        if (mClampedMinInitialized) {
            if (clampedMax < mClampedMin) {
                clampedMax = mClampedMin;
            }
        }
        mClampedMaxInitialized = true;
        if (mClampedMinInitialized && clampedMax != mClampedMax) {
            mClampedMax = clampedMax;
            if (getProgress() > clampedMax) {
                setProgress(clampedMax);
            }
        } else {
            mClampedMax = clampedMax;
        }
    }

    public int getClampedMin() {
        return mClampedMin;
    }

    public void setClampedMin(int clampedMin) {
        if (mClampedMaxInitialized) {
            if (clampedMin > mClampedMax) {
                clampedMin = mClampedMax;
            }
        }
        mClampedMinInitialized = true;
        if (mClampedMaxInitialized && clampedMin != mClampedMin) {
            mClampedMin = clampedMin;
            if (getProgress() < clampedMin) {
                setProgress(clampedMin);
            }
        } else {
            mClampedMin = clampedMin;
        }
    }
    //endregion Methods

    //region Private methods
    private void initAppCompatSeekBarEx(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.AppCompatSeekBarEx,
                defStyleAttr,
                0);
        final int clampedMin =
                a.getInt(R.styleable.AppCompatProgressBarEx_clampedMin, Integer.MIN_VALUE);
        final int clampedMax =
                a.getInt(R.styleable.AppCompatProgressBarEx_clampedMax, Integer.MAX_VALUE);
        setClampedMin(clampedMin);
        setClampedMax(clampedMax);
        a.recycle();
        super.setOnSeekBarChangeListener(this);
    }
    //endregion Private methods
}
