	precision mediump float;
	
	/* Position of the lights */
	uniform vec2 u_SparksPos;
	/* Color vectors for the lights */
	uniform vec4 u_SparksCol;
	/* Flare sizes that fall off with distance */
	uniform float u_SparksFlSize;
	/* Flare alpha factors that fall off with distance */
	uniform float u_SparksFlAlpha;
	/* Passed in data for fragment position */
	varying vec2 v_Position;
	
	/* Constants */
	// Compensation factor to keep float calcs from exceeding a value of 64K, 
	// which causes issues in certain devices.	
	const float COMP_FACT = 0.05;
	// Factor used in 'cheaper' fall-off equation, tuned for best appearance.
	const float K = 0.4;
	// Blank color vector.
	const vec4 BLANKCOL = vec4(0.0, 0.0, 0.0, 0.0);
	
	/* Function for flare fall-off */
	float falloff(float distRat) {
		return K * ((1.0 + K) / (distRat * distRat + K) - 1.0);
	}

	/* Main shader execution function */
	void main() {
		vec4 fragCol = BLANKCOL;
		if (u_SparksCol.a > .05) {
			float distRatio = length((COMP_FACT * u_SparksPos) - (COMP_FACT * v_Position))
				/ (COMP_FACT * u_SparksFlSize);
			if (distRatio <= 1.0) {
				fragCol = falloff(distRatio) * u_SparksCol;
				fragCol.a = u_SparksFlAlpha * fragCol.a;
			}
		}
		gl_FragColor = fragCol;
	}
	