package com.codepunk.demo.support;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.ImageView;

public class ImageViewCompat {

    //region Nested classes

    private interface ImageViewCompatImpl {
        boolean getCropToPadding(ImageView imageView);
        void setCropToPadding(ImageView imageView, boolean cropToPadding);
    }

    private static class BaseIntegerCompatImpl implements ImageViewCompatImpl {
        @Override
        public boolean getCropToPadding(ImageView imageView) {
            return false;
        }

        @Override
        public void setCropToPadding(ImageView imageView, boolean cropToPadding) {
            // NO OP
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static class JellyBeanIntegerCompatImpl implements ImageViewCompatImpl {
        @Override
        public boolean getCropToPadding(ImageView imageView) {
            return imageView.getCropToPadding();
        }

        @Override
        public void setCropToPadding(ImageView imageView, boolean cropToPadding) {
            imageView.setCropToPadding(cropToPadding);
        }
    }

    //endregion Nested classes

    //region Constants

    private static final ImageViewCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            IMPL = new JellyBeanIntegerCompatImpl();
        } else {
            IMPL = new BaseIntegerCompatImpl();
        }
    }

    //endregion Constants

    //region Constructors

    private ImageViewCompat() {}

    //endregion Constructors

    //region Public methods

    public static boolean getCropToPadding(ImageView imageView) {
        return IMPL.getCropToPadding(imageView);
    }

    public static void setCropToPadding(ImageView imageView, boolean cropToPadding) {
        IMPL.setCropToPadding(imageView, cropToPadding);
    }

    //endregion Public methods
}
