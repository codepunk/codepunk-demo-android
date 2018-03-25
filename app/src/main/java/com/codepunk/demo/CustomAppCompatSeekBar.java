package com.codepunk.demo;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.math.MathUtils;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.widget.SeekBar;

@SuppressWarnings({"unused"})
public class CustomAppCompatSeekBar extends AppCompatSeekBar
        implements SeekBar.OnSeekBarChangeListener {
    //region Fields
    private boolean mClampedMaxInitialized;
    private boolean mClampedMinInitialized;
    private int mClampedMax = Integer.MAX_VALUE;
    private int mClampedMin = Integer.MIN_VALUE;
    private OnSeekBarChangeListener mOnSeekBarChangeListener;
    //endregion Fields

    //region Constructors
    public CustomAppCompatSeekBar(Context context) {
        super(context);
        initCustomAppCompatSeekBar(context, null, 0);
    }

    public CustomAppCompatSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCustomAppCompatSeekBar(context, attrs, 0);
    }

    public CustomAppCompatSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initCustomAppCompatSeekBar(context, attrs, defStyleAttr);
    }
    //endregion Constructors

    //region Inherited methods
    @Override
    public boolean performClick() {
        return super.performClick();
    }

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

    public void setClampedMax(int max) {
        if (mClampedMinInitialized) {
            if (max < mClampedMin) {
                max = mClampedMin;
            }
        }
        mClampedMaxInitialized = true;
        if (mClampedMinInitialized && max != mClampedMax) {
            mClampedMax = max;
            if (getProgress() > max) {
                setProgress(max);
            }
        } else {
            mClampedMax = max;
        }
    }

    public int getClampedMin() {
        return mClampedMin;
    }

    public void setClampedMin(int min) {
        if (mClampedMaxInitialized) {
            if (min > mClampedMax) {
                min = mClampedMax;
            }
        }
        mClampedMinInitialized = true;
        if (mClampedMaxInitialized && min != mClampedMin) {
            mClampedMin = min;
            if (getProgress() < min) {
                setProgress(min);
            }
        } else {
            mClampedMin = min;
        }
    }
    //endregion Methods

    //region Private methods
    private void initCustomAppCompatSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.CustomAppCompatSeekBar,
                defStyleAttr,
                0);
        final int clampedMin =
                a.getInt(R.styleable.CustomAppCompatSeekBar_clampedMin, Integer.MIN_VALUE);
        final int clampedMax =
                a.getInt(R.styleable.CustomAppCompatSeekBar_clampedMax, Integer.MAX_VALUE);
        setClampedMin(clampedMin);
        setClampedMax(clampedMax);
        a.recycle();
        super.setOnSeekBarChangeListener(this);
    }
    //endregion Private methods
}
