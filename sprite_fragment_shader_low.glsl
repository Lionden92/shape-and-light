		precision mediump float;
		/* The input texture */
		uniform sampler2D u_Texture;
		/* Sparkle point draw RGB colors */
		uniform vec3 u_SparklePtCol;
		uniform vec4 u_SparkleFlCol;
		/* Varying params passed in from the vertex shader */
		varying vec4 v_Color;
		varying float v_Type;
		varying float v_ClipW;
		
		const float TOL = 0.45;
		const float LIGHT_PT_RAD = 0.019;
		const float LIGHT_PT_RAD_B = 0.0165;
		const float LT_ALPH_FALLOFF = 400.0;
		const float FLARE_BASE_ALPHA = 0.66;
		const float NEAR_CLIP = 3.25;
		// Parameters for sparkle point and flare sprite texture mappings.
		const float SP_PT_RAD = 0.0925;
		// SP_PT_TX_SIZE = 0.185;
		const float SP_PT_SPR_MAP_SCALE = 2.7027;
		const float SP_PT_SPR_MAP_OFFSET = -2.2027;
		// SP_FL_TX_SIZE = 1.7;
		const float SP_FL_SPR_MAP_SCALE = 0.2941;
		const float SP_FL_SPR_MAP_OFFSET = 1.2059;
		// Vector holding the sprite center coords.
		const vec2 CTR_VEC = vec2(0.5, 0.5);
		const vec4 LIGHT_OFF_COL = vec4(0.08, 0.08, 0.08, 1.0);
		
		/* Function duplicating app code for mapping light color to 'brighter'
		light point color */
		vec4 lightPointColor() {
			if (v_Color.a < 0.2) return LIGHT_OFF_COL;
			bool isBright = v_Color.a > 2.0;
			float fLow = isBright ? 0.66 : 0.4;
			float fHi = isBright ? 1.0 : 0.9;
			if (v_Color.r + v_Color.b > 1.0) {
				fHi = 1.25 * fHi / (v_Color.r + v_Color.b);
			}
			float fHiNoGr = fHi - ((fHi - fLow) * v_Color.g);
			mat3 ltPtColTransform = mat3(
				1.0, fHiNoGr, fLow, fLow, 1.0, fLow, fLow, fHiNoGr, 1.0);
			vec3 ltPtCol = ltPtColTransform * v_Color.rgb;
			float maxVal = max(ltPtCol.r, max(ltPtCol.g, ltPtCol.b));
			float nrmVal = maxVal > 1.0 ? 1.0 / maxVal : 1.0;
			return vec4(nrmVal * ltPtCol.rgb, 1.0);
		}

		
		void main() {
			/* For Grav Circle type. */
			if (abs(v_Type - 1.0) < TOL) {
				// Color for solid ring.
				float distCtr = length(gl_PointCoord - CTR_VEC);
				float rim = 0.278;
				float ringAlpha = 1.0 - 30.0 * abs(distCtr - rim);
				float oscAlpha = v_Color.a;
				vec4 ringCol = vec4(
					max(v_Color.r * oscAlpha, 0.08),
					max(v_Color.g * oscAlpha, 0.08),
					max(v_Color.b * oscAlpha, 0.08),
					max(ringAlpha, 0.0)
				);
				// Color for flare effect.
				float flareAlpha = 0.45 * (1.0 - 12.0 * abs(distCtr - rim));
				vec4 flareCol = oscAlpha * vec4(v_Color.rgb, max(flareAlpha, 0.0));
				gl_FragColor = vec4(
					max(ringCol.r, flareCol.r), 
					max(ringCol.g, flareCol.g),
					max(ringCol.b, flareCol.b), 
					max(ringCol.a, flareCol.a)
				);
			/* For Dust Point type.	*/
			} else if (abs(v_Type - 2.0) < TOL) {
				float distCtr = length(gl_PointCoord - CTR_VEC);
				float alpha = max(1.0 - (2.0 * distCtr), 0.0);
				gl_FragColor = vec4(v_Color.rgb, v_Color.a * alpha);
			/* For Bubble type.	*/
			} else if (abs(v_Type - 3.0) < TOL) {
				vec2 txCoord = vec2(0.0, 0.5) + (gl_PointCoord * 0.5);
				gl_FragColor = v_Color * texture2D(u_Texture, txCoord);
			/* For Sparkle Point type.	*/
			} else if (abs(v_Type - 4.0) < TOL) {
				vec4 spColor = vec4(0.0, 0.0, 0.0, 0.0);
				if (length(gl_PointCoord - CTR_VEC) < SP_PT_RAD) 
					spColor = vec4(u_SparklePtCol, v_Color.a);
				vec2 txCoord = SP_PT_SPR_MAP_SCALE * gl_PointCoord
					+ SP_PT_SPR_MAP_OFFSET * CTR_VEC;
				vec4 sparklePtCol = spColor * texture2D(u_Texture, txCoord);
				txCoord = SP_FL_SPR_MAP_SCALE * gl_PointCoord
					+ SP_FL_SPR_MAP_OFFSET * CTR_VEC;
				vec4 sparkleFlCol = vec4(u_SparkleFlCol.rgb, v_Color.a)
					* texture2D(u_Texture, txCoord);
				sparkleFlCol.a *= u_SparkleFlCol.a;
				gl_FragColor = vec4(
					max(sparklePtCol.r, sparkleFlCol.r), 
					max(sparklePtCol.g, sparkleFlCol.g),
					max(sparklePtCol.b, sparkleFlCol.b), 
					max(sparklePtCol.a, sparkleFlCol.a)
				);
			/* For Light Point type. */
			} else {
				bool isBright = abs(v_Type - 10.0) < TOL;
				float alpha = isBright ? 0.25 * v_Color.a : v_Color.a;
				// Distance effect on base alpha for lens flare.
				float distFct = FLARE_BASE_ALPHA * pow(LT_ALPH_FALLOFF 
					/ (v_ClipW + LT_ALPH_FALLOFF - NEAR_CLIP), 2.0);
				alpha *= distFct;
				vec4 lightColor = vec4(v_Color.rgb, alpha);
				float lt_pt_rad = isBright ? LIGHT_PT_RAD_B : LIGHT_PT_RAD;
				if (length(gl_PointCoord - CTR_VEC) <= lt_pt_rad) 
					lightColor = lightPointColor();
				vec2 txCoord = vec2(0.5, 0.0) + (gl_PointCoord * 0.5);
				gl_FragColor = lightColor * texture2D(u_Texture, txCoord);
			}
		}
		