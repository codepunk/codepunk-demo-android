package com.codepunk.demo.interactiveimageview.version5;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class DemoInteractiveImageView extends InteractiveImageView {
    //region Nested classes
    public interface DemoInteractiveImageViewListener {
        void onDraw(InteractiveImageView view, Canvas canvas);
        void onInteractionBegin(InteractiveImageView view);
        void onInteractionEnd(InteractiveImageView view);
        void onSetImageResource(InteractiveImageView view, int resId);
    }
    //endregion Nested classes

    //region Constants
    public static final String LOG_TAG = DemoInteractiveImageView.class.getSimpleName();
    //endregion Constants

    //region Fields
    private int mLastResId;
    private DemoInteractiveImageViewListener mDemoInteractiveImageViewListener;
    private boolean mInteracting = false;
    //endregion Fields

    //region Constructors
    public DemoInteractiveImageView(Context context) {
        super(context);
        initDemoInteractiveImageView(context, null);
    }

    public DemoInteractiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDemoInteractiveImageView(context, attrs);
    }

    public DemoInteractiveImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initDemoInteractiveImageView(context, attrs);
    }
    //endregion Constructors

    //region Inherited methods
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d(LOG_TAG, "SASTEST: onDraw");
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

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        mLastResId = resId;
        if (mDemoInteractiveImageViewListener != null) {
            mDemoInteractiveImageViewListener.onSetImageResource(this, mLastResId);
        }
    }
    //endregion Inherited methods

    //region Methods
    private void initDemoInteractiveImageView(
            Context context,
            AttributeSet attrs) {
        if (attrs != null) {
            try {
                final String src = getResources().getResourceEntryName(android.R.attr.src);
                final int count = attrs.getAttributeCount();
                for (int i = 0; i < count; i++) {
                    final String name = attrs.getAttributeName(i);
                    if (src.equals(name)) {
                        mLastResId = attrs.getAttributeResourceValue(i, 0);
                        if (mDemoInteractiveImageViewListener != null) {
                            mDemoInteractiveImageViewListener.onSetImageResource(this, mLastResId);
                        }
                        break;
                    }
                }
            } catch (Resources.NotFoundException e) {
                // No-op
            }
        }
    }

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

    public void setDemoInteractiveImageViewListener(DemoInteractiveImageViewListener listener) {
        mDemoInteractiveImageViewListener = listener;
        if (mDemoInteractiveImageViewListener != null) {
            mDemoInteractiveImageViewListener.onSetImageResource(this, mLastResId);
        }
    }
    //endregion Methods
}
