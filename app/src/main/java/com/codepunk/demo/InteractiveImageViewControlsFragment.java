package com.codepunk.demo;

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
    private static final String KEY_TRANSFORM_INFO = CLASS_NAME + ".transformInfo";

    //endregion Constants

    //region Fields

    private List<Integer> mImageEntryValues;
    private List<String> mScaleTypeEntryValues;

    private Spinner mImageSpinner;
    private Spinner mScaleTypeSpinner;
    private ToggleButton mLockButton;
    private FloatSeekBarLayout mScaleXSeekBarLayout;
    private FloatSeekBarLayout mScaleYSeekBarLayout;
    private FloatSeekBarLayout mPivotXSeekBarLayout;
    private FloatSeekBarLayout mPivotYSeekBarLayout;
    private DemoInteractiveImageView mImageView;

    private boolean mDisallowUpdatingSeekBars = false;

    private boolean mPendingResetClamps = true;

    private InteractiveImageView.TransformInfo mTransformInfo;

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
        mPivotXSeekBarLayout = view.findViewById(R.id.layout_seek_bar_center_x);
        mPivotYSeekBarLayout = view.findViewById(R.id.layout_seek_bar_center_y);

        mScaleXSeekBarLayout.setOnSeekBarChangeListener(this);
        mScaleYSeekBarLayout.setOnSeekBarChangeListener(this);
        mLockButton.setOnClickListener(this);
        mPivotXSeekBarLayout.setOnSeekBarChangeListener(this);
        mPivotYSeekBarLayout.setOnSeekBarChangeListener(this);

        // Prevent drawer from intercepting touch event from seek bars
        mScaleXSeekBarLayout.setSeekBarOnTouchListener(this);
        mScaleYSeekBarLayout.setSeekBarOnTouchListener(this);
        mPivotXSeekBarLayout.setSeekBarOnTouchListener(this);
        mPivotYSeekBarLayout.setSeekBarOnTouchListener(this);

        // Initialize center seek bars
        mPivotXSeekBarLayout.setMinValue(0.0f);
        mPivotYSeekBarLayout.setMinValue(0.0f);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState == null) {
            mImageView.setImageResource(DEFAULT_DRAWABLE_RES_ID);
            final int position = mImageEntryValues.indexOf(DEFAULT_DRAWABLE_RES_ID);
            mImageSpinner.setSelection(position, false);
            mTransformInfo = new InteractiveImageView.TransformInfo();
        } else {
            setImageResourceByPosition(mImageSpinner.getSelectedItemPosition());
            mTransformInfo = savedInstanceState.getParcelable(KEY_TRANSFORM_INFO);
            if (mTransformInfo != null) {
                mScaleXSeekBarLayout.setValue(mTransformInfo.sx);
                mScaleYSeekBarLayout.setValue(mTransformInfo.sy);
                mPivotXSeekBarLayout.setValue(mTransformInfo.px);
                mPivotYSeekBarLayout.setValue(mTransformInfo.py);
                mImageView.transformImage(mTransformInfo);
            }
            mLockButton.setChecked(
                    savedInstanceState.getBoolean(KEY_SCALE_LOCKED,false));
        }

        mImageSpinner.setOnItemSelectedListener(InteractiveImageViewControlsFragment.this);
        mScaleTypeSpinner.setOnItemSelectedListener(InteractiveImageViewControlsFragment.this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        final InteractiveImageView.TransformInfo info = new InteractiveImageView.TransformInfo();
        mImageView.getTransformInfo(info);
        outState.putParcelable(KEY_TRANSFORM_INFO, mTransformInfo);
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
            mDisallowUpdatingSeekBars = true;
            final int id = seekBarLayout.getId();
            switch (id) {
                case R.id.layout_seek_bar_center_x: {
                    mTransformInfo.px = seekBarLayout.getValue();
                    break;
                }
                case R.id.layout_seek_bar_center_y: {
                    mTransformInfo.py = seekBarLayout.getValue();
                    break;
                }
                case R.id.layout_seek_bar_scale_x: {
                    final float oldSx = mTransformInfo.sx;
                    mTransformInfo.sx = seekBarLayout.getValue();
                    if (mLockButton.isChecked()) {
                        mTransformInfo.sy *= mTransformInfo.sx / oldSx;
                        mScaleYSeekBarLayout.setValue(mTransformInfo.sy);
                    }
                    break;
                }
                case R.id.layout_seek_bar_scale_y: {
                    final float oldSy = mTransformInfo.sy;
                    mTransformInfo.sy = seekBarLayout.getValue();
                    if (mLockButton.isChecked()) {
                        mTransformInfo.sx *= mTransformInfo.sy / oldSy;
                        mScaleXSeekBarLayout.setValue(mTransformInfo.sx);
                    }
                    break;
                }
                default:
                    mDisallowUpdatingSeekBars = false;
                    return;
            }
            mImageView.transformImage(mTransformInfo);
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

        mImageView.getTransformInfo(mTransformInfo);

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
        mPivotXSeekBarLayout.setMaxValue((float) mImageView.getDrawableIntrinsicWidth());
        mPivotYSeekBarLayout.setMaxValue((float) mImageView.getDrawableIntrinsicHeight());

        if (mDisallowUpdatingSeekBars) {
            mDisallowUpdatingSeekBars = false;
        } else {
            mScaleXSeekBarLayout.setValue(mTransformInfo.sx, false);
            mScaleYSeekBarLayout.setValue(mTransformInfo.sy, false);
            mPivotXSeekBarLayout.setValue(mTransformInfo.px, false);
            mPivotYSeekBarLayout.setValue(mTransformInfo.py, false);
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
        final int id = view.getId();
        switch (id) {
            case R.id.btn_lock:
                resetClamps();
                break;
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
