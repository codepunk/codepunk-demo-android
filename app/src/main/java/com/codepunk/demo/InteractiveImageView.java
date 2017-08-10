package com.codepunk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import static android.graphics.Matrix.MSCALE_X;
import static android.graphics.Matrix.MSCALE_Y;

public class InteractiveImageView extends AppCompatImageView {
    private final static String TAG = "tag_" + InteractiveImageView.class.getSimpleName();

    private final RectF mRectF = new RectF();
    private final float[] mValues = new float[9];
    private final float[] mSrcPoints = new float[2];
    private final float[] mDstPoints = new float[2];
    private final Matrix mInverseMatrix = new Matrix();

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

    public boolean getRelativeCenter(PointF outPoint) {
        final Drawable d = getDrawable();
        if (d != null) {
            final int intrinsicWidth = d.getIntrinsicWidth();
            final int intrinsicHeight = d.getIntrinsicHeight();
            if (intrinsicWidth >= 0 && intrinsicHeight >= 0) {
                mRectF.set(0, 0, intrinsicWidth, intrinsicHeight);
                final Matrix matrix = getImageMatrix();
                matrix.mapRect(mRectF);
                matrix.invert(mInverseMatrix);

                mSrcPoints[0] = (getWidth() - getPaddingLeft() - getPaddingRight()) / 2.0f;
                mSrcPoints[1] = (getHeight() - getPaddingTop()- getPaddingBottom()) / 2.0f;

                mInverseMatrix.mapPoints(mDstPoints, mSrcPoints);
                outPoint.x = mDstPoints[0] / intrinsicWidth;
                outPoint.y = mDstPoints[1] / intrinsicHeight;
                return true;

                // TODO: When anything but MATRIX, if actual size <= avail size, make it 0.5
                // Is that true for EVERYTHING but MATRIX?
            }
        }
        return false;
    }

    public boolean getScale(PointF outPoint) {
        final Drawable d = getDrawable();
        if (d != null) {
            final int intrinsicWidth = d.getIntrinsicWidth();
            final int intrinsicHeight = d.getIntrinsicHeight();
            if (intrinsicWidth >= 0 && intrinsicHeight >= 0) {
                getImageMatrix().getValues(mValues);
                outPoint.x = mValues[MSCALE_X];
                outPoint.y = mValues[MSCALE_Y];
                return true;
            }
        }
        return false;
    }

    public void setOnDrawListener(OnDrawListener onDrawListener) {
        mOnDrawListener = onDrawListener;
    }
}
