package com.codepunk.demo.interactiveimageview.version6;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.OverScroller;

import com.codepunk.demo.R;
import com.codepunk.demo.support.DisplayCompat;

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
        final ScaleScroller scaleScroller;

        Initializer(InteractiveImageView view) {
            final Context context = view.getContext();
            gestureDetector = new GestureDetectorCompat(context, view);
            gestureDetector.setIsLongpressEnabled(false);
            gestureDetector.setOnDoubleTapListener(view);
            scaleGestureDetector = new ScaleGestureDetector(context, view);
            overScroller = new OverScroller(context);
            scaleScroller = new ScaleScroller(context);
        }
    }

    // TODO VERSION6
    protected static class ScaleScroller {
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

        public ScaleScroller(Context context) {
            mInterpolator = new DecelerateInterpolator();
            mAnimationDurationMillis =
                    context.getResources().getInteger(android.R.integer.config_shortAnimTime);
        }

        /**
         * Aborts the animation, setting the current values to the ending value.
         *
         * @see android.widget.Scroller#abortAnimation()
         */
        @SuppressWarnings("unused")
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
        void startScaleAndScroll(
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
        boolean computeScaleAndScroll() {
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
    //endregion Nested classes

    //region Constants
    private static final String LOG_TAG = InteractiveImageView.class.getSimpleName();
    private static final float MAX_SCALE_BREADTH_MULTIPLIER = 4.0f;
    private static final float MAX_SCALE_LENGTH_MULTIPLIER = 6.0f;
    private static final float FLOAT_EPSILON = 0.005f;
    private static final float ZOOM_PIVOT_EPSILON = 0.2f;
    private static final float CENTER = 0.5f;

    public static final int INTERACTIVITY_FLAG_NONE = 0;
    public static final int INTERACTIVITY_FLAG_SCROLL = 0x00000001;
    public static final int INTERACTIVITY_FLAG_FLING = 0x00000002;
    public static final int INTERACTIVITY_FLAG_SCALE = 0x00000004;
    public static final int INTERACTIVITY_FLAG_DOUBLE_TAP = 0x00000008;
    public static final int INTERACTIVITY_FLAG_ALL = INTERACTIVITY_FLAG_SCROLL |
            INTERACTIVITY_FLAG_FLING |
            INTERACTIVITY_FLAG_SCALE |
            INTERACTIVITY_FLAG_DOUBLE_TAP;

    private static final int INVALID_FLAG_BASELINE_IMAGE_MATRIX = 0x00000001;
    private static final int INVALID_FLAG_IMAGE_MAX_SCALE = 0x00000002;
    private static final int INVALID_FLAG_IMAGE_MIN_SCALE = 0x00000004;
    private static final int INVALID_FLAG_DEFAULT = INVALID_FLAG_BASELINE_IMAGE_MATRIX |
            INVALID_FLAG_IMAGE_MAX_SCALE |
            INVALID_FLAG_IMAGE_MIN_SCALE;

    private static final String KEY_ANIMATE = "animate";
    private static final String KEY_PX = "px";
    private static final String KEY_PY = "py";
    private static final String KEY_RELATIVE = "relative";
    private static final String KEY_SX = "sx";
    private static final String KEY_SY = "sy";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    //endregion Constants

    //region Fields
    private final @NonNull GestureDetectorCompat mGestureDetector;
    private final @NonNull ScaleGestureDetector mScaleGestureDetector;
    private final @NonNull OverScroller mOverScroller;
    private final @NonNull ScaleScroller mScaleScroller;

    private int mInteractivity;
    private float[] mZoomPivots;

    private ScaleType mScaleType = super.getScaleType();

    private float mMaxScaleX;
    private float mMaxScaleY;
    private float mMinScaleX;
    private float mMinScaleY;

    private float mLastScrollPx;
    private float mLastScrollPy;
    private float mLastScrollX;
    private float mLastScrollY;
    private float mLastSpan;
    private PointF mScaleBeginDrawablePoint = new PointF();

    private final float[] mMatrixValues = new float[9];
    private final float[] mSrcPts = new float[2];
    private final float[] mDstPts = new float[2];
    private final Matrix mBaselineImageMatrix = new Matrix();
    private final Matrix mImageMatrix = new Matrix();
    private final Matrix mNewImageMatrix = new Matrix();
    private final RectF mSrcRect = new RectF();
    private final RectF mDstRect = new RectF();

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
        mScaleScroller = initializer.scaleScroller;
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
        mScaleScroller = initializer.scaleScroller;
    }

    public InteractiveImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final Initializer initializer =
                initializeInteractiveImageView(context, attrs, defStyleAttr, 0);
        mGestureDetector = initializer.gestureDetector;
        mScaleGestureDetector = initializer.scaleGestureDetector;
        mOverScroller = initializer.overScroller;
        mScaleScroller = initializer.scaleScroller;
    }
    //endregion Constructors

    //region Inherited methods
    @Override
    public void computeScroll() {
        super.computeScroll();

        synchronized (mLock) {
            boolean needsInvalidate = false;
            if (mOverScroller.computeScrollOffset()) {
                getImageMatrixInternal(mImageMatrix);
                mNewImageMatrix.set(mImageMatrix);
                mNewImageMatrix.getValues(mMatrixValues);
                mMatrixValues[MTRANS_X] = mOverScroller.getCurrX();
                mMatrixValues[MTRANS_Y] = mOverScroller.getCurrY();
                mNewImageMatrix.setValues(mMatrixValues);

                if (!mImageMatrix.equals(mNewImageMatrix)) {
                    if (super.getScaleType() != ScaleType.MATRIX) {
                        super.setScaleType(ScaleType.MATRIX);
                    }

                    super.setImageMatrix(mNewImageMatrix);
                    needsInvalidate = true;
                }
            } else if (mScaleScroller.computeScaleAndScroll()) {
                final float px = mScaleScroller.getPx();
                final float py = mScaleScroller.getPy();
                final float sx = mScaleScroller.getCurrScaleX();
                final float sy = mScaleScroller.getCurrScaleY();
                final float x = mScaleScroller.getCurrX();
                final float y = mScaleScroller.getCurrY();
                /*
                needsInvalidate = setImageTransformation(sx, sy, px, py, x, y, false);
                */
            }

            if (needsInvalidate) {
                ViewCompat.postInvalidateOnAnimation(this);
            } else {
                mOverScroller.abortAnimation();
                mScaleScroller.abortAnimation();
            }
        }
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
            final boolean relative = mPendingTransformation.getBoolean(KEY_RELATIVE);
            final float sx = mPendingTransformation.getFloat(KEY_SX);
            final float sy = mPendingTransformation.getFloat(KEY_SY);
            final float px = mPendingTransformation.getFloat(KEY_PX);
            final float py = mPendingTransformation.getFloat(KEY_PY);
            final float x = mPendingTransformation.getFloat(KEY_X);
            final float y = mPendingTransformation.getFloat(KEY_Y);
            final boolean animate = mPendingTransformation.getBoolean(KEY_ANIMATE);
            if (relative) {
                setImageTransformRelative(sx, sy, px, py, x, y, animate);
            } else {
                setImageTransform(sx, sy, px, py, x, y, animate);
            }
            mPendingTransformation = null;
        }
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
        mInvalidFlags |= INVALID_FLAG_DEFAULT;
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

    //region Interface methods
    @Override // GestureDetector.OnGestureListener
    public boolean onDown(MotionEvent e) {
        mOverScroller.forceFinished(true);
        mScaleScroller.forceFinished(true);
        synchronized (mLock) {
            getImageMatrixInternal(mImageMatrix);
            mLastScrollX = e.getX();
            mLastScrollY = e.getY();
            mSrcPts[0] = mLastScrollX;
            mSrcPts[1] = mLastScrollY;
            mapViewPointToDrawablePoint(mDstPts, mSrcPts, mImageMatrix);
            mLastScrollPx = mDstPts[0];
            mLastScrollPy = mDstPts[1];
        }
        return true;
    }

    @Override // GestureDetector.OnGestureListener
    public void onShowPress(MotionEvent e) {
    }

    @Override // GestureDetector.OnGestureListener
    public boolean onSingleTapUp(MotionEvent e) {
        if ((mInteractivity & INTERACTIVITY_FLAG_DOUBLE_TAP) != INTERACTIVITY_FLAG_DOUBLE_TAP) {
            // If we haven't enabled double tap, fire the onSingleTapConfirmed for consistency
            this.onSingleTapConfirmed(e);
        }
        return false;
    }

    @Override // GestureDetector.OnGestureListener
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        synchronized (mLock) {
            setImageTransformInternal(
                    getImageScaleX(),
                    getImageScaleY(),
                    mLastScrollPx,
                    mLastScrollPy,
                    mLastScrollX - distanceX,
                    mLastScrollY - distanceY,
                    true);

            // TODO This code is repeating a bit. Consolidate?
            getImageMatrixInternal(mImageMatrix);
            mLastScrollX = e2.getX();
            mLastScrollY = e2.getY();
            mSrcPts[0] = mLastScrollX;
            mSrcPts[1] = mLastScrollY;
            mapViewPointToDrawablePoint(mDstPts, mSrcPts, mImageMatrix);
            mLastScrollPx = mDstPts[0];
            mLastScrollPy = mDstPts[1];
        }
        return true;
    }

    @Override // GestureDetector.OnGestureListener
    public void onLongPress(MotionEvent e) {
    }

    @Override // GestureDetector.OnGestureListener
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        synchronized (mLock) {
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
        if ((mInteractivity & INTERACTIVITY_FLAG_DOUBLE_TAP) == INTERACTIVITY_FLAG_DOUBLE_TAP) {
            if (e.getAction() == MotionEvent.ACTION_UP) {
                /*
                synchronized (mLock) {
                    final float nextZoomPivot = getNextZoomPivot();

                    getImageMatrixValues(mMatrixValues);
                    final float minSx = getImageMinScaleX();
                    final float minSy = getImageMinScaleY();
                    final float sx = minSx + (getImageMaxScaleX() - minSx) * nextZoomPivot;
                    final float sy = minSy + (getImageMaxScaleY() - minSy) * nextZoomPivot;
                    mMatrixValues[MSCALE_X] = sx;
                    mMatrixValues[MSCALE_Y] = sy;
                    mNewImageMatrix.setValues(mMatrixValues);

                    final float x = e.getX();
                    final float y = e.getY();

                    absolutePositionToCenter(mDstPts, mNewImageMatrix, x, y);

                    setImageTransformation(sx, sy, mDstPts[0], mDstPts[1], x, y, true);
                }
                */
            }
        }

        /* TODO VERSION6
        if (e.getAction() == MotionEvent.ACTION_UP) {
            // TODO This if statement might not be necessary
            // TODO Clean this
            // TODO synchronized
            // TODO Switch these 2 if statements
            if ((mInteractivity & INTERACTIVITY_FLAG_DOUBLE_TAP) == INTERACTIVITY_FLAG_DOUBLE_TAP) {
                final float minScaleX = getImageMinScaleX();
                final float maxScaleX = getImageMaxScaleX();
                final float rangeX = maxScaleX - minScaleX;
                final float relativeScaleX = (Math.abs(rangeX) < FLOAT_EPSILON ?
                        0.0f :
                        (getImageScaleX() - minScaleX) / rangeX);
                int targetPivotIndexX = 0;
                for (int index = 0; index < mZoomPivots.length; index++) {
                    if (mZoomPivots[index] - relativeScaleX > ZOOM_PIVOT_EPSILON) {
                        targetPivotIndexX = index;
                        break;
                    }
                }

                final float minScaleY = getImageMinScaleY();
                final float maxScaleY = getImageMaxScaleY();
                final float rangeY = maxScaleY - minScaleY;
                final float relativeScaleY = (Math.abs(rangeY) < FLOAT_EPSILON ?
                        0.0f :
                        (getImageScaleY() - minScaleY) / rangeY);
                int targetPivotIndexY = 0;
                for (int index = 0; index < mZoomPivots.length; index++) {
                    if (mZoomPivots[index] - relativeScaleY > ZOOM_PIVOT_EPSILON) {
                        targetPivotIndexY = index;
                        break;
                    }
                }

                final int targetPivotIndex = (targetPivotIndexX == 0 || targetPivotIndexY == 0 ?
                        0 :
                        Math.max(targetPivotIndexX, targetPivotIndexY));

                final float sx = minScaleX + rangeX * mZoomPivots[targetPivotIndex];
                final float sy = minScaleY + rangeY * mZoomPivots[targetPivotIndex];
                final float cx = absoluteToRelativeX(e.getX());
                final float cy = absoluteToRelativeY(e.getY());

                // TODO BEGIN These are UNadjusted values. Let's try to adjust them
                final float resolvedSx =
                        resolveScaleX(sx, getImageMinScaleX(), getImageMaxScaleX(), false);
                final float resolvedSy =
                        resolveScaleY(sy, getImageMinScaleY(), getImageMaxScaleY(), false);

                mImageMatrix.set(getImageMatrixInternal());
                mImageMatrix.getValues(mMatrixValues);
                mMatrixValues[MSCALE_X] = resolvedSx;
                mMatrixValues[MSCALE_Y] = resolvedSy;
                mImageMatrix.setValues(mMatrixValues);

                // What's the size of the resulting rect at the current scale?
                final int intrinsicWidth = getDrawableIntrinsicWidth();
                final int intrinsicHeight = getDrawableIntrinsicHeight();
                mSrcRect.set(0.0f, 0.0f, intrinsicWidth, intrinsicHeight);
                mImageMatrix.mapRect(mDstRect, mSrcRect);
                final float mappedWidth = mDstRect.width();
                final float mappedHeight = mDstRect.height();

                mPts[0] = intrinsicWidth * cx;
                mPts[1] = intrinsicHeight * cy;
                mImageMatrix.mapPoints(mPts);

                final int availableWidth = getAvailableWidth();
                final int availableHeight = getAvailableHeight();
                final float tx = mMatrixValues[MTRANS_X] + availableWidth * 0.5f - mPts[0];
                final float ty = mMatrixValues[MTRANS_Y] + availableHeight * 0.5f - mPts[1];

                final float resolvedTransX = resolveTransX(
                        tx,
                        getImageMinTransX(availableWidth, mappedWidth),
                        getImageMaxTransX(availableWidth, mappedWidth),
                        false);
                final float resolvedTransY = resolveTransY(
                        ty,
                        getImageMinTransY(availableHeight, mappedHeight),
                        getImageMaxTransY(availableHeight, mappedHeight),
                        false);

                mMatrixValues[MTRANS_X] = resolvedTransX;
                mMatrixValues[MTRANS_Y] = resolvedTransY;
                mImageMatrix.setValues(mMatrixValues);

                // Convert resolvedTransX/resolvedTransY back to a resolved centerX/centerY
                // TODO This logic is just like absoluteToRelativeX/Y. Pull out and consolidate.
                mImageMatrix.invert(mImageMatrix);
                mPts[0] = getWidth() * 0.5f;
                mPts[1] = getHeight() * 0.5f;
                mImageMatrix.mapPoints(mPts);
                final float resolvedCx = mPts[0] / getDrawableIntrinsicWidth();
                final float resolvedCy = mPts[1] / getDrawableIntrinsicHeight();

                // TODO END adjust. The above logic should maybe also appear in setLayout, and setLayout will always expect adjusted values

                if (mScaler == null) {
                    setLayout(resolvedSx, resolvedSy, resolvedCx, resolvedCy);
                } else {
                    mScaler.forceFinished(true);

                    mScaler.startScale(
                            getImageScaleX(),
                            getImageScaleY(),
                            getImageCenterX(),
                            getImageCenterY(),
                            resolvedSx,
                            resolvedSy,
                            resolvedCx,
                            resolvedCy);
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                return true;
            }
        }
        */
        return false;
    }

    @Override // ScaleGestureDetector.OnScaleGestureListener
    public boolean onScale(ScaleGestureDetector detector) {
        final float currentSpan = detector.getCurrentSpan();
        synchronized (mLock) {
            final float spanDelta = (currentSpan / mLastSpan);
            getImageMatrixInternal(mImageMatrix);
            mImageMatrix.getValues(mMatrixValues);
            setImageTransformInternal(
                    mMatrixValues[MSCALE_X] * spanDelta,
                    mMatrixValues[MSCALE_X] * spanDelta,
                    mScaleBeginDrawablePoint.x,
                    mScaleBeginDrawablePoint.y,
                    detector.getFocusX(),
                    detector.getFocusY(),
                    true);
        }
        mLastSpan = currentSpan;
        return true;
    }

    @Override // ScaleGestureDetector.OnScaleGestureListener
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        synchronized (mLock) {
            getImageMatrixInternal(mImageMatrix);
            mSrcPts[0] = detector.getFocusX();
            mSrcPts[1] = detector.getFocusY();
            mapViewPointToDrawablePoint(mDstPts, mSrcPts, mImageMatrix);
            mScaleBeginDrawablePoint.set(mDstPts[0], mDstPts[1]);
            mLastSpan = detector.getCurrentSpan();
        }
        return true;
    }

    @Override // ScaleGestureDetector.OnScaleGestureListener
    public void onScaleEnd(ScaleGestureDetector detector) {
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

    /*
    public float getImageCenterX() {
        synchronized (mLock) {
            getImageMatrixInternal(mImageMatrix);
            matrixToCenter(mDstPts, mImageMatrix);
            return mDstPts[0];
        }
    }

    public float getImageCenterY() {
        synchronized (mLock) {
            getImageMatrixInternal(mImageMatrix);
            matrixToCenter(mDstPts, mImageMatrix);
            return mDstPts[1];
        }
    }
    */

    public float getImagePivotX() {
        synchronized (mLock) {
            getImageMatrixInternal(mImageMatrix);
            mSrcPts[0] = getPaddingLeft() + getAvailableWidth() * 0.5f;
            mSrcPts[1] = 0.0f;
            mapViewPointToDrawablePoint(mDstPts, mSrcPts, mImageMatrix);
            return mDstPts[0];
        }
    }

    public float getImagePivotY() {
        synchronized (mLock) {
            getImageMatrixInternal(mImageMatrix);
            mSrcPts[0] = 0.0f;
            mSrcPts[1] = getPaddingTop() + getAvailableHeight() * 0.5f;
            mapViewPointToDrawablePoint(mDstPts, mSrcPts, mImageMatrix);
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
            getImageMatrixInternal(mImageMatrix);
            mImageMatrix.getValues(mMatrixValues);
            return mMatrixValues[MSCALE_X];
        }
    }

    public float getImageScaleY() {
        synchronized (mLock) {
            getImageMatrixInternal(mImageMatrix);
            mImageMatrix.getValues(mMatrixValues);
            return mMatrixValues[MSCALE_Y];
        }
    }

    public int getInteractivity() {
        return mInteractivity;
    }

    public boolean onUp(MotionEvent e) {
        return false;
    }

    public void setInteractivity(int flags) {
        mInteractivity = flags;
    }

    public boolean setImageTransform(float sx, float sy, float px, float py) {
        return setImageTransformRelative(sx, sy, px, py, CENTER, CENTER, false);
    }

    public boolean setImageTransform(float sx, float sy, float px, float py, boolean animate) {
        return setImageTransformRelative(sx, sy, px, py, CENTER, CENTER, animate);
    }

    public boolean setImageTransform(float sx, float sy, float px, float py, float x, float y) {
        return setImageTransform(sx, sy, px, py, x, y, false);
    }

    // So the thing is, x and y could be different before/after rotation.
    public boolean setImageTransform(
            float sx,
            float sy,
            float px,
            float py,
            float x,
            float y,
            boolean animate) {
        if (!ViewCompat.isLaidOut(this)) {
            mPendingTransformation = createPendingBundle(
                    false, sx, sy, px, py, x, y, animate);
            return false;
        }
        return setImageTransformInternal(sx, sy, px, py, x, y, false);
    }

    public boolean setImageTransformRelative(
            float sx,
            float sy,
            float px,
            float py,
            float rx,
            float ry) {
        return setImageTransformRelative(sx, sy, px, py, rx, ry, false);
    }

    public boolean setImageTransformRelative(
            float sx,
            float sy,
            float px,
            float py,
            float rx,
            float ry,
            boolean animate) {
        if (!ViewCompat.isLaidOut(this)) {
            mPendingTransformation = createPendingBundle(
                    true, sx, sy, px, py, rx, ry, animate);
            return false;
        }
        final float x = getPaddingLeft() + getAvailableWidth() * rx;
        final float y = getPaddingTop() + getAvailableHeight() * ry;
        return setImageTransform(sx, sy, px, py, x, y, animate);
    }

    /*
    public boolean setImageTransformation(float sx, float sy, float cx, float cy) {
        return setImageTransformation(sx, sy, cx, cy, false);
    }

    public boolean setImageTransformation(float sx, float sy, float cx, float cy, boolean animate) {
        if (!ViewCompat.isLaidOut(this)) {
            mPendingTransformation = new Pair<>(new PointF(sx, sy), new PointF(cx, cy));
            return false;
        }

        return setImageTransformation(sx, sy, cx, cy, animate, false);
    }
    */

    public void setZoomPivots(float... pivots) {
        mZoomPivots = pivots;
    }
    //endregion Methods

    //region Protected methods
    protected boolean drawableHasIntrinsicSize() {
        return getDrawableIntrinsicWidth() > 0 && getDrawableIntrinsicHeight() > 0;
    }

    protected int getAvailableHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    protected int getAvailableWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    @SuppressWarnings("SameParameterValue")
    protected void getBaselineImageMatrix(Matrix outMatrix) {
        getBaselineImageMatrix(mScaleType, outMatrix);
    }

    protected void getBaselineImageMatrix(ScaleType scaleType, Matrix outMatrix) {
        synchronized (mLock) {
            if ((mInvalidFlags & INVALID_FLAG_BASELINE_IMAGE_MATRIX) ==
                    INVALID_FLAG_BASELINE_IMAGE_MATRIX) {
                mInvalidFlags &= ~INVALID_FLAG_BASELINE_IMAGE_MATRIX;
                if (drawableHasIntrinsicSize()) {
                    // We need to do the scaling ourselves.
                    final int dwidth = getDrawableIntrinsicWidth();
                    final int dheight = getDrawableIntrinsicHeight();

                    final int vwidth = getAvailableWidth();
                    final int vheight = getAvailableHeight();

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

    /*
    protected float getContentCenterX() {
        return getPaddingLeft() + getAvailableWidth() * 0.5f;
    }

    protected float getContentCenterY() {
        return getPaddingTop() + getAvailableHeight() * 0.5f;
    }
    */

    protected void getImageMatrixInternal(Matrix outMatrix) {
        if (ScaleType.FIT_XY == super.getScaleType()) {
            getBaselineImageMatrix(ScaleType.FIT_XY, outMatrix);
        } else if (outMatrix != null) {
            outMatrix.set(getImageMatrix());
        }
    }

    // TODO JavaDoc needs to state that method must call setImageMaxScale if overridden
    protected void getImageMaxScale() {
        synchronized (mLock) {
            if ((mInvalidFlags & INVALID_FLAG_IMAGE_MAX_SCALE) == INVALID_FLAG_IMAGE_MAX_SCALE) {
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
                    final int availableWidth = getAvailableWidth();
                    final int availableHeight = getAvailableHeight();
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
                getAvailableWidth(),
                scaledImageWidth,
                ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL,
                true);
    }

    protected float getImageMaxTransY(float scaledImageHeight) {
        return getImageTransBounds(
                getAvailableHeight(),
                scaledImageHeight,
                false,
                true);
    }

    protected float getImageMinTransX(float scaledImageWidth) {
        return getImageTransBounds(
                getAvailableWidth(),
                scaledImageWidth,
                ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL,
                false);
    }

    protected float getImageMinTransY(float scaledImageHeight) {
        return getImageTransBounds(
                getAvailableHeight(),
                scaledImageHeight,
                false,
                false);
    }

    // TODO JavaDoc needs to state that method must call setImageMinScale if overridden
    protected void getImageMinScale() {
        synchronized (mLock) {
            if ((mInvalidFlags & INVALID_FLAG_IMAGE_MIN_SCALE) == INVALID_FLAG_IMAGE_MIN_SCALE) {
                getBaselineImageMatrix(null);
                mBaselineImageMatrix.getValues(mMatrixValues);
                setImageMinScale(mMatrixValues[MSCALE_X], mMatrixValues[MSCALE_Y]);
            }
        }
    }

    protected void mapViewPointToDrawablePoint(float[] dst, float[] src, Matrix imageMatrix) {
        synchronized (mLock) {
            imageMatrix.invert(mNewImageMatrix);
            mNewImageMatrix.mapPoints(dst, src);
        }
    }

    @SuppressWarnings("unused")
    protected float resolveScaleX(float sx, float min, float max, boolean fromUser) {
        return MathUtils.clamp(sx, min, max);
    }

    @SuppressWarnings("unused")
    protected float resolveScaleY(float sy, float min, float max, boolean fromUser) {
        return MathUtils.clamp(sy, min, max);
    }

    @SuppressWarnings("unused")
    protected float resolveTransX(float tx, float min, float max, boolean fromUser) {
        return MathUtils.clamp(tx, min, max);
    }

    @SuppressWarnings("unused")
    protected float resolveTransY(float ty, float min, float max, boolean fromUser) {
        return MathUtils.clamp(ty, min, max);
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

    protected boolean setImageTransformInternal(
            float sx,
            float sy,
            float px,
            float py,
            float x,
            float y,
            boolean fromUser) {
        synchronized (mLock) {
            // First, I need to resolve scale and map px and py to a scaled location
            getImageMatrixInternal(mImageMatrix);
            mNewImageMatrix.set(mImageMatrix);
            mNewImageMatrix.getValues(mMatrixValues);
            mMatrixValues[MSCALE_X] =
                    resolveScaleX(sx, getImageMinScaleX(), getImageMaxScaleX(), fromUser);
            mMatrixValues[MSCALE_Y] =
                    resolveScaleY(sy, getImageMinScaleY(), getImageMaxScaleY(), fromUser);
            mMatrixValues[MTRANS_X] = 0;
            mMatrixValues[MTRANS_Y] = 0;
            mNewImageMatrix.setValues(mMatrixValues);
            mSrcPts[0] = px;
            mSrcPts[1] = py;
            mNewImageMatrix.mapPoints(mDstPts, mSrcPts); // TODO Make a mapDrawablePointToViewPoint method?
            final float tx = x - getPaddingLeft() - mDstPts[0];
            final float ty = y - getPaddingTop() - mDstPts[1];

            getDrawableIntrinsicRect(mSrcRect);
            mNewImageMatrix.mapRect(mDstRect, mSrcRect);
            final float mappedWidth = mDstRect.width();
            final float mappedHeight = mDstRect.height();
            mMatrixValues[MTRANS_X] = resolveTransX(
                    tx,
                    getImageMinTransX(mappedWidth),
                    getImageMaxTransX(mappedWidth),
                    fromUser);
            mMatrixValues[MTRANS_Y] = resolveTransY(
                    ty,
                    getImageMinTransY(mappedHeight),
                    getImageMaxTransY(mappedHeight),
                    fromUser);
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
    }

    /*
    protected boolean setImageTransformation(
            float sx,
            float sy,
            float cx,
            float cy,
            boolean animate,
            boolean fromUser) {
        // Convert this into a matrix. Resolve both scale and translation
        synchronized (mLock) {
            getImageMatrixInternal(mNewImageMatrix);
            mNewImageMatrix.getValues(mMatrixValues);
            final float rSx = resolveScaleX(sx, getImageMinScaleX(), getImageMaxScaleX(), fromUser);
            final float rSy = resolveScaleY(sy, getImageMinScaleY(), getImageMaxScaleY(), fromUser);
            mNewImageMatrix.setValues(mMatrixValues);

            // Convert a cx/cy to actual position?
            centerToAbsolutePosition(mDstPts, cx, cy);
            return false;
        }
    }
    */

    /*
    @SuppressWarnings("SameParameterValue")
    protected boolean setScaleAndCenter(
            float sx,
            float sy,
            float cx,
            float cy,
            boolean animate,
            boolean fromUser) {
        synchronized (mLock) {
            // I will need to check these values so they are resolved at the SAME RATE and not
            // independently? Or do that somehow in onScale?
            getImageMatrixValues(mMatrixValues);
            mMatrixValues[MSCALE_X] =
                    resolveScaleX(sx, getImageMinScaleX(), getImageMaxScaleX(), fromUser);
            mMatrixValues[MSCALE_Y] =
                    resolveScaleY(sy, getImageMinScaleY(), getImageMaxScaleY(), fromUser);
            mNewImageMatrix.setValues(mMatrixValues);

            // Translate the matrix based on center x/y
            centerToMatrix(mNewImageMatrix, cx, cy);
            return setScaleAndTranslate(mNewImageMatrix, animate, fromUser);
        }
    }
    */

    /* This version has resolved scale in imageMatrix, but unresolved translation */
    /*
    protected boolean setScaleAndTranslate(
            @NonNull final Matrix imageMatrix,
            boolean animate,
            boolean fromUser) {
        synchronized (mLock) {
            // Get mapped rect
            getDrawableIntrinsicRect(mSrcRect);
            imageMatrix.mapRect(mDstRect, mSrcRect);

            // Resolve trans x/y values
            final float mappedWidth = mDstRect.width();
            final float mappedHeight = mDstRect.height();
            imageMatrix.getValues(mMatrixValues);
            mMatrixValues[MTRANS_X] = resolveTransX(
                    mMatrixValues[MTRANS_X],
                    getImageMinTransX(mappedWidth),
                    getImageMaxTransX(mappedWidth),
                    fromUser);
            mMatrixValues[MTRANS_Y] = resolveTransY(
                    mMatrixValues[MTRANS_Y],
                    getImageMinTransY(mappedHeight),
                    getImageMaxTransY(mappedHeight),
                    fromUser);

            imageMatrix.setValues(mMatrixValues);
            return setScaleAndTranslate(imageMatrix, animate);
        }
    }
    */
    //endregion Protected methods

    //region Private methods
    /*
    private void absolutePositionToCenter(
            @NonNull final float[] dst,
            @NonNull final Matrix imageMatrix,
            float px,
            float py) {
        synchronized (mLock) {
            imageMatrix.invert(mImageMatrix);
            mSrcPts[0] = px;
            mSrcPts[1] = py;
            mImageMatrix.mapPoints(mDstPts, mSrcPts);
            dst[0] = mDstPts[0] / getDrawableIntrinsicWidth();
            dst[1] = mDstPts[1] / getDrawableIntrinsicHeight();
        }
    }
    */

    /*
    private void centerToAbsolutePosition(@NonNull final float[] dst, float cx, float cy) {
        synchronized (mLock) {
            getImageMatrixInternal(mImageMatrix);
            mSrcPts[0] = getDrawableIntrinsicWidth() * cx;
            mSrcPts[1] = getDrawableIntrinsicHeight() * cy;
            mImageMatrix.mapPoints(dst, mSrcPts);
            dst[0] += getPaddingLeft(); // TOD ??
            dst[1] += getPaddingTop(); // TOD ??
        }
    }
    */

    /*
    private void centerToMatrix(
            @NonNull final Matrix dstMatrix,
            final float cx,
            final float cy) {
        centerToMatrix(dstMatrix, cx, cy, getContentCenterX(), getContentCenterY());
    }
    */

    /* Populates dstMatrix so that the relative position cx, cy in the image appears
     * at px, py (a point in the view */
    /*
    private void centerToMatrix(
            @NonNull final Matrix dstMatrix,
            final float cx,
            final float cy,
            final float px,
            final float py) {
        synchronized (mLock) {
            centerToAbsolutePosition(mDstPts, cx, cy);
            dstMatrix.postTranslate(
                    px - getPaddingLeft() - mDstPts[0], // ?Padding?
                    py - getPaddingTop() - mDstPts[1]);
        }
    }
    */

    private Bundle createPendingBundle(
            boolean relative,
            float sx,
            float sy,
            float px,
            float py,
            float x,
            float y,
            boolean animate) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_RELATIVE, relative);
        bundle.putFloat(KEY_SX, sx);
        bundle.putFloat(KEY_SY, sy);
        bundle.putFloat(KEY_PX, px);
        bundle.putFloat(KEY_PY, py);
        bundle.putFloat(KEY_X, x);
        bundle.putFloat(KEY_Y, y);
        bundle.putBoolean(KEY_ANIMATE, animate);
        return bundle;
    }

    /*
    private void drawablePointToViewPoint(
            PointF dst,
            @NonNull final PointF drawablePoint,
            @NonNull final Matrix imageMatrix) {
        // TODO Move code from setImageTransformInternal here
    }
    */

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

    /*
    private void viewPointToDrawablePoint(
            float[] dst,
            float[] viewPoint,
            @NonNull final Matrix imageMatrix) {
        if (dst != null) {
            // TODO Calc for real

            imageMatrix.invert(imageMatrix);
            mImageMatrix.mapPoints(dst, viewPoint);
            //dst[0] /= mDstPts[0] / getDrawableIntrinsicWidth();
            //dst[1] = mDstPts[1] / getDrawableIntrinsicHeight();



            //dst.x = getDrawableIntrinsicWidth() * 0.5f;
            //dst.y = getDrawableIntrinsicHeight() * 0.5f;
        }
    }
    */

    /*
    private void matrixToCenter(
            @NonNull final float[] dstPts,
            @NonNull final Matrix imageMatrix) {
        matrixToCenter(dstPts, imageMatrix, getContentCenterX(), getContentCenterY());
    }
    */

    /* Fills dstPts with the relative position in the image located at the passed pivot point.
     * px, py is an actual point in the view */
    /*
    private void matrixToCenter(
            @NonNull final float[] dstPts,
            @NonNull final Matrix imageMatrix,
            float px,
            float py) {
        synchronized (mLock) {
            getDrawableIntrinsicRect(mSrcRect);
            imageMatrix.mapRect(mDstRect, mSrcRect);
            dstPts[0] = (px - getPaddingLeft() - mDstRect.left) / mDstRect.width();
            dstPts[1] = (py - getPaddingTop() - mDstRect.top) / mDstRect.height();
        }
    }
    */

    /*
    private void setImageMatrixValues(float[] values) {
        synchronized (mLock) {
            mImageMatrix.setValues(values);
            super.setImageMatrix(mImageMatrix);
        }
    }
    */

    /*
    private boolean setImageTransformation(
            float sx,
            float sy,
            float px,
            float py,
            float x,
            float y,
            boolean animate) {
        return false;
    }

    private boolean setImageTransformation(@NonNull final Matrix imageMatrix) {
        synchronized (mLock) {
            getImageMatrixInternal(mImageMatrix);
            if (mImageMatrix.equals(imageMatrix)) {
                return false;
            }

            if (super.getScaleType() != ScaleType.MATRIX) {
                super.setScaleType(ScaleType.MATRIX);
            }

            super.setImageMatrix(imageMatrix);
            return true;
        }
    }
    */
    //endregion Private methods
}
