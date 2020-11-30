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
		const float FLARE_BASE_ALPHA = 0.66;
		const float LIGHT_PT_RAD = 0.0124;
		const float LIGHT_PT_RAD_B = 0.0093;
		const float LT_RAY_RAD = 0.5;
		const float LT_FL_RAD = 0.37;
		const float LT_RAY_WIDTH = 1.25;
		const float LT_RAY_SLOPE = 0.14;
		const float LT_RAY_SLP_B = 0.11;
		const float LT_FL_FCTR = 0.75;
		const float LT_SM_FL_FCTR = 0.25;
		const float LT_RAY_FCTR = 0.6;
		const float RAY_DIAG_SLOPE = 0.33;
		const float LT_ALPH_FALLOFF = 400.0;
		const float NEAR_CLIP = 3.25;
		// Parameters for light point sprite mapping of the texture to the pt size.
		// Double the offset to adjust for scaling the half-vec CTR_VEC.
		// LIGHT_TX_SIZE = 0.0285;
		const float LT_SPR_MAP_SCALE = 17.5439;
		const float LT_SPR_MAP_OFFSET = -17.0439;
		// LIGHT_TX_SIZE_B = 0.0214;
		const float LT_SPR_MP_SCL_B = 23.3645;
		const float LT_SPR_MP_OFFST_B = -22.8645;
		// Parameters for sparkle point and flare sprite texture mappings.
		const float SP_PT_RAD = 0.05;
		// SP_PT_TX_SIZE = 0.10;
		const float SP_PT_SPR_MAP_SCALE = 5.0;
		const float SP_PT_SPR_MAP_OFFSET = -4.5;
		// SP_FL_TX_SIZE = 1.6;
		const float SP_FL_SPR_MAP_SCALE = 0.3125;
		const float SP_FL_SPR_MAP_OFFSET = 1.1875;
		// Blank color vector.
		const vec4 BLANKCOL = vec4(0.0, 0.0, 0.0, 0.0);
		const vec4 LIGHT_OFF_COL = vec4(0.08, 0.08, 0.08, 1.0);

		// Vector holding the sprite center coords.
		const vec2 CTR_VEC = vec2(0.5, 0.5);
		
		/* Function for flare fall-off */
		float falloff(float distRat, float k) {
			return min(1.0, k * ((1.0 + k) / (distRat * distRat + k) - 1.0));
		}
		
		/* Alternate linear fall-off function for gentler fall-off. */
		float falloff3(float distRat) {
			return min(1.0, 1.0 - distRat);
		}
		
		/* Function returning 'angle ratio' for given ray width and delta x, y */
		float angleRatio(float deltaX, float deltaY, float width, float slope) {
			return abs(deltaY) / (width + (abs(deltaX) * slope));
		}
	
		/* Function returning colorvec for flare ray fragments */
		vec4 rayColor(float distRat, float angleRat, vec4 baseColor) {
			vec4 returnRayCol = BLANKCOL;
			if (distRat <= 1.0 && angleRat <= 1.0) {
				returnRayCol = falloff3(distRat) * falloff(angleRat, 0.3) * baseColor;
			}
			return returnRayCol;
		}

		/* Function to clamp the rgb values of a color vec to 1.0, preserving the color. */
		vec4 clampToOne(vec4 lightColor) {
			float maxVal = max(lightColor.r, max(lightColor.g, lightColor.b));
			float normVal = maxVal > 1.0 ? 1.0 / maxVal : 1.0;
			return normVal * lightColor;
		}
		
		vec4 blendCol(vec4 firstCol, vec4 secondCol) {
			return firstCol + max(0.0, (0.9 - firstCol.a)) * secondCol;
		}

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
				float thickness = 0.00043;
				float rim = 0.278;
				float deltaRadSquared = pow(rim - distCtr, 2.0);
				float ringAlpha = 1.0 - (1.0 / thickness) * deltaRadSquared;
				float oscAlpha = v_Color.a;
				vec4 ringCol = vec4(
					max(v_Color.r * oscAlpha, 0.08),
					max(v_Color.g * oscAlpha, 0.08),
					max(v_Color.b * oscAlpha, 0.08),
					max(ringAlpha, 0.0)
				);
				// Color for flare effect.
				float fact1 = 0.65;
				float fact2 = 500.0;
				float offset = 0.025;
				float flareAlpha = (fact1 / (1.0 + fact2 * deltaRadSquared)) - offset;
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
				float rad = 0.5;
				float alpha = max(1.0 - pow(distCtr / rad, 2.0), 0.0);
				gl_FragColor = vec4(v_Color.rgb, v_Color.a * alpha);
			/* For Bubbles Point type.	*/
			} else if (abs(v_Type - 3.0) < TOL) {
				vec2 txCoord = vec2(0.0, 0.5) + (gl_PointCoord * 0.5);
				gl_FragColor = v_Color * texture2D(u_Texture, txCoord);
			/* For Smoke Point type 1. */
			} else if (abs(v_Type - 5.0) < TOL) {
				vec2 txCoord = vec2(0.5, 0.0) + (gl_PointCoord * 0.5);
				gl_FragColor = v_Color * texture2D(u_Texture, txCoord);
			/* For Smoke Point type 2 (with 90-deg transform).	*/
			} else if (abs(v_Type - 6.0) < TOL) {
				vec2 pointCoord = vec2(gl_PointCoord.y, -gl_PointCoord.x);
				vec2 txCoord = CTR_VEC + (pointCoord * 0.5);
				gl_FragColor = v_Color * texture2D(u_Texture, txCoord);
			/* For Smoke Point type 3 (with 180-deg transform).	*/
			} else if (abs(v_Type - 7.0) < TOL) {
				vec2 txCoord = vec2(1.0, 0.5) - (gl_PointCoord * 0.5);
				gl_FragColor = v_Color * texture2D(u_Texture, txCoord);
			/* For Smoke Point type 4 (with the other 90-deg transform).	*/
			} else if (abs(v_Type - 8.0) < TOL) {
				vec2 pointCoord = vec2(-gl_PointCoord.y, gl_PointCoord.x);
				vec2 txCoord = vec2(1.0, 0.0) + (pointCoord * 0.5);
				gl_FragColor = v_Color * texture2D(u_Texture, txCoord);
			/* For Sparkle Point type. */
			} else if (abs(v_Type - 4.0) < TOL) {
				vec4 spColor = BLANKCOL;
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
				float distCtr = length(gl_PointCoord - CTR_VEC);
				// Set params dependent on 'bright' setting.
				bool isBright = abs(v_Type - 10.0) < TOL;
				bool isOn = v_Color.a > 0.05;
				float lt_pt_rad = isBright ? LIGHT_PT_RAD_B : LIGHT_PT_RAD;
				float lt_spr_mp_scl = isBright ? LT_SPR_MP_SCL_B : LT_SPR_MAP_SCALE;
				float lt_spr_mp_off = isBright ? LT_SPR_MP_OFFST_B : LT_SPR_MAP_OFFSET;
				float lt_ray_slp = isBright ? LT_RAY_SLP_B : LT_RAY_SLOPE;
				bool inLightPt = distCtr < 1.05 * lt_pt_rad;
				// Light point texture, mapped to light point size.
				vec4 lightColor = inLightPt ? lightPointColor() : BLANKCOL;
				if (!isOn) {
					// Draw an "off" light point to look more "round".
					float compFact = 20.0; // Compensation for possibly too-small scale.
					float distCtrComp = length((compFact * gl_PointCoord) 
						- (compFact * CTR_VEC));
					float rad = 1.1 * compFact * lt_pt_rad;
					float k = 0.27; // Minimum factor value.
					float fact = max(
						1.0 + ((k - 1.0) * pow(distCtrComp / rad, 2.0)), 
						0.0
					);
					lightColor.rgb *= fact;
				}
				vec2 txCoord = lt_spr_mp_scl * gl_PointCoord 
					+ lt_spr_mp_off * CTR_VEC;
				vec4 lightPtCol = lightColor * texture2D(u_Texture, txCoord);
				// Distance effect on base alpha for lens flare.
				float distFct = isOn ? FLARE_BASE_ALPHA * pow(LT_ALPH_FALLOFF / 
					(v_ClipW + LT_ALPH_FALLOFF - NEAR_CLIP), 2.0) : 0.0;
				// Factor in RGB attenuation/boost passed in via draw color alpha.
				float rgbAtten = isBright ? 0.25 * v_Color.a : v_Color.a;
				distFct *= rgbAtten;
				vec4 flBaseCol = inLightPt && !isOn ? BLANKCOL : v_Color;
				if (isOn) flBaseCol.a = 1.0;
				// Light rays calculation.
				float delX = gl_PointCoord.x - CTR_VEC.x;
				float delY = gl_PointCoord.y - CTR_VEC.y;
				float rayWidth = LT_RAY_WIDTH * lt_pt_rad;
				bool flareComp = false;
				float distRatio = distCtr / (LT_RAY_RAD * 1.25);
				float delYR = delY - RAY_DIAG_SLOPE * delX;
				float angleRat = angleRatio(delX, delYR, rayWidth, lt_ray_slp);
				vec4 ray1Col = rayColor(distRatio, angleRat, flBaseCol * LT_RAY_FCTR);
				if (ray1Col.a > 0.0) flareComp = true;
				distRatio = distCtr / LT_RAY_RAD;
				float delXR = delX + RAY_DIAG_SLOPE * delY;
				angleRat = angleRatio(delY, delXR, rayWidth, lt_ray_slp);
				vec4 ray2Col = rayColor(distRatio, angleRat, flBaseCol * LT_RAY_FCTR);
				if (ray2Col.a > 0.0) flareComp = true;
				// Center flare calculation.
				vec4 flareCol = BLANKCOL;
				distRatio = distCtr / LT_FL_RAD;
				if (distRatio <= 1.0) {
					flareCol = falloff(distRatio, 0.35) * flBaseCol * LT_FL_FCTR;
					flareComp = true;
				}
				// Small center flare.
				vec4 smFlCol = BLANKCOL;
				distRatio = distCtr / (LT_FL_RAD * 0.2);
				if (distRatio <= 1.0) {
					smFlCol = falloff(distRatio, 0.3) * flBaseCol * LT_SM_FL_FCTR;
					flareComp = true;
				}
				vec4 blendedFlCol = BLANKCOL;
				if (flareComp) {
					blendedFlCol = blendCol(
						clampToOne(flareCol + smFlCol), clampToOne(ray1Col + ray2Col)
					);
				}
				blendedFlCol.a *= distFct;
				gl_FragColor = vec4(
					max(lightPtCol.r, blendedFlCol.r), 
					max(lightPtCol.g, blendedFlCol.g),
					max(lightPtCol.b, blendedFlCol.b), 
					max(lightPtCol.a, blendedFlCol.a)
				);
			}
		}
		