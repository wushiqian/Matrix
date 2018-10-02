package com.shiqian.matrix;
/*
 * 包名：Matrix
 * 文件名： PhotoActivity
 * 创建者：wushiqian
 * 创建时间 2018/9/22 上午9:34
 * 描述： TODO//
 */

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jaeger.library.StatusBarUtil;
import com.shiqian.matrix.view.DrawingView;

import es.dmoral.toasty.Toasty;

public class PaintActivity extends AppCompatActivity implements View.OnClickListener{

    /**
     * DATA
     */
    private static final String TAG = "PaintActivity";
    //按钮状态
    private static int COLOR_PANEL = 0;
    private static int BRUSH = 0;
    private Uri photo = null;
    private Bitmap mBitmap;

    /**
     * UI
     */
    private DrawingView mDrawingView;
    private ImageButton mColorPanel;
    private ImageButton mBrush;
    private ImageButton mUndo;
    private ImageButton mSave;
    private ImageButton mOK;
    private ImageButton mCancel;
    private LinearLayout paintBar;
    private LinearLayout okBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paint);
        initView();
    }

    private void initView() {
        StatusBarUtil.setColor(this, getResources().getColor(R.color.zhihu_primary));
        mDrawingView = findViewById(R.id.iv_paint);
        paintBar = findViewById(R.id.paint_bar);
        okBar = findViewById(R.id.ok_bar);
        mBrush = findViewById(R.id.brush);
        mColorPanel = findViewById(R.id.color_panel);
        mUndo = findViewById(R.id.undo);
        mSave = findViewById(R.id.save);
        mBrush.setOnClickListener(this);
        mColorPanel.setOnClickListener(this);
        mUndo.setOnClickListener(this);
        mSave.setOnClickListener(this);
        mOK = findViewById(R.id.OK);
        mCancel = findViewById(R.id.Cancel);
        mOK.setOnClickListener(this);
        mCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.brush:
                mBrush.setImageResource(BRUSH == 0 ? R.drawable.ic_brush : R.drawable.ic_pen);
//                mDrawingView.setPenSize(BRUSH == 0 ? 40 : 10);
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
                if (mDrawingView.saveImage(sdcardPath, "DrawImg", Bitmap.CompressFormat.PNG, 100)) {
                    Toasty.success(this, "Save Success", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.OK:
                mBitmap = mDrawingView.getImageBitmap();
                mDrawingView.setDrawMode(DrawingView.SCALE);
                break;
            case R.id.Cancel:
                mDrawingView.loadImage(mBitmap);
                mDrawingView.clear();
                mDrawingView.setDrawMode(DrawingView.SCALE);
                break;
            default:
                break;
        }
    }

}
