package com.codepunk.demo;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.ImageView.ScaleType;

@SuppressWarnings("WeakerAccess")
public class GraphicsUtils {
    //region Fields
    private static final RectF sTempSrc = new RectF();
    private static final RectF sTempDst = new RectF();
    private static final Matrix sMatrix = new Matrix();
    private static final float[] sMatrixValues = new float[9];
    //endregion Fields

    //region Constructors
    private GraphicsUtils() {
    }
    //endregion Constructors

    //region Methods
    /**
     * Mimics the logic in {@link android.widget.ImageView}.configureBounds()
     * @param src the source rectangle to map from
     * @param dst the destination rectangle to map to (i.e. the rectangle being scaled)
     * @param scaleType the desired scaling mode
     * @param outRect receives the computed scaled rectangle
     * @see android.widget.ImageView
     * @see ScaleType
     */
    public synchronized static void scale(Rect src, Rect dst, ScaleType scaleType, Rect outRect) {
        final int srcWidth = src.width();
        final int srcHeight = src.height();
        final int dstWidth = dst.width();
        final int dstHeight = dst.height();
        final boolean fits = (srcWidth < 0 || dstWidth == srcWidth)
                && (srcHeight < 0 || dstHeight == srcHeight);
        if (srcWidth <= 0 || srcHeight <= 0) {
            outRect.set(src);
        } else if (ScaleType.FIT_XY == scaleType) {
            outRect.set(dst);
        } else if (ScaleType.MATRIX == scaleType) {
            outRect.set(src);
        } else if (fits) {
            // The bitmap fits exactly, no transform needed.
            outRect.set(src);
        } else if (ScaleType.CENTER == scaleType) {
            // Center bitmap in view, no scaling.
            float dx = (dstWidth - srcWidth) * 0.5f;
            float dy = (dstHeight - srcHeight) * 0.5f;
            outRect.set(src);
            outRect.offsetTo(Math.round(dx), Math.round(dy));
        } else if (ScaleType.CENTER_CROP == scaleType) {
            float scale;
            float dx = 0, dy = 0;
            if (srcWidth * dstHeight > dstWidth * srcHeight) {
                scale = (float) dstHeight / (float) srcHeight;
                dx = (dstWidth - srcWidth * scale) * 0.5f;
            } else {
                scale = (float) dstWidth / (float) srcWidth;
                dy = (dstHeight - srcHeight * scale) * 0.5f;
            }
            outRect.set(0, 0, Math.round(srcWidth * scale), Math.round(srcHeight * scale));
            outRect.offsetTo(Math.round(dx), Math.round(dy));
        } else if (ScaleType.CENTER_INSIDE == scaleType) {
            float scale;
            float dx;
            float dy;
            if (srcWidth <= dstWidth && srcHeight <= dstHeight) {
                scale = 1.0f;
            } else {
                scale = Math.min((float) dstWidth / (float) srcWidth,
                        (float) dstHeight / (float) srcHeight);
            }
            dx = Math.round((dstWidth - srcWidth * scale) * 0.5f);
            dy = Math.round((dstHeight - srcHeight * scale) * 0.5f);
            outRect.set(0, 0, Math.round(srcWidth * scale), Math.round(srcHeight * scale));
            outRect.offsetTo(Math.round(dx), Math.round(dy));
        } else {
            // Generate the required transform.
            sTempSrc.set(0, 0, srcWidth, srcHeight);
            sTempDst.set(0, 0, dstWidth, dstHeight);
            sMatrix.setRectToRect(sTempSrc, sTempDst, scaleTypeToScaleToFit(scaleType));
            sMatrix.getValues(sMatrixValues);
            float sx = sMatrixValues[Matrix.MSCALE_X];
            float sy = sMatrixValues[Matrix.MSCALE_Y];
            float dx = sMatrixValues[Matrix.MTRANS_X];
            float dy = sMatrixValues[Matrix.MTRANS_Y];
            outRect.set(0, 0, Math.round(srcWidth * sx), Math.round(srcHeight * sy));
            outRect.offsetTo(Math.round(dx), Math.round(dy));
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
