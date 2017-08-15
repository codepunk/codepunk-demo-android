package com.codepunk.demo;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.IntDef;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

public class ViewCompat {

    //region Nested classes
    private interface ViewCompatImpl {
        int getTextAlignment(View view);
        void setTextAlignment(View view, int textAlignment);
    }

    @SuppressWarnings("deprecation")
    private static class BaseViewCompatImpl
            implements ViewCompatImpl {
        @Override
        public int getTextAlignment(View view) {
            return TEXT_ALIGNMENT_DEFAULT;
        }

        @Override
        public void setTextAlignment(View view, int textAlignment) {
            // NOOP
        }
    }

    @TargetApi(JELLY_BEAN_MR1)
    private static class JellyBeanMr1ViewCompatImpl
            implements ViewCompatImpl {
        @Override
        public int getTextAlignment(View view) {
            return view.getTextAlignment();
        }

        @Override
        public void setTextAlignment(View view, int textAlignment) {
            view.setTextAlignment(textAlignment);
        }
    }
    //endregion Nested classes

    //region Constants
    @IntDef({
            TEXT_ALIGNMENT_INHERIT,
            TEXT_ALIGNMENT_GRAVITY,
            TEXT_ALIGNMENT_CENTER,
            TEXT_ALIGNMENT_TEXT_START,
            TEXT_ALIGNMENT_TEXT_END,
            TEXT_ALIGNMENT_VIEW_START,
            TEXT_ALIGNMENT_VIEW_END
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextAlignment {}

    /**
     * Default text alignment. The text alignment of this View is inherited from its parent.
     * Use with {@link #setTextAlignment(View, int)}
     */
    public static final int TEXT_ALIGNMENT_INHERIT = 0;

    /**
     * Default for the root view. The gravity determines the text alignment, ALIGN_NORMAL,
     * ALIGN_CENTER, or ALIGN_OPPOSITE, which are relative to each paragraph’s text direction.
     *
     * Use with {@link #setTextAlignment(View, int)}
     */
    public static final int TEXT_ALIGNMENT_GRAVITY = 1;

    /**
     * Align to the start of the paragraph, e.g. ALIGN_NORMAL.
     *
     * Use with {@link #setTextAlignment(View, int)}
     */
    public static final int TEXT_ALIGNMENT_TEXT_START = 2;

    /**
     * Align to the end of the paragraph, e.g. ALIGN_OPPOSITE.
     *
     * Use with {@link #setTextAlignment(View, int)}
     */
    public static final int TEXT_ALIGNMENT_TEXT_END = 3;

    /**
     * Center the paragraph, e.g. ALIGN_CENTER.
     *
     * Use with {@link #setTextAlignment(View, int)}
     */
    public static final int TEXT_ALIGNMENT_CENTER = 4;

    /**
     * Align to the start of the view, which is ALIGN_LEFT if the view’s resolved
     * layoutDirection is LTR, and ALIGN_RIGHT otherwise.
     *
     * Use with {@link #setTextAlignment(View, int)}
     */
    public static final int TEXT_ALIGNMENT_VIEW_START = 5;

    /**
     * Align to the end of the view, which is ALIGN_RIGHT if the view’s resolved
     * layoutDirection is LTR, and ALIGN_LEFT otherwise.
     *
     * Use with {@link #setTextAlignment(View, int)}
     */
    public static final int TEXT_ALIGNMENT_VIEW_END = 6;

    /**
     * Default text alignment is inherited
     */
    private static final int TEXT_ALIGNMENT_DEFAULT = TEXT_ALIGNMENT_GRAVITY;

    private static final ViewCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
            IMPL = new JellyBeanMr1ViewCompatImpl();
        } else {
            IMPL = new BaseViewCompatImpl();
        }
    }
    //endregion Constants

    //region Public methods
    public static int getTextAlignment(View view) {
        return IMPL.getTextAlignment(view);
    }

    public static void setTextAlignment(View view, int textAlignment) {
        IMPL.setTextAlignment(view, textAlignment);
    }
    //endregion Public methods
}
