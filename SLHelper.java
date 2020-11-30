package com.pathfinder.shapeandlight;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Lionden on 5/25/2016.
 */

@SuppressWarnings("WeakerAccess")
public class SLHelper {
    public static final String LOG_TAG = SLHelper.class.getSimpleName();

    public static final float[] WHI_COLOR_VEC = {1.0f, 1.0f, 1.0f, 1.0f};
    public static final float[] RED_COLOR_VEC = {1.0f, 0.0f, 0.0f, 1.0f};
    public static final float[] YEL_COLOR_VEC = {1.0f, 1.0f, 0.0f, 1.0f};
    public static final float[] GRN_COLOR_VEC = {0.0f, 1.0f, 0.0f, 1.0f};
    public static final float[] CYA_COLOR_VEC = {0.0f, 1.0f, 1.0f, 1.0f};
    public static final float[] BLU_COLOR_VEC = {0.0f, 0.0f, 1.0f, 1.0f};
    public static final float[] MAG_COLOR_VEC = {1.0f, 0.0f, 1.0f, 1.0f};
    public static final float[] BLK_COLOR_VEC = {0.0f, 0.0f, 0.0f, 0.0f};
    public static final float[] DEF_BUTTON_COL = {1.0f, 1.0f, 1.0f, 0.25f};
    public static final float[] HIGHLIT_BUTTON_COL = {0.80f, 0.90f, 1.0f, 0.60f};
    public static final float[] HIGHLIT_BUTTON_COL2 = {0.46f, 0.46f, 1.0f, 0.92f};
    public static final float[] PRESSED_BUTTON_COL = {1.0f, 1.0f, 1.0f, 0.92f};
    public static final float[] DISABLED_BUTTON_COL = {1.0f, 1.0f, 1.0f, 0.10f};
    public static final float[] HELP_BUTTON_COL = {0.25f, 0.75f, 1.0f, 0.60f};

    /** How many bytes per float. */
    public static final int BYTES_PER_FLOAT = 4;
    public static final int BYTES_PER_INT = 4;
    /** Size of the position data in elements. */
    public static final int POSITION_DATA_SIZE = 3;

    public static final int COLOR_DATA_SIZE = 4;
    /** Size of the texture coordinate data in elements. */
    public static final int TEXTURE_COORD_DATA_SIZE = 2;
    /** Maximum distance allowed from the center point. */
    public static final float MAX_DISTANCE_CENTER = 750.0f;


    public static String loadShaderCode(Context context, int resourceId) {
        InputStream inputStream = context.getResources().openRawResource(resourceId);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String nextLine;
        StringBuilder body = new StringBuilder();
        try {
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        }
        catch (IOException e) {
            Log.e(LOG_TAG, "Error loading shader code from resource file.");
            return null;
        }
        return body.toString();
    }

    /**
     * Helper function to compile a shader.
     *
     * @param shaderType The shader type.
     * @param shaderSource The shader source code.
     * @return An OpenGL handle to the shader.
     */
    public static int compileShader(int shaderType, String shaderSource) {
        int shaderHandle = GLES20.glCreateShader(shaderType);
        if (shaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(shaderHandle, shaderSource);
            // Compile the shader.
            GLES20.glCompileShader(shaderHandle);
            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Log.e(LOG_TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }
        if (shaderHandle == 0) {
            throw new RuntimeException("Error creating shader.");
        }
        return shaderHandle;
    }

    /**
     * Helper function to compile and link a program.
     *
     * @param vertexShaderHandle An OpenGL handle to an already-compiled vertex shader.
     * @param fragmentShaderHandle An OpenGL handle to an already-compiled fragment shader.
     * @param attributes Attributes that need to be bound to the program.
     * @return An OpenGL handle to the program.
     */
    public static int createAndLinkProgram(int vertexShaderHandle, int fragmentShaderHandle,
                                     String[] attributes) {
        int programHandle = GLES20.glCreateProgram();
        if (programHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);
            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);
            // Bind attributes
            if (attributes != null) {
                int size = attributes.length;
                for (int i = 0; i < size; i++)
                {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }
            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);
            // Get the link status.
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                Log.e(LOG_TAG, "Error compiling program: "
                        + GLES20.glGetProgramInfoLog(programHandle));
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }
        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }
        return programHandle;
    }


    public static int loadTexture(Context context, int resourceId)
    {
        int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        if (textureHandle[0] != 0)
        {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling
            // Read in the resource
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            // Set filtering
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR_MIPMAP_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }
        if (textureHandle[0] == 0) throw new RuntimeException("Error loading texture.");
        return textureHandle[0];
    }

    // Return a ColorDrawable for given gl RGB float values.
    public static ColorDrawable getColorDrawable(float r, float g, float b) {
        int red = (int) (r * 255.0f);
        int grn = (int) (g * 255.0f);
        int blu = (int) (b * 255.0f);
        float[] hsv = new float[3];
        android.graphics.Color.RGBToHSV(red, grn, blu, hsv);
        return new ColorDrawable(android.graphics.Color.HSVToColor(hsv));
    }

    /**
     * Method that computes the brighter color used to draw a point light source with a given
     * base light color.
     *
     * @param baseCol The base color of the light source.
     * @param fLow The low parameter which sets the brightness of the light source, the higher it is
     *             the brighter the point source appears to be.
     * @param fHi The high parameter which also sets the brightness of the light source.
     * @return The brighter RGB color used to draw the point itself.
     */
    public static float[] getBrightRGB(float[] baseCol, float fLow, float fHi) {
        if (baseCol[0] + baseCol[2] > 1.0f)
            fHi = 1.25f * fHi / (baseCol[0] + baseCol[2]);
        float fHiNoGr = fHi - ((fHi - fLow) * baseCol[1]);
        float redComp = baseCol[0] + fLow * baseCol[1] + fLow * baseCol[2];
        float grnComp = fHiNoGr * baseCol[0] + baseCol[1] + fHiNoGr * baseCol[2];
        float bluComp = fLow * baseCol[0] + fLow * baseCol[1] + baseCol[2];
        return Vector.clampVec3(1.0f, new float[] {redComp, grnComp, bluComp});
    }

    // Return max value between floats a & b, using a smoothening function, the factor k defining
    // its 'sharpness'.
    public static float smoothMax(float a, float b, float k) {
        return (1.0f / k) * (float) Math.log(Math.exp(k * a) + Math.exp(k * b));
    }

    // Return the min value between floats a & b by employing smoothMax with a negative factor.
    public static float smoothMin(float a, float b, float k) {
        return smoothMax(a, b, -k);
    }

    /**
     * Convenience method to implement a sinusoidal/oscillating behavior.
     *
     * @param timeMillis The incrementing time in milliseconds.
     * @param amplitude The amplitude of the oscillating.
     * @param periodMillis The period of the oscillating in milliseconds.
     * @return The oscillating quantity, of the same units as 'amplitude'.
     */
    public static float sineCycle(double timeMillis, float amplitude, double periodMillis) {
        return amplitude * (float) Math.sin(2.0 * Math.PI * timeMillis / periodMillis);
    }


    /**
     * Method to map a linear value to a position on the RGB "rainbow", where 0 begins at
     * red, advances through shades of orange, yellow, green, cyan, blue, magenta, and then
     * back to red.
     *
     * @param period The period of the mapping
     * @param value The "x" value being entered.
     * @return The RGB color (with default alpha of 1.0).
     */
    public static float[] rainbowMap(float period, float value) {
        double fctr = 2.0 * Math.PI / period;
        double grnVal = value - (period / 3.0);
        double bluVal = value - (2.0 * period / 3.0);
        float redComp = Math.max(0.0f, Math.min(1.0f, (float) Math.cos(fctr * value) + 0.5f));
        float grnComp = Math.max(0.0f, Math.min(1.0f, (float) Math.cos(fctr * grnVal) + 0.5f));
        float bluComp = Math.max(0.0f, Math.min(1.0f, (float) Math.cos(fctr * bluVal) + 0.5f));
        return Vector.convertToVec4(new float[] {redComp, grnComp, bluComp});
    }


}
