package com.codepunk.demo.interactiveimageview.version5;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView.ScaleType;
import android.widget.Spinner;

import com.codepunk.demo.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InteractiveImageViewControlsFragment extends Fragment
        implements AdapterView.OnItemSelectedListener,
                DemoInteractiveImageView.DemoInteractiveImageViewListener {
    //region Constants
    private static final String LOG_TAG =
            InteractiveImageViewControlsFragment.class.getSimpleName();
    //endregion Constants

    //region Fields
    private List<Integer> mImageEntryValues;
    private List<String> mScaleTypeEntryValues;

    private Spinner mImageSpinner;
    private Spinner mScaleTypeSpinner;
    private DemoInteractiveImageView mInteractiveImageView;

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

        // TODO Maybe post these?
        mImageSpinner.setOnItemSelectedListener(this);
        mScaleTypeSpinner.setOnItemSelectedListener(this);
    }
    //endregion Lifecycle methods

    //region Implemented methods
    @Override // AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.spinner_image:
                if (mInteractiveImageView != null && position >= 0 && position < mImageEntryValues.size()) {
                    mPendingResetClamps = true;
                    mInteractiveImageView.setImageResource(mImageEntryValues.get(position));
                }
                break;
            case R.id.spinner_scale_type:
                if (mInteractiveImageView != null && position >= 0 && position < mScaleTypeEntryValues.size()) {
                    mPendingResetClamps = true;
                    mInteractiveImageView.setScaleType(
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
    }

    @Override
    public void onInteractionBegin(InteractiveImageView view) {
        final View mainView = getView();
        if (mainView != null) {
            mainView.setAlpha(0.5f);
        }
    }

    @Override
    public void onInteractionEnd(InteractiveImageView view) {
        final View mainView = getView();
        if (mainView != null) {
            mainView.setAlpha(1.0f);
        }
    }

    @Override
    public void onSetImageResource(InteractiveImageView view, int resId) {
        mImageSpinner.setSelection(mImageEntryValues.indexOf(resId), false);
    }
    //endregion Implemented methods

    //region Methods
    public void setInteractiveImageView(DemoInteractiveImageView imageView) {
        mInteractiveImageView = imageView;
        mInteractiveImageView.setDemoInteractiveImageViewListener(this);
        updateScaleTypeSpinner();
    }

    private void updateScaleTypeSpinner() {
        if (mInteractiveImageView != null) {
            final ScaleType scaleType = mInteractiveImageView.getScaleType();
            final int position = mScaleTypeEntryValues.indexOf(scaleType.name());
            mScaleTypeSpinner.setSelection(position, false);
        }
    }
    //endregion Methods
}
