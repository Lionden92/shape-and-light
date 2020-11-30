package com.pathfinder.shapeandlight;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * Created by Lionden on 5/24/2016.
 *
 * Does both the 3D world simulation computations and OpenGL rendering.
 *
 */

@SuppressWarnings({"WeakerAccess", "NullableProblems"})
public class SLSimRenderer implements GLSurfaceView.Renderer {
    private static final String LOG_TAG = SLSimRenderer.class.getSimpleName();

    private Context mContext;

    static boolean is_high_dp;
    // Time parameters.
    // Frame time constant in nanoseconds, giving 60 fps for an FPS_mult of 1.
    private final static double FRAME_TIME = 16666666.667;
    final static long NANOS_PER_MILLI = 1000000;
    private final static long NANOS_PER_SECOND = 1000000000;
    private final static long LONG_PRESS_TIME = 300 * NANOS_PER_MILLI;
    private final static long FADE_TIME = 250 * NANOS_PER_MILLI;
    private boolean mSplashUp;
    private final static float PI = (float) Math.PI;
    private final static float NEAR_CLIP = 3.25f;
    private final static float FAR_PLANE_LOW_BIT_BUFFER = 500.0f;
    private final static float FAR_PLANE_NORMAL_BUFFER = 850.0f;
    private final static float FLICKER_DIST = 300.0f;
    private static float far_plane;
    // Factor calculated from actual nanoseconds passed during a frame to normalize time-dependent
    // factors like velocities and forces.
    private float mFrametimeFactor = 1.0f;
    static final float MAX_FRAMETIME_FACTOR = 7.5f;
    protected boolean mSimPaused;
    protected long mPausedTime;
    protected boolean mPerfPaused;
    protected boolean mCracklePaused;
    // Framerate params.
    private long mThisFrameStartTime;
    private long mPrevFrameStartTime;
    private long mPrevFrameEndTime;
//    private int mFrameCounter;
//    private int mFrameRate;
//    private long mFrameCounterTime;
    // Time params for light strobing.
    private final static long STROBE_TOL = 66 * NANOS_PER_MILLI;
    private long mStrobeInterval;
    private long mSimStartTime;
    private boolean mStrobeCycled;
    private boolean mStrobing;
    private int mStrobeCounter;
    // Objects giving access to parent classes.
    protected ResourceGetter mResGetter;
    protected SLSurfaceView.SLMainLauncher mMainLauncher;
    // The Shape object.
    volatile Shape mShape;
    // Array for the 3 lights.
    volatile LightPoint[] mLights = new LightPoint[3];
    private volatile LightPoint[] mOnLights;
    // Active/selected FreeMover interface object.
    volatile FreeMover mActiveMover;
    // Gravity Circles.
    private GravityCircle mCtrRing;
    GravityCircle mMobRing;
    GravityCircle[] mGravRings;
    // The eye object.
    volatile Eye mEye;
    // Shape decal constants and field.
    static final int STAR_DECAL = 1;
    static final int ROSE_DECAL = 2;
    static final int EYE_DECAL = 3;
    static final int BFLY_DECAL = 4;
    static final int FISH_DECAL = 5;
    // Int flag for the shape decal.
    private int mShapeDecalFlag;
    // Eye control sensitivity setting params.
    private int mEyeSensitivy;
    static final int LOW_SENSITIVITY = 1;
    static final int MED_SENSITIVITY = 2;
    static final int HIGH_SENSITIVITY = 3;
    private static final float EYE_SENS_FACTOR = 1.8f;
    private boolean mEyeAltLock;
    // Constants for active objects.
    static final int SHAPE = 1;
    static final int LIGHT_0 = 2;
    static final int LIGHT_1 = 3;
    static final int LIGHT_2 = 4;
    static final int GRAV_CIRC = 5;
    volatile int mActiveMoverFlag = SHAPE;
    // Int parameter for number of active gravity circles (always 1 or 2).
    private int mGravCircleCount;
    // Boolean for whether the shape & lights are exclusively affected by mobile gravity if present.
    private boolean mMobGravExclusive;
    // Minimum background lighting setting parameter.
    private float mMinLightingFactor;
    protected static final float MIN_LIGHT_FACTOR_1 = 0.3f;
    protected static final float MIN_LIGHT_FACTOR_2 = 0.5f;
    protected static final float MIN_LIGHT_FACTOR_3 = 0.85f;
    protected static final float MIN_LIGHT_FACTOR_4 = 1.3f;
    protected static final float MIN_LIGHT_FACTOR_5 = 1.9f;
    // Force constants.
    private static final float SPRING_FORCE = 0.000025f;
    private static final float SPRING_SQ_FORCE = 0.000003f;
    private static final float SPRING_FORCE_SCALING = 0.00008f;
    private static final float SPRING_SQ_SCALING = 0.000006f;
//    protected static final float NEWTON_FORCE = 1.6f;
    private static final float RING_RADIUS = 1.38f;
    private int mSpriteCount;
    private FloatBuffer mSpritePositions;
    private FloatBuffer mSpriteColors;
    private FloatBuffer mSpriteSizes;
    private FloatBuffer mSpriteTypes;
    // Dust field array.
    private DustPoint[] mDustPoints;
    private static int dust_count;
    private static final float DUST_RANGE = 1350.0f;
    // Smoke bubbles parameters.
    private static int bubbles_count;
    private static final float BUBBLES_START_SIZE = 1.5f;
    private static float bubbles_delta_size;
    private BubblePoint[] mBubblePoints;
    protected boolean mBubblesGenerating;
    protected boolean mBubblesUp;
    private static final float BUBB_VEL = 0.0061f;
    private static final float BUBBLE_RND_FACTOR = 0.000195f;
    private int mBubblesEmittedCount = 0;
    private int mNewBubbleIndex = 0;
    private static int bub_fade_num;
    private static long bubbles_duration;
    private static float[] bubble_min_color;
    // Sparkle points parameters.
    private static int sparkle_count;
    private static float sparkle_start_size;
    private static float sparkle_delta_size;
    private volatile SparklePoint[][] mSparkles;
    protected boolean mSparklesGenerating = false;
    protected boolean mSparkling = false;
    private static final float SPARKLE_SPEED = 0.038f;
    private static final float SPARKLE_RND_FACTOR = 0.007f;
    private int mSparklesEmittedCount = 0;
    private long mSparklingStartTime;
    private boolean mCrackling;
    private static long sparkles_duration;
    private int mSparkleFrameCounter;
    private static final long CRACKLE_WAIT = NANOS_PER_SECOND;
    protected float[] mSparkleLightPos1 = Vector.ZERO;
    protected float[] mSparkleLightPos2 = Vector.ZERO;
    protected float[] mSparkleLightCol = Vector.ZERO4;
    protected boolean mCracklePlayed;
    protected static final float SPARKLE_COL_FUNC_PER = 30.0f;
    protected float mSparkleColorFuncX;
    private static float[] sparkle_color;
    private float mSparkleSpriteSize;
    private float mSpAlpha;
    // Smoke point parameters.
    private static int smoke_count;
    private static final float SMOKE_START_SIZE = 22.0f;
    private static final float SMOKE_DELTA_SIZE = 115.0f;
    private static final float SMOKE_SPEED = 0.021f;
    private SmokePoint[] mSmokePuffs;
    protected boolean mSmoking;
    private static final float SMOKE_RND_FACTOR = 0.00044f;
    private int mSmokeEmittedCount = 0;
    private long mSmokingStartTime;
    private static final long SMOKE_DURATION = 16 * NANOS_PER_SECOND;
    private static int rolling_smoke_sprite_type;
    private static float[] smoke_min_color;
    // Help mode parameters
    protected volatile boolean mHelpOn;
    protected boolean mHelpColorOn;
    protected boolean mHelpColorCycled;
    protected static final long HELP_BLINK_PERIOD = 300 * NANOS_PER_MILLI;
    // Performance mode parameters
    protected boolean mPerforming;
    protected int[] mPerfRotationData;
    protected int mRotatorIndex;
    protected int[] mPerfBubblerData;
    protected int mBubblerIndex;
    protected int[] mPerfSparklerTimes;
    protected int mSparklerIndex;
    private static final int BASE_PERF_OFFSET = 25;
    private static int perf_add_offset = 0;
    protected boolean mReqMusPosUpdate;
    private int mPerfFrameCounter;
    private static final long INIT_WAIT = 250 * NANOS_PER_MILLI;
    private static final long MUS_POS_UPDATE_WAIT = 500 * NANOS_PER_MILLI;
    protected static final int BEAUTIFUL_SIL = 1;
    protected static final int BLUE_DANUBE = 2;
    protected static final int FANTASY_ICE = 3;
    protected static final int MIDNIGHT_CITY = 4;
    protected static final int STREET_LIGHTS = 5;
    protected static final int SUNRISE_WITHOUT = 6;
    protected static final int TIME_REFLECTION = 7;
    protected int mMusicTrack;
    protected float mPerfAttenuation;
    // Sound & music params.
    protected volatile int mMusicPosition;
    protected volatile boolean mMusicPlaying;
    protected volatile long mMusicStartTime;
    protected long mMusPosUpdateT;
    protected volatile long mMusPosReadTime;
    private final static float SOUND_FALLOFF_FACTOR = 2400.0f;
    // For Doppler calculations.
    protected final static float SPEED_OF_SOUND = 80.0f;
    // Window width & height params for use in pixel coordinate computations.
    protected int mWidth;
    protected int mHeight;
    // Flag for visuals settings.
    protected static final int LOW_VISUALS = 0;
    protected static final int MED_VISUALS = 1;
    protected static final int HIGH_VISUALS = 2;
    protected static final int DUMMY_DEFAULT_VIS = 9;
    protected int mVisualsLevel;
    protected boolean mIsHighVisuals;
    protected boolean mIsMedVisuals;
    protected boolean mIsLowVisuals;
    protected boolean mIsDepthBufferLowBit;
    // OpenGL projection matrix.
    private float[] mProjectionMatrix = new float[16];
    // View matrix for the orthographic UI rendering.
    private float[] mUIViewMatrix = new float[16];
    // OpenGL MVP Matrix.
    private float[] mMVPMatrix = new float[16];
    // MVP Matrix for origin-centered array.
    private float[] mOrigMVPMat = new float[16];
    // MVP Matrix for the GUI.
    private float[] mUIMVPMatrix = new float[16];
    // OpenGL program handles.
    private int mShapeProgramHandle;
    private int mSpriteProgramHandle;
    private int mFlatProgramHandle;
    private int mUIProgramHandle;
    private int mFlareSurfProgramHandle;
    private int mSparkleFlareProgHandle;
    // Handles for the textures.
    private int mMarbleTextureDataHandle;
    private int mStarMarbleTexDataHandle;
    private int mRoseMarbleTexDataHandle;
    private int mEyeMarbleTexDataHandle;
    private int mButterflyTexDataHandle;
    private int mFishMarbleTexDataHandle;
    private int mSpriteTexAtlasDataHandle;
    private int mLensFlareTexDataHandle;
    private int mSoftCircTexDataHandle;
    // Icons.
    protected int mHorizIconTexDataHandle;
    protected int mVertIconTexDataHandle;
    protected int mLockedIconTexDataHandle;
    protected int mUnlockedIconTexDataHandle;
    protected int mResetIconTexDataHandle;
    protected int mHelpIconTexDataHandle;
    protected int mGravCircIconTexDataHandle;
    protected int mCubeIconTexDataHandle;
    protected int mIcosIconTexDataHandle;
    protected int mRndicosIconTexDataHandle;
    protected int mRnddodecIconTexDataHandle;
    protected int mLightIconTexDataHandle;
    protected int mLightIconBrTexDataHandle;
    protected int mEmitterIconTexDataHandle;
    protected int mMusicIconTexDataHandle;
    protected int mMenuIconTexDataHandle;
    protected int mPauseIconTexDataHandle;
    protected int mEyeControlRightTexDataHandle;
    protected int mEyeControlLeftTexDataHandle;
    // Handles for gl shader uniforms and attributes.
    private int mMVPMatrixHandle;
    private int mRotMatrixHandle;
    private int mModMatrixHandle;
    private int mPositionHandle;
    private int mNormalsHandle;
    private int mCentersHandle;
    private int mTextureCoordHandle0;
    private int mTextureCoordHandle1;
    private int mTextureCoordHandle2;
    private int mTextureUniformHandle;
    private int mTextureUniformHandle2;
    private int mColorHandle;
    private int mBaseSizeHandle;
    private int mSpriteTypeHandle;
    private int mLight00PosHandle;
    private int mLight01PosHandle;
    private int mLight02PosHandle;
    private int mLight00ColHandle;
    private int mLight01ColHandle;
    private int mLight02ColHandle;
    private int mSparklePosHandle;
    private int mSparkleColHandle;
    private int mCircle00PosHandle;
    private int mCircle01PosHandle;
    private int mCircle00ColHandle;
    private int mCircle01ColHandle;
    // Preset colors.
    private static final float[] GRAV_CIRC_RED = {1.0f, 0.0f, 0.33f, 1.0f};
    private static final float[] GRAV_CIRC_BLUE = {0.33f, 0.0f, 1.0f, 1.0f};
    private static final float[] DUST_COLOR = {0.7f, 0.6f, 0.6f, 0.4f};
    private static final float[] BUBBLE_BASE_COLOR = {0.12f, 0.12f, 0.16f, 0.4f};
    private static final float[] SMOKE_BASE_COLOR = {0.06f, 0.06f, 0.06f, 0.5f};
    private static final float[] EYE_CNTRL_COLOR = {0.50f, 0.50f, 0.50f, 0.25f};
    private static final float[] EYE_CNTRL_PRESSED = {0.25f, 0.40f, 0.66f, 0.66f};
    private static final float[] EYE_CNTRL_SCRN_EDGE = {0.74f, 0.40f, 0.25f, 0.85f};
    // Types for the gl sprite shaders to distinguish sprite types.
    protected static final int GRAV_CIRCLE_SPRITE = 1;
    protected static final int DUST_POINT = 2;
    protected static final int BUBBLE_POINT = 3;
    protected static final int SPARKLE_POINT = 4;
    protected static final int SMOKE_POINT_1 = 5;
    protected static final int SMOKE_POINT_2 = 6;
    protected static final int SMOKE_POINT_3 = 7;
    protected static final int SMOKE_POINT_4 = 8;
    protected static final int LIGHT_POINT = 9;
    protected static final int BR_LIGHT_POINT = 10;
    // Preset mobile gravity circle parameters.
    protected static final float[] INIT_MOV_RING_POS = new float[] {0.0f, 18.0f, 0.0f};
    protected static final float[] INIT_MOV_RING_VEL = new float[] {0.06f, 0.0f, 0.0f};
    // Data for the GUI.
    protected volatile boolean mDrawUI;
    protected boolean mPerformanceHidesUI = false;
    protected boolean mOnScrnEdgeRt;
    protected boolean mOnScrnEdgeLft;
    private FloatBuffer mXaxisPositions;
    private FloatBuffer mYaxisPositions;
    private FloatBuffer mZaxisPositions;
    private FloatBuffer mAxisPolygonPositions;
    private int mAxisViewportX;
    private int mAxisViewportY;
    private int mAxisViewportW;
    private int mAxisViewportH;
    private static final float[] XAXIS_COL = new float[] {0.1f, 0.30f, 1.0f, 0.55f};
    private static final float[] YAXIS_COL = new float[] {0.0f, 1.0f, 0.0f, 0.33f};
    private static final float[] ZAXIS_COL = new float[] {1.0f, 0.2f, 0.1f, 0.33f};
    private static final float[] AXIS_POLY_COL = new float[] {0.5f, 0.5f, 0.5f, 0.33f};
    private float mButtonSize;
    private float mButtonMargin;
    // Buttons.
    protected SLButton mEyeMoveModeB;
    protected SLButton mEyeLockB;
    protected SLButton mResetB;
    protected SLButton mHelpInfoB;
    protected SLButton mEyeControlRight;
    protected SLButton mEyeControlLeft;
    protected SLButton mEyeControlPosRight;
    protected SLButton mEyeControlPosLeft;
    protected SLButton mShapeB;
    protected SLButton mLight0B;
    protected SLButton mLight1B;
    protected SLButton mLight2B;
    protected SLButton mGravCircB;
    protected SLButton mEmitterB;
    protected SLButton mMusicB;
    protected SLButton mMenuB;
    protected SLButton mPauseB;
    protected volatile SLButton mObjManipHelpB;
    protected volatile SLButton mOrientAxesB;
    // "pop-up menu" buttons.
    protected SLButton mCubeB;
    protected SLButton mIcosB;
    protected SLButton mRndDodecB;
    protected SLButton mRndIcosB;
    protected SLButton mRedColorB;
    protected SLButton mYellowColorB;
    protected SLButton mGreenColorB;
    protected SLButton mCyanColorB;
    protected SLButton mBlueColorB;
    protected SLButton mMagentColorB;
    protected SLButton mWhiteColorB;
    protected float mEyeControlWidth;
    protected float mEyeControlHeight;
    // "Button" used as surface to draw shader-based lens flares and rays.
    protected SLButton mFlareSurface;
    // "Button" used as surface to draw sparkle flare in medium settings.
    protected SLButton mSparkleFlareSurf;
    // Tolerance parameter for detecting touches on the active FreeMover object.
    protected float mBaseGrabTolerance;
    // "pop-up menu" button params.
    protected volatile boolean mShapeMenuOn = false;
    protected volatile long mShapeMenuStart;
    protected volatile boolean mLight0MenuOn = false;
    protected volatile long mLight0MenuStart;
    protected volatile boolean mLight1MenuOn = false;
    protected volatile long mLight1MenuStart;
    protected volatile boolean mLight2MenuOn = false;
    protected volatile long mLight2MenuStart;


    // Interface for objects that can be acted on by the center force.
    @SuppressWarnings("unused")
    protected interface FreeMover {

        float[] getPos();

        float[] getVel();

        float getMass();

        void setPos(float[] pos);

        void incVel(float[] accel);

        void setVel(float[] vel);

        boolean isFree();

        void setFree(boolean free);

        void reset();

        boolean setTouched(boolean touched);

        boolean isTouched();

        float[] getScreenCoords();

        void setScreenCoords(float[] coords);

    }

    // Interface for sprite points that need z-ordered rendering.
    protected interface OrderedSprite {

        void setDepth(float depth);

        float getDepth();

        float getSpriteSize();

        float[] getDrawPos();

        float[] getCol();

        int getSpriteType();

    }

    protected interface ResourceGetter {

        int[] getIntArrayResource(int resID);

    }


    // Class for the center circle which implements OrderedSprite.
    protected class GravityCircle implements OrderedSprite, FreeMover, Comparable<OrderedSprite> {

        float gDepth;
        float gSpriteSize = 8.0f;
        float gStrength;
        float gForceFactor;

        float[] gColor = Vector.ZERO4.clone();
        float[] gPos;
        float[] gVel;
        volatile boolean gIsFree = true;
        volatile boolean gSpringSq = false;
        volatile boolean gTouched = false;
        volatile float[] gScreenCoords;
        int gSpriteType = GRAV_CIRCLE_SPRITE;
        double gPulseTime;
        float gPulsePeriodMillis;


        public GravityCircle(float[] pos, float[] vel) {
            gPos = pos;
            gVel = vel;
            // Give the pulsing an initial random offset.
            gPulseTime = 1000.0 * Math.random();
        }

        public void setDepth(float depth) {
            gDepth = depth;
        }

        public float getDepth() {
            return gDepth;
        }

        public float getSpriteSize() {
            return gSpriteSize;
        }

        public float[] getCol() {
            return gColor;
        }

        public float[] getDrawPos() {
            return gPos;
        }

        public float[] getPos() {
            return gPos;
        }

        public float[] getVel() {
            return gVel;
        }

        public float getMass() {
            return 2.0f;
        }

        public void setPos(float[] pos) {
            gPos = pos.clone();
        }

        public void incVel(float[] accel) {
            gVel = Vector.sumVV(gVel, accel);
        }

        public void setVel(float[] vel) {
            gVel = vel.clone();
        }

        public boolean isFree() {
            return gIsFree;
        }

        public void setFree(boolean free) {
            gIsFree = free;
        }

        @Override
        public boolean setTouched(boolean touched) {
            gTouched = touched;
            return touched;
        }

        @Override
        public boolean isTouched() {
            return gTouched;
        }

        @Override
        public void setScreenCoords(float[] coords) {
            gScreenCoords = coords.clone();
        }

        @Override
        public float[] getScreenCoords() {
            return gScreenCoords;
        }

        public void reset() {
            gPos = Vector.ZERO.clone();
            gVel = Vector.ZERO.clone();
        }

        public int getSpriteType() {
            return gSpriteType;
        }

        public boolean isSpringSq() {
            return gSpringSq;
        }

        public void setSpringSq(boolean springSq) {
            gSpringSq = springSq;
        }

        public void setForceFactor(float strength) {
            // Force mult scale will range from 1 to 5.
            gStrength = strength;
            if (gSpringSq) {
                gForceFactor = SPRING_SQ_FORCE + (SPRING_SQ_SCALING * (strength - 1.0f));
            } else {
                gForceFactor = SPRING_FORCE + (SPRING_FORCE_SCALING * (strength - 1.0f));
            }
            // Pulse period will range from 1500 ms down to 300 ms.
            gPulsePeriodMillis = 1500.0f / strength;
        }

        @Override
        public int compareTo(OrderedSprite another) {
            return Float.compare(another.getDepth(), this.getDepth());
        }

        public void perFrameUpdate(float frameFactor) {
            // Do no update if framerate has stalled.
            if (frameFactor > MAX_FRAMETIME_FACTOR) return;
            // Position coord limiter, causing the object to "deflect" back towards the center point.
            if (Vector.length(gPos) > SLHelper.MAX_DISTANCE_CENTER) {
                gVel = Vector.reverseComp(gVel, Vector.norm(gPos));
            }
            // Speed limiter, to 90% the SLSimRenderer "speed of sound".
            float speed = Vector.length(gVel);
            if (speed > SLSimRenderer.SPEED_OF_SOUND * 0.9f) {
                float slower = SLSimRenderer.SPEED_OF_SOUND * 0.9f;
                gVel = Vector.scaleSV(slower / speed, gVel);
            }
            float[] frameVel = Vector.scaleSV(frameFactor, gVel);
            gPos = Vector.sumVV(gPos, frameVel);
            // Code to pulsate the circle color.
            gPulseTime += 16.67 * frameFactor;
            float alpha = SLHelper.sineCycle(gPulseTime, 0.5f, gPulsePeriodMillis) + 0.5f;
            float[] color = gSpringSq ? GRAV_CIRC_RED : GRAV_CIRC_BLUE;
            gColor = Vector.multVV(color, new float[] {1.0f, 1.0f, 1.0f, alpha});
        }

        // Save params to SharedPreferences, invoked only on the mobile ring.
        public void saveToPrefs(SharedPreferences.Editor edit) {
            for (int i = 0; i < 3; i++) {
                edit.putFloat("mov_ring_pos_" + i, gPos[i]);
                edit.putFloat("mov_ring_vel_" + i, gVel[i]);
            }
            edit.putBoolean(SLSurfaceView.MOV_RING_SPRINGSQ, gSpringSq);
        }

        public void restoreFromPrefs(SharedPreferences prefs) {
            float[] pos = new float[3];
            float[] vel = new float[3];
            for (int i = 0; i < 3; i++) {
                pos[i] = prefs.getFloat("mov_ring_pos_" + i, INIT_MOV_RING_POS[i]);
                vel[i] = prefs.getFloat("mov_ring_vel_" + i, INIT_MOV_RING_VEL[i]);
            }
            gPos = pos;
            gVel = vel;

        }

        public void defaultState() {
            gPos = INIT_MOV_RING_POS.clone();
            gVel = INIT_MOV_RING_VEL.clone();
        }

    }

    // Class for the dust points which implements OrderedSprite.
    protected class DustPoint implements OrderedSprite, Comparable<OrderedSprite> {

        float dDepth;
        float dSpriteSize;

        float[] dColor = DUST_COLOR;
        float[] dPos;

        int dSpriteType = DUST_POINT;

        public DustPoint(float[] pos) {
            dPos = pos;
            // Make dust points a little bigger for low visuals settings where they're few and
            // the device is likely to have a small screen. And boost the alpha.
            dSpriteSize = is_high_dp ? 0.51f : 0.6f;
            if (mIsLowVisuals) {
                dSpriteSize = is_high_dp ? 0.67f : 0.8f;
                dColor[3] = 0.55f;
            }
        }

        public void setDepth(float depth) {
            dDepth = depth;
        }

        public float getDepth() {
            return dDepth;
        }

        public float getSpriteSize() {
            return dSpriteSize;
        }

        public float[] getCol() {
            return dColor;
        }

        public float[] getDrawPos() {
            return dPos;
        }

        public int getSpriteType() {
            return dSpriteType;
        }

        @Override
        public int compareTo(OrderedSprite another) {
            return Float.compare(another.getDepth(), this.getDepth());
        }
    }

    // Class for bubble points which implements OrderedSprite.
    protected class BubblePoint implements OrderedSprite, Comparable<OrderedSprite> {

        float bDepth;
        float bSpriteSize = BUBBLES_START_SIZE;
        float[] bColor;
        float bColAlpha;
        float[] bPos;
        float[] bVel;
        int bSpriteType = BUBBLE_POINT;
        long bStartTime;
        boolean bUp;
        int bIndex;

        public BubblePoint(int index) {
            bPos = Vector.ZERO.clone();
            bVel = Vector.ZERO.clone();
            bColor = SLHelper.BLK_COLOR_VEC;
            bColAlpha = 0.0f;
            bUp = false;
            bIndex = index;
        }

        public void setDepth(float depth) {
            bDepth = depth;
        }

        public float getDepth() {
            return bDepth;
        }

        public float getSpriteSize() {
            return bSpriteSize;
        }

        public float[] getCol() {
            return bColor;
        }

        public float[] getDrawPos() {
            return bPos;
        }

        public BubblePoint setPos(float[] pos) {
            bPos = pos;
            return this;
        }

        public BubblePoint setVel(float[] vel) {
            bVel = vel;
            return this;
        }

        public BubblePoint setStartTime (long time) {
            bStartTime = time;
            return this;
        }

        public void incStartTime (long time) {
            if (bUp) bStartTime += time;
        }

        public BubblePoint setSize (float size) {
            bSpriteSize = size;
            return this;
        }

        public BubblePoint setColor (float[] color) {
            bColor = color.clone();
            bColAlpha = color[3];
            return this;
        }

        public void setUp (boolean up) {
            bUp = up;
        }

        public int getSpriteType() {
            return bSpriteType;
        }

        @Override
        public int compareTo(OrderedSprite another) {
            return Float.compare(another.getDepth(), this.getDepth());
        }

        protected void perFrameUpdate(float frameFactor) {
            bPos = Vector.sumVV(bPos, Vector.scaleSV(frameFactor, bVel));
            bVel = Vector.sumVV(bVel, Vector.randomV(BUBBLE_RND_FACTOR));
            long timeSinceStart = mThisFrameStartTime - bStartTime;
            float timeFactor = Math.min(1.0f, (float) timeSinceStart / (float) bubbles_duration);
            if (timeFactor >= 1.0f) bUp = false;
            bSpriteSize = BUBBLES_START_SIZE + bubbles_delta_size * timeFactor;
            // Fade bubbles away as they are replaced during generation.
            if (mBubblesGenerating) {
                int lastNewBubInd = mNewBubbleIndex - 1;
                if (mNewBubbleIndex == 0) lastNewBubInd = bubbles_count - 1;
                int newBubIndexDelta = bubbles_count - lastNewBubInd;
                float alphaFact = 2.0f;
                if (newBubIndexDelta >= bub_fade_num) {
                    if (bIndex > lastNewBubInd && bIndex < lastNewBubInd + bub_fade_num) {
                        alphaFact = (float) (bIndex - lastNewBubInd) / (float) bub_fade_num;
                    }
                } else if (bIndex > lastNewBubInd) {
                    alphaFact = (float) (bIndex - lastNewBubInd) / (float) bub_fade_num;
                } else if (bIndex < bub_fade_num - newBubIndexDelta) {
                    alphaFact = (float) (bIndex + newBubIndexDelta) / (float) bub_fade_num;
                }
                if (alphaFact <= 1.0f)
                    bColAlpha = bubble_min_color[3] * alphaFact;
            }
            bColor[3] = bColAlpha * (1.0f - timeFactor);
        }
    }

    // Class for fireworks sparkle points.
    protected class SparklePoint implements OrderedSprite, Comparable<OrderedSprite> {

        float spDepth;
        float[] spColor;
        float[] spPos;
        float[] spVel;
        int spSpriteType = SPARKLE_POINT;
        int spCrackleOffset;
        int spCracklePace;
        boolean spDraw;

        public SparklePoint() {
            spPos = Vector.ZERO.clone();
            spVel = Vector.ZERO.clone();
            spColor = sparkle_color.clone();
            spDraw = true;
            spCrackleOffset = (int) (12.0 * Math.random());
            spCracklePace = 6 + (int) (4.0 * Math.random());
        }

        @Override
        public int compareTo(OrderedSprite another) {
            return Float.compare(another.getDepth(), this.getDepth());
        }

        @Override
        public void setDepth(float depth) {
            spDepth = depth;
        }

        @Override
        public float getDepth() {
            return spDepth;
        }

        @Override
        public float getSpriteSize() {
            return mSparkleSpriteSize;
        }

        @Override
        public float[] getDrawPos() {
            return spPos;
        }

        @Override
        public float[] getCol() {
            return spColor;
        }

        @Override
        public int getSpriteType() {
            return spSpriteType;
        }

        public SparklePoint setPos(float[] pos) {
            spPos = pos.clone();
            return this;
        }

        public void setVel(float[] vel) {
            spVel = vel.clone();
        }

        protected void perFrameUpdate(float frameFactor) {
            float rndFactor = SPARKLE_RND_FACTOR * frameFactor;
            spPos = Vector.sumVV(spPos, Vector.scaleSV(frameFactor, spVel));
            spVel = Vector.sumVV(spVel, Vector.randomV(rndFactor));
            if (mCrackling && (mSparkleFrameCounter + spCrackleOffset) % spCracklePace == 0)
                    spDraw = !spDraw;
            spColor[3] = spDraw ? mSpAlpha : 0.15f * mSpAlpha;
        }

    }

    protected class SmokePoint implements OrderedSprite, Comparable<OrderedSprite> {

        float smDepth;
        float smSpriteSize = SMOKE_START_SIZE;
        float[] smColor = smoke_min_color;
        float[] smPos;
        float[] smVel;
        int smSpriteType;

        public SmokePoint(float[] pos, float[] vel) {
            smPos = pos;
            smVel = vel;
            smSpriteType = rolling_smoke_sprite_type;
            rolling_smoke_sprite_type++;
            if (rolling_smoke_sprite_type > 8) rolling_smoke_sprite_type = 5;
        }

        @Override
        public int compareTo(OrderedSprite another) {
            return Float.compare(another.getDepth(), this.getDepth());
        }

        @Override
        public void setDepth(float depth) {
            smDepth = depth;
        }

        @Override
        public float getDepth() {
            return smDepth;
        }

        @Override
        public float getSpriteSize() {
            return smSpriteSize;
        }

        @Override
        public float[] getDrawPos() {
            return smPos;
        }

        @Override
        public float[] getCol() {
            return smColor;
        }

        @Override
        public int getSpriteType() {
            return smSpriteType;
        }

        protected void perFrameUpdate(float frameFactor) {
            float rndFactor = SMOKE_RND_FACTOR * frameFactor;
            smPos = Vector.sumVV(smPos, Vector.scaleSV(frameFactor, smVel));
            smVel = Vector.sumVV(smVel, Vector.randomV(rndFactor));
            float timeFactor = (float)(mThisFrameStartTime - mSmokingStartTime) / (float) SMOKE_DURATION;
            smSpriteSize = SMOKE_START_SIZE + SMOKE_DELTA_SIZE * timeFactor;
            smColor = Vector.multVV(smoke_min_color, new float[] {1.0f, 1.0f, 1.0f, 1.0f - timeFactor});
        }
    }

    // The class constructor.
    public SLSimRenderer(Context ctx) {
        mContext = ctx;
        mSimStartTime = System.nanoTime();
        mSplashUp = true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // Change default depth test to less than or equal to solve 'flickering' issues with
        // OrderedSprites in low-bit depth buffer devices.
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        // Set actual default for visuals level if the dummy setting was loaded off prefs.
        // Determine the default depending on device's OpenGL supported version.
        String glVersionString = GLES20.glGetString(GLES20.GL_VERSION);
        if (mVisualsLevel == DUMMY_DEFAULT_VIS) {
            if (glVersionString == null) {
                mVisualsLevel = LOW_VISUALS;
            } else if (glVersionString.length() < 11) {
                mVisualsLevel = LOW_VISUALS;
            } else {
                String versionNumString = String.valueOf(glVersionString.charAt(10));
                try {
                    int versionNum = Integer.valueOf(versionNumString);
                    mVisualsLevel = versionNum > 2 ? MED_VISUALS : LOW_VISUALS;
                } catch (NumberFormatException e) {
                    mVisualsLevel = LOW_VISUALS;
                }
            }
        }
        mIsHighVisuals = mVisualsLevel == HIGH_VISUALS;
        mIsMedVisuals = mVisualsLevel == MED_VISUALS;
        mIsLowVisuals = mVisualsLevel == LOW_VISUALS;
         // Determine depth buffer bits for boolean which selects size of the gl far plane.
        int[] bitDepth = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_DEPTH_BITS, bitDepth, 0);
        mIsDepthBufferLowBit = bitDepth[0] < 24;
        // Load shader code.
        final String shapeVertexShader = SLHelper.loadShaderCode(mContext,
                R.raw.shape_vertex_shader_low);
        final String shapeFragmentShader = SLHelper.loadShaderCode(mContext,
                R.raw.shape_fragment_shader_low);
        final String spriteVertexShader = SLHelper.loadShaderCode(mContext,
                R.raw.sprite_vertex_shader);
        final String spriteFragmentShader;
        if (mIsHighVisuals) {
            spriteFragmentShader = SLHelper.loadShaderCode(mContext,
                    R.raw.sprite_fragment_shader_high);
        } else if (mIsMedVisuals) {
            spriteFragmentShader = SLHelper.loadShaderCode(mContext,
                    R.raw.sprite_fragment_shader_med);
        } else {
            spriteFragmentShader = SLHelper.loadShaderCode(mContext,
                    R.raw.sprite_fragment_shader_low);
        }
        final String flatVertexShader = SLHelper.loadShaderCode(mContext,
                R.raw.flat_vertex_shader);
        final String flatFragmentShader = SLHelper.loadShaderCode(mContext,
                R.raw.flat_fragment_shader);
        final String guiVertexShader = SLHelper.loadShaderCode(mContext,
                R.raw.gui_vertex_shader);
        final String guiFragmentShader = SLHelper.loadShaderCode(mContext,
                R.raw.gui_fragment_shader);
        final String flareSurfVertexShader = SLHelper.loadShaderCode(mContext,
                R.raw.flaresurf_vertex_shader);
        final String flareSurfFragShader = SLHelper.loadShaderCode(mContext,
                R.raw.flaresurf_fragment_shader);
        final String sparkleFlareFragShader = SLHelper.loadShaderCode(mContext,
                R.raw.sparkleflare_fragment_shader);
        // Compile the shaders and get the handles.
        final int shapeVertexShaderHandle = SLHelper.compileShader(GLES20.GL_VERTEX_SHADER,
                shapeVertexShader);
        final int shapeFragmentShaderHandle = SLHelper.compileShader(GLES20.GL_FRAGMENT_SHADER,
                shapeFragmentShader);
        final int spriteVertShaderHandle = SLHelper.compileShader(GLES20.GL_VERTEX_SHADER,
                spriteVertexShader);
        final int spriteFragShaderHandle = SLHelper.compileShader(GLES20.GL_FRAGMENT_SHADER,
                spriteFragmentShader);
        final int flatVertShaderHandle = SLHelper.compileShader(GLES20.GL_VERTEX_SHADER,
                flatVertexShader);
        final int flatFragShaderHandle = SLHelper.compileShader(GLES20.GL_FRAGMENT_SHADER,
                flatFragmentShader);
        final int guiVertShaderHandle = SLHelper.compileShader(GLES20.GL_VERTEX_SHADER,
                guiVertexShader);
        final int guiFragShaderHandle = SLHelper.compileShader(GLES20.GL_FRAGMENT_SHADER,
                guiFragmentShader);
        final int flareSurfVertShadHandle = SLHelper.compileShader(GLES20.GL_VERTEX_SHADER,
                flareSurfVertexShader);
        final int flareSurfFragShadHandle = SLHelper.compileShader(GLES20.GL_FRAGMENT_SHADER,
                flareSurfFragShader);
        final int sparkleFlareFragShadHandle = SLHelper.compileShader(GLES20.GL_FRAGMENT_SHADER,
                sparkleFlareFragShader);
        // Create the programs and get the handles.
        mShapeProgramHandle = SLHelper.createAndLinkProgram(
                shapeVertexShaderHandle,
                shapeFragmentShaderHandle,
                new String[] {"a_Position", "a_Normal", "a_Center",
                        "a_TexCoordinate0",
                        "a_TexCoordinate1",
                        "a_TexCoordinate2" }
        );
        mSpriteProgramHandle = SLHelper.createAndLinkProgram(
                spriteVertShaderHandle,
                spriteFragShaderHandle,
                new String[]{"a_Position", "a_Color", "a_BaseSize", "a_Type"}
        );
        mFlatProgramHandle = SLHelper.createAndLinkProgram(
                flatVertShaderHandle,
                flatFragShaderHandle,
                new String[]{"a_Position", "a_Color"}
        );
        mUIProgramHandle = SLHelper.createAndLinkProgram(
                guiVertShaderHandle,
                guiFragShaderHandle,
                new String[]{"a_Position", "a_Color", "a_TexCoordinate"}
        );
        mFlareSurfProgramHandle = SLHelper.createAndLinkProgram(
                flareSurfVertShadHandle,
                flareSurfFragShadHandle,
                new String[]{"a_Position"}
        );
        mSparkleFlareProgHandle = SLHelper.createAndLinkProgram(
                flareSurfVertShadHandle,
                sparkleFlareFragShadHandle,
                new String[]{"a_Position"}
        );
        // Load the textures and get the handles.
        mMarbleTextureDataHandle = SLHelper.loadTexture(mContext, R.drawable.marble_02);
        mStarMarbleTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.star_on_marble_05);
        mRoseMarbleTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.rose_on_marble_04);
        mEyeMarbleTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.eye_on_marble_01);
        mButterflyTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.bfly_on_marble_02);
        mFishMarbleTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.fish_on_marble_01);
        mSpriteTexAtlasDataHandle = SLHelper.loadTexture(mContext,
                mIsLowVisuals ? R.drawable.sprite_atlas_16 : R.drawable.sprite_atlas_15);
        mLensFlareTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.light_flare_03);
        mSoftCircTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_soft_circle_02);
        // UI icons/graphics.
        mHorizIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_horizontal_256);
        mVertIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_vertical_256);
        mLockedIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_lock_256);
        mUnlockedIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_lock_open_256);
        mResetIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_reset_256);
        mHelpIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_help_256);
        mGravCircIconTexDataHandle =
                SLHelper.loadTexture(mContext, R.drawable.ic_gravity_circle2_256);
        mCubeIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_cube2_256);
        mIcosIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_icos_256);
        mRnddodecIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_rnd_dodec_256);
        mRndicosIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_rnd_icos_256);
        mLightIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_light_256_2);
        mLightIconBrTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_light_256_br_2);
        mEmitterIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_bubbles_256);
        mMusicIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_music_256);
        mMenuIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_menu_2_256);
        mPauseIconTexDataHandle = SLHelper.loadTexture(mContext, R.drawable.ic_pause_256);
        mEyeControlRightTexDataHandle =
                SLHelper.loadTexture(mContext, R.drawable.eye_control_right_02);
        mEyeControlLeftTexDataHandle =
                SLHelper.loadTexture(mContext, R.drawable.eye_control_left_02);
        // Set up LightPoint static params.
        LightPoint.high_settings = mIsHighVisuals;
        if (mIsLowVisuals) {
            LightPoint.base_sprite_size = LightPoint.BASE_SPRITE_SIZE * 23.1f;
        } else if (mIsMedVisuals) {
            LightPoint.base_sprite_size = LightPoint.BASE_SPRITE_SIZE * 37.8f;
        } else {
            LightPoint.base_sprite_size = LightPoint.BASE_SPRITE_SIZE * 3.0f;
        }
        // Set params according to visuals settings.
        switch (mVisualsLevel) {
            case HIGH_VISUALS:
                dust_count = 320;
                bubbles_count = 180;
                bub_fade_num = 22;
                bubbles_delta_size = 10.0f;
                bubbles_duration = 40 * NANOS_PER_SECOND;
                sparkle_count = 8;
                sparkle_start_size = 4.2f;
                sparkle_delta_size = 7.5f;
                sparkles_duration = 7 * NANOS_PER_SECOND;
                smoke_count = 3;
                break;
            case MED_VISUALS:
                dust_count = 320;
                bubbles_count = 180;
                bub_fade_num = 22;
                bubbles_delta_size = 10.0f;
                bubbles_duration = 40 * NANOS_PER_SECOND;
                sparkle_count = 8;
                sparkle_start_size = 4.2f;
                sparkle_delta_size = 7.5f;
                sparkles_duration = 7 * NANOS_PER_SECOND;
                smoke_count = 2;
                break;
            case LOW_VISUALS:
                dust_count = 190;
                bubbles_count = 90;
                bub_fade_num = 11;
                bubbles_delta_size = 4.5f;
                bubbles_duration = 30 * NANOS_PER_SECOND;
                sparkle_count = 6;
                sparkle_start_size = 2.5f;
                sparkle_delta_size = 4.0f;
                sparkles_duration = 6500 * NANOS_PER_MILLI;
                smoke_count = 0;
        }
        // Generate random dust field array.
        mDustPoints = new DustPoint[dust_count];
        float x; float y; float z;
        float rad; float theta; float phi;
        for (int i = 0; i < dust_count; i++) {
            rad = 6.0f + DUST_RANGE * (float) Math.random();
            theta = 2 * PI * (float) Math.random();
            phi = (PI * 0.5f ) * 1.0f * ((float) Math.random() - 0.5f);
            x = rad * (float) (Math.cos(phi) * Math.cos(theta));
            z = -rad * (float) (Math.cos(phi) * Math.sin(theta));
            y = rad * (float) Math.sin(phi);
            mDustPoints[i] = new DustPoint(new float[] {x, y, z});
        }
        // Set emitter counts and initialize.
        mBubblePoints = new BubblePoint[bubbles_count];
        for (int i = 0; i < bubbles_count; i++) {
            mBubblePoints[i] = new BubblePoint(i);
        }
        if (mBubblesGenerating) {
            // Initialize bubble generation
            mBubblesUp = true;
            mNewBubbleIndex = 0;
            mShape.initBubbleEmitInterpolation();
        } else {
            mBubblesUp = false;
        }
        mBubblesEmittedCount = 0;
        mNewBubbleIndex = 0;
        mSparkles = new SparklePoint[8][sparkle_count];
        mSparklesGenerating = false;
        mSparkling = false;
        mSparklesEmittedCount = 0;
        mSparkleColorFuncX = -5.0f;
        mCrackling = false;
        mCracklePlayed = false;
        mSparkleLightPos1 = Vector.ZERO.clone();
        mSparkleLightCol = Vector.ZERO4.clone();
        sparkle_color = Vector.ZERO4.clone();
        mSmokePuffs  = new SmokePoint[smoke_count];
        mSmoking = false;
        mSmokeEmittedCount = 0;
        rolling_smoke_sprite_type = 5 + (int)(4.0f * (float)Math.random());
        // Initialize framerate params.
//        mFrameRate = 60;
//        mFrameCounterTime = 0;
//        mFrameCounter = 0;
        mPrevFrameStartTime = 0;
        mPrevFrameEndTime = 0;
        mDrawUI = true;
        // Init help mode params.
        mHelpOn = false;
        mHelpColorOn = true;
        mHelpColorCycled = false;
        // Init pop-up menu booleans.
        mShapeMenuOn = false;
        mLight0MenuOn = false;
        mLight1MenuOn = false;
        mLight2MenuOn = false;
        // Init screen edge warning booleans.
        mOnScrnEdgeRt = false;
        mOnScrnEdgeLft = false;
        // Load music track and performance array data.
        int musResID; float vol; int rotData; int bubData; int sparkData;
        switch (mMusicTrack) {
            case BLUE_DANUBE:
                musResID = R.raw.mus_blue_danube_3;
                vol = 0.95f;
                mPerfAttenuation = 0.7f;
                rotData = R.array.rotation_data_bd;
                bubData = R.array.bubbler_data_bd;
                sparkData = R.array.sparkler_times_bd;
                break;
            case MIDNIGHT_CITY:
                musResID = R.raw.mus_midnight_city_2;
                vol = 0.60f;
                mPerfAttenuation = 0.9f;
                rotData = R.array.rotation_data_mc;
                bubData = R.array.bubbler_data_mc;
                sparkData = R.array.sparkler_times_mc;
                break;
            case FANTASY_ICE:
                musResID = R.raw.mus_fantasy_on_ice_2;
                vol = 0.50f;
                mPerfAttenuation = 0.8f;
                rotData = R.array.rotation_data_foi;
                bubData = R.array.bubbler_data_foi;
                sparkData = R.array.sparkler_times_foi;
                break;
            case TIME_REFLECTION:
                musResID = R.raw.mus_time_of_reflection_2;
                vol = 0.60f;
                mPerfAttenuation = 0.9f;
                rotData = R.array.rotation_data_tor;
                bubData = R.array.bubbler_data_tor;
                sparkData = R.array.sparkler_times_tor;
                break;
            case SUNRISE_WITHOUT:
                musResID = R.raw.mus_sunrise_without_you_2;
                vol = 0.70f;
                mPerfAttenuation = 0.6f;
                rotData = R.array.rotation_data_swy;
                bubData = R.array.bubbler_data_swy;
                sparkData = R.array.sparkler_times_swy;
                break;
            case BEAUTIFUL_SIL:
                musResID = R.raw.mus_beautiful_silence_2;
                vol = 0.60f;
                mPerfAttenuation = 0.5f;
                rotData = R.array.rotation_data_sil;
                bubData = R.array.bubbler_data_sil;
                sparkData = R.array.sparkler_times_sil;
                break;
            default:
                musResID = R.raw.mus_streetlights_people_2;
                vol = 0.60f;
                mPerfAttenuation = 0.9f;
                rotData = R.array.rotation_data_slp;
                bubData = R.array.bubbler_data_slp;
                sparkData = R.array.sparkler_times_slp;
        }
        mMainLauncher.createMusicPlayer(musResID, vol);
        mMusicPlaying = false;
        mPerfRotationData = mResGetter.getIntArrayResource(rotData);
        mPerfBubblerData = mResGetter.getIntArrayResource(bubData);
        mPerfSparklerTimes = mResGetter.getIntArrayResource(sparkData);
        // Initialize position data for UI axis display.
        mXaxisPositions = ByteBuffer.allocateDirect(6 * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mXaxisPositions.put(new float[] {-0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f}).position(0);
        mYaxisPositions = ByteBuffer.allocateDirect(6 * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mYaxisPositions.put(new float[] {-0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f}).position(0);
        mZaxisPositions = ByteBuffer.allocateDirect(6 * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mZaxisPositions.put(new float[] {-0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f}).position(0);
        float[] axisPolygonPoints = {
                -0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f, 0.5f,

                -0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f, 0.5f,
                -0.5f, 0.5f, -0.5f,

                -0.5f, -0.5f, -0.5f,
                -0.5f, 0.5f, -0.5f,
                0.5f, -0.5f, -0.5f
        };
        mAxisPolygonPositions = ByteBuffer.allocateDirect(
                axisPolygonPoints.length * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mAxisPolygonPositions.put(axisPolygonPoints).position(0);
        // Initialize view matrix for UI drawing.
        Matrix.setLookAtM(mUIViewMatrix, 0, 0.0f, 0.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        // Performance mode param initialization.
        mPerforming = false;
        mPerfPaused = false;
        mCracklePaused = false;
    }


    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);
//        GLES20.glDepthRangef(0.0f, 1.0f);
        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / (float) height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        far_plane = mIsDepthBufferLowBit ? FAR_PLANE_LOW_BIT_BUFFER : FAR_PLANE_NORMAL_BUFFER;
        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, NEAR_CLIP, far_plane);
        float[] orthoMatrix = new float[16];
        Matrix.orthoM(orthoMatrix, 0, 0.0f, width, 0.0f, height, 1.0f, 3.0f);
        Matrix.multiplyMM(mUIMVPMatrix, 0, orthoMatrix, 0, mUIViewMatrix, 0);
        mWidth = width;
        mHeight = height;
        mBaseGrabTolerance = height * 7.0f;
        // Set scale factors for eye move & slew controls.
        float factor;
        switch (mEyeSensitivy) {
            case LOW_SENSITIVITY:
                factor = 1.0f / EYE_SENS_FACTOR;
                break;
            case HIGH_SENSITIVITY:
                factor = EYE_SENS_FACTOR;
                break;
            default:
                factor = 1.0f;
        }
        Eye.setControlScales(5.0f * factor / height, 6.0f * factor / height);
        // Set up default button size params.
        mButtonSize = height * 0.15f;
        mEyeControlWidth = width * 0.27f;
        mEyeControlHeight = height * 0.4f;
        mButtonMargin = mButtonSize * 0.3f;
        float size = (height - mEyeControlHeight - (mButtonMargin * 2)) / 4.0f;
        if (mButtonSize > size) mButtonSize = size;
        float eyeControlWidth = mEyeControlWidth - mButtonMargin;
        float eyeControlHeight = mEyeControlHeight - mButtonMargin;
        // Init viewport params for the view direction indicator axes.
        mAxisViewportX = (int) (mWidth - mEyeControlWidth - (0.5f * mButtonMargin)
                - 0.5f * mButtonSize * (ratio + 1.0f));
        mAxisViewportY = (int) mButtonMargin;
        mAxisViewportH = (int) mButtonSize;
        mAxisViewportW = (int) (ratio * mButtonSize);
        // Set axes line width.
        float[] lineWidthRange = new float[2];
        GLES20.glGetFloatv(GLES20.GL_ALIASED_LINE_WIDTH_RANGE, lineWidthRange, 0);
        if (lineWidthRange[1] < 1.0f) lineWidthRange[1] = 1.0f;
        float defaultWidth = is_high_dp ? 0.0033f * width : 0.0038f * width;
        float axesLineWidth = Math.min(defaultWidth, lineWidthRange[1]);
        GLES20.glLineWidth(axesLineWidth);
        // Initialize UI buttons.
        mEyeLockB = new SLButton(
                mButtonMargin,
                height - mButtonMargin - mButtonSize * 4,
                mButtonSize,
                mButtonSize,
                SLHelper.DEF_BUTTON_COL,
                SLHelper.PRESSED_BUTTON_COL,
                mUnlockedIconTexDataHandle
        );
        mEmitterB = new SLButton(
                width - mButtonMargin - mButtonSize,
                mEyeControlHeight + mButtonMargin + mButtonSize,
                mButtonSize,
                mButtonSize,
                SLHelper.DEF_BUTTON_COL,
                SLHelper.PRESSED_BUTTON_COL,
                mEmitterIconTexDataHandle
        );
        mMusicB = new SLButton(
                width - mButtonMargin - mButtonSize,
                mEyeControlHeight + mButtonMargin + (mButtonSize * 2.0f),
                mButtonSize,
                mButtonSize,
                SLHelper.DEF_BUTTON_COL,
                SLHelper.PRESSED_BUTTON_COL,
                mMusicIconTexDataHandle
        );
        mEyeMoveModeB = new SLButton(
                width - mButtonMargin - mButtonSize,
                mEyeControlHeight + mButtonMargin,
                mButtonSize,
                mButtonSize,
                SLHelper.DEF_BUTTON_COL,
                SLHelper.PRESSED_BUTTON_COL,
                mHorizIconTexDataHandle
        );
        mShapeB = new SLButton(
                mButtonMargin,
                height - mButtonMargin - mButtonSize * 2,
                mButtonSize,
                mButtonSize,
                Vector.scaleSV(0.5f, SLHelper.WHI_COLOR_VEC),
                Vector.scaleSV(1.0f, SLHelper.WHI_COLOR_VEC),
                mCubeIconTexDataHandle
        );
        mCubeB = new SLButton(
                mButtonMargin + mButtonSize,
                height - mButtonMargin - mButtonSize * 2,
                mButtonSize,
                mButtonSize,
                Vector.scaleSV(0.33f, SLHelper.WHI_COLOR_VEC),
                Vector.scaleSV(1.0f, SLHelper.WHI_COLOR_VEC),
                mCubeIconTexDataHandle
        );
        mRndDodecB = new SLButton(
                mButtonMargin + mButtonSize * 2,
                height - mButtonMargin - mButtonSize * 2,
                mButtonSize,
                mButtonSize,
                Vector.scaleSV(0.33f, SLHelper.WHI_COLOR_VEC),
                Vector.scaleSV(1.0f, SLHelper.WHI_COLOR_VEC),
                mRnddodecIconTexDataHandle
        );
        mIcosB = new SLButton(
                mButtonMargin + mButtonSize * 3,
                height - mButtonMargin - mButtonSize * 2,
                mButtonSize,
                mButtonSize,
                Vector.scaleSV(0.33f, SLHelper.WHI_COLOR_VEC),
                Vector.scaleSV(1.0f, SLHelper.WHI_COLOR_VEC),
                mIcosIconTexDataHandle
        );
        mRndIcosB = new SLButton(
                mButtonMargin + mButtonSize * 4,
                height - mButtonMargin - mButtonSize * 2,
                mButtonSize,
                mButtonSize,
                Vector.scaleSV(0.33f, SLHelper.WHI_COLOR_VEC),
                Vector.scaleSV(1.0f, SLHelper.WHI_COLOR_VEC),
                mRndicosIconTexDataHandle
        );
        mLight0B = new SLButton(
                mButtonMargin,
                height - mButtonMargin - mButtonSize * 2,
                mButtonSize,
                mButtonSize,
                Vector.scaleSV(0.5f, mLights[0].mColorVec),
                Vector.scaleSV(1.0f, mLights[0].mColorVec),
                mLights[0].mBright ? mLightIconBrTexDataHandle : mLightIconTexDataHandle
        );
        mLight1B = new SLButton(
                mButtonMargin,
                height - mButtonMargin - mButtonSize * 2,
                mButtonSize,
                mButtonSize,
                Vector.scaleSV(0.5f, mLights[1].mColorVec),
                Vector.scaleSV(1.0f, mLights[1].mColorVec),
                mLights[1].mBright ? mLightIconBrTexDataHandle : mLightIconTexDataHandle
        );
        mLight2B = new SLButton(
                mButtonMargin,
                height - mButtonMargin - mButtonSize * 2,
                mButtonSize,
                mButtonSize,
                Vector.scaleSV(0.5f, mLights[2].mColorVec),
                Vector.scaleSV(1.0f, mLights[2].mColorVec),
                mLights[2].mBright ? mLightIconBrTexDataHandle : mLightIconTexDataHandle
        );
        initLightColorButtons(mLight0B);
        mGravCircB = new SLButton(
                mButtonMargin,
                height - mButtonMargin - mButtonSize * 2,
                mButtonSize,
                mButtonSize,
                SLHelper.DEF_BUTTON_COL,
                SLHelper.PRESSED_BUTTON_COL,
                mGravCircIconTexDataHandle
        );
        mEyeControlRight = new SLButton(
                width - mButtonMargin - eyeControlWidth,
                mButtonMargin,
                eyeControlWidth,
                eyeControlHeight,
                EYE_CNTRL_COLOR,
                EYE_CNTRL_PRESSED,
                mEyeControlRightTexDataHandle
        );
        mEyeControlLeft = new SLButton(
                mButtonMargin,
                mButtonMargin,
                eyeControlWidth,
                eyeControlHeight,
                EYE_CNTRL_COLOR,
                EYE_CNTRL_PRESSED,
                mEyeControlLeftTexDataHandle
        );
        mPauseB = new SLButton(
                mEyeControlWidth + (0.5f * mButtonMargin),
                mButtonMargin,
                mButtonSize,
                mButtonSize,
                SLHelper.DEF_BUTTON_COL,
                SLHelper.PRESSED_BUTTON_COL,
                mPauseIconTexDataHandle
        );
        mResetB = new SLButton(
                mButtonMargin,
                height - mButtonMargin - mButtonSize * 3,
                mButtonSize,
                mButtonSize,
                SLHelper.DEF_BUTTON_COL,
                SLHelper.PRESSED_BUTTON_COL,
                mResetIconTexDataHandle
        );
        mHelpInfoB = new SLButton(
                width - mButtonMargin - mButtonSize,
                height - mButtonMargin - mButtonSize,
                mButtonSize,
                mButtonSize,
                SLHelper.DEF_BUTTON_COL,
                SLHelper.PRESSED_BUTTON_COL,
                mHelpIconTexDataHandle
        );
        mHelpInfoB.mIsHelpButton = true;
        mMenuB = new SLButton(
                mButtonMargin,
                height - mButtonMargin - mButtonSize,
                mButtonSize,
                mButtonSize,
                SLHelper.DEF_BUTTON_COL,
                SLHelper.PRESSED_BUTTON_COL,
                mMenuIconTexDataHandle
        );
        float manHelpBWidth = width - (mEyeControlWidth * 2.0f);
        mObjManipHelpB = new SLButton(
                (width - manHelpBWidth) / 2.0f,
                (height - manHelpBWidth) / 2.0f + mButtonSize,
                manHelpBWidth,
                manHelpBWidth,
                SLHelper.BLK_COLOR_VEC,
                SLHelper.BLK_COLOR_VEC,
                mLensFlareTexDataHandle
        );
        mObjManipHelpB.setEnabled(false);
        mOrientAxesB = new SLButton(
                mWidth - mEyeControlWidth - (0.5f * mButtonMargin) - mButtonSize,
                mButtonMargin,
                mButtonSize,
                mButtonSize,
                SLHelper.BLK_COLOR_VEC,
                SLHelper.BLK_COLOR_VEC,
                mLensFlareTexDataHandle
        );
        float controlBWidth = mButtonSize * 3.81f;
        mEyeControlPosRight = new SLButton(
                width - controlBWidth,
                0.0f,
                controlBWidth,
                controlBWidth,
                SLHelper.DEF_BUTTON_COL,
                EYE_CNTRL_PRESSED,
                mLensFlareTexDataHandle
        );
        mEyeControlPosLeft = new SLButton(
                0.0f,
                0.0f,
                controlBWidth,
                controlBWidth,
                SLHelper.DEF_BUTTON_COL,
                EYE_CNTRL_PRESSED,
                mLensFlareTexDataHandle
        );
        // "Hack" to give the eye control touch location sprites a default "pressed" state.
        mEyeControlPosRight.mPressPointerID = 1;
        mEyeControlPosLeft.mPressPointerID = 1;
        // Use SLButton object to create surface covering the screen for flare shader effects.
        mFlareSurface = new SLButton(
                0.0f,
                0.0f,
                width,
                height,
                SLHelper.DEF_BUTTON_COL,
                SLHelper.PRESSED_BUTTON_COL,
                mLensFlareTexDataHandle
        );
        // Use SLButton object to create surface for sparkle flare in medium settings. Initial
        // dummy values for position and sizes since those params are set continuously at runtime.
        mSparkleFlareSurf = new SLButton(
                0.0f,
                0.0f,
                width,
                height,
                SLHelper.DEF_BUTTON_COL,
                SLHelper.PRESSED_BUTTON_COL,
                mLensFlareTexDataHandle
        );
    }


    // Method called by the SLSurfaceView to load in params from the SharedPreferences.
    public void resumeState(SharedPreferences prefs) {
        // Load visuals settings.
        mVisualsLevel = prefs.getInt(SLSurfaceView.VISUALS_LEVEL, DUMMY_DEFAULT_VIS);
        // Load music track.
        mMusicTrack = prefs.getInt(SLSurfaceView.MUSIC_TRACK, MIDNIGHT_CITY);
        perf_add_offset = prefs.getInt(SLSurfaceView.PERFORM_OFFSET_INT, 0);
        // Create shape, lights, and eye objects.
        mCtrRing = new GravityCircle(Vector.ZERO, Vector.ZERO);
        mMobRing = new GravityCircle(INIT_MOV_RING_POS, INIT_MOV_RING_VEL);
        mGravCircleCount = prefs.getInt(SLSurfaceView.GRAV_RING_COUNT, 2);
        mCtrRing.setSpringSq(prefs.getBoolean(SLSurfaceView.CTR_RING_SPRINGSQ, false));
        mMobRing.setSpringSq(prefs.getBoolean(SLSurfaceView.MOV_RING_SPRINGSQ, true));
        mCtrRing.setForceFactor(prefs.getFloat(SLSurfaceView.CTR_RING_STRENGTH, 2.0f));
        mMobRing.setForceFactor(prefs.getFloat(SLSurfaceView.MOV_RING_STRENGTH, 3.0f));
        mMobRing.restoreFromPrefs(prefs);
        mGravRings = mGravCircleCount > 1
                ? new GravityCircle[ ]{mCtrRing, mMobRing}
                : new GravityCircle[ ]{mCtrRing};
        mMobGravExclusive = prefs.getBoolean(SLSurfaceView.MOV_RING_EXCLUSIVE, false);
        mShape = new Shape(prefs);
        mShapeDecalFlag = prefs.getInt(SLSurfaceView.SHAPE_DECAL, STAR_DECAL);
        mEye = new Eye();
        mEye.restoreFromPrefs(prefs);
        mEyeSensitivy = prefs.getInt(SLSurfaceView.EYE_SENSITIVITY, MED_SENSITIVITY);
        mEyeAltLock = prefs.getBoolean(SLSurfaceView.EYE_ALT_LOCK, true);
        LightPoint.resetLightsOnCount();
        for (int i = 0; i < 3; i++) {
            mLights[i] = new LightPoint(i);
            mLights[i].restoreFromPrefs(prefs, i);
        }
        int lightsOnCount = LightPoint.getLightsOnCount();
        mOnLights = new LightPoint[lightsOnCount];
        if (lightsOnCount > 0) {
            boolean light0 = mLights[0].isOn();
            boolean light1 = mLights[1].isOn();
            boolean light2 = mLights[2].isOn();
            if (light0 && light1 && light2) {
                mOnLights[0] = mLights[0];
                mOnLights[1] = mLights[1];
                mOnLights[2] = mLights[2];
            } else if (light0 && light1) {
                mOnLights[0] = mLights[0];
                mOnLights[1] = mLights[1];
            } else if (light0 && light2) {
                mOnLights[0] = mLights[0];
                mOnLights[1] = mLights[2];
            } else if (light1 && light2) {
                mOnLights[0] = mLights[1];
                mOnLights[1] = mLights[2];
            } else if (light0) {
                mOnLights[0] = mLights[0];
            } else if (light1) {
                mOnLights[0] = mLights[1];
            } else if (light2) {
                mOnLights[0] = mLights[2];
            }
        }
        // Renderer boolean to let it know to execute the strobeLights method each frame.
        mStrobing = false;
        int strobingCount = 0;
        for (int i = 0; i < lightsOnCount; i++) {
            if (mOnLights[i].mStrobeEnabled) {
                mStrobing = true;
                strobingCount++;
            }
        }
        // Set strobe interval according to how many lights are strobing.
        if (strobingCount == 3) {
            mStrobeInterval = 120 * NANOS_PER_MILLI;
        } else if (strobingCount == 2) {
            mStrobeInterval = 140 * NANOS_PER_MILLI;
        } else if (strobingCount == 1) {
            mStrobeInterval = 200 * NANOS_PER_MILLI;
        }
        mStrobeCounter = 0;
        mStrobeCycled = false;
        // Set active mover to mShape initially.
        restoreActiveMover(prefs.getInt(SLSurfaceView.ACTIVE_MOVER, SHAPE));
        mPerformanceHidesUI = prefs.getBoolean(SLSurfaceView.PERFORM_HIDE_UI, false);
        // Load bubbling state.
        mBubblesGenerating = prefs.getBoolean(SLSurfaceView.BUBBLES_GEN, false);
        // Load minimum background lighting level factor.
        mMinLightingFactor = prefs.getFloat(SLSurfaceView.MIN_LIGHT_FACTOR, MIN_LIGHT_FACTOR_3);
        float[] minFactorVec = {mMinLightingFactor, mMinLightingFactor, mMinLightingFactor, 1.0f};
        bubble_min_color = Vector.multVV(BUBBLE_BASE_COLOR, minFactorVec);
        smoke_min_color = Vector.multVV(SMOKE_BASE_COLOR, minFactorVec);
        mSimPaused = false;
    }

    @SuppressLint("ApplySharedPref")
    public void saveState(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        mShape.saveToPrefs(editor);
        mMobRing.saveToPrefs(editor);
        for (int i = 0; i < 3; i++) {
            mLights[i].saveToPrefs(editor, i);
        }
        mEye.saveToPrefs(editor);
        editor.putInt(SLSurfaceView.ACTIVE_MOVER, mActiveMoverFlag);
        editor.putInt(SLSurfaceView.VISUALS_LEVEL, mVisualsLevel);
        editor.putBoolean(SLSurfaceView.BUBBLES_GEN, mBubblesGenerating);
        editor.putInt(SLSurfaceView.EYE_SENSITIVITY, mEyeSensitivy);
        editor.commit();
    }

    protected void restoreActiveMover(int activeMover) {
        boolean light0 = mLights[0].isOn();
        boolean light1 = mLights[1].isOn();
        boolean light2 = mLights[2].isOn();
        boolean gravCirc = mGravCircleCount == 2;
        switch(activeMover) {
            case SHAPE:
                mActiveMoverFlag = SHAPE;
                mActiveMover = mShape;
                break;
            case LIGHT_0:
                if (light0) {
                    mActiveMoverFlag = LIGHT_0;
                    mActiveMover = mLights[0];
                } else if (light1) {
                    mActiveMoverFlag = LIGHT_1;
                    mActiveMover = mLights[1];
                } else if (light2) {
                    mActiveMoverFlag = LIGHT_2;
                    mActiveMover = mLights[2];
                } else if (gravCirc) {
                    mActiveMoverFlag = GRAV_CIRC;
                    mActiveMover = mMobRing;
                } else {
                    mActiveMoverFlag = SHAPE;
                    mActiveMover = mShape;
                }
                break;
            case LIGHT_1:
                if (light1) {
                    mActiveMoverFlag = LIGHT_1;
                    mActiveMover = mLights[1];
                } else if (light2) {
                    mActiveMoverFlag = LIGHT_2;
                    mActiveMover = mLights[2];
                } else if (light0) {
                    mActiveMoverFlag = LIGHT_0;
                    mActiveMover = mLights[0];
                } else if (gravCirc) {
                    mActiveMoverFlag = GRAV_CIRC;
                    mActiveMover = mMobRing;
                } else {
                    mActiveMoverFlag = SHAPE;
                    mActiveMover = mShape;
                }
                break;
            case LIGHT_2:
                if (light2) {
                    mActiveMoverFlag = LIGHT_2;
                    mActiveMover = mLights[2];
                } else if (light1) {
                    mActiveMoverFlag = LIGHT_1;
                    mActiveMover = mLights[1];
                } else if (light0) {
                    mActiveMoverFlag = LIGHT_0;
                    mActiveMover = mLights[0];
                } else if (gravCirc) {
                    mActiveMoverFlag = GRAV_CIRC;
                    mActiveMover = mMobRing;
                } else {
                    mActiveMoverFlag = SHAPE;
                    mActiveMover = mShape;
                }
                break;
            case GRAV_CIRC:
                if (gravCirc) {
                    mActiveMoverFlag = GRAV_CIRC;
                    mActiveMover = mMobRing;
                } else {
                    mActiveMoverFlag = SHAPE;
                    mActiveMover = mShape;
                }
                break;
            default:
                mActiveMoverFlag = SHAPE;
                mActiveMover = mShape;
        }
        if (mEye.mFocusedMoverFlag != mActiveMoverFlag) {
            mEye.mMoveFree = true;
            mEye.mSlewFree = true;
        }
    }

    // Set all active objects back to default positions and velocities.
    protected void defaultAllObjects() {
        mShape.defaultState();
        for (int i = 0; i < LightPoint.getLightsOnCount(); i++) {
            mOnLights[i].defaultState(i);
        }
        if (mGravCircleCount > 1) mMobRing.defaultState();
        mEye.defaultState();
    }


    /**
     * *****************************************************************************************
     * *********** The draw frame method which also executes the 3D space simulation ***********
     * *****************************************************************************************
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        mThisFrameStartTime = System.nanoTime();
        // Compute time factor to scale frame's time-dependent parameters.
        mFrametimeFactor = mPrevFrameStartTime == 0 ? 1.0f
                : (float) ((double) (mThisFrameStartTime - mPrevFrameStartTime) / FRAME_TIME);
        mPrevFrameStartTime = mThisFrameStartTime;
        // Frame rate counter, outputs FPS every 2 seconds in the log.
//        if (mThisFrameStartTime - mFrameCounterTime >= NANOS_PER_SECOND * 2) {
//            mFrameCounterTime = mThisFrameStartTime;
//            mFrameRate = mFrameCounter / 2;
//            Log.d(LOG_TAG, "FPS: " + mFrameRate);
//            mFrameCounter = 0;
//        }
//        mFrameCounter++;
        // Take down the splash screen after SPLASH_TIME has elapsed.
        if (mSplashUp) {
            if (mMainLauncher.isSplashTimeUp(mThisFrameStartTime)) {
                mMainLauncher.closeSplashScreen();
                mSplashUp = false;
            }
        }
        // Detect long-pressed buttons.
        if (mEmitterB.isPressed() && !mEmitterB.isLongPressRegistered() && !mHelpOn
                && (mThisFrameStartTime - mEmitterB.mPressedTime) > LONG_PRESS_TIME) {
            mEmitterB.registerLongPress();
            toggleBubbleGen();
        }
        if (mMusicB.isPressed() && !mMusicB.isLongPressRegistered() && !mHelpOn
                && (mThisFrameStartTime - mMusicB.mPressedTime) > LONG_PRESS_TIME) {
            mMusicB.registerLongPress();
            mMainLauncher.toggleMusic();
        }
        if (mEyeLockB.isPressed() && !mEyeLockB.isLongPressRegistered() && !mHelpOn
                && (mThisFrameStartTime - mEyeLockB.mPressedTime) > LONG_PRESS_TIME
                && !mSimPaused) {
            mEyeLockB.registerLongPress();
            mEye.mMoveFree = !mEye.mMoveFree;
            mEye.mInitChase = true;
            mEye.mMovHorizontal = true;
            if (mEye.mSlewFree) {
                mEye.mSlewFree = false;
                mEye.mLockTransitCount = 0;
                mEyeControlRight.unPress();
            }
            playUITick();
        }
        if (mShapeB.isPressed() && !mShapeB.isLongPressRegistered() && !mHelpOn
                && (mThisFrameStartTime - mShapeB.mPressedTime) > LONG_PRESS_TIME) {
            mShapeB.registerLongPress();
            mShapeMenuOn = true;
            mShapeMenuStart = mThisFrameStartTime;
        }
        if (mLight0B.isPressed() && !mLight0B.isLongPressRegistered() && !mHelpOn
                && (mThisFrameStartTime - mLight0B.mPressedTime) > LONG_PRESS_TIME) {
            mLight0B.registerLongPress();
            mLight0MenuOn = true;
            mLight0MenuStart = mThisFrameStartTime;
        }
        if (mLight1B.isPressed() && !mLight1B.isLongPressRegistered() && !mHelpOn
                && (mThisFrameStartTime - mLight1B.mPressedTime) > LONG_PRESS_TIME) {
            mLight1B.registerLongPress();
            mLight1MenuOn = true;
            mLight1MenuStart = mThisFrameStartTime;
        }
        if (mLight2B.isPressed() && !mLight2B.isLongPressRegistered() && !mHelpOn
                && (mThisFrameStartTime - mLight2B.mPressedTime) > LONG_PRESS_TIME) {
            mLight2B.registerLongPress();
            mLight2MenuOn = true;
            mLight2MenuStart = mThisFrameStartTime;
        }
        if (mGravCircB.isPressed() && !mGravCircB.isLongPressRegistered() && !mHelpOn
                && (mThisFrameStartTime - mGravCircB.mPressedTime) > LONG_PRESS_TIME) {
            mGravCircB.registerLongPress();
            mMobRing.setSpringSq(!mMobRing.isSpringSq());
            mMobRing.setForceFactor(mMobRing.gStrength);
            playUITick();
        }
        if (mMenuB.isPressed() && !mMenuB.isLongPressRegistered() && !mHelpOn
                && (mThisFrameStartTime - mMenuB.mPressedTime) > LONG_PRESS_TIME) {
            mMenuB.registerLongPress();
            enableUI(false);
        }
        if (mResetB.isPressed() && !mResetB.isLongPressRegistered() && !mHelpOn
                && (mThisFrameStartTime - mResetB.mPressedTime) > LONG_PRESS_TIME) {
            mResetB.registerLongPress();
            defaultAllObjects();
            playUITick();
        }
        // Update 3D World unless the sim is paused.
        if (!mSimPaused) {
            applyForce(mShape);
            mShape.perFrameUpdate(mFrametimeFactor);
            for (LightPoint light : mOnLights) {
                applyForce(light);
                light.perFrameUpdate(mFrametimeFactor);
            }
            if (mStrobing) strobeLights();
            mCtrRing.perFrameUpdate(mFrametimeFactor);
            if (mGravCircleCount == 2) {
                applyForce(mMobRing);
                mMobRing.perFrameUpdate(mFrametimeFactor);
            }
            if (mBubblesUp) bubbler(mFrametimeFactor);
            if (mSparkling) sparkler(mFrametimeFactor);
            if (mSmoking) smoker(mFrametimeFactor);
            if (mPerforming) performer();
        }
        for (LightPoint light : mOnLights) light.cycleOcclusionTest();
        if (mHelpOn) blinkHelpColor();
        // Set locked view point and eye positions if booleans call for them.
        if (!mEye.mSlewFree) {
            // Pass in the point to lock the viewpoint, adding an offset if the boolean calls for it.
            if (mEyeAltLock) {
                float[] gravPos;
                if (mGravCircleCount == 2 && mActiveMoverFlag != GRAV_CIRC) {
                    gravPos = mMobGravExclusive ? mMobRing.getPos()
                            : Vector.avgVArray(mMobRing.getPos(), mCtrRing.getPos());
                } else {
                    gravPos = mCtrRing.getPos();
                }
                float[] moverToGrav = Vector.diffVV(gravPos, mActiveMover.getPos());
                float dampedMoverToGravDist = Vector.length(moverToGrav) * 0.20f;
                float camDist = Vector.length(Vector.diffVV(mActiveMover.getPos(), mEye.mPos));
                float maxOffset = camDist * 0.40f;
                // Scale factor using equation to smoothly keep offset from exceeding distance from the
                // object to gravPos.
                float scaleF = dampedMoverToGravDist + maxOffset > 0.0f
                        ? dampedMoverToGravDist * maxOffset / (dampedMoverToGravDist + maxOffset)
                        : 0.0f;
                mEye.mLockViewPoint = Vector.sumVV(mActiveMover.getPos(),
                        Vector.scaleSV(scaleF, Vector.norm(moverToGrav)));
            } else {
                mEye.mLockViewPoint = mActiveMover.getPos();
            }
        }
        if (!mEye.mMoveFree) {
            // Pass in velocity of the locked object to use in Doppler calcs when in chase mode.
            mEye.mLockVelDoppler = mSimPaused ? Vector.ZERO.clone() : mActiveMover.getVel();
        }
        // Update the eye.
        mEye.perFrameUpdate(mFrametimeFactor);
        // Compute active object screen coordinates for touch interactions.
        Matrix.multiplyMM(mOrigMVPMat, 0, mProjectionMatrix, 0, mEye.mViewM, 0);
        setMoverScreenCoords();
        // Build the ordered sprite array for ordered rendering.
        buildSpriteArray();
        // Render the frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        renderShape();
        renderSprites();
        // Disable depth testing for light flare effects and UI rendering.
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        if (mIsHighVisuals) {
            setLightFlareVisibility();
            renderFlareSurface();
        }
        if (mIsMedVisuals && mSparkling) {
            renderSparklesFlare();
        }
        renderOrientAxes();
        // Re-establish full-screen viewport after orientAxis cropped it for proper UI location.
        GLES20.glViewport(0, 0, mWidth, mHeight);
        renderUI();
        // Re-enable depth testing for the next frame.
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // Frame-rate governor.
        long frameWaitTime = mPrevFrameEndTime == 0 ? 0
                : (long) FRAME_TIME - (System.nanoTime() - mPrevFrameEndTime);
        // Convert nanoseconds to milliseconds.
        frameWaitTime = (long) ((double) frameWaitTime * 0.000001d);
        if (frameWaitTime > 0) {
            try {
                Thread.sleep(frameWaitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        mPrevFrameEndTime = System.nanoTime();
    }

    private void buildSpriteArray() {
        // Build the SortedSprite array.
        int lightsOn = LightPoint.getLightsOnCount();
        mSpriteCount = mGravCircleCount + lightsOn + dust_count + mBubblesEmittedCount
                + (mSparklesEmittedCount * 8) + mSmokeEmittedCount;
        OrderedSprite[] sortedSprites = new OrderedSprite[mSpriteCount];
        // Copy in all the sprite arrays into the sortedSprites array.
        System.arraycopy(mDustPoints, 0, sortedSprites,
                0,
                dust_count);
        System.arraycopy(mBubblePoints, 0, sortedSprites,
                dust_count,
                mBubblesEmittedCount);
        for (int i = 0; i < 8; i++) {
            System.arraycopy(mSparkles[i], 0, sortedSprites,
                    dust_count + mBubblesEmittedCount + (mSparklesEmittedCount * i),
                    mSparklesEmittedCount);
        }
        System.arraycopy(mSmokePuffs, 0, sortedSprites,
                dust_count + mBubblesEmittedCount + (mSparklesEmittedCount * 8),
                mSmokeEmittedCount
                );
        System.arraycopy(mOnLights, 0, sortedSprites,
                dust_count + mBubblesEmittedCount + (mSparklesEmittedCount * 8)
                        + mSmokeEmittedCount,
                lightsOn);
        System.arraycopy(mGravRings, 0, sortedSprites,
                dust_count + mBubblesEmittedCount + (mSparklesEmittedCount * 8)
                        + mSmokeEmittedCount + lightsOn,
                mGravCircleCount);
        // Compute and set depth values, using eye space coords, then sort accordingly.
        float[] eyeSpaceCoords = new float[4];
        for (OrderedSprite sprite : sortedSprites) {
            Matrix.multiplyMV(eyeSpaceCoords, 0, mEye.mViewM, 0,
                    Vector.convertToVec4(sprite.getDrawPos()), 0);
            // Flip the sign since eye space z coords are negative in the view direction.
            sprite.setDepth(-eyeSpaceCoords[2]);
        }
        Arrays.sort(sortedSprites);
        // Build data arrays.
        float[] spriteData = new float[mSpriteCount * 9];
        float[] drawPos; float[] colorVec;
        for (int i = 0; i < mSpriteCount; i++) {
            drawPos = sortedSprites[i].getDrawPos();
            colorVec = sortedSprites[i].getCol();
            spriteData[i*3] = drawPos[0];
            spriteData[i*3+1] = drawPos[1];
            spriteData[i*3+2] = drawPos[2];
            spriteData[mSpriteCount*3 + (i*4)] = colorVec[0];
            spriteData[mSpriteCount*3 + (i*4) + 1] = colorVec[1];
            spriteData[mSpriteCount*3 + (i*4) + 2] = colorVec[2];
            spriteData[mSpriteCount*3 + (i*4) + 3] = colorVec[3];
            spriteData[mSpriteCount*7 + i] = mHeight * sortedSprites[i].getSpriteSize();
            spriteData[mSpriteCount*8 + i] = (float) sortedSprites[i].getSpriteType();
        }
        // Set up the sprite array float buffers for rendering.
        mSpritePositions = ByteBuffer.allocateDirect(mSpriteCount * 3 * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mSpritePositions.put(spriteData, 0, mSpriteCount * 3).position(0);
        mSpriteColors = ByteBuffer.allocateDirect(mSpriteCount * 4 * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mSpriteColors.put(spriteData, mSpriteCount * 3, mSpriteCount * 4).position(0);
        mSpriteSizes = ByteBuffer.allocateDirect(mSpriteCount * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mSpriteSizes.put(spriteData, mSpriteCount * 7, mSpriteCount).position(0);
        mSpriteTypes = ByteBuffer.allocateDirect(mSpriteCount * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mSpriteTypes.put(spriteData, mSpriteCount * 8, mSpriteCount).position(0);
    }

    private void renderShape() {
        // Set the program.
        GLES20.glUseProgram(mShapeProgramHandle);
        // Set program handles for shape rendering.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_MVPMatrix");
        mRotMatrixHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_RotMatrix");
        mModMatrixHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_ModelMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(mShapeProgramHandle, "a_Position");
        mNormalsHandle = GLES20.glGetAttribLocation(mShapeProgramHandle, "a_Normal");
        mCentersHandle = GLES20.glGetAttribLocation(mShapeProgramHandle, "a_Center");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_Texture0");
        mTextureUniformHandle2 = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_Texture1");
        mTextureCoordHandle0 = GLES20.glGetAttribLocation(mShapeProgramHandle, "a_TexCoordinate0");
        mTextureCoordHandle1 = GLES20.glGetAttribLocation(mShapeProgramHandle, "a_TexCoordinate1");
        mTextureCoordHandle2 = GLES20.glGetAttribLocation(mShapeProgramHandle, "a_TexCoordinate2");
        mLight00PosHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_Light0Pos");
        mLight01PosHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_Light1Pos");
        mLight02PosHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_Light2Pos");
        mLight00ColHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_Light0Col");
        mLight01ColHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_Light1Col");
        mLight02ColHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_Light2Col");
        mCircle00PosHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_Circ0Pos");
        mCircle01PosHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_Circ1Pos");
        mCircle00ColHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_Circ0Col");
        mCircle01ColHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_Circ1Col");
        mSparklePosHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_SparklePos");
        mSparkleColHandle = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_SparkleCol");
        int minLightFactor = GLES20.glGetUniformLocation(mShapeProgramHandle, "u_MinLightFactor");
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        int decalTex;
        switch (mShapeDecalFlag) {
            case STAR_DECAL:
                decalTex = mStarMarbleTexDataHandle;
                break;
            case ROSE_DECAL:
                decalTex = mRoseMarbleTexDataHandle;
                break;
            case EYE_DECAL:
                decalTex = mEyeMarbleTexDataHandle;
                break;
            case BFLY_DECAL:
                decalTex = mButterflyTexDataHandle;
                break;
            case FISH_DECAL:
                decalTex = mFishMarbleTexDataHandle;
                break;
            default:
                decalTex = mMarbleTextureDataHandle;
        }
        // Choose decal texture if the shape is one of the 20-faced types.
        int texDataHandle = mShape.mType == Shape.ICOS || mShape.mType == Shape.RNDICOS
                ? decalTex
                : mMarbleTextureDataHandle;
        // Tell the texture uniform sampler to use this texture in the shader by binding to
        // texture unit 0.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texDataHandle);
        GLES20.glUniform1i(mTextureUniformHandle, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mMarbleTextureDataHandle);
        GLES20.glUniform1i(mTextureUniformHandle2, 1);
        // Set active texture unit back to 0 for the rest of the frame rendering.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // Pass in the position information.
        mShape.mMeshPositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, SLHelper.POSITION_DATA_SIZE, GLES20.GL_FLOAT,
                false, 0, mShape.mMeshPositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        // Pass in the normals information.
        mShape.mMeshNormals.position(0);
        GLES20.glVertexAttribPointer(mNormalsHandle, SLHelper.POSITION_DATA_SIZE, GLES20.GL_FLOAT,
                false, 0, mShape.mMeshNormals);
        GLES20.glEnableVertexAttribArray(mNormalsHandle);
        // Pass in the triangle center point information.
        GLES20.glVertexAttribPointer(mCentersHandle, SLHelper.POSITION_DATA_SIZE, GLES20.GL_FLOAT,
                false, 0, mShape.mMeshCenters);
        GLES20.glEnableVertexAttribArray(mCentersHandle);
        // Pass in the texture coordinate information.
        mShape.mTextureCoords0.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordHandle0, SLHelper.TEXTURE_COORD_DATA_SIZE,
                GLES20.GL_FLOAT, false, 0, mShape.mTextureCoords0);
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle0);
        mShape.mTextureCoords1.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordHandle1, SLHelper.TEXTURE_COORD_DATA_SIZE,
                GLES20.GL_FLOAT, false, 0, mShape.mTextureCoords1);
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle1);
        mShape.mTextureCoords2.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordHandle2, SLHelper.TEXTURE_COORD_DATA_SIZE,
                GLES20.GL_FLOAT, false, 0, mShape.mTextureCoords2);
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle2);
        // Pass in the light sources position and color information
        float[] light0Col = mLights[0].isOn() ? mLights[0].mColorVec : Vector.ZERO4;
        float[] light1Col = mLights[1].isOn() ? mLights[1].mColorVec : Vector.ZERO4;
        float[] light2Col = mLights[2].isOn() ? mLights[2].mColorVec : Vector.ZERO4;
        float[] movCircCol = mGravCircleCount == 2 ? mMobRing.gColor : Vector.ZERO4;
        GLES20.glUniform3fv(mLight00PosHandle, 1, mLights[0].mPos, 0);
        GLES20.glUniform3fv(mLight01PosHandle, 1, mLights[1].mPos, 0);
        GLES20.glUniform3fv(mLight02PosHandle, 1, mLights[2].mPos, 0);
        GLES20.glUniform4fv(mLight00ColHandle, 1, light0Col, 0);
        GLES20.glUniform4fv(mLight01ColHandle, 1, light1Col, 0);
        GLES20.glUniform4fv(mLight02ColHandle, 1, light2Col, 0);
        GLES20.glUniform3fv(mCircle00PosHandle, 1, mCtrRing.gPos, 0);
        GLES20.glUniform3fv(mCircle01PosHandle, 1, mMobRing.gPos, 0);
        GLES20.glUniform4fv(mCircle00ColHandle, 1, mCtrRing.gColor, 0);
        GLES20.glUniform4fv(mCircle01ColHandle, 1, movCircCol, 0);
        GLES20.glUniform3fv(mSparklePosHandle, 1, mSparkleLightPos1, 0);
        GLES20.glUniform4fv(mSparkleColHandle, 1, mSparkleLightCol, 0);
        // Pass in factor for minimum background lighting level.
        GLES20.glUniform1f(minLightFactor, mMinLightingFactor);
        // Compute the MVP matrix and pass it to the pipeline.
        float[] MVMatrix = new float[16];
        Matrix.multiplyMM(MVMatrix, 0, mEye.mViewM, 0, mShape.mModelM, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, MVMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        // Pass in the orientation matrix for normals transforms to world space.
        GLES20.glUniformMatrix4fv(mRotMatrixHandle, 1, false, mShape.mOrientM, 0);
        // Pass in the model matrix for position transform to world space.
        GLES20.glUniformMatrix4fv(mModMatrixHandle, 1, false, mShape.mModelM, 0);
        // Draw the shape.
        int numVerts = mShape.mType == Shape.ICOS || mShape.mType == Shape.RNDICOS ? 60 : 36;
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numVerts);
    }

    private void renderSprites() {
        GLES20.glUseProgram(mSpriteProgramHandle);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(mSpriteProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mSpriteProgramHandle, "a_Color");
        mBaseSizeHandle = GLES20.glGetAttribLocation(mSpriteProgramHandle, "a_BaseSize");
        mSpriteTypeHandle = GLES20.glGetAttribLocation(mSpriteProgramHandle, "a_Type");
        mLight00PosHandle = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_Light0Pos");
        mLight01PosHandle = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_Light1Pos");
        mLight02PosHandle = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_Light2Pos");
        mLight00ColHandle = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_Light0Col");
        mLight01ColHandle = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_Light1Col");
        mLight02ColHandle = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_Light2Col");
        mCircle00PosHandle = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_Circ0Pos");
        mCircle01PosHandle = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_Circ1Pos");
        mCircle00ColHandle = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_Circ0Col");
        mCircle01ColHandle = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_Circ1Col");
        mSparklePosHandle = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_SparklePos");
        mSparkleColHandle = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_SparkleCol");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_Texture");
        int sparklePtColHand = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_SparklePtCol");
        int sparkleFlColHand = GLES20.glGetUniformLocation(mSpriteProgramHandle, "u_SparkleFlCol");
        // Tell the texture uniform sampler to bind to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);
        // Bind the texture.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mSpriteTexAtlasDataHandle);
        // Pass in the position.
        mSpritePositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, SLHelper.POSITION_DATA_SIZE, GLES20.GL_FLOAT,
                false, 0, mSpritePositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        // Pass in the color information.
        mSpriteColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, SLHelper.COLOR_DATA_SIZE, GLES20.GL_FLOAT,
                false, 0, mSpriteColors);
        GLES20.glEnableVertexAttribArray(mColorHandle);
        // Pass in base size information.
        mSpriteSizes.position(0);
        GLES20.glVertexAttribPointer(mBaseSizeHandle, 1, GLES20.GL_FLOAT,
                false, 0, mSpriteSizes);
        GLES20.glEnableVertexAttribArray(mBaseSizeHandle);
        // Pass in type information.
        mSpriteTypes.position(0);
        GLES20.glVertexAttribPointer(mSpriteTypeHandle, 1, GLES20.GL_FLOAT,
                false, 0, mSpriteTypes);
        GLES20.glEnableVertexAttribArray(mSpriteTypeHandle);
        // Pass in the light points position and color information for lighting on the sprites.
        float[] light0Col = mLights[0].isOn() ? mLights[0].mColorVec : Vector.ZERO4;
        float[] light1Col = mLights[1].isOn() ? mLights[1].mColorVec : Vector.ZERO4;
        float[] light2Col = mLights[2].isOn() ? mLights[2].mColorVec : Vector.ZERO4;
        float[] movCircCol = mGravCircleCount == 2 ? mMobRing.gColor : Vector.ZERO4;
        GLES20.glUniform3fv(mLight00PosHandle, 1, mLights[0].mPos, 0);
        GLES20.glUniform3fv(mLight01PosHandle, 1, mLights[1].mPos, 0);
        GLES20.glUniform3fv(mLight02PosHandle, 1, mLights[2].mPos, 0);
        GLES20.glUniform4fv(mLight00ColHandle, 1, light0Col, 0);
        GLES20.glUniform4fv(mLight01ColHandle, 1, light1Col, 0);
        GLES20.glUniform4fv(mLight02ColHandle, 1, light2Col, 0);
        GLES20.glUniform3fv(mCircle00PosHandle, 1, mCtrRing.gPos, 0);
        GLES20.glUniform3fv(mCircle01PosHandle, 1, mMobRing.gPos, 0);
        GLES20.glUniform4fv(mCircle00ColHandle, 1, mCtrRing.gColor, 0);
        GLES20.glUniform4fv(mCircle01ColHandle, 1, movCircCol, 0);
        GLES20.glUniform3fv(mSparklePosHandle, 1, mSparkleLightPos1, 0);
        GLES20.glUniform4fv(mSparkleColHandle, 1, mSparkleLightCol, 0);
        // Pass in sparkle point & flare brightened draw colors.
        float lowFact = 0.36f * mSpAlpha;
        float highFact = 0.82f * mSpAlpha;
        GLES20.glUniform3fv(sparklePtColHand, 1,
                SLHelper.getBrightRGB(sparkle_color, lowFact, highFact), 0);
        lowFact = mIsLowVisuals ? 0.08f * mSpAlpha : 0.20f * mSpAlpha;
        highFact = mIsLowVisuals ? 0.16f * mSpAlpha : 0.50f * mSpAlpha;
        float[] spFlCol = Vector.convertToVec4(
                SLHelper.getBrightRGB(sparkle_color, lowFact, highFact));
        spFlCol[3] = (mIsLowVisuals ? 0.6f : 0.33f) * LightPoint.rgbAlphaFactor(sparkle_color);
        GLES20.glUniform4fv(sparkleFlColHand, 1, spFlCol, 0);
        // Pass in the MVP Matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mOrigMVPMat, 0);
        // Draw the sprites.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mSpriteCount);
    }

    // If 2 or more lights are at the origin combine their colors.
    public void areLightsAtOrigin() {
        float[] newCol;
        // "Close enough" tolerance value for whether a light is at the origin.
        float tol = 0.2f;
        boolean light0 = mLights[0].isOn();
        boolean light1 = mLights[1].isOn();
        boolean light2 = mLights[2].isOn();
        // Weight the light color higher if it's brighter.
        float[] light0Vec = Vector.scaleSV(mLights[0].mBright ? 2.0f : 1.0f, mLights[0].mColorVec);
        float[] light1Vec = Vector.scaleSV(mLights[1].mBright ? 2.0f : 1.0f, mLights[1].mColorVec);
        float[] light2Vec = Vector.scaleSV(mLights[2].mBright ? 2.0f : 1.0f, mLights[2].mColorVec);
        // If all 3 lights are on and at the origin.
        if (Vector.areNear(mLights[0].getPos(), Vector.ZERO, tol)
                && Vector.areNear(mLights[1].getPos(), Vector.ZERO, tol)
                && Vector.areNear(mLights[2].getPos(), Vector.ZERO, tol)
                && light0 && light1 && light2) {
            newCol = Vector.sumVArray(light0Vec, light1Vec, light2Vec);
            newCol = Vector.convertToVec4(Vector.clampVec3(1.0f, newCol));
            mLights[0].setColorVec(newCol);
            mLights[1].setColorVec(newCol);
            mLights[2].setColorVec(newCol);
            // If only 0 & 1 are on and at the origin.
        } else if (Vector.areNear(mLights[0].getPos(), Vector.ZERO, tol)
                && Vector.areNear(mLights[1].getPos(), Vector.ZERO, tol)
                && light0 & light1) {
            newCol = Vector.sumVV(light0Vec, light1Vec);
            newCol = Vector.convertToVec4(Vector.clampVec3(1.0f, newCol));
            mLights[0].setColorVec(newCol);
            mLights[1].setColorVec(newCol);
            // If only 0 & 2 are at the origin.
        } else if (Vector.areNear(mLights[0].getPos(), Vector.ZERO, tol)
                && Vector.areNear(mLights[2].getPos(), Vector.ZERO, tol)
                && light0 & light2) {
            newCol = Vector.sumVV(light0Vec, light2Vec);
            newCol = Vector.convertToVec4(Vector.clampVec3(1.0f, newCol));
            mLights[0].setColorVec(newCol);
            mLights[2].setColorVec(newCol);
            // If only 1 & 2 are at the origin.
        } else if (Vector.areNear(mLights[1].getPos(), Vector.ZERO, tol)
                && Vector.areNear(mLights[2].getPos(), Vector.ZERO, tol)
                && light1 & light2) {
            newCol = Vector.sumVV(light1Vec, light2Vec);
            newCol = Vector.convertToVec4(Vector.clampVec3(1.0f, newCol));
            mLights[1].setColorVec(newCol);
            mLights[2].setColorVec(newCol);
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private void setLightFlareVisibility() {
        float difTol = 80f;
        for (int i = 0; i < 3; i++) {
            if (mLights[i].mDoOccTest && mLights[i].isOn()) {
                // Get light point center coords.
                float[] winCoords = mLights[i].getScreenCoords();
                int ltX = Math.round(winCoords[0]);
                int ltY = Math.round(winCoords[1]);
                // Color of the light point.
                int[] ptColor = {
                        Math.round(mLights[i].mDrawColor[0] * 255),
                        Math.round(mLights[i].mDrawColor[1] * 255),
                        Math.round(mLights[i].mDrawColor[2] * 255)
                };
                ByteBuffer buffer = ByteBuffer.allocateDirect(3)
                        .order(ByteOrder.nativeOrder());
                buffer.position(0);
                // Read pixel color off frame buffer.
                GLES20.glReadPixels(ltX, ltY, 1, 1,
                        GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, buffer);
                byte[] pxRGB = new byte[3];
                buffer.get(pxRGB);
                // Pixel color that was read.
                int[] pxCol = {pxRGB[0] & 0xFF, pxRGB[1] & 0xFF, pxRGB[2] & 0xFF};
                int maxRGBDelta = Math.max(Math.abs(ptColor[0] - pxCol[0]),
                        Math.max(Math.abs(ptColor[1] - pxCol[1]), Math.abs(ptColor[2] - pxCol[2])));
                // Compute visibility factor, compensating for dist to eye to avoid "factor flicker".
                float dist = winCoords[2];
                float d1 = 100.0f; float d2 = FLICKER_DIST;
                // Max delta offsets that begin taking effect at distance d1.
                float maxOffset = 128.0f;
                float offset = Math.min(maxOffset,
                        Math.max(0.0f, (dist - d1) * maxOffset / (d2 - d1)));
                float maxDeltCompensated = Math.max((float) maxRGBDelta - offset, 0.0f);
                float visFactor = Math.max(1.0f - (maxDeltCompensated / difTol), 0.0f);
                int marg = mWidth / 3;
                boolean offNearEdge = (ltX <= 0 && ltX > -marg)
                        || (ltX >= mWidth && ltX < mWidth + marg)
                        || (ltY <= 0 && ltY > -marg)
                        || (ltY >= mHeight && ltY < mHeight + marg);
                // If the light is "off screen but near the edge" then leave its visibility
                // factor and visible boolean unchanged (unless the light is nearer than NEAR_CLIP),
                // otherwise set according to conditions.
                if (offNearEdge) {
                    if (dist < NEAR_CLIP) {
                        mLights[i].setVisibility(0.0f, false, mSimPaused);
                    }
                } else {
                    // Boolean to make the light be considered "visible" if it's off-screen
                    // so the flare is drawn, more accurately modelling real behavior.
                    boolean flareVisibleOffScrn = dist > NEAR_CLIP
                            && (ltX <= 0 || ltX >= mWidth || ltY <= 0 || ltY >= mHeight);
                    mLights[i].setVisibility(visFactor, flareVisibleOffScrn, mSimPaused);
                }
            }
        }
    }

    // Render all light source flares on an orthographic surface covering the full screen.
    private void renderFlareSurface() {
        GLES20.glUseProgram(mFlareSurfProgramHandle);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(mFlareSurfProgramHandle, "a_Position");
        mLight00PosHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light0Pos");
        mLight01PosHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light1Pos");
        mLight02PosHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light2Pos");
        mSparklePosHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_SparksPos");
        mLight00ColHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light0Col");
        mLight01ColHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light1Col");
        mLight02ColHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light2Col");
        mSparkleColHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_SparksCol");
        int light0VisHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light0Vis");
        int light1VisHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light1Vis");
        int light2VisHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light2Vis");
        int sparksVisHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_SparksVis");
        int lt0FlBlHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light0FlBlock");
        int lt1FlBlHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light1FlBlock");
        int lt2FlBlHandle = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light2FlBlock");
        int light0FlSize = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light0FlSize");
        int light1FlSize = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light1FlSize");
        int light2FlSize = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light2FlSize");
        int sparksFlSize = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_SparksFlSize");
        int light0FlAlpha = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light0FlAlpha");
        int light1FlAlpha = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light1FlAlpha");
        int light2FlAlpha = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light2FlAlpha");
        int sparksFlAlpha = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_SparksFlAlpha");
        int light0Ray1Len = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light0Ray1Len");
        int light0Ray2Len = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light0Ray2Len");
        int light0Ray3Len = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light0Ray3Len");
        int light0Ray4Len = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light0Ray4Len");
        int light1Ray1Len = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light1Ray1Len");
        int light1Ray2Len = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light1Ray2Len");
        int light1Ray3Len = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light1Ray3Len");
        int light1Ray4Len = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light1Ray4Len");
        int light2Ray1Len = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light2Ray1Len");
        int light2Ray2Len = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light2Ray2Len");
        int light2Ray3Len = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light2Ray3Len");
        int light2Ray4Len = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light2Ray4Len");
        int light0PtSize = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light0PtSize");
        int light1PtSize = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light1PtSize");
        int light2PtSize = GLES20.glGetUniformLocation(mFlareSurfProgramHandle, "u_Light2PtSize");
        // Pass in the lights position information.
        float[] lt0Coords = mLights[0].getScreenCoords();
        float[] lt1Coords = mLights[1].getScreenCoords();
        float[] lt2Coords = mLights[2].getScreenCoords();
        float[] spCoords = Vector.avgVArray(getWindowCoords(mSparkleLightPos1),
                getWindowCoords(mSparkleLightPos2));
        GLES20.glUniform2f(mLight00PosHandle, lt0Coords[0], lt0Coords[1]);
        GLES20.glUniform2f(mLight01PosHandle, lt1Coords[0], lt1Coords[1]);
        GLES20.glUniform2f(mLight02PosHandle, lt2Coords[0], lt2Coords[1]);
        GLES20.glUniform2f(mSparklePosHandle, spCoords[0], spCoords[1]);
        // Pass in the lights color information.
        GLES20.glUniform4fv(mLight00ColHandle, 1, mLights[0].mColorVec, 0);
        GLES20.glUniform4fv(mLight01ColHandle, 1, mLights[1].mColorVec, 0);
        GLES20.glUniform4fv(mLight02ColHandle, 1, mLights[2].mColorVec, 0);
        float[] sparksColor = {mSparkleLightCol[0], mSparkleLightCol[1], mSparkleLightCol[2],
                0.125f * mSparkleLightCol[3]};
        GLES20.glUniform4fv(mSparkleColHandle, 1, sparksColor, 0);
        // Pass in the lights visibility 'booleans'.
        GLES20.glUniform1i(light0VisHandle, shouldDrawFlare(mLights[0]) ? 1 : 0);
        GLES20.glUniform1i(light1VisHandle, shouldDrawFlare(mLights[1]) ? 1 : 0);
        GLES20.glUniform1i(light2VisHandle, shouldDrawFlare(mLights[2]) ? 1 : 0);
        boolean sparksFlareInFront = spCoords[2] > NEAR_CLIP;
        GLES20.glUniform1i(sparksVisHandle, mSparkling && sparksFlareInFront ? 1 : 0);
        // Visibility factors used to scale all flare params.
        float lt0Vis = mLights[0].mVisFactor;
        float lt1Vis = mLights[1].mVisFactor;
        float lt2Vis = mLights[2].mVisFactor;
        // 'Booleans' for not drawing flare on top of lt pts, used in 'fade-out' transitions.
        GLES20.glUniform1i(lt0FlBlHandle, !mLights[0].mVisible && lt0Vis > 0.01f ? 0 : 1);
        GLES20.glUniform1i(lt1FlBlHandle, !mLights[1].mVisible && lt1Vis > 0.01f ? 0 : 1);
        GLES20.glUniform1i(lt2FlBlHandle, !mLights[2].mVisible && lt2Vis > 0.01f ? 0 : 1);
        // Compute and pass in flare sizes.
        float bsSize = mHeight * 0.7f;
        float mlt0 = mLights[0].mBright ? 2.0f * lt0Vis : lt0Vis;
        float mlt1 = mLights[1].mBright ? 2.0f * lt1Vis : lt1Vis;
        float mlt2 = mLights[2].mBright ? 2.0f * lt2Vis : lt2Vis;
        GLES20.glUniform1f(light0FlSize, distFalloff(bsSize, 80.0f, lt0Coords[2]) * mlt0);
        GLES20.glUniform1f(light1FlSize, distFalloff(bsSize, 80.0f, lt1Coords[2]) * mlt1);
        GLES20.glUniform1f(light2FlSize, distFalloff(bsSize, 80.0f, lt2Coords[2]) * mlt2);
        // Sparks flare size.
        float initSize = 1.85f;
        bsSize = mHeight * (initSize + 3.0f * (1.0f - sparksColor[3]));
        GLES20.glUniform1f(sparksFlSize, distFalloff(bsSize, 90.0f, spCoords[2]));
        // Compute and pass in flare base alphas.
        float bA = 0.66f;
        float a1 = 0.6f; float a2 = 1.0f - a1;
        mlt0 = mLights[0].mBright ? 1.15f * (a2 * lt0Vis + a1) : a2 * lt0Vis + a1;
        mlt1 = mLights[1].mBright ? 1.15f * (a2 * lt1Vis + a1) : a2 * lt1Vis + a1;
        mlt2 = mLights[2].mBright ? 1.15f * (a2 * lt2Vis + a1) : a2 * lt2Vis + a1;
        float rgbFct = LightPoint.rgbAlphaFactor(mLights[0].mColorVec);
        GLES20.glUniform1f(light0FlAlpha, distFalloff(bA * rgbFct, 400.0f, lt0Coords[2]) * mlt0);
        rgbFct = LightPoint.rgbAlphaFactor(mLights[1].mColorVec);
        GLES20.glUniform1f(light1FlAlpha, distFalloff(bA * rgbFct, 400.0f, lt1Coords[2]) * mlt1);
        rgbFct = LightPoint.rgbAlphaFactor(mLights[2].mColorVec);
        GLES20.glUniform1f(light2FlAlpha, distFalloff(bA * rgbFct, 400.0f, lt2Coords[2]) * mlt2);
        // Sparks flare alpha.
        rgbFct = LightPoint.rgbAlphaFactor(mSparkleLightCol);
        GLES20.glUniform1f(sparksFlAlpha, distFalloff(bA * rgbFct, 400.0f, spCoords[2]));
        // Compute and pass in ray 1 sizes.
        bsSize = mHeight * 4.0f;
        mlt0 = mLights[0].mBright ? 2.0f * lt0Vis : lt0Vis;
        mlt1 = mLights[1].mBright ? 2.0f * lt1Vis : lt1Vis;
        mlt2 = mLights[2].mBright ? 2.0f * lt2Vis : lt2Vis;
        float bMlt3 = mLights[0].mBright ? 1.75f : 1.0f;
        float bMlt4 = mLights[1].mBright ? 1.75f : 1.0f;
        float bMlt5 = mLights[2].mBright ? 1.75f : 1.0f;
        GLES20.glUniform1f(light0Ray1Len, distFalloff(bsSize, 68.0f, lt0Coords[2]) * mlt0);
        GLES20.glUniform1f(light1Ray1Len, distFalloff(bsSize, 68.0f, lt1Coords[2]) * mlt1);
        GLES20.glUniform1f(light2Ray1Len, distFalloff(bsSize, 68.0f, lt2Coords[2]) * mlt2);
        // Compute and pass in ray 2 sizes.
        GLES20.glUniform1f(light0Ray2Len, distFalloff(bsSize, 58.0f, lt0Coords[2]) * mlt0);
        GLES20.glUniform1f(light1Ray2Len, distFalloff(bsSize, 58.0f, lt1Coords[2]) * mlt1);
        GLES20.glUniform1f(light2Ray2Len, distFalloff(bsSize, 58.0f, lt2Coords[2]) * mlt2);
        // Compute and pass in ray 3 sizes.
        GLES20.glUniform1f(light0Ray3Len, distFalloff(bsSize, 20.0f * bMlt3, lt0Coords[2]) * mlt0);
        GLES20.glUniform1f(light1Ray3Len, distFalloff(bsSize, 20.0f * bMlt4, lt1Coords[2]) * mlt1);
        GLES20.glUniform1f(light2Ray3Len, distFalloff(bsSize, 20.0f * bMlt5, lt2Coords[2]) * mlt2);
        // Compute and pass in ray 4 sizes.
        GLES20.glUniform1f(light0Ray4Len, distFalloff(bsSize, 17.0f * bMlt3, lt0Coords[2]) * mlt0);
        GLES20.glUniform1f(light1Ray4Len, distFalloff(bsSize, 17.0f * bMlt4, lt1Coords[2]) * mlt1);
        GLES20.glUniform1f(light2Ray4Len, distFalloff(bsSize, 17.0f * bMlt5, lt2Coords[2]) * mlt2);
        // Pass in light point sizes.
        float sizeScale = 0.143f * mHeight; // Scaling needed to match the drawn point sprite.
        GLES20.glUniform1f(light0PtSize, sizeScale * mLights[0].getSpriteSize() / lt0Coords[2]);
        GLES20.glUniform1f(light1PtSize, sizeScale * mLights[1].getSpriteSize() / lt1Coords[2]);
        GLES20.glUniform1f(light2PtSize, sizeScale * mLights[2].getSpriteSize() / lt2Coords[2]);
        // Pass in the MVP Matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mUIMVPMatrix, 0);
        // Pass in the vertex position information.
        mFlareSurface.mButtonPositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, SLHelper.POSITION_DATA_SIZE, GLES20.GL_FLOAT,
                false, 0, mFlareSurface.mButtonPositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        // Draw the surface.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    // Render the sparkles flare when in medium visuals setting.
    private void renderSparklesFlare() {
        float[] sparksCoords = Vector.avgVArray(getWindowCoords(mSparkleLightPos1),
                getWindowCoords(mSparkleLightPos2));
        // Return without drawing the flare if the sparks position isn't 'in front'.
        if (sparksCoords[2] < NEAR_CLIP) return;
        GLES20.glUseProgram(mSparkleFlareProgHandle);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mSparkleFlareProgHandle, "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(mSparkleFlareProgHandle, "a_Position");
        mSparklePosHandle = GLES20.glGetUniformLocation(mSparkleFlareProgHandle, "u_SparksPos");
        mSparkleColHandle = GLES20.glGetUniformLocation(mSparkleFlareProgHandle, "u_SparksCol");
        int sparksFlSize = GLES20.glGetUniformLocation(mSparkleFlareProgHandle, "u_SparksFlSize");
        int sparksFlAlpha = GLES20.glGetUniformLocation(mSparkleFlareProgHandle, "u_SparksFlAlpha");
        // Pass in position information.
        GLES20.glUniform2f(mSparklePosHandle, sparksCoords[0], sparksCoords[1]);
        // Get and pass in color information.
        float[] sparksColor = {mSparkleLightCol[0], mSparkleLightCol[1], mSparkleLightCol[2],
                0.125f * mSparkleLightCol[3]};
        GLES20.glUniform4fv(mSparkleColHandle, 1, sparksColor, 0);
        // Compute and pass in flare size.
        float initSize = 1.85f;
        float baseRadius = mHeight * (initSize + 3.0f * (1.0f - sparksColor[3]));
        float flareRadius = distFalloff(baseRadius, 90.0f, sparksCoords[2]);
        GLES20.glUniform1f(sparksFlSize, flareRadius);
        // Pass in flare base alpha.
        float rgbFct = LightPoint.rgbAlphaFactor(mSparkleLightCol);
        GLES20.glUniform1f(sparksFlAlpha, distFalloff(0.66f * rgbFct, 400.0f, sparksCoords[2]));
        // Pass in the MVP Matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mUIMVPMatrix, 0);
        // Set and pass in vertex position information.
        mSparkleFlareSurf.setSizeAndPos(2.0f * flareRadius, sparksCoords[0], sparksCoords[1]);
        mSparkleFlareSurf.mButtonPositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, SLHelper.POSITION_DATA_SIZE, GLES20.GL_FLOAT,
                false, 0, mSparkleFlareSurf.mButtonPositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        // Draw the surface.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    // Calculate distance falloff for size and alphas of flares.
    private float distFalloff(float base, float falloff, float dist) {
        return base * (float) Math.pow(falloff / (dist + falloff - NEAR_CLIP), 2.0);
    }

    // Return the window/pixel coordinates for a position in the 3D space.
    private float[] getWindowCoords(float[] pos) {
        float[] clipCoords = new float[4];
        Matrix.multiplyMV(clipCoords, 0, mOrigMVPMat, 0, Vector.convertToVec4(pos), 0);
        float xNDC = clipCoords[0] / clipCoords[3];
        float yNDC = clipCoords[1] / clipCoords[3];
        float xWin = mWidth * 0.5f * (xNDC + 1);
        float yWin = mHeight * 0.5f * (yNDC + 1);
//        float zWin = (0.5f * (zNDC + 1));
        return new float[] {xWin, yWin, clipCoords[3]};
    }

    // Return boolean for whether a LightPoint has visibility for flare drawing.
    private boolean shouldDrawFlare(LightPoint light) {
        float dist = light.getScreenCoords()[2];
        // Range where the light point draw detection is unreliable, causing 'flicker' of the flare
        // if it's still conditional on the detection, so always draw the flare.
        boolean inFlickerRange = dist > FLICKER_DIST && dist < far_plane;
        // If the flare is fade-transitioning to off.
        boolean flareFading = light.mVisFactor > 0.01f;
        return light.isOn() && light.mColorVec[3] > 0.1f
                && (light.mVisible || flareFading || inFlickerRange);
    }

    private void renderOrientAxes() {
        if (!mDrawUI) return;
        // Set up the colors
        boolean helpColOn = mHelpOn && mHelpColorOn;
        GLES20.glViewport(mAxisViewportX, mAxisViewportY, mAxisViewportW, mAxisViewportH);
        GLES20.glUseProgram(mFlatProgramHandle);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mFlatProgramHandle, "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(mFlatProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mFlatProgramHandle, "a_Color");
        // Compute model matrix for the axis to keep it positioned in front of the eye as it moves.
        float[] relToEye = Vector.scaleSV(4.8f, mEye.mViewPtOrigin);
        float[] axisPos = Vector.sumVV(mEye.mPos, relToEye);
        float[] MVMatrix = new float[16];
        Matrix.setIdentityM(MVMatrix, 0);
        Matrix.translateM(MVMatrix, 0, axisPos[0], axisPos[1], axisPos[2]);
        Matrix.multiplyMM(MVMatrix, 0, mEye.mViewM, 0, MVMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, MVMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        // Draw the outer grey polygon surfaces.
        mAxisPolygonPositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, SLHelper.POSITION_DATA_SIZE, GLES20.GL_FLOAT,
                false, 0, mAxisPolygonPositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        float[] axisCol = helpColOn
                ? Vector.multVV(SLHelper.HELP_BUTTON_COL, new float[]{1.0f, 1.0f, 1.0f, 0.4f})
                : AXIS_POLY_COL;
        GLES20.glVertexAttrib4f(mColorHandle, axisCol[0], axisCol[1], axisCol[2], axisCol[3]);
        GLES20.glDisableVertexAttribArray(mColorHandle);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 9);
        // Draw the axes.
        drawAxis(mZaxisPositions, ZAXIS_COL);
        drawAxis(mYaxisPositions, YAXIS_COL);
        drawAxis(mXaxisPositions, XAXIS_COL);
    }

    private void drawAxis(FloatBuffer positions, float[] axisCol) {
        positions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, SLHelper.POSITION_DATA_SIZE, GLES20.GL_FLOAT,
                false, 0, positions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        float[] drawCol = mHelpOn && mHelpColorOn ? SLHelper.HELP_BUTTON_COL : axisCol;
        float drawAlpha = mOrientAxesB.isPressed() ? 1.0f : axisCol[3];
        GLES20.glVertexAttrib4f(mColorHandle, drawCol[0], drawCol[1], drawCol[2], drawAlpha);
        GLES20.glDisableVertexAttribArray(mColorHandle);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);
    }

    private void renderUI() {
        if (!mDrawUI) return;
        GLES20.glUseProgram(mUIProgramHandle);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mUIProgramHandle, "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(mUIProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mUIProgramHandle, "a_Color");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mUIProgramHandle, "u_Texture");
        mTextureCoordHandle0 = GLES20.glGetAttribLocation(mUIProgramHandle, "a_TexCoordinate");
        // Pass in the MVP Matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mUIMVPMatrix, 0);
        // Eye move mode button.
        mEyeMoveModeB.mIconTexHandle = mEye.mMovHorizontal ? mHorizIconTexDataHandle
                : mVertIconTexDataHandle;
        renderButton(mEyeMoveModeB);
        // Eye lock button.
        mEyeLockB.mIconTexHandle = mEye.mSlewFree ? mUnlockedIconTexDataHandle
                : mLockedIconTexDataHandle;
        if (mEye.mMoveFree) {
            mEyeLockB.mColor = SLHelper.DEF_BUTTON_COL;
        } else {
            mEyeLockB.mColor = mEye.mAutoOrbit ? SLHelper.HIGHLIT_BUTTON_COL2
                    : SLHelper.HIGHLIT_BUTTON_COL;
        }
        renderButton(mEyeLockB);
        // Emitter button.
        mEmitterB.mColor = mBubblesGenerating ? SLHelper.HIGHLIT_BUTTON_COL
                : SLHelper.DEF_BUTTON_COL;
        renderButton(mEmitterB);
        mMusicB.mColor = mPerforming ? SLHelper.HIGHLIT_BUTTON_COL : SLHelper.DEF_BUTTON_COL;
        renderButton(mMusicB);
        // Shape "pop-up menu" buttons function.
        long menuUpTime;
        float fadeFactor;
        if (mShapeMenuOn) {
            menuUpTime = mThisFrameStartTime - mShapeMenuStart;
            fadeFactor = (float) menuUpTime / (float) FADE_TIME;
            if (fadeFactor > 1.0f) fadeFactor = 1.0f;
            mCubeB.mColor = Vector.multVV(SLHelper.DEF_BUTTON_COL,
                    new float[] {1.0f, 1.0f, 1.0f, fadeFactor});
            mIcosB.mColor = Vector.multVV(SLHelper.DEF_BUTTON_COL,
                    new float[] {1.0f, 1.0f, 1.0f, fadeFactor});
            mRndDodecB.mColor = Vector.multVV(SLHelper.DEF_BUTTON_COL,
                    new float[] {1.0f, 1.0f, 1.0f, fadeFactor});
            mRndIcosB.mColor = Vector.multVV(SLHelper.DEF_BUTTON_COL,
                    new float[] {1.0f, 1.0f, 1.0f, fadeFactor});
            if (mShape.mType == Shape.CUBE) {
                mCubeB.mColor = Vector.multVV(SLHelper.HIGHLIT_BUTTON_COL,
                        new float[] {1.0f, 1.0f, 1.0f, fadeFactor});
            } else if (mShape.mType == Shape.ICOS) {
                mIcosB.mColor = Vector.multVV(SLHelper.HIGHLIT_BUTTON_COL,
                        new float[] {1.0f, 1.0f, 1.0f, fadeFactor});
            } else if (mShape.mType == Shape.RNDDODEC) {
                mRndDodecB.mColor = Vector.multVV(SLHelper.HIGHLIT_BUTTON_COL,
                        new float[] {1.0f, 1.0f, 1.0f, fadeFactor});
            } else if (mShape.mType == Shape.RNDICOS) {
                mRndIcosB.mColor = Vector.multVV(SLHelper.HIGHLIT_BUTTON_COL,
                        new float[] {1.0f, 1.0f, 1.0f, fadeFactor});
            }
            renderButton(mCubeB);
            renderButton(mIcosB);
            renderButton(mRndDodecB);
            renderButton(mRndIcosB);
        }
        // Light color pop-up menu buttons
        if (mLight0MenuOn) {
            renderPopupColorButtons(mThisFrameStartTime - mLight0MenuStart);
        }
        if (mLight1MenuOn) {
            renderPopupColorButtons(mThisFrameStartTime - mLight1MenuStart);
        }
        if (mLight2MenuOn) {
            renderPopupColorButtons(mThisFrameStartTime - mLight2MenuStart);
        }
        // Reset button.
        renderButton(mResetB);
        // Settings button.
        renderButton(mMenuB);
        // Help button.
        renderButton(mHelpInfoB);
        // Update LightPoint button color with LightPoint color
        float[] colorVec;
        // Render object button according to which is active.
        switch (mActiveMoverFlag) {
            case SHAPE:
                switch (mShape.mType) {
                    case Shape.CUBE:
                        mShapeB.mIconTexHandle = mCubeIconTexDataHandle;
                        break;
                    case Shape.ICOS:
                        mShapeB.mIconTexHandle = mIcosIconTexDataHandle;
                        break;
                    case Shape.RNDDODEC:
                        mShapeB.mIconTexHandle = mRnddodecIconTexDataHandle;
                        break;
                    case Shape.RNDICOS:
                        mShapeB.mIconTexHandle = mRndicosIconTexDataHandle;
                        break;
                }
                renderButton(mShapeB);
                break;
            case LIGHT_0:
                colorVec = new float[] {
                        mLights[0].mColorVec[0], mLights[0].mColorVec[1], mLights[0].mColorVec[2],
                        1.0f};
                mLight0B.mColor = Vector.scaleSV(0.66f, colorVec);
                mLight0B.mPressedColor = colorVec;
                renderButton(mLight0B);
                break;
            case LIGHT_1:
                colorVec = new float[] {
                        mLights[1].mColorVec[0], mLights[1].mColorVec[1], mLights[1].mColorVec[2],
                        1.0f};
                mLight1B.mColor = Vector.scaleSV(0.66f, colorVec);
                mLight1B.mPressedColor = colorVec;
                renderButton(mLight1B);
                break;
            case LIGHT_2:
                colorVec = new float[] {
                        mLights[2].mColorVec[0], mLights[2].mColorVec[1], mLights[2].mColorVec[2],
                        1.0f};
                mLight2B.mColor = Vector.scaleSV(0.66f, colorVec);
                mLight2B.mPressedColor = colorVec;
                renderButton(mLight2B);
                break;
            case GRAV_CIRC:
                colorVec = mMobRing.isSpringSq() ? GRAV_CIRC_RED : GRAV_CIRC_BLUE ;
                mGravCircB.mColor = Vector.scaleSV(0.66f, colorVec);
                mGravCircB.mPressedColor = colorVec;
                renderButton(mGravCircB);
                break;
        }
        // Eye control regions.
        if (mEyeControlRight.isPressed() && !mHelpOn) {
            mEyeControlPosRight.mPressedColor = mOnScrnEdgeRt ? EYE_CNTRL_SCRN_EDGE
                    : EYE_CNTRL_PRESSED;
            renderButton(mEyeControlPosRight);
        } else {
            renderButton(mEyeControlRight);
        }
        if (mEyeControlLeft.isPressed() && !mHelpOn) {
            mEyeControlPosLeft.mPressedColor = mOnScrnEdgeLft ? EYE_CNTRL_SCRN_EDGE
                    : EYE_CNTRL_PRESSED;
            renderButton(mEyeControlPosLeft);
        } else {
            renderButton(mEyeControlLeft);
        }
        if (mHelpOn) {
            renderButton(mObjManipHelpB);
        }
        // Pause button.
        mPauseB.mColor = mSimPaused ? SLHelper.HIGHLIT_BUTTON_COL : SLHelper.DEF_BUTTON_COL;
        renderButton(mPauseB);
    }

    private void renderPopupColorButtons(long menuUpTime) {
        float fadeFactor = (float) menuUpTime / (float) FADE_TIME;
        if (fadeFactor > 1.0f) fadeFactor = 1.0f;
        mRedColorB.mColor = new float[] {1.0f, 0.0f, 0.0f, 0.66f * fadeFactor};
        mYellowColorB.mColor = new float[] {1.0f, 1.0f, 0.0f, 0.66f * fadeFactor};
        mGreenColorB.mColor = new float[] {0.0f, 1.0f, 0.0f, 0.66f * fadeFactor};
        mCyanColorB.mColor = new float[] {0.0f, 1.0f, 1.0f, 0.66f * fadeFactor};
        mBlueColorB.mColor = new float[] {0.0f, 0.0f, 1.0f, 0.75f * fadeFactor};
        mMagentColorB.mColor = new float[] {1.0f, 0.0f, 1.0f, 0.66f * fadeFactor};
        mWhiteColorB.mColor = new float[] {1.0f, 1.0f, 1.0f, 0.66f * fadeFactor};
        renderButton(mRedColorB);
        renderButton(mYellowColorB);
        renderButton(mGreenColorB);
        renderButton(mCyanColorB);
        renderButton(mBlueColorB);
        renderButton(mMagentColorB);
        renderButton(mWhiteColorB);
    }

    private void renderButton(SLButton button) {
        float[] color;
        // Select button color according to state.
        if (button.enabled()) {
            if (button.isPressed() && (!mHelpOn || button.mIsHelpButton)) {
                color = button.mPressedColor.clone();
            } else {
                color = button.mColor.clone();
            }
        } else {
            color = SLHelper.DISABLED_BUTTON_COL.clone();
        }
        if (mHelpOn && mHelpColorOn && !button.mIsHelpButton) {
            color = SLHelper.HELP_BUTTON_COL.clone();
        }
        // Bind the texture to the active texture unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, button.mIconTexHandle);
        // Tell the texture uniform sampler to use this texture in the shader by binding to
        // texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);
        // Pass in the position information.
        button.mButtonPositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, SLHelper.POSITION_DATA_SIZE, GLES20.GL_FLOAT,
                false, 0, button.mButtonPositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        // Pass in color information.
        GLES20.glVertexAttrib4f(mColorHandle, color[0], color[1], color[2], color[3]);
        GLES20.glDisableVertexAttribArray(mColorHandle);
        // Pass in the texture coordinate information.
        button.mTextureCoords.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordHandle0, SLHelper.TEXTURE_COORD_DATA_SIZE,
                GLES20.GL_FLOAT, false, 0, button.mTextureCoords);
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    // Create blinking effect by regular toggling of the mHelpColorOn boolean.
    private void blinkHelpColor() {
        if ((mThisFrameStartTime - mSimStartTime) % HELP_BLINK_PERIOD < STROBE_TOL) {
            if (!mHelpColorCycled) {
                mHelpColorCycled = true;
                mHelpColorOn = !mHelpColorOn;
            }
        } else {
            mHelpColorCycled = false;
        }
    }

    private void initLightColorButtons(SLButton lightButton) {
        float x = mButtonMargin + mButtonSize;
        float halfW = mButtonSize / 2.0f;
        // "delta-Y" length for hexagonal arrangement.
        float dY = 0.268f * halfW;
        float y1 = lightButton.mY - mButtonSize + dY;
        float y2 = lightButton.mY;
        float y3 = lightButton.mY + mButtonSize - dY;
        mRedColorB = new SLButton(x, y2, mButtonSize, mButtonSize,
                new float[] {1.0f, 0.0f, 0.0f, 0.75f},
                SLHelper.RED_COLOR_VEC, mSoftCircTexDataHandle
        );
        mYellowColorB = new SLButton(x + halfW, y3,
                mButtonSize, mButtonSize, new float[] {1.0f, 1.0f, 0.0f, 0.75f},
                SLHelper.YEL_COLOR_VEC, mSoftCircTexDataHandle
        );
        mGreenColorB = new SLButton(x + halfW + mButtonSize, y3,
                mButtonSize, mButtonSize, new float[] {0.0f, 1.0f, 0.0f, 0.75f},
                SLHelper.GRN_COLOR_VEC, mSoftCircTexDataHandle
        );
        mCyanColorB = new SLButton(x + mButtonSize * 2, y2,
                mButtonSize, mButtonSize, new float[] {0.0f, 1.0f, 1.0f, 0.75f},
                SLHelper.CYA_COLOR_VEC, mSoftCircTexDataHandle
        );
        mBlueColorB = new SLButton(x + halfW + mButtonSize, y1,
                mButtonSize, mButtonSize, new float[] {0.0f, 0.0f, 1.0f, 0.75f},
                SLHelper.BLU_COLOR_VEC, mSoftCircTexDataHandle
        );
        mMagentColorB = new SLButton(x + halfW, y1,
                mButtonSize, mButtonSize, new float[] {1.0f, 0.0f, 1.0f, 0.75f},
                SLHelper.MAG_COLOR_VEC, mSoftCircTexDataHandle
        );
        mWhiteColorB = new SLButton(x + mButtonSize, y2,
                mButtonSize, mButtonSize, new float[] {1.0f, 1.0f, 1.0f, 0.75f},
                SLHelper.WHI_COLOR_VEC, mSoftCircTexDataHandle
        );
    }

    public void applyForce(FreeMover mover) {
        // Do no update if framerate has stalled.
        if (mFrametimeFactor > MAX_FRAMETIME_FACTOR) return;
        if (mover.isFree()) {
            float[] posV = mover.getPos();
            float posDist = Vector.length(posV);
            float accelMag;
            float forceFactor;
            float[] accelVec = Vector.ZERO.clone();
            boolean springSqCtr = mCtrRing.isSpringSq();
            // Adjust acceleration according to the mover's mass via the forceFactor.
            float moverMass = mover.getMass();
            forceFactor = mCtrRing.gForceFactor / moverMass;
            if (posDist > RING_RADIUS * 0.08f) {
                if (springSqCtr) {
//                    accelMag = -forceFactor * mFrametimeFactor / (posDist * posDist);
//                    accelVec = Vector.scaleSV((accelMag / posDist), posV);
                    accelMag = -forceFactor * mFrametimeFactor * posDist;
                    accelVec = Vector.scaleSV(accelMag, posV);
                } else {
                    accelMag = -forceFactor * mFrametimeFactor;
                    accelVec = Vector.scaleSV(accelMag, posV);
                }
            }
            // If a 2nd GravityCircle object is active and the FreeMover is not the GravityCircle
            // itself, add its force.
            if (mGravCircleCount > 1 && moverMass < 2.0f) {
                float[] deltaPosV = Vector.diffVV(posV, mMobRing.getPos());
                float deltaPosDist = Vector.length(deltaPosV);
                float deltaAccelMag;
                float[] deltaAccVec = Vector.ZERO.clone();
                forceFactor = mMobRing.gForceFactor / moverMass;
                boolean springSq = mMobRing.isSpringSq();
                if (deltaPosDist > RING_RADIUS * 0.08f) {
                    if (springSq) {
                        deltaAccelMag = -forceFactor * mFrametimeFactor * deltaPosDist;
                        deltaAccVec = Vector.scaleSV(deltaAccelMag, deltaPosV);
                    } else {
                        deltaAccelMag = -forceFactor * mFrametimeFactor;
                        deltaAccVec = Vector.scaleSV(deltaAccelMag, deltaPosV);
                    }
                }
                accelVec = mMobGravExclusive ? deltaAccVec : Vector.sumVV(accelVec, deltaAccVec);
            }
            mover.incVel(accelVec);
        }
    }

    public void strobeLights() {
        if ((mThisFrameStartTime - mSimStartTime) % mStrobeInterval < STROBE_TOL) {
            if (!mStrobeCycled) {
                // Execute a strobe.
                mStrobeCycled = true;
                boolean strobed = false;
                int counter;
                for (int i = 0; i < LightPoint.getLightsOnCount(); i++) {
                    if (!strobed) {
                        counter = cycleStrobeCounter();
                        if (mOnLights[counter].mStrobeEnabled) {
                            mOnLights[counter].startStrobe();
                            strobed = true;
                        }
                    }
                }
            }
        } else {
            mStrobeCycled = false;
        }
    }

    // Cycle the lights strobe counter, but return the pre-cycled value.
    public int cycleStrobeCounter() {
        int initCounter = mStrobeCounter++;
        if (mStrobeCounter == LightPoint.getLightsOnCount()) mStrobeCounter = 0;
        return initCounter;
    }

    public void toggleBubbleGen() {
        mBubblesGenerating = !mBubblesGenerating;
        if (mBubblesGenerating) {
            // Initialize bubble generation.
            if (!mBubblesUp) {
                mBubblesUp = true;
                mNewBubbleIndex = 0;
            }
            mShape.initBubbleEmitInterpolation();
        }
    }

    // Stop all bubble generation and rendering, reset the BubblePoint arrays.
    protected void stopBubbling() {
        mBubblesGenerating = false;
        mBubblesUp = false;
        mBubblesEmittedCount = 0;
        for (int i = 0; i < bubbles_count; i++) {
            mBubblePoints[i] = new BubblePoint(i);
        }
    }

    public void bubbler(float frameFactor) {
        // Do no update if framerate has stalled.
        if (frameFactor > MAX_FRAMETIME_FACTOR) return;
        if (mBubblesGenerating) {
            if (mShape.mEmitBubble1) setBubble(mShape.mBubbleEmitter1, mShape.mBubEmitLocation1);
            if (mShape.mEmitBubble2) setBubble(mShape.mBubbleEmitter2, mShape.mBubEmitLocation2);
            if ((mShape.mEmitBubble1 || mShape.mEmitBubble2) && !mMusicPlaying)
                playBubbleEmit(mShape.mPos);
        }
        for (int i = 0; i < mBubblesEmittedCount; i++) {
            mBubblePoints[i].perFrameUpdate(frameFactor);
            if (!mBubblePoints[i].bUp) {
                mBubblePoints = removeBubble(mBubblePoints, i);
                mBubblesEmittedCount--;
            }
        }
        if (mBubblesEmittedCount == 0) {
            mBubblesUp = false;
        }
    }

    private void setBubble(float[] emitter, float[] emitterLoc) {
        // Set bubble index in array back to emitted count in case bubbles have been removed.
        if (mNewBubbleIndex > mBubblesEmittedCount) {
            mNewBubbleIndex = mBubblesEmittedCount;
        }
        // If bubble at current new bubble index is already up don't increase emitted count below.
        boolean bubbleWasUp = mBubblePoints[mNewBubbleIndex].bUp;
        mBubblePoints[mNewBubbleIndex].setPos(emitterLoc)
                .setVel(Vector.scaleSV(BUBB_VEL, Vector.norm(Vector.diffVV(emitter, mShape.mPos))))
                .setSize(BUBBLES_START_SIZE)
                .setColor(bubble_min_color)
                .setStartTime(mThisFrameStartTime)
                .setUp(true);
        mNewBubbleIndex++;
        if (mNewBubbleIndex == bubbles_count) {
            mNewBubbleIndex = 0;
        }
        // Increment emitted count if it's below the max count and bubble at index wasn't already up.
        if (mBubblesEmittedCount < bubbles_count && !bubbleWasUp) {
            mBubblesEmittedCount++;
        }
    }

    public void initSparkler() {
        mSparkling = true;
        mCrackling = false;
        mSmoking = false;
        mSmokeEmittedCount = 0;
        mSparklesEmittedCount = 0;
        // Generate sparkler color via random increment on rgb "rainbow" mapping.
        mSparkleColorFuncX += 5.0f + 10.0f * (float) Math.random();
        sparkle_color = SLHelper.rainbowMap(SPARKLE_COL_FUNC_PER, mSparkleColorFuncX);
        mSparkleLightCol = sparkle_color.clone();
        // Init the array and values.
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < sparkle_count; j++) {
                mSparkles[i][j] = new SparklePoint();
            }
        }
        mSparklesGenerating = true;
        mSparkleFrameCounter = 0;
        if (mCracklePlayed) {
            mMainLauncher.stopSparkleCrackleSound();
            mCracklePlayed = false;
        }
        mSparklingStartTime = mThisFrameStartTime;
        playSparkleEmit(mShape.mPos);
    }

    public void sparkler(float frameFactor) {
        // Do no update if framerate has stalled.
        if (frameFactor > MAX_FRAMETIME_FACTOR) return;
        if (mSparklesGenerating) {
            float[] vel;
            for (int i = 0; i < 8; i++) {
                vel = Vector.norm(Vector.diffVV(mShape.mSparkleEmitters[i], mShape.mPos));
                vel = Vector.scaleSV(SPARKLE_SPEED, vel);
                mSparkles[i][mSparklesEmittedCount].setPos(mShape.mSparkleEmitters[i])
                        .setVel(vel);
            }
            mSparklesEmittedCount++;
            if (mSparklesEmittedCount == sparkle_count) {
                mSparklesGenerating = false;
                // Start smoke effect once all sparks are emitted.
                if (mVisualsLevel != LOW_VISUALS) {
                    initSmoke();
                }
            }
        }
        mSparkleFrameCounter++;
        // Update params shared by all sparkle points.
        long sparklingTime = mThisFrameStartTime - mSparklingStartTime;
        float sparkleTimeFactor = (float) sparklingTime / (float) sparkles_duration;
        mCrackling = sparklingTime > CRACKLE_WAIT;
        mSpAlpha = 1.0f - sparkleTimeFactor;
        mSparkleSpriteSize = sparkle_start_size + sparkle_delta_size * sparkleTimeFactor;
        // Update point-specific params.
        for (int i = 0; i < 8; i++) for (int j = 0; j < mSparklesEmittedCount; j++)
                mSparkles[i][j].perFrameUpdate(frameFactor);
        // Position vector used to compute sparkling light source and positional audio for crackle.
        mSparkleLightPos1 = Vector.avgVArray(
                mSparkles[0][0].spPos,
                mSparkles[1][0].spPos,
                mSparkles[2][0].spPos,
                mSparkles[3][0].spPos,
                mSparkles[4][0].spPos,
                mSparkles[5][0].spPos,
                mSparkles[6][0].spPos,
                mSparkles[7][0].spPos
        );
        mSparkleLightPos2 = Vector.avgVArray(
                mSparkles[0][mSparklesEmittedCount - 1].spPos,
                mSparkles[1][mSparklesEmittedCount - 1].spPos,
                mSparkles[2][mSparklesEmittedCount - 1].spPos,
                mSparkles[3][mSparklesEmittedCount - 1].spPos,
                mSparkles[4][mSparklesEmittedCount - 1].spPos,
                mSparkles[5][mSparklesEmittedCount - 1].spPos,
                mSparkles[6][mSparklesEmittedCount - 1].spPos,
                mSparkles[7][mSparklesEmittedCount - 1].spPos
        );
        // Alpha color value used to compute sparkling light intensity.
        float avgAlphaOctupled = 0.0f;
        for (int i = 0; i < 8; i++) {
            avgAlphaOctupled += mSparkles[i][0].spColor[3];
        }
        mSparkleLightCol[3] = avgAlphaOctupled;
        // Play or update the crackling sound effect.
        if (mCrackling) {
            if (mCracklePlayed) {
                // Update sparkle crackle sound every 12 frames, or 5x/sec.
                if (mSparkleFrameCounter % 12 == 0) updateSparkleCrackle(mSparkleLightPos1);
            } else {
                playSparkleCrackle(mSparkleLightPos1);
                mCracklePlayed = true;
            }
        }
        if (sparklingTime > sparkles_duration) {
            mSparkling = false;
            mCracklePlayed = false;
            mSparklesEmittedCount = 0;
            mSparkleLightPos1 = Vector.ZERO.clone();
            mSparkleLightCol = Vector.ZERO4.clone();
        }
    }

    public void initSmoke() {
        mSmoking = true;
        mSmokeEmittedCount = smoke_count;
        int ind1 = smoke_count == 3 ? 1 : 2;
        mSmokePuffs[0] = new SmokePoint(
                Vector.avgVArray(
                        mSparkles[0][ind1].spPos,
                        mSparkles[2][ind1].spPos,
                        mSparkles[4][ind1].spPos,
                        mSparkles[5][ind1].spPos
                ),
                Vector.scaleSV(SMOKE_SPEED, Vector.norm(mSparkles[0][ind1].spVel))
        );
        int ind2 = sparkle_count - 3;
        mSmokePuffs[1] = new SmokePoint(
                Vector.avgVArray(
                        mSparkles[0][ind2].spPos,
                        mSparkles[2][ind2].spPos,
                        mSparkles[4][ind2].spPos,
                        mSparkles[5][ind2].spPos
                ),
                Vector.scaleSV(SMOKE_SPEED, Vector.norm(mSparkles[2][ind2].spVel))
        );
        if (smoke_count == 3) {
            int ind3 = (sparkle_count / 2) - 1;
            mSmokePuffs[2] = new SmokePoint(
                    Vector.avgVArray(
                            mSparkles[0][ind3].spPos,
                            mSparkles[2][ind3].spPos,
                            mSparkles[4][ind3].spPos,
                            mSparkles[5][ind3].spPos
                    ),
                    Vector.scaleSV(SMOKE_SPEED, Vector.norm(mSparkles[7][ind3].spVel))
            );
        }
        mSmokingStartTime = mThisFrameStartTime;
    }

    public void smoker(float frameFactor) {
        // Do no update if framerate has stalled.
        if (frameFactor > MAX_FRAMETIME_FACTOR) return;
        for (SmokePoint smoke : mSmokePuffs) {
            smoke.perFrameUpdate(frameFactor);
        }
        if (mThisFrameStartTime - mSmokingStartTime > SMOKE_DURATION) {
            mSmoking = false;
            mSmokeEmittedCount = 0;
        }
    }

    public void togglePerformance() {
        if (mMusicPlaying && !mPerforming) {
            mMainLauncher.stopMusic();
            return;
        }
        mPerforming = !mPerforming;
        if (mPerforming) {
            long currTime = System.nanoTime();
            mResetB.unPress();
            mEmitterB.unPress();
            mRotatorIndex = 0;
            mBubblerIndex = 0;
            mSparklerIndex = 0;
            mReqMusPosUpdate = true;
            mPerfFrameCounter = 0;
            stopBubbling();
            mMainLauncher.startMusic();
            mMusicStartTime = currTime;
            mMusPosUpdateT = currTime + MUS_POS_UPDATE_WAIT;
            mMusicPosition = 0;
            mMusPosReadTime = currTime;
            // Stop any current shape rotation.
            mShape.initPerformanceRot(1000, 0, 100, 1, 1);
            if (mPerformanceHidesUI) {
                enableUI(false);
            } else {
                mResetB.disable();
                mEmitterB.disable();
            }
        } else {
            // Stop music.
            mMainLauncher.stopMusic();
            // Stop shape performance rotation.
            mShape.mPerfMode = Shape.NONE;
            // Stop bubbles emitter.
            if (mBubblesGenerating) {
                toggleBubbleGen();
            }
            // Unhide the UI if hidden, otherwise just re-enabled disabled buttons.
            if (mPerformanceHidesUI && !mDrawUI) {
                enableUI(true);
            } else {
                mResetB.enable();
                mEmitterB.enable();
            }
        }
    }

    // Executes shape's music performance routine.
    public void performer() {
        // Update music playing state every 15 frames, or 4x/sec.
        if (mPerfFrameCounter % 15 == 0) mMainLauncher.updateMusicPlayingState();
        mPerfFrameCounter++;
        long currTime = System.nanoTime();
        // Implement initial 250 ms waiting period to allow the MediaPlayer playback to post in the
        // main thread and start providing accurate mMusicPosition updates.
        boolean waitDone = currTime - mMusicStartTime > INIT_WAIT;
        if (mMusicPlaying) {
            // Request music position update every 500 ms.
            if (mReqMusPosUpdate && currTime > mMusPosUpdateT) {
                mMainLauncher.updateMusicPosition();
                mReqMusPosUpdate = false;
            }
            int musPosDelta = (int) ((currTime - mMusPosReadTime) / NANOS_PER_MILLI);
            int offset = BASE_PERF_OFFSET + perf_add_offset;
            int musicPos = mMusicPosition + musPosDelta;
            if (waitDone && musicPos >= (mPerfRotationData[mRotatorIndex] + offset) ) {
                mShape.initPerformanceRot(
                        mPerfRotationData[mRotatorIndex + 1],
                        mPerfRotationData[mRotatorIndex + 2],
                        mPerfRotationData[mRotatorIndex + 3],
                        mPerfRotationData[mRotatorIndex + 4],
                        mPerfRotationData[mRotatorIndex + 5]
                );
                if (mRotatorIndex < mPerfRotationData.length - 6) {
                    mRotatorIndex += 6;
                }
            }
            if (waitDone && musicPos >= (mPerfBubblerData[mBubblerIndex] + offset) ) {
                toggleBubbleGen();
                if (mBubblerIndex < mPerfBubblerData.length - 1) {
                    mBubblerIndex++;
                }
            }
            if (waitDone && musicPos >= (mPerfSparklerTimes[mSparklerIndex] + offset) ) {
                initSparkler();
                if (mSparklerIndex < mPerfSparklerTimes.length - 1) {
                    mSparklerIndex++;
                }
            }
        } else {
            togglePerformance();
        }
    }

    // Set selected FreeMover object as the mActiveMover, called via callback from SLSurfaceView
    // button touch event. The mover flag being passed corresponds to the button click registered
    // in the SLSurfaceView object, leaving the logic for active mover cycling to occur locally in
    // this method.
    public void setActiveMover(int currentMover) {
        boolean light0 = mLights[0].isOn();
        boolean light1 = mLights[1].isOn();
        boolean light2 = mLights[2].isOn();
        boolean gravCirc = mGravCircleCount == 2;
        switch (currentMover) {
            case SHAPE:
                if (light0) {
                    mActiveMoverFlag = LIGHT_0;
                    mActiveMover = mLights[0];
                } else if (light1) {
                    mActiveMoverFlag = LIGHT_1;
                    mActiveMover = mLights[1];
                } else if (light2) {
                    mActiveMoverFlag = LIGHT_2;
                    mActiveMover = mLights[2];
                } else if (gravCirc) {
                    mActiveMoverFlag = GRAV_CIRC;
                    mActiveMover = mMobRing;
                }
                break;
            case LIGHT_0:
                if (light1) {
                    mActiveMoverFlag = LIGHT_1;
                    mActiveMover = mLights[1];
                } else if (light2) {
                    mActiveMoverFlag = LIGHT_2;
                    mActiveMover = mLights[2];
                } else if (gravCirc) {
                    mActiveMoverFlag = GRAV_CIRC;
                    mActiveMover = mMobRing;
                } else {
                    mActiveMoverFlag = SHAPE;
                    mActiveMover = mShape;
                }
                break;
            case LIGHT_1:
                if (light2) {
                    mActiveMoverFlag = LIGHT_2;
                    mActiveMover = mLights[2];
                } else if (gravCirc) {
                    mActiveMoverFlag = GRAV_CIRC;
                    mActiveMover = mMobRing;
                } else {
                    mActiveMoverFlag = SHAPE;
                    mActiveMover = mShape;
                }
                break;
            case LIGHT_2:
                if (gravCirc) {
                    mActiveMoverFlag = GRAV_CIRC;
                    mActiveMover = mMobRing;
                } else {
                    mActiveMoverFlag = SHAPE;
                    mActiveMover = mShape;
                }
                break;
            case GRAV_CIRC:
                mActiveMoverFlag = SHAPE;
                mActiveMover = mShape;
        }
        mEye.mFocusedMoverFlag = mActiveMoverFlag;
        mEye.mLockObjChanged = true;
    }

    // Set FreeMover screen coordinates for touch detection.
    public void setMoverScreenCoords() {
        mShape.setScreenCoords(getWindowCoords(mShape.getPos()));
        for (LightPoint light : mOnLights) {
            light.setScreenCoords(getWindowCoords(light.getPos()));
        }
        if (mGravCircleCount > 1) {
            mMobRing.setScreenCoords(getWindowCoords(mMobRing.getPos()));
        }
    }

    // For given touch coordinates select the nearest FreeMover being touched and return true.
    // If none are being touched return false.
    public boolean selectMoverOnTouch(float tchX, float tchY) {
        // If the touch is on the currently active mover or the currently active mover is being
        // dragged, then keep the selection and return true.
        if (isTouchOnMover(tchX, tchY, mActiveMover.getScreenCoords())
                || !mActiveMover.isFree()) return true;
        // If camera controls are being used or a button was just pressed return false;
        if (mEye.mMoving || SLButton.a_button_was_pressed) return false;
        // Begin mover detection and selection algorithm.
        final float farthestW = 9999.9f;
        final float tol = 0.1f;
        float nearestTouchW = farthestW;
        int selectedMoverFlag = 0;
        FreeMover selectedMover = mShape;
        if (mShape.setTouched(isTouchOnMover(tchX, tchY, mShape.getScreenCoords()))) {
            nearestTouchW = Math.min(nearestTouchW, mShape.getScreenCoords()[2]);
        }
        for (LightPoint lt : mLights) {
            if (lt.isOn() && lt.setTouched(isTouchOnMover(tchX, tchY, lt.getScreenCoords()))) {
                nearestTouchW = Math.min(nearestTouchW, lt.getScreenCoords()[2]);
            }
        }
        if (mGravCircleCount > 1) {
            if (mMobRing.setTouched(isTouchOnMover(tchX, tchY, mMobRing.getScreenCoords()))) {
                nearestTouchW = Math.min(nearestTouchW, mMobRing.getScreenCoords()[2]);
            }
        }
        if (nearestTouchW < farthestW - tol) {
            if (mShape.isTouched()
                    && Vector.areFloatsEqual(mShape.getScreenCoords()[2], nearestTouchW)) {
                selectedMoverFlag = SHAPE;
                selectedMover = mShape;
            } else if (mLights[0].isTouched()
                    && Vector.areFloatsEqual(mLights[0].getScreenCoords()[2], nearestTouchW)) {
                selectedMoverFlag = LIGHT_0;
                selectedMover = mLights[0];
            } else if (mLights[1].isTouched()
                    && Vector.areFloatsEqual(mLights[1].getScreenCoords()[2], nearestTouchW)) {
                selectedMoverFlag = LIGHT_1;
                selectedMover = mLights[1];
            } else if (mLights[2].isTouched()
                    && Vector.areFloatsEqual(mLights[2].getScreenCoords()[2], nearestTouchW)) {
                selectedMoverFlag = LIGHT_2;
                selectedMover = mLights[2];
            } else if (mMobRing.isTouched()
                    && Vector.areFloatsEqual(mMobRing.getScreenCoords()[2], nearestTouchW)) {
                selectedMoverFlag = GRAV_CIRC;
                selectedMover = mMobRing;
            }
        } else {
            return false;
        }
        if (selectedMoverFlag != 0) {
            mActiveMoverFlag = selectedMoverFlag;
            mEye.mFocusedMoverFlag = selectedMoverFlag;
            mEye.mLockObjChanged = true;
            mActiveMover = selectedMover;
            return true;
        }
        return false;
    }

    // Return boolean for whether touch coords are touching a mover with given mover screen coords.
    public boolean isTouchOnMover(float tchX, float tchY, float[] moverScrnCoords) {
        // Size of touch detection box is dependent on the mover's z value.
        float tolerance = mBaseGrabTolerance / moverScrnCoords[2];
        // Disable object touch interactions if in performing or help modes.
        return !mPerforming && !mHelpOn
                && tchX > moverScrnCoords[0] - tolerance
                && tchX < moverScrnCoords[0] + tolerance
                && tchY > moverScrnCoords[1] - tolerance
                && tchY < moverScrnCoords[1] + tolerance;
    }

    // Return the W clip coordinate for the active object.
    public float getActiveObjectWclip() {
        return mActiveMover.getScreenCoords()[2];
    }

    // For given pixel coordinates set a new position vector at the active object's depth.
    public void setNewPositionVec(float pX, float pY) {
        float[] activeClipCoords = new float[4];
        Matrix.multiplyMV(activeClipCoords, 0, mOrigMVPMat, 0,
                Vector.convertToVec4(mActiveMover.getPos()), 0);
        // Get the inverse only of the projection * view matrix so the operation leaves the
        // model matrix translation intact, which gives the desired position vector.
        float[] inverseVPMatrix = new float[16];
        Matrix.invertM(inverseVPMatrix, 0, mOrigMVPMat, 0);
        float xNDC = (2.0f * pX / mWidth) - 1.0f;
        float yNDC = (2.0f * pY / mHeight) - 1.0f;
        // Clip coordinates output of the MVP matrix transform, with modified x & y values per
        // the touch input.
        float[] newClipCoords = {
                xNDC * activeClipCoords[3],
                yNDC * activeClipCoords[3],
                activeClipCoords[2],
                activeClipCoords[3]
        };
        float[] newPosVec = new float[4];
        Matrix.multiplyMV(newPosVec, 0, inverseVPMatrix, 0, newClipCoords, 0);
        mActiveMover.setPos(newPosVec);
    }

    public boolean isInEyeControlLeft(float tchX, float tchY, int id) {
        if (mEyeControlLeft.isPressed()) {
            // If the control is already active return false without doing anything.
            return false;
        }
        if (mDrawUI) {
            float value = 1.0f - (tchX * tchX / (mEyeControlWidth * mEyeControlWidth));
            value = mEyeControlHeight * (float) Math.sqrt(value);
            boolean inside = tchY <= value;
            mEyeControlLeft.mPressPointerID = inside ? id : MotionEvent.INVALID_POINTER_ID;
            return inside;
        } else {
            // If the UI is being hidden allow more relaxed detection of the button press.
            return mEyeControlLeft.initInsideTapLeft(tchX, tchY, id);
        }

    }

    public boolean isInEyeControlRight(float tchX, float tchY, int id) {
        if (mEyeControlRight.isPressed()) {
            return false;
        }
        if (mDrawUI) {
            tchX = mWidth - tchX;
            float value = 1.0f - (tchX * tchX / (mEyeControlWidth * mEyeControlWidth));
            value = mEyeControlHeight * (float) Math.sqrt(value);
            boolean inside = tchY <= value;
            mEyeControlRight.mPressPointerID = inside ? id : MotionEvent.INVALID_POINTER_ID;
            return inside;
        } else {
            // If the UI is being hidden allow more relaxed detection of the button press.
            return mEyeControlRight.initInsideTapRight(tchX, tchY, id);
        }
    }

    // Remove bubble element at index from a BubblePoint array.
    protected BubblePoint[] removeBubble(BubblePoint[] bubbles, int index) {
        BubblePoint[] editedArray = new BubblePoint[bubbles_count];
        System.arraycopy(bubbles, 0, editedArray, 0, index);
        System.arraycopy(bubbles, index + 1, editedArray, index, bubbles_count - index - 1);
        editedArray[bubbles_count - 1] = new BubblePoint(bubbles_count - 1);
        for (int i = 0; i < bubbles_count; i++) {
            editedArray[i].bIndex = i;
        }
        return editedArray;
    }

    private void playBubbleEmit(float[] bubblePos) {
        float basePitch = 1.0f;
        // Don't produce audible bubbling sound if the splash screen is still up.
        float baseVol = mSplashUp ? 0.0f : 0.85f;
        float[][] stereoVols = computeStereoVolumes(baseVol, bubblePos);
        // Doppler calculation.
        float[] normToEye = Vector.negV(stereoVols[1]);
        float[] shapeVel = mSimPaused ? Vector.ZERO : mShape.mVel;
        float[] deltaVel = Vector.diffVV(shapeVel, mEye.mDopplerVel);
        float closingSpd = Vector.dotVV(deltaVel, normToEye);
        float dopplerFact = -SPEED_OF_SOUND / (closingSpd - SPEED_OF_SOUND);
        mMainLauncher.playBubbleSound(stereoVols[0][0], stereoVols[0][1],
                dopplerFact * basePitch);
    }

    private void playSparkleEmit(float[] sparklePos) {
        float baseVol = mPerforming ? mPerfAttenuation : 1.0f;
        float[][] stereoVols = computeStereoVolumes(baseVol, sparklePos);
        mMainLauncher.playSparkleEmitSound(stereoVols[0][0], stereoVols[0][1]);
    }

    private void playSparkleCrackle(float[] sparklePos) {
        float baseVol = mPerforming ? mPerfAttenuation : 1.0f;
        float[][] stereoVols = computeStereoVolumes(baseVol, sparklePos);
        mMainLauncher.playSparkleCrackleSound(stereoVols[0][0], stereoVols[0][1]);
    }

    private void updateSparkleCrackle(float[] sparklePos) {
        float baseVol = mPerforming ? mPerfAttenuation : 1.0f;
        float[][] stereoVols = computeStereoVolumes(baseVol, sparklePos);
        mMainLauncher.updateSparkleCrackleSound(stereoVols[0][0], stereoVols[0][1]);
    }

    public void playFreeMoverTap(float[] moverPos) {
        float[][] stereoVols = computeStereoVolumes(1.0f, moverPos);
        int tapPlayer = mActiveMoverFlag == SHAPE ? 1 : 0;
        float rate = mActiveMoverFlag != GRAV_CIRC && mActiveMoverFlag != SHAPE ? 1.6f : 1.0f;
        mMainLauncher.playFreeMoverTapSound(stereoVols[0][0], stereoVols[0][1], rate, tapPlayer);
    }

    private float[][] computeStereoVolumes(float baseVolume, float[] objPos) {
        float[][] resultVecs = {new float[2], new float[3]};
        float[] fromEye = Vector.diffVV(objPos, mEye.mPos);
        resultVecs[1] = Vector.norm(fromEye);
        float eyeDist = Vector.length(fromEye);
        float distVol = baseVolume / (1.0f + (eyeDist * eyeDist / SOUND_FALLOFF_FACTOR));
        float minVol = 0.5f;
        float halfMinVol = 0.5f * minVol;
        float leftFact = 0.5f + halfMinVol + (0.5f - halfMinVol)
                * Vector.dotVV(mEye.mLeftEar, resultVecs[1]);
        float rightFact = 0.5f + halfMinVol + (0.5f - halfMinVol)
                * Vector.dotVV(mEye.mRightEar, resultVecs[1]);
        resultVecs[0][0] = leftFact * distVol;
        resultVecs[0][1] = rightFact * distVol;
        return resultVecs;
    }

    public void playUITick() {
        if (!mMusicPlaying) {
            mMainLauncher.playUITickSound();
        }
    }



    public void unPressAllButtons() {
        mEyeMoveModeB.unPress(); mEyeLockB.unPress(); mResetB.unPress(); mHelpInfoB.unPress();
        mEyeControlRight.unPress(); mEyeControlLeft.unPress(); mShapeB.unPress();
        mLight0B.unPress(); mLight1B.unPress(); mLight2B.unPress(); mGravCircB.unPress();
        mEmitterB.unPress(); mMusicB.unPress(); mMenuB.unPress(); mPauseB.unPress(); mCubeB.unPress();
        mIcosB.unPress(); mRndDodecB.unPress(); mRndIcosB.unPress(); mRedColorB.unPress();
        mYellowColorB.unPress(); mGreenColorB.unPress(); mCyanColorB.unPress();
        mBlueColorB.unPress(); mMagentColorB.unPress(); mWhiteColorB.unPress();
    }

    public void enableUI(boolean enable) {
        mDrawUI = enable;
        mMenuB.setEnabled(enable);
        mShapeB.setEnabled(enable);
        mLight0B.setEnabled(enable);
        mLight1B.setEnabled(enable);
        mLight2B.setEnabled(enable);
        mGravCircB.setEnabled(enable);
        mResetB.setEnabled(enable);
        mEyeLockB.setEnabled(enable);
        mHelpInfoB.setEnabled(enable);
        mMusicB.setEnabled(enable);
        mEmitterB.setEnabled(enable);
        mEyeMoveModeB.setEnabled(enable);
        mPauseB.setEnabled(enable);
        if (mPerforming) {
            mResetB.disable();
            mEmitterB.disable();
        }
        if (mSimPaused) {
            mResetB.disable();
            mMusicB.disable();
            mEmitterB.disable();
        }
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    public void togglePause() {
        mSimPaused = !mSimPaused;
        mEye.mSimPaused = mSimPaused;
        mMusicB.setEnabled(!mSimPaused);
        long currentTime = System.nanoTime();
        if (mSimPaused) {
            mPausedTime = currentTime;
            if (mPerforming) {
                mMainLauncher.pauseMusic();
                mPerfPaused = true;
            }
            if (mSparkling && mCracklePlayed) {
                mMainLauncher.pauseCrackle();
                mCracklePaused = true;
            }
            mResetB.unPress();
            mResetB.disable();
            mEmitterB.unPress();
            mEmitterB.disable();
            mActiveMover.setFree(true);
        } else {
            long timePaused = currentTime - mPausedTime;
            if (mBubblesUp) {
                for (BubblePoint bubble : mBubblePoints) bubble.incStartTime(timePaused);
                if (mBubblesGenerating) {
                    mShape.mBubEmitTime1 += timePaused;
                    mShape.mBubEmitTime2 += timePaused;
                }
            }
            if (mSparkling) {
                mSparklingStartTime += timePaused;
                if (mCracklePaused) {
                    mMainLauncher.resumeCrackle();
                    mCracklePaused = false;
                }
            }
            if (mSmoking) mSmokingStartTime += timePaused;
            if (mPerfPaused) {
                mPerfPaused = false;
                mMainLauncher.resumeMusic();
                mReqMusPosUpdate = true;
                mMusPosUpdateT = currentTime + MUS_POS_UPDATE_WAIT;
                mMusicStartTime += timePaused;
                mMusPosReadTime += timePaused;
                mShape.mPerfRotStartTime += timePaused;
            }
            if (!mPerforming) {
                mResetB.enable();
                mEmitterB.enable();
            }
        }
    }

}
