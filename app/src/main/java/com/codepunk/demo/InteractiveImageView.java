package com.codepunk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class InteractiveImageView extends AppCompatImageView {
    //region Nested classes
    public interface OnDrawListener {
        void onDraw(InteractiveImageView view, Canvas canvas);
    }
    //endregion Nested classes

    //region Fields
    private ScaleType mScaleType;
    private OnDrawListener mOnDrawListener;
    //endregion Fields

    //region Constructors
    public InteractiveImageView(Context context) {
        super(context);
    }

    public InteractiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InteractiveImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    //endregion Constructors

    //region Inherited methods
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mOnDrawListener != null) {
            mOnDrawListener.onDraw(this, canvas);
        }
    }
    //endregion Inherited methods

    //region methods
    public boolean getCenter(PointF outPoint) {
        // TODO
        return true;
    }

    public float getMaxScaleX() {
        // TODO
        return 5.0f;
    }

    public float getMaxScaleY() {
        // TODO
        return 5.0f;
    }

    public float getMinScaleX() {
        // TODO
        return 1.0f;
    }

    public float getMinScaleY() {
        // TODO
        return 1.0f;
    }

    public boolean getScale(PointF outPoint) {
        // TODO
        return true;
    }

    public boolean setCenter(float centerX, float centerY) {
        // TODO
        return true;
    }

    public boolean setPlacement(float centerX, float centerY, float scaleX, float scaleY) {
        // TODO
        return true;
    }

    public boolean setScale(float scaleX, float scaleY) {
        // TODO
        return true;
    }

    public boolean hasCustomPlacement() {
        // TODO -- compare exact values
        return (mScaleType != super.getScaleType());
    }

    public void setOnDrawListener(OnDrawListener onDrawListener) {
        mOnDrawListener = onDrawListener;
    }
    //endregion methods
}
