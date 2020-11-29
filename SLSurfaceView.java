package com.pathfinder.shapeandlight;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.VelocityTracker;

/**
 * Created by Lionden on 5/24/2016.
 */
@SuppressLint("ViewConstructor")
public class SLSurfaceView extends GLSurfaceView implements SLSimRenderer.ResourceGetter {
    private static final String LOG_TAG = SLSurfaceView.class.getSimpleName();

    // OpenGL renderer.
    protected SLSimRenderer mRenderer;
    // Touch event parameters.
    protected float mEyeDownX1;
    protected float mEyeDownY1;
    protected float mEyeDownX2;
    protected float mEyeDownY2;
    protected float mEyeControlPosLeftX;
    protected float mEyeControlPosLeftY;
    protected float mEyeControlPosRightX;
    protected float mEyeControlPosRightY;
    protected static final int STILL_EYE = 0;
    protected static final int MOVE_EYE = 1;
    protected static final int SLEW_EYE = 2;
    protected int mActiveMoverFlag;
    protected int mPtr1EyeMoveMode = STILL_EYE;
    protected int mPtr2EyeMoveMode = STILL_EYE;
    protected int mPointerID01;
    protected int mPointerID02;
    private static final float BASE_SWIPE_SPIN_FACTOR = 1.0f;
    private static final float POKE_FORCE = 0.2f;
    protected float mObjDownX;
    protected float mObjDownY;
    protected boolean mNoUIActive;
    protected VelocityTracker mVelTracker;
    protected float[] mPokeImpetus = new float[3];
    protected final long HOLD_TIME = 300;
    // Boolean for code to keep object button from cycling on closing long-press menu
    protected boolean mShapeMenuClosed = false;
    protected boolean mLightColMenuClosed = false;
    // Booleans to assist code detecting UI restoration tap release.
    protected boolean mMenuButtonReleased = false;
    protected boolean mShapeJustSpun = false;
    // Fields to pass in values to active object & shape manipulation callbacks.
    protected float mPx;
    protected float mPy;
    protected float[] mShapeRotAxis;
    protected float mShapeRotSpd;
    // Paused boolean to keep in the UI thread to prevent queued commands that are pause-disabled.
    protected boolean mSimPaused;
    // Boolean used to prevent 2 quick pointer-up events on the Menu button from double-calling the
    // mMainLauncher settings activity launch method, preventing a duplicate settings activity instance
    // from being launched.
    protected boolean mMenuLaunched = false;

    protected boolean mHelpOn;
    // Help dialog flags.
    protected static final int SETTINGS_HELP = 0;
    protected static final int OBJSELECT_HELP = 1;
    protected static final int RESET_HELP = 2;
    protected static final int EYELOCK_HELP = 3;
    protected static final int EYEMOVCONTR_HELP = 4;
    protected static final int EYEDIRCONTR_HELP = 5;
    protected static final int MUSIC_HELP = 6;
    protected static final int EMITTER_HELP = 7;
    protected static final int EYEMOVMODE_HELP = 8;
    protected static final int OBJMANIP_HELP = 9;
    protected static final int ORIENTAX_HELP = 10;
    protected static final int PAUSE_HELP = 11;
    protected static final int ABOUT_APP_DIALOG = 12;

    // Params for settings and state saving.
    // Flag strings for SharedPreferences storage/retrieval.
    protected static final String MUSIC_TRACK = "music_track_flag";
    protected static final String ACTIVE_MOVER = "active_mover_flag";
    protected static final String VISUALS_LEVEL = "visuals_settings_flag";
    protected static final String BUBBLES_GEN = "bubbles_generating_boolean";
    protected static final String PERFORM_HIDE_UI = "performance_hides_ui_boolean";
    protected static final String PERFORM_OFFSET_INT = "performance_ms_offset_int";

    protected static final String CTR_RING_SPRINGSQ = "center_ring_spring_squared_boolean";
    protected static final String CTR_RING_STRENGTH = "center_ring_gravity_strength_float";
    protected static final String MOV_RING_SPRINGSQ = "free_moving_ring_spring_squared_boolean";
    protected static final String MOV_RING_STRENGTH = "free_moving_ring_gravity_strength_float";
    protected static final String GRAV_RING_COUNT = "gravity_ring_count_int";
    protected static final String MOV_RING_EXCLUSIVE = "moving_ring_exclusive_effect_boolean";

    protected static final String LIGHT0_ON = "light_0_on_boolean";
    protected static final String LIGHT1_ON = "light_1_on_boolean";
    protected static final String LIGHT2_ON = "light_2_on_boolean";
    protected static final String LIGHT0_STROBE = "light_0_strobe_boolean";
    protected static final String LIGHT1_STROBE = "light_1_strobe_boolean";
    protected static final String LIGHT2_STROBE = "light_2_strobe_boolean";
    protected static final String LIGHT0_BRIGHT = "light_0_bright_boolean";
    protected static final String LIGHT1_BRIGHT = "light_1_bright_boolean";
    protected static final String LIGHT2_BRIGHT = "light_2_bright_boolean";

    protected static final String MIN_LIGHT_FACTOR = "minimum_background_lighting_factor_float";

    protected static final String SHAPE_STORED = "shape_stored_boolean";
    protected static final String SHAPE_TYPE = "shape_type_flag";
    protected static final String SHAPE_DECAL = "shape_decal_flag";
    protected static final String SHAPE_ROT_SPD = "shape_rotation_speed_float";

    protected static final String EYE_SENSITIVITY = "eye_sensitivity_flag";
    protected static final String EYE_FLIP_VERT = "eye_flip_vertical_boolean";
    protected static final String EYE_SLEW_FREE = "eye_free_slewing_boolean";
    protected static final String EYE_MOVE_FREE = "eye_free_moving_boolean";
    protected static final String EYE_OBJ_FOCUS = "eye_focused_object_flag";
    protected static final String EYE_MOVE_HORIZ = "eye_move_horizontal_boolean";
    protected static final String EYE_ALT_LOCK = "eye_alternative_lock_boolean";

    protected static final String SHOW_WELCOME_DIALOG = "show_welcome_dialog_boolean";
    protected static final String SHOW_INFO_BUTTON_DIALOG = "show_info_button_dialog_boolean";
    protected static final String ALT_SOUND_SYSTEM = "alternate_sound_system_boolean";
    protected static final String PLAY_UI_TICKS = "play_ui_ticks";
    protected static final String SAMPLING_RATE_INDEX = "sampling_rate_index_int";

    protected SLMainLauncher mLauncher;
    protected SharedPreferences mPrefs;

    // Renderer callbacks.
    protected Runnable mInitSparkler;
    protected Runnable mAreLightsAtOrigin;
    protected Runnable mInitEyeLock;
    protected Runnable mSetEyeControlPosLeft;
    protected Runnable mSetEyeControlPosRight;
    protected Runnable mSetCubeShape;
    protected Runnable mSetIcosShape;
    protected Runnable mSetRndDodecShape;
    protected Runnable mSetRndIcosShape;
    protected Runnable mSetActiveMover;
    protected Runnable mPokeMover;
    protected Runnable mResetMover;
    protected Runnable mResetEye;
    protected Runnable mToggleMusPerformance;
    protected Runnable mEnableUI;
    protected Runnable mSetNewObjPosVec;
    protected Runnable mSpinShape;
    protected Runnable mTogglePause;

    // Interface implemented by MainActivity to launch the menu/settings activity, popup dialogs,
    // and implement callbacks for sounds and music track to be played via the main thread.
    protected interface SLMainLauncher {

        void launchSLMenuActivity();

        void launchHelpPopup(int flag);

        void launchHelpButtonDialog();

        boolean isSplashTimeUp(long currentTime);

        void closeSplashScreen();

        void playBubbleSound(float leftV, float rightV, float rate);

        void playSparkleEmitSound(float leftV, float rightV);

        void playSparkleCrackleSound(float leftV, float rightV);

        void updateSparkleCrackleSound(float leftV, float rightV);

        void stopSparkleCrackleSound();

        void playFreeMoverTapSound(float leftV, float rightV, float rate, int tapPlayer);

        void playUITickSound();

        void playUITickSndDirect();

        void createMusicPlayer(int track, float volume);

        void updateMusicPlayingState();

        void updateMusicPosition();

        void startMusic();

        void stopMusic();

        void toggleMusic();

        void pauseMusic();

        void resumeMusic();

        void pauseCrackle();

        void resumeCrackle();

    }

    public SLSurfaceView(Context ctx, SLMainLauncher launcher) {
        super(ctx);
        setEGLContextClientVersion(2);
        mRenderer = new SLSimRenderer(ctx);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mRenderer.mResGetter = this;
        mLauncher = launcher;
        mRenderer.mMainLauncher = launcher;
        mInitSparkler = new Runnable() {
            @Override
            public void run() {
                mRenderer.initSparkler();
            }
        };
        mAreLightsAtOrigin = new Runnable() {
            @Override
            public void run() {
                mRenderer.areLightsAtOrigin();
            }
        };
        mInitEyeLock = new Runnable() {
            @Override
            public void run() {
                mRenderer.mEye.mSlewFree = !mRenderer.mEye.mSlewFree;
                mRenderer.mEye.mLockTransitCount = 0;
                if (mRenderer.mEye.mSlewFree && !mRenderer.mEye.mMoveFree) {
                    mRenderer.mEye.mMoveFree = true;
                }
                mRenderer.mEyeControlRight.unPress();
            }
        };
        mSetEyeControlPosRight = new Runnable() {
            @Override
            public void run() {
                mRenderer.mEyeControlPosRight.setPos(mEyeControlPosRightX, mEyeControlPosRightY);
            }
        };
        mSetEyeControlPosLeft = new Runnable() {
            @Override
            public void run() {
                mRenderer.mEyeControlPosLeft.setPos(mEyeControlPosLeftX, mEyeControlPosLeftY);
            }
        };
        mSetCubeShape = new Runnable() {
            @Override
            public void run() {
                mRenderer.mShape.setShapeType(Shape.CUBE);
            }
        };
        mSetIcosShape = new Runnable() {
            @Override
            public void run() {
                mRenderer.mShape.setShapeType(Shape.ICOS);
            }
        };
        mSetRndDodecShape = new Runnable() {
            @Override
            public void run() {
                mRenderer.mShape.setShapeType(Shape.RNDDODEC);
            }
        };
        mSetRndIcosShape = new Runnable() {
            @Override
            public void run() {
                mRenderer.mShape.setShapeType(Shape.RNDICOS);
            }
        };
        mSetActiveMover = new Runnable() {
            @Override
            public void run() {
                mRenderer.mActiveMover.setFree(true);
                mRenderer.setActiveMover(mActiveMoverFlag);
            }
        };
        mPokeMover = new Runnable() {
            @Override
            public void run() {
                mRenderer.mActiveMover.incVel(mPokeImpetus);
                mRenderer.playFreeMoverTap(mRenderer.mActiveMover.getPos());
            }
        };
        mResetMover = new Runnable() {
            @Override
            public void run() {
                mRenderer.mActiveMover.reset();
                mRenderer.areLightsAtOrigin();
            }
        };
        mResetEye = new Runnable() {
            @Override
            public void run() {
                mRenderer.mEye.defaultState();
            }
        };
        mToggleMusPerformance = new Runnable() {
            @Override
            public void run() {
                mRenderer.togglePerformance();
            }
        };
        mEnableUI = new Runnable() {
            @Override
            public void run() {
                mRenderer.enableUI(true);
            }
        };
        mSetNewObjPosVec = new Runnable() {
            @Override
            public void run() {
                mRenderer.setNewPositionVec(mPx, mPy);
            }
        };
        mSpinShape = new Runnable() {
            @Override
            public void run() {
                mRenderer.mShape.mAxis = mShapeRotAxis;
                mRenderer.mShape.mRotSpd = mShapeRotSpd;
            }
        };
        mTogglePause = new Runnable() {
            @Override
            public void run() {
                mRenderer.togglePause();
            }
        };
    }

    @Override
    public void onPause() {
        super.onPause();
        mRenderer.saveState(mPrefs);
    }


    @Override
    public void onResume() {
        super.onResume();
        mRenderer.resumeState(mPrefs);
        mSimPaused = false;
        mMenuLaunched = false;
    }

    public int[] getIntArrayResource(int resID) {
        return getResources().getIntArray(resID);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (rendererNullCheck()) {
            return true;
        }
        int action = event.getActionMasked();
        float tchX = event.getX();
        float tchY = event.getY();
        float tchX2;
        float tchY2;
        float scrnHeight = mRenderer.mHeight;
        float scrnWidth = mRenderer.mWidth;
        int id;
        int pointerIndex01;
        int pointerIndex02;
        float vX; float vY; float vMag;
        // Init tolerance parameter for near-screen-edge detection.
        float tol = scrnWidth * 0.025f;
        mHelpOn = mRenderer.mHelpOn;
        mActiveMoverFlag = mRenderer.mActiveMoverFlag;
        boolean isFree = mRenderer.mShape.isFree() && mRenderer.mLights[0].isFree()
                && mRenderer.mLights[1].isFree() && mRenderer.mLights[2].isFree()
                && mRenderer.mMobRing.isFree();
        boolean isOnMover;
        boolean isInEyeCtrlL;
        boolean isInEyeCtrlR;
        mNoUIActive = !mRenderer.mMenuB.isPressed() && !mRenderer.mShapeB.isPressed()
                && !mRenderer.mLight0B.isPressed() && !mRenderer.mLight1B.isPressed()
                && !mRenderer.mLight2B.isPressed() && !mRenderer.mGravCircB.isPressed()
                && !mRenderer.mCubeB.isPressed() && !mRenderer.mRndDodecB.isPressed()
                && !mRenderer.mIcosB.isPressed() && !mRenderer.mRndIcosB.isPressed()
                && !mRenderer.mRedColorB.isPressed() && !mRenderer.mGreenColorB.isPressed()
                && !mRenderer.mBlueColorB.isPressed() && !mRenderer.mYellowColorB.isPressed()
                && !mRenderer.mCyanColorB.isPressed() && !mRenderer.mMagentColorB.isPressed()
                && !mRenderer.mWhiteColorB.isPressed() && !mRenderer.mPauseB.isPressed()
                && !mRenderer.mResetB.isPressed() && !mRenderer.mEyeLockB.isPressed()
                && !mRenderer.mHelpInfoB.isPressed() && !mRenderer.mMusicB.isPressed()
                && !mRenderer.mEmitterB.isPressed() && !mRenderer.mEyeMoveModeB.isPressed()
                && !mRenderer.mEyeControlLeft.isPressed() && !mRenderer.mEyeControlRight.isPressed()
                && !mRenderer.mOrientAxesB.isPressed();
        mMenuButtonReleased = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                id = event.getPointerId(event.getActionIndex());
                // Reset the static button-was-pressed boolean to allow the SimRenderer method
                // "selectMoverOnTouch" to detect whether a button was pressed.
                SLButton.a_button_was_pressed = false;
                if (buttonDownDetection(tchX, scrnHeight - tchY, id)) return true;
                if (mVelTracker == null) mVelTracker = VelocityTracker.obtain();
                else mVelTracker.clear();
                mVelTracker.addMovement(event);
                // Eye control detection.
                isInEyeCtrlL = mRenderer.isInEyeControlLeft(tchX, scrnHeight - tchY, id);
                isInEyeCtrlR = mRenderer.isInEyeControlRight(tchX, scrnHeight - tchY, id);
                if (isInEyeCtrlL) {
                    if (mHelpOn) {
                        mLauncher.playUITickSndDirect();
                        mLauncher.launchHelpPopup(EYEMOVCONTR_HELP);
                        return true;
                    }
                    mPointerID01 = id;
                    mEyeDownX1 = tchX;
                    mEyeDownY1 = tchY;
                    mRenderer.mOnScrnEdgeLft = tchX < tol || tchY > scrnHeight - tol;
                    mPtr1EyeMoveMode = MOVE_EYE;
                    mRenderer.mEye.mMoving = true;
                    mRenderer.mEye.mAutoOrbit = false;
                    mEyeControlPosLeftX = tchX;
                    mEyeControlPosLeftY = scrnHeight - tchY;
                    queueEvent(mSetEyeControlPosLeft);
                } else if (isInEyeCtrlR) {
                    if (mHelpOn) {
                        mLauncher.playUITickSndDirect();
                        mLauncher.launchHelpPopup(EYEDIRCONTR_HELP);
                        return true;
                    }
                    mPointerID01 = id;
                    mEyeDownX1 = tchX;
                    mEyeDownY1 = tchY;
                    mRenderer.mOnScrnEdgeRt = tchX > scrnWidth - tol || tchY > scrnHeight - tol;
                    mPtr1EyeMoveMode = SLEW_EYE;
                    mRenderer.mEye.mMoving = true;
                    mRenderer.mEye.mSlewFree = true;
                    mRenderer.mEye.mMoveFree = true;
                    mEyeControlPosRightX = tchX;
                    mEyeControlPosRightY = scrnHeight - tchY;
                    queueEvent(mSetEyeControlPosRight);
                } else {
                    mPtr1EyeMoveMode = STILL_EYE;
                }
                if (mRenderer.selectMoverOnTouch(tchX, scrnHeight - tchY)) {
                    mObjDownX = tchX; mObjDownY = tchY;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                pointerIndex02 = event.getActionIndex();
                tchX2 = event.getX(pointerIndex02);
                tchY2 = event.getY(pointerIndex02);
                id = event.getPointerId(pointerIndex02);
                if (buttonDownDetection(tchX2, scrnHeight - tchY2, id)) return true;
                if (isFree) {
                    isInEyeCtrlL = mRenderer.isInEyeControlLeft(tchX2, scrnHeight - tchY2, id);
                    isInEyeCtrlR = mRenderer.isInEyeControlRight(tchX2, scrnHeight - tchY2, id);
                } else {
                    isInEyeCtrlL = false;
                    isInEyeCtrlR = false;
                }
                // Code to keep pointers straight, if id == 0 then it means pointerId02 is still
                // active, so this ACTION_POINTER_DOWN event should assign the id to pointerId01
                if (id == 0) {
                    mPointerID01 = id;
                    // if id == 0 and pointer 2 is still active, associate the new eye movement
                    // control with pointer 1.
                    if (isInEyeCtrlL) {
                        mEyeDownX1 = tchX2;
                        mEyeDownY1 = tchY2;
                        mRenderer.mOnScrnEdgeLft = tchX2 < tol || tchY2 > scrnHeight - tol;
                        mPtr1EyeMoveMode = MOVE_EYE;
                        mRenderer.mEye.mMoving = true;
                        mRenderer.mEye.mAutoOrbit = false;
                        mEyeControlPosLeftX = tchX2;
                        mEyeControlPosLeftY = scrnHeight - tchY2;
                        queueEvent(mSetEyeControlPosLeft);
                    } else if (isInEyeCtrlR) {
                        mEyeDownX1 = tchX2;
                        mEyeDownY1 = tchY2;
                        mRenderer.mOnScrnEdgeRt = tchX2 > scrnWidth - tol || tchY2 > scrnHeight - tol;
                        mPtr1EyeMoveMode = SLEW_EYE;
                        mRenderer.mEye.mMoving = true;
                        mRenderer.mEye.mSlewFree = true;
                        mRenderer.mEye.mMoveFree = true;
                        mEyeControlPosRightX = tchX2;
                        mEyeControlPosRightY = scrnHeight - tchY2;
                        queueEvent(mSetEyeControlPosRight);
                    } else {
                        mPtr1EyeMoveMode = STILL_EYE;
                    }
                } else if (id == 1) {
                    mPointerID02 = id;
                    // if id == 1 then proceed normally, associating the new eye control w/ ptr 2.
                    if (isInEyeCtrlL) {
                        mEyeDownX2 = tchX2;
                        mEyeDownY2 = tchY2;
                        mRenderer.mOnScrnEdgeLft = tchX2 < tol || tchY2 > scrnHeight - tol;
                        mPtr2EyeMoveMode = MOVE_EYE;
                        mRenderer.mEye.mMoving = true;
                        mEyeControlPosLeftX = tchX2;
                        mEyeControlPosLeftY = scrnHeight - tchY2;
                        queueEvent(mSetEyeControlPosLeft);
                    } else if (isInEyeCtrlR) {
                        mEyeDownX2 = tchX2;
                        mEyeDownY2 = tchY2;
                        mRenderer.mOnScrnEdgeRt = tchX2 > scrnWidth - tol || tchY2 > scrnHeight - tol;
                        mPtr2EyeMoveMode = SLEW_EYE;
                        mRenderer.mEye.mMoving = true;
                        mRenderer.mEye.mSlewFree = true;
                        mRenderer.mEye.mMoveFree = true;
                        mEyeControlPosRightX = tchX2;
                        mEyeControlPosRightY = scrnHeight - tchY2;
                        queueEvent(mSetEyeControlPosRight);
                    } else {
                        mPtr2EyeMoveMode = STILL_EYE;
                    }
                }
                // Otherwise if id is not 0 or 1 then it means this is an extraneous 3rd pointer,
                // so start no new eye control.
                break;
            case MotionEvent.ACTION_MOVE:
                mVelTracker.addMovement(event);
                // For moving touch events consider the touch "on mover" if the mover is not free,
                // i.e. being dragged, whether or not the actual touch coords are in the mover box,
                // to prevent the mover from "escaping" rapid grab gestures.
                isOnMover = mRenderer.selectMoverOnTouch(tchX, scrnHeight - tchY)
                        || !mRenderer.mActiveMover.isFree();
                if (isOnMover && !mHelpOn && mNoUIActive && !mRenderer.mEye.mMoving && !mSimPaused) {
                    if (event.getEventTime() - event.getDownTime() >= HOLD_TIME) {
                        mPx = tchX;
                        mPy = scrnHeight - tchY;
                        // Prepare to drag the object, but first remove any eye lock.
                        mRenderer.mEye.mSlewFree = true;
                        mRenderer.mEye.mMoveFree = true;
                        // Drag the active object.
                        mRenderer.mActiveMover.setFree(false);
                        mRenderer.mActiveMover.setVel(Vector.ZERO);
                        if (mActiveMoverFlag == SLSimRenderer.SHAPE) {
                            mRenderer.mShape.mRotSpd = 0.0f;
                        }
                        queueEvent(mSetNewObjPosVec);
                    } else if (mActiveMoverFlag == SLSimRenderer.SHAPE) {
                        // Give new spin rotation from swipe gesture.
                        mVelTracker.computeCurrentVelocity(1000);
                        vX = mVelTracker.getYVelocity();
                        vY = mVelTracker.getXVelocity();
                        vMag = (float) Math.sqrt(vX*vX + vY*vY);
                        vMag = vMag * BASE_SWIPE_SPIN_FACTOR / scrnHeight;
                        float[] newAxis = Vector.sumVV(
                                Vector.scaleSV(vX, mRenderer.mEye.mUvec),
                                Vector.scaleSV(vY, mRenderer.mEye.mVvec)
                        );
                        if (vMag == 0.0f) {
                            newAxis = Shape.INIT_AXIS;
                        }
                        mShapeRotAxis = Vector.norm(newAxis);
                        mShapeRotSpd = vMag;
                        queueEvent(mSpinShape);
                        mShapeJustSpun = true;
                    }
                }
                // Eye movement and slewing.
                pointerIndex01 = event.findPointerIndex(mPointerID01);
                pointerIndex02 = event.findPointerIndex(mPointerID02);
                if (pointerIndex01 != MotionEvent.INVALID_POINTER_ID) {
                    tchX = event.getX(pointerIndex01);
                    tchY = event.getY(pointerIndex01);
                    if (mPtr1EyeMoveMode == MOVE_EYE) {
                        mRenderer.mOnScrnEdgeLft = tchX < tol || tchY > scrnHeight - tol;
                        mRenderer.mEye.moveEye(tchX - mEyeDownX1, mEyeDownY1 - tchY);
                        mEyeControlPosLeftX = tchX;
                        mEyeControlPosLeftY = scrnHeight - tchY;
                        queueEvent(mSetEyeControlPosLeft);
                    } else if (mPtr1EyeMoveMode == SLEW_EYE) {
                        mRenderer.mOnScrnEdgeRt = tchX > scrnWidth - tol || tchY > scrnHeight - tol;
                        mRenderer.mEye.slewEye(tchX - mEyeDownX1, mEyeDownY1 - tchY);
                        mEyeControlPosRightX = tchX;
                        mEyeControlPosRightY = scrnHeight - tchY;
                        queueEvent(mSetEyeControlPosRight);
                    }
                }
                if (pointerIndex02 != MotionEvent.INVALID_POINTER_ID) {
                    tchX2 = event.getX(pointerIndex02);
                    tchY2 = event.getY(pointerIndex02);
                    if (mPtr2EyeMoveMode == MOVE_EYE) {
                        mRenderer.mOnScrnEdgeLft = tchX2 < tol || tchY2 > scrnHeight - tol;
                        mRenderer.mEye.moveEye(tchX2 - mEyeDownX2, mEyeDownY2 - tchY2);
                        mEyeControlPosLeftX = tchX2;
                        mEyeControlPosLeftY = scrnHeight - tchY2;
                        queueEvent(mSetEyeControlPosLeft);
                    } else if (mPtr2EyeMoveMode == SLEW_EYE) {
                        mRenderer.mOnScrnEdgeRt = tchX2 > scrnWidth - tol || tchY2 > scrnHeight - tol;
                        mRenderer.mEye.slewEye(tchX2 - mEyeDownX2, mEyeDownY2 - tchY2);
                        mEyeControlPosRightX = tchX2;
                        mEyeControlPosRightY = scrnHeight - tchY2;
                        queueEvent(mSetEyeControlPosRight);
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                // Eye movement control release detection.
                id = event.getPointerId(event.getActionIndex());
                boolean eyeCtrlLeftReleased = false;
                boolean eyeCtrlRightReleased = false;
                if (id == mPointerID01) {
                    if (mPtr1EyeMoveMode == MOVE_EYE) {
                        mRenderer.mEye.moveEye(0.0f, 0.0f);
                        mPtr1EyeMoveMode = STILL_EYE;
                        mRenderer.mEyeControlLeft.unPress();
                        eyeCtrlLeftReleased = true;
                        if (mPtr2EyeMoveMode == STILL_EYE) {
                            mRenderer.mEye.mMoving = false;
                        }
                    } else if (mPtr1EyeMoveMode == SLEW_EYE) {
                        mRenderer.mEye.slewEye(0.0f, 0.0f);
                        mPtr1EyeMoveMode = STILL_EYE;
                        mRenderer.mEyeControlRight.unPress();
                        eyeCtrlRightReleased = true;
                        if (mPtr2EyeMoveMode == STILL_EYE) {
                            mRenderer.mEye.mMoving = false;
                        }
                    }
                }
                if (id == mPointerID02) {
                    if (mPtr2EyeMoveMode == MOVE_EYE) {
                        mRenderer.mEye.moveEye(0.0f, 0.0f);
                        mPtr2EyeMoveMode = STILL_EYE;
                        mRenderer.mEyeControlLeft.unPress();
                        eyeCtrlLeftReleased = true;
                        if (mPtr1EyeMoveMode == STILL_EYE) {
                            mRenderer.mEye.mMoving = false;
                        }
                    } else if (mPtr2EyeMoveMode == SLEW_EYE) {
                        mRenderer.mEye.slewEye(0.0f, 0.0f);
                        mPtr2EyeMoveMode = STILL_EYE;
                        mRenderer.mEyeControlRight.unPress();
                        eyeCtrlRightReleased = true;
                        if (mPtr1EyeMoveMode == STILL_EYE) {
                            mRenderer.mEye.mMoving = false;
                        }
                    }
                }
                buttonReleaseDetection(id);
                // Detect release of tap intended to bring UI back.
                if (!mRenderer.mDrawUI && !mMenuButtonReleased && !mRenderer.mEye.mMoving
                        && !eyeCtrlLeftReleased && !eyeCtrlRightReleased) {
                    queueEvent(mEnableUI);
                }
                break;
            case MotionEvent.ACTION_UP:
                id = event.getPointerId(event.getActionIndex());
                buttonReleaseDetection(id);
                float dTchX = tchX - mObjDownX;
                float dTchY = tchY - mObjDownY;
                float dTchR = (float) Math.sqrt(dTchX * dTchX + dTchY * dTchY);
                isOnMover = mRenderer.selectMoverOnTouch(tchX, scrnHeight - tchY)
                        || !mRenderer.mActiveMover.isFree();
                // Set tolerance for touch movement radius for "poke" input on shape object.
                tol = scrnHeight * 0.0125f;
                if (isOnMover && mNoUIActive && !mRenderer.mEye.mMoving) {
                    if (event.getEventTime() - event.getDownTime() < HOLD_TIME && dTchR <= tol) {
                        // Add "poke" impetus.
                        mPokeImpetus = Vector.scaleSV (
                                POKE_FORCE,
                                Vector.norm(Vector.diffVV(mRenderer.mActiveMover.getPos(),
                                        mRenderer.mEye.mPos))
                        );
                        queueEvent(mPokeMover);
                    } else if (event.getEventTime() - event.getDownTime() > HOLD_TIME
                            && !mSimPaused) {
                        // Otherwise release the object with "fling" velocity.
                        setFlingVelocity(event);
                    }
                }
                // Detect release of tap intended to bring UI back.
                if (!mRenderer.mDrawUI && !isOnMover && !mShapeJustSpun
                        && !mMenuButtonReleased
                        && !mRenderer.mEyeControlLeft.isPointerReleasingNotLong(id)
                        && !mRenderer.mEyeControlRight.isPointerReleasingNotLong(id)) {
                    queueEvent(mEnableUI);
                }
                mShapeJustSpun = false;
                mPtr1EyeMoveMode = STILL_EYE;
                mPtr2EyeMoveMode = STILL_EYE;
                mRenderer.mEye.slewEye(0.0f, 0.0f).moveEye(0.0f, 0.0f);
                mRenderer.mEye.mMoving = false;
                mRenderer.mActiveMover.setFree(true);
                mRenderer.unPressAllButtons();
        }
        return true;
    }

    // Set a new velocity on the active object from a "fling" gesture.
    private void setFlingVelocity(MotionEvent event) {
        float wClip = mRenderer.getActiveObjectWclip();
        float scrnHeight = mRenderer.mHeight;
        float velFactor = wClip * 0.0055f / scrnHeight;
        float velMax = 10.0f / velFactor;
        mVelTracker.addMovement(event);
        mVelTracker.computeCurrentVelocity(1000, velMax);
        float vX = mVelTracker.getXVelocity() * velFactor;
        float vY = mVelTracker.getYVelocity() * -velFactor;
        // New velocity vector.
        float[] newVel = Vector.sumVV(
                Vector.scaleSV(vX, mRenderer.mEye.mUvec),
                Vector.scaleSV(vY, mRenderer.mEye.mVvec)
        );
        mRenderer.mActiveMover.setVel(newVel);
        if (Vector.areEqual(newVel, Vector.ZERO)) {
            queueEvent(mAreLightsAtOrigin);
        }
    }

    // Handle button detection for down events. Return true if in help mode and a button was pressed.
    private boolean buttonDownDetection(float x, float y, int id) {
        // Object button state detection.
        if (mActiveMoverFlag == SLSimRenderer.SHAPE) {
            if (mRenderer.mShapeB.initInsideTap(x, y, id, mHelpOn) && mHelpOn) {
                mLauncher.playUITickSndDirect();
                mLauncher.launchHelpPopup(OBJSELECT_HELP);
                return true;
            }
            mShapeMenuClosed = mRenderer.mShapeMenuOn;
        }
        if (mActiveMoverFlag == SLSimRenderer.LIGHT_0) {
            if (mRenderer.mLight0B.initInsideTap(x, y, id, mHelpOn) && mHelpOn) {
                mLauncher.playUITickSndDirect();
                mLauncher.launchHelpPopup(OBJSELECT_HELP);
                return true;
            }
            mLightColMenuClosed = mRenderer.mLight0MenuOn;
        }
        if (mActiveMoverFlag == SLSimRenderer.LIGHT_1) {
            if (mRenderer.mLight1B.initInsideTap(x, y, id, mHelpOn) && mHelpOn) {
                mLauncher.playUITickSndDirect();
                mLauncher.launchHelpPopup(OBJSELECT_HELP);
                return true;
            }
            mLightColMenuClosed = mRenderer.mLight1MenuOn;
        }
        if (mActiveMoverFlag == SLSimRenderer.LIGHT_2) {
            if (mRenderer.mLight2B.initInsideTap(x, y, id, mHelpOn) && mHelpOn) {
                mLauncher.playUITickSndDirect();
                mLauncher.launchHelpPopup(OBJSELECT_HELP);
                return true;
            }
            mLightColMenuClosed = mRenderer.mLight2MenuOn;
        }
        if (mActiveMoverFlag == SLSimRenderer.GRAV_CIRC) {
            if (mRenderer.mGravCircB.initInsideTap(x, y, id, mHelpOn) && mHelpOn) {
                mLauncher.playUITickSndDirect();
                mLauncher.launchHelpPopup(OBJSELECT_HELP);
                return true;
            }
        }
        // "Pop-up menu" button state detection.
        if (mRenderer.mShapeMenuOn) {
            mRenderer.mCubeB.initInsideTap(x, y, id, mHelpOn);
            mRenderer.mIcosB.initInsideTap(x, y, id, mHelpOn);
            mRenderer.mRndDodecB.initInsideTap(x, y, id, mHelpOn);
            mRenderer.mRndIcosB.initInsideTap(x, y, id, mHelpOn);
            mRenderer.mShapeMenuOn = mRenderer.mCubeB.isPressed()
                    || mRenderer.mIcosB.isPressed() || mRenderer.mRndDodecB.isPressed()
                    || mRenderer.mRndIcosB.isPressed();
        }
        if (mRenderer.mLight0MenuOn) {
            mRenderer.mLight0MenuOn = colorPopupButtonDetection(x, y, id);
        }
        if (mRenderer.mLight1MenuOn) {
            mRenderer.mLight1MenuOn = colorPopupButtonDetection(x, y, id);
        }
        if (mRenderer.mLight2MenuOn) {
            mRenderer.mLight2MenuOn = colorPopupButtonDetection(x, y, id);
        }
        // Regular button state detection, using boolean returned to activate help screen if in
        // help mode.
        if (mRenderer.mMenuB.initInsideTap(x, y, id, mHelpOn) && mHelpOn) {
            mLauncher.playUITickSndDirect();
            mLauncher.launchHelpPopup(SETTINGS_HELP);
            return true;
        }
        if (mRenderer.mResetB.initInsideTap(x, y, id, mHelpOn) && mHelpOn) {
            mLauncher.playUITickSndDirect();
            mLauncher.launchHelpPopup(RESET_HELP);
            return true;
        }
        if (mRenderer.mEyeLockB.initInsideTap(x, y, id, mHelpOn) && mHelpOn) {
            mLauncher.playUITickSndDirect();
            mLauncher.launchHelpPopup(EYELOCK_HELP);
            return true;
        }
        if (mRenderer.mMusicB.initInsideTap(x, y, id, mHelpOn) && mHelpOn) {
            mLauncher.playUITickSndDirect();
            mLauncher.launchHelpPopup(MUSIC_HELP);
            return true;
        }
        if (mRenderer.mEmitterB.initInsideTap(x, y, id, mHelpOn) && mHelpOn) {
            mLauncher.playUITickSndDirect();
            mLauncher.launchHelpPopup(EMITTER_HELP);
            return true;
        }
        if (mRenderer.mEyeMoveModeB.initInsideTap(x, y, id, mHelpOn) && mHelpOn) {
            mLauncher.playUITickSndDirect();
            mLauncher.launchHelpPopup(EYEMOVMODE_HELP);
            return true;
        }
        if (mRenderer.mPauseB.initInsideTap(x, y, id, mHelpOn) && mHelpOn) {
            mLauncher.playUITickSndDirect();
            mLauncher.launchHelpPopup(PAUSE_HELP);
            return true;
        }
        if (mRenderer.mObjManipHelpB.initInsideTap(x, y, id, mHelpOn) && mHelpOn) {
            mLauncher.playUITickSndDirect();
            mLauncher.launchHelpPopup(OBJMANIP_HELP);
            return true;
        }
        if (mRenderer.mOrientAxesB.initInsideTap(x, y, id, mHelpOn) && mHelpOn) {
            mLauncher.playUITickSndDirect();
            mLauncher.launchHelpPopup(ORIENTAX_HELP);
            return true;
        }
        mRenderer.mHelpInfoB.initInsideTap(x, y, id, mHelpOn);
        return false;
    }

    // Handle button release detection for up events.
    private void buttonReleaseDetection(int id) {
        // Set "pop-up menu" button actions first.
        if (mRenderer.mShapeMenuOn) {
            if (mRenderer.mCubeB.isPointerReleasingNotLong(id)) {
                queueEvent(mSetCubeShape);
                mLauncher.playUITickSndDirect();
            }
            if (mRenderer.mIcosB.isPointerReleasingNotLong(id)) {
                queueEvent(mSetIcosShape);
                mLauncher.playUITickSndDirect();
            }
            if (mRenderer.mRndDodecB.isPointerReleasingNotLong(id)) {
                queueEvent(mSetRndDodecShape);
                mLauncher.playUITickSndDirect();
            }
            if (mRenderer.mRndIcosB.isPointerReleasingNotLong(id)) {
                queueEvent(mSetRndIcosShape);
                mLauncher.playUITickSndDirect();
            }
        }
        if (mRenderer.mLight0MenuOn) {
            int color = colorPopupButtonAction(id);
            if (color != LightPoint.BLANK) {
                mRenderer.mLights[0].setColor(color);
                queueEvent(mAreLightsAtOrigin);
                mLauncher.playUITickSndDirect();
            }
        }
        if (mRenderer.mLight1MenuOn) {
            int color = colorPopupButtonAction(id);
            if (color != LightPoint.BLANK) {
                mRenderer.mLights[1].setColor(color);
                queueEvent(mAreLightsAtOrigin);
                mLauncher.playUITickSndDirect();
            }
        }
        if (mRenderer.mLight2MenuOn) {
            int color = colorPopupButtonAction(id);
            if (color != LightPoint.BLANK) {
                mRenderer.mLights[2].setColor(color);
                queueEvent(mAreLightsAtOrigin);
                mLauncher.playUITickSndDirect();
            }
        }
        // Then the rest of the buttons.
        if (mRenderer.mEyeMoveModeB.isPointerReleasingNotLong(id) && !mHelpOn) {
            mRenderer.mEye.mMovHorizontal = !mRenderer.mEye.mMovHorizontal;
            mLauncher.playUITickSndDirect();
        }
        if (mRenderer.mEyeLockB.isPointerReleasingNotLong(id) && !mHelpOn) {
            queueEvent(mInitEyeLock);
            mLauncher.playUITickSndDirect();

        }
        if (mRenderer.mResetB.isPointerReleasingNotLong(id) && !mHelpOn) {
            if (!mSimPaused) {
                queueEvent(mResetMover);
                mLauncher.playUITickSndDirect();
            }
        }
        if (mRenderer.mOrientAxesB.isPointerReleasingNotLong(id) && !mHelpOn) {
            queueEvent(mResetEye);
            mLauncher.playUITickSndDirect();
        }
        if (mRenderer.mEmitterB.isPointerReleasingNotLong(id) && !mHelpOn) {
            if (!mSimPaused) queueEvent(mInitSparkler);
        }
        if (mRenderer.mMusicB.isPointerReleasingNotLong(id) && !mHelpOn) {
            if (!mSimPaused) queueEvent(mToggleMusPerformance);
        }
        if (mRenderer.mPauseB.isPointerReleasingNotLong(id) && !mHelpOn) {
            mSimPaused = !mSimPaused;
            queueEvent(mTogglePause);
            mLauncher.playUITickSndDirect();
        }
        // Detect whether the menu button is being released, transmitting the boolean to code
        // detecting release of tap intended to restore the UI. Used to prevent release of the
        // long-pressed menu button itself from restoring the UI.
        mMenuButtonReleased = mRenderer.mMenuB.isPointerThatPressed(id);
        // Then proceed with normal menu button release detection for its function.
        if (mRenderer.mMenuB.isPointerReleasingNotLong(id) && !mHelpOn && !mMenuLaunched) {
            mMenuLaunched = true;
            mLauncher.launchSLMenuActivity();
        }
        if (mRenderer.mHelpInfoB.isPointerReleasingNotLong(id)) {
            toggleHelpMode();
            mLauncher.playUITickSndDirect();
        }
        // Use menu closed booleans to avoid cycling on closing the long-press menus.
        if (mRenderer.mShapeB.isPointerReleasingNotLong(id) && !mShapeMenuClosed && !mHelpOn) {
            mActiveMoverFlag = SLSimRenderer.SHAPE;
            queueEvent(mSetActiveMover);
            mLauncher.playUITickSndDirect();
        }
        if (mRenderer.mLight0B.isPointerReleasingNotLong(id) && !mLightColMenuClosed && !mHelpOn) {
            mActiveMoverFlag = SLSimRenderer.LIGHT_0;
            queueEvent(mSetActiveMover);
            mLauncher.playUITickSndDirect();
        }
        if (mRenderer.mLight1B.isPointerReleasingNotLong(id) && !mLightColMenuClosed && !mHelpOn) {
            mActiveMoverFlag = SLSimRenderer.LIGHT_1;
            queueEvent(mSetActiveMover);
            mLauncher.playUITickSndDirect();
        }
        if (mRenderer.mLight2B.isPointerReleasingNotLong(id) && !mLightColMenuClosed && !mHelpOn) {
            mActiveMoverFlag = SLSimRenderer.LIGHT_2;
            queueEvent(mSetActiveMover);
            mLauncher.playUITickSndDirect();
        }
        if (mRenderer.mGravCircB.isPointerReleasingNotLong(id) && !mHelpOn) {
            mActiveMoverFlag = SLSimRenderer.GRAV_CIRC;
            queueEvent(mSetActiveMover);
            mLauncher.playUITickSndDirect();
        }
    }

    // Detect touches on color popup buttons, returning true if any are touched.
    private boolean colorPopupButtonDetection(float x, float y, int id) {
        mRenderer.mRedColorB.initInsideTapHex(x, y, id);
        mRenderer.mYellowColorB.initInsideTapHex(x, y, id);
        mRenderer.mGreenColorB.initInsideTapHex(x, y, id);
        mRenderer.mCyanColorB.initInsideTapHex(x, y, id);
        mRenderer.mBlueColorB.initInsideTapHex(x, y, id);
        mRenderer.mMagentColorB.initInsideTapHex(x, y, id);
        mRenderer.mWhiteColorB.initInsideTapHex(x, y, id);
        return mRenderer.mRedColorB.isPressed() || mRenderer.mYellowColorB.isPressed()
                || mRenderer.mGreenColorB.isPressed() || mRenderer.mCyanColorB.isPressed()
                || mRenderer.mBlueColorB.isPressed() || mRenderer.mMagentColorB.isPressed()
                || mRenderer.mWhiteColorB.isPressed();
    }

    // Set color popup button actions, returning the color to assign the affected light object.
    private int colorPopupButtonAction(int id) {
        int color = LightPoint.BLANK;
        if (mRenderer.mRedColorB.isPointerReleasingNotLong(id)) {
            color = LightPoint.RED;
        }
        if (mRenderer.mYellowColorB.isPointerReleasingNotLong(id)) {
            color = LightPoint.YELLOW;
        }
        if (mRenderer.mGreenColorB.isPointerReleasingNotLong(id)) {
            color = LightPoint.GREEN;
        }
        if (mRenderer.mCyanColorB.isPointerReleasingNotLong(id)) {
            color = LightPoint.CYAN;
        }
        if (mRenderer.mBlueColorB.isPointerReleasingNotLong(id)) {
            color = LightPoint.BLUE;
        }
        if (mRenderer.mMagentColorB.isPointerReleasingNotLong(id)) {
            color = LightPoint.MAGENTA;
        }
        if (mRenderer.mWhiteColorB.isPointerReleasingNotLong(id)) {
            color = LightPoint.WHITE;
        }
        return color;
    }

    private void toggleHelpMode() {
        mHelpOn = !mHelpOn;
        mRenderer.mHelpOn = mHelpOn;
        mRenderer.mObjManipHelpB.setEnabled(mHelpOn);
        if (mHelpOn) {
            mLauncher.launchHelpButtonDialog();
        }
    }

    // Do null check for the last objects instantiated in the renderer object to avoid touch events
    // that may occur before full initializations causing null pointer exceptions.
    private boolean rendererNullCheck() {
        return  mRenderer.mEyeControlPosLeft == null || mRenderer.mEyeControlPosRight == null;
    }


}
