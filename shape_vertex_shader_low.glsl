		/* A constant representing the combined model/view/projection matrix. */
		uniform mat4 u_MVPMatrix;
		/* The model matrix to transform position to world space. */
		uniform mat4 u_ModelMatrix;
		/* The orientation/rotation matrix used to transform the normals. */
		uniform mat4 u_RotMatrix;
		/* Position of the light points */
		uniform vec3 u_Light0Pos;
		uniform vec3 u_Light1Pos;
		uniform vec3 u_Light2Pos;
		uniform vec3 u_Circ0Pos;
		uniform vec3 u_Circ1Pos;
		uniform vec3 u_SparklePos;
		/* Color vectors for the light points */
		uniform vec4 u_Light0Col;
		uniform vec4 u_Light1Col;
		uniform vec4 u_Light2Col;
		uniform vec4 u_Circ0Col;
		uniform vec4 u_Circ1Col;
		uniform vec4 u_SparkleCol;
		/* Minimum background lighting factor */
		uniform float u_MinLightFactor;
		/* Per-vertex position information. */
		attribute vec4 a_Position;
		/* Per-vertex normal information */
		attribute vec3 a_Normal;
		/* Per-vertex centers information for lighting position calcs. */
		attribute vec3 a_Center;
		/* Per-vertex texture coordinate information we will pass in. */
		attribute vec2 a_TexCoordinate0;
		attribute vec2 a_TexCoordinate1;
		attribute vec2 a_TexCoordinate2;
		/* These will be passed into the fragment shader. */
		varying vec2 v_TexCoordinate0;
		varying vec2 v_TexCoordinate1;
		varying vec2 v_TexCoordinate2;
		varying vec4 v_Color;
		/* Constants */
		// const float SCALE_FCT = 40.0;
		const float SCALE_FCT = 48.0;
		const float MIN_RED = 0.066;
		const float MIN_GRN = 0.046;
		const float MIN_BLU = 0.046;
		const float BASE_MAG = 1.5;
		const float CIRC_MAG = 0.45;
		const float SPARK_RADIUS = 14.0;
		const float PI = 3.1416;
		
		/* Function returning color vec contribution for a given light source */
		vec3 lightColor(vec3 vecToLight, vec3 normal, vec4 lightCol, float mag) {
			float dist = length(vecToLight);
			float diffuse = max(dot(normalize(vecToLight), normal), 0.0);
			diffuse = diffuse * mag * pow((SCALE_FCT / (SCALE_FCT + dist)), 2.0);
			return lightCol.a * vec3(diffuse * lightCol);
		}

		/* The entry point for our vertex shader. In low settings lighting will be computed here instead
		of in the fragment shader. */
		void main() {
			/* Pass through the texture coordinate */
			v_TexCoordinate0 = a_TexCoordinate0;
			v_TexCoordinate1 = a_TexCoordinate1;
			v_TexCoordinate2 = a_TexCoordinate2;
			/* Compute normal orientation in world space */
			vec3 normalVec = vec3(u_RotMatrix * vec4(a_Normal, 0.0));
			/* Compute triangle center position vec in world space, which will be used for lighting pos. */
			vec3 centerVec = vec3(u_ModelMatrix * vec4(a_Center, 1.0));
			/* Lighting computations */
			vec3 light0Col = lightColor(u_Light0Pos - centerVec, normalVec, u_Light0Col, BASE_MAG);
			vec3 light1Col = lightColor(u_Light1Pos - centerVec, normalVec, u_Light1Col, BASE_MAG);
			vec3 light2Col = lightColor(u_Light2Pos - centerVec, normalVec, u_Light2Col, BASE_MAG);
			vec3 circ0Col = lightColor(u_Circ0Pos - centerVec, normalVec, u_Circ0Col, CIRC_MAG);
			vec3 circ1Col = lightColor(u_Circ1Pos - centerVec, normalVec, u_Circ1Col, CIRC_MAG);
			vec3 sparkCol = vec3(0.0, 0.0, 0.0);
			if (u_SparkleCol.a != 0.0) {
				vec3 vecToSparkle = u_SparklePos - centerVec;
				float distToSpark = length(vecToSparkle);
				float diffuseSp = max(dot(normalize(vecToSparkle), normalVec), 0.0);
				/* Additional computation if the shape is "within" the sparkles */
				if (distToSpark < SPARK_RADIUS) {
					diffuseSp = diffuseSp + 0.5 * (1.0 + cos((PI / SPARK_RADIUS) * distToSpark));
				}
				diffuseSp = diffuseSp * BASE_MAG * pow((SCALE_FCT / (SCALE_FCT + distToSpark)), 2.0);
				sparkCol = u_SparkleCol.a * vec3(diffuseSp * u_SparkleCol);
			}
			float redComponent = light0Col.r + light1Col.r + light2Col.r 
				+ circ0Col.r + circ1Col.r + sparkCol.r;
			float grnComponent = light0Col.g + light1Col.g + light2Col.g 
				+ circ0Col.g + circ1Col.g + sparkCol.g;
			float bluComponent = light0Col.b + light1Col.b + light2Col.b 
				+ circ0Col.b + circ1Col.b + sparkCol.b;
			// Code to prevent lighting oversaturation.
			float maxVal = max(redComponent, max(grnComponent, bluComponent));
			float normVal = maxVal > 1.0 ? 1.0 / maxVal : 1.0;
			// Determine minimum component values.
			float minRed = MIN_RED * u_MinLightFactor;
			float minGrn = MIN_GRN * u_MinLightFactor;
			float minBlu = MIN_BLU * u_MinLightFactor;
			/* Pass in the computed lit color value to the fragment shader now */
			v_Color = vec4(
				redComponent * normVal * (1.0 - minRed) + minRed,
				grnComponent * normVal * (1.0 - minGrn) + minGrn,
				bluComponent * normVal * (1.0 - minBlu) + minBlu,
				1.0
			);
			/* Multiply the vertex by the matrix to get the final point in normalized screen coordinates. */
			gl_Position = u_MVPMatrix * a_Position;
		}
		