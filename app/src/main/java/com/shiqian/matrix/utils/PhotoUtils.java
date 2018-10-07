package com.shiqian.matrix.utils;
/*
 * 包名：Matrix
 * 文件名： PhotoUtils
 * 创建者：wushiqian
 * 创建时间 2018/9/21 下午12:26
 * 描述： TODO//
 */

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;

public class PhotoUtils {

    /**
     * ----------------------------------------------------
     * 处理图像的色调，饱和度，亮度。
     * 因为Android中不能处理原Bitmap，需要处理Bitmap的副本，
     * 因此使用原Bitmap创建副本，进行处理。
     * ---------------------------------------------------
     */
    public static Bitmap handleImageEffect(Bitmap bm, float hue, float saturation, float lum) {

        //创建副本Bitmap
        Bitmap bitmap = bm.copy(Bitmap.Config.ARGB_8888, true);

        //创建画板、画笔
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        /*-----------------------------------
         * 处理色调: setRotate中0,1,2代表R,G,B
         * -----------------------------------*/
        ColorMatrix hueColorMatrix = new ColorMatrix();
        hueColorMatrix.setRotate(0, hue);
        hueColorMatrix.setRotate(1, hue);
        hueColorMatrix.setRotate(2, hue);

        /*-----------------------------------
         *  设置饱和度
         * -----------------------------------*/
        ColorMatrix saColorMatrix = new ColorMatrix();
        saColorMatrix.setSaturation(saturation);

        /*-------------------------------------------
         * 设置亮度：将三原色相同比例混合显示出白色，以此提高亮度
         * ------------------------------------------*/
        ColorMatrix lumColorMatrix = new ColorMatrix();
        lumColorMatrix.setScale(lum, lum, lum, 1);

        /*---------------------------------
         *  将矩阵作用效果混合, 叠加效果。
         * --------------------------------*/
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.postConcat(hueColorMatrix);
        colorMatrix.postConcat(saColorMatrix);
        colorMatrix.postConcat(lumColorMatrix);

        /*-------------------------
         *  设置矩阵, 进行绘制
         * ------------------------*/
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return bitmap;
    }

    public static Bitmap ScaleImage(Bitmap bit,float x,float y){
        Matrix matrix = new Matrix();
        matrix.setScale(x,y);
        int width = bit.getWidth();
        int height = bit.getHeight();
        return Bitmap.createBitmap(bit, 0, 0, width, height, matrix, true);
    }

    /**
     * 图片旋转
     *
     * @param bit     旋转原图像
     * @param degrees 旋转度数
     * @return 旋转之后的图像
     */
    public static Bitmap rotateImage(Bitmap bit, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap tempBitmap = Bitmap.createBitmap(bit, 0, 0, bit.getWidth(),
                bit.getHeight(), matrix, true);
        return tempBitmap;
    }

    /**
     * 翻转图像
     *
     * @param bit 翻转原图像
     * @param x   翻转X轴
     * @param y   翻转Y轴
     * @return 翻转之后的图像
     * <p>
     * 说明:
     * (1,-1)上下翻转
     * (-1,1)左右翻转
     */
    public static Bitmap reverseImage(Bitmap bit, int x, int y) {
        Matrix matrix = new Matrix();
        matrix.postScale(x, y);
        Bitmap tempBitmap = Bitmap.createBitmap(bit, 0, 0, bit.getWidth(),
                bit.getHeight(), matrix, true);
        return tempBitmap;
    }

    public static Bitmap ResizeBitmap(Bitmap bitmap, int scale) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.postScale(1 / scale, 1 / scale);

        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height,
                matrix, true);
        bitmap.recycle();
        return resizedBitmap;
    }

}
