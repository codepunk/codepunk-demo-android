package com.codepunk.demo.interactiveimageview2;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ArrayRes;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.WindowManager;
import android.widget.OverScroller;

import com.codepunk.demo.R;
import com.codepunk.demo.support.DisplayCompat;

public class InteractiveImageView extends AppCompatImageView {

    //region Nested classes

    private interface ImageViewCompatImpl {
        boolean getCropToPadding();
        void setCropToPadding(boolean cropToPadding);
    }

    private static class BaseImageViewCompatImpl implements ImageViewCompatImpl {
        private final InteractiveImageView mView;
        private boolean mCropToPadding;

        public BaseImageViewCompatImpl(InteractiveImageView view) {
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
        private final InteractiveImageView mView;

        public JBImageViewCompatImpl(InteractiveImageView view) {
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

    private static class Initializer {
        final ImageViewCompatImpl impl;
        final GestureDetectorCompat gestureDetector;
        final ScaleGestureDetector scaleGestureDetector;
        final OverScroller overScroller;
        final Transformer transformer;

        Initializer(
                InteractiveImageView view,
                OnGestureListener onGestureListener,
                OnScaleGestureListener onScaleGestureListener) {
            final Context context = view.getContext();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                impl = new JBImageViewCompatImpl(view);
            } else {
                impl = new BaseImageViewCompatImpl(view);
            }
            gestureDetector = new GestureDetectorCompat(context, onGestureListener);
            gestureDetector.setIsLongpressEnabled(false);
            gestureDetector.setOnDoubleTapListener(onGestureListener);
            scaleGestureDetector = new ScaleGestureDetector(context, onScaleGestureListener);
            overScroller = new OverScroller(context);
            transformer = new Transformer(context);
        }
    }

    private class OnGestureListener extends SimpleOnGestureListener {

    }

    private class OnScaleGestureListener extends SimpleOnScaleGestureListener {

    }

    private static class Transformer {
        final Context mContext;

        public Transformer(Context context) {
            mContext = context;
        }
    }

    //endregion Nested classes

    //region Constants

    private static final String LOG_TAG = InteractiveImageView.class.getSimpleName();

    private static final float ALMOST_EQUALS_THRESHOLD = 8;

    private static final float MAX_SCALE_BREADTH_MULTIPLIER = 4.0f;
    private static final float MAX_SCALE_LENGTH_MULTIPLIER = 6.0f;

    private static final int INVALID_FLAG_BASELINE_IMAGE_MATRIX = 0x00000001;
    private static final int INVALID_FLAG_IMAGE_MAX_SCALE = 0x00000002;
    private static final int INVALID_FLAG_IMAGE_MIN_SCALE = 0x00000004;
    private static final int INVALID_FLAG_DEFAULT =
            INVALID_FLAG_BASELINE_IMAGE_MATRIX |
                    INVALID_FLAG_IMAGE_MAX_SCALE |
                    INVALID_FLAG_IMAGE_MIN_SCALE;

    public static final float TRANSFORM_TO_CENTER = Float.NaN;

    //endregion Constants

    //region Fields

    private final ImageViewCompatImpl mImpl;
    private final GestureDetectorCompat mGestureDetector;
    private final ScaleGestureDetector mScaleGestureDetector;
    private final OverScroller mOverScroller;
    private final Transformer mTransformer;

    protected boolean mDoubleTapToScaleEnabled;
    protected boolean mFlingEnabled;
    protected boolean mScaleEnabled;
    protected boolean mScrollEnabled;
    private float[] mScalePresets;

    private ScaleType mScaleType;
    private int mInvalidFlags;

    private final Matrix mBaselineImageMatrix = new Matrix();
    private final Matrix mImageMatrix = new Matrix();
    private final PointF mImageMaxScale = new PointF();
    private final PointF mImageMinScale = new PointF();

    // Avoid allocations...
    private float[] mPts = new float[2];
    private float[] mValues = new float[9];
    private Matrix mTempMatrix = new Matrix();
    private PointF mTempPoint = new PointF();
    private RectF mTempRectSrc = new RectF();
    private RectF mTempRectDst = new RectF();

    //endregion Fields

    //region Constructors

    public InteractiveImageView(Context context) {
        super(context);
        final Initializer initializer = initImageView(
                context,
                null,
                R.attr.interactiveImageViewStyle,
                0);
        mImpl = initializer.impl;
        mGestureDetector = initializer.gestureDetector;
        mScaleGestureDetector = initializer.scaleGestureDetector;
        mOverScroller = initializer.overScroller;
        mTransformer = initializer.transformer;
    }

    public InteractiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final Initializer initializer = initImageView(
                context,
                attrs,
                R.attr.interactiveImageViewStyle,
                0);
        mImpl = initializer.impl;
        mGestureDetector = initializer.gestureDetector;
        mScaleGestureDetector = initializer.scaleGestureDetector;
        mOverScroller = initializer.overScroller;
        mTransformer = initializer.transformer;
    }

    public InteractiveImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final Initializer initializer =
                initImageView(context, attrs, defStyleAttr, 0);
        mImpl = initializer.impl;
        mGestureDetector = initializer.gestureDetector;
        mScaleGestureDetector = initializer.scaleGestureDetector;
        mOverScroller = initializer.overScroller;
        mTransformer = initializer.transformer;
    }

    //endregion Constructors

    //region Inherited methods

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mInvalidFlags |= INVALID_FLAG_DEFAULT;
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
    }

    public void setFlingEnabled(boolean flingEnabled) {
        mFlingEnabled = flingEnabled;
    }

    public void setScaleEnabled(boolean scaleEnabled) {
        mScaleEnabled = scaleEnabled;
    }

    public void setScalePresets(float[] scalePresets) {
        mScalePresets = scalePresets;
    }

    public void setScrollEnabled(boolean scrollEnabled) {
        mScrollEnabled = scrollEnabled;
    }

    public void transformBy(
            float imagePivotX,
            float imagePivotY,
            float scaleXBy,
            float scaleYBy,
            float xBy,
            float yBy) {
        getImageScale(mTempPoint);
        final float scaleX = mTempPoint.x * scaleXBy;
        final float scaleY = mTempPoint.y * scaleYBy;
        imagePointToViewPoint(getImageMatrixInternal(), mTempPoint);
        final float x = mTempPoint.x + xBy;
        final float y = mTempPoint.y + yBy;
        transformTo(imagePivotX, imagePivotY, scaleX, scaleY, x, y);
    }

    public void transformTo(
            float imagePivotX,
            float imagePivotY,
            float imageScaleX,
            float imageScaleY) {
        transformTo(
                imagePivotX,
                imagePivotY,
                imageScaleX,
                imageScaleY,
                TRANSFORM_TO_CENTER,
                TRANSFORM_TO_CENTER);
    }

    public void transformTo(
            float imagePivotX,
            float imagePivotY,
            float imageScaleX,
            float imageScaleY,
            float x,
            float y) {
        // TODO This version will clamp automatically
    }

    //endregion Methods

    //region Protected methods

    protected boolean clampTransform(
            float imagePivotX,
            float imagePivotY,
            float imageScaleX,
            float imageScaleY,
            float x,
            float y,
            PointF outScale,
            PointF outPoint) {
        // Clamp scale
        outScale.set(
                MathUtils.clamp(imageScaleX, getImageMinScaleX(), getImageMaxScaleX()),
                MathUtils.clamp(imageScaleY, getImageMinScaleY(), getImageMaxScaleY()));

        // Clamp view point
        mImageMatrix.set(getImageMatrixInternal());
        mImageMatrix.getValues(mValues);

        // Pre-scale the matrix to the clamped scale
        if (Float.compare(outScale.x, mValues[Matrix.MSCALE_X]) != 0 ||
                Float.compare(outScale.y, mValues[Matrix.MSCALE_Y]) != 0) {
            final float deltaSx = outScale.x / mValues[Matrix.MSCALE_X];
            final float deltaSy = outScale.y / mValues[Matrix.MSCALE_Y];
            mImageMatrix.preScale(deltaSx, deltaSy);
            mImageMatrix.getValues(mValues);
        }

        mTempPoint.set(imagePivotX, imagePivotY);
        imagePointToViewPoint(mImageMatrix, mTempPoint);
        final float mappedPx = mTempPoint.x - mValues[Matrix.MTRANS_X];
        final float mappedPy = mTempPoint.y - mValues[Matrix.MTRANS_Y];

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

        return (!almostEquals(imageScaleX, outScale.x) ||
                !almostEquals(imageScaleY, outScale.y) ||
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

        final int vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
        final int vheight = getHeight() - getPaddingTop() - getPaddingBottom();

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
                baselineMatrix.getValues(mValues);

                final Drawable d = getDrawable();
                mTempRectSrc.set(
                        0.0f,
                        0.0f,
                        Math.max(d.getIntrinsicWidth(), 0),
                        Math.max(d.getIntrinsicHeight(), 0));
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
                    availableSize = getWidth() - getPaddingLeft() - getPaddingRight();
                } else if (baselineWidth > baselineHeight) {
                    availableSize = getHeight() - getPaddingTop() - getPaddingBottom();
                } else {
                    availableSize = Math.min(
                            getWidth() - getPaddingLeft() - getPaddingRight(),
                            getHeight() - getPaddingTop() - getPaddingBottom());
                }
                final float viewBasedScale = availableSize / baselineBreadth;
                final float scale = Math.max(screenBasedScale, viewBasedScale);
                mImageMaxScale.set(
                        scale * mValues[Matrix.MSCALE_X],
                        scale * mValues[Matrix.MSCALE_Y]);
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
                getBaselineImageMatrix().getValues(mValues);
                mImageMinScale.set(mValues[Matrix.MSCALE_X], mValues[Matrix.MSCALE_Y]);
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
        mTempPoint.set(
                (getPaddingLeft() + getWidth() - getPaddingRight()) * 0.5f,
                (getPaddingTop() + getHeight() - getPaddingBottom()) * 0.5f);
        viewPointToImagePoint(matrix, outPoint);
    }

    protected void getImageScale(PointF outPoint) {
        getImageMatrixInternal().getValues(mValues);
        outPoint.set(mValues[Matrix.MSCALE_X], mValues[Matrix.MSCALE_Y]);
    }

    protected void imagePointToViewPoint(Matrix matrix, PointF point) {
        mPts[0] = point.x;
        mPts[1] = point.y;
        matrix.mapPoints(mPts);
        if (getCompatCropToPadding()) {
            mPts[0] += getPaddingLeft();
            mPts[1] += getPaddingTop();
        }
        point.set(mPts[0], mPts[1]);
    }

    protected void viewPointToImagePoint(Matrix matrix, PointF point) {
        mPts[0] = point.x;
        mPts[1] = point.y;
        if (getCompatCropToPadding()) {
            mPts[0] -= getPaddingLeft();
            mPts[1] -= getPaddingTop();
        }
        matrix.invert(mTempMatrix);
        mTempMatrix.mapPoints(mPts);
        point.set(mPts[0], mPts[1]);
    }

    //endregion Protected methods

    //region Private methods

    private boolean almostEquals(float a, float b) {
        final int diff = Math.abs(Float.floatToIntBits(a) - Float.floatToIntBits(b));
        return diff <= ALMOST_EQUALS_THRESHOLD;
    }

    /*
     * TODO Mention how if inX or inY < 0, it's the minimum translation allowed.
     * If > 0, then it's a set translation and is not scrollable.
     */
    private void getTranslationCoefficient(Matrix matrix, PointF outPoint) {
        if (drawableHasFunctionalDimensions()) {
            final Drawable d = getDrawable();
            mTempRectSrc.set(
                    0.0f,
                    0.0f,
                    Math.max(d.getIntrinsicWidth(), 0),
                    Math.max(d.getIntrinsicHeight(), 0));
            matrix.mapRect(mTempRectDst, mTempRectSrc);
            final float xDiff =
                    mTempRectSrc.width() - (getWidth() - getPaddingLeft() - getPaddingRight());
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
            final float yDiff =
                    mTempRectSrc.height() - (getHeight() - getPaddingTop() - getPaddingBottom());
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
    private Initializer initImageView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        final Initializer initializer =
                new Initializer(
                        this,
                        new OnGestureListener(),
                        new OnScaleGestureListener());

        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.InteractiveImageView,
                defStyleAttr,
                defStyleRes);

        initializer.impl.setCropToPadding(a.getBoolean(
                R.styleable.InteractiveImageView_compatCropToPadding,
                false));
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
                R.styleable.InteractiveImageView_scalePresets,
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
                R.styleable.InteractiveImageView_scrollEnabled,
                true));

        a.recycle();

        return initializer;
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

    //endregion Private methods
}
