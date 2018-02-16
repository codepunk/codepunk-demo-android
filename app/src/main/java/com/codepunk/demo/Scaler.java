/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codepunk.demo;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.Locale;

/**
 * A simple class that animates double-touch zoom gestures. Functionally similar to a {@link
 * android.widget.Scroller}.
 */
@SuppressWarnings("SpellCheckingInspection")
public class Scaler {
    private final String LOG_TAG = Scaler.class.getSimpleName();

    /**
     * The interpolator, used for making zooms animate 'naturally.'
     */
    private Interpolator mInterpolator;

    /**
     * The total animation duration for a zoom.
     */
    private int mAnimationDurationMillis;

    /**
     * Whether or not the current zoom has finished.
     */
    private boolean mFinished = true;

    /**
     * The current zoom value; computed by {@link #computeScale()}.
     */
    //private float mCurrentZoom;
    private float mCurrentScaleX;

    private float mCurrentScaleY;

    /**
     * The time the zoom started, computed using {@link SystemClock#elapsedRealtime()}.
     */
    private long mStartRTC;

    private float mStartScaleX;

    private float mStartScaleY;

    /**
     * The destination zoom factor.
     */
    //private float mEndZoom;
    private float mEndScaleX;

    private float mEndScaleY;

    public Scaler(Context context) {
        mInterpolator = new DecelerateInterpolator();
        mAnimationDurationMillis = context.getResources().getInteger(
                android.R.integer.config_shortAnimTime);
    }

    /**
     * Forces the zoom finished state to the given value. Unlike {@link #abortAnimation()}, the
     * current zoom value isn't set to the ending value.
     *
     * @see android.widget.Scroller#forceFinished(boolean)
     */
    public void forceFinished(boolean finished) {
        Log.d(LOG_TAG, String.format(Locale.ENGLISH, "forceFinished: finished=%b", finished));
        mFinished = finished;
    }

    /**
     * Aborts the animation, setting the current zoom value to the ending value.
     *
     * @see android.widget.Scroller#abortAnimation()
     */
    public void abortAnimation() {
        Log.d(LOG_TAG, "abortAnimation");
        mFinished = true;
//        mCurrentZoom = mEndZoom;
        mCurrentScaleX = mEndScaleX;
        mCurrentScaleY = mEndScaleY;
    }

    /*
     * Starts a zoom from 1.0 to (1.0 + endZoom). That is, to zoom from 100% to 125%, endZoom should
     * by 0.25f.
     *
     * @see android.widget.Scroller#startScroll(int, int, int, int)
    public void startZoom(float endZoom) {
        mStartRTC = SystemClock.elapsedRealtime();
        mEndZoom = endZoom;

        mFinished = false;
        mCurrentZoom = 1f;
    }
     */

    public void startScale(float startScaleX, float startScaleY, float endScaleX, float endScaleY) {
        mStartRTC = SystemClock.elapsedRealtime();
        mCurrentScaleX = mStartScaleX = startScaleX;
        mCurrentScaleY = mStartScaleY = startScaleY;
        mEndScaleX = endScaleX;
        mEndScaleY = endScaleY;

        mFinished = false;
        Log.d(LOG_TAG, String.format(Locale.ENGLISH, "startScale: mFinished=%b", mFinished));
    }

    /**
     * Computes the current zoom level, returning true if the zoom is still active and false if the
     * zoom has finished.
     *
     * @see android.widget.Scroller#computeScrollOffset()
     */
    public boolean computeScale() {
        if (mFinished) {
            Log.d(LOG_TAG, String.format(Locale.ENGLISH, "computeScale: mFinished=%b", false));
            return false;
        }

        long tRTC = SystemClock.elapsedRealtime() - mStartRTC;

        Log.d(LOG_TAG, String.format(Locale.ENGLISH, "computeScale: mFinished=%b, tRTC=%d", mFinished, tRTC));

        if (tRTC >= mAnimationDurationMillis) {
            mFinished = true;
//            mCurrentZoom = mEndZoom;
            mCurrentScaleX = mEndScaleX;
            mCurrentScaleY = mEndScaleY;
            return false;
        }

        float t = tRTC * 1f / mAnimationDurationMillis;
//        mCurrentZoom = mEndZoom * mInterpolator.getInterpolation(t);
        float interpolation = mInterpolator.getInterpolation(t);
        mCurrentScaleX = mStartScaleX + (mEndScaleX - mStartScaleX) * interpolation;
        mCurrentScaleY = mStartScaleY + (mEndScaleY - mStartScaleY) * interpolation;
        return true;
    }

    /*
     * Returns the current zoom level.
     *
     * @see android.widget.Scroller#getCurrX()
    public float getCurrZoom() {
        return mCurrentZoom;
    }
     */

    public float getCurrScaleX() {
        return mCurrentScaleX;
    }

    public float getCurrScaleY() {
        return mCurrentScaleY;
    }
}
