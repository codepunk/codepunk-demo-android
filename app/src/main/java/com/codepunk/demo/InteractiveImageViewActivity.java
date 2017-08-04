package com.codepunk.demo;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintLayout.LayoutParams;
import android.support.constraint.Guideline;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.ImageView;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.os.Build.VERSION_CODES.HONEYCOMB;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class InteractiveImageViewActivity extends AppCompatActivity {

    //region Nested classes
    private interface GuidelineAnimateCompatImpl {
        void animate(final float toValue);
    }

    private class BaseGuidelineAnimateCompatImpl implements GuidelineAnimateCompatImpl {
        private Animation mAnimation;

        @Override
        public void animate(final float toValue) {
            if (mAnimation != null) {
                mAnimation.cancel();
                mGuideline.clearAnimation();
                mAnimation = null;
            }
            final LayoutParams lp = (LayoutParams) mGuideline.getLayoutParams();
            final float fraction = Math.abs(lp.guidePercent - toValue) / (HIDDEN_PCT - SHOWN_PCT);
            final float fromValue = lp.guidePercent;
            mAnimation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    lp.guidePercent = fromValue + (toValue - fromValue) * interpolatedTime;
                    mGuideline.setLayoutParams(lp);
                }
            };
            mAnimation.setDuration((int) (mDuration * fraction));
            mAnimation.setInterpolator(mInterpolator);
            mLayoutView.startAnimation(mAnimation);
        }
    }

    @TargetApi(HONEYCOMB)
    private class HoneyCombGuidelineAnimateCompatImpl
            implements GuidelineAnimateCompatImpl {
        private ValueAnimator mAnimator;

        @Override
        public void animate(final float toValue) {
            if (mAnimator != null) {
                mAnimator.cancel();
            }
            final LayoutParams lp = (LayoutParams) mGuideline.getLayoutParams();
            final float fraction = Math.abs(lp.guidePercent - toValue) / (HIDDEN_PCT - SHOWN_PCT);
            mAnimator = ValueAnimator.ofFloat(lp.guidePercent, toValue);
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    lp.guidePercent = (Float) animation.getAnimatedValue();
                    mGuideline.setLayoutParams(lp);
                }
            });
            mAnimator.setDuration((int) (mDuration * fraction));
            mAnimator.setInterpolator(mInterpolator);
            mAnimator.start();
        }
    }
    //endregion Nested classes

    //region Constants
    private static final float SHOWN_PCT = 2.0f / 3;
    private static final float HIDDEN_PCT = 1.0f;
    private static final String KEY_SHOWING_CONTROLS = InteractiveImageViewActivity.class.getName() + ".showingControls";
    //endregion Constants

    //region Fields
    private Interpolator mInterpolator = new DecelerateInterpolator();

    private Guideline mGuideline;
    private ConstraintLayout mLayoutView;
    private ImageView mImageView;
    private View mControlsView;
    private boolean mShowingControls = true;

    private int mDuration;

    private GuidelineAnimateCompatImpl mGuidelineAnimateCompatImpl;
    //endregion Fields

    //region Lifecycle methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interactive_image_view);
        mLayoutView = (ConstraintLayout) findViewById(R.id.layout_view);
        mGuideline = (Guideline) findViewById(R.id.guideline);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mControlsView = findViewById(R.id.controls_view);
        mDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mShowingControls = savedInstanceState == null ||
                savedInstanceState.getBoolean(KEY_SHOWING_CONTROLS, false);

        final OnGlobalLayoutListener listener = new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewTreeObserverCompat.removeOnGlobalLayoutListener(
                        mLayoutView.getViewTreeObserver(), this);
                LayoutParams lp = (LayoutParams) mControlsView.getLayoutParams();
                if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE) {
                    final float guidelineX = ViewCompat.getX(mGuideline);
                    final int guidelineWidth = mGuideline.getWidth();
                    lp.width = mLayoutView.getWidth() - (int) (guidelineX + guidelineWidth);
                } else {
                    final float guidelineY = ViewCompat.getY(mGuideline);
                    final int guidelineHeight = mGuideline.getHeight();
                    lp.height = mLayoutView.getHeight() - (int) (guidelineY + guidelineHeight);
                }
                mControlsView.setLayoutParams(lp);
                if (!mShowingControls) {
                    hideControls(false);
                }
            }
        };
        mLayoutView.getViewTreeObserver().addOnGlobalLayoutListener(listener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_interactive_image_view, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SHOWING_CONTROLS, mShowingControls);
    }
    //endregion Lifecycle methods

    //region Methods
    public void onControlsClick(MenuItem item) {
        if (mShowingControls) {
            hideControls(true);
        } else {
            showControls(true);
        }
    }

    public void showControls(boolean animate) {
        if (!mShowingControls || !animate) {
            mShowingControls = true;
            showOrHideControls(SHOWN_PCT, animate);
        }
    }

    public void hideControls(boolean animate) {
        if (mShowingControls || !animate) {
            mShowingControls = false;
            showOrHideControls(HIDDEN_PCT, animate);
        }
    }
    //endregion Methods

    //region Private methods
    private void showOrHideControls(float toValue, boolean animate) {
        if (animate) {
            getGuidelineAnimateCompatImpl().animate(toValue);
        } else {
            final LayoutParams lp = (LayoutParams) mGuideline.getLayoutParams();
            lp.guidePercent = toValue;
            mGuideline.setLayoutParams(lp);
        }
    }

    private GuidelineAnimateCompatImpl getGuidelineAnimateCompatImpl() {
        if (mGuidelineAnimateCompatImpl == null) {
            if (Build.VERSION.SDK_INT >= HONEYCOMB) {
                mGuidelineAnimateCompatImpl = new HoneyCombGuidelineAnimateCompatImpl();
            } else {
                mGuidelineAnimateCompatImpl = new BaseGuidelineAnimateCompatImpl();
            }
        }
        return mGuidelineAnimateCompatImpl;
    }
    //endregion Private methods
}
