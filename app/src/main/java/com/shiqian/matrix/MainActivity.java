package com.shiqian.matrix;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageHelper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.shiqian.photoedit.utils.MatisseGlide;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.FileNotFoundException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private static final String FILE_PATH = "com.shiqian.matrix.fileprovider";
    private static final String TAG = "MainActivity";
    private int REQUEST_CODE_CHOOSE = 2;

    List<Uri> mSelected;
    private Uri photo = null;

    private Button mButton;
    private ImageView mImageView;
    private SeekBar hueSeekBar;
    private SeekBar satSeekBar;
    private SeekBar lumSeekBar;
    private float mHue = 0.0f;
    private float mSaturation = 1f;
    private float mLum = 1f;
    private int MID_VALUE = 127;
    private int MAX_VALUE = 255;

    private static final int REQUEST_PERMISSION_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        hueSeekBar = findViewById(R.id.sb_hue);
        satSeekBar = findViewById(R.id.sb_saturation);
        lumSeekBar = findViewById(R.id.sb_lum);
        hueSeekBar.setMax(MAX_VALUE);
        lumSeekBar.setMax(MAX_VALUE);
        satSeekBar.setMax(MAX_VALUE);
        hueSeekBar.setProgress(MID_VALUE);
        satSeekBar.setProgress(MID_VALUE);
        lumSeekBar.setProgress(MID_VALUE);
        hueSeekBar.setOnSeekBarChangeListener(this);
        satSeekBar.setOnSeekBarChangeListener(this);
        lumSeekBar.setOnSeekBarChangeListener(this);

        mButton = findViewById(R.id.btn_photo);
        mImageView = findViewById(R.id.iv_main);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_PERMISSION_CODE);
                } else {
                    Matisse.from(MainActivity.this)
                            .choose(MimeType.ofImage())
                            .countable(false)
                            .capture(true) //使用拍照功能
                            .captureStrategy(new CaptureStrategy(true, FILE_PATH))
                            .maxSelectable(1)
                            .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.media_big_size))
                            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                            .thumbnailScale(0.85f)
                            .theme(R.style.Matisse_Zhihu)//主题
                            .imageEngine(new MatisseGlide())
                            .forResult(REQUEST_CODE_CHOOSE);
                }
            }
        });
    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    /*
     * SeekBar滚动时的回调函数
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        Log.d(TAG, "seekid:" + seekBar.getId() + ", progess" + progress);
        switch (seekBar.getId()) {
            case R.id.sb_hue:
                mHue = (progress - MID_VALUE) * 1.0F / MID_VALUE * 180;
                break;
            case R.id.sb_saturation:
                mSaturation = progress * 1.0F / MID_VALUE;
                break;
            case R.id.sb_lum:
                mLum = progress * 1.0F / MID_VALUE;
                break;
        }
        Bitmap bmp = null;
        try {
            bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(mSelected.get(0)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mImageView.setImageBitmap(handleImageEffect(bmp, mHue, mSaturation, mLum));
    }

    /**
     * ----------------------------------------------------
     * 处理图像的色调，饱和度，亮度。
     * 因为Android中不能处理原Bitmap，需要处理Bitmap的副本，
     * 因此使用原Bitmap创建副本，进行处理。
     * ---------------------------------------------------
     */
    public Bitmap handleImageEffect(Bitmap bm, float hue, float saturation, float lum) {

        //创建副本Bitmap
//        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_drop_down_white_24dp);
//        Bitmap bitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);
//        Bitmap bmp = Bitmap.createBitmap(bm.getWidth(),bm.getHeight(),Bitmap.Config.ARGB_8888);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //用户已授权
                    Matisse.from(MainActivity.this)
                            .choose(MimeType.ofAll())
                            .countable(false)
                            .capture(true) //使用拍照功能
                            .captureStrategy(new CaptureStrategy(true, FILE_PATH))
                            .maxSelectable(1)
                            .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.media_big_size))
                            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                            .thumbnailScale(0.85f)
                            .imageEngine(new MatisseGlide())
                            .forResult(REQUEST_CODE_CHOOSE);
                } else {
                    //用户未授权
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
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mSelected.get(0)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            mImageView.setImageBitmap(bitmap);
            hueSeekBar.setProgress(MID_VALUE);
            satSeekBar.setProgress(MID_VALUE);
            lumSeekBar.setProgress(MID_VALUE);
        }
    }

}

