package com.shiqian.matrix.ui;
/*
 * 包名：Matrix
 * 文件名： SettingActivity
 * 创建者：wushiqian
 * 创建时间 2018/10/3 12:03 PM
 * 描述： TODO//
 */

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.jaeger.library.StatusBarUtil;
import com.shiqian.matrix.R;

public class SettingActivity extends BaseActivity{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        initView();
    }

    private void initView() {
        StatusBarUtil.setColorNoTranslucent(this, getResources().getColor(R.color.zhihu_primary));
    }
}
