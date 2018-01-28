package com.codepunk.demo;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.view.GravityCompat;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.widget.SeekBar;

import com.codepunk.demo.support.ViewCompat;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@SuppressWarnings("unused")
public class SeekBarLayout extends ConstraintLayout
        implements Handler.Callback,
                SeekBar.OnSeekBarChangeListener {

    //region Constants
    private static final String LOG_TAG = SeekBarLayout.class.getSimpleName();
    private static final DecimalFormat DEFAULT_DECIMAL_FORMAT = new DecimalFormat();
    //endregion Constants

    //region Fields
    private AppCompatTextView mLabelText;
    private AppCompatTextView mValueText;
    private AppCompatTextView mMinValueText;
    private AppCompatTextView mMaxValueText;
    private CustomAppCompatSeekBar mSeekBar;

    private DecimalFormat mDecimalFormat = null;

    private Number mMinValue = 0;
    private Number mMaxValue = 0;

    private Handler mUpdateHandler = new Handler(this);
    //endregion Fields

    //region Constructors
    public SeekBarLayout(Context context) {
        super(context);
        initSliderLayout(context, null, R.attr.seekBarLayoutStyle, 0);
    }

    public SeekBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSliderLayout(context, attrs, R.attr.seekBarLayoutStyle, 0);
    }

    public SeekBarLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initSliderLayout(context, attrs, defStyleAttr, 0);
    }
    //endregion Constructors

    //region Implemented methods
    @Override
    public boolean handleMessage(Message message) {
        update();
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar bar, int i, boolean b) {
        mUpdateHandler.sendEmptyMessage(0);
    }

    @Override
    public void onStartTrackingTouch(SeekBar bar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar bar) {

    }
    //endregion Implemented methods

    //region Methods
    public DecimalFormat getDecimalFormat() {
        return (mDecimalFormat == null ? DEFAULT_DECIMAL_FORMAT : mDecimalFormat);
    }

    public void setDecimalFormat(DecimalFormat format) {
        mDecimalFormat = format;
    }

    public void setLabelText(CharSequence text) {
        mLabelText.setText(text);
    }

    public void setLabelText(int resId) {
        mLabelText.setText(resId);
    }

    public void setMaxValue(Number maxValue) {
        mMaxValue = maxValue;
        mUpdateHandler.sendEmptyMessage(0);
    }

    public void setMinValue(Number minValue) {
        mMinValue = minValue;
        mUpdateHandler.sendEmptyMessage(0);
    }

    public void setSeekBarOnTouchListener(OnTouchListener listener) {
        mSeekBar.setOnTouchListener(listener);
    }

    public void setRange(int range) {
        mSeekBar.setMax(range);
    }

    public void setValue(Number value) {
        // TODO
    }
    //endregion Methods

    //region Protected methods
    protected void update() {
        final DecimalFormat format = getDecimalFormat();
        mMinValueText.setText(format.format(mMinValue));
        mMaxValueText.setText(format.format(mMaxValue));

        final double value;
        final int max = mSeekBar.getMax();
        if (max == 0) {
            value = 0;
        } else {
            final BigDecimal pct = BigDecimal.valueOf((double) mSeekBar.getProgress() / max);
            final BigDecimal minValue = new BigDecimal(mMinValue.toString());
            final BigDecimal maxValue = new BigDecimal(mMaxValue.toString());
            final BigDecimal diff = maxValue.subtract(minValue);
            value = minValue.add(diff.multiply(pct)).doubleValue();
        }
        mValueText.setText(format.format(value));
    }
    //endregion Protected methods

    //region Private methods
    @SuppressWarnings("SameParameterValue")
    private void initSliderLayout(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {

        final Resources resources = context.getResources();
        final int standardMargin =
                resources.getDimensionPixelOffset(R.dimen.standard_margin);
        final int valueWidth =
                resources.getDimensionPixelSize(R.dimen.extended_seek_bar_value_view_width);

        // Label text
        mLabelText = new AppCompatTextView(context);
        mLabelText.setId(R.id.text_label);
        addView(mLabelText);

        // Value text
        mValueText = new AppCompatTextView(context);
        mValueText.setId(R.id.text_value);
        addView(mValueText);

        // Min value text
        mMinValueText = new AppCompatTextView(context);
        mMinValueText.setId(R.id.text_min_value);
        mMinValueText.setGravity(GravityCompat.END);
        ViewCompat.setTextAlignment(mMinValueText, ViewCompat.TEXT_ALIGNMENT_GRAVITY);
        addView(mMinValueText);

        // Max value text
        mMaxValueText = new AppCompatTextView(context);
        mMaxValueText.setId(R.id.text_max_value);
        mMaxValueText.setGravity(GravityCompat.END);
        ViewCompat.setTextAlignment(mMaxValueText, ViewCompat.TEXT_ALIGNMENT_GRAVITY);
        addView(mMaxValueText);

        // Seek bar
        mSeekBar = new CustomAppCompatSeekBar(context);
        mSeekBar.setId(R.id.seek_value);
        addView(mSeekBar);

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.constrainWidth(R.id.text_label, LayoutParams.WRAP_CONTENT);
        constraintSet.constrainHeight(R.id.text_label, LayoutParams.WRAP_CONTENT);
        constraintSet.connect(
                R.id.text_label,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START);
        constraintSet.connect(R.id.text_label,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP);

        constraintSet.constrainWidth(R.id.text_value, LayoutParams.WRAP_CONTENT);
        constraintSet.constrainHeight(R.id.text_value, LayoutParams.WRAP_CONTENT);
        constraintSet.connect(
                R.id.text_value,
                ConstraintSet.START,
                R.id.text_label,
                ConstraintSet.END,
                standardMargin);
        constraintSet.connect(
                R.id.text_value,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP);

        constraintSet.constrainWidth(R.id.text_min_value, valueWidth);
        constraintSet.constrainHeight(R.id.text_min_value, LayoutParams.WRAP_CONTENT);
        constraintSet.connect(
                R.id.text_min_value,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START);
        constraintSet.connect(
                R.id.text_min_value,
                ConstraintSet.TOP,
                R.id.text_label,
                ConstraintSet.BOTTOM,
                standardMargin);

        constraintSet.constrainWidth(R.id.text_max_value, valueWidth);
        constraintSet.constrainHeight(R.id.text_max_value, LayoutParams.WRAP_CONTENT);
        constraintSet.connect(
                R.id.text_max_value,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END);
        constraintSet.connect(
                R.id.text_max_value,
                ConstraintSet.TOP,
                R.id.text_label,
                ConstraintSet.BOTTOM,
                standardMargin);

        constraintSet.constrainWidth(R.id.seek_value, 0);
        constraintSet.constrainHeight(R.id.seek_value, LayoutParams.WRAP_CONTENT);
        constraintSet.connect(
                R.id.seek_value,
                ConstraintSet.START,
                R.id.text_min_value,
                ConstraintSet.END,
                standardMargin);
        constraintSet.connect(
                R.id.seek_value,
                ConstraintSet.END,
                R.id.text_max_value,
                ConstraintSet.START,
                standardMargin);
        constraintSet.connect(
                R.id.seek_value,
                ConstraintSet.TOP,
                R.id.text_label,
                ConstraintSet.BOTTOM,
                standardMargin);

        constraintSet.applyTo(this);

        final TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.SeekBarLayout,
                defStyleAttr,
                defStyleRes);

        final String label = a.getString(R.styleable.SeekBarLayout_android_label);
        setLabelText(label);

        final int range = a.getInteger(R.styleable.SeekBarLayout_range, 100);
        setRange(range);

        a.recycle();

        mSeekBar.setOnSeekBarChangeListener(this);
    }
    //endregion Private methods
}
