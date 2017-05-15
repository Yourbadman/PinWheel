package com.rocf.pinwheel;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;


/**
 * <p>
 * This custom view PinWheel include two base widget : the simplification CircleProgressBar and ParticleSystem.
 * </p>
 * Use:
 * Before use it ,must call {@link #init()}  to beginning.
 * call {@link #setMax(float)} to set total degree,{@link #setProgress(float)} to set current degree,
 * {@link #start()} to start animator ,it will be continue util call {@link #stop()}
 * in the end of use it ,call {@link #free()}
 * <p/>
 * That's all ,enjoy it.
 *
 * @author rocf.wong@gmail.com create by 2016/08/10
 */
public class PinWheelWidget extends CircleProgressBar implements Handler.Callback {
    private static final String TAG = "PinWheelWidget";

    //Handle message
    private static final int UNIFORM = 1; //to keep the speed of pinwheel
    private static final int STOP_EMIT = 2;//stop toast particle
    private static final int CANCEL_EMIT = 3;//cancel particle
    private static final int DOWN_PINWHEEL = 4;//make pinwheel's speed slow
    private static final int CANCLE_ALL_MESSAGE = 5;


    //PinWheel
    private Bitmap pinWheelBmp = null;
    private volatile float rotationDegree = 0;
    public static final float UNIFORM_DEGREE = 16;
    private float xScaleFactor = 0;
    private float yScaleFactor = 0;
    private float pXyx = 0;
    private float pXyy = 0;
//    private int uniformRat = 8;


    //Progressbar
    private static final int mProgressbarDuration = 495;
    private volatile boolean isRunning = false;


    //use for interpolator new PathInterpolator(path));
    Interpolator interpolator;

    //Particle
    private ParticleSystem particleSystem = null;
    private Handler handler = null;
    private ValueAnimator progressbarStartAnimator = null;
    private ValueAnimator pinWheelUpSpeedAnimator = null;

    private ValueAnimator pinWheelDownAnimator = null;
    private ValueAnimator progressbarStopAnimator = null;


    private DisplayMetrics displayMetrics;
    private float mDpToPxScale;
    private float mDensity;
    private volatile boolean UNIFORM_STATE;
    private volatile boolean ROTATION_STATE;


    public PinWheelWidget(Context context) {
        super(context);
    }

    public PinWheelWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWidget();
    }

    private void initWidget() {
        //this can be calculate
        setStartDegree(-222);
        setTotalDegree(264);
        setMax(100.0f);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDensity = (int) getResources().getDisplayMetrics().density;
        pinWheelBmp = BitmapFactory.decodeResource(getResources(), R.drawable.le_control_center_pinwheel);
    }


    /**
     * <p>
     * Start animator include particle,progressbar,pinwheel
     * <p/>
     * </p>
     * <p/>
     * About Animator:
     * <p>
     * Progressbar's animator includes Interpolator file{@link @com.android.internal.R.interpolator.le_c_interpolator}
     * it will be continue 495ms.
     * Particle's will be emit by {@link ParticleSystem} when animator start,until call {@link #stop()}
     * Pinwheel will be start when progressbar begin,util call {@link #stop()}
     * </p>
     *
     * @return success return
     */
    public boolean start() {

        if (this.isRunning)
            return false;
        this.isRunning = true;
        PinWheelWidget.this.rotationDegree = 0;

        if (this.handler == null) {

            throw new IllegalArgumentException("PinWheel#handler is null,you must call PinWheel#init()");
        }
        particleSystem = new ParticleSystem(PinWheelWidget.this, getResources().getDrawable(R.drawable.le_control_center_point), 30, 6000, mDpToPxScale);

        particleSystem
                .setSpeedByComponentsRange(-0.08f, 0.08f, -0.08f, 0.08f)
                .addModifier(new ParticleSystem.AlphaModifier(-250, 250, 0, 2000))
                .addModifier(new ParticleSystem.ScaleModifier(0.3f, 0.5f, 0, 400));
        particleSystem.emit();

        progressbarStartAnimator = ValueAnimator.ofFloat(getProgress(), 0.0f);

        progressbarStartAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                PinWheelWidget.this.setProgress((Float) animation.getAnimatedValue());

            }
        });

        progressbarStartAnimator.addListener(new PinWheelAnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                ROTATION_STATE = true;
                pinWheelUpSpeedAnimator.start();
            }

        });

        progressbarStartAnimator.setDuration(mProgressbarDuration);

        pinWheelUpSpeedAnimator = ValueAnimator.ofFloat(0.0f, UNIFORM_DEGREE);
        pinWheelUpSpeedAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                PinWheelWidget.this.setRotationDegree((Float) animation.getAnimatedValue());

            }
        });

        pinWheelUpSpeedAnimator.addListener(new PinWheelAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {

                UNIFORM_STATE = true;
                handler.sendEmptyMessageDelayed(PinWheelWidget.UNIFORM, 0);
                PinWheelWidget.this.rotationDegree = 0;

            }
        });

        progressbarStartAnimator.start();

        return true;
    }


    /**
     * stop pinwheel's animator
     * <p/>
     * include particle,progress,pinwheel animator.
     * <p/>
     * tips:
     * before call this you must call {@link #setProgress(float)}
     *
     * @return success return
     */

    public boolean stop() {

        if (!this.isRunning)
            return false;
        PinWheelWidget.this.rotationDegree = 0;

        pinWheelDownAnimator = ValueAnimator.ofFloat(UNIFORM_DEGREE, 0);
        pinWheelDownAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                if (value > 1f)
                    PinWheelWidget.this.setRotationDegree(value);
            }
        });
        pinWheelDownAnimator.addListener(new PinWheelAnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                UNIFORM_STATE = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                ROTATION_STATE = true;
                handler.sendEmptyMessageDelayed(PinWheelWidget.CANCEL_EMIT, 5);
                //                PinWheelWidget.this.rotationDegree = 0;
//                handler.sendEmptyMessageDelayed(PinWheelWidget.STOP_EMIT, 5);
//                handler.sendEmptyMessageDelayed(PinWheelWidget.CANCEL_EMIT, 10);
//                handler.sendEmptyMessageDelayed(PinWheelWidget.CANCLE_ALL_MESSAGE, 50);
            }
        });

        pinWheelDownAnimator.setDuration(mProgressbarDuration * 2);
        pinWheelDownAnimator.setInterpolator(new LinearInterpolator());


        progressbarStopAnimator = ValueAnimator.ofFloat(0.0f, getProgress());
        progressbarStopAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                PinWheelWidget.this.setProgress((Float) animation.getAnimatedValue());
            }
        });
        progressbarStopAnimator.addListener(new PinWheelAnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                pinWheelDownAnimator.start();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                handler.sendEmptyMessage(PinWheelWidget.STOP_EMIT);


            }
        });

        progressbarStopAnimator.setDuration(mProgressbarDuration);
        progressbarStopAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        progressbarStopAnimator.start();


        this.isRunning = false;
        return true;
    }

    /**
     * @param progress
     * @return
     */
    public boolean stop(float progress) {
        setProgress(progress);
        return stop();
    }


    public void init() {

        this.rotationDegree = 0;
        handler = new Handler(this);
        displayMetrics = getResources().getDisplayMetrics();
        mDpToPxScale = (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT);
        mDensity = getResources().getDisplayMetrics().density;


        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.cubicTo(1 / 3f, 0, 0, 1, 1, 1);
        interpolator = new PathInterpolator(path);

    }


    /**
     * free current
     */
    public void free() {
        if (this.handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (particleSystem != null) {
            particleSystem.cancel();
        }
        if (pinWheelUpSpeedAnimator != null) {
            pinWheelUpSpeedAnimator.cancel();
        }
        if (progressbarStartAnimator != null) {
            progressbarStartAnimator.cancel();
        }
        if (pinWheelDownAnimator != null) {
            pinWheelDownAnimator.cancel();
        }
        if (progressbarStopAnimator != null) {
            progressbarStopAnimator.cancel();
        }
        if (pinWheelBmp != null && pinWheelBmp.isRecycled()) {
            pinWheelBmp.recycle();
        }

    }


    /**
     * set rotation angle of pinwheel
     *
     * @param rad angle
     * @return this
     */
    private PinWheelWidget setRotationDegree(float rad) {
        this.rotationDegree -= rad;
        postInvalidate();
        return this;
    }


    //draw the center of circle view,means pinwheel
    @Override
    public boolean drawCustomView(Canvas canvas, float mCenterX, float mCenterY, float mTotalDegree) {

        Matrix matrix = new Matrix();
        float halfBmpWidth = pinWheelBmp.getWidth() / 2;
        float halfBmpHeight = pinWheelBmp.getHeight() / 2;
        if (xScaleFactor == 0 || yScaleFactor == 0) {
            xScaleFactor = (float) (0.67 * mCenterX / halfBmpWidth);//Math.cos(48) = -0.67 Math.sin(42) = -0.91
            yScaleFactor = (float) (0.67 * mCenterY / halfBmpWidth);
            pXyx = ((1 + xScaleFactor) * mCenterX) / mDensity;
            pXyy = ((1 + yScaleFactor) * mCenterY) / mDensity;
        }
        matrix.setScale(xScaleFactor, yScaleFactor, (int) (pXyx * 1.5 * mDensity / 4), (int) (pXyx * 1.5 * mDensity / 4));
        if (ROTATION_STATE)
            matrix.preRotate(rotationDegree, halfBmpWidth, halfBmpHeight);
        canvas.drawBitmap(pinWheelBmp, matrix, null);
        return true;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case UNIFORM: {

                PinWheelWidget.this.setRotationDegree(UNIFORM_DEGREE);
                if (UNIFORM_STATE)
                    handler.sendEmptyMessageDelayed(UNIFORM, 0);
                break;
            }
            case DOWN_PINWHEEL: {
//                uniformRat -= 3;
//                PinWheelWidget.this.setRotationDegree(uniformRat);
//                if (uniformRat < 5)
//                    handler.sendEmptyMessageDelayed(DOWN_PINWHEEL, 0);
            }
            case STOP_EMIT: {
                particleSystem.stopEmitting();
                break;
            }
            case CANCEL_EMIT: {
                particleSystem.cancel();
                break;
            }
            case CANCLE_ALL_MESSAGE: {

                handler.removeCallbacksAndMessages(null);
                break;
            }

        }
        return false;
    }

    private static class PinWheelAnimatorListener implements Animator.AnimatorListener {


        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {

        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }


}
