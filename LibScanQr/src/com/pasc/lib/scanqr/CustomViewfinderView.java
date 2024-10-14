package com.pasc.lib.scanqr;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;

import com.pasc.lib.barcodescanner.ViewfinderView;

/**
 * 自定义二维码扫描框
 */
public class CustomViewfinderView extends ViewfinderView {

    public int laserLinePosition = 0;
    public float[] position = new float[]{0f, 0.5f, 1f};
    public int[] colors = new int[]{0x7F35ACFA, 0xff51BAFF, 0x7F35ACFA};
    public LinearGradient linearGradient;

    private int framingRectSize;
    private int top = 0;

    private Bitmap bgCorner = null;
    private Bitmap bitmap;

    public CustomViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        framingRectSize = dp2px(245);
        initView(attrs);
    }

    private void initView(AttributeSet attrs) {
        bitmap =
                ((BitmapDrawable) getResources().getDrawable(R.mipmap.zxing_scan_border_nor)).getBitmap();

        bgCorner =
                ((BitmapDrawable) getResources().getDrawable(R.mipmap.zxing_scan_border)).getBitmap();
        if (attrs != null) {
            TypedArray a =
                    getContext().obtainStyledAttributes(attrs, R.styleable.zxing_CustomViewfinderView);
            top = a.getDimensionPixelSize(R.styleable.zxing_CustomViewfinderView_zxing_top, top);
        }
    }

    @Override
    protected void refreshSizes() {
        if (cameraPreview == null) {
            return;
        }

        int offset = (getWidth() - framingRectSize) / 2;

        if (top == 0) {
            framingRect = cameraPreview.getFramingRect();
        } else {
            framingRect = new Rect(offset, top, offset + framingRectSize, top + framingRectSize);
        }

        Rect previewFramingRect = cameraPreview.getPreviewFramingRect();
        if (framingRect != null && previewFramingRect != null) {
            this.previewFramingRect = previewFramingRect;
        }
    }

    /**
     * 重写draw方法绘制自己的扫描框
     */
    @Override
    public void onDraw(Canvas canvas) {
        refreshSizes();
        if (framingRect == null || previewFramingRect == null) {
            int offset = (getWidth() - framingRectSize) / 2;
            framingRect = new Rect(offset, top, offset + framingRectSize, top + framingRectSize);
        }

        Rect frame = framingRect;

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.top + frame.height(), paint);
        canvas.drawRect(frame.left + frame.width(), frame.top, width, frame.top + frame.height(),
                paint);
        canvas.drawRect(0, frame.top + frame.height(), width, height, paint);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(ViewfinderView.CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {

            //控制网格下拉速度
            if (laserLinePosition >= frame.height()) {
                laserLinePosition = 0;
            } else if (laserLinePosition > frame.height() * 3 / 5) {
                laserLinePosition = laserLinePosition + 5;
            } else {
                laserLinePosition = laserLinePosition + 9;
            }
            if (laserLinePosition > frame.height()) {
                laserLinePosition = frame.height();
            }

            // 绘制网格
            int bitWidth = bitmap.getWidth();
            int bitHeight = bitmap.getHeight();
            Rect srcRect = new Rect(0, bitHeight - laserLinePosition, bitWidth, bitHeight);
            Rect desRect = new Rect(frame.left, frame.top, frame.left + bitWidth,
                    frame.top + laserLinePosition);
            canvas.drawBitmap(bitmap, srcRect, desRect, null);

            //绘制4个角
            int bitWidthCorner = bgCorner.getWidth();
            int bitHeightCorner = bgCorner.getHeight();
            Rect srcRectCorner = new Rect(0, 0, bitWidthCorner, bitHeightCorner);
            Rect desRectCorner = new Rect(frame.left, frame.top, frame.left + bitWidthCorner,
                    frame.top + bitHeightCorner);
            canvas.drawBitmap(bgCorner, srcRectCorner, desRectCorner, null);

            paint.setShader(null);

            if (laserLinePosition >= frame.height()) {
                postInvalidateDelayed(200, frame.left, frame.top, frame.right, frame.bottom);
            } else {
                invalidate(frame.left, frame.top, frame.right, frame.bottom);
            }
        }
    }

    /**
     * dp转px
     *
     * @param dpValue dp值
     * @return px值
     */
    public static int dp2px(final float dpValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
