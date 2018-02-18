package com.codepunk.demo.interactiveimageview.version6;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
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
import android.util.Pair;
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
         * The current scale X value; computed by {@link #computeScroll()}.
         */
        private float mCurrentSx;

        /**
         * The current scale Y value; computed by {@link #computeScroll()}.
         */
        private float mCurrentSy;

        /**
         * The current translation X value; computed by {@link #computeScroll()}.
         */
        private float mCurrentTx;

        /**
         * The current translation Y value; computed by {@link #computeScroll()}.
         */
        private float mCurrentTy;

        /**
         * The time the zoom started, computed using {@link SystemClock#elapsedRealtime()}.
         */
        private long mStartRTC;

        private float mStartSx;

        private float mStartSy;

        private float mStartTx;

        private float mStartTy;

        /**
         * The destination scale X.
         */
        private float mEndSx;

        /**
         * The destination scale Y.
         */
        private float mEndSy;

        /**
         * The destination translation X.
         */
        private float mEndTx;

        /**
         * The destination translation Y.
         */
        private float mEndTy;

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
        public void abortAnimation() {
            mFinished = true;
            mCurrentSx = mEndSx;
            mCurrentSy = mEndSy;
            mCurrentTx = mEndTx;
            mCurrentTy = mEndTy;
        }

        /**
         * Forces the zoom finished state to the given value. Unlike {@link #abortAnimation()}, the
         * current zoom value isn't set to the ending value.
         *
         * @see android.widget.Scroller#forceFinished(boolean)
         */
        public void forceFinished(boolean finished) {
            mFinished = finished;
        }

        /**
         * Starts a scroll from the supplied start values to the supplied end values.
         *
         * @see android.widget.Scroller#startScroll(int, int, int, int)
         */
        public void startScaleAndScroll(
                float startSx, 
                float startSy, 
                float startTx, 
                float startTy, 
                float endSx, 
                float endSy, 
                float endTx, 
                float endTy) {
            mStartRTC = SystemClock.elapsedRealtime();
            mCurrentSx = mStartSx = startSx;
            mCurrentSy = mStartSy = startSy;
            mCurrentTx = mStartTx = startTx;
            mCurrentTy = mStartTy = startTy;
            mEndSx = endSx;
            mEndSy = endSy;
            mEndTx = endTx;
            mEndTy = endTy;
            mFinished = false;
        }

        /**
         * Computes the current scroll, returning true if the zoom is still active and false if the
         * scroll has finished.
         *
         * @see android.widget.Scroller#computeScrollOffset()
         */
        public boolean computeScaleAndScroll() {
            if (mFinished) {
                return false;
            }

            long tRTC = SystemClock.elapsedRealtime() - mStartRTC;

            if (tRTC >= mAnimationDurationMillis) {
                mFinished = true;
                mCurrentSx = mEndSx;
                mCurrentSy = mEndSy;
                mCurrentTx = mEndTx;
                mCurrentTy = mEndTy;
                return false;
            }

            float t = tRTC * 1f / mAnimationDurationMillis;
            float interpolation = mInterpolator.getInterpolation(t);
            mCurrentSx = mStartSx + (mEndSx - mStartSx) * interpolation;
            mCurrentSy = mStartSy + (mEndSy - mStartSy) * interpolation;
            mCurrentTx = mStartTx + (mEndTx - mStartTx) * interpolation;
            mCurrentTy = mStartTy + (mEndTy - mStartTy) * interpolation;
            return true;
        }


        /**
         * Returns the current scale X.
         *
         * @see android.widget.Scroller#getCurrX()
         */
        public float getCurrScaleX() {
            return mCurrentSx;
        }

        /**
         * Returns the current scale Y.
         *
         * @see android.widget.Scroller#getCurrX()
         */
        public float getCurrScaleY() {
            return mCurrentSy;
        }

        /**
         * Returns the current translation X.
         *
         * @see android.widget.Scroller#getCurrX()
         */
        public float getCurrTransX() {
            return mCurrentTx;
        }

        /**
         * Returns the current translation Y.
         *
         * @see android.widget.Scroller#getCurrX()
         */
        public float getCurrTransY() {
            return mCurrentTy;
        }
    }
    //endregion Nested classes

    //region Constants
    private static final String LOG_TAG = InteractiveImageView.class.getSimpleName();
    static final float MAX_SCALE_BREADTH_MULTIPLIER = 4.0f;
    static final float MAX_SCALE_LENGTH_MULTIPLIER = 6.0f;
    final float FLOAT_EPSILON = 0.005f;
    final float ZOOM_PIVOT_EPSILON = 0.2f;

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

    private float mLastSpan;

    private final float[] mMatrixValues = new float[9];
    private final float[] mSrcPts = new float[2];
    private final float[] mDstPts = new float[2];
    private final Matrix mBaselineImageMatrix = new Matrix();
    private final Matrix mImageMatrixInternal = new Matrix();
    private final Matrix mImageMatrix = new Matrix();
    private final RectF mSrcRect = new RectF();
    private final RectF mDstRect = new RectF();

    private final Object mLock = new Object();

    private Pair<PointF, PointF> mPendingLayout = null;
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

        boolean needsInvalidate = false;

        if (mScaleScroller.computeScaleAndScroll()) {
            final float sx = mScaleScroller.getCurrScaleX();
            final float sy = mScaleScroller.getCurrScaleY();
            final float tx = mScaleScroller.getCurrTransX();
            final float ty = mScaleScroller.getCurrTransY();
            setLayoutInternal(sx, sy, tx, ty, false);
            needsInvalidate = true;
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }

        /* TODO VERSION6
        // TODO synchronized
        // TODO This has a lot of similar logic as setLayoutInternal. Consolidate?
        boolean needsInvalidate = false;

        if (mOverScroller != null) {
            if (mOverScroller.computeScrollOffset()) {
                mImageMatrix.set(getImageMatrixInternal());
                mImageMatrix.getValues(mMatrixValues);

                // The scroller isn't finished, meaning a fling or programmatic
                // pan operation is currently active.
                mSrcRect.set(
                        0.0f,
                        0.0f,
                        getDrawableIntrinsicWidth(),
                        getDrawableIntrinsicHeight());
                mImageMatrix.mapRect(mDstRect, mSrcRect);
                final float mappedWidth = mDstRect.width();
                final float mappedHeight = mDstRect.height();

                final int availableWidth = getAvailableWidth();
                final int availableHeight = getAvailableHeight();
                final int tx = mOverScroller.getCurrX();
                final int ty = mOverScroller.getCurrY();

                final float resolvedTransX = resolveTransX(
                        tx,
                        getImageMinTransX(availableWidth, mappedWidth),
                        getImageMaxTransX(availableWidth, mappedWidth),
                        true);
                final float resolvedTransY = resolveTransY(
                        ty,
                        getImageMinTransY(availableHeight, mappedHeight),
                        getImageMaxTransY(availableHeight, mappedHeight),
                        true);

                if (mMatrixValues[MTRANS_X] == resolvedTransX &&
                        mMatrixValues[MTRANS_Y] == resolvedTransY) {
                    // No change; fling is finished
                    mOverScroller.forceFinished(true);
                    return; // TODO Don't want to return though!
                }

                mMatrixValues[MTRANS_X] = resolvedTransX;
                mMatrixValues[MTRANS_Y] = resolvedTransY;
                mImageMatrix.setValues(mMatrixValues);

                if (ScaleType.MATRIX != super.getScaleType()) {
                    super.setScaleType(ScaleType.MATRIX);
                }
                super.setImageMatrix(mImageMatrix);
                needsInvalidate = true;
            }
        }



        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
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
        if (mPendingLayout != null) {
            setLayout(
                    mPendingLayout.first.x,
                    mPendingLayout.first.y,
                    mPendingLayout.second.x,
                    mPendingLayout.second.y);
            mPendingLayout = null;
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
        /* TODO VERSION6
        // TODO synchronized
        // TODO This has a lot of similar logic as setLayoutInternal. Consolidate?
        mImageMatrix.set(getImageMatrixInternal());
        mImageMatrix.getValues(mMatrixValues);

        // What's the size of the resulting rect at the current scale?
        mSrcRect.set(
                0.0f,
                0.0f,
                getDrawableIntrinsicWidth(),
                getDrawableIntrinsicHeight());
        mImageMatrix.mapRect(mDstRect, mSrcRect);
        final float mappedWidth = mDstRect.width();
        final float mappedHeight = mDstRect.height();

        final int availableWidth = getAvailableWidth();
        final int availableHeight = getAvailableHeight();
        float tx = mMatrixValues[MTRANS_X] - distanceX;
        float ty = mMatrixValues[MTRANS_Y] - distanceY;

        final float resolvedTransX = resolveTransX(
                tx,
                getImageMinTransX(availableWidth, mappedWidth),
                getImageMaxTransX(availableWidth, mappedWidth),
                true);
        final float resolvedTransY = resolveTransY(
                ty,
                getImageMinTransY(availableHeight, mappedHeight),
                getImageMaxTransY(availableHeight, mappedHeight),
                true);

        mMatrixValues[MTRANS_X] = resolvedTransX;
        mMatrixValues[MTRANS_Y] = resolvedTransY;
        mImageMatrix.setValues(mMatrixValues);

        if (ScaleType.MATRIX != super.getScaleType()) {
            super.setScaleType(ScaleType.MATRIX);
        }

        super.setImageMatrix(mImageMatrix);
        return true;
        */
        return false;
    }

    @Override // GestureDetector.OnGestureListener
    public void onLongPress(MotionEvent e) {
    }

    @Override // GestureDetector.OnGestureListener
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        /* TODO VERSION6
        // TODO synchronized
        // TODO This has a lot of similar logic as setLayoutInternal. Consolidate?
        if (mOverScroller != null) {
            mOverScroller.forceFinished(true);

            // What's the size of the resulting rect at the current scale?
            mSrcRect.set(
                    0.0f,
                    0.0f,
                    getDrawableIntrinsicWidth(),
                    getDrawableIntrinsicHeight());
            mImageMatrix.mapRect(mDstRect, mSrcRect);
            final float mappedWidth = mDstRect.width();
            final float mappedHeight = mDstRect.height();

            final int availableWidth = getAvailableWidth();
            final int availableHeight = getAvailableHeight();
            getImageMatrixInternal().getValues(mMatrixValues);
            mOverScroller.fling(
                    (int) mMatrixValues[MTRANS_X],
                    (int) mMatrixValues[MTRANS_Y],
                    (int) velocityX,
                    (int) velocityY,
                    (int) getImageMinTransX(availableWidth, mappedWidth),
                    (int) getImageMaxTransX(availableWidth, mappedWidth),
                    (int) getImageMinTransY(availableHeight, mappedHeight),
                    (int) getImageMaxTransY(availableHeight, mappedHeight),
                    (int) (availableWidth * 0.5f),
                    (int) (availableHeight * 0.5f));
        }
        return false; // TRUE???
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

                // TODO END adjust. The above logic should maybe also appear in setLayout, and setLayoutInternal will always expect adjusted values

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
        /* TODO VERSION6
        // TODO synchronized
        final float currentSpan = detector.getCurrentSpan();
        final float spanDelta = (currentSpan / mLastSpan);
        getImageMatrixInternal().getValues(mMatrixValues);
        setLayoutInternal(
                mMatrixValues[MSCALE_X] *= spanDelta,
                mMatrixValues[MSCALE_Y] *= spanDelta,
                getImageCenterX(),
                getImageCenterY(),
                true);
        mLastSpan = currentSpan;
        return true;
        */
        return false;
    }

    @Override // ScaleGestureDetector.OnScaleGestureListener
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mLastSpan = detector.getCurrentSpan();
        return true;
    }

    @Override // ScaleGestureDetector.OnScaleGestureListener
    public void onScaleEnd(ScaleGestureDetector detector) {
    }
    //endregion Interface methods

    //region Methods
    /* TODO VERSION6
    public float absoluteToRelativeX(float x) {
        synchronized (mLock) {
            getImageMatrixInternal().invert(mImageMatrix);
            mPts[0] = x;
            mPts[1] = 0.0f;
            mImageMatrix.mapPoints(mPts);
            return mPts[0] / getDrawableIntrinsicWidth();
        }
    }

    public float absoluteToRelativeY(float y) {
        synchronized (mLock) {
            getImageMatrixInternal().invert(mImageMatrix);
            mPts[0] = 0.0f;
            mPts[1] = y;
            mImageMatrix.mapPoints(mPts);
            return mPts[1] / getDrawableIntrinsicHeight();
        }
    }
    */

    protected int getDrawableIntrinsicHeight() {
        final Drawable dr = getDrawable();
        return (dr == null ? -1 : dr.getIntrinsicHeight());
    }

    protected int getDrawableIntrinsicWidth() {
        final Drawable dr = getDrawable();
        return (dr == null ? -1 : dr.getIntrinsicWidth());
    }

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
            getImageMatrixValues(mMatrixValues);
            return mMatrixValues[MSCALE_X];
        }
    }

    public float getImageScaleY() {
        synchronized (mLock) {
            getImageMatrixValues(mMatrixValues);
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

        /* Can delete all of this
        final boolean scaleEnableddd =
                (mInteractivity & INTERACTIVITY_FLAG_SCALE) == INTERACTIVITY_FLAG_SCALE;
        if (scaleEnabled) {
            if (mScaleGestureDetector == null) {
                mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);
            }
        } else {
            mScaleGestureDetector = null;
        }

        final boolean scrollEnableddd =
                (mInteractivity & INTERACTIVITY_FLAG_SCROLL) == INTERACTIVITY_FLAG_SCROLL ||
                        (mInteractivity & INTERACTIVITY_FLAG_FLING) == INTERACTIVITY_FLAG_FLING;
        if (scrollEnabled) {
            if (mOverScroller == null) {
                mOverScroller = new OverScroller(getContext());
            }
        } else {
            mOverScroller = null;
        }

        final boolean doubleTapEnabled =
                (mInteractivity & INTERACTIVITY_FLAG_DOUBLE_TAP) == INTERACTIVITY_FLAG_DOUBLE_TAP;
        if (scrollEnabled || doubleTapEnabled) {
            if (mGestureDetector == null) {
                mGestureDetector = new GestureDetectorCompat(getContext(), this);
            }
            mGestureDetector.setIsLongpressEnabled(false); // TODO Yes? No?
            mGestureDetector.setOnDoubleTapListener(doubleTapEnabled ? this : null);
        } else {
            mGestureDetector = null;
        }
        if (doubleTapEnabled) {
            if (mScaler == null) {
                mScaler = new Scaler(getContext());
            }
        } else {
            mScaler = null;
        }
        */
    }

    public void setLayout(float sx, float sy, float cx, float cy) {
        setLayout(sx, sy, cx, cy, false);
    }

    public void setLayout(float sx, float sy, float cx, float cy, boolean animate) {
        if (!ViewCompat.isLaidOut(this)) {
            mPendingLayout = new Pair<>(new PointF(sx, sy), new PointF(cx, cy));
            return;
        }

        setLayout(sx, sy, cx, cy, animate, false);
    }

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

    protected float getContentCenterX() {
        return getPaddingLeft() + getAvailableWidth() * 0.5f;
    }

    protected float getContentCenterY() {
        return getPaddingTop() + getAvailableHeight() * 0.5f;
    }

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

    @SuppressWarnings("SameParameterValue")
    protected void setLayout(
            float sx,
            float sy,
            float cx,
            float cy,
            boolean animate,
            boolean fromUser) {
        // Resolve scale x/y and store the values in mImageMatrix
        final float rSx = resolveScaleX(sx, getImageMinScaleX(), getImageMaxScaleX(), fromUser);
        final float rSy = resolveScaleY(sy, getImageMinScaleY(), getImageMaxScaleY(), fromUser);
        getImageMatrixValues(mMatrixValues);
        mMatrixValues[MSCALE_X] = rSx;
        mMatrixValues[MSCALE_Y] = rSy;
        mImageMatrix.setValues(mMatrixValues);

        // Translate the matrix based on center x/y
        centerToMatrix(mImageMatrix, cx, cy);

        // Get mapped rect
        getDrawableIntrinsicRect(mSrcRect);
        mImageMatrix.mapRect(mDstRect, mSrcRect);

        // Resolve mapped trans x/y values
        final float mappedWidth = mDstRect.width();
        final float mappedHeight = mDstRect.height();
        mImageMatrix.getValues(mMatrixValues);
        final float rTx = resolveTransX(
                mMatrixValues[MTRANS_X],
                getImageMinTransX(mappedWidth),
                getImageMaxTransX(mappedWidth),
                fromUser);
        final float rTy = resolveTransY(
                mMatrixValues[MTRANS_Y],
                getImageMinTransY(mappedHeight),
                getImageMaxTransY(mappedHeight),
                fromUser);

        setLayoutInternal(rSx, rSy, rTx, rTy, animate);
    }
    //endregion Protected methods

    //region Private methods
    private void centerToMatrix(
            @NonNull final Matrix dstMatrix,
            final float cx,
            final float cy) {
        centerToMatrix(dstMatrix, cx, cy, getContentCenterX(), getContentCenterY());
    }

    /* Populates dstMatrix so that the relative position cx, cy in the image appears
     * at px, py (a point in the view */
    private void centerToMatrix(
            @NonNull final Matrix dstMatrix,
            final float cx,
            final float cy,
            final float px,
            final float py) {
        synchronized (mLock) {
            mSrcPts[0] = getDrawableIntrinsicWidth() * cx;
            mSrcPts[1] = getDrawableIntrinsicHeight() * cy;
            dstMatrix.mapPoints(mDstPts, mSrcPts);
            dstMatrix.postTranslate(
                    px - getPaddingLeft() - mDstPts[0],
                    py - getPaddingTop() - mDstPts[1]);
        }
    }

    private void getDrawableIntrinsicRect(@NonNull final RectF outRect) {
        outRect.set(
                0.0f,
                0.0f,
                getDrawableIntrinsicWidth(),
                getDrawableIntrinsicHeight());
    }

    private void getImageMatrixValues(float[] outValues) {
        synchronized (mLock) {
            getImageMatrixInternal(mImageMatrix);
            if (outValues != null) {
                mImageMatrix.getValues(outValues);
            }
        }
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

    private void matrixToCenter(
            @NonNull final float[] dstPts,
            @NonNull final Matrix imageMatrix) {
        matrixToCenter(dstPts, imageMatrix, getContentCenterX(), getContentCenterY());
    }

    /* Fills dstPts with the relative position in the image located at the passed pivot point.
     * px, py is an actual point in the view */
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

    private void setImageMatrixValues(float[] values) {
        synchronized (mLock) {
            mImageMatrix.setValues(values);
            super.setImageMatrix(mImageMatrix);
        }
    }

    private void setLayoutInternal(float sx, float sy, float tx, float ty, boolean animate) {
        synchronized (mLock) {
            getImageMatrixValues(mMatrixValues);
            if (animate) {
                mScaleScroller.forceFinished(true);
                mScaleScroller.startScaleAndScroll(
                        mMatrixValues[MSCALE_X],
                        mMatrixValues[MSCALE_Y],
                        mMatrixValues[MTRANS_X],
                        mMatrixValues[MTRANS_Y],
                        sx,
                        sy,
                        tx,
                        ty);
                ViewCompat.postInvalidateOnAnimation(this);
            } else {
                mMatrixValues[MSCALE_X] = sx;
                mMatrixValues[MSCALE_Y] = sy;
                mMatrixValues[MTRANS_X] = tx;
                mMatrixValues[MTRANS_Y] = ty;
                if (ScaleType.MATRIX != super.getScaleType()) {
                    super.setScaleType(ScaleType.MATRIX);
                }
                setImageMatrixValues(mMatrixValues);
            }
        }
    }
    //endregion Private methods
}
