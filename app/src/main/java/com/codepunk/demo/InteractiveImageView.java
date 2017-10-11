package com.codepunk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.codepunk.demo.support.DisplayCompat;

import static android.graphics.Matrix.MSCALE_X;
import static android.graphics.Matrix.MSCALE_Y;
import static android.widget.ImageView.ScaleType.FIT_XY;
import static android.widget.ImageView.ScaleType.MATRIX;

// TODO NEXT SavedState, restore relative center & scale on configuration change

@SuppressWarnings({"unused", "WeakerAccess"})
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
        private final RectF mSrcRectF = new RectF();
        private final RectF mDstRectF = new RectF();

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

    /*
    private static class SavedState extends View.BaseSavedState {
        //region Nested classes
        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        //endregion Nested classes

        //region Fields
        private boolean hasCustomPlacement;
        private ScaleType savedScaleType;
        private float scaleX;
        private float scaleY;
        private float relativeCenterX;
        private float relativeCenterY;
        //endregion Fields

        //region Constructors
        public SavedState(Parcel source) {
            super(source);
        }

        @TargetApi(Build.VERSION_CODES.N)
        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            hasCustomPlacement = (source.readByte() != (byte) 0);
            savedScaleType = (ScaleType) source.readSerializable();
            scaleX = source.readFloat();
            scaleY = source.readFloat();
            relativeCenterX = source.readFloat();
            relativeCenterY = source.readFloat();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }
        //endregion Constructors

        //region Inherited methods
        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (hasCustomPlacement ? 1 : 0));
            out.writeSerializable(savedScaleType);
            out.writeFloat(scaleX);
            out.writeFloat(scaleY);
            out.writeFloat(relativeCenterX);
            out.writeFloat(relativeCenterY);
        }
        //endregion Inherited methods
    }
    */
    //endregion Nested classes

    //region Constants
    private static final String TAG = "tag_" + InteractiveImageView.class.getSimpleName();
    //endregion Constants

    //region Fields
    private ScaleType mScaleType;

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

    /* private SavedState mPendingSavedState; */
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
    public Matrix getImageMatrix() {
        final Matrix imageMatrix = super.getImageMatrix();
        if (super.getScaleType() == ScaleType.FIT_XY) {
            imageMatrix.reset();
            getIntrinsicImageSize(mPoint);
            mSrcRect.set(0, 0, mPoint.x, mPoint.y);
            mDstRect.set(0, 0, getAvailableWidth(), getAvailableHeight());
            GraphicsUtils.scale(mSrcRect, mDstRect, mScaleType, mPointF);
            imageMatrix.getValues(mMatrixValues);
            mMatrixValues[MSCALE_X] = mPointF.x;
            mMatrixValues[MSCALE_Y] = mPointF.y;
            imageMatrix.setValues(mMatrixValues);
        }
        return imageMatrix;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO Anywhere else I can do this? *STILL NOT WORKING*
        /*
        if (mPendingSavedState != null) {
            if (mPendingSavedState.hasCustomPlacement) {
                setScaleType(mPendingSavedState.savedScaleType);
                setRelativeCenter(mPendingSavedState.relativeCenterX, mPendingSavedState.relativeCenterY);
                setScale(mPendingSavedState.scaleX, mPendingSavedState.scaleY);
            }
            mPendingSavedState = null;
        }
        */

        if (mOnDrawListener != null) {
            mOnDrawListener.onDraw(this, canvas);
        }
    }

    /*
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Log.i(TAG, "onRestoreInstanceState");

        if (!state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        mPendingSavedState = (SavedState) state;
        super.onRestoreInstanceState(mPendingSavedState.getSuperState());
    }
    */

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidateScalingStrategy();
    }

    /*
    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);

        // TODO Determine if we have custom placement
        state.hasCustomPlacement = hasCustomPlacement();

        //state.hasPureScaleType = false; // TODO If size is equal to default size for scale type
        if (state.hasCustomPlacement) {
            state.savedScaleType = mScaleType;
            getScale(mPointF);
            state.scaleX = mPointF.x;
            state.scaleY = mPointF.y;
            getRelativeCenter(mPointF);
            state.relativeCenterX = mPointF.x;
            state.relativeCenterY = mPointF.y;
        }
        return state;
    }

    @Override
    public boolean performClick() {
        final boolean retVal = super.performClick();

        // TODO Where can I restore this so it actually works?
        if (mPendingSavedState != null) {
            if (mPendingSavedState.hasCustomPlacement) {
                setScaleType(mPendingSavedState.savedScaleType);
                setRelativeCenter(mPendingSavedState.relativeCenterX, mPendingSavedState.relativeCenterY);
                setScale(mPendingSavedState.scaleX, mPendingSavedState.scaleY);
            }
            mPendingSavedState = null;
        }

        return retVal;
    }
    */

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        Log.i(TAG, "setImageDrawable");
        super.setImageDrawable(drawable);
        super.setScaleType(mScaleType);
        invalidateScalingStrategy();
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        Log.i(TAG, "setImageMatrix");
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
            Log.i(TAG, "setScaleType: scaleType=" + scaleType);
            mScaleType = scaleType;
            invalidateScalingStrategy();
        }
    }
    //endregion Inherited methods

    //region Methods
    public boolean getDisplayedImageSize(@NonNull Point outPoint) {
        getImageMatrix().getValues(mMatrixValues);
        return getImageSizeAtScale(mMatrixValues[MSCALE_X], mMatrixValues[MSCALE_Y], outPoint);
    }

    public boolean getDisplayedImageSize(@NonNull PointF outPoint) {
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

    public boolean getImageSizeAtScale(float scaleX, float scaleY, @NonNull PointF outPoint) {
        boolean retVal = true;
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            outPoint.set(-1.0f, -1.0f);
            retVal = false;
        } else {
            final int intrinsicWidth = drawable.getIntrinsicWidth();
            if (intrinsicWidth > 0) {
                outPoint.x = intrinsicWidth * scaleX;
            } else {
                outPoint.x = -1.0f;
                retVal = false;
            }
            final int intrinsicHeight = drawable.getIntrinsicHeight();
            if (intrinsicHeight > 0) {
                outPoint.y = intrinsicHeight * scaleY;
            } else {
                outPoint.y = -1.0f;
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
                    getImageMatrix().getValues(mMatrixValues);
                    outPoint.x = mMatrixValues[MSCALE_X];
                    outPoint.y = mMatrixValues[MSCALE_Y];
                    return true;
                }
            }
        }
        return false;
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
                internalSetScaleType(MATRIX);
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
                internalSetScaleType(MATRIX);
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

    private boolean hasIntrinsicSize() {
        final Drawable d = getDrawable();
        if (d == null) {
            return false;
        }
        return (d.getIntrinsicWidth() > 0 && d.getIntrinsicWidth() > 0);
    }

    private void internalSetScaleType(ScaleType scaleType) {
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
