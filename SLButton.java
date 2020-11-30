package com.pathfinder.shapeandlight;

import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by Lionden on 6/9/2016.
 *
 * Class to contain functionality for GUI buttons, comprised of a basic textured 2-triangle square
 * object rendered in an orthogonal gl projection.
 */

@SuppressWarnings("WeakerAccess")
class SLButton {
    private static final String LOG_TAG = SLButton.class.getSimpleName();
    protected static final float BUTTON_DEPTH = 0.0f;

    protected float mX;
    protected float mY;
    protected float mWidth;
    protected float mHeight;

    protected int mIconTexHandle;
    protected float[] mColor;
    protected float[] mPressedColor;
    protected volatile boolean mEnabled;
    protected boolean mIsHelpButton;
    protected volatile int mPressPointerID;
    protected volatile long mPressedTime;
    protected boolean mLongPressRegistered;

    protected FloatBuffer mButtonPositions;
    protected FloatBuffer mTextureCoords;

    // Static field used by method which detects and free-selects FreeMover objects when they're
    // touched, to prevent selection when a button was pressed.
    public static boolean a_button_was_pressed;

    /**
     * Constructor for a button object.
     *
     * @param x X-coordinate of the lower-left corner defining the bounds.
     * @param y Y-coordinate of the lower-left corner defining the bounds (normal OpenGL orientation).
     * @param width Width of the bounds.
     * @param height Height of the bounds.
     */
    public SLButton(float x, float y, float width, float height,
                    float[] color, float[] pressedColor, int iconTexHandle) {
        mX = x;
        mY = y;
        mWidth = width;
        mHeight = height;
        mColor = color;
        mEnabled = true;
        mIsHelpButton = false;
        mLongPressRegistered = false;
        mPressPointerID = MotionEvent.INVALID_POINTER_ID;
        mPressedColor = pressedColor;
        mIconTexHandle = iconTexHandle;
        mButtonPositions = ByteBuffer.allocateDirect(18 * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        setButtonPosData();
        float[] textureCoordData = new float[] {
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 0.0f
        };
        mTextureCoords = ByteBuffer
                .allocateDirect(textureCoordData.length * SLHelper.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureCoords.put(textureCoordData).position(0);
    }

    // Set a new button position from a center coordinate, used for "touch-following"
    // interface elements.
    public void setPos(float x, float y) {
        mX = x - (0.5f * mWidth);
        mY = y - (0.5f * mHeight);
        setButtonPosData();
    }

    // Set a new square-shaped button size and position, used for sparkle flare in medium settings.
    public void setSizeAndPos(float size, float x, float y) {
        mX = x - (0.5f * size);
        mY = y - (0.5f * size);
        mWidth = size;
        mHeight = size;
        setButtonPosData();
    }

    // Set the position data into the mButtonPositions float buffer.
    private void setButtonPosData() {
        mButtonPositions.put(
                new float[] {
                        mX, mY, BUTTON_DEPTH,
                        mX + mWidth, mY, BUTTON_DEPTH,
                        mX + mWidth, mY + mHeight, BUTTON_DEPTH,
                        mX, mY, BUTTON_DEPTH,
                        mX + mWidth, mY + mHeight, BUTTON_DEPTH,
                        mX, mY + mHeight, BUTTON_DEPTH
                }
        ).position(0);
    }

    /**
     * Initializes the button state for a tap that lies within, checking whether passed-in pixel
     * coordinates of a down event lie inside the button bounds, and assigns the applicable
     * pointerID if the button is in enabled state.
     *
     * @param x X-coordinate of the pixel.
     * @param y Y-coordinate of the pixel, in normal OpenGL orientation (need to convert from
     *          flipped device pixel coordinate when passed in).
     * @param id The pointerID of the touch event being tested.
     * @param helpMode Boolean for whether help mode is on, in which case override disabled state.
     * @return Boolean whether the pixel is inside the bounds.
     */
    public boolean initInsideTap(float x, float y, int id, boolean helpMode) {
        boolean inside = (mEnabled || helpMode)
                && x > mX && x < mX + mWidth
                && y > mY && y < mY + mHeight;
        if (inside) {
            mPressPointerID = id;
            mPressedTime = System.nanoTime();
            a_button_was_pressed = true;
        }
        return inside;
    }

    // Specialized initInsideTap methods for use by eye/view control 'buttons'.
    public boolean initInsideTapLeft(float x, float y, int id) {
        boolean inside = mEnabled && x  < mX + mWidth && y < mY + mHeight;
        mPressPointerID = inside ? id : MotionEvent.INVALID_POINTER_ID;
        return inside;
    }

    public boolean initInsideTapRight(float x, float y, int id) {
        boolean inside = mEnabled && x > mX && y < mY + mHeight;
        mPressPointerID = inside ? id : MotionEvent.INVALID_POINTER_ID;
        return inside;
    }


    /**
     * Checks whether the passed-in pointerID of an up event is the same as the one which initially
     * pressed it. If so, reset the mPressPointerID field and return the boolean. But even if true,
     * return a false boolean if mLongPressRegistered was "true" so the simple-click function of
     * the button will not be executed, since the long-click function has been called.
     *
     * @param id The pointerID of the up event.
     * @return Boolean whether it is the pointer which pressed the button, and a long-click hasn't
     * been executed.
     */
    public boolean isPointerReleasingNotLong(int id) {
        boolean released = id == mPressPointerID;
        boolean wasLongPressRegistered = mLongPressRegistered;
        if (released) {
            mPressPointerID = MotionEvent.INVALID_POINTER_ID;
            // Reset long press registered boolean back to false.
            mLongPressRegistered = false;
        }
        return released && !wasLongPressRegistered;
    }

    /**
     * Basic check for whether a passed-in pointerID is the same as the one which initially
     * pressed it.
     *
     * @param id The pointerID of the up event.
     * @return Boolean whether it is the pointer which pressed the button.
     */
    public boolean isPointerThatPressed(int id) {
        return id == mPressPointerID;
    }



    public boolean isPressed() {
        return mPressPointerID != MotionEvent.INVALID_POINTER_ID;
    }

    public void unPress() {
        mPressPointerID = MotionEvent.INVALID_POINTER_ID;
    }

    public void registerLongPress() {
        mLongPressRegistered = true;
    }

    public boolean isLongPressRegistered() {
        return mLongPressRegistered;
    }

    public boolean enabled() {
        return mEnabled;
    }

    public void enable() {
        mEnabled = true;
    }

    public void disable() {
        mEnabled = false;
    }

    public void setEnabled(boolean enable) {
        mEnabled = enable;
    }


    /**
     * Checks whether passed-in pixel coordinates lie inside the button bounds for buttons
     * arranged hexagonally.
     *
     * @param x X-coordinate of the pixel.
     * @param y Y-coordinate of the pixel, in normal OpenGL orientation (need to convert from
     *          flipped device pixel coordinate when passed in).
     * @return Boolean whether the pixel is inside the bounds.
     */
    public boolean initInsideTapHex(float x, float y, int id) {
        float dY = 0.268f * mHeight * 0.5f * 0.5f;
        boolean inside = x > mX && x < mX + mWidth && y > mY + dY && y < mY + mHeight - dY;
        if (inside) {
            mPressPointerID = id;
            a_button_was_pressed = true;
        }
        return inside;
    }

}
