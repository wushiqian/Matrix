package com.shiqian.matrix;
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
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.jaeger.library.StatusBarUtil;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;
import java.util.List;

import es.dmoral.toasty.Toasty;

public class PhotoActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * DATA
     */
    private static final String TAG = "PhotoActivity";
    private List<Uri> mSelected;
    private Bitmap mBitmap;

    private ImageButton ibChoose;
    private ImageButton ibTake;
    private int REQUEST_CODE_CHOOSE = 2;
    private static final int REQUEST_PERMISSION_CODE = 0;
    private String FILE_PATH = "com.shiqian.matrix.fileprovider";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        initView();
    }

    private void initView() {
        StatusBarUtil.setColor(this, getResources().getColor(R.color.zhihu_primary));
        ibChoose = findViewById(R.id.ib_choose);
        ibTake = findViewById(R.id.ib_take);
        ibChoose.setOnClickListener(this);
        ibTake.setOnClickListener(this);
        Toolbar toolbar = findViewById(R.id.phototoolbar);
        setSupportActionBar(toolbar);
    }

    /**
     * 加载菜单
     *
     * @param menu 菜单
     * @return 是否有菜单
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.phototoolbar, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ib_choose:
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_PERMISSION_CODE);
                } else {
                    Matisse.from(this)
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
            default:
                break;
        }
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
                            .thumbnailScale(0.85f)
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
            mSelected = Matisse.obtainResult(data);
            Log.d("Matisse", "mSelected: " + mSelected);
            startCrop();
        }
        if (resultCode == RESULT_OK) {
            //裁切成功
            if (requestCode == UCrop.REQUEST_CROP) {
                Uri croppedFileUri = UCrop.getOutput(data);
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("Photo", croppedFileUri.toString());
                startActivity(intent);
            }
        }
        //裁切失败
        if (resultCode == UCrop.RESULT_ERROR) {
            Toasty.error(this, "裁切图片失败", Toast.LENGTH_SHORT, true).show();
        }
    }

    //裁剪框架配置
    private void startCrop() {
        Uri sourceUri = mSelected.get(0);
        //裁剪后保存到文件中
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "SampleCropImage.jpeg"));
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
//        options.setCompressionQuality(100);
        //设置裁剪框横竖线的颜色
//        options.setCropGridColor(Color.GREEN);
        uCrop.withOptions(options);
        uCrop = uCrop.useSourceImageAspectRatio();
        uCrop.withMaxResultSize(800, 800).start(this);
    }

    /**
     * 菜单点击事件
     *
     * @param item 子选项
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share:

                break;
            case R.id.setting:
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

}
