package com.codepunk.demo;

import android.graphics.PointF;
import android.graphics.Rect;
import android.widget.ImageView.ScaleType;

import static android.widget.ImageView.ScaleType.CENTER;
import static android.widget.ImageView.ScaleType.CENTER_INSIDE;
import static android.widget.ImageView.ScaleType.FIT_XY;
import static android.widget.ImageView.ScaleType.MATRIX;

@SuppressWarnings("WeakerAccess")
public class GraphicsUtils {
    //region Fields
    private static final PointF sScalePoint = new PointF();
    //endregion Fields

    //region Constructors
    private GraphicsUtils() {
    }
    //endregion Constructors

    //region Methods
    public static void scale(
            Rect src,
            Rect dst,
            ScaleType scaleType,
            PointF outPoint) {
        if (scaleType == CENTER || scaleType == MATRIX) {
            outPoint.set(1.0f, 1.0f);
            return;
        }

        final int srcWidth = src.width();
        final int srcHeight = src.height();
        final int dstWidth = dst.width();
        final int dstHeight = dst.height();
        if (scaleType == CENTER_INSIDE && srcWidth <= dstWidth && srcHeight <= dstHeight) {
            outPoint.set(1.0f, 1.0f);
            return;
        }

        final float widthFit = (float) dstWidth / srcWidth;
        final float heightFit = (float) dstHeight / srcHeight;
        switch (scaleType) {
            case CENTER_CROP:
                final float maxScale = Math.max(widthFit, heightFit);
                outPoint.set(maxScale, maxScale);
                break;
            case FIT_XY:
                outPoint.set(widthFit, heightFit);
                break;
            case CENTER_INSIDE:
            case FIT_CENTER:
            case FIT_END:
            case FIT_START:
            default:
                final float minScale = Math.min(widthFit, heightFit);
                outPoint.set(minScale, minScale);
                break;
        }
    }

    /**
     * Mimics the logic in {@link android.widget.ImageView}.configureBounds()
     * @param src the source rectangle to map from
     * @param dst the destination rectangle to map to (i.e. the rectangle being scaled)
     * @param scaleType the desired scaling mode
     * @param outRect receives the computed scaled rectangle
     * @see android.widget.ImageView
     * @see ScaleType
     */
    public static void scale(
            Rect src,
            Rect dst,
            ScaleType scaleType,
            Rect outRect) {
        if (src.width() < 0 || src.height() < 0 || scaleType == MATRIX) {
            outRect.set(src);
            return;
        } else if (scaleType == FIT_XY) {
            outRect.set(dst);
            return;
        }

        synchronized (sScalePoint) {
            scale(src, dst, scaleType, sScalePoint);
            final int width = Math.round(src.width() * sScalePoint.x);
            final int height = Math.round(src.height() * sScalePoint.y);
            final int left;
            final int top;
            switch (scaleType) {
                case FIT_END:
                    left = dst.right - width;
                    top = dst.bottom - height;
                    break;
                case FIT_START:
                    left = top = 0;
                    break;
                case CENTER:
                case CENTER_INSIDE:
                case CENTER_CROP:
                case FIT_CENTER:
                default:
                    left = Math.round(dst.left + (dst.width() - width) / 2.0f);
                    top = Math.round(dst.top + (dst.height() - height) / 2.0f);
                    break;
            }
            outRect.set(left, top, left + width, top + height);
        }
    }
    //endregion Methods
}
