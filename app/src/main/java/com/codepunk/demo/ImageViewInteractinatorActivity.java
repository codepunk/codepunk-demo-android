package com.codepunk.demo;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

public class ImageViewInteractinatorActivity extends AppCompatActivity
        implements DemoDrawerLayout.DemoDrawerLayoutListener,
        View.OnClickListener {
    //region Constants

    private static final String LOG_TAG = ImageViewInteractinatorActivity.class.getSimpleName();

    //endregion Constants

    //region Fields

    private DemoDrawerLayout mDrawerLayout;
    private View mControlsDrawer;
    private DemoImageViewInteractinator mImageView;
    private final Rect mHitRect = new Rect();

    //endregion Fields

    //region Lifecycle methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view_interactinator);

        mDrawerLayout = findViewById(R.id.layout_drawer);
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);
        mDrawerLayout.setDemoDrawerLayoutListener(this);

        mControlsDrawer = findViewById(R.id.layout_controls);

        final InteractinatorControlsFragment fragment =
                (InteractinatorControlsFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_controls);
        mImageView = findViewById(R.id.image);
        mImageView.setOnClickListener(this);
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

    @Override // View.OnClickListener
    public void onClick(View v) {
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
