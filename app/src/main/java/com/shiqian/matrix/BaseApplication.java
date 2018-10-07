package com.shiqian.matrix;
/*
 * 包名：Matrix
 * 文件名： BaseApplication
 * 创建者：wushiqian
 * 创建时间 2018/9/20 下午9:35
 * 描述： TODO//
 */

import android.app.Application;
import android.content.Context;
import android.graphics.Color;

import com.mob.MobSDK;
import com.shiqian.matrix.utils.StaticUtils;
import com.tencent.bugly.crashreport.CrashReport;

import es.dmoral.toasty.Toasty;

public class BaseApplication extends Application {

    public static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        //初始化Bugly
        CrashReport.initCrashReport(getApplicationContext(), StaticUtils.BUGLY_APP_ID, true);
        MobSDK.init(this);
        Toasty.Config.getInstance()
                .setErrorColor(Color.RED) // optional
                .setInfoColor(Color.BLUE) // optional
                .setSuccessColor(Color.GREEN) // optional
                .tintIcon(true) // optional (apply textColor also to the icon)
//                .setToastTypeface(@NonNull Typeface typeface) // optional
                .apply(); // required
    }

    public static Context getContext() {
        return mContext;
    }
}
