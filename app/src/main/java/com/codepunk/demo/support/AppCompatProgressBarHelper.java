package com.codepunk.demo.support;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.widget.ProgressBar;

import java.lang.reflect.Field;

import static android.os.Build.VERSION_CODES.BASE;
import static android.os.Build.VERSION_CODES.O;

public class AppCompatProgressBarHelper {
    //region Nested classes
    public interface AppCompatProgressBarImpl {
        // TODO Can I accomplish the same without mMax?
        // Then I can restore OreoIMPL and also only have getSupportMin and setSupportMin
        int getMax();
        int getMin();
        void setMax(int max);
        void setMin(int min);
    }

    @TargetApi(BASE)
    private static class BaseAppCompatProgressBarImpl
            implements AppCompatProgressBarImpl {
        @NonNull
        protected final ProgressBar mOwner;
        private int mMax;
        protected int mMin;
        private int mProgress;
        private boolean mMaxInitialized;
        private boolean mMinInitialized;
        private boolean mMaxSet;

        public BaseAppCompatProgressBarImpl(@NonNull ProgressBar owner) {
            mOwner = owner;
        }

        @Override
        public int getMax() {
            return mMax;
        }

        @Override
        public int getMin() {
            return mMin;
        }

        @Override
        public void setMax(int max) {
            if (mMinInitialized) {
                if (max < mMin) {
                    max = mMin;
                }
            }
            mMaxInitialized = true;
            if (mMinInitialized && max != getMax()) {
                mMax = max;
                setMaxInternal(max - mMin);
                mOwner.postInvalidate();

                if (mProgress > max) {
                    mProgress = max;
                }
                mOwner.setProgress(mProgress - mMin);
//                refreshProgress(android.R.id.progress, mProgress - mMin, false, false);
            } else {
                mMax = max;
            }
        }

        @Override
        public void setMin(int min) {
            if (mMaxInitialized) {
                if (min > mMax) {
                    min = mMax;
                }
            }
            mMinInitialized = true;
            if (mMaxInitialized && min != mMin) {
                mMin = min;
                if (!mMaxSet) {
                    setMaxInternal(mMax - min);
                }
                mOwner.postInvalidate();

                if (mProgress < min) {
                    mProgress = min;
                }
                mOwner.setProgress(mProgress - mMin);
//                refreshProgress(android.R.id.progress, mProgress - min, false, false);
            } else {
                mMin = min;
            }
        }

        /*
        protected void refreshProgress(int id, int progress, boolean fromUser, boolean animate) {
            try {
                final Method method =
                        ProgressBar.class.getDeclaredMethod(
                                "refreshProgress",
                                int.class,
                                int.class,
                                boolean.class);
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                method.invoke(mOwner, id, progress, fromUser);
            } catch (Exception e) {
                // NOOP
            }
        }
        */

        private void setMaxInternal(int max) {
            try {
                final Field field = ProgressBar.class.getDeclaredField("mMax");
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                field.setInt(mOwner, max);
                mMaxSet = true;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /*
    @TargetApi(O)
    private static class OreoAppCompatProgressBarImpl
            implements AppCompatProgressBarImpl {
        @NonNull
        private final ProgressBar mOwner;

        public OreoAppCompatProgressBarImpl(@NonNull ProgressBar owner) {
            mOwner = owner;
        }

        @Override
        public int getMax() {
            return mOwner.getMax();
        }

        @Override
        public int getMin() {
            return mOwner.getMin();
        }

        @Override
        public void setMax(int max) {
            mOwner.setMax(max);
        }

        @Override
        public void setMin(int min) {
            mOwner.setMin(min);
        }
    }
    */
    //endregion Nested classes

    //region Methods
    public static AppCompatProgressBarImpl newInstance(@NonNull ProgressBar owner) {
        if (Build.VERSION.SDK_INT >= O) {
            return null; //new OAppCompatProgressBarImpl(owner);
        } else {
            return new BaseAppCompatProgressBarImpl(owner);
        }
    }
    //endregion Methods
}
