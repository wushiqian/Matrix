package com.shiqian.matrix.utils;
/*
 * 包名：Matrix
 * 文件名： UIUtils
 * 创建者：
 * 创建时间 2018/10/9 8:26 PM
 * 描述： TODO//
 */

import android.content.Context;
import android.support.annotation.Nullable;

public class UIUtils {
    public static int dip2px(@Nullable Context context, float dpValue) {
        final float scale ;
        if (context != null) {
            scale = context.getResources().getDisplayMetrics().density;
        }
        return (int) (dpValue * scale + 0.5f);
    }
}