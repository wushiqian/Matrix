package com.shiqian.matrix.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.jaeger.library.StatusBarUtil;
import com.shiqian.matrix.R;
import com.shiqian.matrix.utils.FilterUtils;
import com.shiqian.matrix.utils.PhotoUtils;
import com.shiqian.matrix.view.EditImageView;
import com.xw.repo.BubbleSeekBar;

import java.io.FileNotFoundException;
import java.util.Objects;

import es.dmoral.toasty.Toasty;

//TODO //分享，保存, 手势
//FIXME

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        View.OnClickListener {

    /**
     * DATA
     */
    private static final String TAG = "MainActivity";

    //按钮状态
    private static int COLOR_PANEL = 0;
    private static int BRUSH = 0;
    private static int FILTER = 0;
    private static int DETAILS = 0;
    private static int CROP = 0;
    private static int DRAW = 0;
    private static int CUT = 0;

    //图片
    private Bitmap mBitmap;

    //色调饱和度亮度
    private float mHue = 0.0f;
    private float mSaturation = 1f;
    private float mLum = 1f;
    private int MID_VALUE = 127;

    //是否已保存或分享
    private boolean isSaved = false;
    private boolean isShared = false;

    /**
     * UI
     */
    private EditImageView mEditImageView;
    private ImageButton mColorPanel;
    private ImageButton mBrush;
    private LinearLayout detailsBar;
    private LinearLayout rotateBar;
    private LinearLayout filterBar;
    private LinearLayout cropBar;
    private LinearLayout paintBar;
    private LinearLayout okBar;
    private LinearLayout mainBar;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initPaintMode();
    }

    /**
     * 初始化模式
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initPaintMode() {
        mEditImageView.initializePen();
        mEditImageView.setPenSize(10);
        mEditImageView.setPenColor(getColor(R.color.red));
    }

    /**
     * 加载菜单
     *
     * @param menu 菜单
     * @return 是否有菜单
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    /**
     * 菜单点击事件
     *
     * @param item 子选项
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                String sdcardPath = Environment.getExternalStorageDirectory().toString();
                if (mEditImageView.saveImage(sdcardPath, "Image", Bitmap.CompressFormat.JPEG,
                        100)) {
                    Toasty.success(this, "Save Success", Toast.LENGTH_SHORT).show();
                    isSaved = true;
                }
                break;
            case R.id.share:
                String shareString = "分享照片";
                shareImage(shareString);

                break;
            case R.id.setting:
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    /**
     * 初始化界面
     */
    private void initView() {
        StatusBarUtil.setColorNoTranslucent(this, getResources().getColor(R.color.zhihu_primary));
        isSaved = false;
        isShared = false;
        detailsBar = findViewById(R.id.details_bar);
        filterBar = findViewById(R.id.filter_bar);
        rotateBar = findViewById(R.id.rotate_bar);
        cropBar = findViewById(R.id.crop_bar);
        paintBar = findViewById(R.id.paint_bar);
        okBar = findViewById(R.id.ok_bar);
        mainBar = findViewById(R.id.main_bar);

        mEditImageView = findViewById(R.id.iv_main);
        mBrush = findViewById(R.id.brush);
        mColorPanel = findViewById(R.id.color_panel);
        ImageButton undo = findViewById(R.id.undo);
        ImageButton delete = findViewById(R.id.delete);

        mBrush.setOnClickListener(this);
        mColorPanel.setOnClickListener(this);
        undo.setOnClickListener(this);
        delete.setOnClickListener(this);

        ImageButton filter = findViewById(R.id.filter);
        ImageButton details = findViewById(R.id.details);
        ImageButton crop = findViewById(R.id.Crop);
        ImageButton draw = findViewById(R.id.Draw);

        filter.setOnClickListener(this);
        details.setOnClickListener(this);
        crop.setOnClickListener(this);
        draw.setOnClickListener(this);

        ImageButton OK = findViewById(R.id.OK);
        ImageButton cancel = findViewById(R.id.Cancel);
        OK.setOnClickListener(this);
        cancel.setOnClickListener(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSaved || isShared) {
                    finish();//返回
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("你确定要返回吗");
                    builder.setMessage("你还没有保存或分享");
                    builder.setCancelable(true);
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    builder.show();
                }

            }
        });
        SeekBar hueSeekBar = findViewById(R.id.sb_hue);
        SeekBar satSeekBar = findViewById(R.id.sb_saturation);
        SeekBar lumSeekBar = findViewById(R.id.sb_lum);
        BubbleSeekBar rotateSeekBar = findViewById(R.id.sb_rotate);
        int MAX_VALUE = 255;
        hueSeekBar.setMax(MAX_VALUE);
        lumSeekBar.setMax(MAX_VALUE);
        satSeekBar.setMax(MAX_VALUE);
        rotateSeekBar.setProgress(0);
        hueSeekBar.setProgress(MID_VALUE);
        satSeekBar.setProgress(MID_VALUE);
        lumSeekBar.setProgress(MID_VALUE);
        hueSeekBar.setOnSeekBarChangeListener(this);
        satSeekBar.setOnSeekBarChangeListener(this);
        lumSeekBar.setOnSeekBarChangeListener(this);
        rotateSeekBar.setOnProgressChangedListener((new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat,
                                          boolean fromUser) {
                switch (bubbleSeekBar.getId()) {
                    case R.id.sb_rotate:
                        mEditImageView.loadImage(PhotoUtils.rotateImage(mBitmap, progress));
                        break;
                }
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress,
                                              float progressFloat) {
            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress,
                                             float progressFloat, boolean fromUser) {

            }
        }));
        hueSeekBar.setProgress(MID_VALUE);
        satSeekBar.setProgress(MID_VALUE);
        lumSeekBar.setProgress(MID_VALUE);
        initPhoto();
    }

    private void initPhoto() {
        Bundle extras = getIntent().getExtras();
        assert extras != null;
        Uri photo;
        photo = Uri.parse(extras.getString("Photo"));
        try {
            mBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(photo));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mBitmap = PhotoUtils.ScaleImage(mBitmap, 2, 2);
        mEditImageView.loadImage(mBitmap);
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
        Log.d(TAG, "seekId:" + seekBar.getId() + ", progress" + progress);
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
        mEditImageView.loadImage(PhotoUtils.handleImageEffect(mBitmap, mHue, mSaturation, mLum));
    }

    /**
     * 启动裁剪
     */
    public void btnCrop(View view) {
        if (CUT == 0) {
            mEditImageView.setDrawMode(EditImageView.CROP);
            rotateBar.setVisibility(View.GONE);
            cropBar.setVisibility(View.GONE);
        }
        mEditImageView.setImageBitmap(mEditImageView.getImageBitmap());
        CUT = 1 - CUT;
    }

    /**
     * 滤镜
     */
    public void btnNegative(View view) {
        mEditImageView.loadImage(FilterUtils.handleImageNegative(mBitmap));
    }

    public void btnOld(View view) {
        mEditImageView.loadImage(FilterUtils.handleImageOld(mBitmap));
    }

    public void btnEmboss(View view) {
        mEditImageView.loadImage(FilterUtils.handleImageEmboss(mBitmap));
    }

    public void btnPolaroid(View view) {
        mEditImageView.loadImage(FilterUtils.handleImagePolaroid(mBitmap));
    }

    public void btnCool(View view) {
        mEditImageView.loadImage(FilterUtils.handleImageCool(mBitmap));
    }

    public void btnNeon(View view) {
        mEditImageView.loadImage(FilterUtils.handleImageNeon(mBitmap));
    }

    public void btnRotate(View view) {
        mEditImageView.loadImage(PhotoUtils.rotateImage(mBitmap, 90));
        mBitmap = PhotoUtils.rotateImage(mBitmap, 90);
    }

    public void btnScale(View view) {
        mEditImageView.loadImage(PhotoUtils.ScaleImage(mBitmap, 2, 2));
    }

    public void btnReScale(View view) {
        mEditImageView.loadImage(PhotoUtils.ScaleImage(mBitmap, 0.5f, 0.5f));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.brush:
                mBrush.setImageResource(BRUSH == 0 ? R.drawable.ic_brush : R.drawable.ic_pen);
                mEditImageView.setPenSize(BRUSH == 0 ? 40 : 10);
                BRUSH = 1 - BRUSH;
                break;
            case R.id.color_panel:
                COLOR_PANEL++;
                if (COLOR_PANEL == 2) {
                    mColorPanel.setImageResource(R.drawable.ic_color_green);
                    mEditImageView.setPenColor(getColor(R.color.green));
                    COLOR_PANEL = -1;
                } else {
                    mColorPanel.setImageResource(COLOR_PANEL == 1 ? R.drawable.ic_color_blue :
                            R.drawable.ic_color_red);
                    mEditImageView.setPenColor(COLOR_PANEL == 1 ? getColor(R.color.blue) :
                            getColor(R.color.red));
                }
                break;
            case R.id.undo:
                mEditImageView.undo();
                break;
            case R.id.delete:
                mEditImageView.clear();
                break;
            case R.id.filter:
                close();
                mainBar.setVisibility(View.GONE);
                okBar.setVisibility(FILTER == 0 ? View.VISIBLE : View.GONE);
                filterBar.setVisibility(FILTER == 0 ? View.VISIBLE : View.GONE);
                if (FILTER == 0) allZero();
                FILTER = 1 - FILTER;
                break;
            case R.id.details:
                close();
                mainBar.setVisibility(View.GONE);
                okBar.setVisibility(DETAILS == 0 ? View.VISIBLE : View.GONE);
                detailsBar.setVisibility(DETAILS == 0 ? View.VISIBLE : View.GONE);
                if (DETAILS == 0) allZero();
                DETAILS = 1 - DETAILS;
                break;
            case R.id.Crop:
                close();
                mainBar.setVisibility(View.GONE);
                if (CROP == 0) {
                    mEditImageView.setDrawMode(EditImageView.SCALE);
                } else {
                    mEditImageView.setDrawMode(EditImageView.NONE);
                }
                okBar.setVisibility(CROP == 0 ? View.VISIBLE : View.GONE);
                rotateBar.setVisibility(CROP == 0 ? View.VISIBLE : View.GONE);
                cropBar.setVisibility(CROP == 0 ? View.VISIBLE : View.GONE);
                if (CROP == 0) allZero();
                CROP = 1 - CROP;
                break;
            case R.id.Draw:
                close();
                mainBar.setVisibility(View.GONE);
                mEditImageView.loadImage(mBitmap);
                mEditImageView.setDrawMode(EditImageView.DRAW);
                if (DRAW == 1) mEditImageView.setDrawMode(EditImageView.SCALE);
                mEditImageView.loadImage(mBitmap);
                okBar.setVisibility(DRAW == 0 ? View.VISIBLE : View.GONE);
                paintBar.setVisibility(DRAW == 0 ? View.VISIBLE : View.GONE);
                if (DRAW == 0) allZero();
                DRAW = 1 - DRAW;
                break;
            case R.id.OK:
                if (CUT == 1) {
                    mBitmap = mEditImageView.getCroppedImage();
                    mEditImageView.loadImage(mBitmap);
                } else {
                    mBitmap = mEditImageView.getImageBitmap();
                    mEditImageView.loadImage(mBitmap);
                }

                allZero();
                mEditImageView.setDrawMode(EditImageView.NONE);
                close();
                mainBar.setVisibility(View.VISIBLE);
                break;
            case R.id.Cancel:
                mEditImageView.loadImage(mBitmap);
                mEditImageView.clear();
                allZero();
                mEditImageView.setDrawMode(EditImageView.NONE);
                close();
                mainBar.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void allZero() {
        FILTER = 0;
        CROP = 0;
        DETAILS = 0;
        DRAW = 0;
        CUT = 0;
    }

    private void close() {
        if (okBar.getVisibility() == View.VISIBLE) okBar.setVisibility(View.GONE);
        if (filterBar.getVisibility() == View.VISIBLE) filterBar.setVisibility(View.GONE);
        else if (paintBar.getVisibility() == View.VISIBLE) paintBar.setVisibility(View.GONE);
        else if (cropBar.getVisibility() == View.VISIBLE) {
            cropBar.setVisibility(View.GONE);
            rotateBar.setVisibility(View.GONE);
        } else if (detailsBar.getVisibility() == View.VISIBLE) detailsBar.setVisibility(View.GONE);
    }

    public void shareImage(String title) {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();
        Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), mBitmap,
                null, null));
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);//设置分享行为
        intent.setType("image/*");//设置分享内容的类型
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent = Intent.createChooser(intent, title);
        startActivity(intent);
        isShared = true;
    }

}