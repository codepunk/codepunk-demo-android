package com.codepunk.demo;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;
import android.util.AttributeSet;

public class FloatSeekBarLayout extends AbsSeekBarLayout<Float> {
    /* TODO CLEAN
    protected static class SavedState extends BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };

        float maxValue;
        float minValue;
        float value;

        SavedState(Parcel source) {
            super(source);
            maxValue = source.readFloat();
            minValue = source.readFloat();
            value = source.readFloat();
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(maxValue);
            out.writeValue(minValue);
            out.writeValue(value);
        }
    }
    */

    public FloatSeekBarLayout(Context context) {
        super(context);
    }

    public FloatSeekBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatSeekBarLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /* TODO CLEAN
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mMaxValue = ss.maxValue;
        mMinValue = ss.minValue;
        mValue = ss.value;
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.maxValue = mMaxValue;
        ss.minValue = mMinValue;
        ss.value = mValue;
        return ss;
    }
    */

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
