package com.codepunk.demo;

import android.content.Context;
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
    protected Float progressToValue(int progress, int maxProgress, Float minValue, Float maxValue) {
        return (maxProgress == 0 ?
                0.0f :
                minValue + (float) progress / maxProgress * (maxValue - minValue));
    }
}
