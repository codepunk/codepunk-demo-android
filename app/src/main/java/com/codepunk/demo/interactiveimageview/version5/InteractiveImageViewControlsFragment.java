package com.codepunk.demo.interactiveimageview.version5;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView.ScaleType;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.codepunk.demo.AbsSeekBarLayout;
import com.codepunk.demo.FloatSeekBarLayout;
import com.codepunk.demo.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InteractiveImageViewControlsFragment extends Fragment
        implements AbsSeekBarLayout.OnSeekBarChangeListener<Float>,
                AdapterView.OnItemSelectedListener,
                DemoInteractiveImageView.DemoInteractiveImageViewListener,
                View.OnClickListener,
                View.OnTouchListener {
    //region Constants
    private static final String LOG_TAG =
            InteractiveImageViewControlsFragment.class.getSimpleName();

    private static final String CLASS_NAME = InteractiveImageViewControlsFragment.class.getName();
    private static final String KEY_SCALE_LOCKED = CLASS_NAME + ".scaleLocked";
    //endregion Constants

    //region Fields
    private List<Integer> mImageEntryValues;
    private List<String> mScaleTypeEntryValues;

    private Spinner mImageSpinner;
    private Spinner mScaleTypeSpinner;
    private FloatSeekBarLayout mCenterXSeekBarLayout;
    private FloatSeekBarLayout mCenterYSeekBarLayout;
    private ToggleButton mLockButton;
    private FloatSeekBarLayout mScaleXSeekBarLayout;
    private FloatSeekBarLayout mScaleYSeekBarLayout;
    private DemoInteractiveImageView mImageView;

    private final Set<AbsSeekBarLayout> mManipulatedSeekBars = new HashSet<>();

    private boolean mPendingResetClamps = true;
    //endregion Fields

    //region Constructors
    public InteractiveImageViewControlsFragment() {
    }
    //endregion Constructors

    //region Lifecycle methods
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Resources res = getResources();
        TypedArray array = res.obtainTypedArray(R.array.image_res_ids);
        final int length = array.length();
        mImageEntryValues = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            mImageEntryValues.add(array.getResourceId(i, 0));
        }
        array.recycle();
        final String[] scaleTypes = res.getStringArray(R.array.scale_type_values);
        mScaleTypeEntryValues = new ArrayList<>(Arrays.asList(scaleTypes));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(
                R.layout.fragment_interactive_image_view_controls,
                container,
                false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mImageSpinner = view.findViewById(R.id.spinner_image);
        mScaleTypeSpinner = view.findViewById(R.id.spinner_scale_type);
        mCenterXSeekBarLayout = view.findViewById(R.id.layout_seek_bar_center_x);
        mCenterYSeekBarLayout = view.findViewById(R.id.layout_seek_bar_center_y);
        mLockButton = view.findViewById(R.id.btn_lock);
        mScaleXSeekBarLayout = view.findViewById(R.id.layout_seek_bar_scale_x);
        mScaleYSeekBarLayout = view.findViewById(R.id.layout_seek_bar_scale_y);

        if (savedInstanceState == null) {

        } else {
            mLockButton.setChecked(
                    savedInstanceState.getBoolean(KEY_SCALE_LOCKED,
                            false));
        }

        mImageSpinner.setOnItemSelectedListener(this);
        mScaleTypeSpinner.setOnItemSelectedListener(this);
        mCenterXSeekBarLayout.setOnSeekBarChangeListener(this);
        mCenterYSeekBarLayout.setOnSeekBarChangeListener(this);
        mScaleXSeekBarLayout.setOnSeekBarChangeListener(this);
        mScaleYSeekBarLayout.setOnSeekBarChangeListener(this);
        mLockButton.setOnClickListener(this);

        // Prevent drawer from intercepting touch event from seek bars
        mCenterXSeekBarLayout.setSeekBarOnTouchListener(this);
        mCenterYSeekBarLayout.setSeekBarOnTouchListener(this);
        mScaleXSeekBarLayout.setSeekBarOnTouchListener(this);
        mScaleYSeekBarLayout.setSeekBarOnTouchListener(this);

        // Initialize center seek bars
        mCenterXSeekBarLayout.setMinValue(0.0f);
        mCenterXSeekBarLayout.setMaxValue(1.0f);
        mCenterYSeekBarLayout.setMinValue(0.0f);
        mCenterYSeekBarLayout.setMaxValue(1.0f);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SCALE_LOCKED, mLockButton.isChecked());
    }

    //endregion Lifecycle methods

    //region Implemented methods
    @Override // AbsSeekBarLayout.OnSeekBarChangeListener
    public void onProgressChanged(
            AbsSeekBarLayout<Float> seekBarLayout,
            int progress,
            boolean fromUser) {
        if (fromUser) {
            final int id = seekBarLayout.getId();
            float sx = mImageView.getImageScaleX();
            float sy = mImageView.getImageScaleY();
            float cx = mImageView.getImageCenterX();
            float cy = mImageView.getImageCenterY();
            switch (id) {
                case R.id.layout_seek_bar_center_x: {
                    mManipulatedSeekBars.add(mCenterXSeekBarLayout);
                    mManipulatedSeekBars.add(mCenterYSeekBarLayout);
                    cx = seekBarLayout.getValue();
                    break;
                }
                case R.id.layout_seek_bar_center_y: {
                    mManipulatedSeekBars.add(mCenterXSeekBarLayout);
                    mManipulatedSeekBars.add(mCenterYSeekBarLayout);
                    cy = seekBarLayout.getValue();
                    break;
                }
                case R.id.layout_seek_bar_scale_x: {
                    mManipulatedSeekBars.add(mScaleXSeekBarLayout);
                    mManipulatedSeekBars.add(mScaleYSeekBarLayout);
                    final float oldSx = sx;
                    sx = seekBarLayout.getValue();
                    if (mLockButton.isChecked()) {
                        sy *= sx / oldSx;
                        mScaleYSeekBarLayout.setValue(sy);
                    }
                    break;
                }
                case R.id.layout_seek_bar_scale_y: {
                    mManipulatedSeekBars.add(mScaleXSeekBarLayout);
                    mManipulatedSeekBars.add(mScaleYSeekBarLayout);
                    final float oldSy = sy;
                    sy = seekBarLayout.getValue();
                    if (mLockButton.isChecked()) {
                        sx *= sy / oldSy;
                        mScaleXSeekBarLayout.setValue(sx);
                    }
                    break;
                }
                default:
                    return;
            }
            mImageView.setLayout(sx, sy, cx, cy);
        }
    }

    @Override // AbsSeekBarLayout.OnSeekBarChangeListener
    public void onStartTrackingTouch(AbsSeekBarLayout<Float> seekBarLayout) {

    }

    @Override // AbsSeekBarLayout.OnSeekBarChangeListener
    public void onStopTrackingTouch(AbsSeekBarLayout<Float> seekBarLayout) {

    }

    @Override // AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.spinner_image:
                mPendingResetClamps = true;
                if (mImageView != null && position >= 0 && position < mImageEntryValues.size()) {
                    mPendingResetClamps = true;
                    mImageView.setImageResource(mImageEntryValues.get(position));
                }
                break;
            case R.id.spinner_scale_type:
                mPendingResetClamps = true;
                if (mImageView != null && position >= 0 && position < mScaleTypeEntryValues.size()) {
                    mPendingResetClamps = true;
                    mImageView.setScaleType(
                            ScaleType.valueOf(mScaleTypeEntryValues.get(position)));
                }
                break;
        }
    }

    @Override // AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> view) {

    }

    @Override // DemoInteractiveImageView.DemoInteractiveImageViewListener
    public void onDraw(InteractiveImageView view, Canvas canvas) {
        updateScaleTypeSpinner();

        if (mPendingResetClamps) {
            mScaleXSeekBarLayout.setClampedMin(Integer.MIN_VALUE);
            mScaleXSeekBarLayout.setClampedMax(Integer.MAX_VALUE);
            mScaleYSeekBarLayout.setClampedMin(Integer.MIN_VALUE);
            mScaleYSeekBarLayout.setClampedMax(Integer.MAX_VALUE);
        }

        mScaleXSeekBarLayout.setMinValue(mImageView.getImageMinScaleX());
        mScaleXSeekBarLayout.setMaxValue(mImageView.getImageMaxScaleX());
        mScaleYSeekBarLayout.setMinValue(mImageView.getImageMinScaleY());
        mScaleYSeekBarLayout.setMaxValue(mImageView.getImageMaxScaleY());

        if (!mManipulatedSeekBars.remove(mCenterXSeekBarLayout)) {
            mCenterXSeekBarLayout.setValue(mImageView.getImageCenterX(), true);
        }
        if (!mManipulatedSeekBars.remove(mCenterYSeekBarLayout)) {
            mCenterYSeekBarLayout.setValue(mImageView.getImageCenterY(), true);
        }
        if (!mManipulatedSeekBars.remove(mScaleXSeekBarLayout)) {
            mScaleXSeekBarLayout.setValue(mImageView.getImageScaleX(), true);
        }
        if (!mManipulatedSeekBars.remove(mScaleYSeekBarLayout)) {
            mScaleYSeekBarLayout.setValue(mImageView.getImageScaleY(), true);
        }

        if (mPendingResetClamps) {
            mPendingResetClamps = false;
            resetClamps();
        }
    }

    @Override // DemoInteractiveImageView.DemoInteractiveImageViewListener
    public void onInteractionBegin(InteractiveImageView view) {
        final View mainView = getView();
        if (mainView != null) {
            mainView.setAlpha(0.5f);
        }
    }

    @Override // DemoInteractiveImageView.DemoInteractiveImageViewListener
    public void onInteractionEnd(InteractiveImageView view) {
        final View mainView = getView();
        if (mainView != null) {
            mainView.setAlpha(1.0f);
        }
    }

    @Override // DemoInteractiveImageView.DemoInteractiveImageViewListener
    public void onSetImageResource(InteractiveImageView view, int resId) {
        mImageSpinner.setSelection(mImageEntryValues.indexOf(resId), false);
    }

    @Override
    public void onClick(View view) {
        if (view == mLockButton) {
            resetClamps();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override // View.OnTouchListener
    public boolean onTouch(View view, MotionEvent event) {
        view.getParent().requestDisallowInterceptTouchEvent(true);
        return false;
    }
    //endregion Implemented methods

    //region Methods
    public void setImageView(DemoInteractiveImageView imageView) {
        mImageView = imageView;
        mImageView.setDemoInteractiveImageViewListener(this);
        updateScaleTypeSpinner();
    }
    //endregion Methods

    //region Private methods
    private void resetClamps() {
        final float minScaleX = mImageView.getImageMinScaleX();
        final float maxScaleX = mImageView.getImageMaxScaleX();
        final float minScaleY = mImageView.getImageMinScaleY();
        final float maxScaleY = mImageView.getImageMaxScaleY();
        final int scaleXClampedMin;
        final int scaleXClampedMax;
        final int scaleYClampedMin;
        final int scaleYClampedMax;
        if (mLockButton.isChecked()) {
            final float currentScaleX = mScaleXSeekBarLayout.getValue();
            final float currentScaleY = mScaleYSeekBarLayout.getValue();
            final float shrinkFactor;
            final float growFactor;
            if (mScaleXSeekBarLayout.getRelativeProgress() <
                    mScaleYSeekBarLayout.getRelativeProgress()) {
                shrinkFactor = currentScaleX / minScaleX;
                growFactor = maxScaleY / currentScaleY;
                scaleXClampedMin = Integer.MIN_VALUE;
                scaleXClampedMax =
                        mScaleXSeekBarLayout.valueToProgress(currentScaleX * growFactor);
                scaleYClampedMin =
                        mScaleYSeekBarLayout.valueToProgress(currentScaleY / shrinkFactor);
                scaleYClampedMax = Integer.MAX_VALUE;
            } else {
                shrinkFactor = currentScaleY / minScaleY;
                growFactor = maxScaleX / currentScaleX;
                scaleXClampedMin =
                        mScaleXSeekBarLayout.valueToProgress(currentScaleX / shrinkFactor);
                scaleXClampedMax = Integer.MAX_VALUE;
                scaleYClampedMin = Integer.MIN_VALUE;
                scaleYClampedMax =
                        mScaleYSeekBarLayout.valueToProgress(currentScaleY * growFactor);
            }
        } else {
            scaleXClampedMin = scaleYClampedMin = Integer.MIN_VALUE;
            scaleXClampedMax = scaleYClampedMax = Integer.MAX_VALUE;
        }
        mScaleXSeekBarLayout.setClampedMin(scaleXClampedMin);
        mScaleXSeekBarLayout.setClampedMax(scaleXClampedMax);
        mScaleYSeekBarLayout.setClampedMin(scaleYClampedMin);
        mScaleYSeekBarLayout.setClampedMax(scaleYClampedMax);
    }

    private void updateScaleTypeSpinner() {
        if (mImageView != null) {
            final ScaleType scaleType = mImageView.getScaleType();
            final int position = mScaleTypeEntryValues.indexOf(scaleType.name());
            mScaleTypeSpinner.setSelection(position, false);
        }
    }
    //endregion Methods
}
