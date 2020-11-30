		precision mediump float;
		/* The input texture */
		uniform sampler2D u_Texture0;
		uniform sampler2D u_Texture1;
		/* Interpolated texture coordinate per fragment */
		varying vec2 v_TexCoordinate0;
		varying vec2 v_TexCoordinate1;
		varying vec2 v_TexCoordinate2;
		/* Passed in lit color information */
		varying vec4 v_Color;
		
		void main() {
			vec4 texMap0 = texture2D(u_Texture0, v_TexCoordinate0);
			vec4 texMap1 = texture2D(u_Texture1, v_TexCoordinate1);
			vec4 texMap2 = texture2D(u_Texture1, v_TexCoordinate2);
			gl_FragColor = v_Color * (texMap0 * texMap1 * texMap2);
		}
		