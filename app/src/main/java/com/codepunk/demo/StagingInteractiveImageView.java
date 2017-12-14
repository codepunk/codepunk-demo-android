package com.codepunk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

// TODO NEXT Continue on applyPlacement
public class StagingInteractiveImageView extends AppCompatImageView {
    //region Nested classes
    public interface OnDrawListener {
        void onDraw(StagingInteractiveImageView view, Canvas canvas);
    }
    //endregion Nested classes

    //region Constants
    private static final String TAG = StagingInteractiveImageView.class.getSimpleName();
    //endregion Constants

    //region Fields
    private static Map<ScaleType, ScaleToFit> sScaleMap;

    private ScaleType mScaleType;
    private OnDrawListener mOnDrawListener;
    private float mImageScaleX = 1.0f;
    private float mImageScaleY = 1.0f;
    private float mImageCenterX = 0.5f;
    private float mImageCenterY = 0.5f;

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
            applyPlacement(mImageScaleX, mImageScaleY, mImageCenterX, mImageCenterY);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBaseImageMatrixDirty = true;
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        mImageCenterDirty = true;
        mImageScaleDirty = true;
        mBaseImageMatrixDirty = true;
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        super.setImageMatrix(matrix);
        mImageCenterDirty = true;
        mImageScaleDirty = true;
        if (ScaleType.MATRIX == mScaleType) {
            mBaseImageMatrix.set(matrix);
            mBaseImageMatrixDirty = false;
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        mBaseImageMatrixDirty = true;
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        super.setPaddingRelative(start, top, end, bottom);
        mBaseImageMatrixDirty = true;
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
        return 5.0f;
    }

    public float getMaxScaleY() {
        return 5.0f;
    }

    public float getMinScaleX() {
        return 0.5f;
    }

    public float getMinScaleY() {
        return 0.5f;
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
                applyPlacement(scaleX, scaleY, centerX, centerY);
                invalidate();
            } else {
                mPlacementDirty = true;
            }
        }
    }
    //endregion Methods

    //region Private methods
    private void applyPlacement(float scaleX, float scaleY, float centerX, float centerY) {
        // TODO Get clamped scale from scaling strategy
        // First, we can clamp scale (this is based on mBaseImageMatrix)
        //final float clampedScaleX = scaleX; //MathUtils.clamp(scaleX, getMinScaleX(), getMaxScaleX());
        //final float clampedScaleY = scaleY; //MathUtils.clamp(scaleY, getMinScaleY(), getMaxScaleY());

        final int intrinsicWidth = getDrawableIntrinsicWidth();
        final int intrinsicHeight = getDrawableIntrinsicHeight();

        // First, let's get the requested scale into the matrix:
        mTempMatrix.set(getBaseImageMatrix());
        mTempMatrix.getValues(mMatrixValues);
        mMatrixValues[Matrix.MSCALE_X] = scaleX; //clampedScaleX;
        mMatrixValues[Matrix.MSCALE_Y] = scaleY; //clampedScaleY;
        mTempMatrix.setValues(mMatrixValues);

        // Second, get the size of the resulting rectangle:
        /*
        mTempSrc.set(0.0f, 0.0f, intrinsicWidth, intrinsicHeight);
        imageMatrix.mapRect(mTempDst, mTempSrc);
        */

        // Last, convert center % into points in the transformed image rect
        mPts[0] = intrinsicWidth * centerX;
        mPts[1] = intrinsicHeight * centerY;
        mTempMatrix.mapPoints(mPts);

        // Move the actual matrix
        final int availableWidth = getWidth() - getPaddingLeft() - getPaddingBottom();
        final int availableHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        final float transX;
        final float transY;

        // TODO Account for scaleType in placement (get this from panning strategy)
        transX = (availableWidth * 0.5f) - mPts[0];
        transY = (availableHeight * 0.5f) - mPts[1];

        mMatrixValues[Matrix.MTRANS_X] = transX;
        mMatrixValues[Matrix.MTRANS_Y] = transY;
        mTempMatrix.setValues(mMatrixValues);

        if (ScaleType.MATRIX != super.getScaleType()) {
            super.setScaleType(ScaleType.MATRIX);
        }
        super.setImageMatrix(mTempMatrix);
        invalidate();
    }

    private boolean drawableHasIntrinsicSize() {
        final Drawable dr = getDrawable();
        return dr != null && (dr.getIntrinsicWidth() > 0 && dr.getIntrinsicHeight() > 0);
    }

    @SuppressWarnings("SpellCheckingInspection")
    private Matrix getBaseImageMatrix() {
        synchronized (mBaseImageMatrix) {
            if (mBaseImageMatrixDirty) {
                mBaseImageMatrixDirty = false;

                Log.d(TAG, "StagingInteractiveImageView#getBaseImageMatrix: ");

                // See ImageView#configureBounds() for the basis for the following logic.
                if (!drawableHasIntrinsicSize()) {
                    // If the drawable has no intrinsic size, then we just fill our entire view.
                    mBaseImageMatrix.reset();
                } else {
                    // We need to do the scaling ourselves.
                    final int dwidth = getDrawableIntrinsicWidth();
                    final int dheight = getDrawableIntrinsicHeight();

                    final int vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
                    final int vheight = getHeight() - getPaddingTop() - getPaddingBottom();

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
            if (ViewCompat.isLaidOut(this)) {
                mImageCenterDirty = false;
                final Matrix matrix = getImageMatrixInternal();
                matrix.invert(matrix);
                mPts[0] = (getWidth() - getPaddingLeft() - getPaddingRight()) * 0.5f;
                mPts[1] = (getHeight() - getPaddingTop() - getPaddingBottom()) * 0.5f;
                matrix.mapPoints(mPts);
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
                        getWidth() - getPaddingLeft() - getPaddingRight(),
                        getHeight() - getPaddingTop() - getPaddingBottom());
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

    private static ScaleToFit scaleTypeToScaleToFit(ScaleType scaleType) {
        if (sScaleMap == null) {
            sScaleMap = new HashMap<>(4);
            sScaleMap.put(ScaleType.FIT_CENTER, ScaleToFit.CENTER);
            sScaleMap.put(ScaleType.FIT_END, ScaleToFit.END);
            sScaleMap.put(ScaleType.FIT_START, ScaleToFit.START);
            sScaleMap.put(ScaleType.FIT_XY, ScaleToFit.FILL);
        }
        return sScaleMap.get(scaleType);
    }
    //endregion Private methods
}
