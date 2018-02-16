package com.codepunk.demo.interactiveimageview.version5;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.codepunk.demo.R;

public class InteractiveImageViewActivity extends AppCompatActivity
        implements DemoDrawerLayout.DemoDrawerLayoutListener {

    //region Fields
    private DemoDrawerLayout mDrawerLayout;
    private DemoInteractiveImageView mImageView;
    //endregion Fields

    //region Lifecycle methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interactive_image_view_v5);

        mDrawerLayout = findViewById(R.id.layout_drawer);

        mDrawerLayout.requestDisallowInterceptTouchEvent(true);
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);

        mDrawerLayout.setDemoDrawerLayoutListener(this);

        final InteractiveImageViewControlsFragment fragment =
                (InteractiveImageViewControlsFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_controls);
        mImageView = findViewById(R.id.image);
        fragment.setImageView(mImageView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_interactive_image_view, menu);
        return true;
    }
    //endregion Lifecycle methods

    //region Implemented methods
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return false;
            default:
                if (mImageView.isInteracting()) {
                    return false;
                }
        }
        return true;
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
