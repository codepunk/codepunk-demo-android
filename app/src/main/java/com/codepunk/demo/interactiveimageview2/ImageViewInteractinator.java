package com.codepunk.demo.interactiveimageview2;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ArrayRes;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.WindowManager;
import android.widget.EdgeEffect;
import android.widget.OverScroller;

import com.codepunk.demo.R;
import com.codepunk.demo.support.DisplayCompat;

public class ImageViewInteractinator extends AppCompatImageView {

    //region Nested classes

    private interface ImageViewCompatImpl {
        boolean getCropToPadding();
        void setCropToPadding(boolean cropToPadding);
    }

    private static class BaseImageViewCompatImpl implements ImageViewCompatImpl {
        private final ImageViewInteractinator mView;
        private boolean mCropToPadding;

        public BaseImageViewCompatImpl(ImageViewInteractinator view) {
            mView = view;
        }

        @Override
        public boolean getCropToPadding() {
            return mCropToPadding;
        }

        @Override
        public void setCropToPadding(boolean cropToPadding) {
            if (mCropToPadding != cropToPadding) {
                mCropToPadding = cropToPadding;
                mView.requestLayout();
                mView.invalidate();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static class JBImageViewCompatImpl implements ImageViewCompatImpl {
        private final ImageViewInteractinator mView;

        public JBImageViewCompatImpl(ImageViewInteractinator view) {
            mView = view;
        }

        @Override
        public boolean getCropToPadding() {
            return mView.getCropToPadding();
        }

        @Override
        public void setCropToPadding(boolean cropToPadding) {
            mView.setCropToPadding(cropToPadding);
        }
    }

    private static class Initializinator {
        final ImageViewCompatImpl impl;
        final OverScroller overScroller;
        final Transforminator transformer;

        Initializinator(
                ImageViewInteractinator view) {
            final Context context = view.getContext();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                impl = new JBImageViewCompatImpl(view);
            } else {
                impl = new BaseImageViewCompatImpl(view);
            }
            overScroller = new OverScroller(context);
            transformer = new Transforminator(context);
        }
    }

    private class OnGestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            mOverScroller.forceFinished(true);
            releaseEdgeGlows();

            mTouchPivotPoint.set(e.getX(), e.getY());
            viewPointToImagePoint(getImageMatrixInternal(), mTouchPivotPoint);

            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!mFlingEnabled || !drawableHasFunctionalDimensions()) {
                return false;
            }

            releaseEdgeGlows();
            final Matrix matrix = getImageMatrixInternal();
            matrix.getValues(mTempValues);

            final Drawable d = getDrawable();
            mTempRectSrc.set(0.0f, 0.0f, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(mTempRectDst, mTempRectSrc);

            final float startX = e2.getX();
            final float startY = e2.getY();
            mTouchPivotPoint.set(startX, startY);
            viewPointToImagePoint(getImageMatrixInternal(), mTouchPivotPoint);

            final int contentWidth = getContentWidth();
            final int contentHeight = getContentHeight();
            final float scrollableX = Math.max(mTempRectDst.width() - contentWidth, 0.0f);
            final float scrollableY = Math.max(mTempRectDst.height() - contentHeight, 0.0f);

            final float scrolledX = -Math.min(mTempValues[Matrix.MTRANS_X], 0.0f);
            final float scrolledY = -Math.min(mTempValues[Matrix.MTRANS_Y], 0.0f);
            final int overScrollMode = getOverScrollMode();
            final boolean canOverScrollX = (overScrollMode == OVER_SCROLL_ALWAYS ||
                    (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && scrollableX > 0));
            final boolean canOverScrollY = (overScrollMode == OVER_SCROLL_ALWAYS ||
                    (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && scrollableY > 0));

            final int overX = (canOverScrollX ? contentWidth / 2 : 0);
            final int overY = (canOverScrollY ? contentHeight / 2 : 0);
            mOverScroller.fling(
                    (int) startX,
                    (int) startY,
                    (int) velocityX,
                    (int) velocityY,
                    (int) (startX + scrolledX - scrollableX),
                    (int) (startX + scrolledX),
                    (int) (startY + scrolledY - scrollableY),
                    (int) (startY + scrolledY),
                    overX,
                    overY);

            ViewCompat.postInvalidateOnAnimation(ImageViewInteractinator.this);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!mScrollEnabled) {
                return false;
            }

            final float x = e2.getX();
            final float y = e2.getY();
            boolean needsInvalidate = transform(
                    false,
                    false,
                    mTouchPivotPoint.x,
                    mTouchPivotPoint.y,
                    USE_DEFAULT,
                    USE_DEFAULT,
                    x,
                    y,
                    true);

            mTouchPivotPoint.set(e2.getX(), e2.getY());
            viewPointToImagePoint(getImageMatrixInternal(), mTouchPivotPoint);

            // TODO Edge effects

            if (needsInvalidate) {
                ViewCompat.postInvalidateOnAnimation(ImageViewInteractinator.this);
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return performClick();
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return super.onSingleTapUp(e); // TODO ?
        }
    }

    private class OnScaleGestureListener extends SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mLastSpan = detector.getCurrentSpan();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mTempPoint.set(detector.getFocusX(), detector.getFocusY());
            viewPointToImagePoint(getImageMatrixInternal(), mTempPoint);
            final float px = mTempPoint.x;
            final float py = mTempPoint.y;

            final float currentSpan = detector.getCurrentSpan();
            final float spanDelta = (currentSpan / mLastSpan);

            final boolean transformed =
                    transformBy(px, py, spanDelta, spanDelta, 0.0f, 0.0f);

            if (transformed) {
                ViewCompat.postInvalidateOnAnimation(ImageViewInteractinator.this);
            }

            mLastSpan = currentSpan;
            return true;
        }
    }

    private static class Transforminator {
        final Context mContext;

        public Transforminator(Context context) {
            mContext = context;
        }

        boolean computeTransform() {
            return false; // TODO
        }
    }

    //endregion Nested classes

    //region Constants

    private static final String LOG_TAG = ImageViewInteractinator.class.getSimpleName();

    private static final float ALMOST_EQUALS_THRESHOLD = 8;

    private static final float MAX_SCALE_BREADTH_MULTIPLIER = 4.0f;
    private static final float MAX_SCALE_LENGTH_MULTIPLIER = 6.0f;
    private static final float SCALE_PRESET_THRESHOLD = 0.2f;
    private static final float EDGE_GLOW_SIZE_FACTOR = 1.25f;

    private static final int INVALID_FLAG_BASELINE_IMAGE_MATRIX = 0x00000001;
    private static final int INVALID_FLAG_IMAGE_MAX_SCALE = 0x00000002;
    private static final int INVALID_FLAG_IMAGE_MIN_SCALE = 0x00000004;
    private static final int INVALID_FLAG_DEFAULT =
            INVALID_FLAG_BASELINE_IMAGE_MATRIX |
                    INVALID_FLAG_IMAGE_MAX_SCALE |
                    INVALID_FLAG_IMAGE_MIN_SCALE;

    private static final String CLASS_NAME = ImageViewInteractinator.class.getName();
    private static final String KEY_BY = CLASS_NAME + ".by";
    private static final String KEY_CLAMP = CLASS_NAME + ".clamp";
    private static final String KEY_PX = CLASS_NAME + ".px";
    private static final String KEY_PY = CLASS_NAME + ".py";
    private static final String KEY_SX = CLASS_NAME + ".sx";
    private static final String KEY_SY = CLASS_NAME + ".sy";
    private static final String KEY_SMOOTH = CLASS_NAME + ".smooth";
    private static final String KEY_X = CLASS_NAME + ".x";
    private static final String KEY_Y = CLASS_NAME + ".y";

    public static final float USE_DEFAULT = Float.NaN;

    //endregion Constants

    //region Fields

    private final ImageViewCompatImpl mImpl;
    private final OverScroller mOverScroller;
    private final Transforminator mTransforminator;

    private GestureDetectorCompat mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;

    protected boolean mDoubleTapToScaleEnabled;
    protected boolean mFlingEnabled;
    protected boolean mScaleEnabled;
    protected boolean mScrollEnabled;
    private float[] mScalePresets;

    private ScaleType mScaleType = super.getScaleType();
    private int mInvalidFlags;

    private final Matrix mBaselineImageMatrix = new Matrix();
    private final Matrix mImageMatrix = new Matrix();
    private final PointF mImageMaxScale = new PointF();
    private final PointF mImageMinScale = new PointF();
    private final PointF mTouchPivotPoint = new PointF();

    // Avoid allocations...
    private float[] mTempPts = new float[2];
    private float[] mTempValues = new float[9];
    private Matrix mTempMatrix = new Matrix();
    private PointF mTempPoint = new PointF();
    private PointF mTempScale = new PointF();
    private RectF mTempRectSrc = new RectF();
    private RectF mTempRectDst = new RectF();

    private Bundle mPendingTransform = null;

    private SparseArray<EdgeEffect> mEdgeGlow = null;

    private float mLastSpan;

    //endregion Fields

    //region Constructors

    public ImageViewInteractinator(Context context) {
        super(context);
        final Initializinator initializinator = initImageView(
                context,
                null,
                R.attr.interactiveImageViewStyle,
                0);
        mImpl = initializinator.impl;
        mOverScroller = initializinator.overScroller;
        mTransforminator = initializinator.transformer;
    }

    public ImageViewInteractinator(Context context, AttributeSet attrs) {
        super(context, attrs);
        final Initializinator initializinator = initImageView(
                context,
                attrs,
                R.attr.interactiveImageViewStyle,
                0);
        mImpl = initializinator.impl;
        mOverScroller = initializinator.overScroller;
        mTransforminator = initializinator.transformer;
    }

    public ImageViewInteractinator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final Initializinator initializinator =
                initImageView(context, attrs, defStyleAttr, 0);
        mImpl = initializinator.impl;
        mOverScroller = initializinator.overScroller;
        mTransforminator = initializinator.transformer;
    }

    //endregion Constructors

    //region Inherited methods

    @Override
    public void computeScroll() {
        super.computeScroll();

        boolean needsInvalidate = false;

        if (mOverScroller.computeScrollOffset()) {
            final int currX = mOverScroller.getCurrX();
            final int currY = mOverScroller.getCurrY();
            needsInvalidate = transform(
                    false,
                    false,
                    mTouchPivotPoint.x,
                    mTouchPivotPoint.y,
                    USE_DEFAULT,
                    USE_DEFAULT,
                    currX,
                    currY,
                    true);
        } else if (mTransforminator.computeTransform()) {

        }

        if (getOverScrollMode() != OVER_SCROLL_NEVER) {
            // TODO GLOWS
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public ScaleType getScaleType() {
        return mScaleType;
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);
        if (mPendingTransform != null) {
            restoreTransform(mPendingTransform);
            mPendingTransform = null;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mInvalidFlags |= INVALID_FLAG_DEFAULT;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean retVal = false;
        // final int action = event.getActionMasked();
        if (drawableHasFunctionalDimensions()) {
            // When going from one to two pointers (or vice versa),
            // reset the touch pivot point to be the first "down" pointer
            final int action = event.getActionMasked();
            final int count = event.getPointerCount();
            if ((action == MotionEvent.ACTION_POINTER_UP ||
                    action == MotionEvent.ACTION_POINTER_DOWN) &&
                    count == 2) {
                final int actionIndex = event.getActionIndex();
                for (int i = 0; i < count; i++) {
                    if (action == MotionEvent.ACTION_POINTER_DOWN || i != actionIndex) {
                        mTouchPivotPoint.set(event.getX(i), event.getY(i));
                        viewPointToImagePoint(getImageMatrixInternal(), mTouchPivotPoint);
                        break;
                    }
                }
            }

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
        setScaleType(mScaleType);
        mInvalidFlags |= INVALID_FLAG_DEFAULT;
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        super.setImageMatrix(matrix);
        if (ScaleType.MATRIX == mScaleType) {
            mBaselineImageMatrix.set(matrix);
            mInvalidFlags &= INVALID_FLAG_DEFAULT & ~INVALID_FLAG_BASELINE_IMAGE_MATRIX;
        }
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void setOverScrollMode(int mode) {
        super.setOverScrollMode(mode);
        if (mode == OVER_SCROLL_NEVER) {
            mEdgeGlow = null;
        } else {
            final Context context = getContext();
            mEdgeGlow = new SparseArray<>(4);
            mEdgeGlow.put(Gravity.LEFT, new EdgeEffect(context));
            mEdgeGlow.put(Gravity.TOP, new EdgeEffect(context));
            mEdgeGlow.put(Gravity.RIGHT, new EdgeEffect(context));
            mEdgeGlow.put(Gravity.BOTTOM, new EdgeEffect(context));
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        mInvalidFlags |= INVALID_FLAG_DEFAULT;
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        super.setPaddingRelative(start, top, end, bottom);
        mInvalidFlags |= INVALID_FLAG_DEFAULT;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        mScaleType = scaleType;
        super.setScaleType(mScaleType);
        mInvalidFlags |= INVALID_FLAG_DEFAULT;
    }

    //endregion Inherited methods

    //region Methods

    public boolean getCompatCropToPadding() {
        return mImpl.getCropToPadding();
    }

    public float getImageMaxScaleX() {
        getImageMaxScale(mTempPoint);
        return mTempPoint.x;
    }

    public float getImageMaxScaleY() {
        getImageMaxScale(mTempPoint);
        return mTempPoint.y;
    }

    public float getImageMinScaleX() {
        getImageMinScale(mTempPoint);
        return mTempPoint.x;
    }

    public float getImageMinScaleY() {
        getImageMinScale(mTempPoint);
        return mTempPoint.y;
    }

    public float getImagePivotX() {
        getImagePivot(mTempPoint);
        return mTempPoint.x;
    }

    public float getImagePivotY() {
        getImagePivot(mTempPoint);
        return mTempPoint.y;
    }

    public float getImageScaleX() {
        getImageScale(mTempPoint);
        return mTempPoint.x;
    }

    public float getImageScaleY() {
        getImageScale(mTempPoint);
        return mTempPoint.y;
    }

    public float[] getScalePresets() {
        return mScalePresets;
    }

    public boolean isDoubleTapToScaleEnabled() {
        return mDoubleTapToScaleEnabled;
    }

    public boolean isFlingEnabled() {
        return mFlingEnabled;
    }

    public boolean isScaleEnabled() {
        return mScaleEnabled;
    }

    public boolean isScrollEnabled() {
        return mScrollEnabled;
    }

    public boolean isTransformed() {
        final float imageMatrixValues[] = new float[9];
        final float baselineImageMatrixValues[] = new float[9];
        getImageMatrixInternal().getValues(imageMatrixValues);
        getBaselineImageMatrix().getValues(baselineImageMatrixValues);
        for (int i = 0; i < 9; i++) {
            if (!almostEquals(imageMatrixValues[i], baselineImageMatrixValues[i])) {
                return true;
            }
        }
        return false;
    }

    public void setCompatCropToPadding(boolean cropToPadding) {
        mImpl.setCropToPadding(cropToPadding);
    }

    public void setDoubleTapToScaleEnabled(boolean doubleTapToScaleEnabled) {
        mDoubleTapToScaleEnabled = doubleTapToScaleEnabled;
        updateGestureDetector();
    }

    public void setFlingEnabled(boolean flingEnabled) {
        mFlingEnabled = flingEnabled;
        updateGestureDetector();
    }

    public void setScaleEnabled(boolean scaleEnabled) {
        mScaleEnabled = scaleEnabled;
        updateScaleGestureDetector();
    }

    public void setScalePresets(float[] scalePresets) {
        mScalePresets = scalePresets;
    }

    public void setScrollEnabled(boolean scrollEnabled) {
        mScrollEnabled = scrollEnabled;
        updateGestureDetector();
    }

    public boolean smoothTransformBy(
            float px,
            float py,
            float sxBy,
            float syBy,
            float xBy,
            float yBy) {
        return transform(true, true, px, py, sxBy, syBy, xBy, yBy, true);
    }

    public void smoothTransformTo(float px, float py, float sx, float sy) {
        smoothTransformTo(px, py, sx, sy, USE_DEFAULT, USE_DEFAULT);
    }

    public boolean smoothTransformTo(
            float px,
            float py,
            float sx,
            float sy,
            float x,
            float y) {
        return transform(true, false, px, py, sx, sy, x, y, true);
    }

    public boolean transformBy(
            float px,
            float py,
            float sxBy,
            float syBy,
            float xBy,
            float yBy) {
        return transform(false, true, px, py, sxBy, syBy, xBy, yBy, true);
    }

    public boolean transformTo(float px, float py, float sx, float sy) {
        return transformTo(px, py, sx, sy, USE_DEFAULT, USE_DEFAULT);
    }

    public boolean transformTo(
            float px,
            float py,
            float sx,
            float sy,
            float x,
            float y) {
        return transform(false, false, px, py, sx, sy, x, y, true);
    }

    //endregion Methods

    //region Protected methods

    protected boolean clampTransform(
            float px,
            float py,
            float sx,
            float sy,
            float x,
            float y,
            PointF outScale,
            PointF outPoint) {
        // Clamp scale
        outScale.set(
                MathUtils.clamp(sx, getImageMinScaleX(), getImageMaxScaleX()),
                MathUtils.clamp(sy, getImageMinScaleY(), getImageMaxScaleY()));

        // Clamp view point
        mImageMatrix.set(getImageMatrixInternal());
        mImageMatrix.getValues(mTempValues);

        // Pre-scale the matrix to the clamped scale
        if (Float.compare(outScale.x, mTempValues[Matrix.MSCALE_X]) != 0 ||
                Float.compare(outScale.y, mTempValues[Matrix.MSCALE_Y]) != 0) {
            final float deltaSx = outScale.x / mTempValues[Matrix.MSCALE_X];
            final float deltaSy = outScale.y / mTempValues[Matrix.MSCALE_Y];
            mImageMatrix.preScale(deltaSx, deltaSy);
            mImageMatrix.getValues(mTempValues);
        }

        mTempPoint.set(px, py);
        imagePointToViewPoint(mImageMatrix, mTempPoint);
        final float mappedPx = mTempPoint.x - mTempValues[Matrix.MTRANS_X];
        final float mappedPy = mTempPoint.y - mTempValues[Matrix.MTRANS_Y];

        getTranslationCoefficient(mImageMatrix, mTempPoint);
        if (mTempPoint.x >= 0) {
            outPoint.x = mTempPoint.x + mappedPx;
        } else {
            final float minX = Math.min(mTempPoint.x, 0.0f);
            final float clampedDx = MathUtils.clamp(x - mappedPx, minX, 0.0f);
            outPoint.x = mappedPx + clampedDx;
        }
        if (mTempPoint.y >= 0) {
            outPoint.y = mTempPoint.y + mappedPy;
        } else {
            final float minY = Math.min(mTempPoint.y, 0.0f);
            final float clampedDy = MathUtils.clamp(y - mappedPy, minY, 0.0f);
            outPoint.y = mappedPy + clampedDy;
        }

        return (!almostEquals(sx, outScale.x) ||
                !almostEquals(sy, outScale.y) ||
                !almostEquals(x, outPoint.x) ||
                !almostEquals(y, outPoint.y));
    }

    protected boolean drawableHasFunctionalDimensions() {
        final Drawable d = getDrawable();
        return !(d == null || d.getIntrinsicWidth() < 1 || d.getIntrinsicHeight() < 1);
    }

    /**
     * Returns the baseline image matrix; that is, the matrix that describes the image
     * at the current scale type
     * minus any padding. Do not change this rectangle in place but make a copy.
     *
     * @return
     */
    protected Matrix getBaselineImageMatrix() {
        if ((mInvalidFlags & INVALID_FLAG_BASELINE_IMAGE_MATRIX) != 0) {
            mInvalidFlags &= ~INVALID_FLAG_BASELINE_IMAGE_MATRIX;
            getBaselineImageMatrix(mScaleType, mBaselineImageMatrix);
        }
        return mBaselineImageMatrix;
    }

    /**
     * See ImageView.configureBounds()
     *
     * @param scaleType The scale type to use to compute the baseline image matrix
     * @param outMatrix Matrix in which to place the computed baseline coordinates
     */
    @SuppressWarnings("SpellCheckingInspection")
    protected void getBaselineImageMatrix(ScaleType scaleType, Matrix outMatrix) {
        if (outMatrix == null || getDrawable() == null) {
            return;
        }

        // We need to do the scaling ourselves.
        final int dwidth = getDrawableFunctionalWidth();
        final int dheight = getDrawableFunctionalHeight();

        final int vwidth = getContentWidth();
        final int vheight = getContentHeight();

        final boolean fits = (dwidth < 0 || vwidth == dwidth)
                && (dheight < 0 || vheight == dheight);

        if (ScaleType.MATRIX == scaleType) {
            // Use the specified matrix as-is.
            outMatrix.set(getImageMatrix());
        } else if (fits) {
            // The bitmap fits exactly, no transform needed.
            outMatrix.reset();
        } else if (ScaleType.CENTER == scaleType) {
            // Center bitmap in view, no scaling.
            outMatrix.setTranslate(
                    Math.round((vwidth - dwidth) * 0.5f),
                    Math.round((vheight - dheight) * 0.5f));
        } else if (ScaleType.CENTER_CROP == scaleType) {
            float scale;
            float dx = 0, dy = 0;

            if (dwidth * vheight > vwidth * dheight) {
                scale = (float) vheight / (float) dheight;
                dx = (vwidth - dwidth * scale) * 0.5f;
            } else {
                scale = (float) vwidth / (float) dwidth;
                dy = (vheight - dheight * scale) * 0.5f;
            }

            outMatrix.setScale(scale, scale);
            outMatrix.postTranslate(Math.round(dx), Math.round(dy));
        } else if (ScaleType.CENTER_INSIDE == scaleType) {
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

            outMatrix.setScale(scale, scale);
            outMatrix.postTranslate(dx, dy);
        } else {
            // Generate the required transform.
            mTempRectSrc.set(0.0f, 0.0f, dwidth, dheight);
            mTempRectDst.set(0.0f, 0.0f, vwidth, vheight);
            outMatrix.setRectToRect(
                    mTempRectSrc,
                    mTempRectDst,
                    scaleTypeToScaleToFit(scaleType));
        }
    }

    protected int getDrawableFunctionalHeight() {
        final Drawable dr = getDrawable();
        return (dr == null ? 0 : Math.max(dr.getIntrinsicHeight(), 0));
    }

    protected int getDrawableFunctionalWidth() {
        final Drawable dr = getDrawable();
        return (dr == null ? 0 : Math.max(dr.getIntrinsicWidth(), 0));
    }

    /**
     * Returns the view's optional matrix, amended to return a valid matrix when the scale type is
     * set to FIT_XY. This is applied to the view's drawable when it is drawn. If there is no
     * matrix, this method will return an identity matrix. Do not change this matrix in place but
     * make a copy. If you want a different matrix applied to the drawable, be sure to call
     * setImageMatrix().
     *
     * @return The view's optional matrix
     */
    protected Matrix getImageMatrixInternal() {
        if (ScaleType.FIT_XY == super.getScaleType()) {
            getBaselineImageMatrix(ScaleType.FIT_XY, mImageMatrix);
        } else {
            mImageMatrix.set(super.getImageMatrix());
        }
        return mImageMatrix;
    }

    protected void getImageMaxScale(PointF outPoint) {
        if ((mInvalidFlags & INVALID_FLAG_IMAGE_MAX_SCALE) != 0) {
            mInvalidFlags &= ~INVALID_FLAG_IMAGE_MAX_SCALE;
            if (drawableHasFunctionalDimensions()) {
                final Matrix baselineMatrix = getBaselineImageMatrix();
                baselineMatrix.getValues(mTempValues);

                final Drawable d = getDrawable();
                mTempRectSrc.set(
                        0.0f,
                        0.0f,
                        d.getIntrinsicWidth(),
                        d.getIntrinsicHeight());
                baselineMatrix.mapRect(mTempRectDst, mTempRectSrc);

                final float baselineWidth = mTempRectDst.width();
                final float baselineHeight = mTempRectDst.height();
                final float baselineBreadth = Math.min(baselineWidth, baselineHeight);
                final float baselineLength = Math.max(baselineWidth, baselineHeight);

                final Context context = getContext();
                final DisplayMetrics dm;
                final WindowManager windowManager =
                        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
                if (windowManager == null) {
                    dm = context.getResources().getDisplayMetrics();
                } else {
                    dm = new DisplayMetrics();
                    DisplayCompat.getRealMetrics(windowManager.getDefaultDisplay(), dm);
                }

                final int min = Math.min(dm.widthPixels, dm.heightPixels);
                final int max = Math.max(dm.widthPixels, dm.heightPixels);
                final float maxBreadth = MAX_SCALE_BREADTH_MULTIPLIER * min;
                final float maxLength = MAX_SCALE_LENGTH_MULTIPLIER * max;
                final float screenBasedScale = Math.min(
                        maxBreadth / baselineBreadth,
                        maxLength / baselineLength);
                final int availableSize;
                if (baselineWidth < baselineHeight) {
                    availableSize = getContentWidth();
                } else if (baselineWidth > baselineHeight) {
                    availableSize = getContentHeight();
                } else {
                    availableSize = Math.min(getContentWidth(), getContentHeight());
                }
                final float viewBasedScale = availableSize / baselineBreadth;
                final float scale = Math.max(screenBasedScale, viewBasedScale);
                mImageMaxScale.set(
                        scale * mTempValues[Matrix.MSCALE_X],
                        scale * mTempValues[Matrix.MSCALE_Y]);
            } else {
                mImageMaxScale.set(1.0f, 1.0f);
            }
        }
        outPoint.set(mImageMaxScale);
    }

    protected void getImageMinScale(PointF outPoint) {
        if ((mInvalidFlags & INVALID_FLAG_IMAGE_MIN_SCALE) != 0) {
            mInvalidFlags &= ~INVALID_FLAG_IMAGE_MIN_SCALE;
            if (drawableHasFunctionalDimensions()) {
                getBaselineImageMatrix().getValues(mTempValues);
                mImageMinScale.set(mTempValues[Matrix.MSCALE_X], mTempValues[Matrix.MSCALE_Y]);
            } else {
                mImageMinScale.set(1.0f, 1.0f);
            }
        }
        outPoint.set(mImageMinScale);
    }

    protected void getImagePivot(PointF outPoint) {
        getImagePivot(getImageMatrixInternal(), outPoint);
    }

    protected void getImagePivot(Matrix matrix, PointF outPoint) {
        mTempPoint.set(getContentCenterX(), getContentCenterY());
        viewPointToImagePoint(matrix, outPoint);
    }

    protected void getImageScale(PointF outPoint) {
        getImageMatrixInternal().getValues(mTempValues);
        outPoint.set(mTempValues[Matrix.MSCALE_X], mTempValues[Matrix.MSCALE_Y]);
    }

    protected float getNextScalePreset() {
        float next = 0.0f;
        if (mScalePresets != null) {
            final float minSx = getImageMinScaleX();
            final float minSy = getImageMinScaleY();
            final float xScaleRange = (getImageMaxScaleX() - minSx);
            final float yScaleRange = (getImageMaxScaleY() - minSy);
            final float relativeSx = (Float.compare(xScaleRange, 0.0f) == 0 ?
                    0.0f :
                    (getImageScaleX() - minSx) / xScaleRange);
            final float relativeSy = (Float.compare(yScaleRange, 0.0f) == 0 ?
                    0.0f :
                    (getImageScaleY() - minSy) / yScaleRange);
            boolean foundX = false;
            boolean foundY = false;
            for (final float zoomPivot : mScalePresets) {
                if (Float.compare(zoomPivot - relativeSx, SCALE_PRESET_THRESHOLD) > 0) {
                    foundX = true;
                    next = zoomPivot;
                }
                if (Float.compare(zoomPivot - relativeSy, SCALE_PRESET_THRESHOLD) > 0) {
                    foundY = true;
                    next = zoomPivot;
                }
                if (foundX && foundY) {
                    break;
                }
            }
        }
        return next;
    }

    protected void imagePointToViewPoint(Matrix matrix, PointF point) {
        mTempPts[0] = point.x;
        mTempPts[1] = point.y;
        matrix.mapPoints(mTempPts);
        if (getCompatCropToPadding()) {
            mTempPts[0] += getPaddingLeft();
            mTempPts[1] += getPaddingTop();
        }
        point.set(mTempPts[0], mTempPts[1]);
    }

    protected void viewPointToImagePoint(Matrix matrix, PointF point) {
        mTempPts[0] = point.x;
        mTempPts[1] = point.y;
        if (getCompatCropToPadding()) {
            mTempPts[0] -= getPaddingLeft();
            mTempPts[1] -= getPaddingTop();
        }
        matrix.invert(mTempMatrix);
        mTempMatrix.mapPoints(mTempPts);
        point.set(mTempPts[0], mTempPts[1]);
    }

    //endregion Protected methods

    //region Private methods

    private boolean almostEquals(float a, float b) {
        final int diff = Math.abs(Float.floatToIntBits(a) - Float.floatToIntBits(b));
        return diff <= ALMOST_EQUALS_THRESHOLD;
    }

    private float getContentCenterX() {
        return (getPaddingLeft() + getWidth() - getPaddingRight()) * 0.5f;
    }

    private float getContentCenterY() {
        return (getPaddingTop() + getHeight() - getPaddingBottom()) * 0.5f;
    }

    private int getContentHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private int getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    /*
     * TODO Mention how if inX or inY < 0, it's the minimum translation allowed.
     * If > 0, then it's a set translation and is not scrollable.
     */
    private void getTranslationCoefficient(Matrix matrix, PointF outPoint) {
        if (drawableHasFunctionalDimensions()) {
            final Drawable d = getDrawable();
            mTempRectSrc.set(0.0f, 0.0f, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(mTempRectDst, mTempRectSrc);
            final float xDiff = getContentWidth() - mTempRectDst.width();
            if (Float.compare(xDiff, 0.0f) > 0) {
                // The mapped image is narrower than the content area
                switch (mScaleType) {
                    case FIT_START:
                        outPoint.x = (ViewCompat.getLayoutDirection(this) ==
                                ViewCompat.LAYOUT_DIRECTION_RTL ?
                                xDiff :
                                0.0f);
                        break;
                    case FIT_END:
                        outPoint.x = (ViewCompat.getLayoutDirection(this) ==
                                ViewCompat.LAYOUT_DIRECTION_RTL ?
                                0.0f :
                                xDiff);
                        break;
                    default:
                        outPoint.x = xDiff * 0.5f;
                }
            } else {
                outPoint.x = xDiff;
            }
            final float yDiff = getContentHeight() - mTempRectDst.height();
            if (Float.compare(yDiff, 0.0f) > 0) {
                // The mapped image is shorter than the content area
                switch (mScaleType) {
                    case FIT_START:
                        outPoint.y = 0.0f;
                        break;
                    case FIT_END:
                        outPoint.y = yDiff;
                        break;
                    default:
                        outPoint.y = yDiff * 0.5f;
                }
            } else {
                outPoint.y = yDiff;
            }
        } else {
            outPoint.set(0.0f, 0.0f);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private Initializinator initImageView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        final Initializinator initializinator = new Initializinator(this);
        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.ImageViewInteractinator,
                defStyleAttr,
                defStyleRes);
        initializinator.impl.setCropToPadding(a.getBoolean(
                R.styleable.ImageViewInteractinator_compatCropToPadding,
                false));
        setDoubleTapToScaleEnabled(a.getBoolean(
                R.styleable.ImageViewInteractinator_doubleTapToScaleEnabled,
                true));
        setFlingEnabled(a.getBoolean(
                R.styleable.ImageViewInteractinator_flingEnabled,
                true));
        setScaleEnabled(a.getBoolean(
                R.styleable.ImageViewInteractinator_scaleEnabled,
                true));
        final @ArrayRes int resId = a.getResourceId(
                R.styleable.ImageViewInteractinator_scalePresets,
                -1);
        if (resId != -1) {
            TypedArray ta = getResources().obtainTypedArray(resId);
            final int length = ta.length();
            final float[] scalePresets = new float[length];
            for (int i = 0; i < length; i++) {
                scalePresets[i] = ta.getFloat(i, Float.NaN);
            }
            setScalePresets(scalePresets);
            ta.recycle();
        }
        setScrollEnabled(a.getBoolean(
                R.styleable.ImageViewInteractinator_scrollEnabled,
                true));
        a.recycle();
        return initializinator;
    }

    private void releaseEdgeGlows() {
        if (mEdgeGlow != null) {
            for (int i = 0; i < mEdgeGlow.size(); i++) {
                mEdgeGlow.valueAt(i).onRelease();
            }
        }
    }

    private void restoreTransform(Bundle bundle) {
        final boolean smooth = bundle.getBoolean(KEY_SMOOTH, false);
        final boolean by = bundle.getBoolean(KEY_BY, false);
        final float px = bundle.getFloat(KEY_PX, USE_DEFAULT);
        final float py = bundle.getFloat(KEY_PY, USE_DEFAULT);
        final float sx = bundle.getFloat(KEY_SX, USE_DEFAULT);
        final float sy = bundle.getFloat(KEY_SY, USE_DEFAULT);
        final float x = bundle.getFloat(KEY_X, USE_DEFAULT);
        final float y = bundle.getFloat(KEY_Y, USE_DEFAULT);
        final boolean clamp = bundle.getBoolean(KEY_CLAMP, true);
        transform(smooth, by, px, py, sx, sy, x, y, clamp);
    }

    private Bundle saveTransform(
            boolean smooth,
            boolean by,
            float px,
            float py,
            float sx,
            float sy,
            float x,
            float y,
            boolean clamp) {
        final Bundle bundle = new Bundle(9);
        bundle.putBoolean(KEY_SMOOTH, smooth);
        bundle.putBoolean(KEY_BY, by);
        bundle.putFloat(KEY_PX, px);
        bundle.putFloat(KEY_PY, py);
        bundle.putFloat(KEY_SX, sx);
        bundle.putFloat(KEY_SY, sy);
        bundle.putFloat(KEY_X, x);
        bundle.putFloat(KEY_Y, y);
        bundle.putBoolean(KEY_CLAMP, clamp);
        return bundle;
    }

    private static Matrix.ScaleToFit scaleTypeToScaleToFit(ScaleType scaleType) {
        if (scaleType == null) {
            return null;
        } else switch (scaleType) {
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

    private boolean transform(
            boolean smooth,
            boolean by,
            float px,
            float py,
            float sx,
            float sy,
            float x,
            float y,
            boolean clamp) {
        if (!ViewCompat.isLaidOut(this)) {
            mPendingTransform = saveTransform(smooth, by, px, py, sx, sy, x, y, clamp);
            return false;
        }

        // Resolve all values
        final boolean pxIsDefault = (Float.compare(px, USE_DEFAULT) == 0);
        final boolean pyIsDefault = (Float.compare(py, USE_DEFAULT) == 0);
        final boolean sxIsDefault = (Float.compare(sx, USE_DEFAULT) == 0);
        final boolean syIsDefault = (Float.compare(sy, USE_DEFAULT) == 0);
        final boolean xIsDefault = (Float.compare(x, USE_DEFAULT) == 0);
        final boolean yIsDefault = (Float.compare(y, USE_DEFAULT) == 0);
        final float resolvedPx = (pxIsDefault ? getDrawableFunctionalWidth() * 0.5f : px);
        final float resolvedPy = (pyIsDefault ? getDrawableFunctionalHeight() * 0.5f : py);
        final float resolvedSx;
        final float resolvedSy;
        final float resolvedX;
        final float resolvedY;
        if (by) {
            resolvedSx = (sxIsDefault ? getImageScaleX() : getImageScaleX() * sx);
            resolvedSy = (syIsDefault ? getImageScaleY() : getImageScaleY() * sy);
            mTempPoint.set(resolvedPx, resolvedPy);
            imagePointToViewPoint(getImageMatrixInternal(), mTempPoint);
            resolvedX = (xIsDefault ? mTempPoint.x : mTempPoint.x + x);
            resolvedY = (yIsDefault ? mTempPoint.y : mTempPoint.y + y);
        } else {
            resolvedSx = (sxIsDefault ? getImageScaleX() : sx);
            resolvedSy = (syIsDefault ? getImageScaleY() : sy);
            resolvedX = (xIsDefault ? getContentCenterX() : x);
            resolvedY = (yIsDefault ? getContentCenterY() : y);
        }

        // Optionally clamp values
        final boolean clamped; // TODO Might not need
        final float clampedSx;
        final float clampedSy;
        final float clampedX;
        final float clampedY;
        if (clamp) {
            clamped = clampTransform(
                    resolvedPx,
                    resolvedPy,
                    resolvedSx,
                    resolvedSy,
                    resolvedX,
                    resolvedY,
                    mTempScale,
                    mTempPoint);
            clampedSx = mTempScale.x;
            clampedSy = mTempScale.y;
            clampedX = mTempPoint.x;
            clampedY = mTempPoint.y;
        } else {
            clamped = false;
            clampedSx = resolvedSx;
            clampedSy = resolvedSy;
            clampedX = resolvedX;
            clampedY = resolvedY;
        }

        // Convert transform to matrix
        final Matrix matrix = getImageMatrixInternal();
        mTempMatrix.set(matrix);
        mTempMatrix.getValues(mTempValues);
        final float deltaSx = clampedSx / mTempValues[Matrix.MSCALE_X];
        final float deltaSy = clampedSy / mTempValues[Matrix.MSCALE_Y];
        mTempMatrix.preScale(deltaSx, deltaSy, resolvedPx, resolvedPy);
        mTempPoint.set(resolvedPx, resolvedPy);
        imagePointToViewPoint(mTempMatrix, mTempPoint);
        final float deltaTx = clampedX - mTempPoint.x;
        final float deltaTy = clampedY - mTempPoint.y;
        mTempMatrix.postTranslate(deltaTx, deltaTy);

        final boolean transformed = !matrix.equals(mTempMatrix);
        if (transformed) {
            if (smooth) {
                /* TODO
                mPts[0] = info.px;
                mPts[1] = info.py;
                mapDrawablePtsToViewPts(getImageMatrixInternal(), mPts);
                mTransforminator.startTransform(
                        outInfo.px,
                        outInfo.py,
                        getImageScaleX(),
                        getImageScaleY(),
                        mPts[0],
                        mPts[1],
                        outInfo.sx,
                        outInfo.sy,
                        outInfo.x,
                        outInfo.y);
                */
                ViewCompat.postInvalidateOnAnimation(this);
            } else {
                if (super.getScaleType() != ScaleType.MATRIX) {
                    super.setScaleType(ScaleType.MATRIX);
                }
                super.setImageMatrix(mTempMatrix);
            }
        }
        return transformed;
    }

    private void updateGestureDetector() {
        if (!mScrollEnabled && !mFlingEnabled && !mDoubleTapToScaleEnabled) {
            mGestureDetector = null;
        } else if (mGestureDetector == null) {
            final OnGestureListener listener = new OnGestureListener();
            mGestureDetector = new GestureDetectorCompat(getContext(), listener);
            mGestureDetector.setIsLongpressEnabled(false);
            mGestureDetector.setOnDoubleTapListener(listener);
        }
    }

    private void updateScaleGestureDetector() {
        if (!mScaleEnabled) {
            mScaleGestureDetector = null;
        } else if (mScaleGestureDetector == null) {
            mScaleGestureDetector =
                    new ScaleGestureDetector(getContext(), new OnScaleGestureListener());
        }
    }

    //endregion Private methods
}
