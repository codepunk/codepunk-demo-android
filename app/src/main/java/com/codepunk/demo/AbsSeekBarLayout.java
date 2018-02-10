package com.codepunk.demo;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.view.GravityCompat;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.widget.SeekBar;

import com.codepunk.demo.support.ProgressBarCompat;
import com.codepunk.demo.support.ViewCompat;

import java.text.DecimalFormat;

@SuppressWarnings("unused")
public abstract class AbsSeekBarLayout<T extends Number> extends ConstraintLayout
        implements Handler.Callback,
        SeekBar.OnSeekBarChangeListener {

    //region Constants
    private static final String LOG_TAG = AbsSeekBarLayout.class.getSimpleName();
    private static final DecimalFormat DEFAULT_DECIMAL_FORMAT = new DecimalFormat();
    //endregion Constants

    //region Nested classes
    public interface OnSeekBarChangeListener<T extends Number> {
        void onProgressChanged(AbsSeekBarLayout<T> seekBarLayout, int progress, boolean fromUser);
        void onStartTrackingTouch(AbsSeekBarLayout<T> seekBarLayout);
        void onStopTrackingTouch(AbsSeekBarLayout<T> seekBarLayout);
    }
    //endregion Nested classes

    //region Fields
    protected AppCompatTextView mLabelText;
    protected AppCompatTextView mValueText;
    protected AppCompatTextView mMinValueText;
    protected AppCompatTextView mMaxValueText;
    protected CustomAppCompatSeekBar mSeekBar;

    protected DecimalFormat mDecimalFormat = null;

    protected T mMinValue;
    protected T mMaxValue;
    protected T mValue;

    protected boolean mValueDirty = true;

    private Handler mUpdateHandler = new Handler(this);
    private OnSeekBarChangeListener<T> mOnSeekBarChangeListener;
    //endregion Fields

    //region Constructors
    public AbsSeekBarLayout(Context context) {
        super(context);
        initAbsSeekBarLayout(context, null, R.attr.absSeekBarLayoutStyle, 0);
    }

    public AbsSeekBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAbsSeekBarLayout(context, attrs, R.attr.absSeekBarLayoutStyle, 0);
    }

    public AbsSeekBarLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAbsSeekBarLayout(context, attrs, defStyleAttr, 0);
    }
    //endregion Constructors

    //region Implemented methods
    @Override
    public boolean handleMessage(Message message) {
        final boolean valueDirty = mValueDirty;
        if (mValueDirty) {
            mValue = progressToValue(
                    mSeekBar.getProgress(),
                    mSeekBar.getMax(),
                    mMinValue,
                    mMaxValue);
            mValueDirty = false;
        }
        updateUI();
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
        mValue = progressToValue(mSeekBar.getProgress(), mSeekBar.getMax(), mMinValue, mMaxValue);
        updateUI();
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onProgressChanged(this, progress, fromUser);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar bar) {
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStartTrackingTouch(this);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar bar) {
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStopTrackingTouch(this);
        }
    }
    //endregion Implemented methods

    //region Methods
    public T getValue() {
        return mValue;
    }

    public void setFormat(String format) {
        mDecimalFormat = (format == null ? null : new DecimalFormat(format));
    }

    public void setLabelText(CharSequence text) {
        mLabelText.setText(text);
    }

    public void setLabelText(int resId) {
        mLabelText.setText(resId);
    }

    public void setMaxValue(T maxValue) {
        mMaxValue = maxValue;
        mValueDirty = true;
        mUpdateHandler.removeMessages(0);
        mUpdateHandler.sendEmptyMessage(0);
    }

    public void setMinValue(T minValue) {
        mMinValue = minValue;
        mValueDirty = true;
        mUpdateHandler.removeMessages(0);
        mUpdateHandler.sendEmptyMessage(0);
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener<T> listener) {
        mOnSeekBarChangeListener = listener;
    }

    public void setSeekBarOnTouchListener(OnTouchListener listener) {
        mSeekBar.setOnTouchListener(listener);
    }

    public void setRange(int range) {
        mSeekBar.setMax(range);
    }

    public void setValue(T value) {
        setValue(value, false);
    }

    public void setValue(T value, boolean animate) {
        mValue = value;
        ProgressBarCompat.setProgress(
                mSeekBar,
                valueToProgress(value, mMinValue, mMaxValue, mSeekBar.getMax()), animate);
    }
    //endregion Methods

    //region Protected methods
    protected void updateUI() {
        final DecimalFormat format = getDecimalFormat();
        mMinValueText.setText(format.format(mMinValue.doubleValue()));
        mMaxValueText.setText(format.format(mMaxValue.doubleValue()));
        mValueText.setText(format.format(mValue.doubleValue()));
    }

    protected abstract T progressToValue(
            int progress,
            int maxProgress,
            @NonNull T minValue,
            @NonNull T maxValue);

    protected abstract int valueToProgress(
            @NonNull T value,
            @NonNull T minValue,
            @NonNull T maxValue,
            int maxProgress);
    //endregion Protected methods

    //region Private methods
    private DecimalFormat getDecimalFormat() {
        return (mDecimalFormat == null ? DEFAULT_DECIMAL_FORMAT : mDecimalFormat);
    }

    @SuppressWarnings("SameParameterValue")
    private void initAbsSeekBarLayout(
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
                R.styleable.AbsSeekBarLayout,
                defStyleAttr,
                defStyleRes);

        final String label = a.getString(R.styleable.AbsSeekBarLayout_android_label);
        setLabelText(label);

        final int range = a.getInteger(R.styleable.AbsSeekBarLayout_range, 100);
        setRange(range);

        if (a.hasValue(R.styleable.AbsSeekBarLayout_android_format)) {
            final String format = a.getString(R.styleable.AbsSeekBarLayout_android_format);
            setFormat(format);
        }

        a.recycle();

        mSeekBar.setOnSeekBarChangeListener(this);
    }
    //endregion Private methods
}
