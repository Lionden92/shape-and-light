
	uniform mat4 u_MVPMatrix;
	attribute vec4 a_Position;
	varying vec2 v_Position;

	void main() {
		v_Position = vec2(a_Position.x, a_Position.y);
		gl_Position = u_MVPMatrix * a_Position;
	}
	