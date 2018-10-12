package com.shiqian.matrix.view;
/*
 * 包名：Matrix
 * 文件名： EditImageView
 * 创建者：wushiqian
 * 创建时间 2018/10/1 12:50 PM
 * 描述： TODO//
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.shiqian.matrix.R;
import com.shiqian.matrix.handle.CropWindowEdgeSelector;
import com.shiqian.matrix.utils.CatchEdgeUtils;
import com.shiqian.matrix.utils.UIUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

/**
 * 多点触控加Matrix类实现图片的旋转、缩放、平移
 *
 * @attr R.styleable#TransformativeImageView_max_scale
 * @attr R.styleable#TransformativeImageView_min_scale
 * @attr R.styleable#TransformativeImageView_revert_duration
 * @attr R.styleable#TransformativeImageView_revert
 * @attr R.styleable#TransformativeImageView_scale_center
 */

public class EditImageView extends AppCompatImageView {

    private static final String TAG = EditImageView.class.getSimpleName();

    private static final float TOUCH_TOLERANCE = 4;

    /**
     * 模式
     */
    private int mMode;
    public static final int NONE = 0;
    public static final int DRAW = 1;
    public static final int SCALE = 2;
    public static final int CROP = 3;

    private static final float MAX_SCALE_FACTOR = 3.0f; // 默认最大缩放比例为3
    private static final float UNSPECIFIED_SCALE_FACTOR = -1f; // 未指定缩放比例
    private static final float MIN_SCALE_FACTOR = 0.5f; // 默认最小缩放比例为0.5
    private static final float INIT_SCALE_FACTOR = 1.0f; // 默认适应控件大小后的初始化缩放比例
    private static final int DEFAULT_REVERT_DURATION = 300;

    /**
     * 平移缩放旋转相关
     */
    private int mRevertDuration = DEFAULT_REVERT_DURATION; // 回弹动画时间
    private float mMaxScaleFactor = MAX_SCALE_FACTOR; // 最大缩放比例
    private float mMinScaleFactor = UNSPECIFIED_SCALE_FACTOR; // 此最小缩放比例优先级高于下面两个
    private float mVerticalMinScaleFactor = INIT_SCALE_FACTOR; // 图片最初的最小缩放比例
    private float mHorizontalMinScaleFactor = MIN_SCALE_FACTOR; // 图片旋转90（或-90）度后的的最小缩放比例
    protected Matrix mMatrix = new Matrix(); // 用于图片旋转、平移、缩放的矩阵
    protected RectF mImageRect = new RectF(); // 保存图片所在区域矩形，坐标为相对于本View的坐标
    private boolean mOpenScaleRevert = false; // 是否开启缩放回弹
    private boolean mOpenRotateRevert = false; // 是否开启旋转回弹
    private boolean mOpenTranslateRevert = false; // 是否开启平移回弹
    private boolean mOpenAnimator = false; // 是否开启动画

    //目标bitmap
    private Bitmap mBitmap;
    //原始bitmap
    private Bitmap mOriginBitmap;
    private Canvas mCanvas;

    /**
     * 涂鸦相关
     */
    //涂鸦路径
    private Path mPath;
    //涂鸦画笔
    private Paint mPaint;
    private float mX, mY;
    //长宽比例
    private float mProportion = 0;
    //保存的涂鸦路径
    private LinkedList<DrawPath> savePath;
    //上一条涂鸦路径
    private DrawPath mLastDrawPath;
    private Matrix matrix;

    //画笔大小和颜色
    private float mPaintBarPenSize;
    private int mPaintBarPenColor;

    /**
     * 裁剪相关
     */
    //裁剪框边框画笔
    private Paint mBorderPaint;

    //裁剪框九宫格画笔
    private Paint mGuidelinePaint;

    //绘制裁剪边框四个角的画笔
    private Paint mCornerPaint;

    //判断手指位置是否处于缩放裁剪框位置的范围：如果是当手指移动的时候裁剪框会相应的变化大小
    //否则手指移动的时候就是拖动裁剪框使之随着手指移动
    private float mScaleRadius;

    private float mCornerThickness;

    private float mBorderThickness;

    //四个角小短边的长度
    private float mCornerLength;

    //用来表示图片边界的矩形
    private RectF mBitmapRect = new RectF();

    //手指位置距离裁剪框的偏移量
    private PointF mTouchOffset = new PointF();

    private CropWindowEdgeSelector mPressedCropWindowEdgeSelector;

    /**
     * 设置模式
     *
     * @param drawMode 模式
     */
    public void setDrawMode(int drawMode) {
        mMode = drawMode;
    }


    public EditImageView(Context context) {
        this(context, null);
    }

    public EditImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        obtainAttrs(attrs);
        init(context);
        initializePen();
    }

    private void obtainAttrs(AttributeSet attrs) {
        if (attrs == null) return;
        TypedArray typedArray;
        typedArray = getContext()
                .obtainStyledAttributes(attrs, R.styleable.EditImageView);
        mMaxScaleFactor = typedArray.getFloat(
                R.styleable.EditImageView_max_scale, MAX_SCALE_FACTOR);
        mMinScaleFactor = typedArray.getFloat(
                R.styleable.EditImageView_min_scale, UNSPECIFIED_SCALE_FACTOR);
        mRevertDuration = typedArray.getInteger(
                R.styleable.EditImageView_revert_duration, DEFAULT_REVERT_DURATION);
        mOpenScaleRevert = typedArray.getBoolean(
                R.styleable.EditImageView_open_scale_revert, false);
        mOpenRotateRevert = typedArray.getBoolean(
                R.styleable.EditImageView_open_rotate_revert, false);
        mOpenTranslateRevert = typedArray.getBoolean(
                R.styleable.EditImageView_open_translate_revert, false);
        mOpenAnimator = typedArray.getBoolean(
                R.styleable.EditImageView_open_animator, true);
        mScaleBy = typedArray.getInt(
                R.styleable.EditImageView_scale_center, SCALE_BY_IMAGE_CENTER);
        typedArray.recycle();
    }

    private void init(Context context) {
        // FIXME 修复图片锯齿,关闭硬件加速ANTI_ALIAS_FLAG才能生效
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setScaleType(ScaleType.MATRIX);
        mRevertAnimator.setDuration(mRevertDuration);

        mMode = NONE;
        savePath = new LinkedList<>();
        matrix = new Matrix();

        mBorderPaint = new Paint();
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(UIUtils.dip2px(context, 3));
        mBorderPaint.setColor(Color.parseColor("#AAFFFFFF"));

        mGuidelinePaint = new Paint();
        mGuidelinePaint.setStyle(Paint.Style.STROKE);
        mGuidelinePaint.setStrokeWidth(UIUtils.dip2px(context, 1));
        mGuidelinePaint.setColor(Color.parseColor("#AAFFFFFF"));

        mCornerPaint = new Paint();
        mCornerPaint.setStyle(Paint.Style.STROKE);
        mCornerPaint.setStrokeWidth(UIUtils.dip2px(context, 5));
        mCornerPaint.setColor(Color.WHITE);

        mScaleRadius = UIUtils.dip2px(context, 24);
        mBorderThickness = UIUtils.dip2px(context, 3);
        mCornerThickness = UIUtils.dip2px(context, 5);
        mCornerLength = UIUtils.dip2px(context, 20);
    }

    /**
     * 初始化画笔
     */
    public void initializePen() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setFilterBitmap(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        //初始化位置和大小
        initImgPositionAndSize();
        //初始化裁剪框
        mBitmapRect = getBitmapRect();
        initCropWindow(mBitmapRect);
    }

    /**
     * 初始化图片位置和大小
     */
    private void initImgPositionAndSize() {
        mMatrix.reset();
        // 初始化ImageRect
        refreshImageRect();

        // 计算缩放比例，使图片适应控件大小
        mHorizontalMinScaleFactor = Math.min(getWidth() / mImageRect.width(),
                getHeight() / mImageRect.height());
        mVerticalMinScaleFactor = Math.min(getHeight() / mImageRect.width(),
                getWidth() / mImageRect.height());

        float scaleFactor = mHorizontalMinScaleFactor;

        scaleFactor *= INIT_SCALE_FACTOR;
        mScaleFactor = scaleFactor;
        mMatrix.postScale(scaleFactor, scaleFactor, mImageRect.centerX(), mImageRect.centerY());
        refreshImageRect();
        // 移动图片到中心
        mMatrix.postTranslate((getRight() - getLeft()) / 2 - mImageRect.centerX(),
                (getBottom() - getTop()) / 2 - mImageRect.centerY());
        applyMatrix();

        // 如果用户有指定最小缩放比例则使用用户指定的
        if (mMinScaleFactor != UNSPECIFIED_SCALE_FACTOR) {
            mHorizontalMinScaleFactor = mMinScaleFactor;
            mVerticalMinScaleFactor = mMinScaleFactor;
        }
    }

    private PaintFlagsDrawFilter mDrawFilter =
            new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.setDrawFilter(mDrawFilter);
        super.onDraw(canvas);
        if (mMode == DRAW) {
            Draw(canvas);
        }
        if (mMode == CROP) {
            //绘制九宫格引导线
            drawGuidelines(canvas);
            //绘制裁剪边框
            drawBorder(canvas);
            //绘制裁剪边框的四个角
            drawCorners(canvas);
        }
    }

    public void Draw(Canvas canvas) {
        Log.i(TAG, "canvas.getHeight()" + canvas.getHeight() + "mBitmap.getHeight()" + mBitmap.getHeight());
        // 根据图片尺寸缩放图片，高大于宽的情况
        float proportion;
        if (mBitmap.getHeight() > mBitmap.getWidth()) {
            proportion = (float) canvas.getHeight() / mBitmap.getHeight();
        } else {
            proportion = (float) canvas.getWidth() / mBitmap.getWidth();
        }
        mProportion = proportion;
        Log.d(TAG, "proportion: " + proportion);
    }


    private void drawGuidelines(@NonNull Canvas canvas) {

        final float left = Edge.LEFT.getCoordinate();
        final float top = Edge.TOP.getCoordinate();
        final float right = Edge.RIGHT.getCoordinate();
        final float bottom = Edge.BOTTOM.getCoordinate();

        final float oneThirdCropWidth = Edge.getWidth() / 3;

        final float x1 = left + oneThirdCropWidth;
        //引导线竖直方向第一条线
        canvas.drawLine(x1, top, x1, bottom, mGuidelinePaint);
        final float x2 = right - oneThirdCropWidth;
        //引导线竖直方向第二条线
        canvas.drawLine(x2, top, x2, bottom, mGuidelinePaint);

        final float oneThirdCropHeight = Edge.getHeight() / 3;

        final float y1 = top + oneThirdCropHeight;
        //引导线水平方向第一条线
        canvas.drawLine(left, y1, right, y1, mGuidelinePaint);
        final float y2 = bottom - oneThirdCropHeight;
        //引导线水平方向第二条线
        canvas.drawLine(left, y2, right, y2, mGuidelinePaint);
    }

    private void drawBorder(@NonNull Canvas canvas) {

        canvas.drawRect(Edge.LEFT.getCoordinate(),
                Edge.TOP.getCoordinate(),
                Edge.RIGHT.getCoordinate(),
                Edge.BOTTOM.getCoordinate(),
                mBorderPaint);
    }

    private void drawCorners(@NonNull Canvas canvas) {

        final float left = Edge.LEFT.getCoordinate();
        final float top = Edge.TOP.getCoordinate();
        final float right = Edge.RIGHT.getCoordinate();
        final float bottom = Edge.BOTTOM.getCoordinate();

        //简单的数学计算

        final float lateralOffset = (mCornerThickness - mBorderThickness) / 2f;
        final float startOffset = mCornerThickness - (mBorderThickness / 2f);

        //左上角左面的短线
        canvas.drawLine(left - lateralOffset, top - startOffset, left - lateralOffset, top + mCornerLength, mCornerPaint);
        //左上角上面的短线
        canvas.drawLine(left - startOffset, top - lateralOffset, left + mCornerLength, top - lateralOffset, mCornerPaint);

        //右上角右面的短线
        canvas.drawLine(right + lateralOffset, top - startOffset, right + lateralOffset, top + mCornerLength, mCornerPaint);
        //右上角上面的短线
        canvas.drawLine(right + startOffset, top - lateralOffset, right - mCornerLength, top - lateralOffset, mCornerPaint);

        //左下角左面的短线
        canvas.drawLine(left - lateralOffset, bottom + startOffset, left - lateralOffset, bottom - mCornerLength, mCornerPaint);
        //左下角底部的短线
        canvas.drawLine(left - startOffset, bottom + lateralOffset, left + mCornerLength, bottom + lateralOffset, mCornerPaint);

        //右下角左面的短线
        canvas.drawLine(right + lateralOffset, bottom + startOffset, right + lateralOffset, bottom - mCornerLength, mCornerPaint);
        //右下角底部的短线
        canvas.drawLine(right + startOffset, bottom + lateralOffset, right - mCornerLength, bottom + lateralOffset, mCornerPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        }
        mCanvas = new Canvas(mBitmap);
        mCanvas.drawColor(Color.TRANSPARENT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = getMeasureWidth(widthMeasureSpec);
        int heightSize = getMeasureHeight(heightMeasureSpec);
        if (mBitmap != null) {
            if ((mBitmap.getHeight() > heightSize) && (mBitmap.getHeight() > mBitmap.getWidth())) {
                widthSize = heightSize * mBitmap.getWidth() / mBitmap.getHeight();
            } else if ((mBitmap.getWidth() > widthSize) && (mBitmap.getWidth() > mBitmap.getHeight())) {
                heightSize = widthSize * mBitmap.getHeight() / mBitmap.getWidth();
            } else {
                heightSize = mBitmap.getHeight();
                widthSize = mBitmap.getWidth();
            }
        }
        Log.d(TAG,"onMeasure");
        setMeasuredDimension(widthSize, heightSize);

    }

    private int getMeasureHeight(int heightMeasureSpec) {
        int result = 0;
        int size = MeasureSpec.getSize(heightMeasureSpec);  //每次调用此方法，测量用到的size会发生变化
        int mode = MeasureSpec.getMode(heightMeasureSpec);  //根据定义的Layout_width,Layout_height，会对此值产生影响
        if (mode == MeasureSpec.EXACTLY) {
            result = size;
        } else if (mode == MeasureSpec.UNSPECIFIED) {
            result = (int) mPaint.measureText("") + getPaddingLeft()
                    + getPaddingRight();
        } else {
            result = Math.min(result, size);
        }
        System.out.println("Height size:" + size);
        System.out.println("Height mode:" + mode);
        return result;
    }

    private int getMeasureWidth(int widthMeasureSpec) {
        int result = 0;
        int size = MeasureSpec.getSize(widthMeasureSpec);
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            result = size;
        } else if (mode == MeasureSpec.UNSPECIFIED) {
            result = (int) mPaint.measureText("") + getPaddingTop()
                    + getPaddingBottom();
        } else {
            result = Math.min(result, size);
        }
        System.out.println("Width size:" + size);
        System.out.println("Width mode:" + mode);
        return result;
    }

    /**
     * 路径对象
     */
    private class DrawPath {
        Path path;
        int paintColor;
        float paintWidth;

        DrawPath(Path path, int paintColor, float paintWidth) {
            this.path = path;
            this.paintColor = paintColor;
            this.paintWidth = paintWidth;
        }

        int getPaintColor() {
            return paintColor;
        }

        float getPaintWidth() {
            return paintWidth;
        }

    }

    private PointF mLastPoint1 = new PointF(); // 上次事件的第一个触点
    private PointF mLastPoint2 = new PointF(); // 上次事件的第二个触点
    private PointF mCurrentPoint1 = new PointF(); // 本次事件的第一个触点
    private PointF mCurrentPoint2 = new PointF(); // 本次事件的第二个触点
    private float mScaleFactor = 1.0f; // 当前的缩放倍数
    private boolean mCanScale = false; // 是否可以缩放

    protected PointF mLastMidPoint = new PointF(); // 图片平移时记录上一次ACTION_MOVE的点
    private PointF mCurrentMidPoint = new PointF(); // 当前各触点的中点
    protected boolean mCanDrag = false; // 是否可以平移

    private PointF mLastVector = new PointF(); // 记录上一次触摸事件两指所表示的向量
    private PointF mCurrentVector = new PointF(); // 记录当前触摸事件两指所表示的向量
    private boolean mCanRotate = false; // 判断是否可以旋转

    private MatrixRevertAnimator mRevertAnimator = new MatrixRevertAnimator(); // 回弹动画
    private float[] mFromMatrixValue = new float[9]; // 动画初始时矩阵值
    private float[] mToMatrixValue = new float[9]; // 动画终结时矩阵值

    protected boolean isTransforming = false; // 图片是否正在变化

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        PointF midPoint = getMidPointOfFinger(event);
        float x;
        float y;
        if (mProportion != 0) {
            x = (event.getX()) / mProportion;
            y = event.getY() / mProportion;
        } else {
            x = event.getX();
            y = event.getY();
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (mMode == CROP) {
                    onActionDown(event.getX(), event.getY());
                    return true;
                }
                if (mMode == DRAW) {
                    // This happens when we undo a path
                    if (mLastDrawPath != null) {
                        mPaint.setColor(mPaintBarPenColor);
                        mPaint.setStrokeWidth(mPaintBarPenSize);
                    }
                    mPath = new Path();
                    mPath.reset();
                    mPath.moveTo(x, y);
                    mX = x;
                    mY = y;
                    Log.d(TAG, "DOWN");
                    mCanvas.drawPath(mPath, mPaint);
                }
            case MotionEvent.ACTION_POINTER_DOWN:
                if (mMode == SCALE) {
                    // 每次触摸事件开始都初始化mLastMidPonit
                    mLastMidPoint.set(midPoint);
                    isTransforming = false;
                    mRevertAnimator.cancel();
                    // 新手指落下则需要重新判断是否可以对图片进行变换
                    mCanRotate = false;
                    mCanScale = false;
                    mCanDrag = false;
                    if (event.getPointerCount() == 2) {
                        // 旋转、平移、缩放分别使用三个判断变量，避免后期某个操作执行条件改变
                        mCanScale = true;
                        mLastPoint1.set(event.getX(0), event.getY(0));
                        mLastPoint2.set(event.getX(1), event.getY(1));
                        mCanRotate = true;
                        mLastVector.set(event.getX(1) - event.getX(0),
                                event.getY(1) - event.getY(0));
                    } else if (event.getPointerCount() == 1) {
                        mCanDrag = true;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mMode == DRAW) {
                    float dx = Math.abs(x - mX);
                    float dy = Math.abs(y - mY);
                    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                        mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                        mX = x;
                        mY = y;
                    }
                    Log.d(TAG, "MOVE");
                    mCanvas.drawPath(mPath, mPaint);
                }
                if (mMode == SCALE) {
                    if (mCanDrag) translate(midPoint);
                    if (mCanScale) scale(event);
                    if (mCanRotate) rotate(event);
                    // 判断图片是否发生了变换
                    if (!getImageMatrix().equals(mMatrix)) isTransforming = true;
                    if (mCanDrag || mCanScale || mCanRotate) applyMatrix();
                }
                if (mMode == CROP) {
                    onActionMove(event.getX(), event.getY());
                    //父组件不拦截事件
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mMode == DRAW) {
                    mPath.lineTo(mX, mY);
                    mCanvas.drawPath(mPath, mPaint);
                    mLastDrawPath = new DrawPath(mPath, mPaint.getColor(), mPaint.getStrokeWidth());
                    savePath.add(mLastDrawPath);
                    mPath = null;
                }
            case MotionEvent.ACTION_CANCEL:
                if (mMode == SCALE) {
                    // 检测是否需要回弹
                    if (mOpenRotateRevert || mOpenScaleRevert || mOpenTranslateRevert) {
                        mMatrix.getValues(mFromMatrixValue);/*设置矩阵动画初始值*/

                        if (mOpenRotateRevert) checkRotation();
                        if (mOpenScaleRevert) checkScale();
                        if (mOpenTranslateRevert) checkBorder();
                        mMatrix.getValues(mToMatrixValue);/*设置矩阵动画结束值*/
                        if (mOpenAnimator) {
                            // 启动回弹动画
                            mRevertAnimator.setMatrixValue(mFromMatrixValue, mToMatrixValue);
                            mRevertAnimator.cancel();
                            mRevertAnimator.start();
                        } else {
                            applyMatrix();
                        }
                    }
                }
                if (mMode == CROP) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                    onActionUp();
                    return true;
                }
            case MotionEvent.ACTION_POINTER_UP:
                if (mMode == SCALE) {
                    mCanScale = false;
                    mCanDrag = false;
                    mCanRotate = false;
                }
                break;
        }
        super.onTouchEvent(event);
        invalidate();
        return true;
    }

    /**
     * 处理手指按下事件
     *
     * @param x 手指按下时水平方向的坐标
     * @param y 手指按下时竖直方向的坐标
     */
    private void onActionDown(float x, float y) {

        //获取边框的上下左右四个坐标点的坐标
        final float left = Edge.LEFT.getCoordinate();
        final float top = Edge.TOP.getCoordinate();
        final float right = Edge.RIGHT.getCoordinate();
        final float bottom = Edge.BOTTOM.getCoordinate();

        //获取手指所在位置位于图二种的A，B，C，D位置种哪一种
        mPressedCropWindowEdgeSelector = CatchEdgeUtils.getPressedHandle(x, y, left, top, right, bottom, mScaleRadius);

        if (mPressedCropWindowEdgeSelector != null) {
            //计算手指按下的位置与裁剪框的偏移量
            CatchEdgeUtils.getOffset(mPressedCropWindowEdgeSelector, x, y, left, top, right, bottom, mTouchOffset);
            invalidate();
        }
    }

    private void onActionUp() {
        if (mPressedCropWindowEdgeSelector != null) {
            mPressedCropWindowEdgeSelector = null;
            invalidate();
        }
    }

    private void onActionMove(float x, float y) {

        if (mPressedCropWindowEdgeSelector == null) {
            return;
        }
        x += mTouchOffset.x;
        y += mTouchOffset.y;

        mPressedCropWindowEdgeSelector.updateCropWindow(x, y, mBitmapRect);
        invalidate();
    }

    private void rotate(MotionEvent event) {
        // 计算当前两指触点所表示的向量
        mCurrentVector.set(event.getX(1) - event.getX(0),
                event.getY(1) - event.getY(0));
        // 获取旋转角度
        float degree = getRotateDegree(mLastVector, mCurrentVector);
        mMatrix.postRotate(degree, mImageRect.centerX(), mImageRect.centerY());
        mLastVector.set(mCurrentVector);
    }

    /**
     * 使用Math#atan2(double y, double x)方法求上次触摸事件两指所示向量与x轴的夹角，
     * 再求出本次触摸事件两指所示向量与x轴夹角，最后求出两角之差即为图片需要转过的角度
     *
     * @param lastVector    上次触摸事件两指间连线所表示的向量
     * @param currentVector 本次触摸事件两指间连线所表示的向量
     * @return 两向量夹角，单位“度”，顺时针旋转时为正数，逆时针旋转时返回负数
     */
    private float getRotateDegree(PointF lastVector, PointF currentVector) {
        //上次触摸事件向量与x轴夹角
        double lastRad = Math.atan2(lastVector.y, lastVector.x);
        //当前触摸事件向量与x轴夹角
        double currentRad = Math.atan2(currentVector.y, currentVector.x);
        // 两向量与x轴夹角之差即为需要旋转的角度
        double rad = currentRad - lastRad;
        //“弧度”转“度”
        return (float) Math.toDegrees(rad);
    }

    protected void translate(PointF midPoint) {
        float dx = midPoint.x - mLastMidPoint.x;
        float dy = midPoint.y - mLastMidPoint.y;
        mMatrix.postTranslate(dx, dy);
        mLastMidPoint.set(midPoint);
    }

    /**
     * 计算所有触点的中点
     *
     * @param event 当前触摸事件
     * @return 本次触摸事件所有触点的中点
     */
    private PointF getMidPointOfFinger(MotionEvent event) {
        // 初始化mCurrentMidPoint
        mCurrentMidPoint.set(0f, 0f);
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            mCurrentMidPoint.x += event.getX(i);
            mCurrentMidPoint.y += event.getY(i);
        }
        mCurrentMidPoint.x /= pointerCount;
        mCurrentMidPoint.y /= pointerCount;
        return mCurrentMidPoint;
    }

    private static final int SCALE_BY_IMAGE_CENTER = 0; // 以图片中心为缩放中心
    private static final int SCALE_BY_FINGER_MID_POINT = 1; // 以所有手指的中点为缩放中心
    private int mScaleBy = SCALE_BY_IMAGE_CENTER;
    private PointF scaleCenter = new PointF();

    /**
     * 获取图片的缩放中心，该属性可在外部设置，或通过xml文件设置
     * 默认中心点为图片中心
     *
     * @return 图片的缩放中心点
     */
    private PointF getScaleCenter() {
        // 使用全局变量避免频繁创建变量
        switch (mScaleBy) {
            case SCALE_BY_IMAGE_CENTER:
                scaleCenter.set(mImageRect.centerX(), mImageRect.centerY());
                break;
            case SCALE_BY_FINGER_MID_POINT:
                scaleCenter.set(mLastMidPoint.x, mLastMidPoint.y);
                break;
        }
        return scaleCenter;
    }

    private void scale(MotionEvent event) {
        PointF scaleCenter = getScaleCenter();

        // 初始化当前两指触点
        mCurrentPoint1.set(event.getX(0), event.getY(0));
        mCurrentPoint2.set(event.getX(1), event.getY(1));
        // 计算缩放比例
        float scaleFactor = distance(mCurrentPoint1, mCurrentPoint2)
                / distance(mLastPoint1, mLastPoint2);

        // 更新当前图片的缩放比例
        mScaleFactor *= scaleFactor;

        mMatrix.postScale(scaleFactor, scaleFactor,
                scaleCenter.x, scaleCenter.y);
        mLastPoint1.set(mCurrentPoint1);
        mLastPoint2.set(mCurrentPoint2);
    }

    /**
     * 获取两点间距离
     */
    private float distance(PointF point1, PointF point2) {
        float dx = point2.x - point1.x;
        float dy = point2.y - point1.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 根据当前图片旋转的角度，判断是否回弹
     */
    private void checkRotation() {
        float currentDegree = getCurrentRotateDegree();
        float degree = currentDegree;
        // 根据当前图片旋转的角度值所在区间，判断要转到几度
        degree = Math.abs(degree);
        if (degree > 45 && degree <= 135) {
            degree = 90;
        } else if (degree > 135 && degree <= 225) {
            degree = 180;
        } else if (degree > 225 && degree <= 315) {
            degree = 270;
        } else {
            degree = 0;
        }
        // 判断顺时针还是逆时针旋转
        degree = currentDegree < 0 ? -degree : degree;
        mMatrix.postRotate(degree - currentDegree, mImageRect.centerX(), mImageRect.centerY());
    }

    private float[] xAxis = new float[]{1f, 0f}; // 表示与x轴同方向的向量

    /**
     * 获取当前图片旋转角度
     *
     * @return 图片当前的旋转角度
     */
    private float getCurrentRotateDegree() {
        // 每次重置初始向量的值为与x轴同向
        xAxis[0] = 1f;
        xAxis[1] = 0f;
        // 初始向量通过矩阵变换后的向量
        mMatrix.mapVectors(xAxis);
        // 变换后向量与x轴夹角
        double rad = Math.atan2(xAxis[1], xAxis[0]);
        return (float) Math.toDegrees(rad);
    }

    /**
     * 检查图片缩放比例是否超过设置的大小
     */
    private void checkScale() {
        PointF scaleCenter = getScaleCenter();

        float scaleFactor = 1.0f;

        // 获取图片当前是水平还是垂直
        int imgOrientation = imgOrientation();
        // 超过设置的上限或下限则回弹到设置的限制值
        // 除以当前图片缩放比例mScaleFactor，postScale()方法执行后的图片的缩放比例即为被除数大小
        if (imgOrientation == HORIZONTAL
                && mScaleFactor < mHorizontalMinScaleFactor) {
            scaleFactor = mHorizontalMinScaleFactor / mScaleFactor;
        } else if (imgOrientation == VERTICAL
                && mScaleFactor < mVerticalMinScaleFactor) {
            scaleFactor = mVerticalMinScaleFactor / mScaleFactor;
        } else if (mScaleFactor > mMaxScaleFactor) {
            scaleFactor = mMaxScaleFactor / mScaleFactor;
        }
        mMatrix.postScale(scaleFactor, scaleFactor, scaleCenter.x, scaleCenter.y);
        mScaleFactor *= scaleFactor;
    }

    private static final int HORIZONTAL = 0;
    private static final int VERTICAL = 1;

    /**
     * 判断图片当前是水平还是垂直
     *
     * @return 水平则返回 {@code HORIZONTAL}，垂直则返回 {@code VERTICAL}
     */
    private int imgOrientation() {
        float degree = Math.abs(getCurrentRotateDegree());
        int orientation = HORIZONTAL;
        if (degree > 45f && degree <= 135f) {
            orientation = VERTICAL;
        }
        return orientation;
    }

    /**
     * 将图片移回控件中心
     */
    private void checkBorder() {
        // 由于旋转回弹与缩放回弹会影响图片所在位置，所以此处需要更新ImageRect的值
        refreshImageRect();
        // 默认不移动
        float dx = 0f;
        float dy = 0f;

        // mImageRect中的坐标值为相对View的值
        // 图片宽大于控件时图片与控件之间不能有白边
        if (mImageRect.width() > getWidth()) {
            if (mImageRect.left > 0) {/*判断图片左边界与控件之间是否有空隙*/
                dx = -mImageRect.left;
            } else if (mImageRect.right < getWidth()) {/*判断图片右边界与控件之间是否有空隙*/
                dx = getWidth() - mImageRect.right;
            }
        } else {/*宽小于控件则移动到中心*/
            dx = getWidth() / 2 - mImageRect.centerX();
        }

        // 图片高大于控件时图片与控件之间不能有白边
        if (mImageRect.height() > getHeight()) {
            if (mImageRect.top > 0) {/*判断图片上边界与控件之间是否有空隙*/
                dy = -mImageRect.top;
            } else if (mImageRect.bottom < getHeight()) {/*判断图片下边界与控件之间是否有空隙*/
                dy = getHeight() - mImageRect.bottom;
            }
        } else {/*高小于控件则移动到中心*/
            dy = getHeight() / 2 - mImageRect.centerY();
        }
        mMatrix.postTranslate(dx, dy);
    }

    /**
     * 更新图片所在区域，并将矩阵应用到图片
     */
    protected void applyMatrix() {
        refreshImageRect(); /*将矩阵映射到ImageRect*/
        setImageMatrix(mMatrix);
    }

    /**
     * 图片使用矩阵变换后，刷新图片所对应的mImageRect所指示的区域
     */
    private void refreshImageRect() {
        if (getDrawable() != null) {
            mImageRect.set(getDrawable().getBounds());
            mMatrix.mapRect(mImageRect, mImageRect);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mRevertAnimator.cancel();
    }

    //-----Aninmator-------------------

    /**
     * 图片回弹动画
     */
    private class MatrixRevertAnimator extends ValueAnimator
            implements ValueAnimator.AnimatorUpdateListener {

        private float[] mFromMatrixValue; // 动画初始时矩阵值
        private float[] mToMatrixValue; // 动画终结时矩阵值
        private float[] mInterpolateMatrixValue; // 动画执行过程中矩阵值

        MatrixRevertAnimator() {
            mInterpolateMatrixValue = new float[9];
            setFloatValues(0f, 1f);
            addUpdateListener(this);
        }

        void setMatrixValue(float[] fromMatrixValue, final float[] toMatrixValue) {
            mFromMatrixValue = fromMatrixValue;
            mToMatrixValue = toMatrixValue;

            addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMatrix.setValues(toMatrixValue);
                    applyMatrix();
                }
            });
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (mFromMatrixValue != null
                    && mToMatrixValue != null && mInterpolateMatrixValue != null) {
                // 根据动画当前进度设置矩阵的值
                for (int i = 0; i < 9; i++) {
                    float animatedValue = (float) animation.getAnimatedValue();
                    mInterpolateMatrixValue[i] = mFromMatrixValue[i]
                            + (mToMatrixValue[i] - mFromMatrixValue[i]) * animatedValue;
                }
                mMatrix.setValues(mInterpolateMatrixValue);
                applyMatrix();
            }
        }

    }

    /**
     * 载入bitmap
     *
     * @param bitmap 图像
     */
    public void loadImage(Bitmap bitmap) {
        Log.d(TAG, "loadImage: ");
        mOriginBitmap = bitmap;
        mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        mCanvas = new Canvas(mBitmap);
        measure(mCanvas.getWidth(), mCanvas.getHeight());
        setImageBitmap(mBitmap);
        invalidate();
    }

    @Override
    public void setBackgroundColor(int color) {
        mCanvas.drawColor(color);
        super.setBackgroundColor(color);
    }

    /**
     * 撤销上一步
     */
    public void undo() {
        Log.d(TAG, "undo: recall last path");
        if (savePath != null && savePath.size() > 0) {
            // 清空画布
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            loadImage(mOriginBitmap);

            savePath.removeLast();

            // 将路径保存列表中的路径重绘在画布上 遍历绘制
            for (DrawPath dp : savePath) {
                mPaint.setColor(dp.getPaintColor());
                mPaint.setStrokeWidth(dp.getPaintWidth());
                mCanvas.drawPath(dp.path, mPaint);
            }
            invalidate();
        }
    }

    /**
     * 清空画布
     */
    public void clear() {
        Log.d(TAG, "clear the path");
        if (savePath != null && savePath.size() > 0) {
            // 清空画布
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            loadImage(mOriginBitmap);
            invalidate();
        }
    }

    /**
     * @param filePath 路径名
     * @param filename 文件名
     * @param format   存储格式
     * @param quality  质量
     * @return 是否保存成功
     */
    public boolean saveImage(String filePath, String filename, Bitmap.CompressFormat format,
                             int quality) {
        if (quality > 100) {
            Log.d("saveImage", "quality cannot be greater that 100");
            return false;
        }
        File file;
        FileOutputStream out = null;
        try {
            switch (format) {
                case PNG:
                    file = new File(filePath, filename + ".png");
                    out = new FileOutputStream(file);
                    return mBitmap.compress(Bitmap.CompressFormat.PNG, quality, out);
                case JPEG:
                    file = new File(filePath, filename + ".jpg");
                    out = new FileOutputStream(file);
                    return mBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                default:
                    file = new File(filePath, filename + ".png");
                    out = new FileOutputStream(file);
                    return mBitmap.compress(Bitmap.CompressFormat.PNG, quality, out);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 获取裁剪好的BitMap
     */
    public Bitmap getCroppedImage() {

        final Drawable drawable = getDrawable();
        if (drawable == null || !(drawable instanceof BitmapDrawable)) {
            return null;
        }

        final float[] matrixValues = new float[9];
        getImageMatrix().getValues(matrixValues);

        final float scaleX = matrixValues[Matrix.MSCALE_X];
        final float scaleY = matrixValues[Matrix.MSCALE_Y];
        final float transX = matrixValues[Matrix.MTRANS_X];
        final float transY = matrixValues[Matrix.MTRANS_Y];

        float bitmapLeft = (transX < 0) ? Math.abs(transX) : 0;
        float bitmapTop = (transY < 0) ? Math.abs(transY) : 0;

        final Bitmap originalBitmap = ((BitmapDrawable) drawable).getBitmap();

        final float cropX = (bitmapLeft + Edge.LEFT.getCoordinate()) / scaleX;
        final float cropY = (bitmapTop + Edge.TOP.getCoordinate()) / scaleY;

        final float cropWidth = Math.min(Edge.getWidth() / scaleX, originalBitmap.getWidth() - cropX);
        final float cropHeight = Math.min(Edge.getHeight() / scaleY, originalBitmap.getHeight() - cropY);

        return Bitmap.createBitmap(originalBitmap,
                (int) cropX,
                (int) cropY,
                (int) cropWidth,
                (int) cropHeight);

    }


    /**
     * 获取图片ImageView周围的边界组成的RectF对象
     */
    private RectF getBitmapRect() {

        final Drawable drawable = getDrawable();
        if (drawable == null) {
            return new RectF();
        }

        final float[] matrixValues = new float[9];
        getImageMatrix().getValues(matrixValues);

        final float scaleX = matrixValues[Matrix.MSCALE_X];
        final float scaleY = matrixValues[Matrix.MSCALE_Y];
        final float transX = matrixValues[Matrix.MTRANS_X];
        final float transY = matrixValues[Matrix.MTRANS_Y];

        final int drawableIntrinsicWidth = drawable.getIntrinsicWidth();
        final int drawableIntrinsicHeight = drawable.getIntrinsicHeight();

        final int drawableDisplayWidth = Math.round(drawableIntrinsicWidth * scaleX);
        final int drawableDisplayHeight = Math.round(drawableIntrinsicHeight * scaleY);

        final float left = Math.max(transX, 0);
        final float top = Math.max(transY, 0);
        final float right = Math.min(left + drawableDisplayWidth, getWidth());
        final float bottom = Math.min(top + drawableDisplayHeight, getHeight());

        return new RectF(left, top, right, bottom);
    }

    /**
     * 初始化裁剪框
     */
    private void initCropWindow(@NonNull RectF bitmapRect) {

        //裁剪框距离图片左右的padding值
        final float horizontalPadding = 0.01f * bitmapRect.width();
        final float verticalPadding = 0.01f * bitmapRect.height();

        //初始化裁剪框上下左右四条边
        Edge.LEFT.initCoordinate(bitmapRect.left + horizontalPadding);
        Edge.TOP.initCoordinate(bitmapRect.top + verticalPadding);
        Edge.RIGHT.initCoordinate(bitmapRect.right - horizontalPadding);
        Edge.BOTTOM.initCoordinate(bitmapRect.bottom - verticalPadding);
    }

    //-------getter and setter---------

    /**
     * 设置画笔大小
     */
    public void setPenSize(float size) {
        mPaintBarPenSize = size;
        mPaint.setStrokeWidth(size);
    }

    /**
     * 获取画笔大小
     *
     * @return 画笔大小
     */
    public float getPenSize() {
        return mPaint.getStrokeWidth();
    }

    /**
     * 设置画笔颜色
     */
    public void setPenColor(@ColorInt int color) {
        mPaintBarPenColor = color;
        mPaint.setColor(color);
    }

    /**
     * 获取画笔颜色
     *
     * @return 画笔颜色
     */
    public
    @ColorInt
    int getPenColor() {
        return mPaint.getColor();
    }

    /**
     * @return 当前画布上的内容
     */
    public Bitmap getImageBitmap() {
        return mBitmap;
    }

}
