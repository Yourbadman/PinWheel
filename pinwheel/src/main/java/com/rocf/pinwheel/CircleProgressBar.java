package com.rocf.pinwheel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.LinearLayout;


/**
 * # It supports ProgerssBar's function with the shape of circle.<br/>
 * <p/>
 * it's also supports a arc on display,{@link #setStartDegree(int)} to start scale ,{@link #setTotalDegree(int)}
 * to calculate the end of arc.
 * <p/>
 * #The Center of Circle:
 * The center of circle area can be use to draw custom view via extends it
 * and implements it's Method{@link #drawCustomView(Canvas, float, float, float)}<br/>
 *
 * @author rocf.wong@gmail.com create by 2016/08/10
 */
public class CircleProgressBar extends LinearLayout {

    private static final float DEFAULT_PROGRESS_STROKE_WIDTH = 1.0f;

    private final RectF mProgressRectF = new RectF();
    private final Paint mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mProgressBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint.Cap mCap;

    private float mRadius;
    private float mCenterX;
    private float mCenterY;

    private int mProgressColor;
    private int mBackgroundColor;
    private float mProgressStrokeWidth;
    private int mProgressBackgroundColor;
    private float mStartDegree;
    private float mTotalDegree;

    private float mMax = 100;
    private float mCurrentProgress;
    private boolean mIndeterminate;
    public Context context = null;

    public CircleProgressBar(Context context) {
        this(context, null);
    }

    public CircleProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        initFromAttributes(context, attrs);
        initPaint();
    }

    /**
     * Basic data initialization
     */
    private void initFromAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircleProgressBar);
        mBackgroundColor = a.getColor(R.styleable.CircleProgressBar_background_color, Color.TRANSPARENT);
        mCap = a.hasValue(R.styleable.CircleProgressBar_progress_stroke_cap) ?
                Paint.Cap.values()[a.getInt(R.styleable.CircleProgressBar_progress_stroke_cap, 0)] : Paint.Cap.ROUND;
        mProgressStrokeWidth = a.getDimensionPixelSize(R.styleable.CircleProgressBar_progress_stroke_width,
                (int) (context.getResources().getDisplayMetrics().density * DEFAULT_PROGRESS_STROKE_WIDTH + 0.5f));
        mProgressBackgroundColor = a.getColor(R.styleable.CircleProgressBar_progress_background_color, Color.GRAY);
        mProgressColor = a.getColor(R.styleable.CircleProgressBar_progress_color, Color.WHITE);
        mStartDegree = a.getFloat(R.styleable.CircleProgressBar_progress_start_degree, -90.0f);
        mTotalDegree = a.getFloat(R.styleable.CircleProgressBar_progress_total_degree, 360.0f);
        a.recycle();
    }

    /**
     * Paint initialization
     */
    private void initPaint() {
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setStrokeWidth(mProgressStrokeWidth);
        mProgressPaint.setStrokeCap(mCap);
        mProgressPaint.setShader(null);
        mProgressPaint.setColor(mProgressColor);

        mProgressBackgroundPaint.setStyle(Paint.Style.STROKE);
        mProgressBackgroundPaint.setStrokeWidth(mProgressStrokeWidth);
        mProgressBackgroundPaint.setColor(mProgressBackgroundColor);
        mProgressBackgroundPaint.setStrokeCap(mCap);

        mBackgroundPaint.setStyle(Paint.Style.STROKE);
        mBackgroundPaint.setColor(mBackgroundColor);

    }

    public synchronized float getProgress() {
        return mIndeterminate ? 0 : mCurrentProgress;
    }

    public synchronized float getMax() {
        return mMax;
    }

    public void setMax(float mMax) {
        this.mMax = mMax;
    }


    public synchronized void setProgress(float progress) {

        if (mIndeterminate) {
            return;
        }

        if (progress < 0) {
            progress = 0;
        }
        if (progress > mMax) {
            progress = mMax;
        }

        if (progress != mCurrentProgress) {
            mCurrentProgress = progress;
            invalidate();
        }
    }


    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        drawProgress(canvas);
        drawCustomView(canvas, mCenterX, mCenterY, mTotalDegree);
    }

    private void drawBackground(Canvas canvas) {

        if (mBackgroundColor != Color.TRANSPARENT) {
            canvas.drawCircle(mCenterX, mCenterX, mRadius, mBackgroundPaint);
        }

    }

    private void drawProgress(Canvas canvas) {

        canvas.drawArc(mProgressRectF, mStartDegree, mTotalDegree, false, mProgressBackgroundPaint);
        if (getProgress() != 0)
            canvas.drawArc(mProgressRectF, mStartDegree, mTotalDegree * getProgress() / getMax(), false, mProgressPaint);

    }

    /**
     * When the size of CircleProgressBar changed, need to re-adjust the drawing area
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = w / 2;
        mCenterY = h / 2;

        mRadius = Math.min(mCenterX, mCenterY);
        mProgressRectF.top = mCenterY - mRadius;
        mProgressRectF.bottom = mCenterY + mRadius;
        mProgressRectF.left = mCenterX - mRadius;
        mProgressRectF.right = mCenterX + mRadius;
        //Prevent the progress from clipping
        mProgressRectF.inset(mProgressStrokeWidth / 2, mProgressStrokeWidth / 2);
    }

    public int getBackgroundColor() {
        return mBackgroundColor;
    }


    public void setBackgroundColor(int backgroundColor) {
        this.mBackgroundColor = backgroundColor;
        mBackgroundPaint.setColor(backgroundColor);
        invalidate();
    }

    public void setProgressStrokeWidth(float progressStrokeWidth) {
        this.mProgressStrokeWidth = progressStrokeWidth;
        mProgressRectF.inset(mProgressStrokeWidth / 2, mProgressStrokeWidth / 2);
        invalidate();
    }

    public float getProgressStrokeWidth() {
        return mProgressStrokeWidth;
    }


    public void setProgressColor(int progressColor) {
        this.mProgressColor = progressColor;
        invalidate();
    }

    public void setProgressBackgroundColor(int progressBackgroundColor) {
        this.mProgressBackgroundColor = progressBackgroundColor;
        mProgressBackgroundPaint.setColor(mProgressBackgroundColor);
        invalidate();
    }

    public int getProgressBackgroundColor() {
        return mProgressBackgroundColor;
    }


    public CircleProgressBar setStartDegree(int startDegree) {
        this.mStartDegree = startDegree;
        return this;
    }

    public CircleProgressBar setTotalDegree(int totalDegree) {
        this.mTotalDegree = totalDegree;
        return this;
    }


    public Paint.Cap getCap() {
        return mCap;
    }

    public void setCap(Paint.Cap cap) {
        mCap = cap;
        mProgressPaint.setStrokeCap(cap);
        mProgressBackgroundPaint.setStrokeCap(cap);
        invalidate();
    }


    /**
     * draw custom view ,is call be CircleProgressBar{@link #onDraw(Canvas)}
     *
     * @param canvas
     * @param centerX     the center of X
     * @param centerY     the center of Y
     * @param totalDegree the arc of degree on the display
     * @return if has custom view must return true;
     */
    public boolean drawCustomView(Canvas canvas, float centerX, float centerY, float totalDegree) {

        return false;
    }
}


