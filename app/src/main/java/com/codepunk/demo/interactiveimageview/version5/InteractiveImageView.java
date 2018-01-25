package com.codepunk.demo.interactiveimageview.version5;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;
import android.widget.OverScroller;

import com.codepunk.demo.support.DisplayCompat;

import static com.codepunk.demo.R.attr.interactiveImageViewStyle;
import static com.codepunk.demo.R.styleable.InteractiveImageView;
import static com.codepunk.demo.R.styleable.InteractiveImageView_interactivity;

/*
 * TODO Play with cropToPadding
 */

public class InteractiveImageView extends AppCompatImageView
        implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener {

    //region Constants
    public static final String LOG_TAG = InteractiveImageView.class.getSimpleName();
    static final float MAX_SCALE_BREADTH_MULTIPLIER = 3.0f;
    static final float MAX_SCALE_LENGTH_MULTIPLIER = 5.0f;

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
    private int mInteractivity;

    private ScaleType mScaleType = super.getScaleType();

    private GestureDetectorCompat mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private OverScroller mScroller;

    private final Matrix mBaselineImageMatrix = new Matrix();
    private final PointF mMaxScale = new PointF();
    private final PointF mMinScale = new PointF();

    private int mInvalidFlags;
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mInvalidFlags |= INVALID_FLAG_DEFAULT;
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
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
        //Log.d(LOG_TAG, "onDown");
        return true;
    }

    @Override // GestureDetector.OnGestureListener
    public void onShowPress(MotionEvent e) {
        //Log.d(LOG_TAG, "onShowPress");
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
        Log.d(LOG_TAG, "onScroll");
        return false;
    }

    @Override // GestureDetector.OnGestureListener
    public void onLongPress(MotionEvent e) {
        //Log.d(LOG_TAG, "onLongPress");
    }

    @Override // GestureDetector.OnGestureListener
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d(LOG_TAG, "onFling");
        return false;
    }

    @Override // GestureDetector.OnDoubleTapListener
    public boolean onSingleTapConfirmed(MotionEvent e) {
        Log.d(LOG_TAG, "onSingleTapConfirmed");
        return false;
    }

    @Override // GestureDetector.OnDoubleTapListener
    public boolean onDoubleTap(MotionEvent e) {
        Log.d(LOG_TAG, "onDoubleTap");
        return false;
    }

    @Override // GestureDetector.OnDoubleTapListener
    public boolean onDoubleTapEvent(MotionEvent e) {
        //Log.d(LOG_TAG, "onDoubleTapEvent");
        return false;
    }

    @Override // ScaleGestureDetector.OnScaleGestureListener
    public boolean onScale(ScaleGestureDetector detector) {
        Log.d(LOG_TAG, "onScale");
        return false;
    }

    @Override // ScaleGestureDetector.OnScaleGestureListener
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        Log.d(LOG_TAG, "onScaleBegin");
        return true;
    }

    @Override // ScaleGestureDetector.OnScaleGestureListener
    public void onScaleEnd(ScaleGestureDetector detector) {
        Log.d(LOG_TAG, "onScaleEnd");
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

    public float getImageMaxScaleX() {
        getImageMaxScale(null);
        return mMaxScale.x;
    }

    public float getImageMaxScaleY() {
        getImageMaxScale(null);
        return mMaxScale.y;
    }

    public float getImageMinScaleX() {
        getImageMinScale(null);
        return mMinScale.x;
    }

    public float getImageMinScaleY() {
        getImageMinScale(null);
        return mMinScale.y;
    }

    public int getInteractivity() {
        return mInteractivity;
    }

    public void setInteractivity(int flags) {
        mInteractivity = flags;

        final boolean scaleEnabled =
                (mInteractivity & INTERACTIVITY_FLAG_SCALE) == INTERACTIVITY_FLAG_SCALE;
        if (scaleEnabled) {
            if (mScaleGestureDetector == null) {
                mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);
            }
        } else {
            mScaleGestureDetector = null;
        }

        final boolean scrollEnabled =
                (mInteractivity & INTERACTIVITY_FLAG_SCROLL) == INTERACTIVITY_FLAG_SCROLL ||
                        (mInteractivity & INTERACTIVITY_FLAG_FLING) == INTERACTIVITY_FLAG_FLING;
        if (scrollEnabled) {
            if (mScroller == null) {
                mScroller = new OverScroller(getContext());
            }
        } else {
            mScroller = null;
        }

        final boolean doubleTapEnabled =
                (mInteractivity & INTERACTIVITY_FLAG_DOUBLE_TAP) == INTERACTIVITY_FLAG_DOUBLE_TAP;
        if (scrollEnabled || doubleTapEnabled) {
            if (mGestureDetector == null) {
                mGestureDetector = new GestureDetectorCompat(getContext(), this);
            }
            mGestureDetector.setIsLongpressEnabled(false);
            mGestureDetector.setOnDoubleTapListener(doubleTapEnabled ? this : null);
        } else {
            mGestureDetector = null;
        }
    }
    //endregion Methods

    //region Protected methods
    protected boolean drawableHasIntrinsicSize() {
        return getDrawableIntrinsicWidth() > 0 && getDrawableIntrinsicHeight() > 0;
    }

    @SuppressWarnings("SameParameterValue ")
    protected void getBaselineImageMatrix(Matrix outMatrix) {
        getBaselineImageMatrix(mScaleType, outMatrix);
    }

    @SuppressWarnings("SpellCheckingInspection")
    protected void getBaselineImageMatrix(ScaleType scaleType, Matrix outMatrix) {
        if ((mInvalidFlags & INVALID_FLAG_BASELINE_IMAGE_MATRIX) ==
                INVALID_FLAG_BASELINE_IMAGE_MATRIX) {
            synchronized (mBaselineImageMatrix) {
                mInvalidFlags &= ~INVALID_FLAG_BASELINE_IMAGE_MATRIX;
                if (drawableHasIntrinsicSize()) {
                    // We need to do the scaling ourselves.
                    final int dwidth = getDrawableIntrinsicWidth();
                    final int dheight = getDrawableIntrinsicHeight();

                    final int vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
                    final int vheight = getHeight() - getPaddingTop() - getPaddingBottom();

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
                        mBaselineImageMatrix.setRectToRect(
                                new RectF(0, 0, dwidth, dheight),
                                new RectF(0, 0, vwidth, vheight),
                                scaleTypeToScaleToFit(scaleType));
                    }
                } else {
                    mBaselineImageMatrix.reset();
                }
            }
        }

        if (outMatrix != null && outMatrix != mBaselineImageMatrix) {
            outMatrix.set(mBaselineImageMatrix);
        }
    }

    @SuppressWarnings("SameParameterValue ")
    protected void getImageMaxScale(PointF outPoint) {
        if ((mInvalidFlags & INVALID_FLAG_IMAGE_MAX_SCALE) == INVALID_FLAG_IMAGE_MAX_SCALE) {
            synchronized (mMaxScale) {
                mInvalidFlags &= ~INVALID_FLAG_IMAGE_MAX_SCALE;
                if (drawableHasIntrinsicSize()) {
                    final RectF src = new RectF(
                            0,
                            0,
                            getDrawableIntrinsicWidth(),
                            getDrawableIntrinsicHeight());
                    final RectF dst = new RectF();
                    getBaselineImageMatrix(null);
                    final float[] values = new float[9];
                    mBaselineImageMatrix.getValues(values);
                    mBaselineImageMatrix.mapRect(dst, src);
                    final float baselineWidth = dst.width();
                    final float baselineHeight = dst.height();
                    final float baselineBreadth = Math.min(baselineWidth, baselineHeight);
                    final float baselineLength = Math.max(baselineWidth, baselineHeight);

                    final DisplayMetrics dm = getRealMetrics(getContext());
                    final int min = Math.min(dm.widthPixels, dm.heightPixels);
                    final int max = Math.max(dm.widthPixels, dm.heightPixels);
                    final float maxBreadth = MAX_SCALE_BREADTH_MULTIPLIER * min;
                    final float maxLength = MAX_SCALE_LENGTH_MULTIPLIER * max;
                    final float screenBasedScale = Math.min(
                            maxBreadth / baselineBreadth,
                            maxLength / baselineLength);
                    final int availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
                    final int availableHeight = getHeight() - getPaddingTop() - getPaddingBottom();
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
                    mMaxScale.set(scale * values[Matrix.MSCALE_X], scale * values[Matrix.MSCALE_Y]);
                } else {
                    mMaxScale.set(1.0f, 1.0f);
                }
            }
        }

        if (outPoint != null && outPoint != mMaxScale) {
            outPoint.set(mMaxScale);
        }
    }

    @SuppressWarnings("SameParameterValue ")
    protected void getImageMinScale(PointF outPoint) {
        if ((mInvalidFlags & INVALID_FLAG_IMAGE_MIN_SCALE) == INVALID_FLAG_IMAGE_MIN_SCALE) {
            synchronized (mMinScale) {
                mInvalidFlags &= ~INVALID_FLAG_IMAGE_MIN_SCALE;
                getBaselineImageMatrix(null);
                final float[] values = new float[9];
                mBaselineImageMatrix.getValues(values);
                mMinScale.set(values[Matrix.MSCALE_X], values[Matrix.MSCALE_Y]);
            }
        }

        if (outPoint != null && outPoint != mMinScale) {
            outPoint.set(mMinScale);
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
    //endregion Protected methods

    //region Private methods
    private static DisplayMetrics getRealMetrics(Context context) {
        final WindowManager windowManager =
                ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
        if (windowManager == null) {
            return context.getResources().getDisplayMetrics();
        } else {
            final DisplayMetrics dm = new DisplayMetrics();
            DisplayCompat.getRealMetrics(windowManager.getDefaultDisplay(), dm);
            return dm;
        }
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

        setInteractivity(a.getInt(InteractiveImageView_interactivity, INTERACTIVITY_FLAG_ALL));

        a.recycle();
    }
    //endregion Private methods
}
