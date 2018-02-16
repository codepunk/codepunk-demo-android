package com.codepunk.demo.interactiveimageview.version5;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class DemoInteractiveImageView extends InteractiveImageView {
    //region Nested classes
    public interface DemoInteractiveImageViewListener {
        void onDraw(InteractiveImageView view, Canvas canvas);
        void onInteractionBegin(InteractiveImageView view);
        void onInteractionEnd(InteractiveImageView view);
    }
    //endregion Nested classes

    //region Constants
    public static final String LOG_TAG = DemoInteractiveImageView.class.getSimpleName();
    //endregion Constants

    //region Fields
    private DemoInteractiveImageViewListener mDemoInteractiveImageViewListener;
    private boolean mInteracting = false;
    //endregion Fields

    //region Constructors
    public DemoInteractiveImageView(Context context) {
        super(context);
    }

    public DemoInteractiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DemoInteractiveImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
    public void onLongPress(MotionEvent e) {
        super.onLongPress(e);
        if (!mInteracting) {
            onInteractionBegin();
        }
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        final boolean retVal = super.onScaleBegin(detector);
        if (!mInteracting) {
            onInteractionBegin();
        }
        return retVal;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        final boolean retVal = super.onScroll(e1, e2, distanceX, distanceY);
        if (!mInteracting) {
            onInteractionBegin();
        }
        return retVal;
    }

    @Override
    public boolean onUp(MotionEvent e) {
        final boolean retVal = super.onUp(e);
        // TODO At end of fling?
        onInteractionEnd();
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
