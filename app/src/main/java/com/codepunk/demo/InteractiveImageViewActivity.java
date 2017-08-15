package com.codepunk.demo;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout.LayoutParams;
import android.support.constraint.Guideline;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.Build.VERSION_CODES.HONEYCOMB;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class InteractiveImageViewActivity
        extends AppCompatActivity
        implements View.OnClickListener,
                AdapterView.OnItemSelectedListener,
                InteractiveImageView.OnDrawListener {

    //region Nested classes
    /*
    private static class SeekBarWithValues
            implements SeekBar.OnSeekBarChangeListener {

        private interface OnValueChangeListener {
            void onStartTrackingValue(SeekBarWithValues seekBarWithValues, float value);
            void onValueChanged(SeekBarWithValues seekBarWithValues, float value, boolean fromUser);
            void onStopTrackingValue(SeekBarWithValues seekBarWithValues);
        }

        private final ViewGroup mView;
        private final AppCompatImageView mIconView;
        private final TextView mCurrentValueText;
        private final TextView mMinValueText;
        private final AppCompatSeekBar mValueSeekBar;
        private final TextView mMaxValueText;

        private Format mFormat = new DecimalFormat("#0.0");
        private float mMinValue = 0.0f;
        private float mMaxValue = 100.0f;

        private float mTrackingStartValue;
        private OnValueChangeListener mOnValueChangeListener;

        public SeekBarWithValues(Activity activity, @IdRes int resId) {
            super();
            mView = (ViewGroup) activity.findViewById(resId);
            mIconView = (AppCompatImageView) mView.findViewById(R.id.image_icon);
            mCurrentValueText = (TextView) mView.findViewById(R.id.text_current_value);
            mMinValueText = (TextView) mView.findViewById(R.id.text_min_value);
            mValueSeekBar = (AppCompatSeekBar) mView.findViewById(R.id.seek_value);
            mMaxValueText = (TextView) mView.findViewById(R.id.text_max_value);

            mValueSeekBar.setOnSeekBarChangeListener(this);
        }

        public float getCurrentValue() {
            return progressToValue(mValueSeekBar.getProgress());
        }

        public void setCurrentValue(float value) {
            mCurrentValueText.setText(mFormat.format(value));
            mValueSeekBar.setProgress(valueToProgress(value));
        }

        public void setFormat(Format format) {
            mFormat = format;
        }

        public void setIcon(@DrawableRes int resId) {
            mIconView.setImageResource(resId);
        }

        public void setRange(float min, float max) {
            mMinValue = Math.min(min, max);
            mMaxValue = Math.max(min, max);
            mMinValueText.setText(mFormat.format(mMinValue));
            mMaxValueText.setText(mFormat.format(mMaxValue));
            setCurrentValue(getCurrentValue());
        }

        private float checkValue(float value) {
            return Math.max(Math.min(value, mMaxValue), mMinValue);
        }

        private int valueToProgress(float value) {
            final float checkedValue = checkValue(value);
            return Math.round(
                    mValueSeekBar.getMax() * (checkedValue - mMinValue) / (mMaxValue - mMinValue));
        }

        private float progressToValue(int progress) {
            final float relativeProgress =
                    (float) mValueSeekBar.getProgress() / mValueSeekBar.getMax();
            return mMinValue + (mMaxValue - mMinValue) * relativeProgress;
        }

        @Override // SeekBar.OnSeekBarChangeListener
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override // SeekBar.OnSeekBarChangeListener
        public void onStartTrackingTouch(SeekBar seekBar) {
            mTrackingStartValue = progressToValue(seekBar.getProgress());
            if (mOnValueChangeListener != null) {
                mOnValueChangeListener.onStartTrackingValue(this, mTrackingStartValue);
            }
        }

        @Override // SeekBar.OnSeekBarChangeListener
        public void onStopTrackingTouch(SeekBar seekBar) {

        }

        public void setOnValueChangeListener(OnValueChangeListener onValueChangeListener) {
            mOnValueChangeListener = onValueChangeListener;
        }
    }
    */

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
    private ExtendedSeekBar mPanXSeekBar;
    private ExtendedSeekBar mPanYSeekBar;
    private ExtendedSeekBar mScaleXSeekBar;
    private ExtendedSeekBar mScaleYSeekBar;
    private ImageButton mLockBtn;

    private boolean mShowingControls = true;

    private int mShownEnd;
    private int mDuration;

    private String[] mScaleTypeEntryValues;

    private GuidelineAnimateCompatImpl mGuidelineAnimateCompatImpl;

    private boolean mScaleLocked = true;
    private SeekBar mTrackingSeekBar;
    private final Map<SeekBar, Integer> mStartValues = new HashMap<>();

    private final PointF mCenter = new PointF();
    private final PointF mScale = new PointF();
    //endregion Fields

    //region Lifecycle methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interactive_image_view);
        mMainLayout = (ViewGroup) findViewById(R.id.layout_main);
        mGuideline = (Guideline) findViewById(R.id.guideline);
        mImageView = (InteractiveImageView) findViewById(R.id.view_image);
        mControlsView = (ViewGroup) findViewById(R.id.layout_controls);
        mDrawableSpinner = (Spinner) findViewById(R.id.spinner_drawable);
        mScaleTypeSpinner = (Spinner) findViewById(R.id.spinner_scale_type);
        mPanXSeekBar = (ExtendedSeekBar) findViewById(R.id.seek_pan_x);
        mPanYSeekBar = (ExtendedSeekBar) findViewById(R.id.seek_pan_y);
        mScaleXSeekBar = (ExtendedSeekBar) findViewById(R.id.seek_scale_x);
        mScaleYSeekBar = (ExtendedSeekBar) findViewById(R.id.seek_scale_y);
        mLockBtn = (ImageButton) findViewById(R.id.image_btn_lock);

        LayoutParams lp = (LayoutParams) mGuideline.getLayoutParams();
        mShownEnd = lp.guideEnd;

        final Resources res = getResources();
        mDuration = res.getInteger(android.R.integer.config_shortAnimTime);
        mScaleTypeEntryValues = res.getStringArray(R.array.scale_type_values);

        final int resId = R.drawable.wilderness_lodge;
        mImageView.setImageResource(resId);
        final int position = DRAWABLE_RES_IDS.indexOf(resId);
        mDrawableSpinner.setSelection(position);
        final ImageView.ScaleType scaleType = mImageView.getScaleType();
        mScaleTypeSpinner.setSelection(scaleType.ordinal());

        /*
        final NumberFormat percentFormat = NumberFormat.getPercentInstance();
        mPanXSeekBar.setFormat(percentFormat);
        mPanXSeekBar.setRange(0.0f, 1.0f);
        mPanYSeekBar.setFormat(percentFormat);
        mPanYSeekBar.setIcon(R.drawable.ic_swap_vert_white_24dp);
        mPanYSeekBar.setRange(0.0f, 1.0f);
        mScaleYSeekBar.setIcon(R.drawable.ic_swap_vert_white_24dp);
        */

        mImageView.setOnDrawListener(this);
        mDrawableSpinner.setOnItemSelectedListener(this);
        mScaleTypeSpinner.setOnItemSelectedListener(this);
        mLockBtn.setOnClickListener(this);

        if (savedInstanceState != null) {
            mShowingControls = savedInstanceState.getBoolean(KEY_SHOWING_CONTROLS, false);
            mScaleLocked = savedInstanceState.getBoolean(KEY_SCALE_LOCKED, false);
        }

        if (mShowingControls) {
            showControls(false);
        } else {
            hideControls(false);
        }

        if (mScaleLocked) {
            mLockBtn.setImageResource(R.drawable.ic_lock_outline_white_24dp);
        } else {
            mLockBtn.setImageResource(R.drawable.ic_lock_open_white_24dp);
        }

        /* TODO TEMP
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final int paddingLeft = mImageView.getPaddingLeft();
                final int paddingTop = mImageView.getPaddingTop();
                final int paddingRight = mImageView.getPaddingRight();
                final int paddingBottom = mImageView.getPaddingBottom();
                final int width = mImageView.getWidth();
                final int height = mImageView.getHeight();
                final int availableWidth = width - paddingLeft - paddingRight;
                final int availableHeight = height - paddingTop - paddingBottom;
                final float px = (availableWidth / 2.0f);
                final float py = (availableHeight / 2.0f);

                mImageView.setScaleType(ImageView.ScaleType.MATRIX);
                final Matrix matrix = mImageView.getImageMatrix();
                matrix.postRotate(30.0f, px, py);
                mImageView.setImageMatrix(matrix);
            }
        }, 2000);
        */
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
        outState.putBoolean(KEY_SCALE_LOCKED, mScaleLocked);
    }
    //endregion Lifecycle methods

    //region Interface methods
    @Override /* View.OnClickListener */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image_btn_lock:
                if (mScaleLocked) {
                    mScaleLocked = false;
                    mLockBtn.setImageResource(R.drawable.ic_lock_open_white_24dp);
                } else {
                    mScaleLocked = true;
                    mLockBtn.setImageResource(R.drawable.ic_lock_outline_white_24dp);
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

    /*
    @Override // SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && mScaleLocked) {
            int startProgress = mStartValues.containsKey(seekBar) ? mStartValues.get(seekBar) : 0;
            float factor = (float) progress / startProgress;
            Log.d(TAG, String.format(Locale.US, "startProgress=%d, factor=%.2f", startProgress, factor));
        }
    }

    @Override // SeekBar.OnSeekBarChangeListener
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (mScaleLocked) {
            mTrackingSeekBar = seekBar;
            mStartValues.put(
                    mScaleXSeekBar.mValueSeekBar,
                    mScaleXSeekBar.mValueSeekBar.getProgress());
            mStartValues.put(
                    mScaleYSeekBar.mValueSeekBar,
                    mScaleYSeekBar.mValueSeekBar.getProgress());
        }
    }

    @Override // SeekBar.OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {
        mTrackingSeekBar = null;
        mStartValues.clear();
    }
    */

    /*
    @Override
    public void onStartTrackingValue(SeekBarWithValues seekBarWithValues, float value) {

    }

    @Override
    public void onValueChanged(SeekBarWithValues seekBarWithValues, float value, boolean fromUser) {

    }

    @Override
    public void onStopTrackingValue(SeekBarWithValues seekBarWithValues) {

    }
    */

    @Override /* InteractiveImageView.OnDrawListener */
    public void onDraw(InteractiveImageView view, Canvas canvas) {
        // TODO TEMP
        /*
        mScaleXSeekBar.setRange(view.getMinScaleX(), view.getMaxScaleX());
        mScaleYSeekBar.setRange(view.getMinScaleY(), view.getMaxScaleY());

        mImageView.getImagePointInCenter(mCenter);
        mPanXSeekBar.setCurrentValue(mCenter.x);
        mPanYSeekBar.setCurrentValue(mCenter.y);

        mImageView.getScale(mScale);
        mScaleXSeekBar.setCurrentValue(mScale.x);
        mScaleYSeekBar.setCurrentValue(mScale.y);
        */
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
    //endregion Private methods
}
