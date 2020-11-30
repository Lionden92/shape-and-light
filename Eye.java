package com.pathfinder.shapeandlight;

import android.content.SharedPreferences;
import android.opengl.Matrix;
import android.util.Log;

/**
 * Created by Lionden on 5/25/2016.
 */

@SuppressWarnings("WeakerAccess")
public class Eye {
    private static final String LOG_TAG = Eye.class.getSimpleName();
    private static final float[] INIT_POSITION = {0.0f, 21.4f, 80.0f};
    private static final float INIT_VIEW_ANGLE = -15.0f;
    private static final float[] INIT_VIEWPOINT = {0.0f, 0.0f, -1.0f, 1.0f};
    private static float[] init_rotated_viewpoint = new float[4];
    private static final float[] INIT_VELOCITY = {0.0f, 0.0f, 0.0f};
    private static final float[] INIT_AXIS = {0.0f, 1.0f, 0.0f};
    private static final float AXIS_MOVE_LIMIT = 0.99f;
    private static final int SLAVE_TRANSIT_FRAMES = 60;
    private static final int LOCK_TRANSIT_FRAMES = 20;
    private static final int PASS_ORB_TRANS_FRAMES = 30;
    private static final float[] INIT_CHASE_POS = {0.0f, 5.0f, 60.0f};
    private static final float AUTO_ORBIT_DIST = 65.0f;
    private static final double AUTO_ORBIT_X_PER = 60000;
    private static final float AUTO_ORBIT_X_AMP = 40.0f;
    private static final double AUTO_ORBIT_Y_PER = 30000;
    private static final float AUTO_ORBIT_Y_AMP = 30.0f;
    private static final double AUTO_ORBIT_D_PER = 30000;
    private static final float AUTO_ORBIT_D_AMP = 50.0f;
    private static final double AUTO_ORBIT_R_PER = 120000;
    private static final float AUTO_ORBIT_R_AMP = 0.83f;
    // Maximum angle in degs allowed between inter-frame chase unit views for active auto-orbiting.
    private static final float ACTIVE_ORBIT_MAX_ANG = 1.9f;
    private static final float ACTIVE_ORB_MX_ANG_DELT = 0.14f;
    // Max angle allowed between inter-frame chase unit views for smooth transition to passive speed.
    private static final float PASSIVE_TRANSITION_MAX_ANG = 45.0f;
    // Auto-orbit cosine limit for treating a lockViewPoint as 'vertical', with a 'sigma' of 2 deg.
    private static final float AUTO_ORBIT_VERT_COS = 0.99939f;
    // Vector defining the eye's position in world space.
    protected volatile float[] mPos;
    // Vector defining the point the eye is looking towards.
    protected volatile float[] mViewPt;
    // Vector holding the view point orientation relative to eye position.
    protected volatile float[] mViewPtOrigin;
    // Vectors holding position of "ears" for sounds playback params.
    protected float[] mEarOrigin;
    protected float[] mRightEar;
    protected float[] mLeftEar;
    // Vector setting eye's control movement, defined in eye space.
    protected volatile float[] mCtrlMovVec;
    // Persistent eye velocity param used for Doppler calcs.
    protected float[] mDopplerVel;
    // Scale factor for movement control. Actual value set by SLSimRenderer in onSurfaceChanged.
    protected static float movScale = 0.004f;
    // Vector setting the eye's slew axis, defined in eye space.
    protected volatile float[] mLocalAxis;
    // Slew speed.
    protected volatile float mCtrlRotSpd = 0.0f;
    // slew control scale factor. Actual value set by SLSimRenderer in onSurfaceChanged.
    protected static float slewScale = 0.005f;
    // Mat4 providing the OpenGL view matrix.
    protected float[] mViewM = new float[16];
    // Vector holding point the view is locked to.
    protected float[] mLockViewPoint;
    // Vector holding velocity of the locked-on object for Doppler calcs.
    protected float[] mLockVelDoppler = INIT_VELOCITY;
    // Vector holding the chase position relative to locked object when slaved.
    protected float[] mChasePos;
    protected boolean mInitChase = false;
    // Boolean for whether the locked object has just changed.
    protected volatile boolean mLockObjChanged;
    // Auto-orbit boolean initially set to true but can be undone by UI input.
    protected volatile boolean mAutoOrbit;
    protected double mAutoOrbitTime;
    // Passive auto-orbit boolean, made true if the chase unit view exceeds a max angular velocity.
    protected boolean mActiveAutoOrbit;
    // Unit vector defining view direction in chased object space.
    protected float[] mChaseUnitViewPt;
    // The previous frame's chase unit view used to determine the view angle velocity..
    protected float[] mPrevChaseUnitV;
    protected float mPrevChaseVwPtDeltAng;
    // Axis used in the passive auto-orbit, computed once at runtime.
    protected float[] mPassOrbAx = Vector.V;
    protected boolean mInitPassiveAutoOrbit;
    protected float mInitialPassiveRotSpd;
    protected boolean mSimPaused;
    // Counters for computing smooth transitions to lock and chase modes.
    protected int mChaseTransitCount;
    protected int mLockTransitCount;
    protected int mPassOrbTransitCount;
    // Boolean for whether the eye view point is "free".
    protected volatile boolean mSlewFree = true;
    // Boolean for whether the eye position is "free".
    protected volatile boolean mMoveFree = true;
    // Flag for which FreeMover object the eye is locked to if not free.
    protected volatile int mFocusedMoverFlag;
    // Boolean for whether the eye is moving viewpoint or position.
    protected volatile boolean mMoving = false;
    // Boolean for whether the eye position movement is horizontal, if not it's vertical.
    protected volatile boolean mMovHorizontal = true;
    // Boolean for whether to flip the vertical slew access.
    protected boolean mFlipVert;
    // The 3 orthonormal vectors defining eye space.
    protected volatile float[] mUvec;
    protected volatile float[] mVvec;
    protected volatile float[] mWvec;


    public Eye() {
        mPos = INIT_POSITION;
        mCtrlMovVec = INIT_VELOCITY;
        mDopplerVel = INIT_VELOCITY;
        mLocalAxis = INIT_AXIS;
        init_rotated_viewpoint = rotateVec(INIT_VIEWPOINT, Vector.U, INIT_VIEW_ANGLE);
        // Clone instead of straight equals to separate the array reference.
        mViewPtOrigin = init_rotated_viewpoint.clone();
        mViewPt = Vector.sumVV(mPos, mViewPtOrigin);
        mEarOrigin = Vector.norm(Vector.crossVV(mViewPtOrigin, Vector.V));
        mRightEar = mEarOrigin;
        mLeftEar = Vector.negV(mEarOrigin);
        mSimPaused = false;
        // Compute the OpenGL view matrix.
        setViewMatrix();
    }


    // Set the OpenGL view matrix.
    private void setViewMatrix() {
        Matrix.setLookAtM(mViewM, 0,
                mPos[0], mPos[1], mPos[2],
                mViewPt[0], mViewPt[1], mViewPt[2],
                Vector.V[0], Vector.V[1], Vector.V[2]
        );
    }

    // Update the eye's per-frame position and view point in world space.
    public void perFrameUpdate(float frameFactor) {
        // Do no update if framerate has stalled.
        if (frameFactor > SLSimRenderer.MAX_FRAMETIME_FACTOR) return;
        computeUnitBasis();
        float dotWithVert = Vector.dotVV(mViewPtOrigin, Vector.V);
        boolean atAxisLimit = Math.abs(dotWithVert) > AXIS_MOVE_LIMIT;
        float[] dA = new float[3];
        if (atAxisLimit) {
            // Compute vector component going in the direction of the axis.
            if (dotWithVert > 0) {
                dA = Vector.diffVV(mViewPtOrigin, Vector.V);
            } else {
                dA = Vector.sumVV(mViewPtOrigin, Vector.V);
            }
            dA = Vector.norm(dA);
        }
        // Increment eye movement.
        // Convert the control move vector which is defined in eye space to world space and pass it
        // as a velocity for eye movement.
        float[] ctrlVel = Vector.transformFrom(mCtrlMovVec, mUvec, mVvec, mWvec);
        // Correction if at an axis limit while locked.
        if (atAxisLimit && !mSlewFree) {
            // Remove any positive component of "dA" from vector "ctrlVel".
            ctrlVel = Vector.subtractComp(ctrlVel, dA);
        }
        // Correction if locked and getting too close to the object.
        if (!mSlewFree) {
            float closeEnough = 15.0f;
            float[] toLock = Vector.diffVV(mLockViewPoint, mPos);
            float[] normToLock = Vector.norm(toLock);
            if (Vector.length(toLock) < closeEnough) {
                ctrlVel = Vector.subtractComp(ctrlVel, normToLock);
            }
        }
        mDopplerVel = ctrlVel;
        float[] frameCtrlVel = Vector.scaleSV(frameFactor, ctrlVel);
        if (mMoveFree) {
            mPos = Vector.sumVV(mPos, frameCtrlVel);
        } else {
            // Eye is in chase mode.
            // Add the velocity of the chased object to provide the total velocity for Doppler calcs.
            mDopplerVel = Vector.sumVV(mDopplerVel, mLockVelDoppler);
            // Set the chase view unit vec for use in auto-orbit.
            float[] chaseUnitViewPt = new float[] {mLockViewPoint[0], 0.0f, mLockViewPoint[2]};
            chaseUnitViewPt = Vector.areFloatsEqual(Vector.length(chaseUnitViewPt), 0.0f)
                    ? Vector.ZERO : Vector.norm(chaseUnitViewPt);
            // Boolean for whether the lock view point is near-vertical.
            boolean vertLockViewPt;
            if (Vector.areEqual(mLockViewPoint, Vector.ZERO)) {
                vertLockViewPt = true;
            } else {
                vertLockViewPt = Math.abs(Vector.dotVV(Vector.norm(mLockViewPoint), Vector.V))
                        > AUTO_ORBIT_VERT_COS;
            }
            if (mInitChase) {
                mChaseTransitCount = 0;
                // Set up initial chase position relative to the object.
                mChasePos = INIT_CHASE_POS.clone();
                mInitChase = false;
                // Auto-orbit inits.
                mAutoOrbit = true;
                mActiveAutoOrbit = true;
                mAutoOrbitTime = 0.0;
                mChaseUnitViewPt = vertLockViewPt ? Vector.W : chaseUnitViewPt.clone();
                mPrevChaseUnitV = mChaseUnitViewPt.clone();
                mPrevChaseVwPtDeltAng = 0.0f;
                mInitPassiveAutoOrbit = true;
                mLockObjChanged = false;
            }
            if (mAutoOrbit && !mSimPaused) {
                // Compute delta angle in degrees between current and previous frame's chase unit
                // view, adjusted for frame rate via the frameFactor.
                // Insure argument to Math.acos doesn't return NaN.
                float acosArg = Math.max(-1.0f,
                        Math.min(1.0f, Vector.dotVV(chaseUnitViewPt, mPrevChaseUnitV)));
                float chaseVwPtDeltAng = (float)((180.0/Math.PI) * Math.acos(acosArg)) / frameFactor;
                float deltaChaseVwPtDeltAng = Math.abs(chaseVwPtDeltAng - mPrevChaseVwPtDeltAng);
                boolean angleTooHigh = chaseVwPtDeltAng > ACTIVE_ORBIT_MAX_ANG;
                boolean deltaAngTooHigh = deltaChaseVwPtDeltAng > ACTIVE_ORB_MX_ANG_DELT
                        && !Vector.areFloatsEqual(mPrevChaseVwPtDeltAng, 0.0f);
                // Boolean computation that keeps mActiveAutoOrbit from going back to 'true' once its
                // condition evaluates to 'false'.
                mActiveAutoOrbit = !angleTooHigh && !deltaAngTooHigh && !vertLockViewPt
                        && mActiveAutoOrbit;
                mPrevChaseVwPtDeltAng = chaseVwPtDeltAng;
                if (mActiveAutoOrbit) {
                    mChaseUnitViewPt = chaseUnitViewPt.clone();
                    mPrevChaseUnitV = chaseUnitViewPt.clone();
                } else {
                    // Use a passive auto-orbit not locked to the object position angle if its
                    // rotation rate (inter-frame difference in that angle), or the rate of
                    // change of that rotation rate, exceeds a max.
                    if (mInitPassiveAutoOrbit) {
                        // Make an init computation to make passive orbit direction equal the active.
                        mPassOrbAx = vertLockViewPt
                                ? Vector.V : Vector.crossVV(mPrevChaseUnitV, chaseUnitViewPt);
                        // Init computation of the view rot speed to smoothly transit to passive spd.
                        mInitialPassiveRotSpd = chaseVwPtDeltAng;
                        // If one of certain conditions are true skip the smooth transition.
                        mPassOrbTransitCount = vertLockViewPt || mLockObjChanged
                                || chaseVwPtDeltAng > PASSIVE_TRANSITION_MAX_ANG
                                ? PASS_ORB_TRANS_FRAMES : 0;
                        mLockObjChanged = false;
                        mInitPassiveAutoOrbit = false;
                    }
                    float orbitRotSpd = SLHelper.sineCycle(mAutoOrbitTime + (AUTO_ORBIT_D_PER / 2.0),
                            AUTO_ORBIT_R_AMP, AUTO_ORBIT_R_PER);
                    if (mPassOrbTransitCount < PASS_ORB_TRANS_FRAMES) {
                        float deltaRotSpd = (mInitialPassiveRotSpd - orbitRotSpd)
                                / (float) PASS_ORB_TRANS_FRAMES;
                        orbitRotSpd = mInitialPassiveRotSpd - (deltaRotSpd * mPassOrbTransitCount);
                        mPassOrbTransitCount++;
                    }
                    // Set a passive auto-orbit w/ constant rotation.
                    float frameOrbitSpd = orbitRotSpd * frameFactor;
                    float[] chasePosVec4 = rotateVec(mChaseUnitViewPt, mPassOrbAx, frameOrbitSpd);
                    System.arraycopy(chasePosVec4, 0, mChaseUnitViewPt, 0, 3);
                }
                float[] autoOrbitX = Vector.norm(Vector.crossVV(Vector.V, mChaseUnitViewPt));
                float[] autoOrbitY = Vector.V;
                float deltaDist = SLHelper.sineCycle(mAutoOrbitTime,
                        AUTO_ORBIT_D_AMP, AUTO_ORBIT_D_PER);
                float deltaX = SLHelper.sineCycle(mAutoOrbitTime + (AUTO_ORBIT_D_PER / 4.0),
                        AUTO_ORBIT_X_AMP, AUTO_ORBIT_X_PER);
                float deltaY = SLHelper.sineCycle(mAutoOrbitTime + (AUTO_ORBIT_D_PER / 4.0),
                        AUTO_ORBIT_Y_AMP, AUTO_ORBIT_Y_PER);
                mChasePos = Vector.sumVArray(
                        Vector.scaleSV(AUTO_ORBIT_DIST - deltaDist, mChaseUnitViewPt),
                        Vector.scaleSV(deltaX, autoOrbitX),
                        Vector.scaleSV(deltaY, autoOrbitY)
                );
                mAutoOrbitTime += 16.67 * (double) frameFactor;
            } else {
                // If in manual orbit apply eye control 'velocity' to the chase position.
                mChasePos = Vector.sumVV(mChasePos, frameCtrlVel);
            }
            if (mChaseTransitCount < SLAVE_TRANSIT_FRAMES) {
                float[] eyeGoalPos = Vector.sumVV(mLockViewPoint, mChasePos);
                float[] deltaPos = Vector.diffVV(eyeGoalPos, mPos);
                float factor = 1.0f / (float) (SLAVE_TRANSIT_FRAMES - mChaseTransitCount);
                deltaPos = Vector.scaleSV(factor, deltaPos);
                mPos = Vector.sumVV(mPos, deltaPos);
                mChaseTransitCount++;
            } else {
                mPos = Vector.sumVV(mLockViewPoint, mChasePos);
            }
        }
        // Increment eye rotation/slewing.
        if (mSlewFree) {
            // Convert the rotation axis defined in eye space to world space and pass it into
            // a rotation matrix.
            float[] rotAxis = Vector.transformFrom(mLocalAxis, mUvec, mVvec, mWvec);
            if (mCtrlRotSpd > 0.0f) {
                if (atAxisLimit) {
                    // Code to stop further slewing into vertical axis limit.
                    float[] vec = Vector.crossVV(rotAxis, Vector.V);
                    vec = Vector.norm(vec);
                    float dotWdA = Vector.dotVV(vec, dA);
                    if (dotWithVert < 0.0f && dotWdA > 0.0f) {
                        mCtrlRotSpd = 0.0f;
                    }
                    if (dotWithVert > 0.0f && dotWdA < 0.0f) {
                        mCtrlRotSpd = 0.0f;
                    }
                }
                // Apply incremental rotation to the relative view point vector.
                mViewPtOrigin = rotateVec(mViewPtOrigin, rotAxis, frameFactor * mCtrlRotSpd);
            }
        } else {
            float[] unitToLock = Vector.norm(Vector.diffVV(mLockViewPoint, mPos));
            if (mLockTransitCount < LOCK_TRANSIT_FRAMES) {
                float[] deltaV = Vector.diffVV(unitToLock, mViewPtOrigin);
                float factor = 1.0f / (float) (LOCK_TRANSIT_FRAMES - mLockTransitCount);
                deltaV = Vector.scaleSV(factor, deltaV);
                mViewPtOrigin = Vector.sumVV(mViewPtOrigin, deltaV);
                mViewPtOrigin = Vector.convertToVec4(Vector.norm(mViewPtOrigin));
                mLockTransitCount++;
            } else {
                mViewPtOrigin = Vector.convertToVec4(unitToLock);
            }
        }
        mViewPt = Vector.sumVV(mPos, mViewPtOrigin);
        mEarOrigin = Vector.norm(Vector.crossVV(mViewPtOrigin, Vector.V));
        mRightEar = mEarOrigin;
        mLeftEar = Vector.negV(mEarOrigin);
        setViewMatrix();
    }

    /**
     * Move eye position from passed-in motion event data.
     *
     * @param dX The delta x of the ACTION_MOVE event from initial DOWN point.
     * @param dY The delta y of the ACTION_MOVE event from initial DOWN point.
     */
    public void moveEye(float dX, float dY) {
        float[] movVec;
        if (mMovHorizontal) {
            movVec = new float[] {dX, 0.0f, -dY};
            } else {
            movVec = new float[] {dX, dY, 0.0f};
        }
        mCtrlMovVec = Vector.scaleSV(movScale, movVec);
    }

    /**
     * Move eye direction from passed-in motion event data.
     *
     * @param dX The delta x of the ACTION_MOVE event from initial DOWN point.
     * @param dY The delta y of the ACTION_MOVE event from initial DOWN point.
     * @return This Eye object for method chaining.
     */
    public Eye slewEye(float dX, float dY) {
        float[] rotAxis = new float[] {mFlipVert ? -dY : dY, -dX, 0.0f};
        mLocalAxis = Vector.scaleSV(slewScale, rotAxis);
        mCtrlRotSpd = Vector.length(mLocalAxis);
        if (mCtrlRotSpd == 0.0f) {
            mLocalAxis = INIT_AXIS;
        }
        return this;
    }

    /**
     * Refresh computation of the orthonormal basis defining eye space.
     */
    private void computeUnitBasis() {
        float[] wVec = Vector.norm(Vector.diffVV(mPos, mViewPt));
        float[] uVec = Vector.norm(Vector.crossVV(Vector.V, wVec));
        float[] vVec = Vector.crossVV(wVec, uVec);
        if (Vector.areOrthoNormal(uVec, vVec, wVec)) {
            mUvec = uVec;
            mVvec = vVec;
            mWvec = wVec;
        } else {
            Log.w(LOG_TAG, "WARNING: computeUnitBasis produced unit vectors of magnitudes "
                    + Vector.length(uVec) + ", " + Vector.length(vVec) + ", " + Vector.length(wVec)
            );
            defaultState();
        }
    }

    // Convenience method to rotate 'vec' by 'ang' along 'axis'.
    private float[] rotateVec(float[] vec, float[] axis, float ang) {
        float[] rotMat = new float[16];
        Matrix.setIdentityM(rotMat, 0);
        Matrix.rotateM(rotMat, 0, ang, axis[0], axis[1], axis[2]);
        float[] rotatedVec = new float[4];
        float[] vec4 = vec.clone();
        if (vec4.length < 4) vec4 = Vector.convertToVec4(vec4);
        Matrix.multiplyMV(rotatedVec, 0, rotMat, 0, vec4, 0);
        return rotatedVec;
    }

    public static void setControlScales(float moveSc, float slewSc) {
        movScale = moveSc;
        slewScale = slewSc;
    }

    public void saveToPrefs(SharedPreferences.Editor edit) {
        for (int i = 0; i < 3; i++) {
            edit.putFloat("eye_pos_" + i, mPos[i]);
            edit.putFloat("eye_viewpoint_origin_" + i, mViewPtOrigin[i]);
        }
        edit.putFloat("eye_viewpoint_origin_3", mViewPtOrigin[3]);
        edit.putInt(SLSurfaceView.EYE_OBJ_FOCUS, mFocusedMoverFlag);
        edit.putBoolean(SLSurfaceView.EYE_SLEW_FREE, mSlewFree);
        edit.putBoolean(SLSurfaceView.EYE_MOVE_FREE, mMoveFree);
        edit.putBoolean(SLSurfaceView.EYE_MOVE_HORIZ, mMovHorizontal);
        edit.putBoolean(SLSurfaceView.EYE_FLIP_VERT, mFlipVert);
    }

    public void restoreFromPrefs(SharedPreferences prefs) {
        float[] pos = new float[3];
        float[] viewPt = new float[4];
        for (int i = 0; i < 3; i++) {
            pos[i] = prefs.getFloat("eye_pos_" + i, INIT_POSITION[i]);
            viewPt[i] = prefs.getFloat("eye_viewpoint_origin_" + i, init_rotated_viewpoint[i]);
        }
        viewPt[3] = prefs.getFloat("eye_viewpoint_origin_3", init_rotated_viewpoint[3]);
        mPos = pos;
        mViewPtOrigin = viewPt;
        mFocusedMoverFlag = prefs.getInt(SLSurfaceView.EYE_OBJ_FOCUS, SLSimRenderer.SHAPE);
        mSlewFree = prefs.getBoolean(SLSurfaceView.EYE_SLEW_FREE, true);
        mMoveFree = prefs.getBoolean(SLSurfaceView.EYE_MOVE_FREE, true);
        mMovHorizontal = prefs.getBoolean(SLSurfaceView.EYE_MOVE_HORIZ, true);
        mFlipVert = prefs.getBoolean(SLSurfaceView.EYE_FLIP_VERT, false);
        mInitChase = true;
    }

    public void defaultState() {
        mPos = INIT_POSITION;
        mViewPtOrigin = init_rotated_viewpoint.clone();
        mSlewFree = true;
        mMoveFree = true;
    }

}
