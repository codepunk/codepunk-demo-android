package com.codepunk.demo.interactiveimageview2;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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
import com.codepunk.demo.interactiveimageview2.ImageViewInteractinator.Transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InteractinatorControlsFragment extends Fragment
        implements AbsSeekBarLayout.OnSeekBarChangeListener<Float>,
                AdapterView.OnItemSelectedListener,
                DemoImageViewInteractinator.DemoInteractiveImageViewListener,
                View.OnClickListener,
                View.OnTouchListener {

    //region Constants

    private static final String LOG_TAG = InteractinatorControlsFragment.class.getSimpleName();
    private static final @DrawableRes int DEFAULT_DRAWABLE_RES_ID = R.drawable.wilderness_lodge;

    private static final String CLASS_NAME = InteractinatorControlsFragment.class.getName();
    private static final String KEY_SCALE_LOCKED = CLASS_NAME + ".scaleLocked";
    private static final String KEY_IS_TRANSFORMED = CLASS_NAME + ".isTransformed";
    private static final String KEY_TRANSFORM = CLASS_NAME + ".transform";

    //endregion Constants

    //region Fields

    private List<Integer> mImageEntryValues;
    private List<String> mScaleTypeEntryValues;

    private Spinner mImageSpinner;
    private Spinner mScaleTypeSpinner;
    private ToggleButton mLockButton;
    private FloatSeekBarLayout mImageScaleXSeekBarLayout;
    private FloatSeekBarLayout mImageScaleYSeekBarLayout;
    private FloatSeekBarLayout mImagePivotXSeekBarLayout;
    private FloatSeekBarLayout mImagePivotYSeekBarLayout;
    private DemoImageViewInteractinator mImageView;
    private final Transform mTransform = new Transform();

    private boolean mDisallowUpdatingSeekBars = false;

    private boolean mPendingResetClamps = true;

    //endregion Fields

    //region Constructors

    public InteractinatorControlsFragment() {
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
        return inflater.inflate(
                R.layout.fragment_interactive_image_view_controls_2,
                container,
                false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mImageSpinner = view.findViewById(R.id.spinner_image);
        mScaleTypeSpinner = view.findViewById(R.id.spinner_scale_type);
        mImagePivotXSeekBarLayout = view.findViewById(R.id.layout_seek_bar_image_pivot_x);
        mImagePivotYSeekBarLayout = view.findViewById(R.id.layout_seek_bar_image_pivot_y);
        mImageScaleXSeekBarLayout = view.findViewById(R.id.layout_seek_bar_image_scale_x);
        mImageScaleYSeekBarLayout = view.findViewById(R.id.layout_seek_bar_image_scale_y);
        mLockButton = view.findViewById(R.id.btn_lock);

        mImagePivotXSeekBarLayout.setOnSeekBarChangeListener(this);
        mImagePivotYSeekBarLayout.setOnSeekBarChangeListener(this);
        mImageScaleXSeekBarLayout.setOnSeekBarChangeListener(this);
        mImageScaleYSeekBarLayout.setOnSeekBarChangeListener(this);
        mLockButton.setOnClickListener(this);

        // Prevent drawer from intercepting touch event from seek bars
        mImagePivotXSeekBarLayout.setSeekBarOnTouchListener(this);
        mImagePivotYSeekBarLayout.setSeekBarOnTouchListener(this);
        mImageScaleXSeekBarLayout.setSeekBarOnTouchListener(this);
        mImageScaleYSeekBarLayout.setSeekBarOnTouchListener(this);

        // Initialize image pivot seek bars
        mImagePivotXSeekBarLayout.setMinValue(0.0f);
        mImagePivotYSeekBarLayout.setMinValue(0.0f);
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
                mTransform.set((Transform) savedInstanceState.getParcelable(KEY_TRANSFORM));
                mImageView.transform(mTransform);
            } else {
                mImageView.setScaleType(mImageView.getScaleType());
            }
        }

        mImageSpinner.setOnItemSelectedListener(InteractinatorControlsFragment.this);
        mScaleTypeSpinner.setOnItemSelectedListener(InteractinatorControlsFragment.this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_SCALE_LOCKED, mLockButton.isChecked());
        if (mImageView.isTransformed()) {
            outState.putBoolean(KEY_IS_TRANSFORMED, true);
            mImageView.getTransform(mTransform);
            outState.putParcelable(KEY_TRANSFORM, mTransform);
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
            float sx = mImageScaleXSeekBarLayout.getValue();
            float sy = mImageScaleYSeekBarLayout.getValue();
            float px = mImagePivotXSeekBarLayout.getValue();
            float py = mImagePivotYSeekBarLayout.getValue();
            switch (id) {
                case R.id.layout_seek_bar_image_pivot_x: {
                    px = value;
                    transformImage = true;
                    break;
                }
                case R.id.layout_seek_bar_image_pivot_y: {
                    py = value;
                    transformImage = true;
                    break;
                }
                case R.id.layout_seek_bar_image_scale_x: {
                    sx = value;
                    if (mLockButton.isChecked()) {
                        final int width = mImageView.getDrawableFunctionalWidth();
                        final int height = mImageView.getDrawableFunctionalHeight();
                        final float ratio = (float) width / height;
                        final float scaledHeight = (width * value) / ratio;
                        sy = scaledHeight / height;
                        mImageScaleYSeekBarLayout.setValue(sy);
                    }
                    transformImage = true;
                    break;
                }
                case R.id.layout_seek_bar_image_scale_y: {
                    sy = value;
                    if (mLockButton.isChecked()) {
                        final int width = mImageView.getDrawableFunctionalWidth();
                        final int height = mImageView.getDrawableFunctionalHeight();
                        final float ratio = (float) width / height;
                        final float scaledWidth = (height * value) * ratio;
                        sx = scaledWidth / width;
                        mImageScaleXSeekBarLayout.setValue(sx);
                    }
                    transformImage = true;
                    break;
                }
                default:
                    mDisallowUpdatingSeekBars = false;
                    transformImage = false;
            }
            if (transformImage) {
                mTransform.reset()
                        .pivot(px, py)
                        .scale(sx, sy)
                        .transform(mImageView);
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
    public void onDraw(ImageViewInteractinator view, Canvas canvas) {
        final int position = mScaleTypeEntryValues.indexOf(mImageView.getScaleType().name());
        if (position != mScaleTypeSpinner.getSelectedItemPosition()) {
            mScaleTypeSpinner.setSelection(position, false);
        }

        final float imageScaleX = mImageView.getImageScaleX();
        final float imageScaleY = mImageView.getImageScaleY();
        final float imagePivotX = mImageView.getImagePivotX();
        final float imagePivotY = mImageView.getImagePivotY();

        if (mPendingResetClamps) {
            mImageScaleXSeekBarLayout.setClampedMin(Integer.MIN_VALUE);
            mImageScaleXSeekBarLayout.setClampedMax(Integer.MAX_VALUE);
            mImageScaleYSeekBarLayout.setClampedMin(Integer.MIN_VALUE);
            mImageScaleYSeekBarLayout.setClampedMax(Integer.MAX_VALUE);
        }

        maybeUpdateRange(
                mImageScaleXSeekBarLayout,
                mImageView.getImageMinScaleX(),
                mImageView.getImageMaxScaleX());
        maybeUpdateRange(
                mImageScaleYSeekBarLayout,
                mImageView.getImageMinScaleY(),
                mImageView.getImageMaxScaleY());
        maybeUpdateRange(
                mImagePivotXSeekBarLayout,
                0.0f,
                (float) mImageView.getDrawableFunctionalWidth());
        maybeUpdateRange(
                mImagePivotYSeekBarLayout,
                0.0f,
                (float) mImageView.getDrawableFunctionalHeight());

        if (mDisallowUpdatingSeekBars) {
            mDisallowUpdatingSeekBars = false;
        } else {
            mImageScaleXSeekBarLayout.setValue(imageScaleX, false);
            mImageScaleYSeekBarLayout.setValue(imageScaleY, false);
            mImagePivotXSeekBarLayout.setValue(imagePivotX, false);
            mImagePivotYSeekBarLayout.setValue(imagePivotY, false);
        }

        if (mPendingResetClamps) {
            mPendingResetClamps = false;
            resetClamps();
        }
    }

    @Override // DemoInteractiveImageView.DemoInteractiveImageViewListener
    public void onInteractionBegin(ImageViewInteractinator view) {
        final View mainView = getView();
        if (mainView != null) {
            mainView.setAlpha(0.5f);
        }
    }

    @Override // DemoInteractiveImageView.DemoInteractiveImageViewListener
    public void onInteractionEnd(ImageViewInteractinator view) {
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

    public void setImageView(DemoImageViewInteractinator imageView) {
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
            final float currentScaleX = mImageScaleXSeekBarLayout.getValue();
            final float currentScaleY = mImageScaleYSeekBarLayout.getValue();
            final float shrinkFactor;
            final float growFactor;
            if (mImageScaleXSeekBarLayout.getRelativeProgress() <
                    mImageScaleYSeekBarLayout.getRelativeProgress()) {
                shrinkFactor = currentScaleX / minScaleX;
                growFactor = maxScaleY / currentScaleY;
                scaleXClampedMin = Integer.MIN_VALUE;
                scaleXClampedMax =
                        mImageScaleXSeekBarLayout.valueToProgress(currentScaleX * growFactor);
                scaleYClampedMin =
                        mImageScaleYSeekBarLayout.valueToProgress(currentScaleY / shrinkFactor);
                scaleYClampedMax = Integer.MAX_VALUE;
            } else {
                shrinkFactor = currentScaleY / minScaleY;
                growFactor = maxScaleX / currentScaleX;
                scaleXClampedMin =
                        mImageScaleXSeekBarLayout.valueToProgress(currentScaleX / shrinkFactor);
                scaleXClampedMax = Integer.MAX_VALUE;
                scaleYClampedMin = Integer.MIN_VALUE;
                scaleYClampedMax =
                        mImageScaleYSeekBarLayout.valueToProgress(currentScaleY * growFactor);
            }
        } else {
            scaleXClampedMin = scaleYClampedMin = Integer.MIN_VALUE;
            scaleXClampedMax = scaleYClampedMax = Integer.MAX_VALUE;
        }
        mImageScaleXSeekBarLayout.setClampedMin(scaleXClampedMin);
        mImageScaleXSeekBarLayout.setClampedMax(scaleXClampedMax);
        mImageScaleYSeekBarLayout.setClampedMin(scaleYClampedMin);
        mImageScaleYSeekBarLayout.setClampedMax(scaleYClampedMax);
    }

    private boolean setImageResourceByPosition(int position) {
        if (mImageView != null && position >= 0 && position < mImageEntryValues.size()) {
            final @DrawableRes int drawableResId = mImageEntryValues.get(position);
            mImageView.setImageResource(drawableResId);

            Drawable d = mImageView.getDrawable();
            final boolean canInteract =
                    !(d == null || d.getIntrinsicWidth() < 1 || d.getIntrinsicHeight() < 1);
            mScaleTypeSpinner.setEnabled(canInteract);
            mImagePivotXSeekBarLayout.setEnabled(canInteract);
            mImagePivotYSeekBarLayout.setEnabled(canInteract);
            mImageScaleXSeekBarLayout.setEnabled(canInteract);
            mImageScaleYSeekBarLayout.setEnabled(canInteract);
            mLockButton.setEnabled(canInteract);

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
