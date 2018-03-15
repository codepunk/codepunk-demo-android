package com.codepunk.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;
import android.support.v4.util.Pools.SimplePool;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;
import android.widget.OverScroller;

import com.codepunk.demo.support.DisplayCompat;
import com.codepunk.demo.support.ImageViewCompat;

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

    private static class PoolManager {
        final SimplePool<Matrix> mMatrixPool;
        final SimplePool<TransformInfo.Options> mOptionsPool;
        final SimplePool<PointF> mPointFPool;
        final SimplePool<PtsWrapper> mPtsWrapperPool;
        final SimplePool<RectF> mRectFPool;
        final SimplePool<TransformInfo> mTransformInfoPool;
        final SimplePool<ValuesWrapper> mValuesWrapperPool;

        public PoolManager(int maxPoolSize) {
            mMatrixPool = new SimplePool<>(maxPoolSize);
            mOptionsPool = new SimplePool<>(maxPoolSize);
            mPointFPool = new SimplePool<>(maxPoolSize);
            mPtsWrapperPool = new SimplePool<>(maxPoolSize);
            mRectFPool = new SimplePool<>(maxPoolSize);
            mTransformInfoPool = new SimplePool<>(maxPoolSize);
            mValuesWrapperPool = new SimplePool<>(maxPoolSize);
        }

        Matrix acquireMatrix() {
            final Matrix instance = mMatrixPool.acquire();
            return (instance == null ? new Matrix() : instance);
        }

        Matrix acquireMatrix(Matrix src) {
            final Matrix instance = acquireMatrix();
            instance.set(src);
            return instance;
        }

        TransformInfo.Options acquireOptions() {
            final TransformInfo.Options instance = mOptionsPool.acquire();
            return (instance == null ? new TransformInfo.Options() : instance);
        }

        TransformInfo.Options acquireOptions(
                boolean constrain,
                boolean animate,
                boolean isTouchEvent) {
            final TransformInfo.Options instance = acquireOptions();
            instance.constrain = constrain;
            instance.animate = animate;
            instance.isTouchEvent = isTouchEvent;
            return instance;
        }

        PointF acquirePointF() {
            final PointF instance = mPointFPool.acquire();
            return (instance == null ? new PointF() : instance);
        }

        PtsWrapper acquirePtsWrapper() {
            final PtsWrapper instance = mPtsWrapperPool.acquire();
            return (instance == null ? new PtsWrapper() : instance);
        }

        PtsWrapper acquirePtsWrapper(float x, float y) {
            final PtsWrapper instance = acquirePtsWrapper();
            instance.pts[0] = x;
            instance.pts[1] = y;
            return instance;
        }

        RectF acquireRectF() {
            final RectF instance = mRectFPool.acquire();
            return (instance == null ? new RectF() : instance);
        }

        RectF acquireRectF(float left, float top, float right, float bottom) {
            final RectF instance = acquireRectF();
            instance.set(left, top, right, bottom);
            return instance;
        }

        RectF acquireRectF(Drawable d) {
            final RectF instance = acquireRectF();
            if (d == null) {
                instance.set(0.0f, 0.0f, 0.0f, 0.0f);
            } else {
                instance.set(0.0f, 0.0f, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            }
            return instance;
        }

        TransformInfo acquireTransformInfo() {
            final TransformInfo instance = mTransformInfoPool.acquire();
            return (instance == null ? new TransformInfo() : instance);
        }

        ValuesWrapper acquireValuesWrapper() {
            final ValuesWrapper instance = mValuesWrapperPool.acquire();
            return (instance == null ? new ValuesWrapper() : instance);
        }

        ValuesWrapper acquireValuesWrapper(Matrix matrix) {
            final ValuesWrapper wrapper = acquireValuesWrapper();
            matrix.getValues(wrapper.values);
            return wrapper;
        }

        void releaseMatrix(Matrix instance) {
            mMatrixPool.release(instance);
        }

        void releaseOptions(TransformInfo.Options instance) {
            mOptionsPool.release(instance);
        }

        void releasePointF(PointF instance) {
            mPointFPool.release(instance);
        }

        void releasePtsWrapper(PtsWrapper instance) {
            mPtsWrapperPool.release(instance);
        }

        void releaseRectF(RectF instance) {
            mRectFPool.release(instance);
        }

        void releaseTransformInfo(TransformInfo instance) {
            mTransformInfoPool.release(instance);
        }

        void releaseValuesWrapper(ValuesWrapper instance) {
            mValuesWrapperPool.release(instance);
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

    @SuppressWarnings({"WeakerAccess, unused"})
    public static class TransformInfo implements Parcelable {

        //region Nested classes

        public static class Options implements Parcelable {

            //region Constants

            public static final Parcelable.Creator<Options> CREATOR
                    = new Parcelable.Creator<Options>() {
                public Options createFromParcel(Parcel in) {
                    return new Options(in);
                }

                public Options[] newArray(int size) {
                    return new Options[size];
                }
            };

            //endregion Constants

            //region Fields

            public boolean constrain = true;
            public boolean animate = false;
            protected boolean isTouchEvent = false;

            //endregion Fields

            //region Constructors

            public Options() {}

            public Options(Options src) {
                this.constrain = src.constrain;
                this.animate = src.animate;
                this.isTouchEvent = src.isTouchEvent;
            }

            private Options(Parcel src) {
                constrain = (src.readByte() != 0);
                animate = (src.readByte() != 0);
                isTouchEvent = (src.readByte() != 0);
            }

            //endregion Constructors

            //region Inherited methods

            @SuppressWarnings("SimplifiableIfStatement")
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Options options = (Options) o;

                if (constrain != options.constrain) return false;
                if (animate != options.animate) return false;
                return isTouchEvent == options.isTouchEvent;
            }

            @Override
            public int hashCode() {
                int result = (constrain ? 1 : 0);
                result = 31 * result + (animate ? 1 : 0);
                result = 31 * result + (isTouchEvent ? 1 : 0);
                return result;
            }

            @Override
            public String toString() {
                return getClass().getSimpleName() +
                        "{constrain=" + constrain +
                        ", animate=" + animate +
                        ", isTouchEvent=" + isTouchEvent +
                        '}';
            }

            //endregion Inherited methods

            //region Implemented methods

            @Override // Parcelable
            public int describeContents() {
                return 0;
            }

            @Override // Parcelable
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeByte((byte) (constrain ? 1 : 0));
                dest.writeByte((byte) (animate ? 1 : 0));
            }

            //endregion Implemented methods
        }

        //endregion Nested classes

        //region Constants

        public static final Parcelable.Creator<TransformInfo> CREATOR
                = new Parcelable.Creator<TransformInfo>() {
            public TransformInfo createFromParcel(Parcel in) {
                return new TransformInfo(in);
            }

            public TransformInfo[] newArray(int size) {
                return new TransformInfo[size];
            }
        };

        //endregion Nested classes

        //region Fields

        public float px = 0.0f;
        public float py = 0.0f;
        public float inSx = 1.0f;
        public float inSy = 1.0f;
        public float inX = PLACE_IN_CENTER;
        public float inY = PLACE_IN_CENTER;
        public float outSx = inSx;
        public float outSy = inSy;
        public float outX = inX;
        public float outY = inY;

        //endregion Fields

        //region Constructors

        public TransformInfo() {
        }

        /*
        public TransformInfo(
                float sx,
                float sy,
                float px,
                float py,
                float x,
                float y,
                boolean animate) {
            this.inSx = sx;
            this.inSy = sy;
            this.px = px;
            this.py = py;
            this.inX = x;
            this.inY = y;
        }
        */

        public TransformInfo(TransformInfo src) {
            px = src.px;
            py = src.py;
            inSx = src.inSx;
            inSy = src.inSy;
            inX = src.inX;
            inY = src.inY;
            outSx = src.outSx;
            outSy = src.outSy;
            outX = src.outX;
            outY = src.outY;
        }

        private TransformInfo(Parcel src) {
            px = src.readFloat();
            px = src.readFloat();
            inSx = src.readFloat();
            inSy = src.readFloat();
            inX = src.readFloat();
            inY = src.readFloat();
            outSx = src.readFloat();
            outSy = src.readFloat();
            outX = src.readFloat();
            outY = src.readFloat();
        }

        //endregion Constructors

        //region Inherited methods

        @SuppressWarnings("SimplifiableIfStatement")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TransformInfo that = (TransformInfo) o;

            if (Float.compare(that.px, px) != 0) return false;
            if (Float.compare(that.py, py) != 0) return false;
            if (Float.compare(that.inSx, inSx) != 0) return false;
            if (Float.compare(that.inSy, inSy) != 0) return false;
            if (Float.compare(that.inX, inX) != 0) return false;
            if (Float.compare(that.inY, inY) != 0) return false;
            if (Float.compare(that.outSx, outSx) != 0) return false;
            if (Float.compare(that.outSy, outSy) != 0) return false;
            if (Float.compare(that.outX, outX) != 0) return false;
            return Float.compare(that.outY, outY) == 0;
        }

        @Override
        public int hashCode() {
            int result = (px != +0.0f ? Float.floatToIntBits(px) : 0);
            result = 31 * result + (py != +0.0f ? Float.floatToIntBits(py) : 0);
            result = 31 * result + (inSx != +0.0f ? Float.floatToIntBits(inSx) : 0);
            result = 31 * result + (inSy != +0.0f ? Float.floatToIntBits(inSy) : 0);
            result = 31 * result + (inX != +0.0f ? Float.floatToIntBits(inX) : 0);
            result = 31 * result + (inY != +0.0f ? Float.floatToIntBits(inY) : 0);
            result = 31 * result + (outSx != +0.0f ? Float.floatToIntBits(outSx) : 0);
            result = 31 * result + (outSy != +0.0f ? Float.floatToIntBits(outSy) : 0);
            result = 31 * result + (outX != +0.0f ? Float.floatToIntBits(outX) : 0);
            result = 31 * result + (outY != +0.0f ? Float.floatToIntBits(outY) : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TransformInfo{" +
                    "px=" + px +
                    ", py=" + py +
                    ", inSx=" + inSx +
                    ", inSy=" + inSy +
                    ", inX=" + inX +
                    ", inY=" + inY +
                    ", outSx=" + outSx +
                    ", outSy=" + outSy +
                    ", outX=" + outX +
                    ", outY=" + outY +
                    '}';
        }

        //endregion Inherited methods

        //region Implemented methods

        @Override // Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeFloat(px);
            dest.writeFloat(py);
            dest.writeFloat(inSx);
            dest.writeFloat(inSy);
            dest.writeFloat(inX);
            dest.writeFloat(inY);
            dest.writeFloat(outSx);
            dest.writeFloat(outSy);
            dest.writeFloat(outX);
            dest.writeFloat(outY);
        }

        //endregion Implemented methods

        //region Methods

        public boolean isConstrained() {
            return (inSx != outSx || inSy != outSy || inX != outX || inY != outY);
        }

        /*
        public void set(float sx, float sy, float px, float py, float x, float y, boolean animate) {
            this.inSx = sx;
            this.inSy = sy;
            this.px = px;
            this.py = py;
            this.inX = x;
            this.inY = y;
            this.animate = animate;
        }
        */

        //endregion Methods
    }

    private static class ValuesWrapper {
        float[] values = new float[9];
    }

    //endregion Nested classes

    //region Constants

    public static final float PLACE_IN_CENTER = Float.NaN;

    private static final String LOG_TAG = InteractiveImageView.class.getSimpleName();
    private static final float MAX_SCALE_BREADTH_MULTIPLIER = 4.0f;
    private static final float MAX_SCALE_LENGTH_MULTIPLIER = 6.0f;
    private static final float SCALE_PIVOT_EPSILON = 0.2f;

    private static final int INVALID_FLAG_CONTENT_RECT = 0x00000001;
    private static final int INVALID_FLAG_BASELINE_IMAGE_MATRIX = 0x00000002;
    private static final int INVALID_FLAG_IMAGE_MAX_SCALE = 0x00000004;
    private static final int INVALID_FLAG_IMAGE_MIN_SCALE = 0x00000008;
    private static final int INVALID_FLAG_PIVOT_POINT = 0x00000010;
    private static final int INVALID_FLAG_DEFAULT = INVALID_FLAG_BASELINE_IMAGE_MATRIX |
            INVALID_FLAG_IMAGE_MAX_SCALE |
            INVALID_FLAG_IMAGE_MIN_SCALE;

    private static final float EDGE_EFFECT_SIZE_FACTOR = 1.25f;

    private static final TransformInfo.Options DEFAULT_OPTIONS;
    static {
        DEFAULT_OPTIONS = new TransformInfo.Options();
        DEFAULT_OPTIONS.constrain = true;
        DEFAULT_OPTIONS.animate = false;
        DEFAULT_OPTIONS.isTouchEvent = false;
    }

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

    /*
    private int mInteractivity;
    */
    private boolean mDoubleTapToScaleEnabled;
    private boolean mScaleEnabled;
    private boolean mScrollEnabled;
    private boolean mFlingEnabled;
    private float[] mScalePivots;

    private ScaleType mScaleType = super.getScaleType();

    private float mLastSpan;

    private final Matrix mImageMatrixInternal = new Matrix();
    private final Matrix mBaselineImageMatrix = new Matrix();
    private final PointF mMaxScale = new PointF();
    private final PointF mMinScale = new PointF();
    private final PointF mPivotPoint = new PointF();
    private final Rect mContentRect = new Rect();

    private final PoolManager mPoolManager = new PoolManager(8);

    private TransformInfo mPendingTransformInfo = null;
    private int mInvalidFlags;
    boolean mPivotDirty = false;

    // Edge effect / over scroll tracking objects.
    private EdgeEffect mEdgeEffectLeft;
    private EdgeEffect mEdgeEffectTop;
    private EdgeEffect mEdgeEffectRight;
    private EdgeEffect mEdgeEffectBottom;

    private boolean mEdgeEffectLeftActive;
    private boolean mEdgeEffectTopActive;
    private boolean mEdgeEffectRightActive;
    private boolean mEdgeEffectBottomActive;

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

        boolean needsInvalidate = false;

        if (mOverScroller.computeScrollOffset()) {
            final int currX = mOverScroller.getCurrX();
            final int currY = mOverScroller.getCurrY();
            final TransformInfo info = mPoolManager.acquireTransformInfo();
            final TransformInfo.Options options =
                    mPoolManager.acquireOptions(true, false, false);
            info.px = mPivotPoint.x;
            info.py = mPivotPoint.y;
            info.inSx = getImageScaleX();
            info.inSy = getImageScaleY();
            info.inX = currX;
            info.inY = currY;
            needsInvalidate = transformImage(info, options); // || needsInvalidate;

            /*
            mGestureTransformInfo.set(
                    getImageScaleX(),
                    getImageScaleY(),
                    mPivotPoint.x,
                    mPivotPoint.y,
                    currX,
                    currY,
                    false);
            constrainTransformInfo(mGestureTransformInfo, true);
            */

            if (getOverScrollMode() != OVER_SCROLL_NEVER) {
                if (canScrollX() && mOverScroller.isOverScrolled()) {
                    final int diff = (currX - Math.round(info.outX));
                    if (diff > 0 &&
                            mEdgeEffectLeft.isFinished() &&
                            !mEdgeEffectLeftActive) {
                        mEdgeEffectLeft.onAbsorb(
                                (int) OverScrollerCompat.getCurrVelocity(mOverScroller));
                        mEdgeEffectLeftActive = true;
                        needsInvalidate = true;
                    } else if (diff < 0 &&
                            mEdgeEffectRight.isFinished() &&
                            !mEdgeEffectRightActive) {
                        mEdgeEffectRight.onAbsorb
                                ((int) OverScrollerCompat.getCurrVelocity(mOverScroller));
                        mEdgeEffectRightActive = true;
                        needsInvalidate = true;
                    }
                }

                if (canScrollY() && mOverScroller.isOverScrolled()) {
                    final int diff = (currY - Math.round(info.outY));
                    if (diff > 0 &&
                            mEdgeEffectTop.isFinished() &&
                            !mEdgeEffectTopActive) {
                        mEdgeEffectTop.onAbsorb(
                                (int) OverScrollerCompat.getCurrVelocity(mOverScroller));
                        mEdgeEffectTopActive = true;
                        needsInvalidate = true;
                    } else if (diff < 0 &&
                            mEdgeEffectBottom.isFinished() &&
                            !mEdgeEffectBottomActive) {
                        mEdgeEffectBottom.onAbsorb
                                ((int) OverScrollerCompat.getCurrVelocity(mOverScroller));
                        mEdgeEffectBottomActive = true;
                        needsInvalidate = true;
                    }
                }
            }

            mPoolManager.releaseOptions(options);
            mPoolManager.releaseTransformInfo(info);
        } else if (mTransformer.computeTransform()) {
            final TransformInfo info = mPoolManager.acquireTransformInfo();
            final TransformInfo.Options options =
                    mPoolManager.acquireOptions(false, false, false);
            info.px = mTransformer.mPx;
            info.py = mTransformer.mPy;
            info.inSx = mTransformer.mCurrentSx;
            info.inSy = mTransformer.mCurrentSy;
            info.inX = mTransformer.mCurrentX;
            info.inY = mTransformer.mCurrentY;
            needsInvalidate = transformImage(info, options);
            mPoolManager.releaseOptions(options);
            mPoolManager.releaseTransformInfo(info);
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public ScaleType getScaleType() {
        return mScaleType;
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);
        if (mPendingTransformInfo != null) {
            // TODO We can also defer this to when we have a layout AND a drawable?
            transformImage(mPendingTransformInfo);
            mPendingTransformInfo = null;
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mEdgeEffectLeft != null) {
            // The methods below rotate and translate the canvas as needed before drawing the glow,
            // since EdgeEffectCompat always draws a top-glow at 0,0.
            boolean needsInvalidate = false;
            final RectF glowRect = mPoolManager.acquireRectF();
            if (ImageViewCompat.getCropToPadding(this)) {
                glowRect.set(getContentRect());
            } else {
                glowRect.set(0.0f, 0.0f, getWidth(), getHeight());
            }
            final float rectWidth = glowRect.width();
            final float rectHeight = glowRect.height();
            final int glowAreaWidth = (int) (rectWidth * EDGE_EFFECT_SIZE_FACTOR);
            final int glowAreaHeight = (int) (rectHeight * EDGE_EFFECT_SIZE_FACTOR);

            if (!mEdgeEffectTop.isFinished()) {
                final int restoreCount = canvas.save();
                canvas.clipRect(glowRect);
                final float dx = glowRect.left - (glowAreaWidth - rectWidth) * 0.5f;
                final float dy = glowRect.top;
                canvas.translate(dx, dy);
                mEdgeEffectTop.setSize(glowAreaWidth, glowAreaHeight);
                if (mEdgeEffectTop.draw(canvas)) {
                    needsInvalidate = true;
                }
                canvas.restoreToCount(restoreCount);
            }

            if (!mEdgeEffectBottom.isFinished()) {
                final int restoreCount = canvas.save();
                canvas.clipRect(glowRect);
                final float dx = glowRect.left - (glowAreaWidth - rectWidth) * 0.5f;
                final float dy = glowRect.bottom;
                canvas.translate(-glowAreaWidth + dx, dy);
                canvas.rotate(180.0f, glowAreaWidth, 0.0f);
                mEdgeEffectBottom.setSize(glowAreaWidth, glowAreaHeight);
                if (mEdgeEffectBottom.draw(canvas)) {
                    needsInvalidate = true;
                }
                canvas.restoreToCount(restoreCount);
            }

            if (!mEdgeEffectLeft.isFinished()) {
                final int restoreCount = canvas.save();
                canvas.clipRect(glowRect);
                final float dx = glowRect.left;
                final float dy = glowRect.bottom + (glowAreaHeight - rectHeight) * 0.5f;
                canvas.translate(dx, dy);
                canvas.rotate(-90.0f, 0.0f, 0.0f);
                mEdgeEffectLeft.setSize(glowAreaHeight, glowAreaWidth);
                if (mEdgeEffectLeft.draw(canvas)) {
                    needsInvalidate = true;
                }
                canvas.restoreToCount(restoreCount);
            }

            if (!mEdgeEffectRight.isFinished()) {
                final int restoreCount = canvas.save();
                canvas.clipRect(glowRect);
                final float dx = glowRect.right;
                final float dy = glowRect.top - (glowAreaHeight - rectHeight) * 0.5f;
                canvas.translate(dx, dy);
                canvas.rotate(90.0f, 0.0f, 0.0f);
                mEdgeEffectRight.setSize(glowAreaHeight, glowAreaWidth);
                if (mEdgeEffectRight.draw(canvas)) {
                    needsInvalidate = true;
                }
                canvas.restoreToCount(restoreCount);
            }

            if (needsInvalidate) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
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
        final int action = event.getActionMasked();
        if (drawableHasIntrinsicSize()) {
            // If we're going from 1 pointer to 2 or vice versa, mark the pivot point dirty
            if ((action == MotionEvent.ACTION_POINTER_DOWN ||
                    action == MotionEvent.ACTION_POINTER_UP)) {
                if (event.getPointerCount() == 2) {
                    mPivotDirty = true;
                }
            }
            retVal = mScaleGestureDetector.onTouchEvent(event);
            retVal = mGestureDetector.onTouchEvent(event) || retVal;
        }
        if (action == MotionEvent.ACTION_UP) {
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
        setScaleType(mScaleType);
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
    public void setOverScrollMode(int mode) {
        super.setOverScrollMode(mode);
        if (mode != OVER_SCROLL_NEVER) {
            if (mEdgeEffectLeft == null) {
                Context context = getContext();
                mEdgeEffectLeft = new EdgeEffect(context);
                mEdgeEffectTop = new EdgeEffect(context);
                mEdgeEffectRight = new EdgeEffect(context);
                mEdgeEffectBottom = new EdgeEffect(context);
            }
        } else {
            mEdgeEffectLeft = null;
            mEdgeEffectTop = null;
            mEdgeEffectRight = null;
            mEdgeEffectBottom = null;
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

    //region Implemented methods

    @Override // GestureDetector.OnGestureListener
    public boolean onDown(MotionEvent e) {
        if (!mScrollEnabled && !mScaleEnabled && !mDoubleTapToScaleEnabled) {
            return false;
        }

        maybeReleaseEdgeEffects();
        mOverScroller.forceFinished(true);
        getPivotPoint(e.getX(), e.getY(), getImageMatrixInternal(), mPivotPoint);
        mPivotDirty = false;
        return true;
    }

    @Override // GestureDetector.OnGestureListener
    public void onShowPress(MotionEvent e) {
    }

    @Override // GestureDetector.OnGestureListener
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override // GestureDetector.OnGestureListener
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (!mScrollEnabled || mScaleGestureDetector.isInProgress()) {
            return false;
        }

        if (mPivotDirty) {
            mPivotDirty = false;
            getPivotPoint(e2.getX(), e2.getY(), getImageMatrixInternal(), mPivotPoint);
        }

        final float x = e2.getX();
        final float y = e2.getY();
        final TransformInfo info = mPoolManager.acquireTransformInfo();
        final TransformInfo.Options options =
                mPoolManager.acquireOptions(true, false, true);
        info.px = mPivotPoint.x;
        info.py = mPivotPoint.y;
        info.inSx = getImageScaleX();
        info.inSy = getImageScaleY();
        info.inX = x;
        info.inY = y;

        /*
        mGestureTransformInfo.set(
                getImageScaleX(),
                getImageScaleY(),
                mPivotPoint.x,
                mPivotPoint.y,
                x,
                y,
                false);
        final boolean constrained = constrainTransformInfo(mGestureTransformInfo, true);
        */

        boolean needsInvalidate = transformImage(info, options);
        if (info.isConstrained()) {
            getPivotPoint(x, y, getImageMatrixInternal(), mPivotPoint);
        }
        final Rect contentRect = getContentRect();
        final int overScrollMode = getOverScrollMode();
        final boolean canOverScrollX = (overScrollMode == OVER_SCROLL_ALWAYS ||
                (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollX()));

        if (canOverScrollX) {
            final float constrainedDiff = info.outX - x;
            if (constrainedDiff < 0.0f) {
                mEdgeEffectLeftActive = true;
                EdgeEffectCompat.onPull(
                        mEdgeEffectLeft,
                        constrainedDiff / getContentRect().width(),
                        1.0f - y / getHeight());
                needsInvalidate = true;
            } else if (constrainedDiff > 0.0f) {
                mEdgeEffectRightActive = true;
                EdgeEffectCompat.onPull(
                        mEdgeEffectRight,
                        constrainedDiff / getContentRect().width(),
                        y / getHeight());
                needsInvalidate = true;
            }
        }

        final boolean canOverScrollY = (overScrollMode == OVER_SCROLL_ALWAYS ||
                (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollY()));
        if (canOverScrollY) {
            final float constrainedDiff = info.outY - y;
            if (constrainedDiff < 0.0f) {
                EdgeEffectCompat.onPull(
                        mEdgeEffectTop,
                        constrainedDiff / contentRect.height(),
                        x / getWidth());
                mEdgeEffectTopActive = true;
                needsInvalidate = true;
            } else if (constrainedDiff > 0.0f) {
                EdgeEffectCompat.onPull(
                        mEdgeEffectBottom,
                        constrainedDiff / contentRect.height(),
                        1.0f - x / getWidth());
                mEdgeEffectBottomActive = true;
                needsInvalidate = true;
            }
        }

        mPoolManager.releaseOptions(options);
        mPoolManager.releaseTransformInfo(info);

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
        return true;
    }

    @Override // GestureDetector.OnGestureListener
    public void onLongPress(MotionEvent e) {
    }

    @Override // GestureDetector.OnGestureListener
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (!mFlingEnabled) {
            return false;
        }

        if (mPivotDirty) {
            // This catches an outlier case where user removes all fingers simultaneously.
            // Depending on the timing, mPivotDirty may be set to true but is not caught by
            // an onScroll or onScale before onFling is called. In that case, ignore the gesture.
            mPivotDirty = false;
            return false;
        }

        maybeReleaseEdgeEffects();
        final Matrix matrix = getImageMatrixInternal();
        final ValuesWrapper matrixValues = mPoolManager.acquireValuesWrapper(matrix);
        final RectF mappedImageRect = mPoolManager.acquireRectF();
        getMappedImageRect(matrix, mappedImageRect);
        final Rect contentRect = getContentRect();
        final float startX = e2.getX();
        final float startY = e2.getY();
        final float scrollableX = Math.max(mappedImageRect.width() - contentRect.width(), 0);
        final float scrollableY = Math.max(mappedImageRect.height() - contentRect.height(), 0);
        final float scrolledX = -Math.min(matrixValues.values[MTRANS_X], 0);
        final float scrolledY = -Math.min(matrixValues.values[MTRANS_Y], 0);
        final int overScrollMode = getOverScrollMode();
        final boolean canOverScrollX = (overScrollMode == OVER_SCROLL_ALWAYS ||
                (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && scrollableX > 0));
        final boolean canOverScrollY = (overScrollMode == OVER_SCROLL_ALWAYS ||
                (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && scrollableY > 0));
        final int overX = (canOverScrollX ? contentRect.width() / 2 : 0);
        final int overY = (canOverScrollY ? contentRect.height() / 2 : 0);
        mOverScroller.fling(
                (int) startX,
                (int) startY,
                (int) velocityX,
                (int) velocityY,
                (int) (startX - scrollableX + scrolledX),
                (int) (startX + scrolledX),
                (int) (startY - scrollableY + scrolledY),
                (int) (startY + scrolledY),
                overX,
                overY);
        mPoolManager.releaseRectF(mappedImageRect);
        mPoolManager.releaseValuesWrapper(matrixValues);
        return true;
    }

    @Override // GestureDetector.OnDoubleTapListener
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override // GestureDetector.OnDoubleTapListener
    public boolean onDoubleTap(MotionEvent e) {
        if (!mDoubleTapToScaleEnabled) {
            return false;
        }

        mTransformer.forceFinished(true);

        final TransformInfo info = mPoolManager.acquireTransformInfo();
        final TransformInfo.Options options =
                mPoolManager.acquireOptions(true, true, false);
        final float next = getNextScalePivot();
//        final float minScaleX = getImageMinScaleX();
//        final float minScaleY = getImageMinScaleY();
        info.px = mPivotPoint.x;
        info.py = mPivotPoint.y;
//        TODO simplify? min + (max - min) * next is equivalent to (min * (1 - next)) + (max * next)
//        info.inSx = minScaleX + (getImageMaxScaleX() - minScaleX) * next;
//        info.inSy = minScaleY + (getImageMaxScaleY() - minScaleY) * next;
        info.inSx = getImageMinScaleX() * (1.0f - next) + getImageMaxScaleX() * next;
        info.inSy = getImageMinScaleY() * (1.0f - next) + getImageMaxScaleY() * next;
        info.inX = getContentRect().exactCenterX(); //getPaddingLeft() + getContentRect().width() * 0.5f;
        info.inY = getContentRect().exactCenterY();


//        mGestureTransformInfo.set(sx, sy, mPivotPoint.x, mPivotPoint.y, x, y, true);
//        constrainTransformInfo(mGestureTransformInfo, false);
        transformImage(info, options);
        mPoolManager.releaseOptions(options);
        mPoolManager.releaseTransformInfo(info);
        return true;
    }

    @Override // GestureDetector.OnDoubleTapListener
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override // ScaleGestureDetector.OnScaleGestureListener
    public boolean onScale(ScaleGestureDetector detector) {
        final float currentSpan = detector.getCurrentSpan();
        final float spanDelta = (currentSpan / mLastSpan);
        /*
        final float sx = getImageScaleX() * spanDelta;
        final float sy = getImageScaleY() * spanDelta;
        final float x = detector.getFocusX();
        final float y = detector.getFocusY();
        */
        final float x = detector.getFocusX();
        final float y = detector.getFocusY();

        if (mPivotDirty) {
            mPivotDirty = false;
            getPivotPoint(x, y, getImageMatrixInternal(), mPivotPoint);
        }

        final TransformInfo info = mPoolManager.acquireTransformInfo();
        info.px = mPivotPoint.x;
        info.py = mPivotPoint.y;
        info.inSx = getImageScaleX() * spanDelta;
        info.inSy = getImageScaleY() * spanDelta;
        info.inX = x;
        info.inY = y;

        // TODO TEMP
        TransformInfo.Options options = new TransformInfo.Options();
        options.constrain = true;
        options.animate = false;
        // END TEMP

//        mGestureTransformInfo.set(sx, sy, mPivotPoint.x, mPivotPoint.y, x, y, false);
//        final boolean constrained = constrainTransformInfo(mGestureTransformInfo, true);
        transformImage(info);
        if (info.isConstrained()) {
            getPivotPoint(x, y, getImageMatrixInternal(), mPivotPoint);
        }
        mLastSpan = currentSpan;
        return true;
    }

    @Override // ScaleGestureDetector.OnScaleGestureListener
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (!mScaleEnabled) {
            return false;
        }

        mLastSpan = detector.getCurrentSpan();
        mPivotDirty = true;
        return true;
    }

    @Override // ScaleGestureDetector.OnScaleGestureListener
    public void onScaleEnd(ScaleGestureDetector detector) {
        mPivotDirty = true;
    }

    //endregion Implemented methods

    //region Methods

    public void constrainTransformInfo(TransformInfo info) {
        constrainTransformInfo(info, false);
    }

    protected int getDrawableIntrinsicHeight() {
        final Drawable dr = getDrawable();
        return (dr == null ? -1 : dr.getIntrinsicHeight());
    }

    protected int getDrawableIntrinsicWidth() {
        final Drawable dr = getDrawable();
        return (dr == null ? -1 : dr.getIntrinsicWidth());
    }

    public float getImagePivotX() {
        final PointF pivotPoint = mPoolManager.acquirePointF();
        getPivotPoint(
                getContentRect().exactCenterX(),
                0.0f,
                getImageMatrixInternal(),
                pivotPoint);
        final float pivotX = pivotPoint.x;
        mPoolManager.releasePointF(pivotPoint);
        return pivotX;
    }

    public float getImagePivotY() {
        final PointF pivotPoint = mPoolManager.acquirePointF();
        getPivotPoint(
                0.0f,
                getContentRect().exactCenterY(),
                getImageMatrixInternal(),
                pivotPoint);
        final float pivotY = pivotPoint.y;
        mPoolManager.releasePointF(pivotPoint);
        return pivotY;
    }

    public float getImageMaxScaleX() {
        getImageMaxScale();
        return mMaxScale.x;
    }

    public float getImageMaxScaleY() {
        getImageMaxScale();
        return mMaxScale.y;
    }

    public float getImageMinScaleX() {
        getImageMinScale();
        return mMinScale.x;
    }

    public float getImageMinScaleY() {
        getImageMinScale();
        return mMinScale.y;
    }

    public float getImageScaleX() {
        final ValuesWrapper matrixValues =
                mPoolManager.acquireValuesWrapper(getImageMatrixInternal());
        final float scaleX = matrixValues.values[MSCALE_X];
        mPoolManager.releaseValuesWrapper(matrixValues);
        return scaleX;
    }

    public float getImageScaleY() {
        final ValuesWrapper matrixValues =
                mPoolManager.acquireValuesWrapper(getImageMatrixInternal());
        final float scaleY = matrixValues.values[MSCALE_Y];
        mPoolManager.releaseValuesWrapper(matrixValues);
        return scaleY;
    }

    public void getTransformInfo(TransformInfo outInfo) {
        matrixToTransformInfo(getImageMatrixInternal(), outInfo);
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

    public boolean onUp(MotionEvent e) {
        return false;
    }

    public void setDoubleTapToScaleEnabled(boolean enabled) {
        mDoubleTapToScaleEnabled = enabled;
    }

    public void setFlingEnabled(boolean enabled) {
        mFlingEnabled = true;
    }

    public void setScaleEnabled(boolean enabled) {
        mScaleEnabled = enabled;
    }

    public void setScrollEnabled(boolean enabled) {
        mScrollEnabled = enabled;
    }

    public void setScalePivots(float... pivots) {
        mScalePivots = pivots;
    }

    /*
    @SuppressWarnings("UnusedReturnValue")
    public boolean transformImage(float sx, float sy, float px, float py) {
        return transformImage(sx, sy, px, py, false);
    }

    public boolean transformImage(float sx, float sy, float px, float py, boolean animate) {
        return transformImage(sx, sy, px, py, PLACE_IN_CENTER, PLACE_IN_CENTER, animate);
    }

    public boolean transformImage(
            float sx,
            float sy,
            float px,
            float py,
            float x,
            float y) {
        return transformImage(sx, sy, px, py, x, y, false);
    }
    */

    public boolean transformImage(
            float px,
            float py,
            float sx,
            float sy,
            float x,
            float y) {
        return transformImage(px, py, sx, sy, x, y, null);
    }

    public boolean transformImage(
            float px,
            float py,
            float sx,
            float sy,
            float x,
            float y,
            TransformInfo.Options options) {
        final TransformInfo info = mPoolManager.acquireTransformInfo();
        info.px = px;
        info.py = py;
        info.inSx = sx;
        info.inSy = sy;
        info.inX = x;
        info.inY = y;
        final boolean transformed = transformImage(info, options);
        mPoolManager.releaseTransformInfo(info);
        return transformed;

        /*
        if (!ViewCompat.isLaidOut(this)) {
            mPendingTransformInfo = new TransformInfo(sx, sy, px, py, x, y, animate);
            mPendingTransformInfo.inSx = sx;
            mPendingTransformInfo.inSy = sy;
            mPending
            return false;
        }

        if (Float.isNaN(x)) {
            x = getContentRect().exactCenterX();
        }
        if (Float.isNaN(y)) {
            y = getContentRect().exactCenterY();
        }

        final Matrix matrix = getImageMatrixInternal();
        final Matrix transformMatrix = mPoolManager.acquireMatrix();
        // TODO When do I check if image has intrinsic size? Or does it matter?
        transformToMatrix(sx, sy, px, py, x, y, transformMatrix);
        final boolean transformed = !matrix.equals(transformMatrix);
        if (transformed) {
            if (animate) {
                final PtsWrapper drawablePts = mPoolManager.acquirePtsWrapper(px, py);
                final PtsWrapper viewPts = mPoolManager.acquirePtsWrapper();
                getImageMatrixInternal().mapPoints(viewPts.pts, drawablePts.pts);
                final float startX = viewPts.getX();
                final float startY = viewPts.getY();
                mPoolManager.releasePtsWrapper(viewPts);
                mPoolManager.releasePtsWrapper(drawablePts);
                mTransformer.startTransform(
                        px,
                        py,
                        getImageScaleX(),
                        getImageScaleY(),
                        startX,
                        startY,
                        sx,
                        sy,
                        x,
                        y);
                ViewCompat.postInvalidateOnAnimation(this);
            } else {
                if (super.getScaleType() != ScaleType.MATRIX) {
                    super.setScaleType(ScaleType.MATRIX);
                }
                super.setImageMatrix(transformMatrix);
            }
        }
        mPoolManager.releaseMatrix(transformMatrix);
        return transformed;
        */
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean transformImage(TransformInfo info) {
        return transformImage(info, null);
    }

    public boolean transformImage(TransformInfo info, TransformInfo.Options options) {
        if (info == null) {
            return false;
        }

        if (!ViewCompat.isLaidOut(this)) {
            mPendingTransformInfo = new TransformInfo(info);
            // TODO Pending options?
            return false;
        }

        if (options == null) {
            options = DEFAULT_OPTIONS;
        }

        if (options.constrain) {
            constrainTransformInfo(info, options.isTouchEvent);
        } else {
            // TODO Think about a clean way to get a "real" number
            // TODO Think about setting outXYZ
            info.outSx = (Float.isNaN(info.inSx) ? getImageScaleX() : info.inSx);
            info.outSy = (Float.isNaN(info.inSy) ? getImageScaleY() : info.inSy);
            info.outX = (Float.isNaN(info.inX) ? getContentRect().exactCenterX() : info.inX);
            info.outY = (Float.isNaN(info.inY) ? getContentRect().exactCenterY() : info.inY);
        }

        final Matrix matrix = getImageMatrixInternal();
        final Matrix transformMatrix = mPoolManager.acquireMatrix();
        // TODO When do I check if image has intrinsic size? Or does it matter?
        transformToMatrix(info, transformMatrix);
        final boolean transformed = !matrix.equals(transformMatrix);
        if (transformed) {
            if (options.animate) {
                final PtsWrapper drawablePts = mPoolManager.acquirePtsWrapper(info.px, info.py);
                final PtsWrapper viewPts = mPoolManager.acquirePtsWrapper();
                getImageMatrixInternal().mapPoints(viewPts.pts, drawablePts.pts);
                final float startX = viewPts.getX();
                final float startY = viewPts.getY();
                mPoolManager.releasePtsWrapper(viewPts);
                mPoolManager.releasePtsWrapper(drawablePts);
                mTransformer.startTransform(
                        info.px,
                        info.py,
                        getImageScaleX(),
                        getImageScaleY(),
                        startX,
                        startY,
                        info.outSx,
                        info.outSy,
                        info.outX,
                        info.outY);
                ViewCompat.postInvalidateOnAnimation(this);
            } else {
                if (super.getScaleType() != ScaleType.MATRIX) {
                    super.setScaleType(ScaleType.MATRIX);
                }
                super.setImageMatrix(transformMatrix);
            }
        }
        mPoolManager.releaseMatrix(transformMatrix);
        return transformed;
    }

    //endregion Methods

    //region Protected methods

    protected boolean canScrollX() {
        return canScrollX(getImageMatrixInternal());
    }

    @SuppressWarnings("SpellCheckingInspection")
    protected boolean canScrollX(Matrix matrix) {
        final PointF tc = mPoolManager.acquirePointF();
        getTranslationCoefficient(matrix, tc);
        final boolean canScrollX = (Math.round(tc.x) < 0
        );
        mPoolManager.releasePointF(tc);
        return canScrollX;
    }

    protected boolean canScrollY() {
        return canScrollY(getImageMatrixInternal());
    }

    protected boolean canScrollY(Matrix matrix) {
        final PointF tc = mPoolManager.acquirePointF();
        getTranslationCoefficient(matrix, tc);
        final boolean canScrollY = (Math.round(tc.y) < 0);
        mPoolManager.releasePointF(tc);
        return canScrollY;
    }

    @SuppressWarnings("unused")
    protected void constrainTransformInfo(TransformInfo info, boolean isTouchEvent) {
        boolean constrained = false;
        if (info != null) {
            if (drawableHasIntrinsicSize()) {
                // Get matrix and values
                final Matrix matrix = mPoolManager.acquireMatrix(getImageMatrixInternal());
                final ValuesWrapper matrixValues =
                        mPoolManager.acquireValuesWrapper(matrix);

                // Clamp scale
                final float clampedSx =
                        MathUtils.clamp(info.inSx, getImageMinScaleX(), getImageMaxScaleX());
                final float clampedSy =
                        MathUtils.clamp(info.inSy, getImageMinScaleY(), getImageMaxScaleY());

                // Pre-scale the matrix to the clamped scale
                final float deltaSx = clampedSx / matrixValues.values[MSCALE_X];
                final float deltaSy = clampedSy / matrixValues.values[MSCALE_Y];
                matrix.preScale(deltaSx, deltaSy);
                matrix.getValues(matrixValues.values);
                info.outSx = matrixValues.values[MSCALE_X];
                info.outSy = matrixValues.values[MSCALE_Y];

                // TODO Describe this while I remember what it all means :-)
                //final float clampedX;
                //final float clampedY;
                final PtsWrapper drawablePts = mPoolManager.acquirePtsWrapper(info.px, info.py);
                final PtsWrapper viewPts = mPoolManager.acquirePtsWrapper();
                matrix.mapPoints(viewPts.pts, drawablePts.pts);
                final float mappedPx = viewPts.getX() - matrixValues.values[MTRANS_X];
                final float mappedPy = viewPts.getY() - matrixValues.values[MTRANS_Y];

                final PointF coefficient = mPoolManager.acquirePointF();
                getTranslationCoefficient(matrix, coefficient);
                if (coefficient.x >= 0) {
                    info.outX = getPaddingLeft() + coefficient.x + mappedPx;
                } else {
                    // TODO Think about setting outXYZ
                    final float x =
                            (Float.isNaN(info.inX) ? getContentRect().exactCenterX() : info.inX);
                    final float minX = Math.min(coefficient.x, 0.0f);
                    final float clampedDx =
                            MathUtils.clamp(x - mappedPx, minX, 0.0f);
                    info.outX = mappedPx + clampedDx;
                }
                if (coefficient.y >= 0) {
                    info.outY = getPaddingTop() + coefficient.y + mappedPy;
                } else {
                    // TODO Think about setting outXYZ
                    final float y =
                            (Float.isNaN(info.inY) ? getContentRect().exactCenterY() : info.inY);
                    final float minY = Math.min(coefficient.y, 0.0f);
                    final float clampedDy =
                            MathUtils.clamp(y - mappedPy, minY, 0.0f);
                    info.outY = mappedPy + clampedDy;
                }

                mPoolManager.releasePointF(coefficient);
                mPoolManager.releasePtsWrapper(viewPts);
                mPoolManager.releasePtsWrapper(drawablePts);
                mPoolManager.releaseValuesWrapper(matrixValues);
                mPoolManager.releaseMatrix(matrix);
            }
        }
    }

    protected boolean drawableHasIntrinsicSize() {
        return getDrawableIntrinsicWidth() > 0 && getDrawableIntrinsicHeight() > 0;
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

    @SuppressWarnings("SpellCheckingInspection")
    protected void getBaselineImageMatrix(ScaleType scaleType, Matrix outMatrix) {
        if (outMatrix != null) {
            if (drawableHasIntrinsicSize()) {
                // We need to do the scaling ourselves.
                final int dwidth = getDrawableIntrinsicWidth();
                final int dheight = getDrawableIntrinsicHeight();

                final int vwidth = getContentRect().width();
                final int vheight = getContentRect().height();

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
                    RectF srcRect =
                            mPoolManager.acquireRectF(0.0f, 0.0f, dwidth, dheight);
                    RectF dstRect =
                            mPoolManager.acquireRectF(0.0f, 0.0f, vwidth, vheight);
                    outMatrix.setRectToRect(
                            srcRect,
                            dstRect,
                            scaleTypeToScaleToFit(scaleType));
                    mPoolManager.releaseRectF(dstRect);
                    mPoolManager.releaseRectF(srcRect);
                }
            } else {
                outMatrix.reset();
            }
        }
    }

    /**
     * Returns the view's content rectangle; that is, the rectangle defined by the view
     * minus any padding. Do not change this rectangle in place but make a copy.
     *
     * @return
     */
    protected Rect getContentRect() {
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
     *
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
        if ((mInvalidFlags & INVALID_FLAG_IMAGE_MAX_SCALE) != 0) {
            mInvalidFlags &= ~INVALID_FLAG_IMAGE_MAX_SCALE;
            final float maxScaleX;
            final float maxScaleY;
            if (drawableHasIntrinsicSize()) {
                final Matrix baselineMatrix = getBaselineImageMatrix();
                final ValuesWrapper matrixValues =
                        mPoolManager.acquireValuesWrapper(baselineMatrix);
                final RectF mappedImageRect = mPoolManager.acquireRectF();
                getMappedImageRect(baselineMatrix, mappedImageRect);

                final float baselineWidth = mappedImageRect.width();
                final float baselineHeight = mappedImageRect.height();
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
                final Rect contentRect = getContentRect();
                final int availableSize;
                if (baselineWidth < baselineHeight) {
                    availableSize = contentRect.width();
                } else if (baselineWidth > baselineHeight) {
                    availableSize = contentRect.height();
                } else {
                    availableSize = Math.min(contentRect.width(), contentRect.height());
                }
                final float viewBasedScale = availableSize / baselineBreadth;
                final float scale = Math.max(screenBasedScale, viewBasedScale);

                maxScaleX = scale * matrixValues.values[MSCALE_X];
                maxScaleY = scale * matrixValues.values[MSCALE_Y];

                mPoolManager.releaseRectF(mappedImageRect);
                mPoolManager.releaseValuesWrapper(matrixValues);
            } else {
                maxScaleX = maxScaleY = 1.0f;
            }
            setImageMaxScale(maxScaleX, maxScaleY);
        }
    }

    // TODO JavaDoc needs to state that method must call setImageMinScale if overridden
    protected void getImageMinScale() {
        if ((mInvalidFlags & INVALID_FLAG_IMAGE_MIN_SCALE) != 0) {
            mInvalidFlags &= ~INVALID_FLAG_IMAGE_MIN_SCALE;
            final ValuesWrapper matrixValues =
                    mPoolManager.acquireValuesWrapper(getBaselineImageMatrix());
            setImageMinScale(matrixValues.values[MSCALE_X], matrixValues.values[MSCALE_Y]);
            mPoolManager.releaseValuesWrapper(matrixValues);
        }
    }

    protected void getPivotPoint(float x, float y, Matrix matrix, PointF outPoint) {
        if (matrix != null && outPoint != null) {
            final Matrix invertedMatrix = mPoolManager.acquireMatrix();
            final PtsWrapper srcPts = mPoolManager.acquirePtsWrapper(x, y);
            final PtsWrapper dstPts = mPoolManager.acquirePtsWrapper();
            matrix.invert(invertedMatrix);
            invertedMatrix.mapPoints(dstPts.pts, srcPts.pts);
            outPoint.set(dstPts.getX(), dstPts.getY());
            mPoolManager.releasePtsWrapper(dstPts);
            mPoolManager.releasePtsWrapper(srcPts);
            mPoolManager.releaseMatrix(invertedMatrix);
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
        mMaxScale.set(sx, sy);
    }

    protected void setImageMinScale(float sx, float sy) {
        mInvalidFlags &= ~INVALID_FLAG_IMAGE_MIN_SCALE;
        mMinScale.set(sx, sy);
    }

    //endregion Protected methods

    //region Private methods

    private void getMappedImageRect(@NonNull Matrix matrix, @NonNull RectF outRect) {
        final RectF drawableRect = mPoolManager.acquireRectF(getDrawable());
        matrix.mapRect(outRect, drawableRect);
        mPoolManager.releaseRectF(drawableRect);
    }

    private float getNextScalePivot() {
        float nextScalePivot = 0.0f;
        if (mScalePivots != null) {
            final float minSx = getImageMinScaleX();
            final float minSy = getImageMinScaleY();
            float relativeSx = (getImageScaleX() - minSx) / (getImageMaxScaleX() - minSx);
            float relativeSy = (getImageScaleY() - minSy) / (getImageMaxScaleY() - minSy);
            // TODO Think about a clean way to get a "real" number
            if (Float.isNaN(relativeSx) || Float.isInfinite(relativeSx)) {
                relativeSx = 0.0f;
            }
            if (Float.isNaN(relativeSy) || Float.isInfinite(relativeSy)) {
                relativeSy = 0.0f;
            }
            boolean foundX = false;
            boolean foundY = false;
            for (final float zoomPivot : mScalePivots) {
                if (zoomPivot - relativeSx > SCALE_PIVOT_EPSILON) {
                    foundX = true;
                    nextScalePivot = zoomPivot;
                }
                if (zoomPivot - relativeSy > SCALE_PIVOT_EPSILON) {
                    foundY = true;
                    nextScalePivot = zoomPivot;
                }
                if (foundX && foundY) {
                    break;
                }
            }
        }
        return nextScalePivot;
    }

    /*
     * TODO Mention how if inX or inY < 0, it's the minimum translation allowed.
     * If > 0, then it's a set translation and is not scrollable.
     */
    private void getTranslationCoefficient(Matrix matrix, PointF outPoint) {
        // TODO Drawable has intrinsic size?
        final Rect contentRect = getContentRect();
        final RectF mappedImageRect = mPoolManager.acquireRectF();
        getMappedImageRect(matrix, mappedImageRect);
        outPoint.set(
                contentRect.width() - mappedImageRect.width(),
                contentRect.height() - mappedImageRect.height());
        if (outPoint.x > 0.0f) {
            // The mapped image is narrower than the content area
            switch (mScaleType) {
                case FIT_START:
                    if (ViewCompat.getLayoutDirection(this) !=
                            ViewCompat.LAYOUT_DIRECTION_RTL) {
                        outPoint.x = 0.0f;
                    }
                    break;
                case FIT_END:
                    if (ViewCompat.getLayoutDirection(this) ==
                            ViewCompat.LAYOUT_DIRECTION_RTL) {
                        outPoint.x = 0.0f;
                    }
                    break;
                default:
                    outPoint.x *= 0.5f;
            }
        }
        if (outPoint.y > 0.0f) {
            // The mapped image is shorter than the content area
            switch (mScaleType) {
                case FIT_START:
                    outPoint.y = 0.0f;
                    break;
                case FIT_END:
                    // No op
                    break;
                default:
                    outPoint.y *= 0.5f;
            }
        }
        mPoolManager.releaseRectF(mappedImageRect);
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

        setDoubleTapToScaleEnabled(a.getBoolean(
                R.styleable.InteractiveImageView_doubleTapToScaleEnabled,
                true));
        setFlingEnabled(a.getBoolean(
                R.styleable.InteractiveImageView_flingEnabled,
                true));
        setScaleEnabled(a.getBoolean(
                R.styleable.InteractiveImageView_scaleEnabled,
                true));
        final @ArrayRes int resId = a.getResourceId(
                R.styleable.InteractiveImageView_scalePivots,
                -1);
        if (resId != -1) {
            TypedArray ta = getResources().obtainTypedArray(resId);
            final int length = ta.length();
            final float[] scalePivots = new float[length];
            for (int i = 0; i < length; i++) {
                scalePivots[i] = ta.getFloat(i, Float.NaN);
            }
            setScalePivots(scalePivots);
            ta.recycle();
        }
        setScrollEnabled(a.getBoolean(
                R.styleable.InteractiveImageView_scrollEnabled,
                true));

        a.recycle();
        return new Initializer(this);
    }

    private void maybeReleaseEdgeEffects() {
        mEdgeEffectLeftActive
                = mEdgeEffectTopActive
                = mEdgeEffectRightActive
                = mEdgeEffectBottomActive
                = false;
        if (mEdgeEffectLeft != null) {
            mEdgeEffectLeft.onRelease();
            mEdgeEffectTop.onRelease();
            mEdgeEffectRight.onRelease();
            mEdgeEffectBottom.onRelease();
        }
    }

    private void matrixToTransformInfo(Matrix matrix, TransformInfo outInfo) {
        final ValuesWrapper matrixValues = mPoolManager.acquireValuesWrapper(matrix);
        outInfo.inSx = matrixValues.values[MSCALE_X];
        outInfo.inSy = matrixValues.values[MSCALE_Y];
        if (ViewCompat.isLaidOut(this)) {
            final PointF pivotPoint = mPoolManager.acquirePointF();
            final Rect contentRect = getContentRect();
            getPivotPoint(
                    contentRect.exactCenterX(),
                    contentRect.exactCenterY(),
                    matrix,
                    pivotPoint);
            outInfo.px = pivotPoint.x;
            outInfo.py = pivotPoint.y;
            mPoolManager.releasePointF(pivotPoint);
        } else {
            final Drawable d = getDrawable();
            final int intrinsicWidth = (d == null ? -1 : d.getIntrinsicWidth());
            final int intrinsicHeight = (d == null ? -1 : d.getIntrinsicHeight());
            outInfo.px = (intrinsicWidth > 0 ? intrinsicWidth * 0.5f : intrinsicWidth);
            outInfo.py = (intrinsicHeight > 0 ? intrinsicHeight * 0.5f : intrinsicHeight);
        }
        outInfo.inX = outInfo.inY = PLACE_IN_CENTER;
        mPoolManager.releaseValuesWrapper(matrixValues);
    }

    private void transformToMatrix(TransformInfo info, Matrix outMatrix) {
        outMatrix.set(getImageMatrixInternal());
        final ValuesWrapper matrixValues = mPoolManager.acquireValuesWrapper(outMatrix);

        // TODO Think about a clean way to get a "real" number
        final float sx = (Float.isNaN(info.outSx) ?
                (Float.isNaN(info.inSx) ? matrixValues.values[MSCALE_X] : info.inSx) :
                info.outSx);
        final float sy = (Float.isNaN(info.outSy) ?
                (Float.isNaN(info.inSy) ? matrixValues.values[MSCALE_Y] : info.inSy) :
                info.outSy);
        final float x = (Float.isNaN(info.outX) ?
                (Float.isNaN(info.inX) ? getContentRect().exactCenterX() : info.inX) :
                info.outX);
        final float y = (Float.isNaN(info.outY) ?
                (Float.isNaN(info.inY) ? getContentRect().exactCenterY() : info.inY) :
                info.outY);

        final float deltaSx = sx / matrixValues.values[MSCALE_X];
        final float deltaSy = sy / matrixValues.values[MSCALE_Y];
        outMatrix.preScale(deltaSx, deltaSy, info.px, info.py);

        // Get location of px/py in the view
        final PtsWrapper drawablePts = mPoolManager.acquirePtsWrapper(info.px, info.py);
        final PtsWrapper viewPts = mPoolManager.acquirePtsWrapper();
        outMatrix.mapPoints(viewPts.pts, drawablePts.pts);

        final float deltaTx = x - viewPts.getX();
        final float deltaTy = y - viewPts.getY();
        outMatrix.postTranslate(deltaTx, deltaTy);
        outMatrix.getValues(matrixValues.values);

        mPoolManager.releasePtsWrapper(viewPts);
        mPoolManager.releasePtsWrapper(drawablePts);
        mPoolManager.releaseValuesWrapper(matrixValues);
    }

    //endregion Private methods
}
