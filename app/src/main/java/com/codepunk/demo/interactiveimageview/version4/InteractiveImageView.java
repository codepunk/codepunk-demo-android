package com.codepunk.demo.interactiveimageview.version4;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class InteractiveImageView extends AppCompatImageView {
    //region Nested classes

    //endregion Nested classes

    //region Constructors
    public InteractiveImageView(Context context) {
        super(context);
    }

    public InteractiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InteractiveImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    //endregion Constructors

    //region Methods
    public float getImageMaxScaleX() {
        return 5.0f;
    }

    public float getImageMaxScaleY() {
        return 5.0f;
    }

    public float getImageMinScaleX() {
        return 0.1f;
    }

    public float getImageMinScaleY() {
        return 0.1f;
    }

    public float getImageCenterX() {
        return 0.5f;
    }

    public float getImageCenterY() {
        return 0.5f;
    }

    public float getImageScaleX() {
        return 1.0f;
    }

    public float getImageScaleY() {
        return 1.0f;
    }

    public boolean hasCustomPlacement() {
        return true;
    }

    public void setImageScale(float sx, float sy) {

    }

    public void setImageScale(float sx, float sy, float cx, float cy) {

    }
    //endregion Methods
}
