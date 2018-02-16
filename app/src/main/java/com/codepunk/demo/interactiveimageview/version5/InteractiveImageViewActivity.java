package com.codepunk.demo.interactiveimageview.version5;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.codepunk.demo.R;

public class InteractiveImageViewActivity extends AppCompatActivity
        implements DemoDrawerLayout.DemoDrawerLayoutListener,
        DemoInteractiveImageView.SingleTapConfirmedListener {
    //region Constants
    private static final String LOG_TAG = InteractiveImageViewActivity.class.getSimpleName();
    //endregion Constants

    //region Fields
    private DemoDrawerLayout mDrawerLayout;
    private View mControlsDrawer;
    private DemoInteractiveImageView mImageView;
    private final Rect mHitRect = new Rect();
    //endregion Fields

    //region Lifecycle methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interactive_image_view_v5);

        mDrawerLayout = findViewById(R.id.layout_drawer);
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);
        mDrawerLayout.setDemoDrawerLayoutListener(this);

        mControlsDrawer = findViewById(R.id.layout_controls);

        final InteractiveImageViewControlsFragment fragment =
                (InteractiveImageViewControlsFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_controls);
        mImageView = findViewById(R.id.image);
        mImageView.setSingleTapConfirmedListener(this);
        fragment.setImageView(mImageView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_interactive_image_view, menu);
        return true;
    }
    //endregion Lifecycle methods

    //region Implemented methods
    @Override // DemoDrawerLayout.DemoDrawerLayoutListener
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mControlsDrawer.getHitRect(mHitRect);
        return mHitRect.contains((int) ev.getRawX(), (int) ev.getRawY()) &&
                !mImageView.isInteracting();
    }

    @Override // DemoInteractiveImageView.SingleTapConfirmedListener
    public void onSingleTapConfirmed(DemoInteractiveImageView view) {
        if (mDrawerLayout.isDrawerOpen(Gravity.END)) {
            mDrawerLayout.closeDrawer(Gravity.END);
        }
    }
    //endregion Implemented methods

    //region Methods
    public void onControlsClick(MenuItem item) {
        if (mDrawerLayout.isDrawerOpen(Gravity.END)) {
            mDrawerLayout.closeDrawer(Gravity.END);
        } else {
            mDrawerLayout.openDrawer(Gravity.END);
        }
    }
    //endregion Methods
}
