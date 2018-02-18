package com.codepunk.demo.interactiveimageview.version6;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
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
import java.util.List;

public class InteractiveImageViewControlsFragment extends Fragment
        implements AbsSeekBarLayout.OnSeekBarChangeListener<Float>,
                AdapterView.OnItemSelectedListener,
                DemoInteractiveImageView.DemoInteractiveImageViewListener,
                View.OnClickListener,
                View.OnTouchListener {
    //region Constants
    private static final String LOG_TAG =
            InteractiveImageViewControlsFragment.class.getSimpleName();
    private static final @DrawableRes int DEFAULT_DRAWABLE_RES_ID = R.drawable.wilderness_lodge;

    private static final String CLASS_NAME = InteractiveImageViewControlsFragment.class.getName();
    private static final String KEY_SCALE_LOCKED = CLASS_NAME + ".scaleLocked";
    private static final String KEY_SX = CLASS_NAME + ".sx";
    private static final String KEY_SY = CLASS_NAME + ".sy";
    private static final String KEY_CX = CLASS_NAME + ".cx";
    private static final String KEY_CY = CLASS_NAME + ".cy";
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

    private boolean mDisallowUpdatingSeekBars = false;

    private boolean mPendingResetClamps = true;

    private float mSx;
    private float mSy;
    private float mCx;
    private float mCy;
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
        mScaleXSeekBarLayout = view.findViewById(R.id.layout_seek_bar_scale_x);
        mScaleYSeekBarLayout = view.findViewById(R.id.layout_seek_bar_scale_y);
        mLockButton = view.findViewById(R.id.btn_lock);
        mCenterXSeekBarLayout = view.findViewById(R.id.layout_seek_bar_center_x);
        mCenterYSeekBarLayout = view.findViewById(R.id.layout_seek_bar_center_y);

        mScaleXSeekBarLayout.setOnSeekBarChangeListener(this);
        mScaleYSeekBarLayout.setOnSeekBarChangeListener(this);
        mLockButton.setOnClickListener(this);
        mCenterXSeekBarLayout.setOnSeekBarChangeListener(this);
        mCenterYSeekBarLayout.setOnSeekBarChangeListener(this);

        // Prevent drawer from intercepting touch event from seek bars
        mScaleXSeekBarLayout.setSeekBarOnTouchListener(this);
        mScaleYSeekBarLayout.setSeekBarOnTouchListener(this);
        mCenterXSeekBarLayout.setSeekBarOnTouchListener(this);
        mCenterYSeekBarLayout.setSeekBarOnTouchListener(this);

        // Initialize center seek bars
        mCenterXSeekBarLayout.setMinValue(0.0f);
        mCenterXSeekBarLayout.setMaxValue(1.0f);
        mCenterYSeekBarLayout.setMinValue(0.0f);
        mCenterYSeekBarLayout.setMaxValue(1.0f);

    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState == null) {
            mImageView.setImageResource(DEFAULT_DRAWABLE_RES_ID);
            final int position = mImageEntryValues.indexOf(DEFAULT_DRAWABLE_RES_ID);
            mImageSpinner.setSelection(position, false);
        } else {
            mLockButton.setChecked(
                    savedInstanceState.getBoolean(KEY_SCALE_LOCKED,false));
            setImageResourceByPosition(mImageSpinner.getSelectedItemPosition());
            mSx = savedInstanceState.getFloat(KEY_SX, 0.0f);
            mSy = savedInstanceState.getFloat(KEY_SY, 0.0f);
            mCx = savedInstanceState.getFloat(KEY_CX, 0.0f);
            mCy = savedInstanceState.getFloat(KEY_CY, 0.0f);
            mScaleXSeekBarLayout.setValue(mSx);
            mScaleYSeekBarLayout.setValue(mSy);
            mCenterXSeekBarLayout.setValue(mCx);
            mCenterYSeekBarLayout.setValue(mCy);
            mImageView.setScaleAndCenter(mSx, mSy, mCx, mCy);
        }

        mImageSpinner.setOnItemSelectedListener(InteractiveImageViewControlsFragment.this);
        mScaleTypeSpinner.setOnItemSelectedListener(InteractiveImageViewControlsFragment.this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SCALE_LOCKED, mLockButton.isChecked());
        outState.putFloat(KEY_SX, mSx);
        outState.putFloat(KEY_SY, mSy);
        outState.putFloat(KEY_CX, mCx);
        outState.putFloat(KEY_CY, mCy);
    }
    //endregion Lifecycle methods

    //region Implemented methods
    @Override // AbsSeekBarLayout.OnSeekBarChangeListener
    public void onProgressChanged(
            AbsSeekBarLayout<Float> seekBarLayout,
            int progress,
            boolean fromUser) {
        if (fromUser) {
            mDisallowUpdatingSeekBars = true;
            final int id = seekBarLayout.getId();
            switch (id) {
                case R.id.layout_seek_bar_center_x: {
                    mCx = seekBarLayout.getValue();
                    break;
                }
                case R.id.layout_seek_bar_center_y: {
                    mCy = seekBarLayout.getValue();
                    break;
                }
                case R.id.layout_seek_bar_scale_x: {
                    final float oldSx = mSx;
                    mSx = seekBarLayout.getValue();
                    if (mLockButton.isChecked()) {
                        mSy *= mSx / oldSx;
                        mScaleYSeekBarLayout.setValue(mSy);
                    }
                    break;
                }
                case R.id.layout_seek_bar_scale_y: {
                    final float oldSy = mSy;
                    mSy = seekBarLayout.getValue();
                    if (mLockButton.isChecked()) {
                        mSx *= mSy / oldSy;
                        mScaleXSeekBarLayout.setValue(mSx);
                    }
                    break;
                }
                default:
                    mDisallowUpdatingSeekBars = false;
                    return;
            }
            mImageView.setScaleAndCenter(mSx, mSy, mCx, mCy);
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
                if (setImageResourceByPosition(position)) {
                    mPendingResetClamps = true;
                }
                break;
            case R.id.spinner_scale_type:
                if (setImageScaleTypeByPosition(position)) {
                    mPendingResetClamps = true;
                }
                break;
        }
    }

    @Override // AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> view) {

    }

    @Override // DemoInteractiveImageView.DemoInteractiveImageViewListener
    public void onDraw(InteractiveImageView view, Canvas canvas) {
        final int position = mScaleTypeEntryValues.indexOf(mImageView.getScaleType().name());
        if (position != mScaleTypeSpinner.getSelectedItemPosition()) {
            mScaleTypeSpinner.setSelection(position, false);
        }

        mSx = mImageView.getImageScaleX();
        mSy = mImageView.getImageScaleY();
        mCx = mImageView.getImageCenterX();
        mCy = mImageView.getImageCenterY();

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

        if (mDisallowUpdatingSeekBars) {
            mDisallowUpdatingSeekBars = false;
        } else {
            mScaleXSeekBarLayout.setValue(mSx, false);
            mScaleYSeekBarLayout.setValue(mSy, false);
            mCenterXSeekBarLayout.setValue(mCx, false);
            mCenterYSeekBarLayout.setValue(mCy, false);
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

    private boolean setImageResourceByPosition(int position) {
        if (mImageView != null && position >= 0 && position < mImageEntryValues.size()) {
            final @DrawableRes int drawableResId = mImageEntryValues.get(position);
            mImageView.setImageResource(drawableResId);
            return true;
        }
        return false;
    }

    private boolean setImageScaleTypeByPosition(int position) {
        if (mImageView != null && position >= 0 && position < mScaleTypeEntryValues.size()) {
            final ScaleType scaleType = ScaleType.valueOf(mScaleTypeEntryValues.get(position));
            if (mImageView.getScaleType() != scaleType) {
                mImageView.setScaleType(scaleType);
                return true;
            }
        }
        return false;
    }
    //endregion Methods
}