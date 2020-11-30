package com.pathfinder.shapeandlight;

import android.util.Log;
import java.text.DecimalFormat;


/**
 * Created by Lionden on 5/13/2016.
 *
 * Class for static vector operations for use in conjunction with OpenGL matrices.
 * Vectors are 3-member float arrays, anchored at the origin. Includes 3x3 matrix operations
 * for unit space transforms, matrices being column-major 9-member float arrays.
 */

@SuppressWarnings("WeakerAccess")
public class Vector {

    private static final String LOG_TAG = Vector.class.getSimpleName();
    public static final float[] U = {1.0f, 0.0f, 0.0f};
    public static final float[] V = {0.0f, 1.0f, 0.0f};
    public static final float[] W = {0.0f, 0.0f, 1.0f};
    public static final float[] ZERO = {0.0f, 0.0f, 0.0f};
    public static final float[] ZERO4 = {0.0f, 0.0f, 0.0f, 0.0f};
    public static final float SIGMA = 0.0005f;


    /**
     * Vector addition, allows any number of components. If the number isn't the same for both
     * vectors the result will have the lesser number.
     *
     * @param v1 Vector to be added.
     * @param v2 Vector to be added.
     * @return The vector sum.
     */
    public static float[] sumVV(float[] v1, float[] v2) {
        int len = Math.min(v1.length, v2.length);
        float[] sum = new float[len];
        for (int i = 0; i < len; i++) {
            sum[i] = v1[i] + v2[i];
        }
        return sum;
    }

    /**
     * Sum of an array of vectors, allows for up to 4 components.
     *
     * @param vecs The array of vectors.
     * @return The sum of the vector array.
     */
    public static float[] sumVArray(float[]... vecs) {
        float[] sum = ZERO4.clone();
        for (float[] vec : vecs) {
            sum = sumVV(sum, vec);
        }
        return sum;
    }

    /**
     * Vector subtraction, for vectors of length 3 or 4.
     *
     * @param v1 First vector.
     * @param v2 Vector to be subtracted from v1.
     * @return The vector difference.
     */
    public static float[] diffVV(float[] v1, float[] v2) {
        int len = Math.min(v1.length, v2.length);
        float[] sum = new float[len];
        for (int i = 0; i < len; i++) {
            sum[i] = v1[i] - v2[i];
        }
        return sum;
    }

    /**
     * Compute vector average for array of vectors.
     *
     * @param vecs Array of vectors to be averaged.
     * @return The vector average.
     */
    public static float[] avgVArray(float[]... vecs) {
        float numF = (float) vecs.length;
        float[] sum = sumVArray(vecs);
        return Vector.scaleSV(1.0f / numF, sum);
    }

    /**
     * Straight vector product, allows any number of components. If the number isn't the same
     * for both vectors the result will have the lesser number.
     *
     * @param v1 Vector to be multiplied.
     * @param v2 Vector to be multiplied.
     * @return The straight vector product.
     */
    public static float[] multVV(float[] v1, float[] v2) {
        int len = Math.min(v1.length, v2.length);
        float[] product = new float[len];
        for (int i = 0; i < len; i++) {
            product[i] = v1[i] * v2[i];
        }
        return product;
    }

    /**
     * Multiply vector by a scalar, allows any number of components.
     *
     * @param s The scalar float.
     * @param v The vector.
     * @return The vector product.
     */
    public static float[] scaleSV(float s, float[] v) {
        int len = v.length;
        float[] scaledVec = new float[len];
        for (int i = 0; i < len; i++) {
            scaledVec[i] = s * v[i];
        }
        return scaledVec;
    }

    /**
     * Negative of a vector.
     *
     * @param v The vector.
     * @return The vector's negative, or 'flipped' in the opposite direction.
     */
    public static float[] negV(float[] v) {
        return scaleSV(-1.0f, v);
    }

    /**
     * Dot product of 2 3-dim vectors.
     *
     * @param v1 Vector 1.
     * @param v2 Vector 2.
     * @return The scalar product.
     */
    public static float dotVV(float[] v1, float[] v2) {
        return (v1[0] * v2[0])
                + (v1[1] * v2[1])
                + (v1[2] * v2[2]);
    }

    /**
     * Cross product of 2 vectors.
     *
     * @param v1 First vector.
     * @param v2 Second vector.
     * @return The vector product.
     */
    public static float[] crossVV(float[] v1, float[] v2) {
        return new float[] {
                (v1[1] * v2[2]) - (v1[2] * v2[1]),
                (v1[2] * v2[0]) - (v1[0] * v2[2]),
                (v1[0] * v2[1]) - (v1[1] * v2[0])
        };
    }

    /**
     *  Multiply a vector by a 3x3 matrix.
     *
     * @param m The mat3.
     * @param v The vector, treated as a vec3.
     * @return The vec3 product.
     */
    public static float[] multiplyM3V(float[] m, float[]v) {
        return new float[] {
                m[0] * v[0] + m[3] * v[1] + m[6] * v[2],
                m[1] * v[0] + m[4] * v[1] + m[7] * v[2],
                m[2] * v[0] + m[5] * v[1] + m[8] * v[2],
        };
    }

    /**
     *  Return a random vector of fixed magnitude.
     * @param mag The magnitude desired.
     * @return The vector pointing in random direction.
     */
    public static float[] randomV(float mag) {
        float[] vec = {
                (float) Math.random() - 0.5f,
                (float) Math.random() - 0.5f,
                (float) Math.random() - 0.5f
        };
        return scaleSV(mag / length(vec), vec);
    }

    /**
     * Magnitude/length of a vec3.
     * @param v The 3-dim vector.
     * @return Its length.
     */
    public static float length(float[] v) {
        return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    /**
     * Distance between 2 vec3's.
     * @param v1 First 3-dim vector.
     * @param v2 Second 3-dim vector.
     * @return Their distance (i.e. length of their difference).
     */
    public static float distanceVV(float[] v1, float[] v2) {
        return length(diffVV(v2, v1));
    }

    /**
     * Angle in radians between 2 3-dim vectors.
     * @param v1 Vector 1.
     * @param v2 Vector 2.
     * @return Angle in radians between the two. Return zero if either vector is a zero vector.
     */
    public static float angleVV(float[] v1, float[] v2) {
        if (areFloatsEqual( length(v1), 0.0f ) || areFloatsEqual( length(v2), 0.0f ))
            return 0.0f;
        else {
            // Insure argument to Math.acos is never > 1.0f or < -1.0f;
            float cosOfAngle = Math.max(-1.0f,
                    Math.min(1.0f, Vector.dotVV(v1, v2) / (length(v1) * length(v2))));
            return (float) Math.acos(cosOfAngle);
        }
    }

    /**
     * Return vector transformed to, or as seen in, an alternate unit vector system.
     *
     * @param vector The original vector in standard space.
     * @param u The alternate "x" unit vector.
     * @param v The alternate "y" unit vector.
     * @param w The alternate "z" unit vector.
     * @return Vector components for the given alternate unit system. Throws a runtime exception
     * if unit vectors are entered which are not orthonormal.
     */
    public static float[] transformTo(float[] vector, float[] u, float[] v, float[] w) {
        if (areOrthoNormal(u, v, w)) {
            return new float[] {
                    dotVV(vector, u),
                    dotVV(vector, v),
                    dotVV(vector, w)
            };
        } else {
            Log.e(LOG_TAG, "Error: improper unit vectors passed to transformTo method.\n" +
                    "Vectors passed had magnitudes of "
                    + length(u) + ", " + length(v) + ", " + length(w));
            throw new RuntimeException("Error transforming vector.");
        }
    }

    /**
     * Return vector transformed from an alternate unit vector system.
     *
     * @param vector The vector expressed as components of the alternate unit system.
     * @param u The alternate "x" unit vector.
     * @param v The alternate "y" unit vector.
     * @param w The alternate "z" unit vector.
     * @return The vector transformed to, or as seen in, standard space. Throws a runtime exception
     * if unit vectors are entered which are not orthonormal.
     */
    public static float[] transformFrom(float[] vector, float[] u, float[] v, float[] w) {
        if (areOrthoNormal(u, v, w)) {
            float[] uScaled = scaleSV(vector[0], u);
            float[] vScaled = scaleSV(vector[1], v);
            float[] wScaled = scaleSV(vector[2], w);
            return sumVV(sumVV(uScaled, vScaled), wScaled);
        } else {
            Log.e(LOG_TAG, "Error: improper unit vectors passed to transformFrom method.\n" +
                    "Vectors passed had magnitudes of "
                    + length(u) + ", " + length(v) + ", " + length(w));
            throw new RuntimeException("Error transforming vector.");
        }
    }

    /**
     * Build a transformation matrix out of an orthonormal basis for use in OpenGL operations
     *
     * @param u Unit vector 1.
     * @param v Unit vector 2.
     * @param w Unit vector 3.
     * @return The mat4 transform. Throws a runtime exception if unit vectors are entered which
     * are not orthonormal.
     */
    public static float[] convertToMat4(float[] u, float[] v, float[] w) {
        if (areOrthoNormal(u, v, w)) {
            return new float[] {
                    u[0],
                    v[0],
                    w[0],
                    0.0f,

                    u[1],
                    v[1],
                    w[1],
                    0.0f,

                    u[2],
                    v[2],
                    w[2],
                    0.0f,

                    0.0f,
                    0.0f,
                    0.0f,
                    1.0f
            };
        } else {
            Log.e(LOG_TAG, "Error: improper unit vectors passed to convertToMat4 method.\n" +
                    "Vectors passed had magnitudes of "
                    + length(u) + ", " + length(v) + ", " + length(w));
            throw new RuntimeException("Error generating transformation matrix.");
        }
    }

    /**
     * Test whether the given set of vectors are orthonormal.
     *
     * @param u 1st vector.
     * @param v 2nd vector.
     * @param w 3rd vector.
     * @return True if the result is within a small tolerance allowing for rounding errors.
     */
    public static boolean areOrthoNormal(float[] u, float[] v, float[] w) {
        return Math.abs(1 - dotVV(crossVV(v, w), u)) < SIGMA
                && Math.abs(1 - dotVV(crossVV(w, u), v)) < SIGMA
                && Math.abs(1 - dotVV(crossVV(u, v), w)) < SIGMA;
    }

    public static boolean arePerpendicular(float[] u, float[] v) {
        return dotVV(u, v) < SIGMA;
    }

    public static boolean areParallel(float[] u, float[] v) {
        return Math.abs(dotVV(u, v)) > 1.0f - SIGMA;
    }

    /**
     * Normalize a 3-dim vector.
     *
     * @param v The vector to be normalized.
     * @return The normalized vector.
     */
    public static float[] norm(float[] v) {
        float length = length(v);
        if (areFloatsEqual(length, 0.0f)) {
            Log.w(LOG_TAG, "Warning: vector normalizing method passed a zero vector argument.");
            return ZERO.clone();
        } else {
            return scaleSV(1.0f / length, v);
        }
    }

    /**
     * Clamp a vec3 or vec4 so its highest component is "c", with the other components
     * proportionately scaled.
     *
     * @param c The float to make the highest component equal to.
     * @param v The vector to clamp.
     * @return The clamped vector.
     */
    public static float[] clampVec3(float c, float[] v) {
        float maxVal = Math.max(Math.max(v[0], v[1]), v[2]);
        float normVal = c / maxVal;
        return new float[] {
                normVal * v[0],
                normVal * v[1],
                normVal * v[2]
        };
    }

    /**
     * Compute the normal vector for a plane defined by 2 vectors.
     *
     * @param v1 1st vector.
     * @param v2 2nd vector.
     * @return The computed normal vector.
     */
    public static float[] getNormal(float[] v1, float[] v2) {
        return norm(crossVV(v1, v2));
    }

    /**
     * Remove/subtract the positive component of a unit vector from a given vector.
     * @param v The given vector.
     * @param u The unit vector whose positive component is to be removed from the given vector.
     * @return The vector with the positive component removed.
     */
    public static float[] subtractComp(float[] v, float[] u) {
        float dot = dotVV(v, u);
        if (dot > 0.0f) {
            return diffVV(v, scaleSV(dot, u));
        } else  {
            return v;
        }
    }

    /**
     * Reverse the positive component of a unit vector from a given vector. Allows for deflection
     * of motion against a surface using its normal.
     * @param v The given vector.
     * @param u The unit vector whose positive component is to be reversed on the given vector.
     * @return The vector with the positive component reversed.
     */
    public static float[] reverseComp(float[]v, float[]u) {
        float dot = dotVV(v, u);
        if (dot > 0.0f) {
            return diffVV(v, scaleSV(2 * dot, u));
        } else  {
            return v;
        }
    }


    /**
     * Return a vector with a 4th component of 1.0f added for use with OpenGL Matrix operations.
     *
     * @param vec3 The 3-component vector to be converted
     * @return the 4-component version with an added 1.0f value
     */
    public static float[] convertToVec4(float[] vec3) {
        return new float[] {
                vec3[0],
                vec3[1],
                vec3[2],
                1.0f
        };
    }

    public static String toString(float[] v) {
        if (v.length < 4) {
            return ("coords are " + v[0] + ", " + v[1] + ", " + v[2]);
        } else {
            return ("coords are " + v[0] + ", " + v[1] + ", " + v[2] + ", " + v[3]);
        }

    }

    public static boolean areEqual(float[] v1, float[] v2) {
        int len = Math.min(v1.length, v2.length);
        boolean equal = true;
        for (int i = 0; i < len; i++) {
            equal = areFloatsEqual(v1[i], v2[i]) && equal;
        }
        return equal;
    }

    public static boolean areFloatsEqual(float f1, float f2) {
        return Math.abs(f1 - f2) < SIGMA;
    }

    /**
     *  Return boolean for whether 2 vectors are within a distance of each other.
     *
     * @param v1 1st vector.
     * @param v2 2nd vector.
     * @param distance The distance being tested.
     * @return The boolean for whether the vectors are within the distance of each other.
     */
    public static boolean areNear(float[] v1, float[] v2, float distance) {
        return length(diffVV(v2, v1)) <= distance;
    }

    public static String mat3ToString(float[] m) {
        if (m.length == 9) {
            DecimalFormat elForm = new DecimalFormat("000.000000");
            return "elements: \n"
                    + "| " + elForm.format(m[0]) + " | " + elForm.format(m[3]) + " | "
                    + elForm.format(m[6]) + " |\n"
                    + "| " + elForm.format(m[1]) + " | " + elForm.format(m[4]) + " | "
                    + elForm.format(m[7]) + " |\n"
                    + "| " + elForm.format(m[2]) + " | " + elForm.format(m[5]) + " | "
                    + elForm.format(m[8]) + " |";
        } else {
            return "Error: mat3ToString given argument with incorrect array length.";
        }
    }

    public static String mat4ToString(float[] m) {
        if (m.length == 16) {
            DecimalFormat elForm = new DecimalFormat("000.000000");
            return "elements: \n"
                    + "| " + elForm.format(m[0]) + " | " + elForm.format(m[4]) + " | "
                    + elForm.format(m[8]) + " | " + elForm.format(m[12]) + " |\n"
                    + "| " + elForm.format(m[1]) + " | " + elForm.format(m[5]) + " | "
                    + elForm.format(m[9]) + " | " + elForm.format(m[13]) + " |\n"
                    + "| " + elForm.format(m[2]) + " | " + elForm.format(m[6]) + " | "
                    + elForm.format(m[10]) + " | " + elForm.format(m[14]) + " |\n"
                    + "| " + elForm.format(m[3]) + " | " + elForm.format(m[7]) + " | "
                    + elForm.format(m[11]) + " | " + elForm.format(m[15]) + " |";
        } else {
            return "Error: mat4ToString given argument with incorrect array length.";
        }
    }

}
