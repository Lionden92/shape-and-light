package com.pathfinder.shapeandlight;

import android.content.SharedPreferences;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by Lionden on 5/25/2016.
 */

@SuppressWarnings("WeakerAccess")
class Shape implements SLSimRenderer.FreeMover {
    private static final String LOG_TAG = Shape.class.getSimpleName();
    protected static final float[] INIT_POSITION = {0.0f, 0.5f, 20.0f};
    protected static final float[] INIT_VELOCITY = {0.5f, 0.0f, 0.0f};
    protected static final float[] INIT_AXIS = {0.0f, 1.0f, 0.0f};
    private static final float DEFAULT_CUBE_SIZE = 3.75f;
    private static final float DEFAULT_ICOS_SIZE = 2.66f;
    private static final float GOLDEN_RATIO = 1.618f;
    protected static final long NANOS_PER_MILLI = 1000000;
    protected volatile int mType;
    // Shape type flags.
    protected static final int CUBE = 0;
    protected static final int ICOS = 1;
    protected static final int RNDDODEC = 2;
    protected static final int RNDICOS = 3;

    // Vector defining the shape's position in world space.
    protected volatile float[] mPos = new float[3];
    // Vector defining the shape's velocity in world space.
    protected volatile float[] mVel = new float[3];
    // Vector holding the net force being acted on the shape, used in performance rotations.
    protected volatile float[] mUnitForce;
    // Vectors holding random directions for "special case" perfSpace generation.
    protected float[] mAltUnitForce1;
    protected float[] mAltUnitForce2;
    protected float[] mAltVel;
    // Vector defining the shape's rotational axis.
    protected volatile float[] mAxis = new float[3];
    // Rotational speed.
    protected volatile float mRotSpd = 0.0f;
    // Mat4 providing translation matrix for OpenGL operations.
    protected float[] mTranslM = new float[16];
    // Mat4 providing incremental per-frame rotations.
    protected float[] mRotM = new float[16];
    // Mat4 providing persistent orientation matrix for OpenGL operations.
    protected float[] mOrientM = new float[16];
    // Mat4 providing the OpenGL model matrix.
    protected float[] mModelM = new float[16];
    // Float array array storing the shape's mesh points for saving/restoring randomized state.
    protected float[][] mMeshPoints;
    // Float buffer containing shape's mesh data for OpenGL use.
    protected volatile FloatBuffer mMeshPositions;
    // Buffer for the vertex normals.
    protected volatile FloatBuffer mMeshNormals;
    // Buffer for the face centers.
    protected volatile FloatBuffer mMeshCenters;
    // Float buffer containing the shape's polygon texture coordinates.
    protected volatile FloatBuffer mTextureCoords0;
    protected volatile FloatBuffer mTextureCoords1;
    protected volatile FloatBuffer mTextureCoords2;
    // Boolean for whether the shape is moving freely.
    protected volatile boolean mIsFree = true;
    // Boolean for whether the shape is being touched by a screen MotionEvent.
    volatile boolean mTouched;
    // Float array containing the shape's screen coordinates used for touch detection.
    volatile float[] mScreenCoords;
    // Smoke bubble emitters.
    protected static final float EMIT_DIST = 4.0f;
    protected static final float EMIT_DIST_ANGLE_FACTOR = 13.0f;
    protected static final long MAX_DELTA_EMIT_NANOS = 2000 * NANOS_PER_MILLI;
    private float[] mBubbleEmitter1localCoords = new float[4];
    private float[] mBubbleEmitter2localCoords = new float[4];
    protected float[] mBubbleEmitter1 = new float[4];
    protected float[] mBubbleEmitter2 = new float[4];
    protected float[] mBubEmitLocation1;
    protected float[] mBubEmitLocation2;
    protected float[] mBubDeltaEmitter1;
    protected float[] mBubDeltaEmitter2;
    protected float mIncremEmitDist1;
    protected float mIncremEmitDist2;
    protected float mPrevEmitDist1;
    protected float mPrevEmitDist2;
    protected long mBubEmitTime1;
    protected long mBubEmitTime2;
    protected boolean mEmitBubble1;
    protected boolean mEmitBubble2;
    protected boolean mInitBubInterpolation;
    // Sparkle emitters.
    private float[][] mSparkleEmitterLocalCoords = new float[8][4];
    protected float[][] mSparkleEmitters = new float[8][4];
    // Params for performance rotation.
    private float[] mBubbleLongestEmitterLocal = new float[4];
    protected static final int NONE = 0;
    protected static final int CORKSCREW = 1;
    protected static final int CARTWHEEL = 2;
    protected static final int CAROUSEL = 3;
    protected int mPerfMode = NONE;
    protected boolean mUsePerfOffsetAxis = false;
    protected boolean mUseAltOffsetAxis = false;
    protected long mPerfRotStartTime;
    protected long mPerfLengthMillis;
    protected float mPerfRotTime = 0.0f;
    protected float mPeriod;
    protected boolean mRotSteady;

    // Constructor that restores shape from SharedPreferences storage, or starts a default new
    // shape if nothing has been stored yet.
    public Shape(SharedPreferences prefs) {
        if (prefs.getBoolean(SLSurfaceView.SHAPE_STORED, false)) {
            int type = prefs.getInt(SLSurfaceView.SHAPE_TYPE, RNDICOS);
            switch (type) {
                case CUBE:
                    mType = CUBE;
                    loadCubeMesh(prefs);
                    buildCubeShape();
                    break;
                case ICOS:
                    mType = ICOS;
                    loadIcosMesh(prefs);
                    buildIcosShape();
                    break;
                case RNDDODEC:
                    mType = RNDDODEC;
                    loadCubeMesh(prefs);
                    buildCubeShape();
                    break;
                case RNDICOS:
                    mType = RNDICOS;
                    loadIcosMesh(prefs);
                    buildIcosShape();
                    break;
            }
            loadParams(prefs);
            // Set the translation matrix.
            setTranslM();
            // Set the initial model matrix.
            Matrix.multiplyMM(mModelM, 0, mTranslM, 0, mOrientM, 0);
        } else {
            // Initial default construction if nothing has been saved in SharedPreferences.
            mType = RNDICOS;
            setIcosMesh(DEFAULT_ICOS_SIZE, 1.65f);
            buildIcosShape();
            mPos = INIT_POSITION.clone();
            mVel = INIT_VELOCITY.clone();
            mAxis = INIT_AXIS.clone();
            mRotSpd = 0.0f;
            // Set the translation matrix.
            setTranslM();
            // Set the initial orientation matrix.
            Matrix.setIdentityM(mOrientM, 0);
            // Set the initial model matrix.
            Matrix.multiplyMM(mModelM, 0, mTranslM, 0, mOrientM, 0);
        }
        generatePerfVecs();
        initBubbleEmitInterpolation();
    }

    // Change the shape's type.
    public void setShapeType(int type) {
        mType = type;
        switch (type) {
            case CUBE:
                setCubeMesh(DEFAULT_CUBE_SIZE, 0.0f);
                buildCubeShape();
                break;
            case ICOS:
                setIcosMesh(DEFAULT_ICOS_SIZE, 0.0f);
                buildIcosShape();
                break;
            case RNDDODEC:
                setCubeMesh(DEFAULT_CUBE_SIZE * 0.85f, 1.85f);
                buildCubeShape();
                break;
            case RNDICOS:
                setIcosMesh(DEFAULT_ICOS_SIZE, 1.65f);
                buildIcosShape();
        }
    }

    public float[] getPos() {
        return mPos;
    }

    public float[] getVel() {
        return mVel;
    }

    public float getMass() {
        return 1.0f;
    }

    public void setPos(float[] pos) {
        mPos = pos.clone();
        setTranslM();
    }

    public void setVel(float[] vel) {
        mVel = vel.clone();
    }

    public void setFree(boolean free) {
        mIsFree = free;
    }

    public void defaultState() {
        mPos = INIT_POSITION.clone();
        mVel = INIT_VELOCITY.clone();
        mRotSpd = 0.0f;
        Matrix.setIdentityM(mOrientM, 0);
        initBubbleEmitInterpolation();
        generatePerfVecs();
    }

    public void reset() {
        mPos = Vector.ZERO.clone();
        mVel = Vector.ZERO.clone();
        mRotSpd = 0.0f;
        Matrix.setIdentityM(mOrientM, 0);
        initBubbleEmitInterpolation();
        generatePerfVecs();
    }

    // Generate initial random mUnitForce vector and "special case" alternate vectors for
    // performance mode.
    public void generatePerfVecs() {
        // Set "tolerance" to allow only vectors of at least ~10 degree angle difference.
        float tolerance = 0.18f;
        mUnitForce = Vector.randomV(1.0f);
        // Generate the alt vecs making sure they are all sufficiently different.
        do {
            mAltVel = Vector.randomV(1.0f);
        } while (Vector.areNear(mAltVel, mUnitForce, tolerance));
        do {
            mAltUnitForce1 = Vector.randomV(1.0f);
        } while (Vector.areNear(mAltUnitForce1, mAltVel, tolerance)
                || Vector.areNear(mAltUnitForce1, mUnitForce, tolerance));
        do {
            mAltUnitForce2 = Vector.randomV(1.0f);
        } while (Vector.areNear(mAltUnitForce2, mAltUnitForce1, tolerance)
                || Vector.areNear(mAltUnitForce2, mAltVel, tolerance)
                || Vector.areNear(mAltUnitForce2, mUnitForce, tolerance));
    }

    // Increment velocity by an acceleration vector, and update the force vector.
    public synchronized void incVel(float[] accel) {
        mVel = Vector.sumVV(mVel, accel);
        if (!Vector.areEqual(accel, Vector.ZERO)) {
            mUnitForce = Vector.norm(accel);
        }
    }

    public boolean isFree() {
        return mIsFree;
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

    // Set the translation matrix from mPos.
    private void setTranslM() {
        Matrix.setIdentityM(mTranslM, 0);
        Matrix.translateM(mTranslM, 0, mPos[0], mPos[1], mPos[2]);
    }

    // Update the shape's per-frame position and rotation in world space.
    public void perFrameUpdate(float frameFactor) {
        // Do no update if framerate has stalled.
        if (frameFactor > SLSimRenderer.MAX_FRAMETIME_FACTOR) return;
        // Position coord limiter, causing the object to "deflect" back towards the center point.
        if (Vector.length(mPos) > SLHelper.MAX_DISTANCE_CENTER) {
            mVel = Vector.reverseComp(mVel, Vector.norm(mPos));
        }
        // Speed limiter, to 90% the SLSimRenderer "speed of sound".
        float speed = Vector.length(mVel);
        float maxSpeed = SLSimRenderer.SPEED_OF_SOUND * 0.9f;
        if (speed > maxSpeed) mVel = Vector.scaleSV(maxSpeed / speed, mVel);
        // Increment the position.
        if (mIsFree) {
            mPos = Vector.sumVV(mPos, Vector.scaleSV(frameFactor, mVel));
            setTranslM();
        }
        // Increment the rotation.
        float frameRotSpd = frameFactor * mRotSpd;
        Matrix.setIdentityM(mRotM, 0);
        Matrix.rotateM(mRotM, 0, frameRotSpd, mAxis[0], mAxis[1], mAxis[2]);
        Matrix.multiplyMM(mOrientM, 0, mRotM, 0, mOrientM, 0);
        // Update the OpenGL model matrix.
        Matrix.multiplyMM(mModelM, 0, mTranslM, 0, mOrientM, 0);
        // Update the bubble emitter positions, using interpolation to maintain regular spacing.
        float[] bubEmitterPrev1 = Vector.ZERO4;
        float[] bubEmitterPrev2 = Vector.ZERO4;
        float[] bubDeltaEmitPrev1 = Vector.ZERO4;
        float[] bubDeltaEmitPrev2 = Vector.ZERO4;
        if (!mInitBubInterpolation) {
            bubEmitterPrev1 = mBubbleEmitter1.clone();
            bubEmitterPrev2 = mBubbleEmitter2.clone();
            bubDeltaEmitPrev1 = mBubDeltaEmitter1.clone();
            bubDeltaEmitPrev2 = mBubDeltaEmitter2.clone();
        }
        Matrix.multiplyMV(mBubbleEmitter1, 0, mModelM, 0, mBubbleEmitter1localCoords, 0);
        Matrix.multiplyMV(mBubbleEmitter2, 0, mModelM, 0, mBubbleEmitter2localCoords, 0);
        if (mInitBubInterpolation) {
            bubEmitterPrev1 = mBubbleEmitter1.clone();
            bubEmitterPrev2 = mBubbleEmitter2.clone();
        }
        mBubDeltaEmitter1 = Vector.diffVV(mBubbleEmitter1, bubEmitterPrev1);
        mBubDeltaEmitter2 = Vector.diffVV(mBubbleEmitter2, bubEmitterPrev2);
        float bubDeltaEmitLength1 = Vector.length(mBubDeltaEmitter1);
        float bubDeltaEmitLength2 = Vector.length(mBubDeltaEmitter2);
        mIncremEmitDist1 += bubDeltaEmitLength1;
        mIncremEmitDist2 += bubDeltaEmitLength2;
        // Compute the variable distance desired between emits. If the emitter has moved a greater
        // distance make that the emit distance.
        float emitDist1 = Math.max(computeEmitDist(mBubDeltaEmitter1, bubDeltaEmitPrev1,
                mPrevEmitDist1), bubDeltaEmitLength1);
        float emitDist2 = Math.max(computeEmitDist(mBubDeltaEmitter2, bubDeltaEmitPrev2,
                mPrevEmitDist2), bubDeltaEmitLength2);
        mEmitBubble1 = mIncremEmitDist1 >= emitDist1 - Vector.SIGMA;
        mEmitBubble2 = mIncremEmitDist2 >= emitDist2 - Vector.SIGMA;
        long currentTime = System.nanoTime();
        if (mEmitBubble1) {
            float rmdr = Vector.areFloatsEqual(mIncremEmitDist1, emitDist1) ? 0.0f
                    // Don't let the remainder scaling exceed the per-frame delta emit length.
                    : Math.min(mIncremEmitDist1 - emitDist1, bubDeltaEmitLength1);
            // Interpolate.
            mBubEmitLocation1 = Vector.sumVV(mBubbleEmitter1,
                    Vector.scaleSV(-rmdr, Vector.norm(mBubDeltaEmitter1)));
            mIncremEmitDist1 = rmdr;
            mBubEmitTime1 = currentTime;
            mPrevEmitDist1 = emitDist1;
        }
        if (mEmitBubble2) {
            float rmdr = Vector.areFloatsEqual(mIncremEmitDist2, emitDist2) ? 0.0f
                    : Math.min(mIncremEmitDist2 - emitDist2, bubDeltaEmitLength2);
            // Interpolate.
            mBubEmitLocation2 = Vector.sumVV(mBubbleEmitter2,
                    Vector.scaleSV(-rmdr, Vector.norm(mBubDeltaEmitter2)));
            mIncremEmitDist2 = rmdr;
            mBubEmitTime2 = currentTime;
            mPrevEmitDist2 = emitDist2;
        }
        if (mInitBubInterpolation) {
            // Make sure initial frame after bubble gen toggle emits bubbles.
            mBubEmitLocation1 = mBubbleEmitter1;
            mBubEmitLocation2 = mBubbleEmitter2;
            mEmitBubble1 = true;
            mEmitBubble2 = true;
            mBubEmitTime1 = currentTime;
            mBubEmitTime2 = currentTime;
            mInitBubInterpolation = false;
        }
        // Emit bubbles after a max delta time if none have been emitted yet.
        currentTime = System.nanoTime();
        if (currentTime - mBubEmitTime1 > MAX_DELTA_EMIT_NANOS) {
            mBubEmitLocation1 = mBubbleEmitter1;
            mEmitBubble1 = true;
            mIncremEmitDist1 = 0.0f;
            mBubEmitTime1 = currentTime;
        }
        if (currentTime - mBubEmitTime2 > MAX_DELTA_EMIT_NANOS) {
            mBubEmitLocation2 = mBubbleEmitter2;
            mEmitBubble2 = true;
            mIncremEmitDist2 = 0.0f;
            mBubEmitTime2 = currentTime;
        }
        // Compute sparkle emitter positions.
        for (int i = 0; i < 8; i++) {
            Matrix.multiplyMV(mSparkleEmitters[i], 0, mModelM, 0, mSparkleEmitterLocalCoords[i], 0);
        }
        // Performance mode rotation adjustments if called for.
        if (mPerfMode != NONE) {
            if (System.nanoTime() - mPerfRotStartTime > mPerfLengthMillis * NANOS_PER_MILLI) {
                mPerfMode = NONE;
            } else {
                performanceRot(frameFactor);
            }
        }
    }

    // Compute bubble emit distances for given previous & current delta emitters, factoring in
    // the angle between the 2 to shorten the emit distance if the emitters are changing direction.
    protected float computeEmitDist(
            float[] deltaEmitter, float[] prevDeltaEmitter, float prevEmitDist) {
        // If the shape is being dragged don't add the angle factor.
        float deltaAngle = mIsFree ? Vector.angleVV(deltaEmitter, prevDeltaEmitter) : 0.0f;
        float curveFactor = 1.0f + EMIT_DIST_ANGLE_FACTOR * deltaAngle;
        float emitDist = EMIT_DIST / curveFactor;
        return Vector.areFloatsEqual(prevEmitDist, 0.0f) ? emitDist
                // Apply "dampening" to keep the emit distance from changing too quickly.
                : Math.max(0.66f * prevEmitDist, Math.min(1.33f * prevEmitDist, emitDist));
    }

    // Initialize bubble emitter interpolation.
    protected void initBubbleEmitInterpolation() {
        mInitBubInterpolation = true;
        mIncremEmitDist1 = 0.0f;
        mIncremEmitDist2 = 0.0f;
        mPrevEmitDist1 = 0.0f;
        mPrevEmitDist2 = 0.0f;
    }

    // Set up mesh data for a cube shape.
    private void setCubeMesh(float size, float rnd) {
        size = size * 0.5f;
        // Points defining the cube mesh.
        mMeshPoints = new float[][] {
                {-size + rndVr3(rnd), -size + rndVr3(rnd), size + rndVr3(rnd), 1.0f},
                {size + rndVr3(rnd), -size - rndVr3(rnd), size - rndVr3(rnd), 1.0f},
                {size - rndVr3(rnd), size - rndVr3(rnd), size + rndVr3(rnd), 1.0f},
                {-size - rndVr3(rnd), size + rndVr3(rnd), size - rndVr3(rnd), 1.0f},
                {size - rndVr3(rnd), -size + rndVr3(rnd), -size - rndVr3(rnd), 1.0f},
                {-size - rndVr3(rnd), -size - rndVr3(rnd), -size + rndVr3(rnd), 1.0f},
                {-size + rndVr3(rnd), size - rndVr3(rnd), -size - rndVr3(rnd), 1.0f},
                {size + rndVr3(rnd), size + rndVr3(rnd), -size + rndVr3(rnd), 1.0f}
        };
        // Set initial orientation, also used for texture projection mapping on the 3 planes.
        float[] rotM = new float[16];
        Matrix.setIdentityM(rotM, 0);
        Matrix.rotateM(rotM, 0, -45.0f, 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(rotM, 0, 54.74f, 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(rotM, 0, 45.0f, 0.0f, 1.0f, 0.0f);
        for (int i = 0; i < 8; i++) {
            Matrix.multiplyMV(mMeshPoints[i], 0, rotM, 0, mMeshPoints[i], 0);
        }
    }

    // Load mesh data for a cube shape from the SharedPreferences file.
    private void loadCubeMesh(SharedPreferences prefs) {
        float[][] meshPoints = new float[8][4];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 4; j++) {
                meshPoints[i][j] = prefs.getFloat("shape_mesh_" + i + "_" + j, 0.0f);
            }
        }
        mMeshPoints = meshPoints;
    }

    // Save mesh data for a cube shape to the SharedPreferences file.
    protected void saveCubeMesh(SharedPreferences.Editor edit) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 4; j++) {
                edit.putFloat("shape_mesh_" + i + "_" + j, mMeshPoints[i][j]);
            }
        }
    }

    // Build a cube/rnddodec shape from the cube mesh.
    private void buildCubeShape() {
        boolean cube = mType == CUBE;
        if (!cube) {
            // "Flip a coin" to select bubble emitters between two different methods, for different
            // possible effects.
            float dist = 0.0f;
            float distCalc;
            float avgMagWeight = 4.0f;
            if (rndVr2(1.0f) > 0.0f) {
                // Choose emitter points using pairing criteria.
                float[] emitter1a = mMeshPoints[0];
                float[] emitter2a = mMeshPoints[2];
                float[] emitter1b = mMeshPoints[7];
                float[] emitter2b = mMeshPoints[1];
                 for (int i = 0; i < 3; i++) {
                    int j = 2 * (i + 1); // Choose between pairs 0-2, 0-4, 0-6
                    distCalc = emitterPairCalc(mMeshPoints[0], mMeshPoints[j], avgMagWeight);
                    if (distCalc > dist) {
                        dist = distCalc;
                        emitter2a = mMeshPoints[j];
                    }
                }
                dist = 0.0f;
                for (int i = 0; i < 3; i++) {
                    int j = 2 * i + 1; // Choose between pairs 7-1, 7-3, 7-5
                    distCalc = emitterPairCalc(mMeshPoints[7], mMeshPoints[j], avgMagWeight);
                    if (distCalc > dist) {
                        dist = distCalc;
                        emitter2b = mMeshPoints[j];
                    }
                }
                // Now choose between the 0 pair or the 7 pair.
                if (emitterPairCalc(emitter1a, emitter2a, avgMagWeight)
                        > emitterPairCalc(emitter1b, emitter2b, avgMagWeight)) {
                    mBubbleEmitter1localCoords = emitter1a;
                    mBubbleEmitter2localCoords = emitter2a;
                } else {
                    mBubbleEmitter1localCoords = emitter1b;
                    mBubbleEmitter2localCoords = emitter2b;
                }
            } else {
                // Or choose the emitters between the farthest-apart points.
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        distCalc = Vector.length(Vector.diffVV(mMeshPoints[i], mMeshPoints[j]));
                        if (distCalc > dist) {
                            dist = distCalc;
                            mBubbleEmitter1localCoords = mMeshPoints[i];
                            mBubbleEmitter2localCoords = mMeshPoints[j];
                        }
                    }
                }
            }
        } else {
            mBubbleEmitter1localCoords = mMeshPoints[0];
            mBubbleEmitter2localCoords = rndVr2(1.0f) > 0.0f ? mMeshPoints[2] : mMeshPoints[7];
        }
        mBubbleLongestEmitterLocal = Vector.length(mBubbleEmitter2localCoords) >
                Vector.length(mBubbleEmitter1localCoords) ?
                mBubbleEmitter2localCoords :
                mBubbleEmitter1localCoords;
        System.arraycopy(mMeshPoints, 0, mSparkleEmitterLocalCoords, 0, 8);
        // The normals for all 12 triangles, beginning with the front face.
        float[] tri01Normal = Vector.getNormal(
                Vector.diffVV(mMeshPoints[1], mMeshPoints[0]),
                Vector.diffVV(mMeshPoints[2], mMeshPoints[0])
        );
        float[] tri02Normal = Vector.getNormal(
                Vector.diffVV(mMeshPoints[2], mMeshPoints[0]),
                Vector.diffVV(mMeshPoints[3], mMeshPoints[0])
        );
        // Right face triangles.
        float[] tri03Normal = Vector.getNormal(
                Vector.diffVV(mMeshPoints[4], mMeshPoints[1]),
                Vector.diffVV(mMeshPoints[7], mMeshPoints[1])
        );
        float[] tri04Normal = Vector.getNormal(
                Vector.diffVV(mMeshPoints[7], mMeshPoints[1]),
                Vector.diffVV(mMeshPoints[2], mMeshPoints[1])
        );
        // Back face triangles.
        float[] tri05Normal = Vector.getNormal(
                Vector.diffVV(mMeshPoints[5], mMeshPoints[4]),
                Vector.diffVV(mMeshPoints[6], mMeshPoints[4])
        );
        float[] tri06Normal = Vector.getNormal(
                Vector.diffVV(mMeshPoints[6], mMeshPoints[4]),
                Vector.diffVV(mMeshPoints[7], mMeshPoints[4])
        );
        // Left face triangles.
        float[] tri07Normal = Vector.getNormal(
                Vector.diffVV(mMeshPoints[0], mMeshPoints[5]),
                Vector.diffVV(mMeshPoints[3], mMeshPoints[5])
        );
        float[] tri08Normal = Vector.getNormal(
                Vector.diffVV(mMeshPoints[3], mMeshPoints[5]),
                Vector.diffVV(mMeshPoints[6], mMeshPoints[5])
        );
        // Top face triangles.
        float[] tri09Normal = Vector.getNormal(
                Vector.diffVV(mMeshPoints[2], mMeshPoints[3]),
                Vector.diffVV(mMeshPoints[7], mMeshPoints[3])
        );
        float[] tri10Normal = Vector.getNormal(
                Vector.diffVV(mMeshPoints[7], mMeshPoints[3]),
                Vector.diffVV(mMeshPoints[6], mMeshPoints[3])
        );
        // Bottom face triangles.
        float[] tri11Normal = Vector.getNormal(
                Vector.diffVV(mMeshPoints[4], mMeshPoints[5]),
                Vector.diffVV(mMeshPoints[1], mMeshPoints[5])
        );
        float[] tri12Normal = Vector.getNormal(
                Vector.diffVV(mMeshPoints[1], mMeshPoints[5]),
                Vector.diffVV(mMeshPoints[0], mMeshPoints[5])
        );
        // Center points for all 12 triangles for vertex lighting calculations.
        float[] tri01Ctr;
        float[] tri02Ctr;
        float[] tri03Ctr;
        float[] tri04Ctr;
        float[] tri05Ctr;
        float[] tri06Ctr;
        float[] tri07Ctr;
        float[] tri08Ctr;
        float[] tri09Ctr;
        float[] tri10Ctr;
        float[] tri11Ctr;
        float[] tri12Ctr;
        if (cube) {
            tri01Ctr = Vector.avgVArray(mMeshPoints[0], mMeshPoints[2]);
            tri02Ctr = tri01Ctr;
            tri03Ctr = Vector.avgVArray(mMeshPoints[1], mMeshPoints[7]);
            tri04Ctr = tri03Ctr;
            tri05Ctr = Vector.avgVArray(mMeshPoints[4], mMeshPoints[6]);
            tri06Ctr = tri05Ctr;
            tri07Ctr = Vector.avgVArray(mMeshPoints[5], mMeshPoints[3]);
            tri08Ctr = tri07Ctr;
            tri09Ctr = Vector.avgVArray(mMeshPoints[3], mMeshPoints[7]);
            tri10Ctr = tri09Ctr;
            tri11Ctr = Vector.avgVArray(mMeshPoints[5], mMeshPoints[1]);
            tri12Ctr = tri11Ctr;
        } else {
            tri01Ctr = Vector.avgVArray(mMeshPoints[0], mMeshPoints[1], mMeshPoints[2]);
            tri02Ctr = Vector.avgVArray(mMeshPoints[0], mMeshPoints[2], mMeshPoints[3]);
            tri03Ctr = Vector.avgVArray(mMeshPoints[1], mMeshPoints[4], mMeshPoints[7]);
            tri04Ctr = Vector.avgVArray(mMeshPoints[1], mMeshPoints[7], mMeshPoints[2]);
            tri05Ctr = Vector.avgVArray(mMeshPoints[4], mMeshPoints[5], mMeshPoints[6]);
            tri06Ctr = Vector.avgVArray(mMeshPoints[4], mMeshPoints[6], mMeshPoints[7]);
            tri07Ctr = Vector.avgVArray(mMeshPoints[5], mMeshPoints[0], mMeshPoints[3]);
            tri08Ctr = Vector.avgVArray(mMeshPoints[5], mMeshPoints[3], mMeshPoints[6]);
            tri09Ctr = Vector.avgVArray(mMeshPoints[3], mMeshPoints[2], mMeshPoints[7]);
            tri10Ctr = Vector.avgVArray(mMeshPoints[3], mMeshPoints[7], mMeshPoints[6]);
            tri11Ctr = Vector.avgVArray(mMeshPoints[5], mMeshPoints[4], mMeshPoints[1]);
            tri12Ctr = Vector.avgVArray(mMeshPoints[5], mMeshPoints[1], mMeshPoints[0]);
        }
        // Array to plug in to mMeshPositions float buffer for gl use.
        float[] meshPositionData = {
                // Front face
                mMeshPoints[0][0], mMeshPoints[0][1], mMeshPoints[0][2],
                mMeshPoints[1][0], mMeshPoints[1][1], mMeshPoints[1][2],
                mMeshPoints[2][0], mMeshPoints[2][1], mMeshPoints[2][2],
                mMeshPoints[0][0], mMeshPoints[0][1], mMeshPoints[0][2],
                mMeshPoints[2][0], mMeshPoints[2][1], mMeshPoints[2][2],
                mMeshPoints[3][0], mMeshPoints[3][1], mMeshPoints[3][2],
                // Right face
                mMeshPoints[1][0], mMeshPoints[1][1], mMeshPoints[1][2],
                mMeshPoints[4][0], mMeshPoints[4][1], mMeshPoints[4][2],
                mMeshPoints[7][0], mMeshPoints[7][1], mMeshPoints[7][2],
                mMeshPoints[1][0], mMeshPoints[1][1], mMeshPoints[1][2],
                mMeshPoints[7][0], mMeshPoints[7][1], mMeshPoints[7][2],
                mMeshPoints[2][0], mMeshPoints[2][1], mMeshPoints[2][2],
                // Back face
                mMeshPoints[4][0], mMeshPoints[4][1], mMeshPoints[4][2],
                mMeshPoints[5][0], mMeshPoints[5][1], mMeshPoints[5][2],
                mMeshPoints[6][0], mMeshPoints[6][1], mMeshPoints[6][2],
                mMeshPoints[4][0], mMeshPoints[4][1], mMeshPoints[4][2],
                mMeshPoints[6][0], mMeshPoints[6][1], mMeshPoints[6][2],
                mMeshPoints[7][0], mMeshPoints[7][1], mMeshPoints[7][2],
                // Left face
                mMeshPoints[5][0], mMeshPoints[5][1], mMeshPoints[5][2],
                mMeshPoints[0][0], mMeshPoints[0][1], mMeshPoints[0][2],
                mMeshPoints[3][0], mMeshPoints[3][1], mMeshPoints[3][2],
                mMeshPoints[5][0], mMeshPoints[5][1], mMeshPoints[5][2],
                mMeshPoints[3][0], mMeshPoints[3][1], mMeshPoints[3][2],
                mMeshPoints[6][0], mMeshPoints[6][1], mMeshPoints[6][2],
                // Top face
                mMeshPoints[3][0], mMeshPoints[3][1], mMeshPoints[3][2],
                mMeshPoints[2][0], mMeshPoints[2][1], mMeshPoints[2][2],
                mMeshPoints[7][0], mMeshPoints[7][1], mMeshPoints[7][2],
                mMeshPoints[3][0], mMeshPoints[3][1], mMeshPoints[3][2],
                mMeshPoints[7][0], mMeshPoints[7][1], mMeshPoints[7][2],
                mMeshPoints[6][0], mMeshPoints[6][1], mMeshPoints[6][2],
                // Bottom face
                mMeshPoints[5][0], mMeshPoints[5][1], mMeshPoints[5][2],
                mMeshPoints[4][0], mMeshPoints[4][1], mMeshPoints[4][2],
                mMeshPoints[1][0], mMeshPoints[1][1], mMeshPoints[1][2],
                mMeshPoints[5][0], mMeshPoints[5][1], mMeshPoints[5][2],
                mMeshPoints[1][0], mMeshPoints[1][1], mMeshPoints[1][2],
                mMeshPoints[0][0], mMeshPoints[0][1], mMeshPoints[0][2]
        };
        mMeshPositions = ByteBuffer.allocateDirect(
                meshPositionData.length * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mMeshPositions.put(meshPositionData).position(0);

        float[] textureCoordData = getTextureCoordsZX(meshPositionData);
        mTextureCoords0 = ByteBuffer.allocateDirect(
                textureCoordData.length * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureCoords0.put(textureCoordData).position(0);
        textureCoordData = getTextureCoordsXY(meshPositionData);
        mTextureCoords1 = ByteBuffer.allocateDirect(
                textureCoordData.length * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureCoords1.put(textureCoordData).position(0);
        textureCoordData = getTextureCoordsYZ(meshPositionData);
        mTextureCoords2 = ByteBuffer.allocateDirect(
                textureCoordData.length * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureCoords2.put(textureCoordData).position(0);

        float[] normalsData = {
                // Front face
                tri01Normal[0], tri01Normal[1], tri01Normal[2],
                tri01Normal[0], tri01Normal[1], tri01Normal[2],
                tri01Normal[0], tri01Normal[1], tri01Normal[2],
                tri02Normal[0], tri02Normal[1], tri02Normal[2],
                tri02Normal[0], tri02Normal[1], tri02Normal[2],
                tri02Normal[0], tri02Normal[1], tri02Normal[2],
                // Right face
                tri03Normal[0], tri03Normal[1], tri03Normal[2],
                tri03Normal[0], tri03Normal[1], tri03Normal[2],
                tri03Normal[0], tri03Normal[1], tri03Normal[2],
                tri04Normal[0], tri04Normal[1], tri04Normal[2],
                tri04Normal[0], tri04Normal[1], tri04Normal[2],
                tri04Normal[0], tri04Normal[1], tri04Normal[2],
                // Back face
                tri05Normal[0], tri05Normal[1], tri05Normal[2],
                tri05Normal[0], tri05Normal[1], tri05Normal[2],
                tri05Normal[0], tri05Normal[1], tri05Normal[2],
                tri06Normal[0], tri06Normal[1], tri06Normal[2],
                tri06Normal[0], tri06Normal[1], tri06Normal[2],
                tri06Normal[0], tri06Normal[1], tri06Normal[2],
                // Left face
                tri07Normal[0], tri07Normal[1], tri07Normal[2],
                tri07Normal[0], tri07Normal[1], tri07Normal[2],
                tri07Normal[0], tri07Normal[1], tri07Normal[2],
                tri08Normal[0], tri08Normal[1], tri08Normal[2],
                tri08Normal[0], tri08Normal[1], tri08Normal[2],
                tri08Normal[0], tri08Normal[1], tri08Normal[2],
                // Top face
                tri09Normal[0], tri09Normal[1], tri09Normal[2],
                tri09Normal[0], tri09Normal[1], tri09Normal[2],
                tri09Normal[0], tri09Normal[1], tri09Normal[2],
                tri10Normal[0], tri10Normal[1], tri10Normal[2],
                tri10Normal[0], tri10Normal[1], tri10Normal[2],
                tri10Normal[0], tri10Normal[1], tri10Normal[2],
                // Bottom face
                tri11Normal[0], tri11Normal[1], tri11Normal[2],
                tri11Normal[0], tri11Normal[1], tri11Normal[2],
                tri11Normal[0], tri11Normal[1], tri11Normal[2],
                tri12Normal[0], tri12Normal[1], tri12Normal[2],
                tri12Normal[0], tri12Normal[1], tri12Normal[2],
                tri12Normal[0], tri12Normal[1], tri12Normal[2],
        };
        mMeshNormals = ByteBuffer.allocateDirect(
                normalsData.length * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mMeshNormals.put(normalsData).position(0);

        float[] centersData = {
                tri01Ctr[0], tri01Ctr[1], tri01Ctr[2],
                tri01Ctr[0], tri01Ctr[1], tri01Ctr[2],
                tri01Ctr[0], tri01Ctr[1], tri01Ctr[2],
                tri02Ctr[0], tri02Ctr[1], tri02Ctr[2],
                tri02Ctr[0], tri02Ctr[1], tri02Ctr[2],
                tri02Ctr[0], tri02Ctr[1], tri02Ctr[2],

                tri03Ctr[0], tri03Ctr[1], tri03Ctr[2],
                tri03Ctr[0], tri03Ctr[1], tri03Ctr[2],
                tri03Ctr[0], tri03Ctr[1], tri03Ctr[2],
                tri04Ctr[0], tri04Ctr[1], tri04Ctr[2],
                tri04Ctr[0], tri04Ctr[1], tri04Ctr[2],
                tri04Ctr[0], tri04Ctr[1], tri04Ctr[2],

                tri05Ctr[0], tri05Ctr[1], tri05Ctr[2],
                tri05Ctr[0], tri05Ctr[1], tri05Ctr[2],
                tri05Ctr[0], tri05Ctr[1], tri05Ctr[2],
                tri06Ctr[0], tri06Ctr[1], tri06Ctr[2],
                tri06Ctr[0], tri06Ctr[1], tri06Ctr[2],
                tri06Ctr[0], tri06Ctr[1], tri06Ctr[2],

                tri07Ctr[0], tri07Ctr[1], tri07Ctr[2],
                tri07Ctr[0], tri07Ctr[1], tri07Ctr[2],
                tri07Ctr[0], tri07Ctr[1], tri07Ctr[2],
                tri08Ctr[0], tri08Ctr[1], tri08Ctr[2],
                tri08Ctr[0], tri08Ctr[1], tri08Ctr[2],
                tri08Ctr[0], tri08Ctr[1], tri08Ctr[2],

                tri09Ctr[0], tri09Ctr[1], tri09Ctr[2],
                tri09Ctr[0], tri09Ctr[1], tri09Ctr[2],
                tri09Ctr[0], tri09Ctr[1], tri09Ctr[2],
                tri10Ctr[0], tri10Ctr[1], tri10Ctr[2],
                tri10Ctr[0], tri10Ctr[1], tri10Ctr[2],
                tri10Ctr[0], tri10Ctr[1], tri10Ctr[2],

                tri11Ctr[0], tri11Ctr[1], tri11Ctr[2],
                tri11Ctr[0], tri11Ctr[1], tri11Ctr[2],
                tri11Ctr[0], tri11Ctr[1], tri11Ctr[2],
                tri12Ctr[0], tri12Ctr[1], tri12Ctr[2],
                tri12Ctr[0], tri12Ctr[1], tri12Ctr[2],
                tri12Ctr[0], tri12Ctr[1], tri12Ctr[2],
        };
        mMeshCenters = ByteBuffer.allocateDirect(
                centersData.length * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mMeshCenters.put(centersData).position(0);
    }


    private void setIcosMesh(float size, float rnd) {
        float dim1 = size * 0.5f;
        float dim2 = dim1 * GOLDEN_RATIO;
        // Points defining the icosahedron mesh, considering point 1 to be the "top" point, 3
        // the "bottom" point.
        mMeshPoints = new float[][] {
                {dim1 + rndVr3(rnd), dim2 - rndVr3(rnd), rndVr2(rnd), 1.0f}, //0
                {-dim1 - rndVr3(rnd), dim2 - rndVr3(rnd), rndVr2(rnd), 1.0f}, //1
                {-dim1 - rndVr3(rnd), -dim2 + rndVr3(rnd), rndVr2(rnd), 1.0f}, //2
                {dim1 + rndVr3(rnd), -dim2 + rndVr3(rnd), rndVr2(rnd), 1.0f}, //3
                {rndVr2(rnd), -dim1 - rndVr3(rnd * 0.5f), dim2 + rndVr3(rnd), 1.0f}, //4
                {rndVr2(rnd), dim1 + rndVr3(rnd * 0.5f), dim2 + rndVr3(rnd), 1.0f}, //5
                {rndVr2(rnd), dim1 + rndVr3(rnd * 0.5f), -dim2 - rndVr3(rnd), 1.0f}, //6
                {rndVr2(rnd), -dim1 - rndVr3(rnd * 0.5f), -dim2 - rndVr3(rnd), 1.0f}, //7
                {dim2 + rndVr3(rnd), rndVr2(rnd), dim1 + rndVr3(rnd), 1.0f}, //8
                {dim2 + rndVr3(rnd), rndVr2(rnd), -dim1 - rndVr3(rnd), 1.0f}, //9
                {-dim2 - rndVr3(rnd), rndVr2(rnd), -dim1 - rndVr3(rnd), 1.0f}, //10
                {-dim2 - rndVr3(rnd), rndVr2(rnd), dim1 + rndVr3(rnd), 1.0f} //11
        };
        // Set orientation of the icosahedron in a "point-up" direction.
        float[] rotM = new float[16];
        Matrix.setIdentityM(rotM, 0);
        Matrix.rotateM(rotM, 0, 18.0f, 0.0f, -1.0f, 0.0f);
        Matrix.rotateM(rotM, 0, 31.72f, 0.0f, 0.0f, 1.0f);
        for (int i = 0; i < 12; i++) {
            Matrix.multiplyMV(mMeshPoints[i], 0, rotM, 0, mMeshPoints[i], 0);
        }
    }


    private void loadIcosMesh(SharedPreferences prefs) {
        float[][] meshPoints = new float[12][4];
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 4; j++) {
                meshPoints[i][j] = prefs.getFloat("shape_mesh_" + i + "_" + j, 0.0f);
            }
        }
        mMeshPoints = meshPoints;
    }


    protected void saveIcosMesh(SharedPreferences.Editor edit) {
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 4; j++) {
                edit.putFloat("shape_mesh_" + i + "_" + j, mMeshPoints[i][j]);
            }
        }
    }


    private void buildIcosShape() {
        if (mType == RNDICOS) {
            // "Flip a coin" to select bubble emitters between two different methods, for different
            // possible effects.
            float dist = 0.0f;
            if (rndVr2(1.0f) > 0.0f) {
                // Choose emitter points using the pairing criteria.
                float avgMagWeight = 3.5f;
                float[] pairCalcs = {
                        emitterPairCalc(mMeshPoints[0], mMeshPoints[3], avgMagWeight),
                        emitterPairCalc(mMeshPoints[1], mMeshPoints[2], avgMagWeight),
                        emitterPairCalc(mMeshPoints[4], mMeshPoints[7], avgMagWeight),
                        emitterPairCalc(mMeshPoints[5], mMeshPoints[6], avgMagWeight),
                        emitterPairCalc(mMeshPoints[8], mMeshPoints[11], avgMagWeight),
                        emitterPairCalc(mMeshPoints[9], mMeshPoints[10], avgMagWeight)
                };
                for (int i = 0; i < 6; i++) {
                    dist = Math.max(dist, pairCalcs[i]);
                }
                if (Vector.areFloatsEqual(pairCalcs[0], dist)) {
                    mBubbleEmitter1localCoords = mMeshPoints[0];
                    mBubbleEmitter2localCoords = mMeshPoints[3];
                } else if (Vector.areFloatsEqual(pairCalcs[1], dist)) {
                    mBubbleEmitter1localCoords = mMeshPoints[1];
                    mBubbleEmitter2localCoords = mMeshPoints[2];
                } else if (Vector.areFloatsEqual(pairCalcs[2], dist)) {
                    mBubbleEmitter1localCoords = mMeshPoints[4];
                    mBubbleEmitter2localCoords = mMeshPoints[7];
                } else if (Vector.areFloatsEqual(pairCalcs[3], dist)) {
                    mBubbleEmitter1localCoords = mMeshPoints[5];
                    mBubbleEmitter2localCoords = mMeshPoints[6];
                } else if (Vector.areFloatsEqual(pairCalcs[4], dist)) {
                    mBubbleEmitter1localCoords = mMeshPoints[8];
                    mBubbleEmitter2localCoords = mMeshPoints[11];
                } else {
                    mBubbleEmitter1localCoords = mMeshPoints[9];
                    mBubbleEmitter2localCoords = mMeshPoints[10];
                }
            } else {
                // Or choose the emitters between the farthest-apart points.
                float distCalc;
                for (int i = 0; i < 12; i++) {
                    for (int j = 0; j < 12; j++) {
                        distCalc = Vector.length(Vector.diffVV(mMeshPoints[i], mMeshPoints[j]));
                        if (distCalc > dist) {
                            dist = distCalc;
                            mBubbleEmitter1localCoords = mMeshPoints[i];
                            mBubbleEmitter2localCoords = mMeshPoints[j];
                        }
                    }
                }
            }
        } else {
            mBubbleEmitter1localCoords = mMeshPoints[9];
            mBubbleEmitter2localCoords = rndVr2(1.0f) > 0.0f ? mMeshPoints[10] : mMeshPoints[11];
        }
        mBubbleLongestEmitterLocal = Vector.length(mBubbleEmitter2localCoords) >
                Vector.length(mBubbleEmitter1localCoords) ?
                mBubbleEmitter2localCoords :
                mBubbleEmitter1localCoords;
        mSparkleEmitterLocalCoords[0] = mMeshPoints[0];
        mSparkleEmitterLocalCoords[1] = mMeshPoints[1];
        mSparkleEmitterLocalCoords[2] = mMeshPoints[2];
        mSparkleEmitterLocalCoords[3] = mMeshPoints[3];
        mSparkleEmitterLocalCoords[4] = mMeshPoints[5];
        mSparkleEmitterLocalCoords[5] = mMeshPoints[7];
        mSparkleEmitterLocalCoords[6] = mMeshPoints[9];
        mSparkleEmitterLocalCoords[7] = mMeshPoints[11];

        float[] triTop1aNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[0], mMeshPoints[5]),
                Vector.diffVV(mMeshPoints[1], mMeshPoints[5])
        );
        float[] triTop1bNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[8], mMeshPoints[5]),
                Vector.diffVV(mMeshPoints[0], mMeshPoints[5])
        );
        float[] triTop2aNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[6], mMeshPoints[0]),
                Vector.diffVV(mMeshPoints[1], mMeshPoints[0])
        );
        float[] triTop2bNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[9], mMeshPoints[0]),
                Vector.diffVV(mMeshPoints[6], mMeshPoints[0])
        );
        float[] triTop3aNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[10], mMeshPoints[6]),
                Vector.diffVV(mMeshPoints[1], mMeshPoints[6])
        );
        float[] triTop3bNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[7], mMeshPoints[6]),
                Vector.diffVV(mMeshPoints[10], mMeshPoints[6])
        );
        float[] triTop4aNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[11], mMeshPoints[10]),
                Vector.diffVV(mMeshPoints[1], mMeshPoints[10])
        );
        float[] triTop4bNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[2], mMeshPoints[10]),
                Vector.diffVV(mMeshPoints[11], mMeshPoints[10])
        );
        float[] triTop5aNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[5], mMeshPoints[11]),
                Vector.diffVV(mMeshPoints[1], mMeshPoints[11])
        );
        float[] triTop5bNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[4], mMeshPoints[11]),
                Vector.diffVV(mMeshPoints[5], mMeshPoints[11])
        );
        float[] triBottom1aNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[4], mMeshPoints[8]),
                Vector.diffVV(mMeshPoints[3], mMeshPoints[8])
        );
        float[] triBottom1bNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[5], mMeshPoints[8]),
                Vector.diffVV(mMeshPoints[4], mMeshPoints[8])
        );
        float[] triBottom2aNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[2], mMeshPoints[4]),
                Vector.diffVV(mMeshPoints[3], mMeshPoints[4])
        );
        float[] triBottom2bNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[11], mMeshPoints[4]),
                Vector.diffVV(mMeshPoints[2], mMeshPoints[4])
        );
        float[] triBottom3aNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[7], mMeshPoints[2]),
                Vector.diffVV(mMeshPoints[3], mMeshPoints[2])
        );
        float[] triBottom3bNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[10], mMeshPoints[2]),
                Vector.diffVV(mMeshPoints[7], mMeshPoints[2])
        );
        float[] triBottom4aNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[9], mMeshPoints[7]),
                Vector.diffVV(mMeshPoints[3], mMeshPoints[7])
        );
        float[] triBottom4bNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[6], mMeshPoints[7]),
                Vector.diffVV(mMeshPoints[9], mMeshPoints[7])
        );
        float[] triBottom5aNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[8], mMeshPoints[9]),
                Vector.diffVV(mMeshPoints[3], mMeshPoints[9])
        );
        float[] triBottom5bNorm = Vector.getNormal(
                Vector.diffVV(mMeshPoints[0], mMeshPoints[9]),
                Vector.diffVV(mMeshPoints[8], mMeshPoints[9])
        );

        float[] triTop1aCtr = Vector.avgVArray(mMeshPoints[5], mMeshPoints[0], mMeshPoints[1]);
        float[] triTop1bCtr = Vector.avgVArray(mMeshPoints[5], mMeshPoints[8], mMeshPoints[0]);
        float[] triTop2aCtr = Vector.avgVArray(mMeshPoints[0], mMeshPoints[6], mMeshPoints[1]);
        float[] triTop2bCtr = Vector.avgVArray(mMeshPoints[0], mMeshPoints[9], mMeshPoints[6]);
        float[] triTop3aCtr = Vector.avgVArray(mMeshPoints[6], mMeshPoints[10], mMeshPoints[1]);
        float[] triTop3bCtr = Vector.avgVArray(mMeshPoints[6], mMeshPoints[7], mMeshPoints[10]);
        float[] triTop4aCtr = Vector.avgVArray(mMeshPoints[10], mMeshPoints[11], mMeshPoints[1]);
        float[] triTop4bCtr = Vector.avgVArray(mMeshPoints[10], mMeshPoints[2], mMeshPoints[11]);
        float[] triTop5aCtr = Vector.avgVArray(mMeshPoints[11], mMeshPoints[5], mMeshPoints[1]);
        float[] triTop5bCtr = Vector.avgVArray(mMeshPoints[11], mMeshPoints[4], mMeshPoints[5]);
        float[] triBottom1aCtr = Vector.avgVArray(mMeshPoints[8], mMeshPoints[4], mMeshPoints[3]);
        float[] triBottom1bCtr = Vector.avgVArray(mMeshPoints[8], mMeshPoints[5], mMeshPoints[4]);
        float[] triBottom2aCtr = Vector.avgVArray(mMeshPoints[4], mMeshPoints[2], mMeshPoints[3]);
        float[] triBottom2bCtr = Vector.avgVArray(mMeshPoints[4], mMeshPoints[11], mMeshPoints[2]);
        float[] triBottom3aCtr = Vector.avgVArray(mMeshPoints[2], mMeshPoints[7], mMeshPoints[3]);
        float[] triBottom3bCtr = Vector.avgVArray(mMeshPoints[2], mMeshPoints[10], mMeshPoints[7]);
        float[] triBottom4aCtr = Vector.avgVArray(mMeshPoints[7], mMeshPoints[9], mMeshPoints[3]);
        float[] triBottom4bCtr = Vector.avgVArray(mMeshPoints[7], mMeshPoints[6], mMeshPoints[9]);
        float[] triBottom5aCtr = Vector.avgVArray(mMeshPoints[9], mMeshPoints[8], mMeshPoints[3]);
        float[] triBottom5bCtr = Vector.avgVArray(mMeshPoints[9], mMeshPoints[0], mMeshPoints[8]);

        float[] meshPositionData = {
                // Top 1A face
                mMeshPoints[5][0], mMeshPoints[5][1], mMeshPoints[5][2],
                mMeshPoints[0][0], mMeshPoints[0][1], mMeshPoints[0][2],
                mMeshPoints[1][0], mMeshPoints[1][1], mMeshPoints[1][2],
                // Top 1B face
                mMeshPoints[5][0], mMeshPoints[5][1], mMeshPoints[5][2],
                mMeshPoints[8][0], mMeshPoints[8][1], mMeshPoints[8][2],
                mMeshPoints[0][0], mMeshPoints[0][1], mMeshPoints[0][2],

                // Top 2A face
                mMeshPoints[0][0], mMeshPoints[0][1], mMeshPoints[0][2],
                mMeshPoints[6][0], mMeshPoints[6][1], mMeshPoints[6][2],
                mMeshPoints[1][0], mMeshPoints[1][1], mMeshPoints[1][2],
                // Top 2B face
                mMeshPoints[0][0], mMeshPoints[0][1], mMeshPoints[0][2],
                mMeshPoints[9][0], mMeshPoints[9][1], mMeshPoints[9][2],
                mMeshPoints[6][0], mMeshPoints[6][1], mMeshPoints[6][2],

                // Top 3A face
                mMeshPoints[6][0], mMeshPoints[6][1], mMeshPoints[6][2],
                mMeshPoints[10][0], mMeshPoints[10][1], mMeshPoints[10][2],
                mMeshPoints[1][0], mMeshPoints[1][1], mMeshPoints[1][2],
                // Top 3B face
                mMeshPoints[6][0], mMeshPoints[6][1], mMeshPoints[6][2],
                mMeshPoints[7][0], mMeshPoints[7][1], mMeshPoints[7][2],
                mMeshPoints[10][0], mMeshPoints[10][1], mMeshPoints[10][2],

                // Top 4A face
                mMeshPoints[10][0], mMeshPoints[10][1], mMeshPoints[10][2],
                mMeshPoints[11][0], mMeshPoints[11][1], mMeshPoints[11][2],
                mMeshPoints[1][0], mMeshPoints[1][1], mMeshPoints[1][2],
                // Top 4B face
                mMeshPoints[10][0], mMeshPoints[10][1], mMeshPoints[10][2],
                mMeshPoints[2][0], mMeshPoints[2][1], mMeshPoints[2][2],
                mMeshPoints[11][0], mMeshPoints[11][1], mMeshPoints[11][2],

                // Top 5A face
                mMeshPoints[11][0], mMeshPoints[11][1], mMeshPoints[11][2],
                mMeshPoints[5][0], mMeshPoints[5][1], mMeshPoints[5][2],
                mMeshPoints[1][0], mMeshPoints[1][1], mMeshPoints[1][2],
                // Top 5B face
                mMeshPoints[11][0], mMeshPoints[11][1], mMeshPoints[11][2],
                mMeshPoints[4][0], mMeshPoints[4][1], mMeshPoints[4][2],
                mMeshPoints[5][0], mMeshPoints[5][1], mMeshPoints[5][2],

                // Bottom 1A face
                mMeshPoints[8][0], mMeshPoints[8][1], mMeshPoints[8][2],
                mMeshPoints[4][0], mMeshPoints[4][1], mMeshPoints[4][2],
                mMeshPoints[3][0], mMeshPoints[3][1], mMeshPoints[3][2],
                // Bottom 1B face
                mMeshPoints[8][0], mMeshPoints[8][1], mMeshPoints[8][2],
                mMeshPoints[5][0], mMeshPoints[5][1], mMeshPoints[5][2],
                mMeshPoints[4][0], mMeshPoints[4][1], mMeshPoints[4][2],

                // Bottom 2A face
                mMeshPoints[4][0], mMeshPoints[4][1], mMeshPoints[4][2],
                mMeshPoints[2][0], mMeshPoints[2][1], mMeshPoints[2][2],
                mMeshPoints[3][0], mMeshPoints[3][1], mMeshPoints[3][2],
                // Bottom 2B face
                mMeshPoints[4][0], mMeshPoints[4][1], mMeshPoints[4][2],
                mMeshPoints[11][0], mMeshPoints[11][1], mMeshPoints[11][2],
                mMeshPoints[2][0], mMeshPoints[2][1], mMeshPoints[2][2],

                // Bottom 3A face
                mMeshPoints[2][0], mMeshPoints[2][1], mMeshPoints[2][2],
                mMeshPoints[7][0], mMeshPoints[7][1], mMeshPoints[7][2],
                mMeshPoints[3][0], mMeshPoints[3][1], mMeshPoints[3][2],
                // Bottom 3B face
                mMeshPoints[2][0], mMeshPoints[2][1], mMeshPoints[2][2],
                mMeshPoints[10][0], mMeshPoints[10][1], mMeshPoints[10][2],
                mMeshPoints[7][0], mMeshPoints[7][1], mMeshPoints[7][2],

                // Bottom 4A face
                mMeshPoints[7][0], mMeshPoints[7][1], mMeshPoints[7][2],
                mMeshPoints[9][0], mMeshPoints[9][1], mMeshPoints[9][2],
                mMeshPoints[3][0], mMeshPoints[3][1], mMeshPoints[3][2],
                // Bottom 4B face
                mMeshPoints[7][0], mMeshPoints[7][1], mMeshPoints[7][2],
                mMeshPoints[6][0], mMeshPoints[6][1], mMeshPoints[6][2],
                mMeshPoints[9][0], mMeshPoints[9][1], mMeshPoints[9][2],

                // Bottom 5A face
                mMeshPoints[9][0], mMeshPoints[9][1], mMeshPoints[9][2],
                mMeshPoints[8][0], mMeshPoints[8][1], mMeshPoints[8][2],
                mMeshPoints[3][0], mMeshPoints[3][1], mMeshPoints[3][2],
                // Bottom 5B face
                mMeshPoints[9][0], mMeshPoints[9][1], mMeshPoints[9][2],
                mMeshPoints[0][0], mMeshPoints[0][1], mMeshPoints[0][2],
                mMeshPoints[8][0], mMeshPoints[8][1], mMeshPoints[8][2],
        };

        float[] normalsData = {
                // Top 1A face
                triTop1aNorm[0], triTop1aNorm[1], triTop1aNorm[2],
                triTop1aNorm[0], triTop1aNorm[1], triTop1aNorm[2],
                triTop1aNorm[0], triTop1aNorm[1], triTop1aNorm[2],
                // Top 1B face
                triTop1bNorm[0], triTop1bNorm[1], triTop1bNorm[2],
                triTop1bNorm[0], triTop1bNorm[1], triTop1bNorm[2],
                triTop1bNorm[0], triTop1bNorm[1], triTop1bNorm[2],

                // Top 2A face
                triTop2aNorm[0], triTop2aNorm[1], triTop2aNorm[2],
                triTop2aNorm[0], triTop2aNorm[1], triTop2aNorm[2],
                triTop2aNorm[0], triTop2aNorm[1], triTop2aNorm[2],
                // Top 2B face
                triTop2bNorm[0], triTop2bNorm[1], triTop2bNorm[2],
                triTop2bNorm[0], triTop2bNorm[1], triTop2bNorm[2],
                triTop2bNorm[0], triTop2bNorm[1], triTop2bNorm[2],

                // Top 3A face
                triTop3aNorm[0], triTop3aNorm[1], triTop3aNorm[2],
                triTop3aNorm[0], triTop3aNorm[1], triTop3aNorm[2],
                triTop3aNorm[0], triTop3aNorm[1], triTop3aNorm[2],
                // Top 3B face
                triTop3bNorm[0], triTop3bNorm[1], triTop3bNorm[2],
                triTop3bNorm[0], triTop3bNorm[1], triTop3bNorm[2],
                triTop3bNorm[0], triTop3bNorm[1], triTop3bNorm[2],

                // Top 4A face
                triTop4aNorm[0], triTop4aNorm[1], triTop4aNorm[2],
                triTop4aNorm[0], triTop4aNorm[1], triTop4aNorm[2],
                triTop4aNorm[0], triTop4aNorm[1], triTop4aNorm[2],
                // Top 4B face
                triTop4bNorm[0], triTop4bNorm[1], triTop4bNorm[2],
                triTop4bNorm[0], triTop4bNorm[1], triTop4bNorm[2],
                triTop4bNorm[0], triTop4bNorm[1], triTop4bNorm[2],

                // Top 5A face
                triTop5aNorm[0], triTop5aNorm[1], triTop5aNorm[2],
                triTop5aNorm[0], triTop5aNorm[1], triTop5aNorm[2],
                triTop5aNorm[0], triTop5aNorm[1], triTop5aNorm[2],
                // Top 5B face
                triTop5bNorm[0], triTop5bNorm[1], triTop5bNorm[2],
                triTop5bNorm[0], triTop5bNorm[1], triTop5bNorm[2],
                triTop5bNorm[0], triTop5bNorm[1], triTop5bNorm[2],

                // Bottom 1A face
                triBottom1aNorm[0], triBottom1aNorm[1], triBottom1aNorm[2],
                triBottom1aNorm[0], triBottom1aNorm[1], triBottom1aNorm[2],
                triBottom1aNorm[0], triBottom1aNorm[1], triBottom1aNorm[2],
                // Bottom 1B face
                triBottom1bNorm[0], triBottom1bNorm[1], triBottom1bNorm[2],
                triBottom1bNorm[0], triBottom1bNorm[1], triBottom1bNorm[2],
                triBottom1bNorm[0], triBottom1bNorm[1], triBottom1bNorm[2],

                // Bottom 2A face
                triBottom2aNorm[0], triBottom2aNorm[1], triBottom2aNorm[2],
                triBottom2aNorm[0], triBottom2aNorm[1], triBottom2aNorm[2],
                triBottom2aNorm[0], triBottom2aNorm[1], triBottom2aNorm[2],
                // Bottom 2B face
                triBottom2bNorm[0], triBottom2bNorm[1], triBottom2bNorm[2],
                triBottom2bNorm[0], triBottom2bNorm[1], triBottom2bNorm[2],
                triBottom2bNorm[0], triBottom2bNorm[1], triBottom2bNorm[2],

                // Bottom 3A face
                triBottom3aNorm[0], triBottom3aNorm[1], triBottom3aNorm[2],
                triBottom3aNorm[0], triBottom3aNorm[1], triBottom3aNorm[2],
                triBottom3aNorm[0], triBottom3aNorm[1], triBottom3aNorm[2],
                // Bottom 3B face
                triBottom3bNorm[0], triBottom3bNorm[1], triBottom3bNorm[2],
                triBottom3bNorm[0], triBottom3bNorm[1], triBottom3bNorm[2],
                triBottom3bNorm[0], triBottom3bNorm[1], triBottom3bNorm[2],

                // Bottom 4A face
                triBottom4aNorm[0], triBottom4aNorm[1], triBottom4aNorm[2],
                triBottom4aNorm[0], triBottom4aNorm[1], triBottom4aNorm[2],
                triBottom4aNorm[0], triBottom4aNorm[1], triBottom4aNorm[2],
                // Bottom 4B face
                triBottom4bNorm[0], triBottom4bNorm[1], triBottom4bNorm[2],
                triBottom4bNorm[0], triBottom4bNorm[1], triBottom4bNorm[2],
                triBottom4bNorm[0], triBottom4bNorm[1], triBottom4bNorm[2],

                // Bottom 5A face
                triBottom5aNorm[0], triBottom5aNorm[1], triBottom5aNorm[2],
                triBottom5aNorm[0], triBottom5aNorm[1], triBottom5aNorm[2],
                triBottom5aNorm[0], triBottom5aNorm[1], triBottom5aNorm[2],
                // Bottom 5B face
                triBottom5bNorm[0], triBottom5bNorm[1], triBottom5bNorm[2],
                triBottom5bNorm[0], triBottom5bNorm[1], triBottom5bNorm[2],
                triBottom5bNorm[0], triBottom5bNorm[1], triBottom5bNorm[2]
        };

        float[] centersData = {
                // Top 1A face
                triTop1aCtr[0], triTop1aCtr[1], triTop1aCtr[2],
                triTop1aCtr[0], triTop1aCtr[1], triTop1aCtr[2],
                triTop1aCtr[0], triTop1aCtr[1], triTop1aCtr[2],
                // Top 1B face
                triTop1bCtr[0], triTop1bCtr[1], triTop1bCtr[2],
                triTop1bCtr[0], triTop1bCtr[1], triTop1bCtr[2],
                triTop1bCtr[0], triTop1bCtr[1], triTop1bCtr[2],

                // Top 2A face
                triTop2aCtr[0], triTop2aCtr[1], triTop2aCtr[2],
                triTop2aCtr[0], triTop2aCtr[1], triTop2aCtr[2],
                triTop2aCtr[0], triTop2aCtr[1], triTop2aCtr[2],
                // Top 2B face
                triTop2bCtr[0], triTop2bCtr[1], triTop2bCtr[2],
                triTop2bCtr[0], triTop2bCtr[1], triTop2bCtr[2],
                triTop2bCtr[0], triTop2bCtr[1], triTop2bCtr[2],

                // Top 3A face
                triTop3aCtr[0], triTop3aCtr[1], triTop3aCtr[2],
                triTop3aCtr[0], triTop3aCtr[1], triTop3aCtr[2],
                triTop3aCtr[0], triTop3aCtr[1], triTop3aCtr[2],
                // Top 3B face
                triTop3bCtr[0], triTop3bCtr[1], triTop3bCtr[2],
                triTop3bCtr[0], triTop3bCtr[1], triTop3bCtr[2],
                triTop3bCtr[0], triTop3bCtr[1], triTop3bCtr[2],

                // Top 4A face
                triTop4aCtr[0], triTop4aCtr[1], triTop4aCtr[2],
                triTop4aCtr[0], triTop4aCtr[1], triTop4aCtr[2],
                triTop4aCtr[0], triTop4aCtr[1], triTop4aCtr[2],
                // Top 4B face
                triTop4bCtr[0], triTop4bCtr[1], triTop4bCtr[2],
                triTop4bCtr[0], triTop4bCtr[1], triTop4bCtr[2],
                triTop4bCtr[0], triTop4bCtr[1], triTop4bCtr[2],

                // Top 5A face
                triTop5aCtr[0], triTop5aCtr[1], triTop5aCtr[2],
                triTop5aCtr[0], triTop5aCtr[1], triTop5aCtr[2],
                triTop5aCtr[0], triTop5aCtr[1], triTop5aCtr[2],
                // Top 5B face
                triTop5bCtr[0], triTop5bCtr[1], triTop5bCtr[2],
                triTop5bCtr[0], triTop5bCtr[1], triTop5bCtr[2],
                triTop5bCtr[0], triTop5bCtr[1], triTop5bCtr[2],

                // Bottom 1A face
                triBottom1aCtr[0], triBottom1aCtr[1], triBottom1aCtr[2],
                triBottom1aCtr[0], triBottom1aCtr[1], triBottom1aCtr[2],
                triBottom1aCtr[0], triBottom1aCtr[1], triBottom1aCtr[2],
                // Bottom 1B face
                triBottom1bCtr[0], triBottom1bCtr[1], triBottom1bCtr[2],
                triBottom1bCtr[0], triBottom1bCtr[1], triBottom1bCtr[2],
                triBottom1bCtr[0], triBottom1bCtr[1], triBottom1bCtr[2],

                // Bottom 2A face
                triBottom2aCtr[0], triBottom2aCtr[1], triBottom2aCtr[2],
                triBottom2aCtr[0], triBottom2aCtr[1], triBottom2aCtr[2],
                triBottom2aCtr[0], triBottom2aCtr[1], triBottom2aCtr[2],
                // Bottom 2B face
                triBottom2bCtr[0], triBottom2bCtr[1], triBottom2bCtr[2],
                triBottom2bCtr[0], triBottom2bCtr[1], triBottom2bCtr[2],
                triBottom2bCtr[0], triBottom2bCtr[1], triBottom2bCtr[2],

                // Bottom 3A face
                triBottom3aCtr[0], triBottom3aCtr[1], triBottom3aCtr[2],
                triBottom3aCtr[0], triBottom3aCtr[1], triBottom3aCtr[2],
                triBottom3aCtr[0], triBottom3aCtr[1], triBottom3aCtr[2],
                // Bottom 3B face
                triBottom3bCtr[0], triBottom3bCtr[1], triBottom3bCtr[2],
                triBottom3bCtr[0], triBottom3bCtr[1], triBottom3bCtr[2],
                triBottom3bCtr[0], triBottom3bCtr[1], triBottom3bCtr[2],

                // Bottom 4A face
                triBottom4aCtr[0], triBottom4aCtr[1], triBottom4aCtr[2],
                triBottom4aCtr[0], triBottom4aCtr[1], triBottom4aCtr[2],
                triBottom4aCtr[0], triBottom4aCtr[1], triBottom4aCtr[2],
                // Bottom 4B face
                triBottom4bCtr[0], triBottom4bCtr[1], triBottom4bCtr[2],
                triBottom4bCtr[0], triBottom4bCtr[1], triBottom4bCtr[2],
                triBottom4bCtr[0], triBottom4bCtr[1], triBottom4bCtr[2],

                // Bottom 5A face
                triBottom5aCtr[0], triBottom5aCtr[1], triBottom5aCtr[2],
                triBottom5aCtr[0], triBottom5aCtr[1], triBottom5aCtr[2],
                triBottom5aCtr[0], triBottom5aCtr[1], triBottom5aCtr[2],
                // Bottom 5B face
                triBottom5bCtr[0], triBottom5bCtr[1], triBottom5bCtr[2],
                triBottom5bCtr[0], triBottom5bCtr[1], triBottom5bCtr[2],
                triBottom5bCtr[0], triBottom5bCtr[1], triBottom5bCtr[2]
        };

        mMeshPositions = ByteBuffer.allocateDirect(
                meshPositionData.length * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mMeshPositions.put(meshPositionData).position(0);
        mMeshNormals = ByteBuffer.allocateDirect(
                normalsData.length * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mMeshNormals.put(normalsData).position(0);
        mMeshCenters = ByteBuffer.allocateDirect(
                centersData.length * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mMeshCenters.put(centersData).position(0);

        float[] textureCoordData = getTextureCoordsZX(meshPositionData);
        mTextureCoords0 = ByteBuffer.allocateDirect(
                textureCoordData.length * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureCoords0.put(textureCoordData).position(0);
        textureCoordData = getTextureCoordsXY(meshPositionData);
        mTextureCoords1 = ByteBuffer.allocateDirect(
                textureCoordData.length * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureCoords1.put(textureCoordData).position(0);
        textureCoordData = getTextureCoordsYZ(meshPositionData);
        mTextureCoords2 = ByteBuffer.allocateDirect(
                textureCoordData.length * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureCoords2.put(textureCoordData).position(0);
    }


    // Compute criteria for choosing an emitter pair.
    // Currently combination of the length of the pair's vector average and pair's mutual distance,
    // weighted more towards the vector average length.
    private float emitterPairCalc(float [] pt1, float[] pt2, float weight) {
        return weight * Vector.length(Vector.avgVArray(pt1, pt2))
                + Vector.length(Vector.diffVV(pt1, pt2));
    }


    // Random float from 0.0 to range
    private float rndVar(float range) {
        return range * ((float) Math.random());
    }

    // Random float from -0.5 * range to 0.5 * range
    private float rndVr2(float range) {
        return range * ((float) Math.random() - 0.5f);
    }

    // Random float from -0.25 * range to 0.75 * range
    private float rndVr3(float range) {
        return range * ((float) Math.random() - 0.25f);
    }

    // Project normalized mesh points to the X-Z plane and retrieve texture coordinates.
    private float[] getTextureCoordsZX(float[] meshPosData) {
        float maxX = 0.0f;
        float minX = 0.0f;
        float maxZ = 0.0f;
        float minZ = 0.0f;
        int numVerts = mType == CUBE || mType == RNDDODEC ? 36 : 60;
        for (int i = 0; i < numVerts * 3; i += 3) {
            maxX = meshPosData[i] > maxX ? meshPosData[i] : maxX;
            minX = meshPosData[i] < minX ? meshPosData[i] : minX;
            maxZ = meshPosData[i + 2] > maxZ ? meshPosData[i + 2] : maxZ;
            minZ = meshPosData[i + 2] < minZ ? meshPosData[i + 2] : minZ;
        }
        float normFactX = 1.0f / (maxX - minX);
        float normFactZ = 1.0f / (maxZ - minZ);
        float[] texPosData = new float[120];
        for (int i = 0; i < numVerts * 6; i += 6) {
            texPosData[i/3] = normFactX * (meshPosData[i/2] - minX);
            texPosData[(i/3) + 1] = normFactZ * (meshPosData[i/2 + 2] - minZ);
        }
        return texPosData;
    }


    private float[] getTextureCoordsYZ(float[] meshPosData) {
        float maxY = 0.0f;
        float minY = 0.0f;
        float maxZ = 0.0f;
        float minZ = 0.0f;
        int numVerts = mType == CUBE || mType == RNDDODEC ? 36 : 60;
        for (int i = 0; i < numVerts * 3; i += 3) {
            maxY = meshPosData[i + 1] > maxY ? meshPosData[i + 1] : maxY;
            minY = meshPosData[i + 1] < minY ? meshPosData[i + 1] : minY;
            maxZ = meshPosData[i + 2] > maxZ ? meshPosData[i + 2] : maxZ;
            minZ = meshPosData[i + 2] < minZ ? meshPosData[i + 2] : minZ;
        }
        float normFactY = 1.0f / (maxY - minY);
        float normFactZ = 1.0f / (maxZ - minZ);
        float[] texPosData = new float[120];
        for (int i = 0; i < numVerts * 6; i += 6) {
            texPosData[i/3] = normFactZ * (meshPosData[i/2 + 2] - minZ);
            texPosData[(i/3) + 1] = -normFactY * (meshPosData[i/2 + 1] - minY);
        }
        return texPosData;
    }


    private float[] getTextureCoordsXY(float[] meshPosData) {
        float maxX = 0.0f;
        float minX = 0.0f;
        float maxY = 0.0f;
        float minY = 0.0f;
        int numVerts = mType == CUBE || mType == RNDDODEC ? 36 : 60;
        for (int i = 0; i < numVerts * 3; i += 3) {
            maxX = meshPosData[i] > maxX ? meshPosData[i] : maxX;
            minX = meshPosData[i] < minX ? meshPosData[i] : minX;
            maxY = meshPosData[i + 1] > maxY ? meshPosData[i + 1] : maxY;
            minY = meshPosData[i + 1] < minY ? meshPosData[i + 1] : minY;
        }
        float normFactX = 1.0f / (maxX - minX);
        float normFactY = 1.0f / (maxY - minY);
        float[] texPosData = new float[120];
        for (int i = 0; i < numVerts * 6; i += 6) {
            texPosData[i/3] = normFactX * (meshPosData[i/2] - minX);
            texPosData[i/3 + 1] = -normFactY * (meshPosData[i/2 + 1] - minY);
        }
        return texPosData;
    }


    /**
     * Initialize a performance mode rotation.
     *
     * @param beat The beat of the music in millis.
     * @param revs The number of revolutions per beat desired.
     * @param length The length of time in millis to maintain desired rotation orientation.
     * @param type The type of rotation desired, can be either corkscrew, cartwheel, or carousel.
     * @param steady Virtual boolean (int 1 or 0) for whether to use a steady/straight rotation.
     */
    protected void initPerformanceRot(int beat, int revs, int length, int type, int steady) {
        mPerfMode = type;
        mPerfLengthMillis = length;
        // Period in ms per revolution, unless revs is zero.
        float period = revs == 0 ? 0.0f : (float) beat / (float) revs;
        // Convert to frames per revolution.
        mPeriod = period / 16.67f;
        // Set up "performance space" to determine rotation axis and offset vector if the axis
        // is too parallel to the emitter.
        float[][] pSpace = perfSpace();
        float[] longestEmitter = new float[4];
        Matrix.multiplyMV(longestEmitter, 0, mOrientM, 0, mBubbleLongestEmitterLocal, 0);
        longestEmitter = Vector.norm(longestEmitter);
        float[] axisVec = new float[3];
        float[] offsetVec = new float[3];
        // nearOne value set for 35 deg angle.
        float nearOne = 0.819f;
        switch (mPerfMode) {
            case CORKSCREW:
                axisVec = pSpace[0];
                offsetVec = pSpace[1];
                break;
            case CARTWHEEL:
                axisVec = pSpace[1];
                offsetVec = pSpace[2];
                break;
            case CAROUSEL:
                axisVec = pSpace[2];
                offsetVec = pSpace[0];
                break;
        }
        // Set boolean for an alternate offset axis depending on whether longestEmitter vec is
        // within 35 deg of the desired axis.
        mUsePerfOffsetAxis = Math.abs(Vector.dotVV(longestEmitter, axisVec)) > nearOne;
        mUseAltOffsetAxis = Math.abs(Vector.dotVV(longestEmitter,
                Vector.norm(Vector.sumVV(axisVec, offsetVec)))) > nearOne;
        mRotSteady = steady == 1;
        mPerfRotStartTime = System.nanoTime();
        mPerfRotTime = 0.0f;
    }


    // Execute per-frame performance rotation computations, updating axis, offset, and rot speed.
    protected void performanceRot(float frameFactor) {
        float[][] pSpace = perfSpace();
        float[] axisVec = new float[3];
        float[] offsetVec = new float[3];
        switch (mPerfMode) {
            case CORKSCREW:
                axisVec = pSpace[0];
                offsetVec = mUseAltOffsetAxis ? Vector.negV(pSpace[1]) : pSpace[1];
                break;
            case CARTWHEEL:
                axisVec = pSpace[1];
                offsetVec = mUseAltOffsetAxis ? Vector.negV(pSpace[2]) : pSpace[2];
                break;
            case CAROUSEL:
                axisVec = pSpace[2];
                offsetVec = mUseAltOffsetAxis ? Vector.negV(pSpace[0]) : pSpace[0];
                break;
        }
        mAxis = mUsePerfOffsetAxis ? Vector.sumVV(axisVec, offsetVec) : axisVec;
        // Rotation speed in degrees per frame, unless period is 0 in which case make rotation 0.
        float baseSpd = mPeriod == 0.0f ? 0.0f : 360.0f / mPeriod;
        float msPeriod = mPeriod * 16.67f;
        if (mRotSteady) {
            mRotSpd = baseSpd;
        } else {
            mPerfRotTime += 16.67f * frameFactor;
            mRotSpd = baseSpd + SLHelper.sineCycle(mPerfRotTime, baseSpd, msPeriod);
        }
    }


    // Compute "performance space" where U is the unit velocity vector, oriented with the force
    // vector mUnitForce.
    protected float[][] perfSpace() {
        float[][] pSpace = new float[3][3];
        pSpace[0] = Vector.areEqual(mVel, Vector.ZERO) ? mAltVel : Vector.norm(mVel);
        float[] uForce = mUnitForce;
        if (Vector.areParallel(uForce, pSpace[0])) {
            uForce = mAltUnitForce1;
            if (Vector.areParallel(uForce, pSpace[0])) {
                uForce = mAltUnitForce2;
            }
        }
        pSpace[2] = Vector.norm(Vector.crossVV(pSpace[0], uForce));
        pSpace[1] = Vector.crossVV(pSpace[2], pSpace[0]);
        return pSpace;
    }


    protected void loadParams(SharedPreferences prefs) {
        float[] pos = new float[3];
        float[] vel = new float[3];
        float[] axis = new float[3];
        for (int i = 0; i < 3; i++) {
            pos[i] = prefs.getFloat("shape_pos_vec_" + i, 0.0f);
            vel[i] = prefs.getFloat("shape_vel_vec_" + i, 0.0f);
            axis[i] = prefs.getFloat("shape_axis_vec_" + i, 1.0f);
        }
        mPos = pos;
        mVel = vel;
        mAxis = axis;
        mRotSpd = prefs.getFloat(SLSurfaceView.SHAPE_ROT_SPD, 0.0f);
        float[] orientM = new float[16];
        for (int i = 0; i < 16; i++) {
            orientM[i] = prefs.getFloat("shape_orient_mat_" + i, 0.0f);
        }
        mOrientM = orientM;
    }


    protected void saveToPrefs(SharedPreferences.Editor edit) {
        edit.putBoolean(SLSurfaceView.SHAPE_STORED, true);
        int type;
        switch (mType) {
            case CUBE:
                type = CUBE;
                saveCubeMesh(edit);
                break;
            case ICOS:
                type = ICOS;
                saveIcosMesh(edit);
                break;
            case RNDDODEC:
                type = RNDDODEC;
                saveCubeMesh(edit);
                break;
            case RNDICOS:
                type = RNDICOS;
                saveIcosMesh(edit);
                break;
            default:
                type = CUBE;
                saveCubeMesh(edit);
        }
        edit.putInt(SLSurfaceView.SHAPE_TYPE, type);
        for (int i = 0; i < 3; i++) {
            edit.putFloat("shape_pos_vec_" + i, mPos[i]);
            edit.putFloat("shape_vel_vec_" + i, mVel[i]);
            edit.putFloat("shape_axis_vec_" + i, mAxis[i]);
        }
        edit.putFloat(SLSurfaceView.SHAPE_ROT_SPD, mRotSpd);
        for (int i = 0; i < 16; i++) {
            edit.putFloat("shape_orient_mat_" + i, mOrientM[i]);
        }
    }


}
