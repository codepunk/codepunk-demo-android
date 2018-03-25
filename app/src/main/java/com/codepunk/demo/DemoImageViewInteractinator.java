package com.codepunk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;

import com.codepunk.demo.widget.ImageViewInteractinator;

public class DemoImageViewInteractinator extends ImageViewInteractinator {

    //region Nested classes

    public interface DemoInteractiveImageViewListener {
        void onDraw(ImageViewInteractinator view, Canvas canvas);
        void onInteractionBegin(ImageViewInteractinator view);
        void onInteractionEnd(ImageViewInteractinator view);
    }

    //endregion Nested classes

    //region Constants
    public static final String LOG_TAG = DemoImageViewInteractinator.class.getSimpleName();
    //endregion Constants

    //region Fields

    private DemoInteractiveImageViewListener mDemoInteractiveImageViewListener;
    private boolean mInteracting = false;

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;

    private final OnGestureListener mOnGestureListener =
            new SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onScroll(
                        MotionEvent e1,
                        MotionEvent e2,
                        float distanceX,
                        float distanceY) {
                    if (!mInteracting) {
                        onInteractionBegin();
                    }
                    return true;
                }
            };

    private final OnScaleGestureListener mOnScaleGestureListener =
            new SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    if (!mInteracting) {
                        onInteractionBegin();
                    }
                    return true;
                }
            };

    //endregion Fields

    //region Constructors

    public DemoImageViewInteractinator(Context context) {
        super(context);
        mGestureDetector = new GestureDetector(context, mOnGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(context, mOnScaleGestureListener);
    }

    public DemoImageViewInteractinator(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(context, mOnGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(context, mOnScaleGestureListener);
    }

    public DemoImageViewInteractinator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mGestureDetector = new GestureDetector(context, mOnGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(context, mOnScaleGestureListener);
    }

    //endregion Constructors

    //region Inherited methods

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDemoInteractiveImageViewListener != null) {
            mDemoInteractiveImageViewListener.onDraw(this, canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean retVal = super.onTouchEvent(event);
        retVal = mGestureDetector.onTouchEvent(event) || retVal;
        retVal = mScaleGestureDetector.onTouchEvent(event) || retVal;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            onInteractionEnd();
        }
        return retVal;
    }

    //endregion Inherited methods

    //region Methods

    public boolean isInteracting() {
        return mInteracting;
    }

    public void setDemoInteractiveImageViewListener(DemoInteractiveImageViewListener listener) {
        mDemoInteractiveImageViewListener = listener;
    }

    //endregion Methods

    //region Private methods

    private void onInteractionBegin() {
        mInteracting = true;
        if (mDemoInteractiveImageViewListener != null) {
            mDemoInteractiveImageViewListener.onInteractionBegin(this);
        }
    }

    private void onInteractionEnd() {
        mInteracting = false;
        if (mDemoInteractiveImageViewListener != null) {
            mDemoInteractiveImageViewListener.onInteractionEnd(this);
        }
    }

    //endregion Private methods
}
