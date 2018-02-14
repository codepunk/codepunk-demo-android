package com.codepunk.demo.interactiveimageview.version5;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import com.codepunk.demo.R;

public class InteractiveImageViewActivity extends AppCompatActivity {

    //region Fields
    private DrawerLayout mDrawerLayout;
    //endregion Fields

    //region Lifecycle methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interactive_image_view_v5);

        mDrawerLayout = findViewById(R.id.layout_drawer);

        mDrawerLayout.requestDisallowInterceptTouchEvent(true);
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);

        final InteractiveImageViewControlsFragment fragment =
                (InteractiveImageViewControlsFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_controls);
        final DemoInteractiveImageView imageView = findViewById(R.id.image);
        fragment.setImageView(imageView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_interactive_image_view, menu);
        return true;
    }
    //endregion Lifecycle methods

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
