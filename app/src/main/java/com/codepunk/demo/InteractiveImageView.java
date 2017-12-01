package com.codepunk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
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

// TODO NEXT REPAIR!!!!
// TODO Next Differentiate between "applied" center/scale and actual/current center/scale -- Done??
// TODO Next 2 observe limits when setting center/scale
// TODO Handle skew, perspective
// TODO Do *I* have to figure out when to invalidate strategies? Or can I just override invalidate()?

public class InteractiveImageView extends AppCompatImageView {
    //region Nested classes
    private enum Boundary {
        MIN,
        MAX
    }

    private enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    public interface OnDrawListener {
        void onDraw(InteractiveImageView view, Canvas canvas);
    }

    public interface PanningStrategy {
        int getMaxTransX(float scale);
        int getMinTransX(float scale);
        int getMaxTransY(float scale);
        int getMinTransY(float scale);
        void invalidate();
    }

    public interface ScalingStrategy {
        float getMaxScaleX();
        float getMaxScaleY();
        float getMinScaleX();
        float getMinScaleY();
        void invalidateMaxScale();
        void invalidateMinScale();
    }

    private class DefaultPanningStrategy implements PanningStrategy {
        private int getScaledImageDimension(@NonNull Orientation orientation, float scale) {
            final int dimension = (orientation == Orientation.VERTICAL ?
                    getIntrinsicImageHeight() :
                    getIntrinsicImageWidth());
            return (dimension == -1 ? -1 : Math.round(dimension * scale));
        }

        // TODO NEXT Can I replace some of this with Matrix operations?
        // Answer: NO. But I also need to query LTR to get the correct side :(
        private int getTrans(
                float scale,
                @NonNull Orientation orientation,
                @NonNull Boundary boundary) {
            final int scaledDimension = getScaledImageDimension(orientation, scale);
            if (scaledDimension < 0) {
                return 0;
            } else {
                final int availableDimension = (orientation == Orientation.VERTICAL ?
                        getAvailableHeight() :
                        getAvailableWidth());
                if (scaledDimension < availableDimension) {
                    // If image is smaller than available dimension, min and max are the same
                    if (mScaleType == ScaleType.FIT_START || mScaleType == ScaleType.FIT_END) {
                        final boolean reversed = (orientation == Orientation.HORIZONTAL &&
                                ViewCompat.getLayoutDirection(InteractiveImageView.this) ==
                                        ViewCompat.LAYOUT_DIRECTION_RTL);
                        final boolean useStart = ((mScaleType == ScaleType.FIT_END) == reversed);
                        return (useStart ? 0 : availableDimension - scaledDimension);
                    } else {
                        // Center image for anything other than FIT_START or FIT_END
                        return Math.round((availableDimension - scaledDimension) / 2.0f);
                    }
                } else if (boundary == Boundary.MIN) {
                    // If image is larger than available dimension, the minimum is the difference
                    return availableDimension - scaledDimension;
                } else {
                    // If image is larger than available dimension, the maximum is always 0
                    return 0;
                }
            }
        }

        @Override
        public int getMaxTransX(float scale) {
            return getTrans(scale, Orientation.HORIZONTAL, Boundary.MAX);
        }

        @Override
        public int getMinTransX(float scale) {
            return getTrans(scale, Orientation.HORIZONTAL, Boundary.MIN);
        }

        @Override
        public int getMaxTransY(float scale) {
            return getTrans(scale, Orientation.VERTICAL, Boundary.MAX);
        }

        @Override
        public int getMinTransY(float scale) {
            return getTrans(scale, Orientation.VERTICAL, Boundary.MIN);
        }

        @Override
        public void invalidate() {
            // no-op
        }
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

        private boolean mMaxScaleDirty = true;
        private boolean mMinScaleDirty = true;

        DefaultScalingStrategy() {
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
                final int displayedWidth = getDisplayedImageWidth();
                final int displayedHeight = getDisplayedImageHeight();
                if (displayedWidth > 0 && displayedHeight > 0) {
                    final Matrix baseImageMatrix = getBaseImageMatrix();
                    baseImageMatrix.getValues(mMatrixValues);
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
    private PanningStrategy mPanningStrategy;
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
        // TODO Examine all these invalidates. Maybe just override invalidate() and invalidate everything?
        invalidatePanningStrategy();
        invalidateScalingStrategy();
        invalidateBaseMatrix();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        invalidatePanningStrategy();
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
            invalidatePanningStrategy();
            invalidateScalingStrategy(); // TODO Inside or outside this block??
            invalidatePlacement(); // TODO Inside or outside this block??
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        invalidatePanningStrategy();
        invalidateScalingStrategy();
        invalidateBaseMatrix();
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        super.setPaddingRelative(start, top, end, bottom);
        invalidatePanningStrategy();
        invalidateScalingStrategy();
        invalidateBaseMatrix();
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        final ScaleType oldScaleType = super.getScaleType();
        super.setScaleType(scaleType);
        if (oldScaleType != super.getScaleType()) {
            mScaleType = scaleType;
            invalidatePanningStrategy();
            invalidateScalingStrategy();
            invalidateBaseMatrix();
            invalidatePlacement();
        }
    }
    //endregion Inherited methods

    //region Private methods
    private int getIntrinsicImageDimension(@NonNull Orientation orientation) {
        final Drawable dr = getDrawable();
        if (dr == null) {
            return -1;
        } else if (orientation == Orientation.VERTICAL) {
            return dr.getIntrinsicHeight();
        } else {
            return dr.getIntrinsicWidth();
        }
    }
    //endregion Private methods

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

    /*
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
    */

    public synchronized int getDisplayedImageHeight() {
        mImageMatrix.set(getImageMatrixInternal());
        mImageMatrix.getValues(mMatrixValues);
        return Math.round(getScaledImageHeight(mMatrixValues[MSCALE_Y]));
    }

    public synchronized int getDisplayedImageWidth() {
        mImageMatrix.set(getImageMatrixInternal());
        mImageMatrix.getValues(mMatrixValues);
        return Math.round(getScaledImageWidth(mMatrixValues[MSCALE_X]));
    }

    public int getIntrinsicImageHeight() {
        return getIntrinsicImageDimension(Orientation.VERTICAL);
    }

    public int getIntrinsicImageWidth() {
        return getIntrinsicImageDimension(Orientation.HORIZONTAL);
    }

    public float getMaxCenterX() {
        final float maxCenterX;
        final RectF displayedRect = mRectFPool.acquire();
        if (getDisplayedImageRect(displayedRect)) {
            final float displayedWidth = displayedRect.width();
            final int availableWidth = getAvailableWidth();
            if (displayedWidth > availableWidth) {
                maxCenterX = 1.0f - (availableWidth / 2.0f) / displayedWidth;
            } else {
                maxCenterX = 0.5f;
            }
        } else {
            maxCenterX = 0.5f;
        }
        mRectFPool.release(displayedRect);
        return maxCenterX;
    }

    public float getMaxCenterY() {
        final float maxCenterY;
        final RectF displayedRect = mRectFPool.acquire();
        if (getDisplayedImageRect(displayedRect)) {
            final float displayedHeight = displayedRect.height();
            final int availableHeight = getAvailableHeight();
            if (displayedHeight > availableHeight) {
                maxCenterY = 1.0f - (availableHeight / 2.0f) / displayedHeight;
            } else {
                maxCenterY = 0.5f;
            }
        } else {
            maxCenterY = 0.5f;
        }
        mRectFPool.release(displayedRect);
        return maxCenterY;
    }

    // TODO Do I need versions of this where I explicitly pass a scale?
    public float getMaxTransX() {
        return getPanningStrategy().getMaxTransX(getScaleX());
    }

    public float getMaxTransY() {
        return getPanningStrategy().getMaxTransY(getScaleY());
    }

    public float getMaxScaleX() {
        return getScalingStrategy().getMaxScaleX();
    }

    public float getMaxScaleY() {
        return getScalingStrategy().getMaxScaleY();
    }

    public float getMinCenterX() {
        final float minCenterX;
        final RectF displayedRect = mRectFPool.acquire();
        if (getDisplayedImageRect(displayedRect)) {
            final float displayedWidth = displayedRect.width();
            final int availableWidth = getAvailableWidth();
            if (displayedWidth > availableWidth) {
                minCenterX = (availableWidth / 2.0f) / displayedWidth;
            } else {
                minCenterX = 0.5f;
            }
        } else {
            minCenterX = 0.5f;
        }
        mRectFPool.release(displayedRect);
        return minCenterX;
    }

    public float getMinCenterY() {
        final float minCenterY;
        final RectF displayedRect = mRectFPool.acquire();
        if (getDisplayedImageRect(displayedRect)) {
            final float displayedHeight = displayedRect.height();
            final int availableHeight = getAvailableHeight();
            if (displayedHeight > availableHeight) {
                minCenterY = (availableHeight / 2.0f) / displayedHeight;
            } else {
                minCenterY = 0.5f;
            }
        } else {
            minCenterY = 0.5f;
        }
        mRectFPool.release(displayedRect);
        return minCenterY;
    }

    public float getMinTransX() {
        return getPanningStrategy().getMinTransX(getScaleX());
    }

    public float getMinTransY() {
        return getPanningStrategy().getMinTransY(getScaleY());
    }

    public float getMinScaleX() {
        return getScalingStrategy().getMinScaleX();
    }

    public float getMinScaleY() {
        return getScalingStrategy().getMinScaleY();
    }

    public synchronized boolean getScale(PointF outPoint) { // TODO Make protected/private?
        final boolean imageHasIntrinsicSize = imageHasIntrinsicSize();
        if (imageHasIntrinsicSize) {
            if (mScaleDirty) {
                if (ViewCompat.isLaidOut(this)) {
                    mScaleDirty = false;
                    mImageMatrix.set(getImageMatrixInternal());
                    mImageMatrix.getValues(mMatrixValues);
                    mScale.set(mMatrixValues[Matrix.MSCALE_X], mMatrixValues[Matrix.MSCALE_Y]);
                } else {
                    if (outPoint != null) {
                        outPoint.set(1.0f, 1.0f);
                    }
                    return false;
                }
            }
            if (outPoint != null) {
                outPoint.set(mScale);
            }
        } else {
            if (outPoint != null) {
                outPoint.set(1.0f, 1.0f);
            }
        }
        return imageHasIntrinsicSize;
    }

    public float getScaledImageHeight(float scale) {
        final int intrinsicHeight = getIntrinsicImageDimension(Orientation.VERTICAL);
        return (intrinsicHeight < 0 ? -1.0f : intrinsicHeight * scale);
    }

    public float getScaledImageWidth(float scale) {
        final int intrinsicWidth = getIntrinsicImageDimension(Orientation.HORIZONTAL);
        return (intrinsicWidth < 0 ? -1.0f : intrinsicWidth * scale);
    }

    /*
    public boolean getScaledImageRect(float scaleX, float scaleY, Rect outRect) {
        final RectF rect = mRectFPool.acquire();
        final boolean imageHasIntrinsicSize = getScaledImageRect(scaleX, scaleY, rect);
        if (outRect != null) {
            outRect.set(
                    Math.round(rect.left),
                    Math.round(rect.top),
                    Math.round(rect.right),
                    Math.round(rect.bottom));
        }
        mRectFPool.release(rect);
        return imageHasIntrinsicSize;
    }

    public boolean getScaledImageRect(float scaleX, float scaleY, RectF outRect) {
        final boolean imageHasIntrinsicSize;
        final float right;
        final float bottom;
        final Drawable dr = getDrawable();
        if (dr == null) {
            right = 0.0f;
            bottom = 0.0f;
            imageHasIntrinsicSize = false;
        } else {
            final int intrinsicWidth = dr.getIntrinsicWidth();
            final int intrinsicHeight = dr.getIntrinsicHeight();
            imageHasIntrinsicSize = !(intrinsicWidth < 0 || intrinsicHeight < 0);
            right = (imageHasIntrinsicSize ? intrinsicWidth * scaleX : 0.0f);
            bottom = (imageHasIntrinsicSize ? intrinsicHeight * scaleY : 0.0f);
        }
        if (outRect != null) {
            outRect.set(0, 0, right, bottom);
        }
        return imageHasIntrinsicSize;
    }
    */

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

    public void invalidatePanningStrategy() {
        getPanningStrategy().invalidate();
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

    public void setPanningStrategy(PanningStrategy panningStrategy) {
        mPanningStrategy = panningStrategy;
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
            final RectF matrixRect = mRectFPool.acquire();
            getIntrinsicImageRect(intrinsicRect);

            // TODO NEXT observe boundaries!! How do I best do this, especially with skew, persp?
            final float constrainedScaleX = MathUtils.clamp(scaleX, getMinScaleX(), getMaxScaleX());
            final float constrainedScaleY = MathUtils.clamp(scaleY, getMinScaleY(), getMaxScaleY());

            final float constrainedCenterX = centerX; // TODO NEXT
            final float constrainedCenterY = centerY; // TODO NEXT

            final Matrix matrix = getImageMatrixInternal();
            matrix.setScale(scaleX, scaleY);  // TODO Use constrained
            matrix.mapRect(matrixRect, intrinsicRect);




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
            // TODO Calculating trans here but maybe I do it in panning strategy?
            // What should a panning strategy be able to do?

            mMatrixValues[Matrix.MTRANS_X] = transX;
            mMatrixValues[Matrix.MTRANS_Y] = transY;
            mImageMatrix.setValues(mMatrixValues);

            if (!ScaleType.MATRIX.equals(super.getScaleType())) {
                super.setScaleType(ScaleType.MATRIX);
            }
            setImageMatrixInternal(mImageMatrix);

            mRectFPool.release(matrixRect);
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

    private PanningStrategy getPanningStrategy() {
        if (mPanningStrategy == null) {
            mPanningStrategy = new DefaultPanningStrategy();
        }
        return mPanningStrategy;
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
