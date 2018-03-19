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

import static com.codepunk.demo.InteractiveImageView.VIEW_CENTER;

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
    private static final String KEY_IS_TRANSFORMED = CLASS_NAME + ".isTransformed";
    private static final String KEY_SCALEX = CLASS_NAME + ".scaleX";
    private static final String KEY_SCALEY = CLASS_NAME + ".scaleY";
    private static final String KEY_PIVOTX = CLASS_NAME + ".pivotX";
    private static final String KEY_PIVOTY = CLASS_NAME + ".pivotY";

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
        } else {
            // This prevents onItemSelected from being fired immediately after calling
            // setOnItemSelected listener below
            final int position = mImageSpinner.getSelectedItemPosition();
            mImageSpinner.setSelection(position, false);

            mLockButton.setChecked(
                    savedInstanceState.getBoolean(KEY_SCALE_LOCKED,false));

            setImageResourceByPosition(position);
            if (savedInstanceState.getBoolean(KEY_IS_TRANSFORMED, false)) {
                final float sx = savedInstanceState.getFloat(KEY_SCALEX, 1.0f);
                final float sy = savedInstanceState.getFloat(KEY_SCALEY, 1.0f);
                final float px = savedInstanceState.getFloat(KEY_PIVOTX);
                final float py = savedInstanceState.getFloat(KEY_PIVOTY);
                mScaleXSeekBarLayout.setValue(sx);
                mScaleYSeekBarLayout.setValue(sy);
                mPivotXSeekBarLayout.setValue(px);
                mPivotYSeekBarLayout.setValue(py);
                mImageView.transformImage(sx, sy, px, py, VIEW_CENTER, VIEW_CENTER);
            }
        }

        mImageSpinner.setOnItemSelectedListener(InteractiveImageViewControlsFragment.this);
        mScaleTypeSpinner.setOnItemSelectedListener(InteractiveImageViewControlsFragment.this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_SCALE_LOCKED, mLockButton.isChecked());
        if (mImageView.isTransformed()) {
            outState.putBoolean(KEY_IS_TRANSFORMED, true);
            outState.putFloat(KEY_SCALEX, mImageView.getImageScaleX());
            outState.putFloat(KEY_SCALEY, mImageView.getImageScaleY());
            outState.putFloat(KEY_PIVOTX, mImageView.getImagePivotX());
            outState.putFloat(KEY_PIVOTY, mImageView.getImagePivotY());
        } else {
            outState.putBoolean(KEY_IS_TRANSFORMED, false);
        }
        super.onSaveInstanceState(outState);
    }

    //endregion Lifecycle methods

    //region Implemented methods

    @Override // AbsSeekBarLayout.OnSeekBarChangeListener
    public void onValueChanged(
            AbsSeekBarLayout<Float> seekBarLayout,
            Float value,
            boolean fromUser) {
        if (fromUser) {
            mDisallowUpdatingSeekBars = true;
            final int id = seekBarLayout.getId();
            final boolean transformImage;
            float sx = mScaleXSeekBarLayout.getValue();
            float sy = mScaleYSeekBarLayout.getValue();
            float px = mPivotXSeekBarLayout.getValue();
            float py = mPivotYSeekBarLayout.getValue();
            switch (id) {
                case R.id.layout_seek_bar_center_x: {
                    px = value;
                    transformImage = true;
                    break;
                }
                case R.id.layout_seek_bar_center_y: {
                    py = value;
                    transformImage = true;
                    break;
                }
                case R.id.layout_seek_bar_scale_x: {
                    sx = value;
                    if (mLockButton.isChecked()) {
                        final int width = mImageView.getDrawableIntrinsicWidth();
                        final int height = mImageView.getDrawableIntrinsicHeight();
                        final float ratio = (float) width / height;
                        final float scaledHeight = (width * value) / ratio;
                        sy = scaledHeight / height;
                        mScaleYSeekBarLayout.setValue(sy);
                    }
                    transformImage = true;
                    break;
                }
                case R.id.layout_seek_bar_scale_y: {
                    sy = value;
                    if (mLockButton.isChecked()) {
                        final int width = mImageView.getDrawableIntrinsicWidth();
                        final int height = mImageView.getDrawableIntrinsicHeight();
                        final float ratio = (float) width / height;
                        final float scaledWidth = (height * value) * ratio;
                        sx = scaledWidth / width;
                        mScaleXSeekBarLayout.setValue(sx);
                    }
                    transformImage = true;
                    break;
                }
                default:
                    mDisallowUpdatingSeekBars = false;
                    transformImage = false;
            }
            if (transformImage) {
                mImageView.transformImage(sx, sy, px, py, VIEW_CENTER, VIEW_CENTER);
            }
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

        final float scaleX = mImageView.getImageScaleX();
        final float scaleY = mImageView.getImageScaleY();
        final float pivotX = mImageView.getImagePivotX();
        final float pivotY = mImageView.getImagePivotY();

        if (mPendingResetClamps) {
            mScaleXSeekBarLayout.setClampedMin(Integer.MIN_VALUE);
            mScaleXSeekBarLayout.setClampedMax(Integer.MAX_VALUE);
            mScaleYSeekBarLayout.setClampedMin(Integer.MIN_VALUE);
            mScaleYSeekBarLayout.setClampedMax(Integer.MAX_VALUE);
        }

        maybeUpdateRange(mScaleXSeekBarLayout, mImageView.getImageMinScaleX(), mImageView.getImageMaxScaleX());
        maybeUpdateRange(mScaleYSeekBarLayout, mImageView.getImageMinScaleY(), mImageView.getImageMaxScaleY());
        maybeUpdateRange(mPivotXSeekBarLayout, 0.0f, (float) mImageView.getDrawableIntrinsicWidth());
        maybeUpdateRange(mPivotYSeekBarLayout, 0.0f, (float) mImageView.getDrawableIntrinsicHeight());

        if (mDisallowUpdatingSeekBars) {
            mDisallowUpdatingSeekBars = false;
        } else {
            mScaleXSeekBarLayout.setValue(scaleX, false);
            mScaleYSeekBarLayout.setValue(scaleY, false);
            mPivotXSeekBarLayout.setValue(pivotX, false);
            mPivotYSeekBarLayout.setValue(pivotY, false);
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

    private void maybeUpdateRange(FloatSeekBarLayout layout, float min, float max) {
        if (Float.compare(layout.getMinValue(), min) != 0) {
            layout.setMinValue(min);
        }
        if (Float.compare(layout.getMaxValue(), max) != 0) {
            layout.setMaxValue(max);
        }
    }

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
