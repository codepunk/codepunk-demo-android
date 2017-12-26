package com.codepunk.demo.interactiveimageview.version4;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.WindowManager;
import android.widget.ImageView;

import com.codepunk.demo.support.DisplayCompat;

import static com.codepunk.demo.R.attr.interactiveImageViewStyle;
import static com.codepunk.demo.R.styleable.InteractiveImageView;
import static com.codepunk.demo.R.styleable.InteractiveImageView_panEnabled;
import static com.codepunk.demo.R.styleable.InteractiveImageView_zoomEnabled;

/*
 * TODO When moving image, set mImageCenter to null
 * TODO When sizing image, set mImageSize to null
 */
public class InteractiveImageView extends AppCompatImageView {
    //region Nested classes
    protected class CustomOnGestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            // TODO EDGE_EFFECTS releaseEdgeEffects();
            // TODO OVERSCROLLER mScroller.forceFinished(true);
            ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            synchronized (mLock) {
                // TODO clamp? Clamping is already happening in setImageScaleInternal so maybe not necessary?
                mImageMatrix.set(getImageMatrixInternal());
                mImageMatrix.getValues(mMatrixValues);
                mMatrixValues[Matrix.MTRANS_X] -= distanceX;
                mMatrixValues[Matrix.MTRANS_Y] -= distanceY;
                mImageMatrix.setValues(mMatrixValues);

                final PointF center = new PointF();
                getActualImageCenter(mImageMatrix, center);
                mImageScale = null;
                mImageCenter = null;
                mActualImageScaleDirty = true;
                mActualImageCenterDirty = true;
                setImageScaleInternal(
                        mMatrixValues[Matrix.MSCALE_X],
                        mMatrixValues[Matrix.MSCALE_Y],
                        center.x,
                        center.y,
                        true);
            }
            return true;
        }
    }

    protected static class CustomOnScaleGestureListener extends SimpleOnScaleGestureListener {

    }
    //endregion Nested class CustomOnScaleGestureListener

    //region Constants
    static final float BREADTH_MULTIPLIER = 3.0f;
    static final float LENGTH_MULTIPLIER = 5.0f;
    //endregion Constants

    //region Fields
    private ScaleType mScaleType;

    private final Object mLock = new Object();

    private boolean mPanEnabled = false;
    private boolean mZoomEnabled = false;
    private GestureDetectorCompat mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;

    // TODO Examine this, choose order
    private final Matrix mImageMatrix = new Matrix();
    private final Matrix mBaseImageMatrix = new Matrix();
    private final Matrix mTempMatrix = new Matrix();
    private final PointF mImageMaxScale = new PointF();
    private final PointF mImageMinScale = new PointF();
    private final PointF mActualImageCenter = new PointF();
    private final PointF mActualImageScale = new PointF();
    private PointF mImageCenter;
    private PointF mImageScale;
    private final RectF mDstRect = new RectF();
    private final RectF mSrcRect = new RectF();
    private final float[] mMatrixValues = new float[9];
    private final float[] mPts = new float[2];

    private boolean mPlacementDirty = false;
    private boolean mActualImageCenterDirty = true;
    private boolean mActualImageScaleDirty = true;
    private boolean mBaseImageMatrixDirty = true;
    private boolean mImageMaxScaleDirty = true;
    private boolean mImageMinScaleDirty = true;
    //endregion Fields

    //region Constructors
    public InteractiveImageView(Context context) {
        super(context);
        initializeInteractiveImageView(context, null, interactiveImageViewStyle, 0);
    }

    public InteractiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeInteractiveImageView(context, attrs, interactiveImageViewStyle, 0);
    }

    public InteractiveImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeInteractiveImageView(context, attrs, defStyleAttr, 0);
    }
    //endregion Constructors

    //region Inherited methods
    @Override
    public ScaleType getScaleType() {
        return mScaleType;
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);

        // NOTE: We need to perform this here in layout()--and not onLayout()--because
        // calls to ViewCompat.isLaidOut() may still return false from onLayout().
        // (isLaidOut() may be called as a result of calling setImageScaleInternal below.)
        synchronized (mLock) {
            if (mPlacementDirty) {
                mPlacementDirty = false;
                setImageScaleInternal(
                        getImageScaleX(),
                        getImageScaleY(),
                        getImageCenterX(),
                        getImageCenterY(),
                        false);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // TODO Clear (or handle) new center?
        mBaseImageMatrixDirty = true;
        mImageMaxScaleDirty = true;
        mImageMinScaleDirty = true;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean retVal = false;
        if (drawableHasIntrinsicSize()) {
            if (mScaleGestureDetector != null) {
                retVal = mScaleGestureDetector.onTouchEvent(event);
            }
            if (mGestureDetector != null) {
                retVal = mGestureDetector.onTouchEvent(event) || retVal;
            }
        }
        return retVal || super.onTouchEvent(event);
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        mImageCenter = null;
        mImageScale = null;
        mActualImageScaleDirty = true;
        mActualImageCenterDirty = true;
        mBaseImageMatrixDirty = true;
        mImageMaxScaleDirty = true;
        mImageMinScaleDirty = true;
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        super.setImageMatrix(matrix);
        if (ScaleType.MATRIX == mScaleType) {
            mBaseImageMatrix.set(matrix);
            mBaseImageMatrixDirty = false;
            mImageCenter = null;
            mImageScale = null;
            mActualImageScaleDirty = true;
            mActualImageCenterDirty = true;
            mImageMaxScaleDirty = true;
            mImageMinScaleDirty = true;
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        // TODO Clear (or handle) new center?
        mBaseImageMatrixDirty = true;
        mImageMaxScaleDirty = true;
        mImageMinScaleDirty = true;
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        super.setPaddingRelative(start, top, end, bottom);
        // TODO Clear (or handle) new center?
        mBaseImageMatrixDirty = true;
        mImageMaxScaleDirty = true;
        mImageMinScaleDirty = true;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        final ScaleType oldScaleType = super.getScaleType();
        super.setScaleType(scaleType);
        if (oldScaleType != super.getScaleType()) {
            mScaleType = scaleType;
            mImageCenter = null;
            mImageScale = null;
            mActualImageScaleDirty = true;
            mActualImageCenterDirty = true;
            mBaseImageMatrixDirty = true;
            mImageMaxScaleDirty = true;
            mImageMinScaleDirty = true;
        }
    }
    //endregion Inherited methods

    //region Methods
    public float getActualImageCenterX() {
        syncActualImageCenter();
        return mActualImageCenter.x;
    }

    public float getActualImageCenterY() {
        syncActualImageCenter();
        return mActualImageCenter.y;
    }

    public float getActualImageScaleX() {
        syncActualImageScale();
        return mActualImageScale.x;
    }

    public float getActualImageScaleY() {
        syncActualImageScale();
        return mActualImageScale.y;
    }

    public float getImageMaxScaleX() {
        syncImageMaxScale();
        return mImageMaxScale.x;
    }

    public float getImageMaxScaleY() {
        syncImageMaxScale();
        return mImageMaxScale.y;
    }

    public float getImageMinScaleX() {
        syncImageMinScale();
        return mImageMinScale.x;
    }

    public float getImageMinScaleY() {
        syncImageMinScale();
        return mImageMinScale.y;
    }

    public float getImageCenterX() {
        return (mImageCenter == null ? getActualImageCenterX() : mImageCenter.x);
    }

    public float getImageCenterY() {
        return (mImageCenter == null ? getActualImageCenterY() : mImageCenter.y);
    }

    public float getImageScaleX() {
        return (mImageScale == null ? getActualImageScaleX() : mImageScale.x);
    }

    public float getImageScaleY() {
        return (mImageScale == null ? getActualImageScaleY() : mImageScale.y);
    }

    public boolean hasCustomPlacement() {
        return !getBaseImageMatrix().equals(getImageMatrixInternal());
    }

    public boolean isPanEnabled() {
        return mPanEnabled;
    }

    public boolean isZoomEnabled() {
        return mZoomEnabled;
    }

    public void setImageScale(float sx, float sy) {
        setImageScale(sx, sy, getImageCenterX(), getImageCenterY());
    }

    public void setImageScale(float sx, float sy, float cx, float cy) {
        if (mImageScale == null) {
            mImageScale = new PointF(sx, sy);
        } else {
            mImageScale.set(sx, sy);
        }
        if (mImageCenter == null) {
            mImageCenter = new PointF(cx, cy);
        } else {
            mImageCenter.set(cx, cy);
        }
        if (drawableHasIntrinsicSize()) {
            if (ViewCompat.isLaidOut(this)) {
                mPlacementDirty = false;
                setImageScaleInternal(sx, sy, cx, cy, false);
            } else {
                mPlacementDirty = true;
            }
        }
    }

    public void setPanEnabled(boolean panEnabled) {
        if (mPanEnabled != panEnabled) {
            mPanEnabled = panEnabled;
            if (panEnabled) {
                mGestureDetector =
                        new GestureDetectorCompat(getContext(), new CustomOnGestureListener());
            } else {
                mGestureDetector = null;
            }
        }
    }

    public void setZoomEnabled(boolean zoomEnabled) {
        if (mZoomEnabled != zoomEnabled) {
            mZoomEnabled = zoomEnabled;
            if (zoomEnabled) {
                mScaleGestureDetector =
                        new ScaleGestureDetector(getContext(), new CustomOnScaleGestureListener());
            } else {
                mScaleGestureDetector = null;
            }
        }
    }
    //endregion Methods

    //region Protected methods
    protected float clampScaleX(float sx) {
        return MathUtils.clamp(sx, getImageMinScaleX(), getImageMaxScaleX());
    }

    protected float clampScaleY(float sy) {
        return MathUtils.clamp(sy, getImageMinScaleY(), getImageMaxScaleY());
    }

    protected float clampTransX(float tx, int availableWidth, float displayedWidth) {
        final float diff = availableWidth - displayedWidth;
        if (diff <= 0.0f) {
            return MathUtils.clamp(tx, diff, 0.0f);
        } else {
            final boolean isRtl =
                    ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
            switch (mScaleType) {
                case FIT_START:
                    return (isRtl ? diff : 0.0f);
                case FIT_END:
                    return (isRtl ? 0.0f : diff);
                default:
                    return diff * 0.5f;
            }
        }
    }

    protected float clampTransY(float ty, int availableHeight, float displayedHeight) {
        final float diff = availableHeight - displayedHeight;
        if (diff <= 0.0f) {
            return MathUtils.clamp(ty, diff, 0.0f);
        } else {
            switch (mScaleType) {
                case FIT_START:
                    return 0.0f;
                case FIT_END:
                    return diff;
                default:
                    return diff * 0.5f;
            }
        }
    }

    protected boolean drawableHasIntrinsicSize() {
        return getDrawableIntrinsicWidth() > 0 && getDrawableIntrinsicHeight() > 0;
    }

    protected void getActualImageCenter(@NonNull final PointF outPoint) {
        getActualImageCenter(getImageMatrixInternal(), outPoint);
    }

    protected void getActualImageCenter(@NonNull Matrix matrix, @NonNull final PointF outPoint) {
        synchronized (mLock) {
            matrix.invert(mTempMatrix);
            mPts[0] = getAvailableWidth() * 0.5f;
            mPts[1] = getAvailableHeight() * 0.5f;
            mTempMatrix.mapPoints(mPts);
            outPoint.set(
                    mPts[0] / getDrawableIntrinsicWidth(),
                    mPts[1] / getDrawableIntrinsicHeight());
        }
    }

    protected void getActualImageScale(@NonNull final PointF outPoint) {
        synchronized (mLock) {
            final Matrix imageMatrix = getImageMatrixInternal();
            imageMatrix.getValues(mMatrixValues);
            outPoint.set(
                    mMatrixValues[Matrix.MSCALE_X],
                    mMatrixValues[Matrix.MSCALE_Y]);
        }
    }

    protected int getAvailableHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    protected int getAvailableWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    /**
     * Returns the view's base image matrix. Do not change this matrix in place but make a copy.
     * @return The view's base image matrix
     */
    @SuppressWarnings("SpellCheckingInspection")
    protected Matrix getBaseImageMatrix() {
        synchronized (mLock) {
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
                        mSrcRect.set(0, 0, dwidth, dheight);
                        mDstRect.set(0, 0, vwidth, vheight);

                        mBaseImageMatrix.setRectToRect(
                                mSrcRect,
                                mDstRect,
                                scaleTypeToScaleToFit(mScaleType));
                    }
                }
            }
        }
        return mBaseImageMatrix;
    }

    protected int getDrawableIntrinsicHeight() {
        final Drawable dr = getDrawable();
        return (dr == null ? -1 : dr.getIntrinsicHeight());
    }

    protected int getDrawableIntrinsicWidth() {
        final Drawable dr = getDrawable();
        return (dr == null ? -1 : dr.getIntrinsicWidth());
    }

    protected void getImageMaxScale(@NonNull final PointF outPoint) {
        if (drawableHasIntrinsicSize()) {
            synchronized (mLock) {
                mSrcRect.set(0, 0, getDrawableIntrinsicWidth(), getDrawableIntrinsicHeight());
                getBaseImageMatrix().mapRect(mDstRect, mSrcRect);
                final float baseWidth = mDstRect.width();
                final float baseHeight = mDstRect.height();
                final float baseBreadth = Math.min(baseWidth, baseHeight);
                final float baseLength = Math.max(baseWidth, baseHeight);
                final DisplayMetrics dm = getDisplayMetrics();
                final float maxBreadth =
                        Math.round(BREADTH_MULTIPLIER * Math.min(dm.widthPixels, dm.heightPixels));
                final float maxLength =
                        Math.round(LENGTH_MULTIPLIER * Math.max(dm.widthPixels, dm.heightPixels));
                final float screenBasedScale =
                        Math.min(
                                maxBreadth / baseBreadth,
                                maxLength / baseLength);
                final int availableSize;
                if (baseWidth < baseHeight) {
                    availableSize = getAvailableWidth();
                } else if (baseWidth > baseHeight) {
                    availableSize = getAvailableHeight();
                } else {
                    availableSize = Math.min(getAvailableWidth(), getAvailableHeight());
                }
                final float viewBasedScale = availableSize / baseBreadth;
                final float scale = Math.max(screenBasedScale, viewBasedScale);
                outPoint.set(
                        scale * mMatrixValues[Matrix.MSCALE_X],
                        scale * mMatrixValues[Matrix.MSCALE_Y]);
            }
        } else {
            outPoint.set(1.0f, 1.0f);
        }
    }

    protected void getImageMinScale(@NonNull final PointF outPoint) {
        synchronized (mLock) {
            getBaseImageMatrix().getValues(mMatrixValues);
            outPoint.set(mMatrixValues[Matrix.MSCALE_X], mMatrixValues[Matrix.MSCALE_Y]);
        }
    }

    protected float resolveScaleX(float sx, float clamped, boolean fromUser) {
        return clamped;
    }

    protected float resolveScaleY(float sy, float clamped, boolean fromUser) {
        return clamped;
    }

    protected float resolveTransX(float tx, float clamped, boolean fromUser) {
        return clamped;
    }

    protected float resolveTransY(float ty, float clamped, boolean fromUser) {
        return clamped;
    }

    protected static Matrix.ScaleToFit scaleTypeToScaleToFit(@NonNull ScaleType scaleType) {
        switch (scaleType) {
            case FIT_CENTER:
                return Matrix.ScaleToFit.CENTER;
            case FIT_END:
                return Matrix.ScaleToFit.END;
            case FIT_START:
                return Matrix.ScaleToFit.START;
            case FIT_XY:
                return Matrix.ScaleToFit.FILL;
            default:
                return null;
        }
    }
    //endregion Protected methods

    //region Private methods
    private DisplayMetrics getDisplayMetrics() {
        final Context context = getContext();
        final WindowManager manager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (manager == null) {
            return context.getResources().getDisplayMetrics();
        } else {
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            DisplayCompat.getRealMetrics(manager.getDefaultDisplay(), displayMetrics);
            return displayMetrics;
        }
    }

    /**
     * Returns the view's optional matrix. Similar to {@link ImageView#getImageMatrix()} except
     * that it will also calculate the matrix in the case when the scale type is
     * {@link ScaleType#FIT_XY}. Do not change this matrix in place but make a copy.
     * @return The view's optional matrix
     */
    private Matrix getImageMatrixInternal() {
        mImageMatrix.set(super.getImageMatrix());
        if (ScaleType.FIT_XY == super.getScaleType()) {
            if (drawableHasIntrinsicSize()) {
                mSrcRect.set(
                        0,
                        0,
                        getDrawableIntrinsicWidth(),
                        getDrawableIntrinsicHeight());
                mDstRect.set(0, 0, getAvailableWidth(), getAvailableHeight());
                mImageMatrix.setRectToRect(mSrcRect, mDstRect, Matrix.ScaleToFit.FILL);
            }
        }
        return mImageMatrix;
    }

    @SuppressWarnings("SameParameterValue")
    private void initializeInteractiveImageView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(
                attrs,
                InteractiveImageView,
                defStyleAttr,
                defStyleRes);

        // TODO scaleX / scaleY / centerX / centerY / show drag edges etc.

        setPanEnabled(a.getBoolean(InteractiveImageView_panEnabled, false));
        setZoomEnabled(a.getBoolean(InteractiveImageView_zoomEnabled, false));

        a.recycle();
    }

    private void setImageScaleInternal(float sx, float sy, float cx, float cy, boolean fromUser) {
        synchronized (mLock) {
            mTempMatrix.set(getBaseImageMatrix());
            mTempMatrix.getValues(mMatrixValues);
            mMatrixValues[Matrix.MSCALE_X] = clampScaleX(sx);
            mMatrixValues[Matrix.MSCALE_Y] = clampScaleY(sy);
            mTempMatrix.setValues(mMatrixValues);

            // Convert center % into points in the transformed image rect
            mPts[0] = getDrawableIntrinsicWidth() * cx;
            mPts[1] = getDrawableIntrinsicHeight() * cy;
            mTempMatrix.mapPoints(mPts);

            // Get desired translation
            final float tx = mMatrixValues[Matrix.MTRANS_X] + getAvailableWidth() * 0.5f - mPts[0];
            final float ty = mMatrixValues[Matrix.MTRANS_Y] + getAvailableHeight() * 0.5f - mPts[1];
            mSrcRect.set(
                    0.0f,
                    0.0f,
                    getDrawableIntrinsicWidth(),
                    getDrawableIntrinsicHeight());
            mTempMatrix.mapRect(mDstRect, mSrcRect);
            final float clampedTx = clampTransX(tx, getAvailableWidth(), mDstRect.width());
            final float clampedTy = clampTransY(ty, getAvailableHeight(), mDstRect.height());

            mMatrixValues[Matrix.MTRANS_X] = resolveTransX(tx, clampedTx, fromUser);
            mMatrixValues[Matrix.MTRANS_Y] = resolveTransY(ty, clampedTy, fromUser);
            mTempMatrix.setValues(mMatrixValues);

            if (ScaleType.MATRIX != super.getScaleType()) {
                super.setScaleType(ScaleType.MATRIX);
            }
            super.setImageMatrix(mTempMatrix);
            invalidate();
        }
    }

    protected void syncActualImageCenter() {
        synchronized (mLock) {
            if (mActualImageCenterDirty) {
                if (ViewCompat.isLaidOut(this)) {
                    mActualImageCenterDirty = false;
                    getActualImageCenter(mActualImageCenter);
                }
            }
        }
    }

    protected void syncActualImageScale() {
        synchronized (mLock) {
            if (mActualImageScaleDirty) {
                if (ViewCompat.isLaidOut(this)) {
                    mActualImageScaleDirty = false;
                    getActualImageScale(mActualImageScale);
                }
            }
        }
    }

    protected void syncImageMaxScale() {
        synchronized (mLock) {
            if (mImageMaxScaleDirty) {
                if (ViewCompat.isLaidOut(this)) {
                    mImageMaxScaleDirty = false;
                    getImageMaxScale(mImageMaxScale);
                }
            }
        }
    }

    protected void syncImageMinScale() {
        synchronized (mLock) {
            if (mImageMinScaleDirty) {
                if (ViewCompat.isLaidOut(this)) {
                    mImageMinScaleDirty = false;
                    getImageMinScale(mImageMinScale);
                }
            }
        }
    }
    //endregion Private methods
}
