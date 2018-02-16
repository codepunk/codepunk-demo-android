package com.codepunk.demo.interactiveimageview.version5;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class DemoDrawerLayout extends DrawerLayout {
    public interface DemoDrawerLayoutListener {
        boolean onInterceptTouchEvent(MotionEvent ev);
    }

    //region Fields
    private DemoDrawerLayoutListener mDemoDrawerLayoutListener;
    //endregion Fields

    public DemoDrawerLayout(Context context) {
        super(context);
    }

    public DemoDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DemoDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean retVal = super.onInterceptTouchEvent(ev);
        if (mDemoDrawerLayoutListener != null) {
            retVal = retVal && mDemoDrawerLayoutListener.onInterceptTouchEvent(ev);
        }
        return retVal;
    }

    //region Methods
    public void setDemoDrawerLayoutListener(DemoDrawerLayoutListener listener) {
        mDemoDrawerLayoutListener = listener;
    }
    //endregion Methods
}
