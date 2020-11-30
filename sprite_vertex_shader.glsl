		/* A constant representing the combined model/view/projection matrix. */
		uniform mat4 u_MVPMatrix;
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
		/* Per-vertex position information. */
		attribute vec4 a_Position;
		attribute vec4 a_Color;
		attribute float a_BaseSize;
		attribute float a_Type;
		varying vec4 v_Color;
		varying float v_Type;
		varying float v_ClipW;

		/* Constants */
		const float SCALE_FCT = 440.0;
		float BASE_MAG = 1.66;
		float CIRC_MAG = 0.66;
		float TOL = 0.45;

		/* Function returning color vec contribution for a given light source */
		vec3 lightColor(vec3 vecToLight, vec4 lightCol, float mag) {
				float dist = length(vecToLight);
				float diffuse = mag / (1.0 + (dist * dist / SCALE_FCT));
				return lightCol.a * vec3(diffuse * lightCol);
		}

		/* The entry point for our vertex shader. */
		void main() {
			v_Color = a_Color;
			v_Type = a_Type;
			/* Compute illuminated color for the bubble and smoke sprites */
			if (abs(a_Type - 3.0) < TOL 
				|| abs(a_Type - 5.0) < TOL 
				|| abs(a_Type - 6.0) < TOL 
				|| abs(a_Type - 7.0) < TOL 
				|| abs(a_Type - 8.0) < TOL) {
				vec3 light0Col = lightColor(u_Light0Pos - a_Position.xyz, u_Light0Col, BASE_MAG);
				vec3 light1Col = lightColor(u_Light1Pos - a_Position.xyz, u_Light1Col, BASE_MAG);
				vec3 light2Col = lightColor(u_Light2Pos - a_Position.xyz, u_Light2Col, BASE_MAG);
				vec3 circ0Col = lightColor(u_Circ0Pos - a_Position.xyz, u_Circ0Col, CIRC_MAG);
				vec3 circ1Col = vec3(0.0, 0.0, 0.0);
				if (u_Circ1Col.a > 0.05) {
					circ1Col = lightColor(u_Circ1Pos - a_Position.xyz, u_Circ1Col, CIRC_MAG);
				}
				vec3 sparkCol = vec3(0.0, 0.0, 0.0);
				if (u_SparkleCol.a > 0.05) {
					sparkCol = lightColor(u_SparklePos - a_Position.xyz, u_SparkleCol, BASE_MAG);
				}
				float redComponent = light0Col.r + light1Col.r + light2Col.r 
					+ circ0Col.r + circ1Col.r + sparkCol.r;
				float grnComponent = light0Col.g + light1Col.g + light2Col.g 
					+ circ0Col.g + circ1Col.g + sparkCol.g;
				float bluComponent = light0Col.b + light1Col.b + light2Col.b 
					+ circ0Col.b + circ1Col.b + sparkCol.b;
				float maxVal = max(redComponent, max(grnComponent, bluComponent));
				float normVal = maxVal > 1.0 ? 1.0 / maxVal : 1.0;
				v_Color = vec4(
					redComponent * normVal * (1.0 - a_Color.r) + a_Color.r,
					grnComponent * normVal * (1.0 - a_Color.g) + a_Color.g,
					bluComponent * normVal * (1.0 - a_Color.b) + a_Color.b,
					a_Color.a
				);
			}
			gl_Position = u_MVPMatrix * a_Position;
			v_ClipW = gl_Position.w;
			gl_PointSize = a_BaseSize / gl_Position.w;
		}
