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
    private final RectF mIntrinsicRect = new RectF();
    private final RectF mDstRect = new RectF();
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

    //region Methods
    /**
     * Returns the view's base matrix. Do not change this matrix in place but make a copy.
     * @return The view's base matrix.
     */
    public synchronized Matrix getBaseImageMatrix() {
        if (mBaseImageMatrixDirty) {
            mBaseImageMatrixDirty = false;
            if (getIntrinsicImageRect(mIntrinsicRect)) {
                if (ScaleType.MATRIX == mScaleType) {
                    mBaseImageMatrix.set(getImageMatrix());
                } else {
                    mDstRect.set(0.0f, 0.0f, getAvailableWidth(), getAvailableHeight());
                    final ScaleToFit scaleToFit = scaleTypeToScaleToFit(mScaleType);
                    if (scaleToFit != null) {
                        mBaseImageMatrix.setRectToRect(mIntrinsicRect, mDstRect, scaleToFit);
                    } else {
                        final float scale;
                        if (ScaleType.CENTER == mScaleType) {
                            scale = 1.0f;
                        } else {
                            final float scaleX = mDstRect.width() / mIntrinsicRect.width();
                            final float scaleY = mDstRect.height() / mIntrinsicRect.height();
                            if (ScaleType.CENTER_CROP == mScaleType) {
                                scale = Math.max(scaleX, scaleY);
                            } else {
                                scale = Math.min(Math.min(scaleX, scaleY), 1.0f);
                            }
                        }
                        final float dx = (mDstRect.width() - mIntrinsicRect.width() * scale) / 2;
                        final float dy = (mDstRect.height() - mIntrinsicRect.height() * scale) / 2;
                        mBaseImageMatrix.setScale(scale, scale);
                        mBaseImageMatrix.postTranslate(dx, dy);
                    }
                }
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
        if (!imageHasIntrinsicSize()) {
            return false;
        }

        if (mCenterDirty) {
            if (!ViewCompat.isLaidOut(this)) {
                return false;
            }

            mCenterDirty = false;
            mImageMatrix.set(getImageMatrixInternal());
            mImageMatrix.invert(mImageMatrix);
            mPts[0] = getAvailableWidth() / 2.0f;
            mPts[1] = getAvailableHeight() / 2.0f;
            mImageMatrix.mapPoints(mPts);
            mCenter.set(mPts[0] / getIntrinsicImageWidth(), mPts[1] / getIntrinsicImageHeight());
        }

        if (outPoint != null) {
            outPoint.set(mCenter);
        }
        return true;
    }

    public float getCenterX() {
        getCenter(null);
        return mCenter.x;
    }

    public float getCenterY() {
        getCenter(null);
        return mCenter.y;
    }

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
        final Drawable dr = getDrawable();
        return (dr == null ? -1 : dr.getIntrinsicHeight());
    }

    public int getIntrinsicImageWidth() {
        final Drawable dr = getDrawable();
        return (dr == null ? -1 : dr.getIntrinsicWidth());
    }

    public float getMaxCenterX() {
        final int displayedImageWidth = getDisplayedImageWidth();
        final int availableWidth = getAvailableWidth();
        if (displayedImageWidth > availableWidth) {
            return 1.0f - (availableWidth / 2.0f) / displayedImageWidth;
        } else {
            return 0.5f;
        }
    }

    public float getMaxCenterY() {
        final int displayedImageHeight = getDisplayedImageHeight();
        final int availableHeight = getAvailableHeight();
        if (displayedImageHeight > availableHeight) {
            return 1.0f - (availableHeight / 2.0f) / displayedImageHeight;
        } else {
            return 0.5f;
        }
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
        final float displayedImageWidth = getDisplayedImageWidth();
        final int availableWidth = getAvailableWidth();
        if (displayedImageWidth > availableWidth) {
            return (availableWidth / 2.0f) / displayedImageWidth;
        } else {
            return 0.5f;
        }
    }

    public float getMinCenterY() {
        final float displayedImageHeight = getDisplayedImageHeight();
        final int availableHeight = getAvailableHeight();
        if (displayedImageHeight > availableHeight) {
            return (availableHeight / 2.0f) / displayedImageHeight;
        } else {
            return 0.5f;
        }
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
        final int intrinsicHeight = getIntrinsicImageHeight();
        return (intrinsicHeight < 0 ? -1.0f : intrinsicHeight * scale);
    }

    public float getScaledImageWidth(float scale) {
        final int intrinsicWidth = getIntrinsicImageWidth();
        return (intrinsicWidth < 0 ? -1.0f : intrinsicWidth * scale);
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

        if (!getIntrinsicImageRect(mIntrinsicRect)) {
            return false;
        }
        // TODO NEXT observe boundaries!! How do I best do this, especially with skew, persp?
        final float constrainedScaleX = MathUtils.clamp(scaleX, getMinScaleX(), getMaxScaleX());
        final float constrainedScaleY = MathUtils.clamp(scaleY, getMinScaleY(), getMaxScaleY());

        final float constrainedCenterX = centerX; // TODO NEXT
        final float constrainedCenterY = centerY; // TODO NEXT

        final Matrix matrix = getImageMatrixInternal();
        matrix.setScale(scaleX, scaleY);  // TODO Use constrained
        matrix.mapRect(mDstRect, mIntrinsicRect);
        // TODO Should I be using mDstRect from now on instead of mIntrinsicRect?

        mScale.set(constrainedScaleX, constrainedScaleY);
        mCenter.set(constrainedCenterX, constrainedCenterY);

        mImageMatrix.set(getImageMatrixInternal());
        mImageMatrix.getValues(mMatrixValues);
        mMatrixValues[Matrix.MSCALE_X] = constrainedScaleX;
        mMatrixValues[Matrix.MSCALE_Y] = constrainedScaleY;
        // TODO Skew / Perspective?
        mImageMatrix.setValues(mMatrixValues);

        mPts[0] = mIntrinsicRect.width() * constrainedCenterX;
        mPts[1] = mIntrinsicRect.height() * constrainedCenterY;
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
        return true;
    }

    private int getAvailableHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private int getAvailableWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private synchronized Matrix getImageMatrixInternal() {
        final Matrix matrix = super.getImageMatrix();
        if (super.getScaleType() == ScaleType.FIT_XY) {
            synchronized (this) {
                if (getIntrinsicImageRect(mIntrinsicRect)) {
                    mDstRect.set(0.0f, 0.0f, getAvailableWidth(), getAvailableHeight());
                    matrix.setRectToRect(mIntrinsicRect, mDstRect, ScaleToFit.FILL);
                }
            }
        }
        return matrix;
    }

    private boolean getIntrinsicImageRect(RectF outRect) {
        final int intrinsicImageWidth = getIntrinsicImageWidth();
        if (intrinsicImageWidth < 1) {
            return false;
        }
        final int intrinsicImageHeight = getIntrinsicImageHeight();
        if (intrinsicImageHeight < 1) {
            return false;
        }
        if (outRect != null) {
            outRect.set(0.0f, 0.0f, intrinsicImageWidth, intrinsicImageHeight);
        }
        return true;
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
        return getIntrinsicImageRect(null);
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
