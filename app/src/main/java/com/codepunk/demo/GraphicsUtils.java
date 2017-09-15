package com.codepunk.demo;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.ImageView.ScaleType;

import static android.widget.ImageView.ScaleType.CENTER;
import static android.widget.ImageView.ScaleType.CENTER_INSIDE;
import static android.widget.ImageView.ScaleType.MATRIX;

@SuppressWarnings("WeakerAccess")
public class GraphicsUtils {
    //region Fields
    private static final Matrix sMatrix = new Matrix();
    private static final float[] sMatrixValues = new float[9];
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
    public synchronized static void scale(
            RectF src,
            RectF dst,
            ScaleType scaleType,
            RectF outRect) {
        // TODO Do this my own way
        final float srcWidth = src.width();
        final float srcHeight = src.height();
        final float dstWidth = dst.width();
        final float dstHeight = dst.height();
        final boolean fits = (srcWidth < 0.0f || dstWidth == srcWidth)
                && (srcHeight < 0 || dstHeight == srcHeight);
        if (srcWidth <= 0.0f || srcHeight <= 0.0f) {
            outRect.set(src);
        } else if (ScaleType.FIT_XY == scaleType) {
            outRect.set(dst);
        } else if (MATRIX == scaleType) {
            outRect.set(src);
        } else if (fits) {
            // The bitmap fits exactly, no transform needed.
            outRect.set(src);
        } else if (CENTER == scaleType) {
            // Center bitmap in view, no scaling.
            float dx = (dstWidth - srcWidth) * 0.5f;
            float dy = (dstHeight - srcHeight) * 0.5f;
            outRect.set(src);
            outRect.offsetTo(dx, dy);
        } else if (ScaleType.CENTER_CROP == scaleType) {
            float scale;
            float dx = 0, dy = 0;
            if (srcWidth * dstHeight > dstWidth * srcHeight) {
                scale = dstHeight / srcHeight;
                dx = (dstWidth - srcWidth * scale) * 0.5f;
            } else {
                scale = dstWidth / srcWidth;
                dy = (dstHeight - srcHeight * scale) * 0.5f;
            }
            outRect.set(0.0f, 0.0f, srcWidth * scale, srcHeight * scale);
            outRect.offsetTo(dx, dy);
        } else if (ScaleType.CENTER_INSIDE == scaleType) {
            float scale;
            float dx;
            float dy;
            if (srcWidth <= dstWidth && srcHeight <= dstHeight) {
                scale = 1.0f;
            } else {
                scale = Math.min(dstWidth / srcWidth, dstHeight / srcHeight);
            }
            dx = (dstWidth - srcWidth * scale) * 0.5f;
            dy = (dstHeight - srcHeight * scale) * 0.5f;
            outRect.set(0.0f, 0.0f, srcWidth * scale, srcHeight * scale);
            outRect.offsetTo(dx, dy);
        } else {
            // Generate the required transform.
            sMatrix.setRectToRect(src, dst, scaleTypeToScaleToFit(scaleType));
            sMatrix.getValues(sMatrixValues);
            outRect.set(
                    0.0f,
                    0.0f,
                    srcWidth * sMatrixValues[Matrix.MSCALE_X],
                    srcHeight * sMatrixValues[Matrix.MSCALE_Y]);
            outRect.offsetTo(sMatrixValues[Matrix.MTRANS_X], sMatrixValues[Matrix.MTRANS_Y]);
        }
    }
    //endregion Methods

    //region Private methods
    private static Matrix.ScaleToFit scaleTypeToScaleToFit(ScaleType scaleType)  {
        switch (scaleType) {
            case FIT_XY:
                return Matrix.ScaleToFit.FILL;
            case FIT_START:
                return Matrix.ScaleToFit.START;
            case FIT_END:
                return Matrix.ScaleToFit.END;
            case FIT_CENTER:
            default:
                return Matrix.ScaleToFit.CENTER;
        }
    }
    //endregion Private methods
}
