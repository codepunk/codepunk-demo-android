package com.codepunk.demo.interactiveimageview.version3;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout.LayoutParams;
import android.support.constraint.Guideline;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.AppCompatTextView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.codepunk.demo.CustomAppCompatSeekBar;
import com.codepunk.demo.R;
import com.codepunk.demo.support.ProgressBarCompat;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import static android.os.Build.VERSION_CODES.HONEYCOMB;

public class InteractiveImageViewActivity
        extends AppCompatActivity
        implements View.OnClickListener,
        AdapterView.OnItemSelectedListener,
        SeekBar.OnSeekBarChangeListener,
        InteractiveImageView.OnDrawListener {

    //region Nested classes
    private interface GuidelineAnimateCompatImpl {
        void animateControlsDrawer(final int toValue);
    }

    private class BaseGuidelineAnimateCompatImpl implements GuidelineAnimateCompatImpl {
        private Animation mControlsDrawerAnimation;

        @Override
        public void animateControlsDrawer(final int toValue) {
            if (mControlsDrawerAnimation != null) {
                mControlsDrawerAnimation.cancel();
                mGuideline.clearAnimation();
                mControlsDrawerAnimation = null;
            }
            final LayoutParams lp = (LayoutParams) mGuideline.getLayoutParams();
            final float fraction = Math.abs(lp.guideEnd - toValue) / (float) mShownEnd;
            final float fromValue = lp.guideEnd;
            mControlsDrawerAnimation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    lp.guideEnd = (int) (fromValue + (toValue - fromValue) * interpolatedTime);
                    mGuideline.setLayoutParams(lp);
                }
            };
            mControlsDrawerAnimation.setDuration((int) (mDuration * fraction));
            mControlsDrawerAnimation.setInterpolator(mInterpolator);
            mMainLayout.startAnimation(mControlsDrawerAnimation);
        }
    }

    @TargetApi(HONEYCOMB)
    private class HoneyCombGuidelineAnimateCompatImpl
            implements GuidelineAnimateCompatImpl {
        private ValueAnimator mControlsDrawerAnimator;

        @Override
        public void animateControlsDrawer(final int toValue) {
            if (mControlsDrawerAnimator != null) {
                mControlsDrawerAnimator.cancel();
            }
            final LayoutParams lp = (LayoutParams) mGuideline.getLayoutParams();
            final float fraction = Math.abs(lp.guideEnd - toValue) / (float) mShownEnd;
            mControlsDrawerAnimator = ValueAnimator.ofInt(lp.guideEnd, toValue);
            mControlsDrawerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    lp.guideEnd = (Integer) animation.getAnimatedValue();
                    mGuideline.setLayoutParams(lp);
                }
            });
            mControlsDrawerAnimator.setDuration((int) (mDuration * fraction));
            mControlsDrawerAnimator.setInterpolator(mInterpolator);
            mControlsDrawerAnimator.start();
        }
    }
    //endregion Nested classes

    //region Constants
    private static final String TAG = "tag_" + InteractiveImageViewActivity.class.getSimpleName();

    private static final @DrawableRes int DEFAULT_DRAWABLE_RES_ID = R.drawable.wilderness_lodge;

    private static final double INTERPOLATION_FACTOR = 3.0d;

    private final List<Integer> DRAWABLE_RES_IDS = Arrays.asList(
            0,
            R.drawable.cinderellas_castle,
            R.drawable.wilderness_lodge,
            R.drawable.polynesian,
            R.drawable.gradient);

    private static final int HIDDEN_END = 0;
    private static final String CLASS_NAME = InteractiveImageViewActivity.class.getName();
    private static final String KEY_DRAWABLE_RES_ID = CLASS_NAME + ".drawableResId";
    private static final String KEY_SCALE_TYPE = CLASS_NAME + ".scaleType";
    private static final String KEY_IMAGE_CENTER_X = CLASS_NAME + ".imageCenterX";
    private static final String KEY_IMAGE_CENTER_Y = CLASS_NAME + ".imageCenterY";
    private static final String KEY_IMAGE_SCALE_X = CLASS_NAME + ".imageScaleX";
    private static final String KEY_IMAGE_SCALE_Y = CLASS_NAME + ".imageScaleY";
    private static final String KEY_HAS_CUSTOM_PLACEMENT = CLASS_NAME + ".hasCustomPlacement"; // TODO Can I move HAS_CUSTOM_PLACEMENT and CENTER/SCALE keys to the widget itself?
    private static final String KEY_SHOWING_CONTROLS = CLASS_NAME + ".showingControls";
    private static final String KEY_SCALE_LOCKED = CLASS_NAME + ".scaleLocked";
    //endregion Constants

    //region Fields
    private Interpolator mInterpolator = new DecelerateInterpolator();

    private Guideline mGuideline;
    private ViewGroup mMainLayout;
    private InteractiveImageView mImageView;
    private ViewGroup mControlsView;
    private Spinner mDrawableSpinner;
    private Spinner mScaleTypeSpinner;
    private AppCompatTextView mPanXValueView;
    private AppCompatTextView mPanXMinValueView;
    private AppCompatSeekBar mPanXSeekBar;
    private AppCompatTextView mPanXMaxValueView;
    private AppCompatTextView mPanYValueView;
    private AppCompatTextView mPanYMinValueView;
    private AppCompatSeekBar mPanYSeekBar;
    private AppCompatTextView mPanYMaxValueView;
    private AppCompatTextView mScaleXValueView;
    private AppCompatTextView mScaleXMinValueView;
    private CustomAppCompatSeekBar mScaleXSeekBar;
    private AppCompatTextView mScaleXMaxValueView;
    private AppCompatTextView mScaleYValueView;
    private AppCompatTextView mScaleYMinValueView;
    private CustomAppCompatSeekBar mScaleYSeekBar;
    private AppCompatTextView mScaleYMaxValueView;
    private ViewGroup mLockBtnLayout;
    private ToggleButton mLockBtn;

    private boolean mShowingControls = true;

    private int mShownEnd;
    private int mDuration;

    private String[] mScaleTypeEntryValues;

    private GuidelineAnimateCompatImpl mGuidelineAnimateCompatImpl;

    private SeekBar mTrackingSeekBar;

    private final NumberFormat mPercentFormat = NumberFormat.getPercentInstance();
    private final NumberFormat mDecimalFormat = new DecimalFormat("#0.00");

    private final Point mDisplayedSizePoint = new Point();
    private final PointF mDisplayedSizePointF = new PointF();
    private final Point mMinScaledSizePoint = new Point();
    private final Point mMaxScaledSizePoint = new Point(); // TODO Replace these with a point "factory"?

    private boolean mPendingResetClamps = true;
    //endregion Fields

    //region Lifecycle methods
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_interactive_image_view_v3);
        mMainLayout = findViewById(R.id.layout_main);
        mGuideline = findViewById(R.id.guideline);
        mImageView = findViewById(R.id.view_image);
        mControlsView = findViewById(R.id.layout_controls);
        mDrawableSpinner = findViewById(R.id.spinner_drawable);
        mScaleTypeSpinner = findViewById(R.id.spinner_scale_type);
        mPanXValueView = findViewById(R.id.text_pan_x_value);
        mPanXMinValueView = findViewById(R.id.text_pan_x_min_value);
        mPanXSeekBar = findViewById(R.id.seek_pan_x_value);
        mPanXMaxValueView = findViewById(R.id.text_pan_x_max_value);
        mPanYValueView = findViewById(R.id.text_pan_y_value);
        mPanYMinValueView = findViewById(R.id.text_pan_y_min_value);
        mPanYSeekBar = findViewById(R.id.seek_pan_y_value);
        mPanYMaxValueView = findViewById(R.id.text_pan_y_max_value);
        mScaleXValueView = findViewById(R.id.text_scale_x_value);
        mScaleXMinValueView = findViewById(R.id.text_scale_x_min_value);
        mScaleXSeekBar = findViewById(R.id.seek_scale_x_value);
        mScaleXMaxValueView = findViewById(R.id.text_scale_x_max_value);
        mScaleYValueView = findViewById(R.id.text_scale_y_value);
        mScaleYMinValueView = findViewById(R.id.text_scale_y_min_value);
        mScaleYSeekBar = findViewById(R.id.seek_scale_y_value);
        mScaleYMaxValueView = findViewById(R.id.text_scale_y_max_value);
        mLockBtnLayout = findViewById(R.id.layout_btn_lock);
        mLockBtn = findViewById(R.id.btn_lock);

        mPanXMinValueView.setText(mPercentFormat.format(0.0f));
        mPanXMaxValueView.setText(mPercentFormat.format(1.0f));
        mPanYMinValueView.setText(mPercentFormat.format(0.0f));
        mPanYMaxValueView.setText(mPercentFormat.format(1.0f));

        LayoutParams lp = (LayoutParams) mGuideline.getLayoutParams();
        mShownEnd = lp.guideEnd;

        final Resources res = getResources();
        mDuration = res.getInteger(android.R.integer.config_shortAnimTime);
        mScaleTypeEntryValues = res.getStringArray(R.array.scale_type_values);

        mImageView.setOnDrawListener(this);
        mLockBtnLayout.setOnClickListener(this);
        mPanXSeekBar.setOnSeekBarChangeListener(this);
        mPanYSeekBar.setOnSeekBarChangeListener(this);
        mLockBtn.setOnClickListener(this);
        mScaleXSeekBar.setOnSeekBarChangeListener(this);
        mScaleYSeekBar.setOnSeekBarChangeListener(this);

        if (savedInstanceState == null) {
            // Set up initial values
            mImageView.setImageResource(DEFAULT_DRAWABLE_RES_ID);
            final int position = DRAWABLE_RES_IDS.indexOf(DEFAULT_DRAWABLE_RES_ID);
            mDrawableSpinner.setSelection(position, false);
            final ScaleType scaleType = mImageView.getScaleType();
            mScaleTypeSpinner.setSelection(scaleType.ordinal(), false);
        } else {
            mLockBtn.setChecked(savedInstanceState.getBoolean(KEY_SCALE_LOCKED, false));
            mShowingControls = savedInstanceState.getBoolean(KEY_SHOWING_CONTROLS, false);

            final @DrawableRes int drawableResId =
                    savedInstanceState.getInt(KEY_DRAWABLE_RES_ID, DEFAULT_DRAWABLE_RES_ID);
            mImageView.setImageResource(drawableResId);
            final ScaleType scaleType =
                    (ScaleType) savedInstanceState.getSerializable(KEY_SCALE_TYPE);
            mImageView.setScaleType(scaleType);
            final boolean hasCustomPlacement =
                    savedInstanceState.getBoolean(KEY_HAS_CUSTOM_PLACEMENT, false);
            if (hasCustomPlacement) {
                final float centerX = savedInstanceState.getFloat(KEY_IMAGE_CENTER_X, 0.5f);
                final float centerY = savedInstanceState.getFloat(KEY_IMAGE_CENTER_Y, 0.5f);
                final float scaleX = savedInstanceState.getFloat(KEY_IMAGE_SCALE_X);
                final float scaleY = savedInstanceState.getFloat(KEY_IMAGE_SCALE_Y);
                mImageView.setPlacement(scaleX, scaleY, centerX, centerY);
            }
        }

        if (mShowingControls) {
            showControls(false);
        } else {
            hideControls(false);
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                // TODO Any better way?
                mDrawableSpinner.setOnItemSelectedListener(InteractiveImageViewActivity.this);
                mScaleTypeSpinner.setOnItemSelectedListener(InteractiveImageViewActivity.this);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_interactive_image_view, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        final int position = mDrawableSpinner.getSelectedItemPosition();
        outState.putInt(KEY_DRAWABLE_RES_ID, DRAWABLE_RES_IDS.get(position));
        outState.putSerializable(KEY_SCALE_TYPE, mImageView.getScaleType());
        final boolean hasCustomPlacement = mImageView.hasCustomPlacement();
        outState.putBoolean(KEY_HAS_CUSTOM_PLACEMENT, hasCustomPlacement);
        if (hasCustomPlacement) {
            outState.putFloat(KEY_IMAGE_CENTER_X, mImageView.getImageCenterX());
            outState.putFloat(KEY_IMAGE_CENTER_Y, mImageView.getImageCenterY());
            outState.putFloat(KEY_IMAGE_SCALE_X, mImageView.getImageScaleX());
            outState.putFloat(KEY_IMAGE_SCALE_Y, mImageView.getImageScaleY());
        }
        outState.putBoolean(KEY_SHOWING_CONTROLS, mShowingControls);
        outState.putBoolean(KEY_SCALE_LOCKED, mLockBtn.isChecked());
    }
    //endregion Lifecycle methods

    //region Interface methods
    @Override // View.OnClickListener
    public void onClick(View v) {
        if (v == mLockBtn) {
            resetClamps();
        }
    }

    @Override // AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.spinner_drawable:
                // TODO
                mPendingResetClamps = true;
                mImageView.setImageResource(DRAWABLE_RES_IDS.get(position));
                // mHasIntrinsicSize = mImageView.getIntrinsicImageSize(mIntrinsicSizePoint);

                // TODO Enable/disable controls based on intrinsic size
                /*
                final Drawable drawable = mImageView.getDrawable();
                if (drawable == null) {
                    mIntrinsicSizeTextView.setText(
                            getResources().getString(R.string.intrinsic_size_text, 0, 0));
                } else {
                    final int intrinsicWidth = drawable.getIntrinsicWidth();
                    final int intrinsicHeight = drawable.getIntrinsicHeight();
                    mIntrinsicSizeTextView.setText(
                            getResources().getString(
                                    R.string.intrinsic_size_text, intrinsicWidth, intrinsicHeight));
                }
                */
                break;
            case R.id.spinner_scale_type:
                mPendingResetClamps = true;
                final String name = mScaleTypeEntryValues[position];
                mImageView.setScaleType(ScaleType.valueOf(name));
                break;
        }
    }

    @Override // AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override // SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            final float scaleX = mImageView.getImageScaleX();
            final float scaleY = mImageView.getImageScaleY();
            final float minScaleX = mImageView.getMinScaleX();
            final float maxScaleX = mImageView.getMaxScaleX();
            final float minScaleY = mImageView.getMinScaleY();
            final float maxScaleY = mImageView.getMaxScaleY();
            switch (seekBar.getId()) {
                case R.id.seek_pan_x_value:
                case R.id.seek_pan_y_value:
                    float centerX = getValue(mPanXSeekBar, 0.0f, 1.0f);
                    float centerY = getValue(mPanYSeekBar, 0.0f, 1.0f);
                    mImageView.setImageCenter(centerX, centerY);
                    break;
                case R.id.seek_scale_x_value: {
                    final float newScaleX = getValue(seekBar, minScaleX, maxScaleX);
                    final float newScaleY = (
                            mLockBtn.isChecked() ?
                                    scaleY * newScaleX / scaleX :
                                    scaleY);
                    mImageView.setImageScale(newScaleX, newScaleY);
                    break;
                }
                case R.id.seek_scale_y_value: {
                    final float newScaleY = getValue(seekBar, minScaleY, maxScaleY);
                    final float newScaleX = (mLockBtn.isChecked() ?
                            scaleX * newScaleY / scaleY :
                            scaleX);
                    mImageView.setImageScale(newScaleX, newScaleY);
                    break;
                }
            }
        }
    }

    @Override // SeekBar.OnSeekBarChangeListener
    public void onStartTrackingTouch(SeekBar seekBar) {
        mTrackingSeekBar = seekBar;
        /*
        if (mScaleLocked) {
            mTrackingStartValues.put(
                    mScaleXSeekBar.mValueSeekBar,
                    mScaleXSeekBar.mValueSeekBar.getProgress());
            mTrackingStartValues.put(
                    mScaleYSeekBar.mValueSeekBar,
                    mScaleYSeekBar.mValueSeekBar.getProgress());
        }
        */
    }

    @Override // SeekBar.OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {
        mTrackingSeekBar = null;
    }

    @Override // InteractiveImageView.DemoInteractiveImageViewListener
    public void onDraw(InteractiveImageView view, Canvas canvas) {
        // TODO Capture which control(s) the user is manipulating and don't updateUI those
        final float centerX = view.getImageCenterX();
        final float centerY = view.getImageCenterY();
        mPanXValueView.setText(mPercentFormat.format(centerX));
        setValue(mPanXSeekBar, 0.0f, 1.0f, centerX, false);
        mPanYValueView.setText(mPercentFormat.format(centerY));
        setValue(mPanYSeekBar, 0.0f, 1.0f, centerY, false);

        if (mPendingResetClamps) {
            mScaleXSeekBar.setClampedMin(Integer.MIN_VALUE);
            mScaleXSeekBar.setClampedMax(Integer.MAX_VALUE);
            mScaleYSeekBar.setClampedMin(Integer.MIN_VALUE);
            mScaleYSeekBar.setClampedMax(Integer.MAX_VALUE);
        }

        final float scaleX = view.getImageScaleX();
        final float scaleY = view.getImageScaleY();
        final float minScaleX = view.getMinScaleX();
        final float maxScaleX = view.getMaxScaleX();
        final float minScaleY = view.getMinScaleY();
        final float maxScaleY = view.getMaxScaleY();
        final int intrinsicImageWidth;
        final int intrinsicImageHeight;
        final Drawable drawable = view.getDrawable();
        if (drawable == null) {
            intrinsicImageWidth = 0;
            intrinsicImageHeight = 0;
        } else {
            intrinsicImageWidth = Math.max(drawable.getIntrinsicWidth(), 0);
            intrinsicImageHeight = Math.max(drawable.getIntrinsicHeight(), 0);
        }
        final int minWidth = Math.round(minScaleX * intrinsicImageWidth);
        final int maxWidth = Math.round(maxScaleX * intrinsicImageWidth);
        final int minHeight = Math.round(minScaleY * intrinsicImageHeight);
        final int maxHeight = Math.round(maxScaleY * intrinsicImageHeight);

        mScaleXValueView.setText(mDecimalFormat.format(scaleX));
        mScaleXMinValueView.setText(mDecimalFormat.format(minScaleX));
        mScaleXMaxValueView.setText(mDecimalFormat.format(maxScaleX));
        mScaleYValueView.setText(mDecimalFormat.format(scaleY));
        mScaleYMinValueView.setText(mDecimalFormat.format(minScaleY));
        mScaleYMaxValueView.setText(mDecimalFormat.format(maxScaleY));

        setValue(mScaleXSeekBar, minWidth, maxWidth, intrinsicImageWidth *  scaleX, false);
        setValue(mScaleYSeekBar, minHeight, maxHeight, intrinsicImageHeight*  scaleY, false);

        if (mPendingResetClamps) {
            mPendingResetClamps = false;
            resetClamps();
        }
    }
    //endregion Interface methods

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
            showOrHideControls(mShownEnd, animate);
        }
    }

    public void hideControls(boolean animate) {
        if (mShowingControls || !animate) {
            mShowingControls = false;
            showOrHideControls(HIDDEN_END, animate);
        }
    }
    //endregion Methods

    //region Private methods
    private void showOrHideControls(int toValue, boolean animate) {
        if (animate) {
            getGuidelineAnimateCompatImpl().animateControlsDrawer(toValue);
        } else {
            final LayoutParams lp = (LayoutParams) mGuideline.getLayoutParams();
            lp.guideEnd = toValue;
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

    private static float progressToValue(
            int min,
            int max,
            int progress,
            float minValue,
            float maxValue) {
        final float percent = (float) (progress - min) / (max - min);
        return (minValue + (maxValue - minValue) * percent);
    }

    private static int getAbsoluteValue(@NonNull ProgressBar progressBar, float relativeValue) {
        return Math.round(progressBar.getMax() * relativeValue);
    }

    private static float getRelativeValue(@NonNull ProgressBar progressBar, int absoluteValue) {
        return (float) absoluteValue / progressBar.getMax();
    }

    private static float getValue(ProgressBar progressBar, float minValue, float maxValue) {
        final int minProgress = 0;
        return progressToValue(
                minProgress,
                progressBar.getMax(),
                progressBar.getProgress(),
                minValue,
                maxValue);
    }

    private void resetClamps() {
        final float minScaleX = mImageView.getMinScaleX();
        final float maxScaleX = mImageView.getMaxScaleX();
        final float minScaleY = mImageView.getMinScaleY();
        final float maxScaleY = mImageView.getMaxScaleY();
        final int scaleXClampedMin;
        final int scaleXClampedMax;
        final int scaleYClampedMin;
        final int scaleYClampedMax;
        if (mLockBtn.isChecked()) {
            final float currentScaleX = getValue(mScaleXSeekBar, minScaleX, maxScaleX);
            final float currentScaleY = getValue(mScaleYSeekBar, minScaleY, maxScaleY);
            final float shrinkFactor;
            final float growFactor;
            if (getRelativeValue(mScaleXSeekBar, mScaleXSeekBar.getProgress()) <
                    getRelativeValue(mScaleYSeekBar, mScaleYSeekBar.getProgress())) {
                shrinkFactor = currentScaleX / minScaleX;
                growFactor = maxScaleY / currentScaleY;
                scaleXClampedMin = Integer.MIN_VALUE;
                scaleXClampedMax = valueToProgress(
                        minScaleX,
                        maxScaleX,
                        currentScaleX * growFactor,
                        0,
                        mScaleXSeekBar.getMax());
                scaleYClampedMin = valueToProgress(
                        minScaleY,
                        maxScaleY,
                        currentScaleY / shrinkFactor,
                        0,
                        mScaleYSeekBar.getMax());
                scaleYClampedMax = Integer.MAX_VALUE;
            } else {
                shrinkFactor = currentScaleY / minScaleY;
                growFactor = maxScaleX / currentScaleX;
                scaleXClampedMin = valueToProgress(
                        minScaleX,
                        maxScaleX,
                        currentScaleX / shrinkFactor,
                        0,
                        mScaleXSeekBar.getMax());
                scaleXClampedMax = Integer.MAX_VALUE;
                scaleYClampedMin = Integer.MIN_VALUE;
                scaleYClampedMax = valueToProgress(
                        minScaleY,
                        maxScaleY,
                        currentScaleY * growFactor,
                        0,
                        mScaleYSeekBar.getMax());
            }
        } else {
            scaleXClampedMin = scaleYClampedMin = Integer.MIN_VALUE;
            scaleXClampedMax = scaleYClampedMax = Integer.MAX_VALUE;
        }
        mScaleXSeekBar.setClampedMin(scaleXClampedMin);
        mScaleXSeekBar.setClampedMax(scaleXClampedMax);
        mScaleYSeekBar.setClampedMin(scaleYClampedMin);
        mScaleYSeekBar.setClampedMax(scaleYClampedMax);
    }

    private static void setProgressPercent(@NonNull ProgressBar progressBar, float progressPercent) {
        progressBar.setProgress(Math.round(progressBar.getMax() * progressPercent));
    }

    private static void setValue(
            ProgressBar progressBar,
            float value,
            boolean animate) {
        final int minProgress = 0;
        final int progress = valueToProgress(
                0.0f,
                1.0f,
                value,
                minProgress,
                progressBar.getMax());
        ProgressBarCompat.setProgress(progressBar, progress, animate);
    }

    private static void setValue(
            ProgressBar progressBar,
            float minValue,
            float maxValue,
            float value,
            boolean animate) {
        final int minProgress = 0;
        final int progress = valueToProgress(
                minValue,
                maxValue,
                value,
                minProgress,
                progressBar.getMax());
        ProgressBarCompat.setProgress(progressBar, progress, animate);
    }

    private static int valueToProgress(
            float minValue,
            float maxValue,
            float value,
            int min,
            int max) {
        final float percent = (
                Float.isNaN(value) ?
                        0.50f :
                        (value - minValue) / (maxValue - minValue));
        return min + (int) ((max - min) * percent);
    }
    //endregion Private methods
}
