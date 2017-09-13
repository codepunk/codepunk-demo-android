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
import android.util.Size;
import android.view.Display;
import android.view.WindowManager;

import com.codepunk.demo.support.DisplayCompat;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
        public void invalidateMaxScale() {
            mMaxScaleDirty = true;
        }

        @Override
        public void invalidateMinScale() {
            mMinScaleDirty = true;
        }

        private synchronized PointF getMaxScale() {
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
                    // TODO Maybe consolidate the above into a fit inside based on scale type thing?
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
            return mMaxScale;
        }

        private synchronized PointF getMinScale() {
            if (mMinScaleDirty) {
                mMinScaleDirty = false;
                getScaleForScaleType(mScaleType, mMinScale);
            }
            return mMinScale;
        }

        private synchronized Point getMinSize() {
            return null;
        }

        private int getCorrespondingDimension(Point point) {
            if (point.x < point.y) {
                return getAvailableWidth();
            } else if (point.x > point.y) {
                return getAvailableHeight();
            } else {
                return Math.min(getAvailableWidth(), getAvailableHeight());
            }
        }

        private void getSmallestMaxScaleBasedOnView(PointF outPoint) {
            if (getIntrinsicImageSize(mPoint)) {
                getImageMatrix().getValues(mMatrixValues);
                final int displayedWidth = Math.round(mPoint.x * mMatrixValues[MSCALE_X]);
                final int displayedHeight = Math.round(mPoint.y * mMatrixValues[MSCALE_Y]);
                final float scale;
                if (displayedWidth < displayedHeight) {
                    scale = (float) getAvailableWidth() / displayedWidth;
                } else if (displayedWidth > displayedHeight) {
                    scale = (float) getAvailableHeight() / displayedHeight;
                } else {
                    scale = (float) Math.min(getAvailableWidth(), getAvailableHeight()) /
                            displayedWidth;
                }
                outPoint.set(scale * mMatrixValues[MSCALE_X], scale * mMatrixValues[MSCALE_Y]);
            } else {
                outPoint.set(1.0f, 1.0f);
            }
        }

        private void getScaleForScaleType(ScaleType scaleType, PointF outPoint) {
            if (!getIntrinsicImageSize(mPoint) || scaleType == ScaleType.CENTER) {
                outPoint.set(1.0f, 1.0f);
            } else if (scaleType == ScaleType.MATRIX) {
                getImageMatrix().getValues(mMatrixValues);
                outPoint.set(mMatrixValues[MSCALE_X], mMatrixValues[MSCALE_Y]);
            } else {
                final float scaleX = (float) getAvailableWidth() / mPoint.x;
                final float scaleY = (float) getAvailableHeight() / mPoint.y;
                final float max = Math.max(scaleX, scaleY);
                final float min = Math.min(scaleX, scaleY);
                switch (mScaleType) {
                    case CENTER_CROP:
                        outPoint.set(max, max);
                        break;
                    case CENTER_INSIDE:
                        final float scale = Math.min(min, 1.0f);
                        outPoint.set(scale, scale);
                        break;
                    case FIT_CENTER:
                    case FIT_END:
                    case FIT_START:
                        outPoint.set(min, min);
                        break;
                    case FIT_XY:
                        outPoint.set(scaleX, scaleY);
                        break;
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
        mScaleType = scaleType;
        super.setScaleType(scaleType);
        invalidateScalingStrategy();
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

    public float getMinScaleX() {
        return getScalingStrategy().getMinScaleX();
    }

    public float getMinScaleY() {
        return getScalingStrategy().getMinScaleY();
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
                    getImageMatrix().getValues(mMatrixValues); // TODO FIT_XY ?
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

            super.setImageMatrix(matrix);
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
