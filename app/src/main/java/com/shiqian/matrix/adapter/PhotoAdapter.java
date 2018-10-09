package com.shiqian.matrix.adapter;
/*
 * 包名：Matrix
 * 文件名： PhotoAdapter
 * 创建者：wushiqian
 * 创建时间 2018/10/5 10:10 PM
 * 描述： TODO//
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.shiqian.matrix.R;

import java.io.FileNotFoundException;
import java.util.List;

public class PhotoAdapter extends BaseAdapter {

    private Context mContext;

    private LayoutInflater inflater;

    private List<Uri> uri_list;


    private class ViewHolder {
        ImageView iv_img;
    }

    public PhotoAdapter(Context mContext, List<Uri> uri_list) {
        this.mContext = mContext;
        this.uri_list = uri_list;
        this.inflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        //写死长度，可以根据需要自己修改
        return uri_list.size() == 9 ? 9 : (uri_list.size() + 1);
    }

    @Override
    public Object getItem(int position) {
        return uri_list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_defect_addimg, parent, false);
            holder = new ViewHolder();
            holder.iv_img = convertView.findViewById(R.id.default_add_img);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        //最多展示三张图片，
        //控制添加按钮的展示和隐藏 就是+图片
        if (position < uri_list.size()) {
            try {
                Uri uri = (Uri) getItem(position);
                Bitmap bitmap = BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(uri));
                holder.iv_img.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            holder.iv_img.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.add));
        }
        return convertView;
    }

}
