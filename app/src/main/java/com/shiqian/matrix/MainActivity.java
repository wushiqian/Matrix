package com.shiqian.matrix;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.SeekBar;
import android.widget.Toast;

import com.shiqian.matrix.view.DrawingView;
import com.shiqian.photoedit.utils.MatisseGlide;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.List;

import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private static final String FILE_PATH = "com.shiqian.matrix.fileprovider";
    private static final String TAG = "MainActivity";
    private int REQUEST_CODE_CHOOSE = 2;

    private DrawingView mDrawingView;
    private static int COLOR_PANEL = 0;
    private static int BRUSH = 0;
    private ImageButton mColorPanel;
    private ImageButton mBrush;
    private ImageButton mUndo;
    private ImageButton mSave;

    List<Uri> mSelected;
    private Uri photo = null;
    private Bitmap mBitmap;

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
        initPaintMode();
    }

    private void initPaintMode() {
        mDrawingView.initializePen();
        mDrawingView.setPenSize(10);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mDrawingView.setPenColor(getColor(R.color.red));
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
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
        return true;
    }

    private void initView() {
        mDrawingView = findViewById(R.id.iv_main);
        mBrush = findViewById(R.id.brush);
        mColorPanel = findViewById(R.id.color_panel);
        mUndo = findViewById(R.id.undo);
        mSave = findViewById(R.id.save);

        mBrush.setOnClickListener(this);
        mColorPanel.setOnClickListener(this);
        mUndo.setOnClickListener(this);
        mSave.setOnClickListener(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
        try {
            mBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(photo));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mDrawingView.loadImage(handleImageEffect(mBitmap, mHue, mSaturation, mLum));
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

    //申请权限回调
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
            Uri destinationUri = mSelected.get(0);
            Log.d("Matisse", "mSelected: " + mSelected);
            startCrop();
            try {
                mBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(destinationUri));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (resultCode == RESULT_OK) {
            //裁切成功
            if (requestCode == UCrop.REQUEST_CROP) {
                Uri croppedFileUri = UCrop.getOutput(data);
//                savePhoto(croppedFileUri);
                try {
                    mBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(croppedFileUri));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                // 使用Glide显示图片
//                Glide.with(this)
//                        .load(croppedFileUri)
//                        .into(mImageView);
                mDrawingView.loadImage(mBitmap);
                photo = croppedFileUri;
                hueSeekBar.setProgress(MID_VALUE);
                satSeekBar.setProgress(MID_VALUE);
                lumSeekBar.setProgress(MID_VALUE);
            }
        }
        //裁切失败
        if (resultCode == UCrop.RESULT_ERROR) {
            Toast.makeText(this, "裁切图片失败", Toast.LENGTH_SHORT).show();
        }
    }

    //保存裁剪后的照片
    private void savePhoto(Uri croppedFileUri) {
        //获取默认的下载目录
        String downloadsDirectoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        String filename = String.format("%d_%s", Calendar.getInstance().getTimeInMillis(), croppedFileUri.getLastPathSegment());
        File saveFile = new File(downloadsDirectoryPath, filename);
        //保存下载的图片
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inStream = new FileInputStream(new File(croppedFileUri.getPath()));
            outStream = new FileOutputStream(saveFile);
            inChannel = inStream.getChannel();
            outChannel = outStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            Toasty.success(this, "Success!", Toast.LENGTH_SHORT, true).show();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outChannel.close();
                outStream.close();
                inChannel.close();
                inStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        options.setMaxScaleMultiplier(5);
        //设置裁剪框横竖线的颜色
//        options.setCropGridColor(Color.GREEN);
        uCrop.withOptions(options);
        uCrop = uCrop.useSourceImageAspectRatio();
        uCrop.withMaxResultSize(500, 500)
                .start(this);
    }

    public void btnNegative(View view) {
        mDrawingView.loadImage(handleImageNegative(mBitmap));
    }

    private Bitmap handleImageNegative(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);
        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            r = 255 - r;
            g = 255 - g;
            b = 255 - b;

            if (r > 255) {
                r = 255;
            } else if (r < 0) {
                r = 0;
            }
            if (g > 255) {
                g = 255;
            } else if (g < 0) {
                g = 0;
            }
            if (b > 255) {
                b = 255;
            } else if (b < 0) {
                b = 0;
            }
            newPx[i] = Color.argb(a, r, g, b);
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        return bmp;
    }

    public void btnOld(View view) {
        mDrawingView.loadImage(handleImageOld(mBitmap));
    }

    private Bitmap handleImageOld(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);
        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            r = (int) (0.393 * r + 0.769 * g + 0.189 * b);
            g = (int) (0.349 * r + 0.686 * g + 0.168 * b);
            b = (int) (0.272 * r + 0.534 * g + 0.131 * b);

            newPx[i] = Color.argb(a, r, g, b);
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        return bmp;
    }

    public void btnEmboss(View view) {
        mDrawingView.loadImage(handleImageEmboss(mBitmap));
    }

    private Bitmap handleImageEmboss(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color, color2;
        int r, g, b, a, r1, g1, b1;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);
        for (int i = 0; i < width * height - 1; i++) {
            color = oldPx[i];
            color2 = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);
            r1 = Color.red(color2);
            g1 = Color.green(color2);
            b1 = Color.blue(color2);

            r = r1 - r + 127;
            g = g1 - g + 127;
            b = b1 - b + 127;

            newPx[i] = Color.argb(a, r, g, b);
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        return bmp;
    }

    public void btnRotate(View view) {
        rotate(90);
    }

    public void btnReRotate(View view) {
        rotate(-90);
    }

    private void rotate(int dregress) {
        Matrix matrix = new Matrix(); //旋转图片 动作
        matrix.setRotate(dregress);//旋转角度
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight(); // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height, matrix, true);
        mBitmap = resizedBitmap;
        mDrawingView.loadImage(mBitmap);
    }

    public void btnScale(View view) {
        Matrix matrix = new Matrix();
        matrix.setScale(2,2);
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        Bitmap resizedBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height, matrix, true);
        mBitmap = resizedBitmap;
        mDrawingView.loadImage(mBitmap);
    }

    public void btnTranslate(View view) {
        Matrix matrix = new Matrix();
        matrix.setTranslate(100,100);
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight(); // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height, matrix, true);
        mBitmap = resizedBitmap;
        mDrawingView.loadImage(mBitmap);
    }

    public void btnSkew(View view) {
        Matrix matrix = new Matrix();
        matrix.setSkew(2,2);
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight(); // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height, matrix, true);
        mBitmap = resizedBitmap;
        mDrawingView.loadImage(mBitmap);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.brush:
                mBrush.setImageResource(BRUSH == 0 ? R.drawable.ic_brush : R.drawable.ic_pen);
                mDrawingView.setPenSize(BRUSH == 0 ? 40 : 10);
                BRUSH = 1 - BRUSH;
                break;
            case R.id.color_panel:
                mColorPanel.setImageResource(COLOR_PANEL == 0 ? R.drawable.ic_color_blue : R.drawable.ic_color_red);
                mDrawingView.setPenColor(COLOR_PANEL == 0 ? getColor(R.color.blue) : getColor(R.color.red));
                COLOR_PANEL = 1 - COLOR_PANEL;
                break;
            case R.id.undo:
                mDrawingView.undo();
                break;
            case R.id.save:
                String sdcardPath = Environment.getExternalStorageDirectory().toString();
                if (mDrawingView.saveImage(sdcardPath, "DrawImg", Bitmap.CompressFormat.PNG, 100)){
                    Toasty.success(this, "Save Success", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }
}

