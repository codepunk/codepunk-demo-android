package com.codepunk.demo.interactiveimageview.version7;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;
import android.support.v4.util.Pools.SimplePool;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.OverScroller;

import com.codepunk.demo.R;
import com.codepunk.demo.support.DisplayCompat;

import java.util.Locale;

import static android.graphics.Matrix.MSCALE_X;
import static android.graphics.Matrix.MSCALE_Y;
import static android.graphics.Matrix.MTRANS_X;
import static android.graphics.Matrix.MTRANS_Y;

public class InteractiveImageView extends AppCompatImageView
        implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener {

    //region Nested classes
    private static class Initializer {
        final GestureDetectorCompat gestureDetector;
        final ScaleGestureDetector scaleGestureDetector;
        final OverScroller overScroller;
        final Transformer transformer;

        Initializer(InteractiveImageView view) {
            final Context context = view.getContext();
            gestureDetector = new GestureDetectorCompat(context, view);
            gestureDetector.setIsLongpressEnabled(false);
            gestureDetector.setOnDoubleTapListener(view);
            scaleGestureDetector = new ScaleGestureDetector(context, view);
            overScroller = new OverScroller(context);
            transformer = new Transformer(context);
        }
    }

    /*
    protected static class MatrixPool extends Pools.SimplePool<Matrix> {
        MatrixPool(int maxPoolSize) {
            super(maxPoolSize);
        }

        @Override
        public Matrix acquire() {
            final Matrix matrix = super.acquire();
            return (matrix == null ? new Matrix() : matrix);
        }
    }
    */

    private static class MatrixValuesWrapper {
        float[] values = new float[9];
    }

    /*
    private static class MatrixValuesPool extends Pools.SimplePool<MatrixValuesWrapper> {
        MatrixValuesPool(int maxPoolSize) {
            super(maxPoolSize);
        }

        @Override
        public MatrixValuesWrapper acquire() {
            final MatrixValuesWrapper values = super.acquire();
            return (values == null ? new MatrixValuesWrapper() : values);
        }
    }
    */

    private static class PoolManager {
        final SimplePool<Matrix> mMatrixPool;
        final SimplePool<MatrixValuesWrapper> mMatrixValuesWrapperPool;
        final SimplePool<PtsWrapper> mPtsWrapperPool;
        final SimplePool<RectF> mRectFPool;

        public PoolManager(int maxPoolSize) {
            mMatrixPool = new SimplePool<>(maxPoolSize);
            mMatrixValuesWrapperPool = new SimplePool<>(maxPoolSize);
            mPtsWrapperPool = new SimplePool<>(maxPoolSize);
            mRectFPool = new SimplePool<>(maxPoolSize);
        }

        Matrix acquireMatrix() {
            final Matrix instance = mMatrixPool.acquire();
            return (instance == null ? new Matrix() : instance);
        }

        MatrixValuesWrapper acquireMatrixValuesWrapper() {
            MatrixValuesWrapper instance = mMatrixValuesWrapperPool.acquire();
            return (instance == null ? new MatrixValuesWrapper() : instance);
        }

        MatrixValuesWrapper acquireMatrixValuesWrapper(Matrix matrix) {
            MatrixValuesWrapper wrapper = acquireMatrixValuesWrapper();
            matrix.getValues(wrapper.values);
            return wrapper;
        }

        PtsWrapper acquirePtsWrapper() {
            PtsWrapper instance = mPtsWrapperPool.acquire();
            return (instance == null ? new PtsWrapper() : instance);
        }

        PtsWrapper acquirePtsWrapper(float x, float y) {
            final PtsWrapper pts = acquirePtsWrapper();
            pts.pts[0] = x;
            pts.pts[1] = y;
            return pts;
        }

        RectF acquireRectF() {
            final RectF instance = mRectFPool.acquire();
            return (instance == null ? new RectF() : instance);
        }

        RectF acquireRectF(float left, float top, float right, float bottom) {
            final RectF rect = acquireRectF();
            rect.set(left, top, right, bottom);
            return rect;
        }

        void releaseMatrix(Matrix instance) {
            mMatrixPool.release(instance);
        }

        void releaseMatrixValuesWrapper(MatrixValuesWrapper instance) {
            mMatrixValuesWrapperPool.release(instance);
        }

        void releasePtsWrapper(PtsWrapper instance) {
            mPtsWrapperPool.release(instance);
        }

        void releaseRectF(RectF rect) {
            mRectFPool.release(rect);
        }
    }

    private static class PtsWrapper {
        float[] pts = new float[2];

        void set(float x, float y) {
            pts[0] = x;
            pts[1] = y;
        }

        float getX() {
            return pts[0];
        }

        float getY() {
            return pts[1];
        }
    }

    /*
    private static class PtsPool extends Pools.SimplePool<PtsWrapper> {
        PtsPool(int maxPoolSize) {
            super(maxPoolSize);
        }

        @Override
        public PtsWrapper acquire() {
            final PtsWrapper pts = super.acquire();
            return (pts == null ? new PtsWrapper() : pts);
        }

        PtsWrapper acquire(float x, float y) {
            final PtsWrapper pts = acquire();
            pts.pts[0] = x;
            pts.pts[1] = y;
            return pts;
        }
    }

    private static class RectFPool extends Pools.SimplePool<RectF> {
        RectFPool(int maxPoolSize) {
            super(maxPoolSize);
        }

        @Override
        public RectF acquire() {
            final RectF rect = super.acquire();
            return (rect == null ? new RectF() : rect);
        }

        RectF acquire(float left, float top, float right, float bottom) {
            final RectF rect = acquire();
            rect.set(left, top, right, bottom);
            return rect;
        }
    }
    */

    @SuppressWarnings("WeakerAccess")
    protected static class Transformer {
        /**
         * The interpolator, used for making zooms animate 'naturally.'
         */
        private Interpolator mInterpolator;

        /**
         * The total animation duration for a zoom.
         */
        private int mAnimationDurationMillis;

        /**
         * Whether or not the current zoom has finished.
         */
        private boolean mFinished = true;

        /**
         * The X value of the pivot point.
         */
        private float mPx;

        /**
         * The Y value of the pivot point.
         */
        private float mPy;

        /**
         * The current scale X value; computed by {@link #computeScroll()}.
         */
        private float mCurrentSx;

        /**
         * The current scale Y value; computed by {@link #computeScroll()}.
         */
        private float mCurrentSy;

        /**
         * The current X location of the pivot point; computed by {@link #computeScroll()}.
         */
        private float mCurrentX;

        /**
         * The current Y location of the pivot point; computed by {@link #computeScroll()}.
         */
        private float mCurrentY;

        /**
         * The time the zoom started, computed using {@link SystemClock#elapsedRealtime()}.
         */
        private long mStartRTC;

        private float mStartSx;

        private float mStartSy;

        private float mStartX;

        private float mStartY;

        /**
         * The destination scale X.
         */
        private float mEndSx;

        /**
         * The destination scale Y.
         */
        private float mEndSy;

        /**
         * The destination X location of the pivot point.
         */
        private float mEndX;

        /**
         * The destination Y location of the pivot point.
         */
        private float mEndY;

        public Transformer(Context context) {
            mInterpolator = new DecelerateInterpolator();
            mAnimationDurationMillis =
                    context.getResources().getInteger(android.R.integer.config_shortAnimTime);
        }

        /**
         * Aborts the animation, setting the current values to the ending value.
         *
         * @see android.widget.Scroller#abortAnimation()
         */
        void abortAnimation() {
            mFinished = true;
            mCurrentSx = mEndSx;
            mCurrentSy = mEndSy;
            mCurrentX = mEndX;
            mCurrentY = mEndY;
        }

        /**
         * Forces the zoom finished state to the given value. Unlike {@link #abortAnimation()}, the
         * current zoom value isn't set to the ending value.
         *
         * @see android.widget.Scroller#forceFinished(boolean)
         */
        void forceFinished(boolean finished) {
            mFinished = finished;
        }

        /**
         * Starts a scroll from the supplied start values to the supplied end values.
         *
         * @see android.widget.Scroller#startScroll(int, int, int, int)
         */
        void startTransform(
                float px,
                float py,
                float startSx, 
                float startSy, 
                float startX,
                float startY,
                float endSx, 
                float endSy, 
                float endX,
                float endY) {
            mStartRTC = SystemClock.elapsedRealtime();
            mPx = px;
            mPy = py;
            mCurrentSx = mStartSx = startSx;
            mCurrentSy = mStartSy = startSy;
            mCurrentX = mStartX = startX;
            mCurrentY = mStartY = startY;
            mEndSx = endSx;
            mEndSy = endSy;
            mEndX = endX;
            mEndY = endY;
            mFinished = false;
        }

        /**
         * Computes the current scroll, returning true if the zoom is still active and false if the
         * scroll has finished.
         *
         * @see android.widget.Scroller#computeScrollOffset()
         */
        boolean computeTransform() {
            if (mFinished) {
                return false;
            }

            long tRTC = SystemClock.elapsedRealtime() - mStartRTC;

            if (tRTC >= mAnimationDurationMillis) {
                mFinished = true;
                mCurrentSx = mEndSx;
                mCurrentSy = mEndSy;
                mCurrentX = mEndX;
                mCurrentY = mEndY;
                return false;
            }

            float t = tRTC * 1f / mAnimationDurationMillis;
            float interpolation = mInterpolator.getInterpolation(t);
            mCurrentSx = mStartSx + (mEndSx - mStartSx) * interpolation;
            mCurrentSy = mStartSy + (mEndSy - mStartSy) * interpolation;
            mCurrentX = mStartX + (mEndX - mStartX) * interpolation;
            mCurrentY = mStartY + (mEndY - mStartY) * interpolation;
            return true;
        }

        /**
         * Returns the current scale X.
         *
         * @see android.widget.Scroller#getCurrX()
         */
        float getCurrScaleX() {
            return mCurrentSx;
        }

        /**
         * Returns the current scale Y.
         *
         * @see android.widget.Scroller#getCurrX()
         */
        float getCurrScaleY() {
            return mCurrentSy;
        }

        /**
         * Returns the current translation X.
         *
         * @see android.widget.Scroller#getCurrX()
         */
        float getCurrX() {
            return mCurrentX;
        }

        /**
         * Returns the current translation Y.
         *
         * @see android.widget.Scroller#getCurrX()
         */
        float getCurrY() {
            return mCurrentY;
        }

        /**
         * Returns the pivot X.
         */
        float getPx() {
            return mPx;
        }

        /**
         * Returns the pivot Y.
         */
        float getPy() {
            return mPy;
        }
    }

    protected static final class TransformInfo {
        public float sx;
        public float sy;
        public float px;
        public float py;
        public float x;
        public float y;

        public TransformInfo() {
            reset();
        }

        public TransformInfo(float sx, float sy, float px, float py, float x, float y) {
            set(sx, sy, px, py, x, y);
        }

        public void reset() {
            sx = 1.0f;
            sy = 1.0f;
            px = 0.0f;
            py = 0.0f;
            x = 0.0f;
            y = 0.0f;
        }

        public void set(float sx, float sy, float px, float py, float x, float y) {
            this.sx = sx;
            this.sy = sy;
            this.px = px;
            this.py = py;
            this.x = x;
            this.y = y;
        }

        public void set(TransformInfo src) {
            if (src == null) {
                reset();
            } else {
                sx = src.sx;
                sy = src.sy;
                px = src.px;
                py = src.py;
                x = src.x;
                y = src.y;
            }
        }
    }
    //endregion Nested classes

    //region Constants
    private static final String LOG_TAG = InteractiveImageView.class.getSimpleName();
    private static final float MAX_SCALE_BREADTH_MULTIPLIER = 4.0f;
    private static final float MAX_SCALE_LENGTH_MULTIPLIER = 6.0f;
    private static final float ZOOM_PIVOT_EPSILON = 0.2f;

    @SuppressWarnings("unused")
    public static final int INTERACTIVITY_FLAG_NONE = 0;
    public static final int INTERACTIVITY_FLAG_SCROLL = 0x00000001;
    public static final int INTERACTIVITY_FLAG_FLING = 0x00000002;
    public static final int INTERACTIVITY_FLAG_SCALE = 0x00000004;
    public static final int INTERACTIVITY_FLAG_DOUBLE_TAP = 0x00000008;
    public static final int INTERACTIVITY_FLAG_EDGE_EFFECTS = 0x00000010;
    public static final int INTERACTIVITY_FLAG_ALL = INTERACTIVITY_FLAG_SCROLL |
            INTERACTIVITY_FLAG_FLING |
            INTERACTIVITY_FLAG_SCALE |
            INTERACTIVITY_FLAG_DOUBLE_TAP |
            INTERACTIVITY_FLAG_EDGE_EFFECTS;

    private static final int INVALID_FLAG_CONTENT_RECT = 0x00000001;
    private static final int INVALID_FLAG_BASELINE_IMAGE_MATRIX = 0x00000002;
    private static final int INVALID_FLAG_IMAGE_MAX_SCALE = 0x00000004;
    private static final int INVALID_FLAG_IMAGE_MIN_SCALE = 0x00000008;
    private static final int INVALID_FLAG_DEFAULT = INVALID_FLAG_BASELINE_IMAGE_MATRIX |
            INVALID_FLAG_IMAGE_MAX_SCALE |
            INVALID_FLAG_IMAGE_MIN_SCALE;

    private static final String KEY_ANIMATE = "animate";
    private static final String KEY_PX = "px";
    private static final String KEY_PY = "py";
    private static final String KEY_SX = "sx";
    private static final String KEY_SY = "sy";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    //endregion Constants

    //region Fields
    @NonNull
    private final GestureDetectorCompat mGestureDetector;

    @NonNull
    private final ScaleGestureDetector mScaleGestureDetector;

    @NonNull
    private final OverScroller mOverScroller;

    @NonNull
    private final Transformer mTransformer;

    private int mInteractivity;
    private float[] mZoomPivots;

    private ScaleType mScaleType = super.getScaleType();

    private float mMaxScaleX;
    private float mMaxScaleY;
    private float mMinScaleX;
    private float mMinScaleY;

    /*
    private float mLastScrollPx;
    private float mLastScrollPy;
    private float mLastScrollX;
    private float mLastScrollY;
    private float mLastSpan;
    private boolean mNeedsDown;
    */

    private float mLastPx;
    private float mLastPy;

    private Matrix mImageMatrixInternal = new Matrix();
    private Matrix mTransformMatrix = new Matrix();

    // TODO Maybe replace with pools
    private final TransformInfo mTransformInfo = new TransformInfo();
    private final float[] mMatrixValues = new float[9];
    private final float[] mSrcPts = new float[2];
    private final float[] mDstPts = new float[2];
    private final RectF mContentRect = new RectF();
    private final Matrix mBaselineImageMatrix = new Matrix();
    private final Matrix mImageMatrix = new Matrix();
    private final Matrix mNewImageMatrix = new Matrix();
    private final RectF mSrcRect = new RectF();
    private final RectF mDstRect = new RectF();

    // Pools
    private final PoolManager mPoolManager = new PoolManager(8);
    /*
    private final MatrixPool mMatrixPool = new MatrixPool(8);
    private final MatrixValuesPool mMatrixValuesPool = new MatrixValuesPool(8);
    private final PtsPool mPtsPool = new PtsPool(8);
    private final RectFPool mRectFPool = new RectFPool(8);
    */

    private final Object mLock = new Object();

    private Bundle mPendingTransformation = null;
    private int mInvalidFlags;
    //endregion Fields

    //region Constructors
    public InteractiveImageView(Context context) {
        super(context);
        final Initializer initializer = initializeInteractiveImageView(
                context,
                null,
                R.attr.interactiveImageViewStyle,
                0);
        mGestureDetector = initializer.gestureDetector;
        mScaleGestureDetector = initializer.scaleGestureDetector;
        mOverScroller = initializer.overScroller;
        mTransformer = initializer.transformer;
    }

    public InteractiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final Initializer initializer = initializeInteractiveImageView(
                context,
                attrs,
                R.attr.interactiveImageViewStyle,
                0);
        mGestureDetector = initializer.gestureDetector;
        mScaleGestureDetector = initializer.scaleGestureDetector;
        mOverScroller = initializer.overScroller;
        mTransformer = initializer.transformer;
    }

    public InteractiveImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final Initializer initializer =
                initializeInteractiveImageView(context, attrs, defStyleAttr, 0);
        mGestureDetector = initializer.gestureDetector;
        mScaleGestureDetector = initializer.scaleGestureDetector;
        mOverScroller = initializer.overScroller;
        mTransformer = initializer.transformer;
    }
    //endregion Constructors

    //region Inherited methods
    @Override
    public void computeScroll() {
        super.computeScroll();

        /*
        synchronized (mLock) {
            boolean needsInvalidate = false;
            if (mOverScroller.computeScrollOffset()) {
                getImageMatrixInternal(mImageMatrix);
                mNewImageMatrix.set(mImageMatrix);
                mNewImageMatrix.getValues(mMatrixValues);
                mMatrixValues[MTRANS_X] = mOverScroller.getCurrX();
                mMatrixValues[MTRANS_Y] = mOverScroller.getCurrY();
                mNewImageMatrix.setValues(mMatrixValues);
                // TODO Can I call setImageTransformInternal here?
                if (!mImageMatrix.equals(mNewImageMatrix)) {
                    if (super.getScaleType() != ScaleType.MATRIX) {
                        super.setScaleType(ScaleType.MATRIX);
                    }

                    super.setImageMatrix(mNewImageMatrix);
                    needsInvalidate = true;
                }
            } else if (mTransformer.computeTransform()) {
                needsInvalidate = setImageTransformInternal(
                        mTransformer.getCurrScaleX(),
                        mTransformer.getCurrScaleY(),
                        mTransformer.getPx(),
                        mTransformer.getPy(),
                        mTransformer.getCurrX(),
                        mTransformer.getCurrY());
            }

            if (needsInvalidate) {
                ViewCompat.postInvalidateOnAnimation(this);
            } else {
                mOverScroller.abortAnimation();
                mTransformer.abortAnimation();
            }
        }
        */
    }

    @Override
    public ScaleType getScaleType() {
        return mScaleType;
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);
        if (mPendingTransformation != null) {
            // TODO We can also defer this to when we have a layout AND a drawable?
            final float sx = mPendingTransformation.getFloat(KEY_SX);
            final float sy = mPendingTransformation.getFloat(KEY_SY);
            final float px = mPendingTransformation.getFloat(KEY_PX);
            final float py = mPendingTransformation.getFloat(KEY_PY);
            final float x = mPendingTransformation.getFloat(KEY_X, getDefaultTargetX());
            final float y = mPendingTransformation.getFloat(KEY_Y, getDefaultTargetY());
            final boolean animate = mPendingTransformation.getBoolean(KEY_ANIMATE);
            mPendingTransformation = null;
            // TODO setImageTransform(sx, sy, px, py, x, y, animate);
        }
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        mInvalidFlags |= INVALID_FLAG_CONTENT_RECT;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean retVal = false;
        if (drawableHasIntrinsicSize()) {
            retVal = mGestureDetector.onTouchEvent(event) ||
                    mScaleGestureDetector.onTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            onUp(event);
        }
        return retVal || super.onTouchEvent(event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mInvalidFlags |= INVALID_FLAG_CONTENT_RECT | INVALID_FLAG_DEFAULT;
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
//        setScaleType(mScaleType); <-- TODO Does setting this here mess up saving state after configuration change? How to reset?
        mInvalidFlags |= INVALID_FLAG_DEFAULT;
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        super.setImageMatrix(matrix);
        if (ScaleType.MATRIX == mScaleType) {
            mBaselineImageMatrix.set(matrix);
            mInvalidFlags &= ~INVALID_FLAG_BASELINE_IMAGE_MATRIX;
            mInvalidFlags |= INVALID_FLAG_IMAGE_MAX_SCALE | INVALID_FLAG_IMAGE_MIN_SCALE;
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        mInvalidFlags |= INVALID_FLAG_CONTENT_RECT | INVALID_FLAG_DEFAULT;
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        super.setPaddingRelative(start, top, end, bottom);
        mInvalidFlags |= INVALID_FLAG_CONTENT_RECT | INVALID_FLAG_DEFAULT;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        mScaleType = scaleType;
        super.setScaleType(mScaleType);
        mInvalidFlags |= INVALID_FLAG_DEFAULT;
    }
    //endregion Inherited methods

    //region Interface methods
    @Override // GestureDetector.OnGestureListener
    public boolean onDown(MotionEvent e) {
        mOverScroller.forceFinished(true);
        mTransformer.forceFinished(true);
        synchronized (mLock) {
            mSrcPts[0] = e.getX();
            mSrcPts[1] = e.getY();
            final Matrix imageMatrix = getImageMatrixInternal();
            imageMatrix.invert(mNewImageMatrix);
            mNewImageMatrix.mapPoints(mDstPts, mSrcPts);
            mLastPx = mDstPts[0];
            mLastPy = mDstPts[1];
        }
        return true;
    }

    @Override // GestureDetector.OnGestureListener
    public void onShowPress(MotionEvent e) {
    }

    @Override // GestureDetector.OnGestureListener
    public boolean onSingleTapUp(MotionEvent e) {
        if ((mInteractivity & INTERACTIVITY_FLAG_DOUBLE_TAP) == 0) {
            // If we haven't enabled double tap, fire the onSingleTapConfirmed for consistency
            this.onSingleTapConfirmed(e);
        }
        return false;
    }

    @Override // GestureDetector.OnGestureListener
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mScaleGestureDetector.isInProgress()) {
            return false;
        }

        // Constrain & transform the image
        final float x = e2.getX();
        final float y = e2.getY();
        mTransformInfo.set(getImageScaleX(), getImageScaleY(), mLastPx, mLastPy, x, y);
        final boolean constrained = constrainTransformInfo(mTransformInfo, true);
        transformImage(
                mTransformInfo.sx,
                mTransformInfo.sy,
                mLastPx,
                mLastPy,
                mTransformInfo.x,
                mTransformInfo.y);

        // If transform info was constrained, we need to re-calculate the current px/py
        if (constrained) {
            final Matrix invertedMatrix = mPoolManager.acquireMatrix();
            getImageMatrixInternal().invert(invertedMatrix);
            final PtsWrapper viewPts = mPoolManager.acquirePtsWrapper(x, y);
            final PtsWrapper drawablePts = mPoolManager.acquirePtsWrapper();
            invertedMatrix.mapPoints(drawablePts.pts, viewPts.pts);
            mLastPx = drawablePts.getX();
            mLastPy = drawablePts.getY();
            mPoolManager.releasePtsWrapper(drawablePts);
            mPoolManager.releasePtsWrapper(viewPts);
            mPoolManager.releaseMatrix(invertedMatrix);
        }

        return true;
    }

    @Override // GestureDetector.OnGestureListener
    public void onLongPress(MotionEvent e) {
    }

    @Override // GestureDetector.OnGestureListener
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        /*
        synchronized (mLock) {
            getImageMatrixInternal(mImageMatrix);
            getDrawableIntrinsicRect(mSrcRect);
            mImageMatrix.mapRect(mDstRect, mSrcRect);
            mImageMatrix.getValues(mMatrixValues);
            final float mappedWidth = mDstRect.width();
            final float mappedHeight = mDstRect.height();
            mOverScroller.fling(
                    (int) mMatrixValues[MTRANS_X],
                    (int) mMatrixValues[MTRANS_Y],
                    (int) velocityX,
                    (int) velocityY,
                    (int) getImageMinTransX(mappedWidth),
                    (int) getImageMaxTransX(mappedWidth),
                    (int) getImageMinTransY(mappedHeight),
                    (int) getImageMaxTransY(mappedHeight),
                    0,
                    0);
        }
        return true;
        */
        return false;
    }

    @Override // GestureDetector.OnDoubleTapListener
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override // GestureDetector.OnDoubleTapListener
    public boolean onDoubleTap(MotionEvent e) {
        return false;
    }

    @Override // GestureDetector.OnDoubleTapListener
    public boolean onDoubleTapEvent(MotionEvent e) {
        /*
        if ((mInteractivity & INTERACTIVITY_FLAG_DOUBLE_TAP) != 0) {
            if (e.getAction() == MotionEvent.ACTION_UP) {
                synchronized (mLock) {
                    final float nextZoomPivot = getNextZoomPivot();
                    final float minSx = getImageMinScaleX();
                    final float minSy = getImageMinScaleY();
                    mSrcPts[0] = e.getX();
                    mSrcPts[1] = e.getY();
                    getImageMatrixInternal(mImageMatrix);
                    mapViewPointToDrawablePoint(mDstPts, mSrcPts, mImageMatrix);
                    setImageTransform(
                            minSx + (getImageMaxScaleX() - minSx) * nextZoomPivot,
                            minSy + (getImageMaxScaleY() - minSy) * nextZoomPivot,
                            mDstPts[0],
                            mDstPts[1],
                            getDefaultTargetX(),
                            getDefaultTargetY(),
                            true);
                    return true;
                }
            }
        }
        */
        return false;
    }

    @Override // ScaleGestureDetector.OnScaleGestureListener
    public boolean onScale(ScaleGestureDetector detector) {
        /*
        final float currentSpan = detector.getCurrentSpan();
        synchronized (mLock) {
            final float spanDelta = (currentSpan / mLastSpan);
            final float sx = getImageScaleX() * spanDelta;
            final float sy = getImageScaleY() * spanDelta;
            final float x = detector.getFocusX();
            final float y = detector.getFocusY();

            final boolean transformed =
                    setImageTransform(sx, sy, mLastScrollPx, mLastScrollPy, x, y, false, true);

            if (!transformed) {
                // TODO If we didn't scale in EITHER direction we need to re-calc that direction
                // If the image didn't move while we were scrolling, re-calculate new values
                // for mLastScrollPx/mLastScrollPy
                getImageMatrixInternal(mImageMatrix);
                mSrcPts[0] = x;
                mSrcPts[1] = y;
                mapViewPointToDrawablePoint(mDstPts, mSrcPts, mImageMatrix);
                mLastScrollPx = mDstPts[0];
                mLastScrollPy = mDstPts[1];
            }
        }
        mLastSpan = currentSpan;
        return true;
        */
        return false;
    }

    @Override // ScaleGestureDetector.OnScaleGestureListener
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        /*
        synchronized (mLock) {
            getImageMatrixInternal(mImageMatrix);
            mSrcPts[0] = detector.getFocusX();
            mSrcPts[1] = detector.getFocusY();
            mapViewPointToDrawablePoint(mDstPts, mSrcPts, mImageMatrix);
            mLastScrollPx = mDstPts[0];
            mLastScrollPy = mDstPts[1];
            mLastSpan = detector.getCurrentSpan();
        }
        return true;
        */
        return false;
    }

    @Override // ScaleGestureDetector.OnScaleGestureListener
    public void onScaleEnd(ScaleGestureDetector detector) {
        /*
        mNeedsDown = true;
        */
    }
    //endregion Interface methods

    //region Methods
    protected int getDrawableIntrinsicHeight() {
        final Drawable dr = getDrawable();
        return (dr == null ? -1 : dr.getIntrinsicHeight());
    }

    protected int getDrawableIntrinsicWidth() {
        final Drawable dr = getDrawable();
        return (dr == null ? -1 : dr.getIntrinsicWidth());
    }

    public float getImagePivotX() {
        synchronized (mLock) {
            final Matrix imageMatrix = getImageMatrixInternal();
            mSrcPts[0] = getDefaultTargetX();
            mSrcPts[1] = 0.0f;
            mapViewPointToDrawablePoint(mDstPts, mSrcPts, imageMatrix);
            return mDstPts[0];
        }
    }

    public float getImagePivotY() {
        synchronized (mLock) {
            final Matrix imageMatrix = getImageMatrixInternal();
            mSrcPts[0] = 0.0f;
            mSrcPts[1] = getDefaultTargetY();
            mapViewPointToDrawablePoint(mDstPts, mSrcPts, imageMatrix);
            return mDstPts[1];
        }
    }

    public float getImageMaxScaleX() {
        getImageMaxScale();
        return mMaxScaleX;
    }

    public float getImageMaxScaleY() {
        getImageMaxScale();
        return mMaxScaleY;
    }

    public float getImageMinScaleX() {
        getImageMinScale();
        return mMinScaleX;
    }

    public float getImageMinScaleY() {
        getImageMinScale();
        return mMinScaleY;
    }

    public float getImageScaleX() {
        synchronized (mLock) {
            final Matrix imageMatrix = getImageMatrixInternal();
            imageMatrix.getValues(mMatrixValues);
            return mMatrixValues[MSCALE_X];
        }
    }

    public float getImageScaleY() {
        synchronized (mLock) {
            final Matrix imageMatrix = getImageMatrixInternal();
            imageMatrix.getValues(mMatrixValues);
            return mMatrixValues[MSCALE_Y];
        }
    }

    @SuppressWarnings("unused")
    public int getInteractivity() {
        return mInteractivity;
    }

    public boolean onUp(MotionEvent e) {
        return false;
    }

    public void setInteractivity(int flags) {
        mInteractivity = flags;
    }

    /*
    @SuppressWarnings("UnusedReturnValue")
    public boolean setImageTransform(float sx, float sy, float px, float py) {
        return setImageTransform(sx, sy, px, py, false);
    }

    @SuppressWarnings("SameParameterValue")
    public boolean setImageTransform(float sx, float sy, float px, float py, boolean animate) {
        if (!ViewCompat.isLaidOut(this)) {
            mPendingTransformation = newPendingTransformation(sx, sy, px, py, NaN, NaN, animate);
            return false;
        }
        final float x = getDefaultTargetX();
        final float y = getDefaultTargetY();
        return setImageTransform(sx, sy, px, py, x, y, animate);
    }

    @SuppressWarnings("unused")
    public boolean setImageTransform(float sx, float sy, float px, float py, float x, float y) {
        return setImageTransform(sx, sy, px, py, x, y, false);
    }

    public boolean setImageTransform(
            float sx,
            float sy,
            float px,
            float py,
            float x,
            float y,
            boolean animate) {
        if (!ViewCompat.isLaidOut(this)) {
            mPendingTransformation = newPendingTransformation(sx, sy, px, py, x, y, animate);
            return false;
        }

        return setImageTransform(sx, sy, px, py, x, y, animate, false);
    }
    */

    public void setZoomPivots(float... pivots) {
        mZoomPivots = pivots;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean transformImage(float sx, float sy, float px, float py) {
        return transformImage(sx, sy, px, py, getDefaultTargetX(), getDefaultTargetY());
    }

    /* Currently, this is a way of transforming without any sort of check */
    public boolean transformImage(float sx, float sy, float px, float py, float x, float y) {
        // TODO When do I check if image has intrinsic size? Or does it matter?
        synchronized (mLock) {
            if (super.getScaleType() != ScaleType.MATRIX) {
                super.setScaleType(ScaleType.MATRIX);
            }
            final boolean transformed = transformToMatrix(sx, sy, px, py, x, y, mTransformMatrix);
            super.setImageMatrix(mTransformMatrix);
            return transformed;
        }
    }
    //endregion Methods

    //region Protected methods
    protected boolean canScrollX() {
        if (drawableHasIntrinsicSize()) {
            synchronized (mLock) {
                final Matrix imageMatrix = getImageMatrixInternal();
                getDrawableIntrinsicRect(mSrcRect);
                imageMatrix.mapRect(mDstRect, mSrcRect);
                return (mDstRect.width() > getContentWidth());
            }
        } else {
            return false;
        }
    }

    protected boolean canScrollY() {
        if (drawableHasIntrinsicSize()) {
            synchronized (mLock) {
                final Matrix imageMatrix = getImageMatrixInternal();
                getDrawableIntrinsicRect(mSrcRect);
                imageMatrix.mapRect(mDstRect, mSrcRect);
                return (mDstRect.height() > getContentHeight());
            }
        } else {
            return false;
        }
    }

    protected boolean constrainTransformInfo(TransformInfo info, boolean fromUser) {
        boolean constrained = false;
        if (info != null) {
            if (drawableHasIntrinsicSize()) {
                // Get matrix and values
                final Matrix matrix = mPoolManager.acquireMatrix();
                matrix.set(getImageMatrixInternal());
                final MatrixValuesWrapper matrixValues = mPoolManager.acquireMatrixValuesWrapper();
                matrix.getValues(matrixValues.values);

                // Clamp scale
                final float clampedSx =
                        MathUtils.clamp(info.sx, getImageMinScaleX(), getImageMaxScaleX());
                final float clampedSy =
                        MathUtils.clamp(info.sy, getImageMinScaleY(), getImageMaxScaleY());

                // Pre-scale the matrix to the clamped scale if needed
                if (info.sx != clampedSx || info.sy != clampedSy) {
                    Log.d(LOG_TAG, "constrainTransformInfo: need to postScale");
                    final float deltaSx = clampedSx / matrixValues.values[MSCALE_X];
                    final float deltaSy = clampedSy / matrixValues.values[MSCALE_Y];
                    matrix.preScale(deltaSx, deltaSy);
                    info.sx = clampedSx;
                    info.sy = clampedSy;
                }

                // Get the mapped rectangle using the updated matrix
                final Drawable d = getDrawable();
                final RectF drawableRect = mPoolManager.acquireRectF(
                        0.0f,
                        0.0f,
                        (float) d.getIntrinsicWidth(),
                        (float) d.getIntrinsicHeight());
                final RectF mappedRect = mPoolManager.acquireRectF();
                matrix.mapRect(mappedRect, drawableRect);

                // Determine the amount we can scroll
                final RectF contentRect = getContentRect();
                final float scrollableX =
                        Math.max(mappedRect.width() - contentRect.width(), 0.0f);
                final float scrollableY =
                        Math.max(mappedRect.height() - contentRect.height(), 0.0f);

                // Map px, py to view coordinates
                final PtsWrapper drawablePts = mPoolManager.acquirePtsWrapper(info.px, info.py);
                final PtsWrapper viewPts = mPoolManager.acquirePtsWrapper();
                matrix.mapPoints(viewPts.pts, drawablePts.pts);
                final float mappedPx = viewPts.getX() - matrixValues.values[MTRANS_X];
                final float mappedPy = viewPts.getY() - matrixValues.values[MTRANS_Y];

                // Find desired translate x/y
                final float dx = mappedPx - info.x;
                final float dy = mappedPy - info.y;
                final float clampedDx = MathUtils.clamp(dx, 0, scrollableX);
                final float clampedDy = MathUtils.clamp(dy, 0, scrollableY);
                final float clampedX = mappedPx - clampedDx;
                final float clampedY = mappedPy - clampedDy;

                if (info.x != clampedX || info.y != clampedY) {
                    info.x = clampedX;
                    info.y = clampedY;
                    constrained = true;
                }

                mPoolManager.releasePtsWrapper(viewPts);
                mPoolManager.releasePtsWrapper(drawablePts);
                mPoolManager.releaseRectF(mappedRect);
                mPoolManager.releaseRectF(drawableRect);
                mPoolManager.releaseMatrixValuesWrapper(matrixValues);
                mPoolManager.releaseMatrix(matrix);
            } else {
                // TODO Not really necessary
                info.sx = info.sy = 1.0f;
                info.x = info.y = 0.0f;
            }
        }
        return constrained;
    }

    /*
    @SuppressWarnings("unused")
    protected void clampTransform(
            float sx,
            float sy,
            float px,
            float py,
            float x,
            float y,
            @NonNull final PointF outScale,
            @NonNull final PointF outViewPt,
            final boolean fromUser) {
        synchronized (mLock) {
            // First, update scale
            getImageMinScale();
            getImageMaxScale();
            outScale.set(MathUtils.clamp(sx, mMinScaleX, mMaxScaleX),
                    MathUtils.clamp(sy, mMinScaleY, mMaxScaleY));

            // Do we want to lock ratio? Or TODO do that in pinch scale?

            // Get the resulting rect at the scaled size TODO This could be a helper method
            getImageMatrixInternal(mImageMatrix);
            mImageMatrix.getValues(mMatrixValues);
            mMatrixValues[MSCALE_X] = outScale.x;
            mMatrixValues[MSCALE_Y] = outScale.y;
            mImageMatrix.setValues(mMatrixValues);
            getDrawableIntrinsicRect(mSrcRect);
            mImageMatrix.mapRect(mDstRect, mSrcRect);
            final float scaledImageWidth = mDstRect.width();
            final float scaledImageHeight = mDstRect.height();

            mSrcPts[0] = px;
            mSrcPts[1] = py;
            mImageMatrix.mapPoints(mDstPts, mSrcPts);

            // What would tx/ty be if we moved drawablePt to viewPt?
            final float tx = x - (mDstPts[0] - mDstRect.left);
            final float ty = y - (mDstPts[1] - mDstRect.top);
            final float clampedTx = MathUtils.clamp(
                    tx,
                    getImageMinTransX(scaledImageWidth),
                    getImageMaxTransX(scaledImageWidth));
            final float clampedTy = MathUtils.clamp(
                    ty,
                    getImageMinTransY(scaledImageHeight),
                    getImageMaxTransY(scaledImageHeight));

            // With values clampedTx/clampedTy, where does the drawable point wind up?
            mMatrixValues[MTRANS_X] = clampedTx;
            mMatrixValues[MTRANS_Y] = clampedTy;
            mImageMatrix.setValues(mMatrixValues);

            mImageMatrix.mapPoints(mDstPts, mSrcPts);
            outViewPt.x = mDstPts[0];
            outViewPt.y = mDstPts[1];
        }
    }
    */

    protected boolean drawableHasIntrinsicSize() {
        return getDrawableIntrinsicWidth() > 0 && getDrawableIntrinsicHeight() > 0;
    }

    protected int getContentWidth() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    protected int getContentHeight() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    @SuppressWarnings("SameParameterValue")
    protected void getBaselineImageMatrix(Matrix outMatrix) {
        getBaselineImageMatrix(mScaleType, outMatrix);
    }

    protected void getBaselineImageMatrix(ScaleType scaleType, Matrix outMatrix) {
        synchronized (mLock) {
            if ((mInvalidFlags & INVALID_FLAG_BASELINE_IMAGE_MATRIX) != 0) {
                mInvalidFlags &= ~INVALID_FLAG_BASELINE_IMAGE_MATRIX;
                if (drawableHasIntrinsicSize()) {
                    // We need to do the scaling ourselves.
                    final int dwidth = getDrawableIntrinsicWidth();
                    final int dheight = getDrawableIntrinsicHeight();

                    final int vwidth = getContentHeight();
                    final int vheight = getContentWidth();

                    final boolean fits = (dwidth < 0 || vwidth == dwidth)
                            && (dheight < 0 || vheight == dheight);

                    if (ScaleType.MATRIX == scaleType) {
                        // Use the specified matrix as-is.
                        mBaselineImageMatrix.set(getImageMatrix());
                    } else if (fits) {
                        // The bitmap fits exactly, no transform needed.
                        mBaselineImageMatrix.reset();
                    } else if (ScaleType.CENTER == scaleType) {
                        // Center bitmap in view, no scaling.
                        mBaselineImageMatrix.setTranslate(
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

                        mBaselineImageMatrix.setScale(scale, scale);
                        mBaselineImageMatrix.postTranslate(Math.round(dx), Math.round(dy));
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

                        mBaselineImageMatrix.setScale(scale, scale);
                        mBaselineImageMatrix.postTranslate(dx, dy);
                    } else {
                        // Generate the required transform.
                        mSrcRect.set(0.0f, 0.0f, dwidth, dheight);
                        mDstRect.set(0.0f, 0.0f, vwidth, vheight);
                        mBaselineImageMatrix.setRectToRect(
                                mSrcRect,
                                mDstRect,
                                scaleTypeToScaleToFit(scaleType));
                    }
                } else {
                    mBaselineImageMatrix.reset();
                }
            }

            if (outMatrix != null && outMatrix != mBaselineImageMatrix) {
                outMatrix.set(mBaselineImageMatrix);
            }
        }
    }

    /**
     * Returns the view's content rectangle; that is, the rectangle defined by the view
     * minus any padding. Do not change this rectangle in place but make a copy.
     * @return
     */
    protected RectF getContentRect() {
        if ((mInvalidFlags & INVALID_FLAG_CONTENT_RECT) != 0) {
            mInvalidFlags &= ~INVALID_FLAG_CONTENT_RECT;
            mContentRect.set(
                    getPaddingLeft(),
                    getPaddingTop(),
                    getWidth() - getPaddingRight(),
                    getHeight() - getPaddingBottom());
        }
        return mContentRect;
    }

    /**
     * Returns the view's optional matrix, amended to return a valid matrix when the scale type is
     * set to FIT_XY. This is applied to the view's drawable when it is drawn. If there is no
     * matrix, this method will return an identity matrix. Do not change this matrix in place but
     * make a copy. If you want a different matrix applied to the drawable, be sure to call
     * setImageMatrix().
     * @return The view's optional matrix
     */
    protected Matrix getImageMatrixInternal() {
        if (ScaleType.FIT_XY == super.getScaleType()) {
            getBaselineImageMatrix(ScaleType.FIT_XY, mImageMatrixInternal);
        } else {
            mImageMatrixInternal.set(super.getImageMatrix());
        }
        return mImageMatrixInternal;
    }

    // TODO JavaDoc needs to state that method must call setImageMaxScale if overridden
    protected void getImageMaxScale() {
        // TODO I might need to take into account Skew & Perspective!
        synchronized (mLock) {
            if ((mInvalidFlags & INVALID_FLAG_IMAGE_MAX_SCALE) != 0) {
                mInvalidFlags &= ~INVALID_FLAG_IMAGE_MAX_SCALE;
                final float maxScaleX;
                final float maxScaleY;
                if (drawableHasIntrinsicSize()) {
                    getDrawableIntrinsicRect(mSrcRect);
                    getBaselineImageMatrix(null);
                    final float[] values = new float[9];
                    mBaselineImageMatrix.getValues(values);
                    mBaselineImageMatrix.mapRect(mDstRect, mSrcRect);
                    final float baselineWidth = mDstRect.width();
                    final float baselineHeight = mDstRect.height();
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
                    final int availableWidth = getContentHeight();
                    final int availableHeight = getContentWidth();
                    final int availableSize;
                    if (baselineWidth < baselineHeight) {
                        availableSize = availableWidth;
                    } else if (baselineWidth > baselineHeight) {
                        availableSize = availableHeight;
                    } else {
                        availableSize = Math.min(availableWidth, availableHeight);
                    }
                    final float viewBasedScale = availableSize / baselineBreadth;
                    final float scale = Math.max(screenBasedScale, viewBasedScale);
                    maxScaleX = scale * values[MSCALE_X];
                    maxScaleY = scale * values[MSCALE_Y];
                } else {
                    maxScaleX = maxScaleY = 1.0f;
                }
                setImageMaxScale(maxScaleX, maxScaleY);
            }
        }
    }

    protected float getImageMaxTransX(float scaledImageWidth) {
        return getImageTransBounds(
                getContentHeight(),
                scaledImageWidth,
                ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL,
                true);
    }

    protected float getImageMaxTransY(float scaledImageHeight) {
        return getImageTransBounds(
                getContentWidth(),
                scaledImageHeight,
                false,
                true);
    }

    protected float getImageMinTransX(float scaledImageWidth) {
        return getImageTransBounds(
                getContentHeight(),
                scaledImageWidth,
                ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL,
                false);
    }

    protected float getImageMinTransY(float scaledImageHeight) {
        return getImageTransBounds(
                getContentWidth(),
                scaledImageHeight,
                false,
                false);
    }

    // TODO JavaDoc needs to state that method must call setImageMinScale if overridden
    protected void getImageMinScale() {
        synchronized (mLock) {
            // TODO I might need to take into account Skew & Perspective!
            if ((mInvalidFlags & INVALID_FLAG_IMAGE_MIN_SCALE) != 0) {
                mInvalidFlags &= ~INVALID_FLAG_IMAGE_MIN_SCALE;
                getBaselineImageMatrix(null);
                mBaselineImageMatrix.getValues(mMatrixValues);
                setImageMinScale(mMatrixValues[MSCALE_X], mMatrixValues[MSCALE_Y]);
            }
        }
    }

    // TODO REMOVE?
    protected void mapViewPointToDrawablePoint(float[] dst, float[] src, Matrix imageMatrix) {
        synchronized (mLock) {
            imageMatrix.invert(mNewImageMatrix);
            mNewImageMatrix.mapPoints(dst, src);
        }
    }

    protected static Matrix.ScaleToFit scaleTypeToScaleToFit(ScaleType scaleType) {
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

    protected void setImageMaxScale(float sx, float sy) {
        mInvalidFlags &= ~INVALID_FLAG_IMAGE_MAX_SCALE;
        mMaxScaleX = sx;
        mMaxScaleY = sy;
    }

    protected void setImageMinScale(float sx, float sy) {
        mInvalidFlags &= ~INVALID_FLAG_IMAGE_MIN_SCALE;
        mMinScaleX = sx;
        mMinScaleY = sy;
    }

    /*
    protected final boolean setImageTransform(
            float sx,
            float sy,
            float px,
            float py,
            float x,
            float y,
            boolean animate,
            boolean fromUser) {
        synchronized (mLock) {
            // TODO Use pools
            final PointF outScale = new PointF();
            final PointF outViewPt = new PointF();
            clampTransform(sx, sy, px, py, x, y, outScale, outViewPt, fromUser);
            if (animate) {
                mSrcPts[0] = px;
                mSrcPts[1] = py;
                getImageMatrixInternal(mImageMatrix);
                mImageMatrix.mapPoints(mDstPts, mSrcPts);
                mTransformer.startTransform(
                        px,
                        py,
                        getImageScaleX(),
                        getImageScaleY(),
                        mDstPts[0],
                        mDstPts[1],
                        outScale.x,
                        outScale.y,
                        outViewPt.x,
                        outViewPt.y);
                ViewCompat.postInvalidateOnAnimation(this);
                return true;
            } else {
                return setImageTransformInternal(outScale.x, outScale.y, px, py, outViewPt.x, outViewPt.y);
            }
        }
    }

    protected final boolean setImageTransformInternal(
            float sx,
            float sy,
            float px,
            float py,
            float x,
            float y) {
        // First, I need to resolve scale and map px and py to a scaled location
        getImageMatrixInternal(mImageMatrix);
        mNewImageMatrix.set(mImageMatrix);
        mNewImageMatrix.getValues(mMatrixValues);
        mMatrixValues[MSCALE_X] = sx;
        mMatrixValues[MSCALE_Y] = sy;
        mMatrixValues[MTRANS_X] = 0;
        mMatrixValues[MTRANS_Y] = 0;
        mNewImageMatrix.setValues(mMatrixValues);
        mSrcPts[0] = px;
        mSrcPts[1] = py;
        mNewImageMatrix.mapPoints(mDstPts, mSrcPts);
        final float tx = x - getPaddingLeft() - mDstPts[0];
        final float ty = y - getPaddingTop() - mDstPts[1];

        mMatrixValues[MTRANS_X] = tx;
        mMatrixValues[MTRANS_Y] = ty;
        mNewImageMatrix.setValues(mMatrixValues);

        if (mImageMatrix.equals(mNewImageMatrix)) {
            return false;
        }
        if (super.getScaleType() != ScaleType.MATRIX) {
            super.setScaleType(ScaleType.MATRIX);
        }
        super.setImageMatrix(mNewImageMatrix);
        return true;
    }
    */
    //endregion Protected methods

    //region Private methods
    private float getDefaultTargetX() {
        return getPaddingLeft() + getContentHeight() * 0.5f;
    }

    private float getDefaultTargetY() {
        return getPaddingTop() + getContentWidth() * 0.5f;
    }

    private void getDrawableIntrinsicRect(@NonNull final RectF outRect) {
        outRect.set(
                0.0f,
                0.0f,
                getDrawableIntrinsicWidth(),
                getDrawableIntrinsicHeight());
    }

    private float getImageTransBounds(
            int availableSize,
            float scaledImageSize,
            boolean isRtl,
            boolean isMax) {
        final float diff = availableSize - scaledImageSize;
        if (diff <= 0) {
            // Image size is larger than or equal to available size
            return (isMax ? 0.0f : diff);
        } else switch (mScaleType) {
            // Image size is smaller than available size
            case FIT_START:
                return (isRtl ? diff : 0.0f);
            case FIT_END:
                return (isRtl ? 0.0f : diff);
            default:
                return diff * 0.5f;
        }
    }

    private float getNextZoomPivot() {
        float nextZoomPivot = 0.0f;
        if (mZoomPivots != null) {
            final float minSx = getImageMinScaleX();
            final float minSy = getImageMinScaleY();
            float relativeSx = (getImageScaleX() - minSx) / (getImageMaxScaleX() - minSx);
            float relativeSy = (getImageScaleY() - minSy) / (getImageMaxScaleY() - minSy);
            if (Float.isNaN(relativeSx) || Float.isInfinite(relativeSx)) {
                relativeSx = 0.0f;
            }
            if (Float.isNaN(relativeSy) || Float.isInfinite(relativeSy)) {
                relativeSy = 0.0f;
            }
            boolean foundX = false;
            boolean foundY = false;
            for (final float zoomPivot : mZoomPivots) {
                if (zoomPivot - relativeSx > ZOOM_PIVOT_EPSILON) {
                    foundX = true;
                    nextZoomPivot = zoomPivot;
                }
                if (zoomPivot - relativeSy > ZOOM_PIVOT_EPSILON) {
                    foundY = true;
                    nextZoomPivot = zoomPivot;
                }
                if (foundX && foundY) {
                    break;
                }
            }
        }
        return nextZoomPivot;
    }

    @SuppressWarnings("SameParameterValue")
    private Initializer initializeInteractiveImageView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.InteractiveImageView,
                defStyleAttr,
                defStyleRes);

        setInteractivity(a.getInt(
                R.styleable.InteractiveImageView_interactivity,
                INTERACTIVITY_FLAG_ALL));

        final @ArrayRes int resId =
                a.getResourceId(R.styleable.InteractiveImageView_zoomPivots, -1);
        if (resId != -1) {
            TypedArray ta = getResources().obtainTypedArray(resId);
            final int length = ta.length();
            final float[] zoomPivots = new float[length];
            for (int i = 0; i < length; i++) {
                zoomPivots[i] = ta.getFloat(i, Float.NaN);
            }
            setZoomPivots(zoomPivots);
            ta.recycle();
        }

        a.recycle();

        return new Initializer(this);
    }

    private static Bundle newPendingTransformation(
            float sx,
            float sy,
            float px,
            float py,
            float x,
            float y,
            boolean animate) {
        Bundle bundle = new Bundle();
        bundle.putFloat(KEY_SX, sx);
        bundle.putFloat(KEY_SY, sy);
        bundle.putFloat(KEY_PX, px);
        bundle.putFloat(KEY_PY, py);
        if (!Float.isNaN(x)) {
            bundle.putFloat(KEY_X, x);
        }
        if (!Float.isNaN(y)) {
            bundle.putFloat(KEY_Y, y);
        }
        bundle.putBoolean(KEY_ANIMATE, animate);
        return bundle;
    }

    private boolean transformToMatrix(
            float sx,
            float sy,
            float px,
            float py,
            float x,
            float y,
            Matrix outMatrix) {
        if (outMatrix != null) {
            final Matrix imageMatrix = getImageMatrixInternal();
            outMatrix.set(getImageMatrixInternal());

            // Get current location of px/py in the view
            mSrcPts[0] = px;
            mSrcPts[1] = py;
            outMatrix.mapPoints(mDstPts, mSrcPts);

            outMatrix.getValues(mMatrixValues);
            final float deltaSx = sx / mMatrixValues[MSCALE_X];
            final float deltaSy = sy / mMatrixValues[MSCALE_Y];
            outMatrix.preScale(deltaSx, deltaSy, px, py);

            final float deltaTx = x - mDstPts[0];
            final float deltaTy = y - mDstPts[1];
            outMatrix.postTranslate(deltaTx, deltaTy);
            return !outMatrix.equals(imageMatrix);
        }
        return false;
    }
    //endregion Private methods
}
