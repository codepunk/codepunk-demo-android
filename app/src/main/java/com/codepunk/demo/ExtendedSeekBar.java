package com.codepunk.demo;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import java.text.Format;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;

public class ExtendedSeekBar extends LinearLayout {

    final static class Initializer {
        AppCompatImageView mIconView;
        AppCompatTextView mValueView;
        AppCompatTextView mMinValueView;
        AppCompatSeekBar mSeekBar;
        AppCompatTextView mMaxValueView;
    }

    final AppCompatImageView mIconView;
    final AppCompatTextView mValueView;
    final AppCompatTextView mMinValueView;
    final AppCompatSeekBar mSeekBar;
    final AppCompatTextView mMaxValueView;

    private Format mFormat;

    public ExtendedSeekBar(Context context) {
        super(context);
        final Initializer initializer = initExtendedSeekBar(
                context,
                null,
                R.attr.extendedSeekBarStyle,
                0);
        mIconView = initializer.mIconView;
        mValueView = initializer.mValueView;
        mMinValueView = initializer.mMinValueView;
        mSeekBar = initializer.mSeekBar;
        mMaxValueView = initializer.mMaxValueView;
    }

    public ExtendedSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        final Initializer initializer = initExtendedSeekBar(
                context,
                attrs,
                R.attr.extendedSeekBarStyle,
                0);
        mIconView = initializer.mIconView;
        mValueView = initializer.mValueView;
        mMinValueView = initializer.mMinValueView;
        mSeekBar = initializer.mSeekBar;
        mMaxValueView = initializer.mMaxValueView;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public ExtendedSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final Initializer initializer = initExtendedSeekBar(context, attrs, defStyleAttr, 0);
        mIconView = initializer.mIconView;
        mValueView = initializer.mValueView;
        mMinValueView = initializer.mMinValueView;
        mSeekBar = initializer.mSeekBar;
        mMaxValueView = initializer.mMaxValueView;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ExtendedSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        final Initializer initializer =
                initExtendedSeekBar(context, attrs, defStyleAttr, defStyleRes);
        mIconView = initializer.mIconView;
        mValueView = initializer.mValueView;
        mMinValueView = initializer.mMinValueView;
        mSeekBar = initializer.mSeekBar;
        mMaxValueView = initializer.mMaxValueView;
    }

    private Initializer initExtendedSeekBar(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super.setOrientation(HORIZONTAL);

        final Initializer initializer = new Initializer();

        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.ExtendedSeekBar,
                defStyleAttr,
                defStyleRes);

        final int iconResId = a.getResourceId(R.styleable.ExtendedSeekBar_android_icon, -1);
        final int valueViewWidth = a.getDimensionPixelSize(
                R.styleable.ExtendedSeekBar_valueViewWidth,
                -1);

        a.recycle();

        LayoutParams lp = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        initializer.mIconView = new AppCompatImageView(context);
        initializer.mIconView.setId(R.id.image_icon);
        if (iconResId != -1) {
            initializer.mIconView.setImageResource(iconResId);
        }
        addView(initializer.mIconView, lp);

        initializer.mValueView = new AppCompatTextView(context);
        initializer.mValueView.setId(R.id.text_value);
        lp = new LayoutParams(valueViewWidth == -1 ? WRAP_CONTENT : valueViewWidth, WRAP_CONTENT);
        addView(initializer.mValueView, lp);

        View divider = new View(context);
        lp = new LayoutParams(2, MATCH_PARENT);
        @Px int margin = getResources().getDimensionPixelOffset(R.dimen.small_margin);
        lp.setMargins(margin, margin, margin, margin);
        divider.setBackgroundColor(Color.WHITE);
        addView(divider, lp);

        initializer.mMinValueView = new AppCompatTextView(context);
        initializer.mMinValueView.setId(R.id.text_min_value);
        initializer.mMinValueView.setGravity(Gravity.END);
        ViewCompat.setTextAlignment(initializer.mMinValueView, ViewCompat.TEXT_ALIGNMENT_GRAVITY);
        lp = new LayoutParams(valueViewWidth == -1 ? WRAP_CONTENT : valueViewWidth, WRAP_CONTENT);
        addView(initializer.mMinValueView, lp);

        initializer.mSeekBar = new AppCompatSeekBar(context);
        initializer.mSeekBar.setId(R.id.seek_value);
        initializer.mSeekBar.setMax(100);
        lp = new LayoutParams(0, WRAP_CONTENT, 1);
        addView(initializer.mSeekBar, lp);

        initializer.mMaxValueView = new AppCompatTextView(context);
        initializer.mMaxValueView.setId(R.id.text_max_value);
        initializer.mMaxValueView.setGravity(Gravity.START);
        ViewCompat.setTextAlignment(initializer.mMaxValueView, ViewCompat.TEXT_ALIGNMENT_GRAVITY);
        lp = new LayoutParams(valueViewWidth == -1 ? WRAP_CONTENT : valueViewWidth, WRAP_CONTENT);
        addView(initializer.mMaxValueView, lp);

        /* TODO TEMP */ initializer.mValueView.setText("50%");
        /* TODO TEMP */ initializer.mMinValueView.setText("0%");
        /* TODO TEMP */ initializer.mMaxValueView.setText("100%");

        return initializer;
    }

    @Override
    public void setOrientation(int orientation) {
        // Prohibit this functionality
    }

    public Format getFormat() {
        return mFormat;
    }

    public void setFormat(Format format) {
        mFormat = format;
    }
}
