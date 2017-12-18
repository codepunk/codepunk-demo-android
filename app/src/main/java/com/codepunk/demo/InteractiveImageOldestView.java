package com.codepunk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.codepunk.demo.support.DisplayCompat;

import static android.graphics.Matrix.MSCALE_X;
import static android.graphics.Matrix.MSCALE_Y;
import static android.widget.ImageView.ScaleType.FIT_XY;
import static android.widget.ImageView.ScaleType.MATRIX;

// TODO NEXT SavedState, restore relative center & scale on configuration change

@SuppressWarnings({"unused", "WeakerAccess"})
public class InteractiveImageOldestView extends AppCompatImageView {

    //region Nested classes
    public interface OnDrawListener {
        void onDraw(InteractiveImageOldestView view, Canvas canvas);
    }

    public interface ScalingStrategy {
        float getMaxScaleX();
        float getMaxScaleY();
        float getMinScaleX();
        float getMinScaleY();
        void invalidateMaxScale();
        void invalidateMinScale();
    }

    private class DefaultScalingStrategy implements ScalingStrategy {
        static final float BREADTH_MULTIPLIER = 3.0f;
        static final float LENGTH_MULTIPLIER = 5.0f;

        private final int mScreenBreadth;
        private final int mScreenLength;
        private final int mMaxBreadth;
        private final int mMaxLength;

        private final PointF mMaxScale = new PointF(1.0f, 1.0f);
        private final PointF mMinScale = new PointF(1.0f, 1.0f);
        private final RectF mSrcRectF = new RectF();
        private final RectF mDstRectF = new RectF();

        private boolean mMaxScaleDirty;
        private boolean mMinScaleDirty;

        private PointF mPendingRelativeCenter;
        private PointF mPendingScale;

        public DefaultScalingStrategy() {
            super();
            WindowManager manager =
                    (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            Point point = new Point();
            DisplayCompat.getRealSize(display, point);
            DisplayMetrics dm = new DisplayMetrics();
            DisplayCompat.getRealMetrics(display, dm);
            mScreenBreadth = Math.min(point.x, point.y);
            mScreenLength = Math.max(point.x, point.y);
            mMaxBreadth = Math.round(BREADTH_MULTIPLIER * mScreenBreadth);
            mMaxLength = Math.round(LENGTH_MULTIPLIER * mScreenLength);
        }

        @Override
        public float getMaxScaleX() {
            calcMaxScale();
            return mMaxScale.x;
        }

        @Override
        public float getMaxScaleY() {
            calcMaxScale();
            return mMaxScale.y;
        }

        @Override
        public float getMinScaleX() {
            calcMinScale();
            return mMinScale.x;
        }

        @Override
        public float getMinScaleY() {
            calcMinScale();
            return mMinScale.y;
        }

        @Override
        public void invalidateMaxScale() {
            mMaxScaleDirty = true;
        }

        @Override
        public void invalidateMinScale() {
            mMinScaleDirty = true;
        }

        private synchronized void calcMaxScale() {
            if (mMaxScaleDirty) {
                mMaxScaleDirty = false;
                if (getIntrinsicImageSize(mPoint)) {
                    getImageMatrix().getValues(mMatrixValues);
                    final int displayedWidth = Math.round(mPoint.x * mMatrixValues[MSCALE_X]);
                    final int displayedHeight = Math.round(mPoint.y * mMatrixValues[MSCALE_Y]);
                    final int displayedBreadth = Math.min(displayedWidth, displayedHeight);
                    final int displayedLength = Math.max(displayedWidth, displayedHeight);
                    final float screenBasedScale = Math.min(
                            (float) mMaxBreadth / displayedBreadth,
                            (float) mMaxLength / displayedLength);

                    final float viewBasedScale;
                    if (displayedWidth < displayedHeight) {
                        viewBasedScale = (float) getAvailableWidth() / displayedWidth;
                    } else if (displayedWidth > displayedHeight) {
                        viewBasedScale = (float) getAvailableHeight() / displayedHeight;
                    } else {
                        viewBasedScale =
                                (float) Math.min(getAvailableWidth(), getAvailableHeight()) /
                                displayedWidth;
                    }
                    final float scale = Math.max(screenBasedScale, viewBasedScale);
                    mMaxScale.set(scale * mMatrixValues[MSCALE_X], scale * mMatrixValues[MSCALE_Y]);
                } else {
                    mMaxScale.set(1.0f, 1.0f);
                }
            }
        }

        private synchronized void calcMinScale() {
            if (mMinScaleDirty) {
                mMinScaleDirty = false;
                if (getIntrinsicImageSize(mPoint)) {
                    getImageMatrix().getValues(mMatrixValues);
                    mSrcRect.set(
                            0,
                            0,
                            Math.round(mPoint.x * mMatrixValues[MSCALE_X]),
                            Math.round(mPoint.y * mMatrixValues[MSCALE_Y]));
                    mDstRect.set(0, 0, getAvailableWidth(), getAvailableHeight());
                    GraphicsUtils.scale(mSrcRect, mDstRect, mScaleType, mPointF);
                    mMinScale.set(
                            mPointF.x * mMatrixValues[MSCALE_X],
                            mPointF.y * mMatrixValues[MSCALE_Y]);
                } else {
                    mMinScale.set(1.0f, 1.0f);
                }
            }
        }
    }
    //endregion Nested classes

    //region Constants
    private static final String TAG = "tag_" + InteractiveImageOldestView.class.getSimpleName();

    private static final PointF INVALID = new PointF(Float.NaN, Float.NaN);
    //endregion Constants

    //region Fields
    private ScaleType mScaleType;

    private final PointF mCenter = new PointF(INVALID.x, INVALID.y);
    private final PointF mScale = new PointF(INVALID.x, INVALID.y);
    private boolean mPlacementDirty = false;

    private final Point mSize = new Point();
    private final Point mPoint = new Point();
    private final PointF mPointF = new PointF();
    private final RectF mRectF = new RectF();
    private final Rect mSrcRect = new Rect();
    private final Rect mDstRect = new Rect();
    private final float[] mMatrixValues = new float[9];
    private final float[] mSrcPoints = new float[2];
    private final float[] mDstPoints = new float[2];
    private final Matrix mInverseMatrix = new Matrix();
    private final Object mLock = new Object();

    private ScalingStrategy mScalingStrategy;

    private OnDrawListener mOnDrawListener;
    //endregion Fields

    //region Constructors
    public InteractiveImageOldestView(Context context) {
        super(context);
    }

    public InteractiveImageOldestView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InteractiveImageOldestView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    //endregion Constructors

    //region Inherited methods
    @Override
    public ScaleType getScaleType() {
        return mScaleType;
    }

    @Override
    public Matrix getImageMatrix() {
        final Matrix imageMatrix = super.getImageMatrix();
        if (super.getScaleType() == ScaleType.FIT_XY) {
            synchronized (this) {
                imageMatrix.reset();
                // TODO If no intrinsic size?
                getIntrinsicImageSize(mSize);
                mSrcRect.set(0, 0, mSize.x, mSize.y);
                mDstRect.set(0, 0, getAvailableWidth(), getAvailableHeight());
                GraphicsUtils.scale(mSrcRect, mDstRect, mScaleType, mPointF);
                imageMatrix.getValues(mMatrixValues);
                mMatrixValues[MSCALE_X] = mPointF.x;
                mMatrixValues[MSCALE_Y] = mPointF.y;
                imageMatrix.setValues(mMatrixValues);
            }
        }
        return imageMatrix;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mPlacementDirty) {
            getScale(null);
            getCenter(null);
            setPlacement(mScale.x, mScale.y, mCenter.x, mCenter.y);
            mPlacementDirty = false;
        }
        super.onDraw(canvas);
        if (mOnDrawListener != null) {
            mOnDrawListener.onDraw(this, canvas);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidateScalingStrategy();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
//        Log.i(TAG, "setImageDrawable");
        super.setImageDrawable(drawable);
        super.setScaleType(mScaleType);
        invalidatePlacement();
        invalidateScalingStrategy();
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
//        Log.i(TAG, "setImageMatrix");
        invalidatePlacement();
        setImageMatrix(matrix, true);
    }

    @Override
    public void setPadding(@Px int left, @Px int top, @Px int right, @Px int bottom) {
        super.setPadding(left, top, right, bottom);
        invalidateScalingStrategy();
    }

    @Override
    public void setPaddingRelative(@Px int start, @Px int top, @Px int end, @Px int bottom) {
        super.setPaddingRelative(start, top, end, bottom);
        invalidateScalingStrategy();
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        final ScaleType oldScaleType = super.getScaleType();
        super.setScaleType(scaleType);
        final boolean changed = (oldScaleType != super.getScaleType());
        if (changed) {
//            Log.i(TAG, "setScaleType: scaleType=" + scaleType);
            mScaleType = scaleType;
            invalidateScalingStrategy();
        }
    }
    //endregion Inherited methods

    //region Methods
    public synchronized boolean getCenter(PointF outPoint) {
        if (getIntrinsicImageSize(mSize)) {
            if (INVALID.equals(mCenter)) {
                if (ViewCompat.isLaidOut(this)) {
                    mRectF.set(0, 0, mSize.x, mSize.y);
                    final Matrix matrix = getImageMatrix();
                    matrix.mapRect(mRectF); // TODO What happens to mRectF when skewed or rotated?
                    final int availableWidth = getAvailableWidth();
                    final int availableHeight = getAvailableHeight();
                    matrix.invert(mInverseMatrix);
                    mSrcPoints[0] = availableWidth / 2.0f;
                    mSrcPoints[1] = availableHeight / 2.0f;
                    mInverseMatrix.mapPoints(mDstPoints, mSrcPoints);
                    // TODO Difference between ACTUAL center and REQUESTED center
                    // (and what did I mean by that?)
                    /*
                    mCenter.x = (mScaleType == MATRIX || Math.round(mRectF.width()) > availableWidth ?
                            mDstPoints[0] / intrinsicWidth :
                            0.5f);
                    mCenter.y = (mScaleType == MATRIX || Math.round(mRectF.height()) > availableHeight ?
                            mDstPoints[1] / intrinsicHeight :
                            0.5f);
                    */
                    mCenter.set(mDstPoints[0] / mSize.x, mDstPoints[1] / mSize.y);
                } else {
                    return false;
                }
            }

            if (outPoint != null) {
                outPoint.set(mCenter);
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean getDisplayedImageSize(Point outPoint) {
        getImageMatrix().getValues(mMatrixValues);
        return getImageSizeAtScale(mMatrixValues[MSCALE_X], mMatrixValues[MSCALE_Y], outPoint);
    }

    public boolean getDisplayedImageSize(PointF outPoint) {
        getImageMatrix().getValues(mMatrixValues);
        return getImageSizeAtScale(mMatrixValues[MSCALE_X], mMatrixValues[MSCALE_Y], outPoint);
    }

    public boolean getImageSizeAtScale(float scaleX, float scaleY, Point outPoint) {
        boolean retVal = true;
        final int width;
        final int height;
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            width = height = -1;
            retVal = false;
        } else {
            final int intrinsicWidth = drawable.getIntrinsicWidth();
            if (intrinsicWidth > 0) {
                width = Math.round(intrinsicWidth * scaleX);
            } else {
                width = -1;
                retVal = false;
            }
            final int intrinsicHeight = drawable.getIntrinsicHeight();
            if (intrinsicHeight > 0) {
                height = Math.round(intrinsicHeight * scaleY);
            } else {
                height = -1;
                retVal = false;
            }
        }
        if (outPoint != null) {
            outPoint.set(width, height);
        }
        return retVal;
    }

    public boolean getImageSizeAtScale(float scaleX, float scaleY, PointF outPoint) {
        boolean retVal = true;
        final float width;
        final float height;
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            width = height = -1.0f;
            retVal = false;
        } else {
            final int intrinsicWidth = drawable.getIntrinsicWidth();
            if (intrinsicWidth > 0) {
                width = intrinsicWidth * scaleX;
            } else {
                width = -1.0f;
                retVal = false;
            }
            final int intrinsicHeight = drawable.getIntrinsicHeight();
            if (intrinsicHeight > 0) {
                height = intrinsicHeight * scaleY;
            } else {
                height = -1.0f;
                retVal = false;
            }
        }
        if (outPoint != null) {
            outPoint.set(width, height);
        }
        return retVal;
    }

    public boolean getIntrinsicImageSize(Point outPoint) {
        return getImageSizeAtScale(1.0f, 1.0f, outPoint);
    }

    public float getMaxScaleX() {
        return getScalingStrategy().getMaxScaleX();
    }

    public float getMaxScaleY() {
        return getScalingStrategy().getMaxScaleY();
    }

    public float getMinScaleX() {
        return getScalingStrategy().getMinScaleX();
    }

    public float getMinScaleY() {
        return getScalingStrategy().getMinScaleY();
    }

    public synchronized boolean getScale(PointF outPoint) {
        if (imageHasIntrinsicSize()) {
            if (INVALID.equals(mScale)) {
                if (ViewCompat.isLaidOut(this)) {
                    getImageMatrix().getValues(mMatrixValues);
                    mScale.set(mMatrixValues[MSCALE_X], mMatrixValues[MSCALE_Y]);
                } else {
                    return false;
                }
            }

            if (outPoint != null) {
                outPoint.set(mScale);
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean hasCustomPlacement() {
        // TODO -- compare exact values
        return (mScaleType != super.getScaleType());
    }

    public void invalidateScalingStrategy() {
        final ScalingStrategy scalingStrategy = getScalingStrategy();
        scalingStrategy.invalidateMaxScale();
        scalingStrategy.invalidateMinScale();
    }

    public void setImageMatrix(Matrix matrix, boolean invalidate) {
        super.setImageMatrix(matrix);
        if (invalidate) {
            invalidateScalingStrategy();
        }
    }

    public void setOnDrawListener(OnDrawListener onDrawListener) {
        mOnDrawListener = onDrawListener;
    }

    public synchronized boolean setCenter(float centerX, float centerY) {
        return setScale(mScale.x, mScale.y, centerX, centerY);
    }

    public synchronized boolean setScale(float scaleX, float scaleY) {
        return setScale(scaleX, scaleY, mCenter.x, mCenter.y);
    }

    public synchronized boolean setScale(float scaleX, float scaleY, float centerX, float centerY) {
        if (imageHasIntrinsicSize()) {
            mCenter.set(centerX, centerY);
            mScale.set(scaleX, scaleY);
            mPlacementDirty = true;
            postInvalidate();
            return true;
        } else {
            return false;
        }
    }

    public void setScalingStrategy(ScalingStrategy scalingStrategy) {
        mScalingStrategy = scalingStrategy;
    }
    //endregion Methods

    //region Protected methods
    protected void invalidatePlacement() {
        mCenter.set(INVALID);
        mScale.set(INVALID);
    }
    //endregion Protected methods

    //region Private methods
    /*
    private synchronized boolean calculateCenter(PointF outPoint) {
        if (!getIntrinsicImageSize(mSize)) {
            return false;
        }

        mRectF.set(0, 0, mSize.x, mSize.y);
        final Matrix matrix = getImageMatrix();
        matrix.mapRect(mRectF); // TODO What happens to mRectF when skewed or rotated?
        final int availableWidth = getAvailableWidth();
        final int availableHeight = getAvailableHeight();
        matrix.invert(mInverseMatrix);
        mSrcPoints[0] = availableWidth / 2.0f;
        mSrcPoints[1] = availableHeight / 2.0f;
        mInverseMatrix.mapPoints(mDstPoints, mSrcPoints);
        // TODO Difference between ACTUAL center and REQUESTED center
//        outPoint.x = (mScaleType == MATRIX || Math.round(mRectF.width()) > availableWidth ?
//                mDstPoints[0] / intrinsicWidth :
//                0.5f);
//        outPoint.y = (mScaleType == MATRIX || Math.round(mRectF.height()) > availableHeight ?
//                mDstPoints[1] / intrinsicHeight :
//                0.5f);
        if (outPoint != null) {
            outPoint.set(mDstPoints[0] / mSize.x, mDstPoints[1] / mSize.y);
        }
        return true;
    }
    */

    private boolean getScaleInternal(PointF outPoint) {
        return false;
    }

    private ScalingStrategy getScalingStrategy() {
        if (mScalingStrategy == null) {
            mScalingStrategy = new DefaultScalingStrategy();
        }
        return mScalingStrategy;
    }

    private int getAvailableHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private int getAvailableWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private boolean imageHasIntrinsicSize() {
        return getIntrinsicImageSize(null);
    }

    private synchronized boolean setPlacement(float scaleX, float scaleY, float centerX, float centerY) {
        if (getIntrinsicImageSize(mSize)) {
            if (super.getScaleType() != MATRIX) {
                setScaleTypeInternal(MATRIX);
            }
            final Matrix matrix = getImageMatrix();
            matrix.getValues(mMatrixValues); // TODO Can I do this without getting values?

            matrix.preScale(
                    scaleX / mMatrixValues[MSCALE_X],
                    scaleY / mMatrixValues[MSCALE_Y],
                    mSize.x * centerX,
                    mSize.y * centerY);

            /*
            mMatrixValues[MSCALE_X] = mMatrixValues[MSCALE_Y] = 3.0f;
            mMatrixValues[MTRANS_X] = mMatrixValues[MTRANS_Y] = 10;
            matrix.setValues(mMatrixValues);
            */

            // Log.d(TAG, String.format(Locale.US, "mSize=%s, scaleX=%.2f, scaleY=%.2f, centerX=%.2f, centerY=%.2f, matrix=%s", mSize, scaleX, scaleY, centerX, centerY, matrix));


            matrix.reset();




            super.setImageMatrix(matrix);
            //postInvalidate();
            return true;
        } else {
            return false;
        }
    }

    /*
    // TODO Maybe replace setRelativeCenterInternal and setScaleInternal with
    // a single method called setPlacement?
    private boolean setRelativeCenterInternal(float centerX, float centerY) {
        // TODO !!! ...
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
            if (mCenter == null) {
                mCenter = new PointF(centerX, centerY);
            } else {
                mCenter.set(centerX, centerY);
            }

            if (super.getScaleType() != MATRIX) {
                setScaleTypeInternal(MATRIX);
            }

            final Matrix matrix = getImageMatrix();
            mRectF.set(0, 0, intrinsicWidth, intrinsicHeight);
            matrix.mapRect(mRectF);
            mSrcPoints[0] = intrinsicWidth * centerX;
            mSrcPoints[1] = intrinsicHeight * centerY;
            matrix.mapPoints(mDstPoints, mSrcPoints);
            final float deltaX = getAvailableWidth() / 2.0f - mDstPoints[0];
            final float deltaY = getAvailableHeight() / 2.0f - mDstPoints[1];
            matrix.postTranslate(deltaX, deltaY);
            super.setImageMatrix(matrix);
            postInvalidate();
            return true;
        }
    }

    private boolean setScaleInternal(float scaleX, float scaleY, float relativeX, float relativeY) {
        // TODO !!! ...
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
                setScaleTypeInternal(MATRIX);
            }
            final Matrix matrix = getImageMatrix();
            matrix.getValues(mMatrixValues); // TODO Can I do this without getting values?

            matrix.preScale(
                    scaleX / mMatrixValues[MSCALE_X],
                    scaleY / mMatrixValues[MSCALE_Y],
                    intrinsicWidth * relativeX,
                    intrinsicHeight * relativeY);

            super.setImageMatrix(matrix);
            postInvalidate();
            return true;
        }
    }
    */

    private void setScaleTypeInternal(ScaleType scaleType) {
        if (super.getScaleType() == FIT_XY && scaleType == MATRIX) {
            final Matrix matrix = getImageMatrix();
            super.setScaleType(scaleType);
            setImageMatrix(matrix);
        } else {
            super.setScaleType(scaleType);
        }
    }
    //endregion Private methods
}
