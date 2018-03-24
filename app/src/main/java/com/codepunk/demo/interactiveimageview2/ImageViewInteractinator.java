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
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.ArrayRes;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;
import android.widget.OverScroller;

import com.codepunk.demo.OverScrollerCompat;
import com.codepunk.demo.R;
import com.codepunk.demo.support.DisplayCompat;

import java.util.Locale;

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
            boolean needsInvalidate = mTempTransform.reset()
                    .pivot(mTouchPivotPoint.x, mTouchPivotPoint.y)
                    .moveTo(x, y)
                    .touchEvent(true)
                    .transform();

            if (mTempTransform.mClamped) {
                if (mEdgeGlow != null) {
                    final int overScrollMode = getOverScrollMode();
                    final boolean canOverScrollX = (overScrollMode == OVER_SCROLL_ALWAYS ||
                            (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollX()));
                    if (canOverScrollX) {
                        final int compare = Float.compare(x, mTempTransform.mX);
                        EdgeEffect edgeGlow = null;
                        float deltaDistance = (mTempTransform.mX - x) / getWidth();
                        float displacement = y / getHeight();
                        switch (compare) {
                            case 1:
                                edgeGlow = mEdgeGlow.get(Gravity.LEFT);
                                displacement = 1.0f - displacement;
                                break;
                            case -1:
                                edgeGlow = mEdgeGlow.get(Gravity.RIGHT);
                                break;
                        }
                        if (edgeGlow != null) {
                            EdgeEffectCompat.onPull(edgeGlow, deltaDistance, displacement);
                            needsInvalidate = true;
                        }
                    }

                    final boolean canOverScrollY = (overScrollMode == OVER_SCROLL_ALWAYS ||
                            (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollY()));
                    if (canOverScrollY) {
                        final int compare = Float.compare(y, mTempTransform.mY);
                        EdgeEffect edgeGlow = null;
                        float deltaDistance = (mTempTransform.mY - y) / getHeight();
                        float displacement = x / getWidth();
                        Log.d(LOG_TAG, "onScroll: compare=" + compare);
                        switch (compare) {
                            case 1:
                                edgeGlow = mEdgeGlow.get(Gravity.TOP);
                                break;
                            case -1:
                                edgeGlow = mEdgeGlow.get(Gravity.BOTTOM);
                                displacement = 1.0f - displacement;
                                break;
                        }
                        if (edgeGlow != null) {
                            EdgeEffectCompat.onPull(edgeGlow, deltaDistance, displacement);
                            needsInvalidate = true;
                        }
                    }
                }

                mTouchPivotPoint.set(x, y);
                viewPointToImagePoint(getImageMatrixInternal(), mTouchPivotPoint);
            }

            if (needsInvalidate) {
                ViewCompat.postInvalidateOnAnimation(ImageViewInteractinator.this);
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return (!mDoubleTapToScaleEnabled && performClick());
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return (mDoubleTapToScaleEnabled && performClick());
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (!mDoubleTapToScaleEnabled) {
                return false;
            }

            mTransforminator.forceFinished(true);
            final float next = getNextScalePreset();
            final float sx = getImageMinScaleX() * (1.0f - next) + getImageMaxScaleX() * next;
            final float sy = getImageMinScaleY() * (1.0f - next) + getImageMaxScaleY() * next;
            mTempTransform.reset()
                    .smooth(true)
                    .pivot(mTouchPivotPoint.x, mTouchPivotPoint.y)
                    .scale(sx, sy)
                    .touchEvent(true)
                    .transform();
            return true;
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

    public static class Transform implements Parcelable {

        //region Nested classes

        public enum Mode {
            ABSOLUTE,
            RELATIVE
        }

        public static final Parcelable.Creator<Transform> CREATOR
                = new Parcelable.Creator<Transform>() {
            public Transform createFromParcel(Parcel src) {
                return new Transform(src);
            }

            public Transform[] newArray(int size) {
                return new Transform[size];
            }
        };

        //endregion Nested classes

        //region Fields

        // TODO Getters, hash, toString, equals, etc.

        private ImageViewInteractinator mTarget;

        private float mPx;
        private float mPy;
        private float mSx;
        private float mSy;
        private float mX;
        private float mY;
        private Mode mScaleMode;
        private Mode mMoveMode;

        private boolean mSmooth;
        private boolean mVerify;
        private boolean mTouchEvent;

        private boolean mResolved;
        private boolean mVerified;
        private boolean mClamped;

        //endregion Fields

        //region Constructors

        public Transform() {
            reset();
        }

        public Transform(ImageViewInteractinator target) {
            mTarget = target;
            reset();
        }

        public Transform(Transform src) {
            set(src);
        }

        private Transform(Parcel src) {
            mPx = src.readFloat();
            mPy = src.readFloat();
            mScaleMode = (Mode) src.readSerializable();
            mSx = src.readFloat();
            mSy = src.readFloat();
            mMoveMode = (Mode) src.readSerializable();
            mX = src.readFloat();
            mY = src.readFloat();
            mSmooth = (src.readByte() != 0);
            mVerify = (src.readByte() != 0);
            mTouchEvent = (src.readByte() != 0);
            mResolved = (src.readByte() != 0);
            mVerified = (src.readByte() != 0);
            mClamped = (src.readByte() != 0);
        }

        //endregion Constructors

        //region Inherited methods

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeFloat(mPx);
            dest.writeFloat(mPy);
            dest.writeSerializable(mScaleMode);
            dest.writeFloat(mSx);
            dest.writeFloat(mSy);
            dest.writeSerializable(mMoveMode);
            dest.writeFloat(mX);
            dest.writeFloat(mY);
            dest.writeByte((byte) (mSmooth ? 1 : 0));
            dest.writeByte((byte) (mVerify ? 1 : 0));
            dest.writeByte((byte) (mTouchEvent ? 1 : 0));
            dest.writeByte((byte) (mResolved ? 1 : 0));
            dest.writeByte((byte) (mVerified ? 1 : 0));
            dest.writeByte((byte) (mClamped ? 1 : 0));
        }

        //endregion Inherited methods

        //region Methods

        public Transform moveBy(float dx, float dy) {
            mMoveMode = Mode.RELATIVE;
            mX = dx;
            mY = dy;
            mResolved = mClamped = false;
            return this;
        }

        public Transform moveTo(float x, float y) {
            mMoveMode = Mode.ABSOLUTE;
            mX = x;
            mY = y;
            mResolved = mClamped = false;
            return this;
        }

        public Transform pivot(float px, float py) {
            mPx = px;
            mPy = py;
            mResolved = mClamped = false;
            return this;
        }

        public Transform reset() {
            mResolved = mVerified = mClamped = false;
            mPx = mPy = mSx = mSy = mX = mY = USE_DEFAULT;
            mVerify = true;
            mSmooth = mTouchEvent = false;
            return this;
        }

        public Transform scaleBy(float dSx, float dSy) {
            mScaleMode = Mode.RELATIVE;
            mSx = dSx;
            mSy = dSy;
            mResolved = mClamped = false;
            return this;
        }

        public Transform scale(float sx, float sy) {
            mScaleMode = Mode.ABSOLUTE;
            mSx = sx;
            mSy = sy;
            mResolved = mClamped = false;
            return this;
        }

        public void set(Transform src) {
            mTarget = src.mTarget;
            mPx = src.mPx;
            mPy = src.mPy;
            mScaleMode = src.mScaleMode;
            mSx = src.mSx;
            mSy = src.mSy;
            mMoveMode = src.mMoveMode;
            mX = src.mX;
            mY = src.mY;
            mSmooth = src.mSmooth;
            mVerify = src.mVerify;
            mTouchEvent = src.mTouchEvent;
            mResolved = src.mResolved;
            mClamped = src.mClamped;
        }

        public Transform smooth(boolean smooth) {
            mSmooth = smooth;
            return this;
        }

        protected void resolve(ImageViewInteractinator view) {
            view.resolveTransform(this);
            mScaleMode = Mode.ABSOLUTE;
            mMoveMode = Mode.ABSOLUTE;
            mResolved = true;
            mClamped = false;
        }

        public Transform target(ImageViewInteractinator target) {
            mTarget = target;
            return this;
        }

        protected Transform touchEvent(boolean touchEvent) {
            mTouchEvent = touchEvent;
            return this;
        }

        public boolean transform(ImageViewInteractinator target) {
            mTarget = target;
            return transform();
        }

        public boolean transform() {
            if (mTarget == null) {
                throw new IllegalStateException("target can not be null");
            }
            return mTarget.transform(this);
        }

        public Transform verify(boolean verify) {
            mVerify = verify;
            return this;
        }

        protected void verifyTransform(ImageViewInteractinator target) {
            mTarget = target;
            verifyTransform();
        }

        protected void verifyTransform() {
            if (mTarget == null) {
                throw new IllegalStateException("target can not be null");
            }
            mClamped = mTarget.clampTransform(this, mTouchEvent);
        }

        //endregion Methods
    }

    private static class Transforminator {
        /**
         * The interpolator, used for making transforms animate 'naturally.'
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

        public Transforminator(Context context) {
            mInterpolator = new DecelerateInterpolator();
            mAnimationDurationMillis =
                    context.getResources().getInteger(android.R.integer.config_mediumAnimTime);
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
         * Forces the transform finished state to the given value. Unlike
         * {@link #abortAnimation()}, the current zoom value isn't set to the ending value.
         *
         * @see android.widget.Scroller#forceFinished(boolean)
         */
        void forceFinished(boolean finished) {
            mFinished = finished;
        }

        /**
         * Starts a transform from the supplied start values to the supplied end values.
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
         * Computes the current scroll, returning true if the zoom is still active and false if
         * the scroll has finished.
         *
         * @see android.widget.Scroller#computeScrollOffset()
         */
        boolean computeTransform() {
            if (mFinished) {
                return false;
            }

            long tRTC = SystemClock.elapsedRealtime() - mStartRTC;
            if (tRTC >= mAnimationDurationMillis) {
                mCurrentSx = mEndSx;
                mCurrentSy = mEndSy;
                mCurrentX = mEndX;
                mCurrentY = mEndY;
                mFinished = true;
            } else {
                float t = tRTC * 1f / mAnimationDurationMillis;
                float interpolation = mInterpolator.getInterpolation(t);
                mCurrentSx = mStartSx + (mEndSx - mStartSx) * interpolation;
                mCurrentSy = mStartSy + (mEndSy - mStartSy) * interpolation;
                mCurrentX = mStartX + (mEndX - mStartX) * interpolation;
                mCurrentY = mStartY + (mEndY - mStartY) * interpolation;
            }
            return true;
        }
    }

    //endregion Nested classes

    //region Constants

    private static final String LOG_TAG = ImageViewInteractinator.class.getSimpleName();

    private static final float MAX_SCALE_BREADTH_MULTIPLIER = 4.0f;
    private static final float MAX_SCALE_LENGTH_MULTIPLIER = 6.0f;
    private static final float SCALE_PRESET_THRESHOLD = 0.2f;
    private static final float FLOAT_EPSILON = 0.00025f;
    private static final float EDGE_GLOW_SIZE_FACTOR = 1.25f;

    private static final int INVALID_FLAG_BASELINE_IMAGE_MATRIX = 0x00000001;
    private static final int INVALID_FLAG_IMAGE_MAX_SCALE = 0x00000002;
    private static final int INVALID_FLAG_IMAGE_MIN_SCALE = 0x00000004;
    private static final int INVALID_FLAG_DEFAULT =
            INVALID_FLAG_BASELINE_IMAGE_MATRIX |
                    INVALID_FLAG_IMAGE_MAX_SCALE |
                    INVALID_FLAG_IMAGE_MIN_SCALE;

    public static final float USE_DEFAULT = Float.NaN;

    //endregion Constants

    //region Fields

    private final ImageViewCompatImpl mImpl;
    private final OverScroller mOverScroller;
    private final Transforminator mTransforminator;

    private GestureDetectorCompat mGestureDetector;
    private OnGestureListener mOnGestureListener;
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
    private RectF mTempRectSrc = new RectF();
    private RectF mTempRectDst = new RectF();
    private Transform mTempTransform = new Transform(this);

    private Transform mPendingTransform = null;

    private SparseArray<EdgeEffect> mEdgeGlow;

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
            mTempTransform.reset()
                    .pivot(mTouchPivotPoint.x, mTouchPivotPoint.y)
                    .moveTo(currX, currY)
                    .transform();
            needsInvalidate = true;

            if (getOverScrollMode() != OVER_SCROLL_NEVER) {
                if (canScrollX() && mOverScroller.isOverScrolled()) {
                    EdgeEffect glowEffect = null;
                    final int diff = (currX - Math.round(mTempTransform.mX));
                    if (diff > 0) {
                        glowEffect = mEdgeGlow.get(Gravity.LEFT);
                    } else if (diff < 0) {
                        glowEffect = mEdgeGlow.get(Gravity.RIGHT);
                    }
                    if (glowEffect != null) {
                        if (glowEffect.isFinished()) {
                            glowEffect.onAbsorb(
                                    (int) OverScrollerCompat.getCurrVelocity(mOverScroller));
                        }
                        needsInvalidate = true;
                    }
                }

                if (canScrollY() && mOverScroller.isOverScrolled()) {
                    EdgeEffect glowEffect = null;
                    final int diff = (currY - Math.round(mTempTransform.mY));
                    if (diff > 0) {
                        glowEffect = mEdgeGlow.get(Gravity.TOP);
                    } else if (diff < 0) {
                        glowEffect = mEdgeGlow.get(Gravity.BOTTOM);
                    }
                    if (glowEffect != null) {
                        if (glowEffect.isFinished()) {
                            glowEffect.onAbsorb(
                                    (int) OverScrollerCompat.getCurrVelocity(mOverScroller));
                        }
                        needsInvalidate = true;
                    }
                }
            }
        } else if (mTransforminator.computeTransform()) {
            mTempTransform.reset()
                    .pivot(mTransforminator.mPx, mTransforminator.mPy)
                    .scale(mTransforminator.mCurrentSx, mTransforminator.mCurrentSy)
                    .moveTo(mTransforminator.mCurrentX, mTransforminator.mCurrentY)
                    .verify(false)
                    .transform();
            needsInvalidate = true;
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mEdgeGlow != null) {
            // The methods below rotate and translate the canvas as needed before drawing the glow,
            // since EdgeEffect always draws a top-glow at 0,0.
            boolean needsInvalidate = false;
            if (getCompatCropToPadding()) {
                mTempRectSrc.set(
                        getPaddingLeft(),
                        getPaddingTop(),
                        getWidth() - getPaddingRight(),
                        getHeight() - getPaddingBottom());
            } else {
                mTempRectSrc.set(0.0f, 0.0f, getWidth(), getHeight());
            }
            final float rectWidth = mTempRectSrc.width();
            final float rectHeight = mTempRectSrc.height();
            final int glowAreaWidth = (int) (rectWidth * EDGE_GLOW_SIZE_FACTOR);
            final int glowAreaHeight = (int) (rectHeight * EDGE_GLOW_SIZE_FACTOR);

            final int size = mEdgeGlow.size();
            for (int i = 0; i < size; i++) {
                final EdgeEffect edgeGlow = mEdgeGlow.valueAt(i);
                if (!edgeGlow.isFinished()) {
                    final int gravity = mEdgeGlow.keyAt(i);
                    final int restoreCount = canvas.save();
                    canvas.clipRect(mTempRectSrc);
                    float dx = 0.0f;
                    float dy = 0.0f;
                    float degrees = 0.0f;
                    float px = 0.0f;
                    float py = 0.0f;
                    int width = glowAreaWidth;
                    int height = glowAreaHeight;
                    switch (gravity) {
                        case Gravity.LEFT:
                            dx = mTempRectSrc.left;
                            dy = mTempRectSrc.bottom + (glowAreaHeight - rectHeight) * 0.5f;
                            degrees = -90.0f;
                            width = glowAreaHeight;
                            height = glowAreaWidth;
                            break;
                        case Gravity.TOP:
                            dx = mTempRectSrc.left - (glowAreaWidth - rectWidth) * 0.5f;
                            dy = mTempRectSrc.top;
                            break;
                        case Gravity.RIGHT:
                            dx = mTempRectSrc.right;
                            dy = mTempRectSrc.top - (glowAreaHeight - rectHeight) * 0.5f;
                            degrees = 90.0f;
                            width = glowAreaHeight;
                            height = glowAreaWidth;
                            break;
                        case Gravity.BOTTOM:
                            dx = mTempRectSrc.left - glowAreaWidth -
                                    (glowAreaWidth - rectWidth) * 0.5f;
                            dy = mTempRectSrc.bottom;
                            degrees = 180.0f;
                            px = glowAreaWidth;
                            break;
                    }
                    canvas.translate(dx, dy);
                    if (Float.compare(degrees, 0.0f) != 0) {
                        canvas.rotate(degrees, px, py);
                    }
                    edgeGlow.setSize(width, height);
                    if (edgeGlow.draw(canvas)) {
                        needsInvalidate = true;
                    }
                    canvas.restoreToCount(restoreCount);
                }
            }

            if (needsInvalidate) {
                ViewCompat.postInvalidateOnAnimation(this);
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
        if (mPendingTransform != null) {
            mPendingTransform.transform(this);
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
        if (drawableHasFunctionalDimensions()) {
            // When # of pointers changes, reset the touch pivot point to the "primary" pointer
            final int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN ||
                    action == MotionEvent.ACTION_POINTER_DOWN ||
                    action == MotionEvent.ACTION_POINTER_UP) {
                final int primaryIndex = getPrimaryPointerIndex(event);
                if (primaryIndex >= 0) {
                    mTouchPivotPoint.set(event.getX(primaryIndex), event.getY(primaryIndex));
                    viewPointToImagePoint(getImageMatrixInternal(), mTouchPivotPoint);
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

    public boolean canScrollDown() {
        final Matrix matrix = getImageMatrixInternal();
        getTranslationCoefficient(matrix, mTempPoint);
        if (mTempPoint.y < 0) {
            matrix.getValues(mTempValues);
            return (Math.round(mTempValues[Matrix.MTRANS_Y]) < getPaddingTop());
        } else {
            return false;
        }
    }

    public boolean canScrollLeft() {
        final Matrix matrix = getImageMatrixInternal();
        getTranslationCoefficient(matrix, mTempPoint);
        if (mTempPoint.x < 0) {
            matrix.getValues(mTempValues);
            return (Math.round(mTempValues[Matrix.MTRANS_X]) > mTempPoint.x);
        } else {
            return false;
        }
    }

    public boolean canScrollRight() {
        final Matrix matrix = getImageMatrixInternal();
        getTranslationCoefficient(matrix, mTempPoint);
        if (mTempPoint.x < 0) {
            matrix.getValues(mTempValues);
            return (Math.round(mTempValues[Matrix.MTRANS_X]) < getPaddingLeft());
        } else {
            return false;
        }
    }

    public boolean canScrollUp() {
        final Matrix matrix = getImageMatrixInternal();
        getTranslationCoefficient(matrix, mTempPoint);
        if (mTempPoint.y < 0) {
            matrix.getValues(mTempValues);
            return (Math.round(mTempValues[Matrix.MTRANS_Y]) > mTempPoint.y);
        } else {
            return false;
        }
    }

    public boolean canScrollX() {
        getTranslationCoefficient(getImageMatrixInternal(), mTempPoint);
        return (Math.round(mTempPoint.x) < 0);
    }

    public boolean canScrollY() {
        getTranslationCoefficient(getImageMatrixInternal(), mTempPoint);
        return (Math.round(mTempPoint.y) < 0);
    }

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

    public void getTransform(Transform outTransform) {
        mTempPoint.set(getContentCenterX(), getContentCenterY());
        viewPointToImagePoint(getImageMatrixInternal(), mTempPoint);
        outTransform.reset()
                .pivot(mTempPoint.x, mTempPoint.y)
                .scale(getImageScaleX(), getImageScaleY());
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
        final float[] va = new float[9];
        final float[] vb = new float[9];
        getImageMatrixInternal().getValues(va);
        getBaselineImageMatrix().getValues(vb);
        for (int i = 0; i < 9; i++) {
            float floatDiff = Math.abs(va[i] - vb[i]);
            if (!almostEqual(va[i], vb[i])) {
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
        if (!mScaleEnabled) {
            mScaleGestureDetector = null;
        } else if (mScaleGestureDetector == null) {
            mScaleGestureDetector =
                    new ScaleGestureDetector(getContext(), new OnScaleGestureListener());
        }
    }

    public void setScalePresets(float[] scalePresets) {
        mScalePresets = scalePresets;
    }

    public void setScrollEnabled(boolean scrollEnabled) {
        mScrollEnabled = scrollEnabled;
        updateGestureDetector();
    }

    public boolean smoothTransformBy(float px, float py, float dSx, float dSy, float dx, float dy) {
        return mTempTransform.reset()
                .smooth(true)
                .pivot(px, py)
                .scaleBy(dSx, dSy)
                .moveBy(dx, dy)
                .transform();
    }

    public boolean smoothTransformTo(float px, float py, float sx, float sy) {
        return mTempTransform.reset()
                .smooth(true)
                .pivot(px, py)
                .scale(sx, sy)
                .transform();
    }

    public boolean smoothTransformTo(float px, float py, float sx, float sy, float x, float y) {
        return mTempTransform.reset()
                .smooth(true)
                .pivot(px, py)
                .scale(sx, sy)
                .moveTo(x, y)
                .transform();
    }

    public Transform beginTransform() {
        return new Transform(this);
    }

    public boolean transform(Transform t) {
        if (!ViewCompat.isLaidOut(this)) {
            mPendingTransform = new Transform(t);
            return false;
        }

        if (!t.mResolved) {
            t.resolve(this);
        }

        if (!t.mVerified && t.mVerify) {
            t.verifyTransform();
        }

        convertTransformToMatrix(t, mTempMatrix);

        // Set the matrix or kick off the Transforminator
        final boolean transformed = !getImageMatrixInternal().equals(mTempMatrix);
        if (transformed) {
            if (t.mSmooth) {
                final float startSx = getImageScaleX();
                final float startSy = getImageScaleY();
                mTempPoint.set(t.mPx, t.mPy);
                imagePointToViewPoint(getImageMatrixInternal(), mTempPoint);
                final float startX = mTempPoint.x;
                final float startY = mTempPoint.y;
                mTransforminator.startTransform(
                        t.mPx,
                        t.mPy,
                        startSx,
                        startSy,
                        startX,
                        startY,
                        t.mSx,
                        t.mSy,
                        t.mX,
                        t.mY);
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

    public boolean transformBy(float px, float py, float dSx, float dSy, float dx, float dy) {
        return mTempTransform.reset()
                .pivot(px, py)
                .scaleBy(dSx, dSy)
                .moveBy(dx, dy)
                .transform();
    }

    public boolean transformTo(float px, float py, float sx, float sy) {
        return mTempTransform.reset()
                .pivot(px, py)
                .scale(sx, sy)
                .transform();
    }

    public boolean transformTo(float px, float py, float sx, float sy, float x, float y) {
        return mTempTransform.reset()
                .pivot(px, py)
                .scale(sx, sy)
                .moveTo(x, y)
                .transform();
    }

    //endregion Methods

    //region Protected methods

    protected boolean clampTransform(Transform t, boolean isTouchEvent) {
        if (!t.mResolved) {
            resolveTransform(t);
        }

        boolean clamped = false;
        float clampedValue = MathUtils.clamp(t.mSx, getImageMinScaleX(), getImageMaxScaleX());
        if (!almostEqual(t.mSx, clampedValue)) {
            t.mSx = clampedValue;
            clamped = true;
        }
        clampedValue = MathUtils.clamp(t.mSy, getImageMinScaleY(), getImageMaxScaleY());
        if (!almostEqual(t.mSy, clampedValue)) {
            t.mSy = clampedValue;
            clamped = true;
        }

        mImageMatrix.set(getImageMatrixInternal());
        mImageMatrix.getValues(mTempValues);
        if (!almostEqual(t.mSx, mTempValues[Matrix.MSCALE_X]) ||
                !almostEqual(t.mSx, mTempValues[Matrix.MSCALE_Y])) {
            final float deltaSx = t.mSx / mTempValues[Matrix.MSCALE_X];
            final float deltaSy = t.mSy / mTempValues[Matrix.MSCALE_Y];
            mImageMatrix.preScale(deltaSx, deltaSy);
            mImageMatrix.getValues(mTempValues);
        }

        mTempPoint.set(t.mPx, t.mPy);
        imagePointToViewPoint(mImageMatrix, mTempPoint);
        final float mappedPx = mTempPoint.x - mTempValues[Matrix.MTRANS_X];
        final float mappedPy = mTempPoint.y - mTempValues[Matrix.MTRANS_Y];

        getTranslationCoefficient(mImageMatrix, mTempPoint);
        if (Float.compare(mTempPoint.x, 0.0f) < 0) {
            final float minX = Math.min(mTempPoint.x, 0.0f);
            final float clampedDx = MathUtils.clamp(t.mX - mappedPx, minX, 0.0f);
            clampedValue = mappedPx + clampedDx;
        } else {
            clampedValue = mTempPoint.x + mappedPx;
        }
        if (!almostEqual(t.mX, clampedValue)) {
            t.mX = clampedValue;
            clamped = true;
        }

        if (Float.compare(mTempPoint.y, 0.0f) < 0) {
            final float minY = Math.min(mTempPoint.y, 0.0f);
            final float clampedDy = MathUtils.clamp(t.mY - mappedPy, minY, 0.0f);
            clampedValue = mappedPy + clampedDy;
        } else {
            clampedValue = mTempPoint.y + mappedPy;
        }
        if (!almostEqual(t.mY, clampedValue)) {
            t.mY = clampedValue;
            clamped = true;
        }
        return clamped;
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

    protected void resolveTransform(Transform t) {
        if (Float.compare(t.mPx, USE_DEFAULT) == 0) {
            t.mPx = getDrawableFunctionalWidth() * 0.5f;
        }
        if (Float.compare(t.mPy, USE_DEFAULT) == 0) {
            t.mPy = getDrawableFunctionalHeight() * 0.5f;
        }
        if (Float.compare(t.mSx, USE_DEFAULT) == 0) {
            t.mSx = getImageScaleX();
        } else if (t.mScaleMode == Transform.Mode.RELATIVE) {
            t.mSx = getImageScaleX() * t.mSx;
        }
        if (Float.compare(t.mSy, USE_DEFAULT) == 0) {
            t.mSy = getImageScaleY();
        } else if (t.mScaleMode == Transform.Mode.RELATIVE) {
            t.mSy = getImageScaleY() * t.mSy;
        }
        if (t.mMoveMode == Transform.Mode.RELATIVE) {
            mTempPoint.set(t.mPx, t.mPy);
            imagePointToViewPoint(getImageMatrixInternal(), mTempPoint);
            t.mX = mTempPoint.x + (Float.compare(t.mX, USE_DEFAULT) == 0 ? 0.0f : t.mX);
            t.mY = mTempPoint.y + (Float.compare(t.mY, USE_DEFAULT) == 0 ? 0.0f : t.mY);
        } else {
            if (Float.compare(t.mX, USE_DEFAULT) == 0) {
                t.mX = getContentCenterX();
            }
            if (Float.compare(t.mY, USE_DEFAULT) == 0) {
                t.mY = getContentCenterY();
            }
        }
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

    private boolean almostEqual(float a, float b) {
        return Math.abs(a - b) < FLOAT_EPSILON;
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

    /**
     * Returns the index of the first pointer in a MotionEvent that is currently down.
     * @param event A MotionEvent
     * @return The index of the first pointer in {@code event} that is currently down, or
     * -1 if no pointers are down
     */
    private int getPrimaryPointerIndex(MotionEvent event) {
        final int count = event.getPointerCount();
        if (count > 0) {
            final int action = event.getActionMasked();
            if ((action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_POINTER_UP) &&
                    event.getActionIndex() == 0) {
                return (count > 1 ? 1 : -1);
            } else {
                return 0;
            }
        } else {
            return -1;
        }
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

    private void convertTransformToMatrix(Transform t, Matrix outMatrix) {
        final Matrix matrix = getImageMatrixInternal();
        outMatrix.set(matrix);
        outMatrix.getValues(mTempValues);
        final float deltaSx = t.mSx / mTempValues[Matrix.MSCALE_X];
        final float deltaSy = t.mSy / mTempValues[Matrix.MSCALE_Y];
        outMatrix.preScale(deltaSx, deltaSy, t.mPx, t.mPy);
        mTempPoint.set(t.mPx, t.mPy);
        imagePointToViewPoint(outMatrix, mTempPoint);
        final float deltaTx = t.mX - mTempPoint.x;
        final float deltaTy = t.mY - mTempPoint.y;
        outMatrix.postTranslate(deltaTx, deltaTy);
    }

    private void updateGestureDetector() {
        if (!mScrollEnabled && !mFlingEnabled && !mDoubleTapToScaleEnabled) {
            mGestureDetector = null;
        } else {
            if (mGestureDetector == null) {
                mOnGestureListener = new OnGestureListener();
                mGestureDetector = new GestureDetectorCompat(getContext(), mOnGestureListener);
                mGestureDetector.setIsLongpressEnabled(false);
            }
            mGestureDetector.setOnDoubleTapListener(
                    mDoubleTapToScaleEnabled ? mOnGestureListener : null);
        }
    }

    //endregion Private methods
}
