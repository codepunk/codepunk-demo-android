package com.codepunk.demo.interactiveimageview.version5;

import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import com.codepunk.demo.R;

public class InteractiveImageViewActivity extends AppCompatActivity
        implements DemoInteractiveImageView.OnDrawListener {

    //region Fields
    private DrawerLayout mDrawerLayout;
    private DemoInteractiveImageView mImageView;
    private InteractiveImageViewControlPanelView mControlPanelView;
    //endregion Fields

    //region Lifecycle methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interactive_image_view_v5);

        mDrawerLayout = findViewById(R.id.layout_drawer);
        mControlPanelView = findViewById(R.id.layout_controls);
        mImageView = findViewById(R.id.image);

        mDrawerLayout.requestDisallowInterceptTouchEvent(true);
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);
        mImageView.setOnDrawListener(this);

        mControlPanelView.setImageView(mImageView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_interactive_image_view, menu);
        return true;
    }
    //endregion Lifecycle methods

    //region Implemented methods
    @Override
    public void onDraw(InteractiveImageView view, Canvas canvas) {

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
