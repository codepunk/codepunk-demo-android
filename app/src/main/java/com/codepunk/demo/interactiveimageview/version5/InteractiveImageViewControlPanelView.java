package com.codepunk.demo.interactiveimageview.version5;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView.ScaleType;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.codepunk.demo.R;

import java.util.Arrays;
import java.util.List;

public class InteractiveImageViewControlPanelView extends ScrollView
        implements AdapterView.OnItemSelectedListener {

    //region Constants
    private final List<Integer> IMAGE_RES_IDS = Arrays.asList(
            0,
            R.drawable.cinderellas_castle,
            R.drawable.wilderness_lodge,
            R.drawable.polynesian,
            R.drawable.gradient);

    private static final @DrawableRes int DEFAULT_IMAGE_RES_ID = R.drawable.wilderness_lodge;
    //endregion Constants

    //region Fields
    private Spinner mImageSpinner;
    private Spinner mScaleTypeSpinner;
    private String[] mScaleTypeEntryValues;

    private InteractiveImageView mImageView;
    private boolean mPendingResetClamps = true;
    //endregion Fields

    //region Constructors
    public InteractiveImageViewControlPanelView(Context context) {
        super(context);
        initializeInteractiveImageViewControlPanelView(context);
    }

    public InteractiveImageViewControlPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeInteractiveImageViewControlPanelView(context);
    }

    public InteractiveImageViewControlPanelView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeInteractiveImageViewControlPanelView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public InteractiveImageViewControlPanelView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initializeInteractiveImageViewControlPanelView(context);
    }
    //endregion Constructors

    //region Implemented methods
    @Override // AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.spinner_image:
                // TODO
                mPendingResetClamps = true;
                mImageView.setImageResource(IMAGE_RES_IDS.get(position));
                // mHasIntrinsicSize = mImageView.getIntrinsicImageSize(mIntrinsicSizePoint);

                // TODO Enable/disable controls based on intrinsic size
                /*
                final Drawable drawable = mImageView.getDrawable();
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
                mPendingResetClamps = true;
                final String name = mScaleTypeEntryValues[position];
                mImageView.setScaleType(ScaleType.valueOf(name));
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
    //endregion Implemented methods

    //region Methods
    public void setImageView(InteractiveImageView imageView) {
        mImageView = imageView;
        mPendingResetClamps = true;
        mImageView.setImageResource(IMAGE_RES_IDS.get(mImageSpinner.getSelectedItemPosition()));
//        final String name = mScaleTypeEntryValues[mScaleTypeSpinner.getSelectedItemPosition()];
//        mImageView.setScaleType(ScaleType.valueOf(name));

        final ScaleType scaleType = mImageView.getScaleType();
        mScaleTypeSpinner.setSelection(scaleType.ordinal(), false);
    }
    //endregion Methods

    //region Private methods
    private void initializeInteractiveImageViewControlPanelView(Context context) {
        View view = inflate(context, R.layout.layout_control_panel, this);
        mImageSpinner = view.findViewById(R.id.spinner_image);
        mScaleTypeSpinner = view.findViewById(R.id.spinner_scale_type);

        //final int position = IMAGE_RES_IDS.indexOf(DEFAULT_IMAGE_RES_ID);
        //mImageSpinner.setSelection(position, false);

        Log.d("SAS", "position=" + mImageSpinner.getSelectedItemPosition());

        mScaleTypeEntryValues = getResources().getStringArray(R.array.scale_type_values);

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                // TODO Any better way?
                mImageSpinner.setOnItemSelectedListener(InteractiveImageViewControlPanelView.this);
                mScaleTypeSpinner.setOnItemSelectedListener(InteractiveImageViewControlPanelView.this);
            }
        });
    }
    //endregion Private methods
}
