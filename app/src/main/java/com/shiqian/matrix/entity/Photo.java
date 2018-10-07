package com.shiqian.matrix.entity;
/*
 * 包名：Matrix
 * 文件名： Photo
 * 创建者：wushiqian
 * 创建时间 2018/10/6 2:27 PM
 * 描述： TODO//
 */

import android.graphics.Bitmap;
import android.net.Uri;

public class Photo {

    private Uri imageUri;

    private Bitmap mBitmap;

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public Photo(Uri uri,Bitmap bitmap){
        this.imageUri = uri;
        this.mBitmap = bitmap;
    }
}
