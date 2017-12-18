package com.codepunk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.codepunk.demo.support.DisplayCompat;

// TODO NEXT Allow panning strategy to draw edges or bounce (from within applyPlacement)
public class StagingInteractiveImageView extends AppCompatImageView {
    //region Nested classes
    public interface OnDrawListener {
        void onDraw(StagingInteractiveImageView view, Canvas canvas);
    }

    public interface PanningStrategy {
        float resolveTransX(float transX, float clampedTransX, boolean fromUser);
        float resolveTransY(float transY, float clampedTransY, boolean fromUser);
    }

    public interface ScalingStrategy {
        float resolveScaleX(float scaleX, boolean fromUser);
        float resolveScaleY(float scaleY, boolean fromUser);
        float getMaxScaleX();
        float getMaxScaleY();
        float getMinScaleX();
        float getMinScaleY();
        void invalidate();
    }

    private static class DefaultPanningStrategy implements PanningStrategy {
        private @NonNull final StagingInteractiveImageView mImageView;

        public DefaultPanningStrategy(@NonNull StagingInteractiveImageView imageView) {
            super();
            mImageView = imageView;
        }

        @Override
        public float resolveTransX(float transX, float clampedTransX, boolean fromUser) {
            return clampedTransX;
        }

        @Override
        public float resolveTransY(float transY, float clampedTransY, boolean fromUser) {
            return clampedTransY;
        }
    }

    private static class DefaultScalingStrategy implements ScalingStrategy {
        //region Constants
        static final float BREADTH_MULTIPLIER = 3.0f;
        static final float LENGTH_MULTIPLIER = 5.0f;
        //endregion Constants

        //region Fields
        private @NonNull final StagingInteractiveImageView mImageView;

        private final int mDisplayBreadth;
        private final int mDisplayLength;
        private final int mMaxBreadth;
        private final int mMaxLength;

        private final float[] mMatrixValues = new float[9];
        private final PointF mMaxScale = new PointF();
        private final PointF mMinScale = new PointF();
        private final RectF mBaseRect = new RectF();

        private boolean mMinScaleDirty = true;
        private boolean mMaxScaleDirty = true;
        //endregion Fields

        //region Constructors
        DefaultScalingStrategy(@NonNull StagingInteractiveImageView imageView) {
            super();
            mImageView = imageView;
            final Context context = imageView.getContext();
            final WindowManager manager =
                    (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (manager == null) {
                final DisplayMetrics dm = context.getResources().getDisplayMetrics();
                mDisplayBreadth = Math.min(dm.widthPixels, dm.heightPixels);
                mDisplayLength = Math.max(dm.widthPixels, dm.heightPixels);
            } else {
                final Point size = new Point();
                DisplayCompat.getRealSize(manager.getDefaultDisplay(), size);
                mDisplayBreadth = Math.min(size.x, size.y);
                mDisplayLength = Math.max(size.x, size.y);
            }
            mMaxBreadth = Math.round(BREADTH_MULTIPLIER * mDisplayBreadth);
            mMaxLength = Math.round(LENGTH_MULTIPLIER * mDisplayLength);
        }
        //endregion Constructors

        //region Implemented methods
        @Override
        public float resolveScaleX(float scaleX, boolean fromUser) {
            return MathUtils.clamp(scaleX, getMinScaleX(), getMaxScaleX());
        }

        @Override
        public float resolveScaleY(float scaleY, boolean fromUser) {
            return MathUtils.clamp(scaleY, getMinScaleY(), getMaxScaleY());
        }

        @Override
        public float getMaxScaleX() {
            return getMaxScale().x;
        }

        @Override
        public float getMaxScaleY() {
            return getMaxScale().y;
        }

        @Override
        public float getMinScaleX() {
            return getMinScale().x;
        }

        @Override
        public float getMinScaleY() {
            return getMinScale().y;
        }

        @Override
        public void invalidate() {
            mMaxScaleDirty = true;
            mMinScaleDirty = true;
        }
        //endregion Implemented methods

        //region Private methods
        private PointF getMaxScale() {
            synchronized (mMaxScale) {
                if (mMaxScaleDirty) {
                    mMaxScaleDirty = false;
                    if (mImageView.drawableHasIntrinsicSize()) {
                        mImageView.getTransformedImageRect(
                                mImageView.getBaseImageMatrix(),
                                mBaseRect);
                        final float baseWidth = mBaseRect.width();
                        final float baseHeight = mBaseRect.height();
                        final float baseBreadth = Math.min(baseWidth, baseHeight);
                        final float baseLength = Math.max(baseWidth, baseHeight);
                        final float screenBasedScale =
                                Math.min(mMaxBreadth / baseBreadth, mMaxLength / baseLength);
                        final int availableWidth = mImageView.getAvailableWidth();
                        final int availableHeight = mImageView.getAvailableHeight();
                        final int availableSize;
                        if (baseWidth < baseHeight) {
                            availableSize = availableWidth;
                        } else if (baseWidth > baseHeight) {
                            availableSize = availableHeight;
                        } else {
                            availableSize = Math.min(availableWidth, availableHeight);
                        }
                        final float viewBasedScale = availableSize / baseBreadth;
                        final float scale = Math.max(screenBasedScale, viewBasedScale);
                        mMaxScale.set(
                                scale * mMatrixValues[Matrix.MSCALE_X],
                                scale * mMatrixValues[Matrix.MSCALE_Y]);
                    } else {
                        mMaxScale.set(1.0f, 1.0f);
                    }
                }
            }
            return mMaxScale;
        }

        private PointF getMinScale() {
            synchronized (mMinScale) {
                if (mMinScaleDirty) {
                    mMinScaleDirty = false;
                    mImageView.getBaseImageMatrix().getValues(mMatrixValues);
                    mMinScale.set(mMatrixValues[Matrix.MSCALE_X], mMatrixValues[Matrix.MSCALE_Y]);
                }
            }
            return mMinScale;
        }
        //endregion Private methods
    }
    //endregion Nested classes

    //region Constants
    private static final String TAG = StagingInteractiveImageView.class.getSimpleName();
    //endregion Constants

    //region Fields
    private ScaleType mScaleType;
    private OnDrawListener mOnDrawListener;
    private float mImageScaleX = 1.0f;
    private float mImageScaleY = 1.0f;
    private float mImageCenterX = 0.5f;
    private float mImageCenterY = 0.5f;

    private PanningStrategy mPanningStrategy;
    private ScalingStrategy mScalingStrategy;

    // Buckets
    private final Matrix mBaseImageMatrix = new Matrix();
    private final Matrix mTempMatrix = new Matrix();
    private final RectF mTempSrc = new RectF();
    private final RectF mTempDst = new RectF();
    private final float[] mMatrixValues = new float[9];
    private final float[] mPts = new float[2];

    // Dirty flags
    private boolean mBaseImageMatrixDirty = true;
    private boolean mPlacementDirty = false;
    private boolean mImageCenterDirty = true;
    private boolean mImageScaleDirty = true;
    //endregion Fields

    //region Constructors
    public StagingInteractiveImageView(Context context) {
        super(context);
    }

    public StagingInteractiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StagingInteractiveImageView(Context context, AttributeSet attrs, int defStyleAttr) {
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
            mOnDrawListener.onDraw(this, canvas);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mPlacementDirty) {
            mPlacementDirty = false;
            applyPlacement(
                    mImageScaleX,
                    mImageScaleY,
                    mImageCenterX,
                    mImageCenterY,
                    false);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBaseImageMatrixDirty = true;
        getScalingStrategy().invalidate();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        mImageCenterDirty = true;
        mImageScaleDirty = true;
        mBaseImageMatrixDirty = true;
        getScalingStrategy().invalidate();
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        super.setImageMatrix(matrix);
        mImageCenterDirty = true;
        mImageScaleDirty = true;
        if (ScaleType.MATRIX == mScaleType) {
            mBaseImageMatrix.set(matrix);
            mBaseImageMatrixDirty = false;
            getScalingStrategy().invalidate();
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        mBaseImageMatrixDirty = true;
        getScalingStrategy().invalidate();
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        super.setPaddingRelative(start, top, end, bottom);
        mBaseImageMatrixDirty = true;
        getScalingStrategy().invalidate();
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        final ScaleType oldScaleType = super.getScaleType();
        super.setScaleType(scaleType);
        if (oldScaleType != super.getScaleType()) {
            mScaleType = scaleType;
            mImageCenterDirty = true;
            mImageScaleDirty = true;
            mBaseImageMatrixDirty = true;
            getScalingStrategy().invalidate();
        }
    }
    //endregion Inherited methods

    //region Methods
    public float getImageScaleX() {
        getImageScale();
        return mImageScaleX;
    }

    public float getImageScaleY() {
        getImageScale();
        return mImageScaleY;
    }

    public float getImageCenterX() {
        getImageCenter();
        return mImageCenterX;
    }

    public float getImageCenterY() {
        getImageCenter();
        return mImageCenterY;
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

    public boolean hasCustomPlacement() {
        // TODO -- compare exact values
        return (mScaleType != super.getScaleType());
    }

    public void setImageCenter(float centerX, float centerY) {
        getImageScale();
        setPlacement(mImageScaleX, mImageScaleY, centerX, centerY);
    }

    public void setImageScale(float scaleX, float scaleY) {
        getImageCenter();
        setPlacement(scaleX, scaleY, mImageCenterX, mImageCenterY);
    }

    public void setOnDrawListener(OnDrawListener onDrawListener) {
        mOnDrawListener = onDrawListener;
    }

    public void setPanningStrategy(PanningStrategy panningStrategy) {
        mPanningStrategy = panningStrategy;
    }

    public void setPlacement(float scaleX, float scaleY, float centerX, float centerY) {
        mImageCenterDirty = false;
        mImageScaleDirty = false;
        mImageScaleX = scaleX;
        mImageScaleY = scaleY;
        mImageCenterX = centerX;
        mImageCenterY = centerY;
        if (drawableHasIntrinsicSize()) {
            if (ViewCompat.isLaidOut(this)) {
                mPlacementDirty = false;
                applyPlacement(scaleX, scaleY, centerX, centerY, false);
                invalidate();
            } else {
                mPlacementDirty = true;
            }
        }
    }

    public void setScalingStrategy(ScalingStrategy scalingStrategy) {
        mScalingStrategy = scalingStrategy;
    }
    //endregion Methods

    //region Protected methods
    private PanningStrategy getPanningStrategy() {
        if (mPanningStrategy == null) {
            mPanningStrategy = new DefaultPanningStrategy(this);
        }
        return mPanningStrategy;
    }

    private ScalingStrategy getScalingStrategy() {
        if (mScalingStrategy == null) {
            mScalingStrategy = new DefaultScalingStrategy(this);
        }
        return mScalingStrategy;
    }
    //endregion Protected methods

    //region Private methods
    private void applyPlacement(
            float scaleX,
            float scaleY,
            float centerX,
            float centerY,
            boolean fromUser) {
        final ScalingStrategy scalingStrategy = getScalingStrategy();
        final PanningStrategy panningStrategy = getPanningStrategy();

        mTempMatrix.set(getBaseImageMatrix());
        mTempMatrix.getValues(mMatrixValues);
        mMatrixValues[Matrix.MSCALE_X] = scalingStrategy.resolveScaleX(scaleX, fromUser);
        mMatrixValues[Matrix.MSCALE_Y] = scalingStrategy.resolveScaleY(scaleY, fromUser);
        mTempMatrix.setValues(mMatrixValues);

        // Convert center % into points in the transformed image rect
        mPts[0] = getDrawableIntrinsicWidth() * centerX;
        mPts[1] = getDrawableIntrinsicHeight() * centerY;
        mTempMatrix.mapPoints(mPts);

        // Move the actual matrix
        mTempSrc.set(0.0f, 0.0f, getDrawableIntrinsicWidth(), getDrawableIntrinsicHeight());
        mTempMatrix.mapRect(mTempDst, mTempSrc);
        final int availableWidth = getAvailableWidth();
        final int availableHeight = getAvailableHeight();
        final float transX = mMatrixValues[Matrix.MTRANS_X] + availableWidth * 0.5f - mPts[0];
        final float transY = mMatrixValues[Matrix.MTRANS_Y] + availableHeight * 0.5f - mPts[1];
        final float clampedTransX = clampTrans(
                transX,
                availableWidth,
                mTempDst.width(),
                mScaleType,
                ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL);
        final float clampedTransY = clampTrans(
                transY,
                availableHeight,
                mTempDst.height(),
                mScaleType,
                false);
        mMatrixValues[Matrix.MTRANS_X] =
                panningStrategy.resolveTransX(transX, clampedTransX, fromUser);
        mMatrixValues[Matrix.MTRANS_Y] =
                panningStrategy.resolveTransY(transY, clampedTransY, fromUser);
        mTempMatrix.setValues(mMatrixValues);

        if (ScaleType.MATRIX != super.getScaleType()) {
            super.setScaleType(ScaleType.MATRIX);
        }
        super.setImageMatrix(mTempMatrix);
        invalidate();
    }

    private float clampTrans(
            float trans,
            int availableSize,
            float size,
            ScaleType scaleType,
            boolean reverse) {
        final float diff = availableSize - size;
        if (diff <= 0.0f) {
            return MathUtils.clamp(trans, diff, 0.0f);
        } else switch (scaleType) {
            case FIT_START:
                return (reverse ? diff : 0.0f);
            case FIT_END:
                return (reverse ? 0.0f : diff);
            default:
                return diff * 0.5f;
        }
    }

    private boolean drawableHasIntrinsicSize() {
        return getDrawableIntrinsicWidth() > 0 && getDrawableIntrinsicHeight() > 0;
    }

    private int getAvailableHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private int getAvailableWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    @SuppressWarnings("SpellCheckingInspection")
    private Matrix getBaseImageMatrix() {
        synchronized (mBaseImageMatrix) {
            if (mBaseImageMatrixDirty) {
                mBaseImageMatrixDirty = false;

                // See ImageView#configureBounds() for the basis for the following logic.
                if (!drawableHasIntrinsicSize()) {
                    // If the drawable has no intrinsic size, then we just fill our entire view.
                    mBaseImageMatrix.reset();
                } else {
                    // We need to do the scaling ourselves.
                    final int dwidth = getDrawableIntrinsicWidth();
                    final int dheight = getDrawableIntrinsicHeight();

                    final int vwidth = getAvailableWidth();
                    final int vheight = getAvailableHeight();

                    final boolean fits = (dwidth < 0 || vwidth == dwidth)
                            && (dheight < 0 || vheight == dheight);

                    if (ScaleType.MATRIX == mScaleType) {
                        // Use the specified matrix as-is.
                        mBaseImageMatrix.set(getImageMatrix());
                    } else if (fits) {
                        // The bitmap fits exactly, no transform needed.
                        mBaseImageMatrix.reset();
                    } else if (ScaleType.CENTER == mScaleType) {
                        // Center bitmap in view, no scaling.
                        mBaseImageMatrix.setTranslate(
                                Math.round((vwidth - dwidth) * 0.5f),
                                Math.round((vheight - dheight) * 0.5f));
                    } else if (ScaleType.CENTER_CROP == mScaleType) {
                        float scale;
                        float dx = 0, dy = 0;

                        if (dwidth * vheight > vwidth * dheight) {
                            scale = (float) vheight / (float) dheight;
                            dx = (vwidth - dwidth * scale) * 0.5f;
                        } else {
                            scale = (float) vwidth / (float) dwidth;
                            dy = (vheight - dheight * scale) * 0.5f;
                        }

                        mBaseImageMatrix.setScale(scale, scale);
                        mBaseImageMatrix.postTranslate(Math.round(dx), Math.round(dy));
                    } else if (ScaleType.CENTER_INSIDE == mScaleType) {
                        float scale;
                        float dx;
                        float dy;

                        if (dwidth <= vwidth && dheight <= vheight) {
                            scale = 1.0f;
                        } else {
                            scale = Math.min((float) vwidth / (float) dwidth,
                                    (float) vheight / (float) dheight);
                        }

                        dx = Math.round((vwidth - dwidth * scale) * 0.5f);
                        dy = Math.round((vheight - dheight * scale) * 0.5f);

                        mBaseImageMatrix.setScale(scale, scale);
                        mBaseImageMatrix.postTranslate(dx, dy);
                    } else {
                        // Generate the required transform.
                        mTempSrc.set(0, 0, dwidth, dheight);
                        mTempDst.set(0, 0, vwidth, vheight);

                        mBaseImageMatrix.setRectToRect(
                                mTempSrc,
                                mTempDst,
                                scaleTypeToScaleToFit(mScaleType));
                    }
                }
            }
        }
        return mBaseImageMatrix;
    }

    private int getDrawableIntrinsicHeight() {
        final Drawable dr = getDrawable();
        return (dr == null ? -1 : dr.getIntrinsicHeight());
    }

    private int getDrawableIntrinsicWidth() {
        final Drawable dr = getDrawable();
        return (dr == null ? -1 : dr.getIntrinsicWidth());
    }

    private void getImageCenter() {
        if (mImageCenterDirty) {
            // TODO synchronized?
            if (ViewCompat.isLaidOut(this)) {
                mImageCenterDirty = false;
                getImageMatrixInternal().invert(mTempMatrix);
                mPts[0] = getAvailableWidth() * 0.5f;
                mPts[1] = getAvailableHeight() * 0.5f;
                mTempMatrix.mapPoints(mPts);
                mImageCenterX = mPts[0] / getDrawableIntrinsicWidth();
                mImageCenterY = mPts[1] / getDrawableIntrinsicHeight();
            }
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    private Matrix getImageMatrixInternal() {
        final Matrix matrix = super.getImageMatrix();
        if (ScaleType.FIT_XY == super.getScaleType()) {
            if (drawableHasIntrinsicSize()) {
                mTempSrc.set(
                        0,
                        0,
                        getDrawableIntrinsicWidth(),
                        getDrawableIntrinsicHeight());
                mTempDst.set(
                        0,
                        0,
                        getAvailableWidth(),
                        getAvailableHeight());
                matrix.setRectToRect(mTempSrc, mTempDst, ScaleToFit.FILL);
            }
        }
        return matrix;
    }

    private void getImageScale() {
        if (mImageScaleDirty) {
            if (ViewCompat.isLaidOut(this)) {
                mImageScaleDirty = false;
                final Matrix matrix = getImageMatrixInternal();
                matrix.getValues(mMatrixValues);
                mImageScaleX = mMatrixValues[Matrix.MSCALE_X];
                mImageScaleY = mMatrixValues[Matrix.MSCALE_Y];
            }
        }
    }

    private void getTransformedImageRect(RectF outRect) {
        getTransformedImageRect(getImageMatrixInternal(), outRect);
    }

    private void getTransformedImageRect(Matrix matrix, RectF outRect) {
        if (matrix != null && outRect != null) {
            outRect.set(0, 0, getDrawableIntrinsicWidth(), getDrawableIntrinsicHeight());
            matrix.mapRect(outRect);
        }
    }

    private static ScaleToFit scaleTypeToScaleToFit(@NonNull ScaleType scaleType) {
        switch (scaleType) {
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
