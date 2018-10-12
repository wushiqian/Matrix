package com.shiqian.matrix.view;
/*
 * 包名：Matrix
 * 文件名： PopupChoisePhoto
 * 创建者：wushiqian
 * 创建时间 2018/10/5 10:20 PM
 * 描述： TODO//
 */

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;

import com.shiqian.matrix.R;

public class PopupChoosePhoto extends PopupWindow {

    // layout : popupwindow显示时方便设置背景
    // putPos ：根据传入的数据长度，判断显示删除和预览按钮
    public PopupChoosePhoto(Activity context, View.OnClickListener btnOnClick,
                            final View layout, int whichPhoto, int photoSize) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.popup_choice_photo, null);

        Button choosePhoto = view.findViewById(R.id.choosePhoto);
        Button img_edit = view.findViewById(R.id.btn_img_edit);
        Button takePhoto = view.findViewById(R.id.takePhoto);
        Button cancle = view.findViewById(R.id.btn_cancel);
        Button del_select = view.findViewById(R.id.btn_del_select);
        del_select.setOnClickListener(btnOnClick);
        choosePhoto.setOnClickListener(btnOnClick);
        takePhoto.setOnClickListener(btnOnClick);
        cancle.setOnClickListener(btnOnClick);
        img_edit.setOnClickListener(btnOnClick);

        //判断是否显示“删除此图片”和“预览按钮”
        if (photoSize == 0 || whichPhoto == photoSize) {
            img_edit.setVisibility(View.GONE);
            del_select.setVisibility(View.GONE);
        } else {
            img_edit.setVisibility(View.VISIBLE);
            del_select.setVisibility(View.VISIBLE);
        }

        this.setContentView(view);
        //设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        //设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        //设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
        // 刷新状态
        this.update();
        this.setOutsideTouchable(true);
        this.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                //消除RelativeLayout半透明
                layout.setAlpha(1);
            }
        });
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0xb0ffffff);
        //设置SelectPicPopupWindow弹出窗体的背景
        this.setBackgroundDrawable(dw);

    }

    /**
     * 显示popupWindow
     *
     * @param parent
     */
    public void showPopupWindow(View parent) {
        if (!this.isShowing()) {
            this.showAtLocation(parent, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        }
    }

    /**
     * 关闭popupWindow
     */
    public void dismissPopupWindow(View layout) {
        layout.setAlpha(1);
        this.dismiss();

    }
}
