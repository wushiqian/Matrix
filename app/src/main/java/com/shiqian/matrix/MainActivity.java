package com.shiqian.matrix;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.jaeger.library.StatusBarUtil;
import com.shiqian.matrix.utils.PhotoUtils;
import com.shiqian.matrix.utils.StaticUtils;
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
    private static int FILTER = 0;
    private static int DETAILS = 0;
    private static int CROP = 0;
    private static int DRAW = 0;
    private ImageButton mColorPanel;
    private ImageButton mBrush;
    private ImageButton mUndo;
    private ImageButton mSave;
    private ImageButton mFilter;
    private ImageButton mDetails;
    private ImageButton mCrop;
    private ImageButton mDraw;
    private ImageButton mOK;
    private ImageButton mCancel;

    List<Uri> mSelected;
    private Uri photo = null;
    private Bitmap mBitmap;

    private SeekBar hueSeekBar;
    private SeekBar satSeekBar;
    private SeekBar lumSeekBar;
    private SeekBar rotateSeekBar;

    private LinearLayout detailsBar;
    private LinearLayout rotateBar;
    private LinearLayout filterBar;
    private LinearLayout cropBar;
    private LinearLayout paintBar;
    private LinearLayout okBar;

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
        StatusBarUtil.setColor(this,getResources().getColor(R.color.zhihu_primary));

        detailsBar = findViewById(R.id.details_bar);
        filterBar = findViewById(R.id.filter_bar);
        rotateBar = findViewById(R.id.rotate_bar);
        cropBar = findViewById(R.id.crop_bar);
        paintBar = findViewById(R.id.paint_bar);
        okBar = findViewById(R.id.ok_bar);

        mDrawingView = findViewById(R.id.iv_main);
        mDrawingView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        mBrush = findViewById(R.id.brush);
        mColorPanel = findViewById(R.id.color_panel);
        mUndo = findViewById(R.id.undo);
        mSave = findViewById(R.id.save);

        mBrush.setOnClickListener(this);
        mColorPanel.setOnClickListener(this);
        mUndo.setOnClickListener(this);
        mSave.setOnClickListener(this);

        mFilter = findViewById(R.id.filter);
        mDetails = findViewById(R.id.details);
        mCrop = findViewById(R.id.Crop);
        mDraw = findViewById(R.id.Draw);

        mFilter.setOnClickListener(this);
        mDetails.setOnClickListener(this);
        mCrop.setOnClickListener(this);
        mDraw.setOnClickListener(this);

        mOK = findViewById(R.id.OK);
        mCancel = findViewById(R.id.Cancel);
        mOK.setOnClickListener(this);
        mCancel.setOnClickListener(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        hueSeekBar = findViewById(R.id.sb_hue);
        satSeekBar = findViewById(R.id.sb_saturation);
        lumSeekBar = findViewById(R.id.sb_lum);
        rotateSeekBar = findViewById(R.id.sb_rotate);
        hueSeekBar.setMax(MAX_VALUE);
        lumSeekBar.setMax(MAX_VALUE);
        satSeekBar.setMax(MAX_VALUE);
        rotateSeekBar.setMax(180);
        rotateSeekBar.setMin(-180);
        rotateSeekBar.setProgress(0);
        hueSeekBar.setProgress(MID_VALUE);
        satSeekBar.setProgress(MID_VALUE);
        lumSeekBar.setProgress(MID_VALUE);
        hueSeekBar.setOnSeekBarChangeListener(this);
        satSeekBar.setOnSeekBarChangeListener(this);
        lumSeekBar.setOnSeekBarChangeListener(this);
        rotateSeekBar.setOnSeekBarChangeListener(this);
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
        boolean isChanged = false;
        switch (seekBar.getId()) {
            case R.id.sb_hue:
                mHue = (progress - MID_VALUE) * 1.0F / MID_VALUE * 180;
                isChanged = true;
                break;
            case R.id.sb_saturation:
                mSaturation = progress * 1.0F / MID_VALUE;
                isChanged = true;
                break;
            case R.id.sb_lum:
                mLum = progress * 1.0F / MID_VALUE;
                isChanged = true;
                break;
            case R.id.sb_rotate:
//                mBitmap = PhotoUtils.rotateImage(mBitmap,progress);
                mDrawingView.loadImage(PhotoUtils.rotateImage(mBitmap,progress));
                break;
        }
        if(isChanged) mDrawingView.loadImage(PhotoUtils.handleImageEffect(mBitmap, mHue, mSaturation, mLum));
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
            Log.d("Matisse", "mSelected: " + mSelected);
            startCrop();
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
//                mBitmap = PhotoUtils.ScaleImage(mBitmap,2,2);
                Intent intent = new Intent();
                intent.putExtra(StaticUtils.NOW_PHOTO,mBitmap);
                mDrawingView.loadImage(mBitmap);
                mDrawingView.setScaleType(ImageView.ScaleType.CENTER_CROP);
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
//        options.setImageToCropBoundsAnimDuration(666);
        //设置最大缩放比例
        options.setMaxScaleMultiplier(4);
        // 设置图片压缩质量
//        options.setCompressionQuality(100);
        //设置裁剪框横竖线的颜色
//        options.setCropGridColor(Color.GREEN);
        uCrop.withOptions(options);
        uCrop = uCrop.useSourceImageAspectRatio();
        uCrop.withMaxResultSize(800, 800)
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
        mBitmap = PhotoUtils.rotateImage(mBitmap,60);
        mDrawingView.loadImage(mBitmap);
    }

    public void btnReRotate(View view) {
        mBitmap = PhotoUtils.rotateImage(mBitmap,-90);
        mDrawingView.loadImage(mBitmap);
    }

    public void btnScale(View view) {
        mBitmap = PhotoUtils.ScaleImage(mBitmap,2,2);
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
            case R.id.filter:
                close();
                okBar.setVisibility(FILTER == 0 ? View.VISIBLE : View.GONE);
                filterBar.setVisibility(FILTER == 0 ? View.VISIBLE : View.GONE);
                FILTER = 1 - FILTER;
                break;
            case R.id.details:
                close();
                okBar.setVisibility(DETAILS == 0 ? View.VISIBLE : View.GONE);
                detailsBar.setVisibility(DETAILS == 0 ? View.VISIBLE : View.GONE);
                DETAILS = 1 - DETAILS;
                break;
            case R.id.Crop:
                close();
                okBar.setVisibility(CROP == 0 ? View.VISIBLE : View.GONE);
                rotateBar.setVisibility(CROP == 0 ? View.VISIBLE : View.GONE);
                cropBar.setVisibility(CROP == 0 ? View.VISIBLE : View.GONE);
                CROP = 1 - CROP;
                break;
            case R.id.Draw:
                close();
                if(DRAW == 0) mDrawingView.NoDrawMode();
                else mDrawingView.DrawMode();
                okBar.setVisibility(DRAW == 0 ? View.VISIBLE : View.GONE);
                paintBar.setVisibility(DRAW == 0 ? View.VISIBLE : View.GONE);
                DRAW = 1 - DRAW;
                break;
            case R.id.OK:
                mBitmap = mDrawingView.getImageBitmap();
                close();
                break;
            case R.id.Cancel:
                close();
                break;
            default:
                break;
        }
    }

    private void close() {
        if(filterBar.getVisibility() == View.VISIBLE) filterBar.setVisibility(View.GONE);
        if(okBar.getVisibility() == View.VISIBLE) okBar.setVisibility(View.GONE);
        if(paintBar.getVisibility() == View.VISIBLE) paintBar.setVisibility(View.GONE);
        if(cropBar.getVisibility() == View.VISIBLE) cropBar.setVisibility(View.GONE);
        if(rotateBar.getVisibility() == View.VISIBLE) rotateBar.setVisibility(View.GONE);
        if(detailsBar.getVisibility() == View.VISIBLE) detailsBar.setVisibility(View.GONE);
    }
}

