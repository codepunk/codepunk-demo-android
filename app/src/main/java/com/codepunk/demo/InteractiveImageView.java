package com.codepunk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class InteractiveImageView extends AppCompatImageView {
    public interface OnDrawListener {
        void onDraw(InteractiveImageView view, Canvas canvas);
    }

    private OnDrawListener mOnDrawListener;

    public InteractiveImageView(Context context) {
        super(context);
    }

    public InteractiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InteractiveImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mOnDrawListener != null) {
            mOnDrawListener.onDraw(this, canvas);
        }
    }

    public float getMaxScaleX() {
        return 4.0f; // TODO TEMP
    }

    public float getMaxScaleY() {
        return 4.0f; // TODO TEMP
    }

    public float getMinScaleX() {
        return 1.0f; // TODO TEMP
    }

    public float getMinScaleY() {
        return 1.0f; // TODO TEMP
    }

    public void setOnDrawListener(OnDrawListener onDrawListener) {
        mOnDrawListener = onDrawListener;
    }
}
