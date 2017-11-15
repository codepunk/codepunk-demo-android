package com.codepunk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;
import android.support.v4.util.Pools.SimplePool;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.codepunk.demo.support.DisplayCompat;

import static android.graphics.Matrix.MSCALE_X;
import static android.graphics.Matrix.MSCALE_Y;

// TODO Next Differentiate between "applied" center/scale and actual/current center/scale
// TODO Next 2 observe limits when setting center/scale
// TODO Handle skew, perspective

public class InteractiveImageView extends AppCompatImageView {
    //region Nested classes
    public interface OnDrawListener {
        void onDraw(InteractiveImageView view, Canvas canvas);
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

        private boolean mMaxScaleDirty;
        private boolean mMinScaleDirty;

        public DefaultScalingStrategy() {
            super();
            final DisplayMetrics dm;
            WindowManager manager =
                    (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            if (manager == null) {
                dm = getContext().getResources().getDisplayMetrics();
            } else {
                dm = new DisplayMetrics();
                DisplayCompat.getRealMetrics(manager.getDefaultDisplay(), dm);
            }
            mScreenBreadth = Math.min(dm.widthPixels, dm.heightPixels);
            mScreenLength = Math.max(dm.widthPixels, dm.heightPixels);
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

                final RectF intrinsicRect = mRectFPool.acquire();
                if (getIntrinsicImageRect(intrinsicRect)) {
                    final Matrix baseImageMatrix = getBaseImageMatrix();
                    baseImageMatrix.getValues(mMatrixValues);

                    final int displayedWidth =
                            Math.round(intrinsicRect.width() * mMatrixValues[MSCALE_X]);
                    final int displayedHeight =
                            Math.round(intrinsicRect.height() * mMatrixValues[MSCALE_Y]);
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
                mRectFPool.release(intrinsicRect);
            }
        }

        private synchronized void calcMinScale() {
            if (mMinScaleDirty) {
                mMinScaleDirty = false;

                final Matrix baseImageMatrix = getBaseImageMatrix();
                baseImageMatrix.getValues(mMatrixValues);
                mMinScale.set(mMatrixValues[MSCALE_X], mMatrixValues[MSCALE_Y]);

                // TODO Make sure it's ok I'm not checking if imageHasIntrinsicSize here
            }
        }
    }

    private class RectFPool extends SimplePool<RectF> {
        public RectFPool(int maxPoolSize) {
            super(maxPoolSize);
        }

        @Override
        public RectF acquire() {
            final RectF rect = super.acquire();
            if (rect == null) {
                return new RectF();
            }
            return rect;
        }
    }
    //endregion Nested classes

    //region Constants
    private static final String TAG = InteractiveImageView.class.getSimpleName();
    //endregion Constants

    //region Fields
    private ScaleType mScaleType;
    private ScalingStrategy mScalingStrategy;
    private OnDrawListener mOnDrawListener;

    // For state
    private final Matrix mBaseImageMatrix = new Matrix();
    private boolean mBaseImageMatrixDirty = false;
    private boolean mCenterDirty = false; // TODO Rethink this
    private boolean mScaleDirty = false;  // TODO Rethink this
    private boolean mImageMatrixDirty = false;

    // Object pools and buckets
    private RectFPool mRectFPool = new RectFPool(10);
    private final PointF mAppliedCenter = new PointF();
    private final PointF mAppliedScale = new PointF();
    private final PointF mCenter = new PointF();
    private final PointF mScale = new PointF();
    private final Matrix mImageMatrix = new Matrix();
    private final float[] mMatrixValues = new float[9];
    private final float[] mPts = new float[2];
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
    public ScaleType getScaleType() {
        return mScaleType;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        if (mOnDrawListener != null) {
            mOnDrawListener.onDraw(InteractiveImageView.this, canvas);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // TODO Can I do these here?
        if (mImageMatrixDirty) {
            mImageMatrixDirty = false;
            applyPlacement(mScale.x, mScale.y, mCenter.x, mCenter.y);
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidateScalingStrategy();
        invalidateBaseMatrix();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        invalidateScalingStrategy();
        invalidateBaseMatrix();
        invalidatePlacement();
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        setImageMatrixInternal(matrix);
        if (ScaleType.MATRIX == super.getScaleType()) {
            mBaseImageMatrix.set(matrix);
            mBaseImageMatrixDirty = false;
            invalidateScalingStrategy(); // TODO Inside or outside this block??
            invalidatePlacement(); // TODO Inside or outside this block??
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        invalidateScalingStrategy();
        invalidateBaseMatrix();
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        super.setPaddingRelative(start, top, end, bottom);
        invalidateScalingStrategy();
        invalidateBaseMatrix();
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        final ScaleType oldScaleType = super.getScaleType();
        super.setScaleType(scaleType);
        if (oldScaleType != super.getScaleType()) {
            mScaleType = scaleType;
            invalidateScalingStrategy();
            invalidateBaseMatrix();
            invalidatePlacement();
        }
    }
    //endregion Inherited methods

    //region Methods
    /**
     * Returns the view's base matrix. Do not change this matrix in place but make a copy.
     * @return The view's base matrix.
     */
    public Matrix getBaseImageMatrix() {
        if (mBaseImageMatrixDirty) {
            mBaseImageMatrixDirty = false;
            if (imageHasIntrinsicSize()) {
                final RectF intrinsicRect = mRectFPool.acquire();
                getIntrinsicImageRect(intrinsicRect);
                if (ScaleType.MATRIX == mScaleType) {
                    mBaseImageMatrix.set(getImageMatrix());
                } else {
                    final RectF dstRect = mRectFPool.acquire();
                    dstRect.set(0.0f, 0.0f, getAvailableWidth(), getAvailableHeight());
                    final ScaleToFit scaleToFit = scaleTypeToScaleToFit(mScaleType);
                    if (scaleToFit != null) {
                        mBaseImageMatrix.setRectToRect(intrinsicRect, dstRect, scaleToFit);
                    } else {
                        final float scale;
                        if (ScaleType.CENTER == mScaleType) {
                            scale = 1.0f;
                        } else {
                            final float scaleX = dstRect.width() / intrinsicRect.width();
                            final float scaleY = dstRect.height() / intrinsicRect.height();
                            if (ScaleType.CENTER_CROP == mScaleType) {
                                scale = Math.max(scaleX, scaleY);
                            } else {
                                scale = Math.min(Math.min(scaleX, scaleY), 1.0f);
                            }
                        }
                        final float dx = (dstRect.width() - intrinsicRect.width() * scale) / 2.0f;
                        final float dy = (dstRect.height() - intrinsicRect.height() * scale) / 2.0f;
                        mBaseImageMatrix.setScale(scale, scale);
                        mBaseImageMatrix.postTranslate(dx, dy);
                    }
                    mRectFPool.release(dstRect);
                }
                mRectFPool.release(intrinsicRect);
            } else {
                mBaseImageMatrix.reset();
            }
        }
        return mBaseImageMatrix;
    }

    public float getAppliedCenterX() {
        return mAppliedCenter.x;
    }

    public float getAppliedCenterY() {
        return mAppliedCenter.y;
    }

    public float getAppliedScaleX() {
        return mAppliedScale.x;
    }

    public float getAppliedScaleY() {
        return mAppliedScale.y;
    }

    public synchronized boolean getCenter(PointF outPoint) { // TODO Make protected/private?
        if (imageHasIntrinsicSize()) {
            if (mCenterDirty) {
                if (ViewCompat.isLaidOut(this)) {
                    mCenterDirty = false;
                    mImageMatrix.set(getImageMatrixInternal());
                    final RectF intrinsicRect = mRectFPool.acquire();
                    getIntrinsicImageRect(intrinsicRect);
                    mImageMatrix.invert(mImageMatrix);
                    mPts[0] = getAvailableWidth() / 2.0f;
                    mPts[1] = getAvailableHeight() / 2.0f;
                    mImageMatrix.mapPoints(mPts);
                    mCenter.set(mPts[0] / intrinsicRect.width(), mPts[1] / intrinsicRect.height());
                    mRectFPool.release(intrinsicRect);
                } else {
                    if (outPoint != null) {
                        outPoint.set(-1.0f, -1.0f);
                    }
                    return false;
                }
            }
            if (outPoint != null) {
                outPoint.set(mCenter);
            }
            return true;
        } else {
            if (outPoint != null) {
                outPoint.set(-1.0f, -1.0f);
            }
            return false;
        }
    }

    public float getCenterX() {
        getCenter(null);
        return mCenter.x;
    }

    public float getCenterY() {
        getCenter(null);
        return mCenter.y;
    }

    public boolean getDisplayedImageRect(Rect outRect) {
        final RectF rect = mRectFPool.acquire();
        final boolean retVal = getDisplayedImageRect(rect);
        if (outRect != null) {
            outRect.set(
                    Math.round(rect.left),
                    Math.round(rect.top),
                    Math.round(rect.right),
                    Math.round(rect.bottom));
        }
        mRectFPool.release(rect);
        return retVal;
    }

    public synchronized boolean getDisplayedImageRect(RectF outRect) {
        mImageMatrix.set(getImageMatrixInternal());
        mImageMatrix.getValues(mMatrixValues);
        return getScaledImageRect(
                mMatrixValues[Matrix.MSCALE_X],
                mMatrixValues[Matrix.MSCALE_Y],
                outRect);
    }

    public boolean getIntrinsicImageRect(Rect outRect) {
        return getScaledImageRect(1.0f, 1.0f, outRect);
    }

    public boolean getIntrinsicImageRect(RectF outRect) {
        return getScaledImageRect(1.0f, 1.0f, outRect);
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

    public synchronized boolean getScale(PointF outPoint) { // TODO Make protected/private?
        if (imageHasIntrinsicSize()) {
            if (mScaleDirty) {
                if (ViewCompat.isLaidOut(this)) {
                    mScaleDirty = false;
                    mImageMatrix.set(getImageMatrixInternal());
                    mImageMatrix.getValues(mMatrixValues);
                    mScale.set(mMatrixValues[Matrix.MSCALE_X], mMatrixValues[Matrix.MSCALE_Y]);
                } else {
                    if (outPoint != null) {
                        outPoint.set(-1.0f, -1.0f);
                    }
                    return false;
                }
            }
            if (outPoint != null) {
                outPoint.set(mScale);
            }
            return true;
        } else {
            if (outPoint != null) {
                outPoint.set(-1.0f, -1.0f);
            }
            return false;
        }
    }

    public boolean getScaledImageRect(float scaleX, float scaleY, Rect outRect) {
        final RectF rect = mRectFPool.acquire();
        final boolean retVal = getScaledImageRect(scaleX, scaleY, rect);
        if (outRect != null) {
            outRect.set(
                    Math.round(rect.left),
                    Math.round(rect.top),
                    Math.round(rect.right),
                    Math.round(rect.bottom));
        }
        mRectFPool.release(rect);
        return retVal;
    }

    public boolean getScaledImageRect(float scaleX, float scaleY, RectF outRect) {
        final boolean retVal;
        final float right;
        final float bottom;
        final Drawable dr = getDrawable();
        if (dr == null) {
            right = 0.0f;
            bottom = 0.0f;
            retVal = false;
        } else {
            final int intrinsicWidth = dr.getIntrinsicWidth();
            final int intrinsicHeight = dr.getIntrinsicHeight();
            if (intrinsicWidth < 0 || intrinsicHeight < 0) {
                right = -1.0f;
                bottom = -1.0f;
                retVal = false;
            } else {
                right = (intrinsicWidth < 0 ? intrinsicWidth : intrinsicWidth * scaleX);
                bottom = (intrinsicHeight < 0 ? intrinsicHeight : intrinsicHeight * scaleY);
                retVal = true;
            }
        }
        if (outRect != null) {
            outRect.set(0, 0, right, bottom);
        }
        return retVal;
    }

    public float getScaleX() {
        getScale(null);
        return mScale.x;
    }

    public float getScaleY() {
        getScale(null);
        return mScale.y;
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

    public boolean setCenter(float centerX, float centerY) {
        return setPlacement(getScaleX(), getScaleY(), centerX, centerY);
    }

    public void setOnDrawListener(OnDrawListener onDrawListener) {
        mOnDrawListener = onDrawListener;
    }

    public boolean setPlacement(float scaleX, float scaleY, float centerX, float centerY) {
        if (imageHasIntrinsicSize()) {
            mScale.set(scaleX, scaleY);
            mCenter.set(centerX, centerY);
            if (ViewCompat.isLaidOut(this)) {
                mImageMatrixDirty = false;
                applyPlacement(scaleX, scaleY, centerX, centerY);
                invalidate();
            } else {
                mImageMatrixDirty = true;
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean setScale(float scaleX, float scaleY) {
        return setPlacement(scaleX, scaleY, getCenterX(), getCenterY());
    }

    public void setScalingStrategy(ScalingStrategy scalingStrategy) {
        mScalingStrategy = scalingStrategy;
    }
    //endregion Methods

    //region Private methods
    private synchronized boolean applyPlacement(
            float scaleX,
            float scaleY,
            float centerX,
            float centerY) {

        mAppliedScale.set(scaleX, scaleY);
        mAppliedCenter.set(centerX, centerY);

        if (imageHasIntrinsicSize()) {
            final RectF intrinsicRect = mRectFPool.acquire();
            getIntrinsicImageRect(intrinsicRect);

            // TODO NEXT observe boundaries!! How do I best do this, especially with skew, persp?
            final float constrainedScaleX = MathUtils.clamp(scaleX, getMinScaleX(), getMaxScaleX());
            final float constrainedScaleY = MathUtils.clamp(scaleY, getMinScaleY(), getMaxScaleY());
            final float constrainedCenterX = centerX; // TODO NEXT
            final float constrainedCenterY = centerY; // TODO NEXT

            mScale.set(constrainedScaleX, constrainedScaleY);
            mCenter.set(constrainedCenterX, constrainedCenterY);

            mImageMatrix.set(getImageMatrixInternal());
            mImageMatrix.getValues(mMatrixValues);
            mMatrixValues[Matrix.MSCALE_X] = constrainedScaleX;
            mMatrixValues[Matrix.MSCALE_Y] = constrainedScaleY;
            // TODO Skew / Perspective?
            mImageMatrix.setValues(mMatrixValues);

            mPts[0] = intrinsicRect.width() * constrainedCenterX;
            mPts[1] = intrinsicRect.height() * constrainedCenterY;
            mImageMatrix.mapPoints(mPts);

            final float viewCenterX = getAvailableWidth() / 2.0f;
            final float viewCenterY = getAvailableHeight() / 2.0f;
            final float deltaTransX = mPts[0] - mMatrixValues[Matrix.MTRANS_X];
            final float deltaTransY = mPts[1] - mMatrixValues[Matrix.MTRANS_Y];
            final float transX = viewCenterX - deltaTransX;
            final float transY = viewCenterY - deltaTransY;

            mMatrixValues[Matrix.MTRANS_X] = transX;
            mMatrixValues[Matrix.MTRANS_Y] = transY;
            mImageMatrix.setValues(mMatrixValues);

            if (!ScaleType.MATRIX.equals(super.getScaleType())) {
                super.setScaleType(ScaleType.MATRIX);
            }
            setImageMatrixInternal(mImageMatrix);
            mRectFPool.release(intrinsicRect);
            return true;
        } else {
            return false;
        }
    }

    private int getAvailableHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private int getAvailableWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private Matrix getImageMatrixInternal() {
        final Matrix matrix = super.getImageMatrix();
        if (super.getScaleType() == ScaleType.FIT_XY) {
            synchronized (this) {
                final RectF srcRect = mRectFPool.acquire();
                if (getIntrinsicImageRect(srcRect)) {
                    final RectF dstRect = mRectFPool.acquire();
                    dstRect.set(0.0f, 0.0f, getAvailableWidth(), getAvailableHeight());
                    matrix.setRectToRect(srcRect, dstRect, ScaleToFit.FILL);
                    mRectFPool.release(dstRect);
                }
                mRectFPool.release(srcRect);
            }
        }
        return matrix;
    }

    private ScalingStrategy getScalingStrategy() {
        if (mScalingStrategy == null) {
            mScalingStrategy = new DefaultScalingStrategy();
        }
        return mScalingStrategy;
    }

    private boolean imageHasIntrinsicSize() {
        return getIntrinsicImageRect((RectF) null);
    }

    private void invalidateBaseMatrix() {
        mBaseImageMatrixDirty = true;
    }

    private void invalidatePlacement() {
        mCenterDirty = true;
        mScaleDirty = true;
    }

    private void setImageMatrixInternal(Matrix matrix) { // TODO Do I need this?
        super.setImageMatrix(matrix);
    }

    private static Matrix.ScaleToFit scaleTypeToScaleToFit(ScaleType scaleType) {
        if (scaleType == null) {
            return null;
        } else switch (scaleType) {
            case FIT_CENTER:
                return ScaleToFit.CENTER;
            case FIT_END:
                return ScaleToFit.END;
            case FIT_START:
                return ScaleToFit.START;
            case FIT_XY:
                return ScaleToFit.FILL;
            default:
                return null;
        }
    }
    //endregion Private methods
}
