	precision mediump float;
	
	/* Position of the lights */
	uniform vec2 u_Light0Pos;
	uniform vec2 u_Light1Pos;
	uniform vec2 u_Light2Pos;
	uniform vec2 u_SparksPos;
	/* Color vectors for the lights */
	uniform vec4 u_Light0Col;
	uniform vec4 u_Light1Col;
	uniform vec4 u_Light2Col;
	uniform vec4 u_SparksCol;
	/* Flare sizes that fall off with distance */
	uniform float u_Light0FlSize;
	uniform float u_Light1FlSize;
	uniform float u_Light2FlSize;
	uniform float u_SparksFlSize;
	/* Ray lengths that fall off with distance */
	uniform float u_Light0Ray1Len;
	uniform float u_Light0Ray2Len;
	uniform float u_Light0Ray3Len;
	uniform float u_Light0Ray4Len;
	uniform float u_Light1Ray1Len;
	uniform float u_Light1Ray2Len;
	uniform float u_Light1Ray3Len;
	uniform float u_Light1Ray4Len;
	uniform float u_Light2Ray1Len;
	uniform float u_Light2Ray2Len;
	uniform float u_Light2Ray3Len;
	uniform float u_Light2Ray4Len;
	/* Light point sizes to use for ray calcs */
	uniform float u_Light0PtSize;
	uniform float u_Light1PtSize;
	uniform float u_Light2PtSize;
	/* Flare alpha factors that fall off with distance */
	uniform float u_Light0FlAlpha;
	uniform float u_Light1FlAlpha;
	uniform float u_Light2FlAlpha;
	uniform float u_SparksFlAlpha;
	/* Virtual booleans for whether they're visible, 1 for true, 0 for false */
	uniform int u_Light0Vis;
	uniform int u_Light1Vis;
	uniform int u_Light2Vis;
	uniform int u_SparksVis;
	/* Light visibility factors */
	uniform int u_Light0FlBlock;
	uniform int u_Light1FlBlock;
	uniform int u_Light2FlBlock;
/* Passed in data for fragment position */
	varying vec2 v_Position;
	
	/* Constants */
	// Compensation factor to keep float calcs from exceeding a value of 64K, 
	// which causes issues in certain devices.	
	const float COMP_FACT = 0.05;
	const float BRIGHT_DIV = 0.25;
	
	// Base 'slope' factor for ray width.
	const float RAY_SLOPE_1 = 0.14;
	const float RAY_SLOPE_2 = 0.09;
	const float RAY_SLOPE_1B = 0.12;
	const float RAY_SLOPE_2B = 0.08;
	// Factors for the ray 'angles'.
	const float RAY_DIAG_SLOPE = 0.33;
	// Base ray width factor.
	const float RAY_WIDTH = 1.6;
	// Ray alpha factor.
	const float RAY_ALPHA = 0.31;
	// Blank color vector.
	const vec4 BLANKCOL = vec4(0.0, 0.0, 0.0, 0.0);
	
	/* Function for flare fall-off */
	float falloff(float distRat, float k) {
		return min(1.0, k * ((1.0 + k) / (distRat * distRat + k) - 1.0));
	}
	
	/* Flare fall-off which creates initially-steeper fall-off for "brighter" look. */
	float falloff2(float distRat) {
		return min(1.0, pow(distRat - 1.0, 2.0));
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
	vec4 rayColor(float distRat, float angleRatio, vec4 baseColor) {
		vec4 returnRayCol = BLANKCOL;
		if (distRat <= 1.0 && angleRatio <= 1.0) {
			returnRayCol = falloff3(distRat) * falloff(angleRatio, 0.3) * baseColor;
		}
		return returnRayCol;
	}

	/* Function to attenuate rgb components when a higher alpha value is present to avoid
	over-bright or saturated areas */
	vec3 attenToAlpha(vec4 colVec, float alpha) {
		return alpha > colVec.a ? colVec.rgb * (colVec.a / alpha) : colVec.rgb;
	}
	
	/* Function to clamp the rgb values of a color vec to 1.0, preserving the color. */
	vec4 clampToOne(vec4 lightColor) {
		float maxVal = max(lightColor.r, max(lightColor.g, lightColor.b));
		float normVal = maxVal > 1.0 ? 1.0 / maxVal : 1.0;
		return normVal * lightColor;
	}
	
	/* Function for light flare fragment color generation */
	vec4 flareColor(int ltNum) {
		// assign params per light ID passed in.
		vec2 lightPosition; vec2 lightPos2; vec2 lightPos3; vec4 lightColor;
		float rayLength1; float rayLength2; float rayLength3; float rayLength4;
		float pointSize; float ptSz2; float ptSz3; float flareSize; float flareAlpha;
		int ltVis; int ltVis2; int ltVis3; int ltFlBlock;
		if (ltNum == 0) {
			lightPosition = u_Light0Pos; 
			lightPos2 = u_Light1Pos; lightPos3 = u_Light2Pos;
			lightColor = u_Light0Vis == 1 ? u_Light0Col : BLANKCOL;
			rayLength1 = u_Light0Ray1Len; rayLength2 = u_Light0Ray2Len;
			rayLength3 = u_Light0Ray3Len; rayLength4 = u_Light0Ray4Len;
			pointSize = u_Light0PtSize; 
			ptSz2 = u_Light1PtSize; ptSz3 = u_Light2PtSize;
			flareSize = u_Light0FlSize; flareAlpha = u_Light0FlAlpha;
			ltVis = u_Light0Vis; ltVis2 = u_Light1Vis; ltVis3 = u_Light2Vis;
			ltFlBlock = u_Light0FlBlock;
		} else if (ltNum == 1) {
			lightPosition = u_Light1Pos; 
			lightPos2 = u_Light0Pos; lightPos3 = u_Light2Pos;
			lightColor = u_Light1Vis == 1 ? u_Light1Col : BLANKCOL;
			rayLength1 = u_Light1Ray1Len; rayLength2 = u_Light1Ray2Len;
			rayLength3 = u_Light1Ray3Len; rayLength4 = u_Light1Ray4Len;
			pointSize = u_Light1PtSize; 
			ptSz2 = u_Light0PtSize; ptSz3 = u_Light2PtSize;
			flareSize = u_Light1FlSize; flareAlpha = u_Light1FlAlpha;
			ltVis = u_Light1Vis; ltVis2 = u_Light0Vis; ltVis3 = u_Light2Vis;	
			ltFlBlock = u_Light1FlBlock;
		} else {
			lightPosition = u_Light2Pos; 
			lightPos2 = u_Light0Pos; lightPos3 = u_Light1Pos;
			lightColor = u_Light2Vis == 1 ? u_Light2Col : BLANKCOL;
			rayLength1 = u_Light2Ray1Len; rayLength2 = u_Light2Ray2Len;
			rayLength3 = u_Light2Ray3Len; rayLength4 = u_Light2Ray4Len;
			pointSize = u_Light2PtSize; 
			ptSz2 = u_Light0PtSize; ptSz3 = u_Light1PtSize;
			flareSize = u_Light2FlSize; flareAlpha = u_Light2FlAlpha;
			ltVis = u_Light2Vis; ltVis2 = u_Light0Vis; ltVis3 = u_Light1Vis;
			ltFlBlock = u_Light2FlBlock;
		}
		float delX = v_Position.x - lightPosition.x;
		float delY = v_Position.y - lightPosition.y;
		float dist = length((COMP_FACT * lightPosition) - (COMP_FACT * v_Position));
		float rayWidth = RAY_WIDTH * pointSize;
		bool flareComp = false;
		bool brightCol = lightColor.a > 1.5;
		float raySlope1 = brightCol ? RAY_SLOPE_1B : RAY_SLOPE_1;
		float raySlope2 = brightCol ? RAY_SLOPE_2B : RAY_SLOPE_2;
		if (brightCol) lightColor.a *= BRIGHT_DIV;
		// Flare rays.
		float distRatio = dist / (COMP_FACT * rayLength2);
		float delXR = delX - RAY_DIAG_SLOPE * delY;
		float angleRat = angleRatio(delY, delXR, rayWidth, raySlope1);
		vec4 rayColD1 = rayColor(distRatio, angleRat, lightColor);
		if (rayColD1.a > 0.0) flareComp = true;
		distRatio = dist / (COMP_FACT * rayLength4);
		delXR = delX + RAY_DIAG_SLOPE * delY;
		angleRat = angleRatio(delY, delXR, rayWidth, raySlope2);
		vec4 rayColD2 = rayColor(distRatio, angleRat, lightColor);
		if (rayColD2.a > 0.0) flareComp = true;
		distRatio = dist / (COMP_FACT * rayLength3);
		float delYR = delY - RAY_DIAG_SLOPE * delX;
		angleRat = angleRatio(delX, delYR, rayWidth, raySlope2);
		vec4 rayColD3 = rayColor(distRatio, angleRat, lightColor);
		if (rayColD3.a > 0.0) flareComp = true;
		distRatio = dist / (COMP_FACT * rayLength1);
		delYR = delY + RAY_DIAG_SLOPE * delX;
		angleRat = angleRatio(delX, delYR, rayWidth, raySlope1);
		vec4 rayColD4 = rayColor(distRatio, angleRat, lightColor);
		if (rayColD4.a > 0.0) flareComp = true;
		// Center flare.
		distRatio = dist / (COMP_FACT * flareSize);
		vec4 flareCol = BLANKCOL;
		if (distRatio <= 1.0) {
			flareCol = falloff2(distRatio) * lightColor;
			flareComp = true;
		}
		vec4 returnCol = BLANKCOL;
		if (flareComp) {
			vec4 rayCol = clampToOne(rayColD1 + rayColD2 + rayColD3 + rayColD4);
			rayCol.a *= RAY_ALPHA;
			// Radial factor which attenuates ray rgb near center to tone it down.
			float rayRadialFct = distRatio <= 1.0 ? 1.0 - pow(1.0 - distRatio, 2.0) : 1.0;
			returnCol = flareCol + (1.0 - flareCol.a) * rayRadialFct * rayCol;
			// Keep flare color from coloring the light point itself.
			bool blockFlare = dist < COMP_FACT * pointSize && ltFlBlock == 1;
			returnCol.a = blockFlare ? 0.0 : flareAlpha * returnCol.a;
			// Keep flare color from over-drawing the other light points.
			if (ltVis2 == 1) {
				dist = length((COMP_FACT * lightPos2) - (COMP_FACT * v_Position));
				if (dist < COMP_FACT * ptSz2) returnCol.a *= 0.33;
			}
			if (ltVis3 == 1) {
				dist = length((COMP_FACT * lightPos3) - (COMP_FACT * v_Position));
				if (dist < COMP_FACT * ptSz3) returnCol.a *= 0.33;
			}
		}
		return returnCol;
	}
	
	
	/* Main shader execution function */
	void main() {
		/* Compute flare for Light0 */
		vec4 fragCol0 = flareColor(0);
		/* Compute flare for Light1 */
		vec4 fragCol1 = flareColor(1);
		/* Compute flare for Light2 */
		vec4 fragCol2 = flareColor(2);
		/* Compute flare for fireworks if in effect */
		vec4 fragCol3 = BLANKCOL;
		if (u_SparksVis == 1 && u_SparksCol.a > .05) {
			float distRatio = length((COMP_FACT * u_SparksPos) - (COMP_FACT * v_Position))
				/ (COMP_FACT * u_SparksFlSize);
			if (distRatio <= 1.0) {
				fragCol3 = falloff(distRatio, 0.5) * u_SparksCol;
				fragCol3.a = u_SparksFlAlpha * fragCol3.a;
			}
		}
		/* Begin blending the flares */
		float alphComponent = min(1.0, fragCol0.a + fragCol1.a + fragCol2.a + fragCol3.a);
		fragCol0.rgb = attenToAlpha(fragCol0, alphComponent);
		fragCol1.rgb = attenToAlpha(fragCol1, alphComponent);
		fragCol2.rgb = attenToAlpha(fragCol2, alphComponent);
		fragCol3.rgb = attenToAlpha(fragCol3, alphComponent);
		vec4 fragColBlend = clampToOne(fragCol0 + fragCol1 + fragCol2 + fragCol3);
		gl_FragColor = vec4(fragColBlend.rgb, alphComponent);
	}
	