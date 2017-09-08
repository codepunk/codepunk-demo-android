package com.codepunk.demo;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ToggleButton;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import static android.os.Build.VERSION_CODES.HONEYCOMB;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class InteractiveImageViewActivity
        extends AppCompatActivity
        implements View.OnClickListener,
                AdapterView.OnItemSelectedListener,
                SeekBar.OnSeekBarChangeListener,
                InteractiveImageView.OnDrawListener {

    //region Nested classes
    private interface GuidelineAnimateCompatImpl {
        void animate(final int toValue);
    }

    private class BaseGuidelineAnimateCompatImpl implements GuidelineAnimateCompatImpl {
        private Animation mAnimation;

        @Override
        public void animate(final int toValue) {
            if (mAnimation != null) {
                mAnimation.cancel();
                mGuideline.clearAnimation();
                mAnimation = null;
            }
            final LayoutParams lp = (LayoutParams) mGuideline.getLayoutParams();
            final float fraction = Math.abs(lp.guideEnd - toValue) / (float) mShownEnd;
            final float fromValue = lp.guideEnd;
            mAnimation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    lp.guideEnd = (int) (fromValue + (toValue - fromValue) * interpolatedTime);
                    mGuideline.setLayoutParams(lp);
                }
            };
            mAnimation.setDuration((int) (mDuration * fraction));
            mAnimation.setInterpolator(mInterpolator);
            mMainLayout.startAnimation(mAnimation);
        }
    }

    @TargetApi(HONEYCOMB)
    private class HoneyCombGuidelineAnimateCompatImpl
            implements GuidelineAnimateCompatImpl {
        private ValueAnimator mAnimator;

        @Override
        public void animate(final int toValue) {
            if (mAnimator != null) {
                mAnimator.cancel();
            }
            final LayoutParams lp = (LayoutParams) mGuideline.getLayoutParams();
            final float fraction = Math.abs(lp.guideEnd - toValue) / (float) mShownEnd;
            mAnimator = ValueAnimator.ofInt(lp.guideEnd, toValue);
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    lp.guideEnd = (Integer) animation.getAnimatedValue();
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
    private static final String TAG = "tag_" + InteractiveImageViewActivity.class.getSimpleName();

    private static final double INTERPOLATION_FACTOR = 3.0d;

    private final List<Integer> DRAWABLE_RES_IDS = Arrays.asList(
            0,
            R.drawable.cinderellas_castle,
            R.drawable.wilderness_lodge,
            R.drawable.polynesian,
            R.drawable.gradient);

    private static final int HIDDEN_END = 0;
    private static final String KEY_SHOWING_CONTROLS = makeKey("showingControls");
    private static final String KEY_SCALE_LOCKED = makeKey("scaleLocked");
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
    private AppCompatSeekBar mScaleXSeekBar;
    private AppCompatTextView mScaleXMaxValueView;
    private AppCompatTextView mScaleYValueView;
    private AppCompatTextView mScaleYMinValueView;
    private AppCompatSeekBar mScaleYSeekBar;
    private AppCompatTextView mScaleYMaxValueView;
    private ViewGroup mLockBtnLayout;
    private ToggleButton mLockBtn;

    private boolean mShowingControls = true;

    private int mShownEnd;
    private int mDuration;

    private String[] mScaleTypeEntryValues;

    private GuidelineAnimateCompatImpl mGuidelineAnimateCompatImpl;

    private SeekBar mTrackingSeekBar;

    private final PointF mCenter = new PointF();
    private final PointF mScale = new PointF();

    private final NumberFormat mPercentFormat = NumberFormat.getPercentInstance();
    private final NumberFormat mDecimalFormat = new DecimalFormat("#0.00");

    private final Point mIntrinsicSizePoint = new Point();
    private final Point mDisplayedSizePoint = new Point();
    //endregion Fields

    //region Lifecycle methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interactive_image_view);
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

        final int resId = R.drawable.wilderness_lodge;
        final int position = DRAWABLE_RES_IDS.indexOf(resId);
        mDrawableSpinner.setSelection(position);
        final ImageView.ScaleType scaleType = mImageView.getScaleType();
        mScaleTypeSpinner.setSelection(scaleType.ordinal());

        mImageView.setOnDrawListener(this);
        mDrawableSpinner.setOnItemSelectedListener(this);
        mScaleTypeSpinner.setOnItemSelectedListener(this);
        mLockBtnLayout.setOnClickListener(this);
        mPanXSeekBar.setOnSeekBarChangeListener(this);
        mPanYSeekBar.setOnSeekBarChangeListener(this);
        mScaleXSeekBar.setOnSeekBarChangeListener(this);
        mScaleYSeekBar.setOnSeekBarChangeListener(this);

        final boolean scaleLocked;
        if (savedInstanceState == null) {
            scaleLocked = true;
        } else  {
            mShowingControls = savedInstanceState.getBoolean(KEY_SHOWING_CONTROLS, false);
            scaleLocked = savedInstanceState.getBoolean(KEY_SCALE_LOCKED, false);
        }

        if (mShowingControls) {
            showControls(false);
        } else {
            hideControls(false);
        }

        mLockBtn.setChecked(scaleLocked);
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
        outState.putBoolean(KEY_SCALE_LOCKED, mLockBtn.isChecked());
    }
    //endregion Lifecycle methods

    //region Interface methods
    @Override /* View.OnClickListener */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_btn_lock:
                // TODO NEXT
                if (((ToggleButton) v).isChecked()) {

                } else {

                }
                break;
        }
    }

    @Override /* AdapterView.OnItemSelectedListener */
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.spinner_drawable:
                final int resId = DRAWABLE_RES_IDS.get(position);
                mImageView.setImageResource(resId);
                final Drawable drawable = mImageView.getDrawable();
                /*
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
                final String name = mScaleTypeEntryValues[position];
                ImageView.ScaleType scaleType = ImageView.ScaleType.valueOf(name);
                mImageView.setScaleType(scaleType);
                break;
        }
    }

    @Override // AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override // SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            /*
            int startProgress = mTrackingStartValues.containsKey(seekBar) ? mTrackingStartValues.get(seekBar) : 0;
            float factor = (float) progress / startProgress;
            Log.d(TAG, String.format(Locale.US, "startProgress=%d, factor=%.2f", startProgress, factor));
            */
            final int id = seekBar.getId();
            switch (id) {
                case R.id.seek_pan_x_value:
                case R.id.seek_pan_y_value:
                    float centerX = getValue(mPanXSeekBar, 0.0f, 1.0f);
                    float centerY = getValue(mPanYSeekBar, 0.0f, 1.0f);
                    mImageView.setRelativeCenter(centerX, centerY);
                    break;
                case R.id.seek_scale_x_value:
                case R.id.seek_scale_y_value:
                    final float minScaleX = mImageView.getMinScaleX();
                    final float maxScaleX = mImageView.getMaxScaleX();
                    final float minScaleY = mImageView.getMinScaleY();
                    final float maxScaleY = mImageView.getMaxScaleY();

                    final float scaleX;
                    final float scaleY;

                    if (mLockBtn.isChecked() &&
                            mImageView.getIntrinsicImageSize(mIntrinsicSizePoint) &&
                            mImageView.getDisplayedImageSize(mDisplayedSizePoint)) {

                        // TODO !!
                        scaleX = getValue(mScaleXSeekBar, minScaleX, maxScaleX);
                        scaleY = getValue(mScaleYSeekBar, minScaleY, maxScaleY);

                    } else {

                    /*
                    if (mScaleLocked &&
                            mImageView.getIntrinsicImageSize(mIntrinsicSizePoint) &&
                            mImageView.getDisplayedImageSize(mDisplayedSizePoint)) {
                        final float whRatio = (float) mDisplayedSizePoint.x / mDisplayedSizePoint.y;
                        if (id == R.id.seek_scale_x_value) {

                            // TODO We're close. Simplify the logic?

                            final float proposedScaleX = getRangeProgress(mScaleXSeekBar, minScaleX, maxScaleX);
                            final int proposedSizeX = Math.round(mIntrinsicSizePoint.x * proposedScaleX);
                            final int proposedSizeY = Math.round(proposedSizeX / whRatio);
                            final float proposedScaleY = (float) proposedSizeY / mIntrinsicSizePoint.y;

//                            Log.d(TAG, String.format(Locale.US, "before constraint: scaleX=%.2f, scaleY=%.2f", proposedScaleX, proposedScaleY));


                            if (proposedScaleY < minScaleY) {
                                scaleY = minScaleY;
                                final int newSizeY = Math.round(mIntrinsicSizePoint.y * scaleY);
                                final int newSizeX = Math.round(newSizeY * whRatio);
                                scaleX = (float) (newSizeX / mIntrinsicSizePoint.x);
                            } else if (proposedScaleY > maxScaleY) {
                                scaleY = maxScaleY;
                                final int newSizeY = Math.round(mIntrinsicSizePoint.y * scaleY);
                                final int newSizeX = Math.round(newSizeY * whRatio);
                                scaleX = (float) (newSizeX / mIntrinsicSizePoint.x);
                            } else {
                                scaleY = proposedScaleY;
                                scaleX = proposedScaleX;
                            }

//                            Log.d(TAG, String.format(Locale.US, "before constraint: scaleX=%.2f, scaleY=%.2f", scaleX, scaleY));

                        } else {
                            final float proposedScaleY = getRangeProgress(mScaleYSeekBar, minScaleY, maxScaleY);
                            final int proposedSizeY = Math.round(mIntrinsicSizePoint.y * proposedScaleY);
                            final int proposedSizeX = Math.round(proposedSizeY * whRatio);
                            final float proposedScaleX = (float) proposedSizeX / mIntrinsicSizePoint.x;
                            if (proposedScaleX < minScaleX) {
                                scaleX = minScaleX;
                                final int newSizeX = Math.round(mIntrinsicSizePoint.x * scaleX);
                                final int newSizeY = Math.round(newSizeX / whRatio);
                                scaleY = (float) (newSizeY / mIntrinsicSizePoint.y);
                            } else if (proposedScaleX > maxScaleX) {
                                scaleX = maxScaleX;
                                final int newSizeX = Math.round(mIntrinsicSizePoint.x * scaleX);
                                final int newSizeY = Math.round(newSizeX / whRatio);
                                scaleY = (float) (newSizeY / mIntrinsicSizePoint.y);
                            } else {
                                scaleX = proposedScaleX;
                                scaleY = proposedScaleY;
                            }
                        }
                    } else {
                    */
                        scaleX = getValue(mScaleXSeekBar, minScaleX, maxScaleX);
                        scaleY = getValue(mScaleYSeekBar, minScaleY, maxScaleY);
                    }

                    // TODO set the 2 seek bars
                    //final int progressX = extrapolate(scaleX);
                    //final int progressY = extrapolate(scaleY);
                    //if ()
//                    setRangeProgress(mScaleXSeekBar, minScaleX, maxScaleX, scaleX, false);
//                    setRangeProgress(mScaleYSeekBar, minScaleX, maxScaleX, scaleX, false);

                    mImageView.setScale(scaleX, scaleY);
                    break;
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

    @Override // InteractiveImageView.OnDrawListener
    public void onDraw(InteractiveImageView view, Canvas canvas) {
        // TODO Capture which control(s) the user is manipulating and don't update those
        mImageView.getRelativeCenter(mCenter);
        mPanXValueView.setText(mPercentFormat.format(mCenter.x));
        setValue(mPanXSeekBar, 0.0f, 1.0f, mCenter.x);
        mPanYValueView.setText(mPercentFormat.format(mCenter.y));
        setValue(mPanYSeekBar, 0.0f, 1.0f, mCenter.y);

        final float minScaleX = view.getMinScaleX();
        final float maxScaleX = view.getMaxScaleX();
        final float minScaleY = view.getMinScaleY();
        final float maxScaleY = view.getMaxScaleY();
        mImageView.getScale(mScale);
        mScaleXValueView.setText(mDecimalFormat.format(mScale.x));
        mScaleXMinValueView.setText(mDecimalFormat.format(minScaleX));
        setValue(mScaleXSeekBar, minScaleX, maxScaleX, mScale.x);
        mScaleXMaxValueView.setText(mDecimalFormat.format(maxScaleX));
        mScaleYValueView.setText(mDecimalFormat.format(mScale.y));
        mScaleYMinValueView.setText(mDecimalFormat.format(minScaleY));
        setValue(mScaleYSeekBar, minScaleY, maxScaleY, mScale.y);
        mScaleYMaxValueView.setText(mDecimalFormat.format(maxScaleY));

        final float minSizeX = view.getMinSizeX();
        final float maxSizeX = view.getMaxSizeX();
        final float minSizeY = view.getMinSizeY();
        final float maxSizeY = view.getMaxSizeY();
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
    private static String makeKey(String key) {
        return InteractiveImageViewActivity.class.getSimpleName() + "." + key;
    }

    private void showOrHideControls(int toValue, boolean animate) {
        if (animate) {
            getGuidelineAnimateCompatImpl().animate(toValue);
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

    private static float getValue(ProgressBar progressBar, float minValue, float maxValue) {
        final int minProgress = 0;
        return progressToValue(
                minProgress,
                progressBar.getMax(),
                progressBar.getProgress(),
                minValue,
                maxValue);
    }

    private static void setValue(
            ProgressBar progressBar,
            float minValue,
            float maxValue,
            float value) {
        final int minProgress = 0;
        progressBar.setProgress(
                valueToProgress(
                        minValue,
                        maxValue,
                        value,
                        minProgress,
                        progressBar.getMax()));
    }

    private static int valueToProgress(
            float minValue,
            float maxValue,
            float value,
            int min,
            int max) {
        final float percent = (value - minValue) / (maxValue - minValue);
        return min + (int) ((max - min) * percent);
    }
    //endregion Private methods
}
