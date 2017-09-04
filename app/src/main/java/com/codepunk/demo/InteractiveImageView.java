package com.codepunk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.Locale;

import static android.graphics.Matrix.MSCALE_X;
import static android.graphics.Matrix.MSCALE_Y;
import static android.widget.ImageView.ScaleType.MATRIX;

// TODO NEXT Figure out how to do "locked" scaling

public class InteractiveImageView extends AppCompatImageView {

    private final static String TAG = "tag_" + InteractiveImageView.class.getSimpleName();

    private ScaleType mScaleType;

    private final Point mPoint = new Point();
    private final PointF mPointF = new PointF();
    private final RectF mRectF = new RectF();
    private final float[] mMatrixValues = new float[9];
    private final float[] mSrcPoints = new float[2];
    private final float[] mDstPoints = new float[2];
    private final Matrix mInverseMatrix = new Matrix();
    private final Object mLock = new Object();

    private boolean mMaxScaleDirty = false;
    private float mMaxScale = 1.0f;

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
    public void setImageDrawable(@Nullable Drawable drawable) {
        invalidateMaxScale();
        super.setImageDrawable(drawable);
        super.setScaleType(mScaleType);
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        mScaleType = scaleType;
        super.setScaleType(scaleType);
    }

    public boolean getDisplayedImageSize(@NonNull Point outPoint) {
        getImageMatrix().getValues(mMatrixValues);
        return getImageSizeAtScale(mMatrixValues[MSCALE_X], mMatrixValues[MSCALE_Y], outPoint);
    }

    public boolean getImageSizeAtScale(float scaleX, float scaleY, @NonNull Point outPoint) {
        final Drawable drawable = getDrawable();
        if (drawable != null) {
            final int intrinsicWidth = drawable.getIntrinsicWidth();
            final int intrinsicHeight = drawable.getIntrinsicHeight();
            if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                outPoint.x = Math.round(intrinsicWidth * scaleX);
                outPoint.y = Math.round(intrinsicHeight * scaleY);
                return true;
            }
        }
        return false;
    }

    public boolean getIntrinsicImageSize(@NonNull Point outPoint) {
        return getImageSizeAtScale(1.0f, 1.0f, outPoint);
    }

    protected synchronized float getMaxScale() {
        // TODO Make this ScalingStrategy
        // TODO Think of meaningful variable names
        // TODO Clean this logic up
        if (mMaxScaleDirty) {
            final float MIN_MULTIPLIER = 3.0f;
            final float MAX_MULTIPLIER = 5.0f;
            mMaxScaleDirty = false;
            if (getIntrinsicImageSize(mPoint)) {
                getImageMatrix().getValues(mMatrixValues);
                final DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
                final float scale1 = MIN_MULTIPLIER *
                        (float) Math.min(dm.widthPixels, dm.heightPixels) /
                        Math.min(mPoint.x, mPoint.y);
                final float scale2 = MAX_MULTIPLIER *
                        (float) Math.max(dm.widthPixels, dm.heightPixels) /
                        Math.max(mPoint.x, mPoint.y);
                final int screenDim; // TODO Tie this to control size, not screen size?
                if ((mPoint.x == mPoint.y)) {
                    screenDim = Math.min(dm.widthPixels, dm.heightPixels);
                } else if ((mPoint.x < mPoint.y)) {
                    screenDim = dm.widthPixels;
                } else {
                    screenDim = dm.heightPixels;
                }
                final float scale3 = (float) screenDim /
                        Math.min(mPoint.x, mPoint.y);
                mMaxScale = Math.max(Math.min(scale1, scale2), scale3);
            } else {
                mMaxScale = 1.0f;
            }
            Log.d(TAG, String.format(Locale.US, "Calculating max scale... mMaxScale=%.2f", mMaxScale));
        }
        return mMaxScale;
    }

    public synchronized float getMaxScaleX() {
        return getMaxScale();
    }

    public synchronized float getMaxScaleY() {
        return getMaxScale();
    }

    public float getMinScaleX() {
        return 0.2f; // TODO TEMP
    }

    public float getMinScaleY() {
        return 0.2f; // TODO TEMP
    }

    public boolean getRelativeCenter(@NonNull PointF outPoint) {
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
            matrix.mapRect(mRectF); // TODO What happens to mRectF when skewed or rotated?
            final int availableWidth = getAvailableWidth();
            final int availableHeight = getAvailableHeight();
            matrix.invert(mInverseMatrix);
            mSrcPoints[0] = availableWidth / 2.0f;
            mSrcPoints[1] = availableHeight / 2.0f;
            mInverseMatrix.mapPoints(mDstPoints, mSrcPoints);
            /* TODO Difference between ACTUAL center and REQUESTED center */
            /*
            outPoint.x = (mScaleType == MATRIX || Math.round(mRectF.width()) > availableWidth ?
                    mDstPoints[0] / intrinsicWidth :
                    0.5f);
            outPoint.y = (mScaleType == MATRIX || Math.round(mRectF.height()) > availableHeight ?
                    mDstPoints[1] / intrinsicHeight :
                    0.5f);
            */
            outPoint.x = mDstPoints[0] / intrinsicWidth;
            outPoint.y = mDstPoints[1] / intrinsicHeight;
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
                    getImageMatrix().getValues(mMatrixValues);
                    outPoint.x = mMatrixValues[MSCALE_X];
                    outPoint.y = mMatrixValues[MSCALE_Y];
                    return true;
                }
            }
        }
        return false;
    }

    public void setOnDrawListener(OnDrawListener onDrawListener) {
        mOnDrawListener = onDrawListener;
    }

    public boolean setRelativeCenter(float centerX, float centerY) {
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
            if (super.getScaleType() != MATRIX) {
                super.setScaleType(MATRIX);
            }

            // TODO Doing weird things when scale is not "normal"
            // Changing Y changes X, etc. Maybe using the wrong

            mRectF.set(0, 0, intrinsicWidth, intrinsicHeight);
            final Matrix matrix = getImageMatrix();
            matrix.mapRect(mRectF);
            mSrcPoints[0] = intrinsicWidth * centerX;
            mSrcPoints[1] = intrinsicHeight * centerY;
            matrix.mapPoints(mDstPoints, mSrcPoints);
            final float deltaX = getAvailableWidth() / 2.0f - mDstPoints[0];
            final float deltaY = getAvailableHeight() / 2.0f - mDstPoints[1];
            matrix.postTranslate(deltaX, deltaY);
            setImageMatrix(matrix);
            postInvalidate();
            return true;
        }
    }

    public boolean setScale(float scaleX, float scaleY) {
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
            getRelativeCenter(mPointF);
            return setScale(scaleX, scaleY, mPointF.x, mPointF.y);
        }
    }

    public boolean setScale(float scaleX, float scaleY, float relativeX, float relativeY) {
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
            if (super.getScaleType() != MATRIX) {
                super.setScaleType(MATRIX);
            }
            final Matrix matrix = getImageMatrix();
            matrix.getValues(mMatrixValues); // TODO Can I do this without getting values?

            matrix.preScale(
                    scaleX / mMatrixValues[MSCALE_X],
                    scaleY / mMatrixValues[MSCALE_Y],
                    intrinsicWidth * relativeX,
                    intrinsicHeight * relativeY);

            setImageMatrix(matrix);
            postInvalidate();
            return true;
        }
    }

    protected void invalidateMaxScale() {
        mMaxScaleDirty = true;
    }

    private int getAvailableHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private int getAvailableWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }
}
