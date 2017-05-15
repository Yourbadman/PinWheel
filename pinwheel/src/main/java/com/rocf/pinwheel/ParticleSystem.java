package com.rocf.pinwheel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Particle Emit System.
 * <p/>
 * <pre>
 *
 * #Example:
 *
 * particleSystem
 * .setSpeedByComponentsRange(-0.08f, 0.08f, -0.08f, 0.08f)
 * .addModifier(new ParticleSystem.AlphaModifier(-250, 250, 0, 2000))
 * .addModifier(new ParticleSystem.ScaleModifier(0.3f, 0.5f, 0, 400));
 * particleSystem.emit();
 * </pre>
 *
 * @author rocf.wong@gmail.com create by 2016/08/10
 */
public class ParticleSystem implements Handler.Callback {
    private static final String TAG = "ParticleSystem";
    private static final int MESSAGE_LOOP_EMIT = 0;
    private static final int MESSAGE_CANCLE = 1;
    private static final int MESSAGE_ONE_SHOT = 2;
    private static final long TIMMERTASK_INTERVAL = 10;
    private ViewGroup mParentView;
    private int mMaxParticles;
    protected static Random mRandom;

    private ParticleField mDrawingView;

    private ArrayList<Particle> mParticles;
    private final ArrayList<Particle> mActiveParticles = new ArrayList<Particle>();
    protected static int mTimeToLive;
    private long mCurrentTime = 0;

    private float mParticlesPerMilisecond;
    private int mActivatedParticles;
    private long mEmitingTime;

    private List<ParticleModifier> mModifiers;
    private List<ParticleInitializer> mInitializers;
    private float mDpToPxScale;
    private int[] mParentLocation;

    private int mEmiterXMin;
    private int mEmiterXMax;
    private int mEmiterYMin;
    private int mEmiterYMax;

    private Handler handler;


    public ParticleSystem(ViewGroup mParentView, Drawable drawable, int maxParticles, int timeToLive, float displayScale) {
        mRandom = new Random();
        mParentLocation = new int[2];
        this.mParentView = mParentView;
        setParentViewGroup(mParentView);
        mModifiers = new ArrayList<ParticleModifier>();
        mInitializers = new ArrayList<ParticleInitializer>();
        mMaxParticles = maxParticles;
        // Create the particles
        mParticles = new ArrayList<Particle>();
        mTimeToLive = timeToLive;
        mDpToPxScale = displayScale;
        handler = new Handler(this);
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            for (int i = 0; i < mMaxParticles; i++) {
                mParticles.add(new Particle(bitmap));
            }
        }

    }

    public float dpToPx(float dp) {
        return dp * mDpToPxScale;
    }

    public ParticleSystem addModifier(ParticleModifier modifier) {
        mModifiers.add(modifier);
        return this;
    }

    public ParticleSystem setSpeedByComponentsRange(float speedMinX, float speedMaxX, float speedMinY, float speedMaxY) {
        mInitializers.add(new SpeedByComponentsInitializer(dpToPx(speedMinX), dpToPx(speedMaxX),
                dpToPx(speedMinY), dpToPx(speedMaxY)));
        return this;
    }

    public ParticleSystem setParentViewGroup(ViewGroup viewGroup) {
        mParentView = viewGroup;
        if (mParentView != null) {
            mParentView.getLocationInWindow(mParentLocation);
        }
        return this;
    }


    public void emit() {
        // Setup emiter
        configureEmiter(mParentView);
        startEmiting(mMaxParticles, MESSAGE_LOOP_EMIT);
    }

    /**
     * Launches particles in one Shot
     */
    public void oneShot() {
        configureEmiter(mParentView);
        startEmiting(mMaxParticles, MESSAGE_ONE_SHOT);
    }


    private void startEmiting(int particlesPerSecond, int messageType) {
        mActivatedParticles = 0;
        mParticlesPerMilisecond = particlesPerSecond / 1000f;
        // Add a full size view to the parent view
        mDrawingView = new ParticleField(mParentView.getContext());
        mParentView.addView(mDrawingView);
        mEmitingTime = -1; // Meaning infinite
        mDrawingView.setParticles(mActiveParticles);
//        updateParticlesBeforeStartTime(particlesPerSecond);
        handler.sendEmptyMessageDelayed(messageType, TIMMERTASK_INTERVAL);
    }

    private void configureEmiter(View emiter) {
        // It works with an emision range
        int[] location = new int[2];
        emiter.getLocationInWindow(location);
        mEmiterXMin = location[0] + emiter.getWidth() / 2 - mParentLocation[0];
        mEmiterXMax = mEmiterXMin;
        mEmiterYMin = location[1] + emiter.getHeight() / 2 - mParentLocation[1];
        mEmiterYMax = mEmiterYMin;
    }

    private void activateParticle(long delay) {
        if (mParticles != null && mParticles.size() > 0) {
            Particle p = mParticles.remove(0);
            p.init();
            // Initialization goes before configuration, scale is required before can be configured properly
            for (int i = 0; i < mInitializers.size(); i++) {
                mInitializers.get(i).initParticle(p, mRandom);
            }
            int particleX = getFromRange(mEmiterXMin, mEmiterXMax);
            int particleY = getFromRange(mEmiterYMin, mEmiterYMax);
            p.configure(mTimeToLive, particleX, particleY);
            p.activate(delay, mModifiers);
            mActiveParticles.add(p);
            mActivatedParticles++;
        }
    }

    private int getFromRange(int minValue, int maxValue) {
        if (minValue == maxValue) {
            return minValue;
        }
        return mRandom.nextInt(maxValue - minValue) + minValue;
    }

    private void onUpdate(long miliseconds) {
        while (((mEmitingTime > 0 && miliseconds < mEmitingTime) || mEmitingTime == -1) && // This le_control_center_point should emit
                !mParticles.isEmpty() && // We have particles in the pool
                mActivatedParticles < mParticlesPerMilisecond * miliseconds) { // and we are under the number of particles that should be launched
            // Activate a new particle
            activateParticle(miliseconds);
        }
        synchronized (mActiveParticles) {
            for (int i = 0; i < mActiveParticles.size(); i++) {
                boolean active = mActiveParticles.get(i).update(miliseconds);
                if (!active) {
                    Particle p = mActiveParticles.remove(i);
                    i--; // Needed to keep the index at the right position
                    mParticles.add(p);
                }
            }
        }
        if (mDrawingView != null)
            mDrawingView.postInvalidate();
    }

    private void cleanupAnimation() {
        mParentView.removeView(mDrawingView);
        mDrawingView = null;
        mParentView.postInvalidate();
        mParticles.addAll(mActiveParticles);
    }

    /**
     * Stops emitting new particles, but will draw the existing ones until their timeToLive expire
     * For an cancellation and stop drawing of the particles, use cancel instead.
     */
    public void stopEmitting() {
        // The time to be emiting is the current time (as if it was a time-limited emiter
        mEmitingTime = mCurrentTime;
    }

    /**
     * Cancels the particle system and all the animations.
     * To stop emitting but animate until the end, use stopEmitting instead.
     */
    public void cancel() {
        handler.sendEmptyMessage(MESSAGE_CANCLE);
    }

    public void free() {
        if (mDrawingView != null)
            mDrawingView.free();
        cleanupAnimation();
    }


    @Override
    public boolean handleMessage(Message msg) {

        if (msg == null)
            return false;
        switch (msg.what) {
            case MESSAGE_LOOP_EMIT: {
                this.onUpdate(mCurrentTime);
                mCurrentTime += TIMMERTASK_INTERVAL;
                handler.sendEmptyMessageDelayed(MESSAGE_LOOP_EMIT, 1);
                break;
            }
            case MESSAGE_ONE_SHOT: {

                if (msg.arg1 > mTimeToLive) {
                    handler.sendEmptyMessage(MESSAGE_CANCLE);

                } else {
                    Message message = handler.obtainMessage();
                    message.what = MESSAGE_ONE_SHOT;
                    message.arg1 = msg.arg1 + 40;
                    this.onUpdate(mCurrentTime);
                    mCurrentTime += TIMMERTASK_INTERVAL;
                    handler.sendMessageDelayed(message, 1);
                }
                break;

            }
            case MESSAGE_CANCLE: {
                handler.removeCallbacksAndMessages(null);
                this.free();
                break;
            }

        }
        return false;
    }


    //-----------------------------------Particle class------------------------------------

    /**
     * Is a View to Draw many of{@link Particle}
     */
    static class ParticleField extends View {

        private ArrayList<Particle> mParticles;

        public ParticleField(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        public ParticleField(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public ParticleField(Context context) {
            super(context);
        }

        public void setParticles(ArrayList<Particle> particles) {
            mParticles = particles;
        }

        public void free() {
            synchronized (mParticles) {
                for (int i = 0; i < mParticles.size(); i++) {
                    mParticles.get(i).free();
                }

            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            // Draw all the particles
            synchronized (mParticles) {
                for (int i = 0; i < mParticles.size(); i++) {
                    mParticles.get(i).drawPoint(canvas);
                }
            }
        }
    }

    /**
     * The Particle class ,include all Attribute.
     * The Method {@link #drawPoint(Canvas)} to finish show Particle animation throw {@link Matrix} change.
     */
    public static class Particle {

        protected Bitmap mImage;

        public float mCurrentX;
        public float mCurrentY;

        public float mScale = 1f;
        public int mAlpha = 255;
        public int mAlphaFinalValue;
        public int mAlphaInitialValue;
        public int mAlphaValueIncrement;
        public float mScaleInitialValue;
        public float mScaleFinalValue;

        public float mSpeedX = 0f;
        public float mSpeedY = 0f;
        public float mAccelerationX;
        public float mAccelerationY;

        private Matrix mMatrix;
        private Paint mPaint;

        private float mInitialX;
        private float mInitialY;

        private int mTimeToLive;
        protected long mStartingMilisecond;
        private int mBitmapHalfWidth;
        private int mBitmapHalfHeight;
        private volatile boolean isRun;

        private List<ParticleModifier> mModifiers;

        protected Particle() {
            mMatrix = new Matrix();
            mPaint = new Paint();

        }

        public Particle(Bitmap bitmap) {
            this();
            mImage = bitmap;
        }

        public void init() {
            mScale = 1;
            mAlpha = 255;
        }

        public void configure(int timeToLive, float emiterX, float emiterY) {
            mBitmapHalfWidth = mImage.getWidth() / 2;
            mBitmapHalfHeight = mImage.getHeight() / 2;
            mInitialX = emiterX - mBitmapHalfWidth;
            mInitialY = emiterY - mBitmapHalfHeight;
            mCurrentX = mInitialX;
            mCurrentY = mInitialY;
            mTimeToLive = timeToLive;
            this.isRun = true;
        }

        public boolean update(long miliseconds) {
            long realMiliseconds = miliseconds - mStartingMilisecond;
            if (realMiliseconds > mTimeToLive || !isRun) {
                return false;
            }
            mCurrentX = mInitialX + mSpeedX * realMiliseconds + mAccelerationX * realMiliseconds * realMiliseconds;
            mCurrentY = mInitialY + mSpeedY * realMiliseconds + mAccelerationY * realMiliseconds * realMiliseconds;

            for (int i = 0; i < mModifiers.size(); i++) {
                if (!mModifiers.get(i).apply(Particle.this, realMiliseconds)) {
                    this.isRun = false;

                }
            }
            return this.isRun;
        }

        public void drawPoint(Canvas c) {
            if (!isRun)
                return;

            mMatrix.reset();
            mMatrix.postScale(mScale, mScale, mBitmapHalfWidth, mBitmapHalfHeight);
            mMatrix.postTranslate(mCurrentX, mCurrentY);
            mPaint.setAlpha(mAlpha);
            c.drawBitmap(mImage, mMatrix, mPaint);
        }

        public Particle activate(long startingMilisecond, List<ParticleModifier> modifiers) {
            mStartingMilisecond = startingMilisecond;
            // We do store a reference to the list, there is no need to copy, since the modifiers do not carte about states
            mModifiers = modifiers;
            return this;
        }

        public void free() {
            if (this.mImage != null && this.mImage.isRecycled()) {
                this.mImage.recycle();
            }
        }
    }


    //-----------------------------------Interface And Implement class------------------------------------

    /**
     * supports particle's modifier that change the particle's transparency or size( or speed and so on) as time go.
     */
    public interface ParticleModifier {

        /**
         * modifies the specific value of a particle given the current miliseconds
         *
         * @param particle
         * @param miliseconds
         */
        boolean apply(Particle particle, long miliseconds);

    }

    /**
     * Change the Particle's size as time go throw{@link #apply(Particle, long)}
     */
    public static class ScaleModifier implements ParticleModifier {

        private float mInitialValue;
        private float mFinalValue;
        private long mEndTime;
        private long mStartTime;
        private long mDuration;
        private Interpolator mInterpolator;

        public ScaleModifier(float initialValue, float finalValue, long startMilis, long endMilis, Interpolator interpolator) {
            mInitialValue = initialValue;
            mFinalValue = finalValue;
            mStartTime = startMilis;
            mEndTime = endMilis;
            mDuration = mEndTime - mStartTime;
            mInterpolator = interpolator;
        }

        public ScaleModifier(float initialValue, float finalValue, long startMilis, long endMilis) {
            this(initialValue, finalValue, startMilis, endMilis, new SinInterpolator());
        }

        @Override
        public boolean apply(Particle particle, long miliseconds) {

            if (miliseconds == 0) {
                float temp = mRandom.nextFloat() / 1.5f;
                if (temp < mInitialValue)
                    particle.mScaleInitialValue = mInitialValue;
                if (temp > mFinalValue)
                    particle.mScaleInitialValue = mFinalValue;

                particle.mScale = particle.mScaleInitialValue;
            } else if (miliseconds < mStartTime) {
                particle.mScale = mInitialValue;
            } else if (miliseconds > mEndTime) {
                particle.mScale = 0;
                return false;
            } else {
                particle.mScale = particle.mScaleInitialValue * mInterpolator.getInterpolation((miliseconds - mStartTime) * 1f / mDuration);
            }
            return true;

        }
    }


    public static class SinInterpolator implements Interpolator {


        @Override
        public float getInterpolation(float input) {
            return (float) Math.sin(Math.PI * input);
        }
    }

    /**
     * Change the Particle's transparency as time go throw {@link #apply(Particle, long)}
     */
    public static class AlphaModifier implements ParticleModifier {

        private int mInitialValue;
        private int mFinalValue;
        private long mStartTime;
        private long mEndTime;
        private float mDuration;
        private Interpolator mInterpolator;

        public AlphaModifier(int initialValue, int finalValue, long startMilis, long endMilis, Interpolator interpolator) {
            mInitialValue = initialValue;
            mFinalValue = finalValue;
            mStartTime = startMilis;
            mEndTime = endMilis;
            mDuration = mEndTime - mStartTime;
            mInterpolator = interpolator;
        }

        public AlphaModifier(int initialValue, int finalValue, long startMilis, long endMilis) {
            this(initialValue, finalValue, startMilis, endMilis, new LinearInterpolator());
        }

        @Override
        public boolean apply(Particle particle, long miliseconds) {

            if (miliseconds == 0) {

                //Random random = new Random();
                int temp = 0;
                if (mInitialValue < 0)
                    temp = mRandom.nextInt(-mInitialValue);
                else
                    temp = mRandom.nextInt(mInitialValue);

                particle.mAlpha = temp;
                particle.mAlphaInitialValue = -particle.mAlpha;

                if (mFinalValue < 0)
                    temp = mRandom.nextInt(-mFinalValue);
                else
                    temp = mRandom.nextInt(mFinalValue);

                particle.mAlphaFinalValue = temp;
                particle.mAlphaValueIncrement = particle.mAlphaFinalValue - particle.mAlphaInitialValue;
            } else if (miliseconds < mStartTime) {
                particle.mAlpha = mInitialValue;
            } else if (miliseconds >= mEndTime) {
                particle.mAlpha = particle.mAlphaFinalValue;
                particle.mScale = 0;
                return false;
            } else {
                float interpolaterdValue = mInterpolator.getInterpolation((miliseconds - mStartTime) * 1f / mDuration);
                int newAlphaValue = (int) (particle.mAlphaInitialValue + particle.mAlphaValueIncrement * interpolaterdValue);
                particle.mAlpha = newAlphaValue;
            }

            return true;
        }

    }

    /**
     * A interface supports set the attribute of Particle out of init .
     */
    public interface ParticleInitializer {

        void initParticle(Particle p, Random r);

    }

    public static class SpeedByComponentsInitializer implements ParticleInitializer {

        private float mMinSpeedX;
        private float mMaxSpeedX;
        private float mMinSpeedY;
        private float mMaxSpeedY;

        public SpeedByComponentsInitializer(float speedMinX, float speedMaxX, float speedMinY, float speedMaxY) {
            mMinSpeedX = speedMinX;
            mMaxSpeedX = speedMaxX;
            mMinSpeedY = speedMinY;
            mMaxSpeedY = speedMaxY;
        }

        @Override
        public void initParticle(Particle p, Random r) {
            p.mSpeedX = r.nextFloat() * (mMaxSpeedX - mMinSpeedX) + mMinSpeedX;
            p.mSpeedY = r.nextFloat() * (mMaxSpeedY - mMinSpeedY) + mMinSpeedY;
        }

    }

}