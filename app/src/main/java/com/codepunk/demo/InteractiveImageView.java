package com.codepunk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;

import static android.graphics.Matrix.MSCALE_X;
import static android.graphics.Matrix.MSCALE_Y;
import static android.widget.ImageView.ScaleType.MATRIX;

// TODO NEXT Save a local ScaleType

public class InteractiveImageView extends AppCompatImageView {

    private final static String TAG = "tag_" + InteractiveImageView.class.getSimpleName();

    private ScaleType mScaleType;

    private final RectF mRectF = new RectF();
    private final float[] mValues = new float[9];
    private final float[] mSrcPoints = new float[2];
    private final float[] mDstPoints = new float[2];
    private final Matrix mInverseMatrix = new Matrix();
    private final Object mLock = new Object();

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

    @Override
    public void setScaleType(ScaleType scaleType) {
        mScaleType = scaleType;
        super.setScaleType(scaleType);
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

    public boolean getImagePointInCenter(@NonNull PointF outPoint) {
        final Drawable d = getDrawable();
        if (d == null) {
            return false;
        }
        final int intrinsicWidth = d.getIntrinsicWidth();
        final int intrinsicHeight = d.getIntrinsicHeight();
        if (intrinsicWidth < 0 || intrinsicHeight < 0) {
            return false;
        }
        synchronized (mLock) {
            mRectF.set(0, 0, intrinsicWidth, intrinsicHeight);
            final Matrix matrix = getImageMatrix();
            matrix.mapRect(mRectF);
            final int availableWidth = getAvailableWidth();
            final int availableHeight = getAvailableHeight();
            matrix.invert(mInverseMatrix);
            mSrcPoints[0] = availableWidth / 2.0f;
            mSrcPoints[1] = availableHeight / 2.0f;
            mInverseMatrix.mapPoints(mDstPoints, mSrcPoints);
            outPoint.x = (mScaleType == MATRIX || Math.round(mRectF.width()) > availableWidth ?
                    mDstPoints[0] / intrinsicWidth :
                    0.5f);
            outPoint.y = (mScaleType == MATRIX || Math.round(mRectF.height()) > availableHeight ?
                    mDstPoints[1] / intrinsicHeight :
                    0.5f);
            return true;
        }
    }

    public boolean getScale(PointF outPoint) {
        final Drawable d = getDrawable();
        if (d != null) {
            synchronized (mLock) {
                final int intrinsicWidth = d.getIntrinsicWidth();
                final int intrinsicHeight = d.getIntrinsicHeight();
                if (intrinsicWidth >= 0 && intrinsicHeight >= 0) {
                    getImageMatrix().getValues(mValues);
                    outPoint.x = mValues[MSCALE_X];
                    outPoint.y = mValues[MSCALE_Y];
                    return true;
                }
            }
        }
        return false;
    }

    public void setOnDrawListener(OnDrawListener onDrawListener) {
        mOnDrawListener = onDrawListener;
    }

    private int getAvailableHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private int getAvailableWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }
}
