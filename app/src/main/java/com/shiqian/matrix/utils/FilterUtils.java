package com.shiqian.matrix.utils;
/*
 * 包名：Matrix
 * 文件名： Filter
 * 创建者：wushiqian
 * 创建时间 2018/9/26 下午1:02
 * 描述： TODO//
 */

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

public class FilterUtils {

    public static Bitmap handleImageNegative(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);
        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            r = 255 - r;
            g = 255 - g;
            b = 255 - b;

            if (r > 255) {
                r = 255;
            } else if (r < 0) {
                r = 0;
            }
            if (g > 255) {
                g = 255;
            } else if (g < 0) {
                g = 0;
            }
            if (b > 255) {
                b = 255;
            } else if (b < 0) {
                b = 0;
            }
            newPx[i] = Color.argb(a, r, g, b);
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        return bmp;
    }

    public static Bitmap handleImageOld(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);
        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            r = (int) (0.393 * r + 0.769 * g + 0.189 * b);
            g = (int) (0.349 * r + 0.686 * g + 0.168 * b);
            b = (int) (0.272 * r + 0.534 * g + 0.131 * b);

            newPx[i] = Color.argb(a, r, g, b);
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        return bmp;
    }

    public static Bitmap handleImageCool(Bitmap bitmap) {
        int width, height;
        width = bitmap.getWidth();
        height = bitmap.getHeight();

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setAntiAlias(true); // 设置抗锯齿
        float[] array = {0.393f, 0.769f, 0.189f, 0, 0,
                0.349f, 0.686f, 0.168f, 0, 0,
                0.272f, 0.534f, 0.131f, 0, 0,
                0, 0, 0, 1, 0};
        ColorMatrix colorMatrix = new ColorMatrix(array);

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return bmp;
    }

    public static Bitmap handleImagePolaroid(Bitmap bitmap) {
        int width, height;
        width = bitmap.getWidth();
        height = bitmap.getHeight();

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setAntiAlias(true); // 设置抗锯齿

        float[] array = {1.438f, -0.062f, -0.062f, 0, 0,
                -0.122f, 1.378f, -0.122f, 0, 0,
                -0.016f, -0.016f, 1.483f, 0, 0,
                -0.03f, 0.05f, -0.02f, 1, 0};
        ColorMatrix colorMatrix = new ColorMatrix(array);

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return bmp;
    }

    /**
     * 浮雕效果
     *
     * @param bitmap 初始bitmap
     * @return 目标bitmap
     */
    public static Bitmap handleImageEmboss(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int color = 0, preColor = 0, a, r, g, b;
        int r1, g1, b1;
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bitmap.getPixels(oldPx, 0, width, 0, 0, width, height);
        for (int i = 1; i < oldPx.length; i++) {
            preColor = oldPx[i - 1];
            a = Color.alpha(preColor);
            r = Color.red(preColor);
            g = Color.green(preColor);
            b = Color.blue(preColor);

            color = oldPx[i];
            r1 = Color.red(color);
            g1 = Color.green(color);
            b1 = Color.blue(color);

            r = r1 - r + 127;
            g = g1 - g + 127;
            b = b1 - b + 127;

            if (r > 255) {
                r = 255;
            } else if (r < 0) {
                r = 0;
            }

            if (g > 255) {
                g = 255;
            } else if (g < 0) {
                g = 0;
            }

            if (b > 255) {
                b = 255;
            } else if (b < 0) {
                b = 0;
            }
            newPx[i] = Color.argb(a, r, g, b);
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        return bmp;
    }

    //霓虹
    public static Bitmap handleImageNeon(Bitmap bitmap){
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Bitmap resultBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        int color = 0;
        int r,g,b,r1,g1,b1,r2,g2,b2;

        int[] oldPx = new int[w * h];
        int[] newPx = new int[w * h];

        bitmap.getPixels(oldPx, 0, w, 0, 0, w, h);
        for(int y = 0; y < h - 1; y++){
            for(int x = 0; x < w - 1; x++){
                color = oldPx[x+y*w];

                r = (color >> 16) & 0xFF;
                g = (color >> 8) & 0xFF;
                b = (color >> 0) & 0xFF;

                int newcolor = oldPx[x+1+y*w];

                r1 = (newcolor >> 16) & 0xFF;
                g1 = (newcolor >> 8) & 0xFF;
                b1 = (newcolor >> 0) & 0xFF;

                int newcolor2 = oldPx[x+(y+1)*w];

                r2 = (newcolor2 >> 16) & 0xFF;
                g2 = (newcolor2 >> 8) & 0xFF;
                b2 = (newcolor2 >> 0) & 0xFF;

                int tr = (int) (2*Math.sqrt(((r-r1)*(r-r1)+(r-r2)*(r-r2))));
                int tg = (int) (2*Math.sqrt(((g-g1)*(g-g1)+(g-g2)*(g-g2))));
                int tb = (int) (2*Math.sqrt(((b-b1)*(b-b1)+(b-b2)*(b-b2))));

                newPx[x+y*w] = (255 << 24)|(tr << 16)|(tg << 8)|(tb);
            }
        }
        resultBitmap.setPixels(newPx, 0, w, 0, 0, w, h);
        return resultBitmap;
    }

}
