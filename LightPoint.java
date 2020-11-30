package com.pathfinder.shapeandlight;

import android.content.SharedPreferences;

/**
 * Created by Lionden on 5/25/2016.
 */

@SuppressWarnings({"WeakerAccess", "NullableProblems"})
class LightPoint implements SLSimRenderer.FreeMover, SLSimRenderer.OrderedSprite,
        Comparable<SLSimRenderer.OrderedSprite> {
    private static final String LOG_TAG = LightPoint.class.getSimpleName();
    protected static final int RED = 0;
    protected static final int GREEN = 1;
    protected static final int BLUE = 2;
    protected static final int WHITE = 3;
    protected static final int YELLOW = 4;
    protected static final int CYAN = 5;
    protected static final int MAGENTA = 6;
    protected static final int BLANK = 7;

    protected static final float[][] INIT_POSITIONS = {
            new float[] {0.0f, 0.0f, -10.0f},
            new float[] {7.07f, 0.0f, 7.07f},
            new float[] {-7.07f, 0.0f, 7.07f}
    };
    protected static final float[][] INIT_VELOCITIES = {
            new float[] {0.7f, 0.0f, 0.0f},
            new float[] {-0.5f, 0.0f, 0.5f},
            new float[] {-0.5f, 0.0f, -0.5f}
    };
    protected static final float[][] INIT_COLORS = {
            SLHelper.RED_COLOR_VEC,
            SLHelper.GRN_COLOR_VEC,
            SLHelper.BLU_COLOR_VEC
    };
    protected static final float BRIGHT_MULT = 4.0f;
    protected static final float BASE_SPRITE_SIZE = 0.8f;
    protected static int lightsOnCount = 3;
    public static boolean high_settings;
    // Vector defining the actual color values.
    protected volatile float[] mColorVec;
    protected float[] mDrawColor;
    // Vector setting the light's position in world space.
    protected volatile float[] mPos;
    // Position vector used in actual rendering, updating to mPos once per frame.
    protected float[] mDrawPos;
    // Vector defining the light's velocity in world space.
    protected volatile float[] mVel;
    protected float mDepth;
    protected static float base_sprite_size;
    protected static final float BRIGHT_SIZE_MULT = 1.5f;
    protected static final float SPRITE_FLARE_BRIGHT_SIZE_MULT = 2.0f;
    protected int mSpriteType;
    // Occlusion test offset so only 1 light point is tested per frame, minimizing performance hit.
    protected int mOccTestCounter;
    // Occlusion boolean.
    protected boolean mVisible;
    protected float mVisFactor;
    // Max visibility deltas allowed per 3-frame visibility computation cycle.
    protected static final float MAX_VIS_PERFRAME_INC = 0.44f;
    protected static final float MAX_VIS_PERFRAME_DEC = 0.5f;
    // 'Bright' light boolean.
    protected boolean mBright = false;
    // Occlusion test boolean.
    protected boolean mDoOccTest = false;
    // Boolean for whether the light point is moving freely.
    protected volatile boolean mIsFree = true;
    // Boolean for whether the light is enabled/on.
    protected volatile boolean mIsOn = true;
    // Boolean for whether the light is in strobe mode.
    protected boolean mStrobeEnabled = false;
    // Boolean for whether the light is touched by a screen MotionEvent.
    volatile boolean mTouched = false;
    // Float array containing the light's screen coordinates for touch detection.
    volatile float[] mScreenCoords;
    // Strobing params.
    protected static final long STROBE_LENGTH = 100 * SLSimRenderer.NANOS_PER_MILLI;
    protected long mStrobeStartT;
    // Boolean for while in strobe mode whether the light is 'strobing', or on.
    protected boolean mStrobeOn = false;
    protected static final float[] ALPHA_OFF = new float[] {1.0f, 1.0f, 1.0f, 0.0f};
    protected static final float[] ALPHA_ON = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
    protected static final float[] ALPHA_BRIGHT = new float[] {0.0f, 0.0f, 0.0f, BRIGHT_MULT};
    protected static final float[] STROBE_OFF_DRAW_COL = new float[] {0.08f, 0.08f, 0.08f, 0.0f};


    // Constructor.
    public LightPoint(int occTestCountStart) {
        mColorVec = SLHelper.WHI_COLOR_VEC.clone();
        mDrawColor = SLHelper.WHI_COLOR_VEC.clone();
        mPos = Vector.ZERO.clone();
        mDrawPos = Vector.ZERO.clone();
        mVel = Vector.ZERO.clone();
        mSpriteType = SLSimRenderer.LIGHT_POINT;
        mScreenCoords = Vector.ZERO.clone();
        mOccTestCounter = occTestCountStart;
    }

    public LightPoint setColor(int color) {
        switch (color) {
            case RED:
                setColorVec(SLHelper.RED_COLOR_VEC);
                break;
            case GREEN:
                setColorVec(SLHelper.GRN_COLOR_VEC);
                break;
            case BLUE:
                setColorVec(SLHelper.BLU_COLOR_VEC);
                break;
            case WHITE:
                setColorVec(SLHelper.WHI_COLOR_VEC);
                break;
            case YELLOW:
                setColorVec(SLHelper.YEL_COLOR_VEC);
                break;
            case CYAN:
                setColorVec(SLHelper.CYA_COLOR_VEC);
                break;
            case MAGENTA:
                setColorVec(SLHelper.MAG_COLOR_VEC);
                break;
            case BLANK:
                setColorVec(SLHelper.BLK_COLOR_VEC);
        }
        return this;
    }

    public void setColorVec(float[] colorVec) {
        mColorVec = colorVec.clone();
        mColorVec[3] = mBright ? BRIGHT_MULT : 1.0f;
        mDrawColor = Vector.convertToVec4(
                SLHelper.getBrightRGB(colorVec, mBright ? 0.66f : 0.4f, mBright ? 1.0f : 0.9f));
        if (mBright) {
            mDrawColor[3] = BRIGHT_MULT;
            mSpriteType = SLSimRenderer.BR_LIGHT_POINT;
        }
    }

    public float[] getPos() {
        return mPos;
    }

    public float[] getDrawPos() {
        return mDrawPos;
    }

    public float[] getVel() {
        return mVel;
    }

    public float getMass() {
        return mBright ? 0.85f : 0.4f;
    }

    public void setPos(float[] pos) {
        mPos = pos.clone();
    }

    public void setVel(float[] vel) {
        mVel = vel.clone();
    }

    public void setFree(boolean free) {
        mIsFree = free;
    }

    public void setOn(boolean on) {
        mIsOn = on;
        mVisible = on;
        mVisFactor = 1.0f;
        addToLightCount(on);
    }

    public void setVisibility(float factor, boolean flVisOffScrn, boolean paused) {
        float newVisFactor = flVisOffScrn ? 1.0f : factor;
        boolean transition = newVisFactor > mVisFactor + MAX_VIS_PERFRAME_INC
                || newVisFactor < mVisFactor - MAX_VIS_PERFRAME_DEC;
        if (transition && !(mStrobeEnabled && !paused)) {
            if (newVisFactor > mVisFactor) mVisFactor += MAX_VIS_PERFRAME_INC;
            else mVisFactor -= MAX_VIS_PERFRAME_DEC;
        } else {
            mVisFactor = newVisFactor;
        }
        mVisible = factor > 0.01f || flVisOffScrn;
    }

    @Override
    public boolean setTouched(boolean touched) {
        mTouched = touched;
        return touched;
    }

    @Override
    public boolean isTouched() {
        return mTouched;
    }

    @Override
    public void setScreenCoords(float[] coords) {
        mScreenCoords = coords.clone();
    }

    @Override
    public float[] getScreenCoords() {
        return mScreenCoords;
    }

    public void enableStrobe(boolean enable) {
        mStrobeEnabled = enable;
        // Keep the lights default state unlit for the strobe effect if enabled.
        if (enable) {
            mStrobeOn = false;
            mColorVec = Vector.multVV(mColorVec, ALPHA_OFF);
        }
    }

    public boolean isFree() {
        return mIsFree;
    }

    public boolean isOn() {
        return mIsOn;
    }

    public void reset() {
        mPos = Vector.ZERO.clone();
        mVel = Vector.ZERO.clone();
    }

    public float[] getCol() {
        float[] drawCol = mStrobeEnabled && !mStrobeOn ? STROBE_OFF_DRAW_COL : mDrawColor;
        float[] returnCol = high_settings ? drawCol.clone() : mColorVec.clone();
        // Adjust the alpha value for use in low & med settings flare shaders.
        returnCol[3] *= rgbAlphaFactor(mColorVec);
        return returnCol;
    }

    public float getDepth() {
        return mDepth;
    }

    public float getSpriteSize() {
        float mult = high_settings ? BRIGHT_SIZE_MULT : SPRITE_FLARE_BRIGHT_SIZE_MULT;
        return mBright ? mult * base_sprite_size : base_sprite_size;
    }

    public int getSpriteType() {
        return mSpriteType;
    }

    public void setDepth(float depth) {
        mDepth = depth;
    }

    @Override
    public int compareTo(SLSimRenderer.OrderedSprite another) {
        return Float.compare(another.getDepth(), this.getDepth());
    }

    // Increment velocity by an acceleration vector.
    public synchronized void incVel(float[] accel) {
        mVel = Vector.sumVV(mVel, accel);
    }

    public void perFrameUpdate(float frameFactor) {
        // Do no update if framerate has stalled.
        if (frameFactor > SLSimRenderer.MAX_FRAMETIME_FACTOR) return;
        // Position coord limiter, causing the object to "deflect" back towards the center point.
        if (Vector.length(mPos) > SLHelper.MAX_DISTANCE_CENTER) {
            mVel = Vector.reverseComp(mVel, Vector.norm(mPos));
        }
        // Speed limiter, to 90% the SLSimRenderer "speed of sound".
        float speed = Vector.length(mVel);
        if (speed > SLSimRenderer.SPEED_OF_SOUND * 0.9f) {
            float slower = SLSimRenderer.SPEED_OF_SOUND * 0.9f;
            mVel = Vector.scaleSV(slower / speed, mVel);
        }
        // Increment position.
        float[] frameVel = Vector.scaleSV(frameFactor, mVel);
        mPos = Vector.sumVV(mPos, frameVel);
        mDrawPos = mPos.clone();
        // Strobing behavior.
        if (mStrobeEnabled && mStrobeOn) {
            if (System.nanoTime() - mStrobeStartT > STROBE_LENGTH) {
                // Stop the "strobing", i.e. turn the light back off.
                mStrobeOn = false;
                // Restore the alpha back to 0.0f.
                mColorVec = Vector.multVV(mColorVec, ALPHA_OFF);
            }
        }
    }

    public void cycleOcclusionTest() {
        // Determine occlusion test boolean
        mOccTestCounter++;
        mDoOccTest = mOccTestCounter >= 3;
        if (mDoOccTest) {
            mOccTestCounter = 0;
        }
    }

    public void startStrobe() {
        mStrobeOn = true;
        mStrobeStartT = System.nanoTime();
        // Turn the light "on" by setting alpha back to 1.0f
        mColorVec = Vector.sumVV(mColorVec, mBright ? ALPHA_BRIGHT : ALPHA_ON);
    }

    public static void resetLightsOnCount() {
        lightsOnCount = 0;
    }

    public static int getLightsOnCount() {
        return lightsOnCount;
    }

    public static void addToLightCount(boolean inc) {
        if (inc) {
            lightsOnCount++;
        }
    }

    public void saveToPrefs(SharedPreferences.Editor edit, int light) {
        edit.putBoolean("light_" + light + "_on_boolean", mIsOn);
        edit.putBoolean("light_" + light + "_strobe_boolean", mStrobeEnabled);
        edit.putBoolean("light_" + light + "_bright_boolean", mBright);
        for (int i = 0; i < 3; i++) {
            edit.putFloat("light_" + light + "_pos_" + i, mPos[i]);
            edit.putFloat("light_" + light + "_vel_" + i, mVel[i]);
            edit.putFloat("light_" + light + "_col_" + i, mColorVec[i]);
        }
    }

    public void restoreFromPrefs(SharedPreferences prefs, int light) {
        float[] pos = new float[3];
        float[] vel = new float[3];
        float[] col = new float[4];
        for (int i = 0; i < 3; i++) {
            pos[i] = prefs.getFloat("light_" + light + "_pos_" + i, INIT_POSITIONS[light][i]);
            vel[i] = prefs.getFloat("light_" + light + "_vel_" + i, INIT_VELOCITIES[light][i]);
            col[i] = prefs.getFloat("light_" + light + "_col_" + i, INIT_COLORS[light][i]);
        }
        // Always load an alpha of 1.0f for the color.
        col[3] = 1.0f;
        mPos = pos;
        mVel = vel;
        mBright = prefs.getBoolean("light_" + light + "_bright_boolean", light == 2);
        setColorVec(col);
        setOn(prefs.getBoolean("light_" + light + "_on_boolean", true));
        enableStrobe(prefs.getBoolean("light_" + light + "_strobe_boolean", false));
        }

    public void defaultState(int i) {
        mPos = INIT_POSITIONS[i].clone();
        mVel = INIT_VELOCITIES[i].clone();
    }

    // Factor to adjust flare alphas to compensate for high-G or no-RG (only B) conditions,
    // either attenuating by 20% or boosting by 28%.
    static float rgbAlphaFactor(float[] colorVec) {
        float grnAt1 = 0.8f;
        float grnAt2 = 1.0f - grnAt1;
        float noRGFct = 0.28f;
        float grnAtten = grnAt1 + (grnAt2 * (1.0f - colorVec[1]));
        float rgSum = Math.min(1.0f, colorVec[0] + colorVec[1]);
        float lowRGboost = 1.0f + noRGFct * (1.0f - rgSum) * colorVec[2];
        return grnAtten * lowRGboost;
    }

}
