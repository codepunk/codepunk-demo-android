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
    protected Float progressToValue(
            int progress,
            int maxProgress,
            @NonNull Float minValue,
            @NonNull Float maxValue) {
        return (maxProgress == 0 ?
                0.0f :
                minValue + (float) progress / maxProgress * (maxValue - minValue));
    }

    @Override
    protected int valueToProgress(
            @NonNull Float value,
            @NonNull Float minValue,
            @NonNull Float maxValue,
            int maxProgress) {
        if (minValue.equals(maxValue)) {
            return 0;
        }
        float clampedValue = MathUtils.clamp(value, minValue, maxValue);
        return (int) (maxProgress * (clampedValue - minValue) / (maxValue - minValue));
    }
}
