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
import android.support.annotation.Px;
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

    //region Nested classes
    public interface OnDrawListener {
        void onDraw(InteractiveImageView view, Canvas canvas);
    }

    public interface ScalingStrategy {
        float getMaxScaleX();
        float getMaxScaleY();
        float getMaxSizeX();
        float getMaxSizeY();
        float getMinScaleX();
        float getMinScaleY();
        float getMinSizeX();
        float getMinSizeY();
        void invalidateMaxScale();
        void invalidateMaxSize();
        void invalidateMinScale();
        void invalidateMinSize();
    }

    private class DefaultScalingStrategy implements ScalingStrategy {
        static final float BASE_MULTIPLIER = 3.0f;
        static final float UPPER_MULTIPLIER = 5.0f;

        private final DisplayMetrics mDisplayMetrics;
        private final int mDisplayWidth;
        private final int mDisplayLength;

        private final PointF mMaxScale = new PointF(1.0f, 1.0f);
        private final Point mMaxSize = new Point();
        private final PointF mMinScale = new PointF(1.0f, 1.0f);
        private final Point mMinSize = new Point();

        private boolean mMaxScaleDirty;
        private boolean mMinScaleDirty;
        private boolean mMaxSizeDirty;
        private boolean mMinSizeDirty;

        public DefaultScalingStrategy() {
            super();
            mDisplayMetrics = getResources().getDisplayMetrics();
            mDisplayWidth = Math.min(mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
            mDisplayLength = Math.max(mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
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
        public float getMaxSizeX() {
            return getMaxSize().x;
        }

        @Override
        public float getMaxSizeY() {
            return getMaxSize().y;
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
        public float getMinSizeX() {
            return 0;
        }

        @Override
        public float getMinSizeY() {
            return 0;
        }

        @Override
        public void invalidateMaxScale() {
            mMaxScaleDirty = true;
        }

        @Override
        public void invalidateMaxSize() {
            mMaxSizeDirty = true;
        }

        @Override
        public void invalidateMinScale() {
            mMinSizeDirty = true;
        }

        @Override
        public void invalidateMinSize() {
            mMinSizeDirty = true;
        }

        private synchronized PointF getMaxScale() {
            if (mMaxScaleDirty) {
                mMaxScaleDirty = false;
                if (getIntrinsicImageSize(mPoint)) {
                    final float baseScale = BASE_MULTIPLIER *
                            (float) mDisplayWidth / Math.min(mPoint.x, mPoint.y);
                    final float upperScale = UPPER_MULTIPLIER *
                            (float) mDisplayLength / Math.max(mPoint.x, mPoint.y);
                    final int correspondingDimension = getCorrespondingDimension(mPoint, true);
                    final float lowerScale = (float) correspondingDimension /
                            Math.min(mPoint.x, mPoint.y);
                    final float scale = Math.max(Math.min(baseScale, upperScale), lowerScale);
                    mMaxScale.set(scale, scale);
                } else {
                    mMaxScale.x = mMaxScale.y = 1.0f;
                }
            }
            return mMaxScale;
        }

        private synchronized Point getMaxSize() {
            if (mMaxSizeDirty) {
                mMaxSizeDirty = false;
                if (getIntrinsicImageSize(mPoint)) {
                    getImageMatrix().getValues(mMatrixValues); // TODO FIT_XY?
                    final int imageWidth = Math.round(mPoint.x * mMatrixValues[MSCALE_X]);
                    final int imageHeight = Math.round(mPoint.y * mMatrixValues[MSCALE_Y]);
                    final int imageBreadth = Math.min(imageWidth, imageHeight);
                    final int imageLength = Math.max(imageWidth, imageHeight);
                    final float desiredScale = BASE_MULTIPLIER * mDisplayWidth / imageBreadth;
                    final float upperScale = UPPER_MULTIPLIER * mDisplayLength / imageLength;
                    final float lowerScale =
                            (float) getCorrespondingDimension(mPoint, true) / imageBreadth;
                    final float constrainedScale = Math.max(
                            Math.min(desiredScale, upperScale),
                            lowerScale);
                    mMaxSize.set(
                            Math.round(constrainedScale * imageWidth),
                            Math.round(constrainedScale * imageHeight));
                    // TODO TEMP
                    Log.d(
                            TAG,
                            String.format(
                                    Locale.US,
                                    "Image Size=%dx%d, Displayed Size=%dx%d, desiredScale=%.2f, upperScale=%.2f, lowerScale=%.2f, constrainedScale=%.2f, Max Size=%dx%d",
                                    mPoint.x, mPoint.y,
                                    imageWidth, imageHeight,
                                    desiredScale,
                                    upperScale,
                                    lowerScale,
                                    constrainedScale,
                                    mMaxSize.x, mMaxSize.y));
                    // END TEMP
                }
            }
            return mMaxSize;
        }

        private synchronized PointF getMinScale() {
            if (mMinScaleDirty) {
                mMinScaleDirty = false;
                if (!getIntrinsicImageSize(mPoint) || mScaleType == ScaleType.CENTER) {
                    mMinScale.set(1.0f, 1.0f);
                } else if (mScaleType == ScaleType.MATRIX) {
                    getImageMatrix().getValues(mMatrixValues);
                    mMinScale.set(mMatrixValues[MSCALE_X], mMatrixValues[MSCALE_Y]);
                } else {
                    final float scaleX = (float) getAvailableWidth() / mPoint.x;
                    final float scaleY = (float) getAvailableHeight() / mPoint.y;
                    final float max = Math.max(scaleX, scaleY);
                    final float min = Math.min(scaleX, scaleY);
                    switch (mScaleType) {
                        case CENTER_CROP:
                            mMinScale.set(max, max);
                            break;
                        case CENTER_INSIDE:
                            final float scale = Math.min(min, 1.0f);
                            mMinScale.set(scale, scale);
                            break;
                        case FIT_CENTER:
                        case FIT_END:
                        case FIT_START:
                            mMinScale.set(min, min);
                            break;
                        case FIT_XY:
                            mMinScale.set(scaleX, scaleY);
                            break;
                    }
                }
            }
            return mMinScale;
        }

        private synchronized Point getMinSize() {
            return null;
        }

        private int getCorrespondingDimension(Point point, boolean smaller) {
            if (smaller) {
                if (point.x < point.y) {
                    return getAvailableWidth();
                } else if (point.x > point.y) {
                    return getAvailableHeight();
                } else {
                    return Math.min(getAvailableWidth(), getAvailableHeight());
                }
            } else {
                if (point.x > point.y) {
                    return getAvailableWidth();
                } else if (point.x < point.y) {
                    return getAvailableHeight();
                } else {
                    return Math.max(getAvailableWidth(), getAvailableHeight());
                }
            }
        }
    }
    //endregion Nested classes

    //region Constants
    private static final String TAG = "tag_" + InteractiveImageView.class.getSimpleName();
    //endregion Constants

    //region Fields
    private ScaleType mScaleType;

    private final Point mPoint = new Point();
    private final PointF mPointF = new PointF();
    private final RectF mRectF = new RectF();
    private final float[] mMatrixValues = new float[9];
    private final float[] mSrcPoints = new float[2];
    private final float[] mDstPoints = new float[2];
    private final Matrix mInverseMatrix = new Matrix();
    private final Object mLock = new Object();

    private ScalingStrategy mScalingStrategy;

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
    public ScaleType getScaleType() {
        return mScaleType;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mOnDrawListener != null) {
            mOnDrawListener.onDraw(this, canvas);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidateScalingStrategy();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        super.setScaleType(mScaleType);
        invalidateScalingStrategy();
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        super.setImageMatrix(matrix);
        invalidateScalingStrategy();
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
        mScaleType = scaleType;
        super.setScaleType(scaleType);
    }
    //endregion Inherited methods

    //region Methods
    public boolean getDisplayedImageSize(@NonNull Point outPoint) {
        getImageMatrix().getValues(mMatrixValues);
        return getImageSizeAtScale(mMatrixValues[MSCALE_X], mMatrixValues[MSCALE_Y], outPoint);
    }

    public boolean getImageSizeAtScale(float scaleX, float scaleY, @NonNull Point outPoint) {
        boolean retVal = true;
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            outPoint.set(-1, -1);
            retVal = false;
        } else {
            final int intrinsicWidth = drawable.getIntrinsicWidth();
            if (intrinsicWidth > 0) {
                outPoint.x = Math.round(intrinsicWidth * scaleX);
            } else {
                outPoint.x = -1;
                retVal = false;
            }
            final int intrinsicHeight = drawable.getIntrinsicHeight();
            if (intrinsicHeight > 0) {
                outPoint.y = Math.round(intrinsicHeight * scaleY);
            } else {
                outPoint.y = -1;
                retVal = false;
            }
        }
        return retVal;
    }

    public boolean getIntrinsicImageSize(@NonNull Point outPoint) {
        return getImageSizeAtScale(1.0f, 1.0f, outPoint);
    }

    public float getMaxScaleX() {
        return getScalingStrategy().getMaxScaleX();
    }

    public float getMaxScaleY() {
        return getScalingStrategy().getMaxScaleY();
    }

    public float getMaxSizeX() {
        return getScalingStrategy().getMaxSizeX();
    }

    public float getMaxSizeY() {
        return getScalingStrategy().getMaxSizeY();
    }

    public float getMinScaleX() {
        return getScalingStrategy().getMinScaleX();
    }

    public float getMinScaleY() {
        return getScalingStrategy().getMinScaleY();
    }

    public float getMinSizeX() {
        return getScalingStrategy().getMinSizeX();
    }

    public float getMinSizeY() {
        return getScalingStrategy().getMinSizeY();
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

    public void invalidateScalingStrategy() {
        final ScalingStrategy scalingStrategy = getScalingStrategy();
        scalingStrategy.invalidateMaxScale();
        scalingStrategy.invalidateMinScale();
        scalingStrategy.invalidateMaxSize();
        scalingStrategy.invalidateMinSize();
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

    public void setScalingStrategy(ScalingStrategy scalingStrategy) {
        mScalingStrategy = scalingStrategy;
    }
    //endregion Methods

    //region Private methods
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
    //endregion Private methods
}
