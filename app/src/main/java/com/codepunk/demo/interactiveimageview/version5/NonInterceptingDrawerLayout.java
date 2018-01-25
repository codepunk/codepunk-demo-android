package com.codepunk.demo.interactiveimageview.version5;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class NonInterceptingDrawerLayout extends DrawerLayout {
    public NonInterceptingDrawerLayout(Context context) {
        super(context);
    }

    public NonInterceptingDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NonInterceptingDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final boolean retVal = super.onInterceptTouchEvent(ev);
        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                return false;
            }
        }
        return retVal;
    }
}
