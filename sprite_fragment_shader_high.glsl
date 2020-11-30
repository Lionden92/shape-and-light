		precision mediump float;
		/* The input texture */
		uniform sampler2D u_Texture;
		/* Sparkle point draw RGB colors */
		uniform vec3 u_SparklePtCol;
		uniform vec4 u_SparkleFlCol;
		/* Varying params passed in from the vertex shader */
		varying vec4 v_Color;
		varying float v_Type;
		
		const float TOL = 0.45;
		const float LIGHT_TX_SIZE = 0.33;
		const float LT_FL_RAD = 0.4;
		const float LT_FL_AlPH = 0.75;
		const float LT_FL_ALPH_BR = 0.92;
		// Parameters for light point sprite mapping of the texture to the pt size.
		const float LT_SPR_MAP_SCALE = 1.5;
		// Double the offset to adjust for scaling the half-vec CTR_VEC.
		const float LT_SPR_MAP_OFFSET = -1.0;
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
		// Vector holding the sprite center coords.
		const vec2 CTR_VEC = vec2(0.5, 0.5);
		
		/* Function for flare fall-off */
		float falloff2(float distRat) {
			return min(1.0, pow(distRat - 1.0, 2.0));
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
			/* For Bubbles Point type. */
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
				vec4 lightColor = BLANKCOL;
				bool isBright = abs(v_Type - 10.0) < TOL;
				bool isOn = v_Color.a > 0.05;
				float distCtr = length(gl_PointCoord - CTR_VEC);
				if (distCtr < 0.48 * LIGHT_TX_SIZE) {
					lightColor = vec4(v_Color.rgb, 1.0);
				}
				vec2 txCoord = LT_SPR_MAP_SCALE * gl_PointCoord 
					+ LT_SPR_MAP_OFFSET * CTR_VEC;
				if (!isOn) {
					// Draw an "off" light point to look more "round".
					float rad = 0.48 * LIGHT_TX_SIZE;
					float k = 0.27; // Minimum factor value.
					float fact = max(
						1.0 + ((k - 1.0) * pow(distCtr / rad, 2.0)), 
						0.0
					);
					lightColor.rgb *= fact;
				}
				vec4 lightPtCol = lightColor * texture2D(u_Texture, txCoord);
				// Small flare calculation.
				vec4 flareCol = BLANKCOL;
				float distRatio =  distCtr / LT_FL_RAD;
				float ltFlareA = isBright ? LT_FL_ALPH_BR : LT_FL_AlPH;
				if (distRatio <= 1.0 && isOn) {
					flareCol = vec4(v_Color.rgb, ltFlareA * falloff2(distRatio));
				}
				gl_FragColor = vec4(
					max(lightPtCol.r, flareCol.r), 
					max(lightPtCol.g, flareCol.g),
					max(lightPtCol.b, flareCol.b), 
					max(lightPtCol.a, flareCol.a)
				);
			}
		}
		