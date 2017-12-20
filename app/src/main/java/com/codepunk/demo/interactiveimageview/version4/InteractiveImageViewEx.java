package com.codepunk.demo.interactiveimageview.version4;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

public class InteractiveImageViewEx extends InteractiveImageView {
    //region Nested classes
    public interface OnDrawListener {
        void onDraw(InteractiveImageView view, Canvas canvas);
    }
    //endregion Nested classes

    //region Fields
    private OnDrawListener mOnDrawListener;
    //endregion Fields

    //region Constructors
    public InteractiveImageViewEx(Context context) {
        super(context);
    }

    public InteractiveImageViewEx(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InteractiveImageViewEx(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    //endregion Constructors

    //region Inherited methods

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mOnDrawListener != null) {
            mOnDrawListener.onDraw(this, canvas);
        }
    }
    //endregion Inherited methods

    //region Methods
    public void setOnDrawListener(OnDrawListener onDrawListener) {
        mOnDrawListener = onDrawListener;
    }
    //endregion Methods
}
