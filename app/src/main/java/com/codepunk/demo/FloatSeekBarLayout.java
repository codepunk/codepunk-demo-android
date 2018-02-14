package com.codepunk.demo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.math.MathUtils;
import android.util.AttributeSet;

public class FloatSeekBarLayout extends AbsSeekBarLayout<Float> {

    public FloatSeekBarLayout(Context context) {
        super(context);
    }

    public FloatSeekBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatSeekBarLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public Float progressToValue(int progress) {
        final int maxProgress = mSeekBar.getMax();
        if (maxProgress == 0) {
            return 0.0f;
        }
        return mMinValue + (float) progress / maxProgress * (mMaxValue - mMinValue);
    }

    @Override
    public int valueToProgress(@NonNull Float value) {
        if (mMinValue.equals(mMaxValue)) {
            return 0;
        }
        float clampedValue = MathUtils.clamp(value, mMinValue, mMaxValue);
        return (int) (mSeekBar.getMax() * (clampedValue - mMinValue) / (mMaxValue - mMinValue));
    }

    @Override
    protected Float getInitialValue() {
        return 0.0f;
    }
}
