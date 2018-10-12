package com.shiqian.matrix.ui;
/*
 * 包名：Matrix
 * 文件名： PhotoActivity
 * 创建者：wushiqian
 * 创建时间 2018/9/22 上午9:34
 * 描述： TODO//
 */

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jaeger.library.StatusBarUtil;
import com.shiqian.matrix.R;
import com.shiqian.matrix.adapter.PhotoAdapter;
import com.shiqian.matrix.utils.ShareUtils;
import com.shiqian.matrix.view.PopupChoosePhoto;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;

public class PhotoActivity extends AppCompatActivity {

    /**
     * DATA
     */
    private static final String TAG = "PhotoActivity";

    private int REQUEST_CODE_CHOOSE = 2;
    private static final int REQUEST_PERMISSION_CODE = 0;
    private String FILE_PATH = "com.shiqian.matrix.fileprovider";
    private Uri imageUri;

    //显示图片的GridView
    private GridView mGridView;
    //GridView Adapter
    private PhotoAdapter mPhotoAdapter;
    //判断是添加新照片还是更新已有照片
    private boolean isAdd = true;
    //数据源初始长度
    private int photosSize = 0;
    //选择哪张照片
    private int whichPhoto = 0;

    public static final int TAKE_PHOTO = 1;

    private LinearLayout parentLayout;

    private List<Uri> uri_list = new ArrayList<>();//图片集合

    private PopupChoosePhoto choosePhotoPopup;//选择相机or图片

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        initView();
    }

    private void initView() {
        StatusBarUtil.setColorNoTranslucent(this, getResources().getColor(R.color.zhihu_primary));
        int uriSize = ShareUtils.getInt(this, ShareUtils.NAME, uri_list.size());
        Uri photo;
        if (uriSize != 0) {
            for (int i = 0; i < uriSize; i++) {
                photo = Uri.parse("file:///data/user/0/com.shiqian.matrix/cache/SampleCropImage"
                        + uri_list.size() + "jpeg");
                uri_list.add(photo);
            }
        }
        mGridView = findViewById(R.id.img_add_grid);
        mPhotoAdapter = new PhotoAdapter(this, uri_list);
        mGridView.setAdapter(mPhotoAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position < uri_list.size()) {
                    //更新
                    isAdd = false;
                    whichPhoto = position;
                    photosSize = uri_list.size();
                } else {
                    //添加
                    whichPhoto = position;
                    photosSize = uri_list.size();
                    isAdd = true;
                }
                showChoisePhoto();
            }
        });
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PhotoActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });
    }

    //选择相机or图片
    private void showChoisePhoto() {
        parentLayout = findViewById(R.id.add_main);
        choosePhotoPopup = new PopupChoosePhoto(this, choosePhotoClick, parentLayout, whichPhoto, photosSize);
        choosePhotoPopup.showPopupWindow(parentLayout);
        parentLayout.setAlpha((float) 0.3);
    }

    private android.view.View.OnClickListener choosePhotoClick = new android.view.View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.takePhoto:
                    //打开照相机
                    takePhoto();
                    break;
                case R.id.choosePhoto:
                    if (ContextCompat.checkSelfPermission(PhotoActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(PhotoActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_PERMISSION_CODE);
                    } else {
                        Matisse.from(PhotoActivity.this)
                                .choose(MimeType.ofImage())
                                .countable(false)
                                .capture(true) //使用拍照功能
                                .captureStrategy(new CaptureStrategy(true, FILE_PATH))
                                .maxSelectable(1)
                                .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.media_big_size))
                                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                                .thumbnailScale(0.85f)
                                .theme(R.style.Matisse_Zhihu)//主题
                                .imageEngine(new com.shiqian.photoedit.utils.MatisseGlide())
                                .forResult(REQUEST_CODE_CHOOSE);
                    }
                    break;
                case R.id.btn_del_select:
                    if (uri_list.size() != 0) {
                        uri_list.remove(whichPhoto);
                        mPhotoAdapter.notifyDataSetChanged();
                        choosePhotoPopup.dismissPopupWindow(parentLayout);
                    } else {
                        Toasty.info(PhotoActivity.this, "暂无图片,请先添加图片", Toast.LENGTH_SHORT).show();
                        choosePhotoPopup.dismissPopupWindow(parentLayout);
                    }
                    break;
                case R.id.btn_img_edit:
                    //图片预览
                    Intent intent = new Intent(PhotoActivity.this, MainActivity.class);
                    intent.putExtra("Photo", uri_list.get(whichPhoto).toString());
                    startActivity(intent);
                    choosePhotoPopup.dismissPopupWindow(parentLayout);
                    break;
                case R.id.btn_cancel:
                    //取消
                    choosePhotoPopup.dismissPopupWindow(parentLayout);
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 获取相机
     */
    private void takePhoto() {
        // 创建File对象，用于存储拍照后的图片
        File outputImage = new File(getExternalCacheDir(), "SampleCropImage" + uri_list.size() + "jpeg");
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT < 24) {
            imageUri = Uri.fromFile(outputImage);
        } else {
            imageUri = FileProvider.getUriForFile(PhotoActivity.this, FILE_PATH, outputImage);
        }
        // 启动相机程序
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, TAKE_PHOTO);

    }

    //申请权限回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //用户已授权
                    Matisse.from(this)
                            .choose(MimeType.ofAll())
                            .countable(false)
                            .capture(true) //使用拍照功能
                            .captureStrategy(new CaptureStrategy(true, FILE_PATH))
                            .maxSelectable(1)
                            .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.media_big_size))
                            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                            .thumbnailScale(1f)
                            .imageEngine(new com.shiqian.photoedit.utils.MatisseGlide())
                            .forResult(REQUEST_CODE_CHOOSE);
                } else {
                    //用户未授权
                    Toasty.error(this, "未授权", Toast.LENGTH_SHORT, true).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            List<Uri> selected = Matisse.obtainResult(data);
            Log.d("Matisse", "mSelected: " + selected);
            startCrop(selected.get(0));
        }
        if (resultCode == RESULT_OK) {
            //裁切成功
            if (requestCode == UCrop.REQUEST_CROP) {
                Uri croppedFileUri = UCrop.getOutput(data);
                //判断是添加照片还是更新照片
                if (isAdd) {
                    if (croppedFileUri != null) {
                        Log.i(TAG, "Uri " + croppedFileUri.toString());
                    }
                    uri_list.add(croppedFileUri);
                } else {
                    uri_list.set(whichPhoto, croppedFileUri);
                }
                //刷新数据  关闭popupwidow
                mPhotoAdapter.notifyDataSetChanged();
                choosePhotoPopup.dismissPopupWindow(parentLayout);
            }
        }
        //裁切失败
        if (resultCode == UCrop.RESULT_ERROR) {
            Toasty.error(this, "裁切图片失败", Toast.LENGTH_SHORT, true).show();
        }
        switch (requestCode) {
            case TAKE_PHOTO:   //调用相机后返回
                //是否正常拍照
                if (resultCode == RESULT_OK) {
                    startCrop(imageUri);
                } else if (resultCode == RESULT_CANCELED) {
                    //取消拍照
                    choosePhotoPopup.dismissPopupWindow(parentLayout);
                }
                break;
        }
    }

    //裁剪框架配置
    private void startCrop(Uri sourceUri) {
//        Uri sourceUri = mSelected.get(0);
        //裁剪后保存到文件中
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "SampleCropImage" + uri_list.size() + "jpeg"));
        UCrop uCrop = UCrop.of(sourceUri, destinationUri);
        // 修改配置参数
        UCrop.Options options = new UCrop.Options();
        // 修改标题栏颜色
        options.setToolbarColor(getResources().getColor(R.color.zhihu_primary));
        // 修改状态栏颜色
        options.setStatusBarColor(getResources().getColor(R.color.zhihu_primary));
        //设置裁剪图片可操作的手势
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);
        //设置图片在切换比例时的动画
        options.setImageToCropBoundsAnimDuration(666);
        //设置最大缩放比例
        options.setMaxScaleMultiplier(4);
        // 设置图片压缩质量
        options.setCompressionQuality(100);
        //设置裁剪框横竖线的颜色
        options.setCropGridColor(Color.GREEN);
        uCrop.withOptions(options);
        uCrop = uCrop.useSourceImageAspectRatio();
        uCrop.withMaxResultSize(3200, 3200).start(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ShareUtils.putInt(this, ShareUtils.NAME, uri_list.size());
    }
}
