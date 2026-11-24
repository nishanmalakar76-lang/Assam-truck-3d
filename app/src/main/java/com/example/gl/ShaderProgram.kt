package com.example.gl

import android.opengl.GLES20

/**
 * OpenGL ES 2.0 shader program with hemisphere + directional sun lighting and atmospheric sky fog.
 */
class ShaderProgram {

  val programId: Int

  val aPositionHandle: Int
  val aNormalHandle: Int
  val uMvpMatrixHandle: Int
  val uModelMatrixHandle: Int
  val uColorHandle: Int
  val uUnlitHandle: Int
  val uFogColorHandle: Int

  init {
    val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
    val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
    programId = GLES20.glCreateProgram().also { prog ->
      GLES20.glAttachShader(prog, vertexShader)
      GLES20.glAttachShader(prog, fragmentShader)
      GLES20.glLinkProgram(prog)
      val linkStatus = IntArray(1)
      GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
      if (linkStatus[0] == 0) {
        val log = GLES20.glGetProgramInfoLog(prog)
        GLES20.glDeleteProgram(prog)
        throw RuntimeException("OpenGL program link failed: $log")
      }
    }
    GLES20.glDeleteShader(vertexShader)
    GLES20.glDeleteShader(fragmentShader)

    aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
    aNormalHandle = GLES20.glGetAttribLocation(programId, "aNormal")
    uMvpMatrixHandle = GLES20.glGetUniformLocation(programId, "uMvpMatrix")
    uModelMatrixHandle = GLES20.glGetUniformLocation(programId, "uModelMatrix")
    uColorHandle = GLES20.glGetUniformLocation(programId, "uColor")
    uUnlitHandle = GLES20.glGetUniformLocation(programId, "uUnlit")
    uFogColorHandle = GLES20.glGetUniformLocation(programId, "uFogColor")
  }

  private fun compileShader(type: Int, source: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, source)
    GLES20.glCompileShader(shader)
    val compiled = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
    if (compiled[0] == 0) {
      val log = GLES20.glGetShaderInfoLog(shader)
      GLES20.glDeleteShader(shader)
      throw RuntimeException("Shader compile failed: $log")
    }
    return shader
  }

  fun use() {
    GLES20.glUseProgram(programId)
  }

  companion object {
    private const val VERTEX_SHADER = """
      uniform mat4 uMvpMatrix;
      uniform mat4 uModelMatrix;
      attribute vec3 aPosition;
      attribute vec3 aNormal;
      varying vec3 vWorldNormal;
      varying float vDist;

      void main() {
        vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
        vec4 clipPos = uMvpMatrix * vec4(aPosition, 1.0);
        vWorldNormal = normalize((uModelMatrix * vec4(aNormal, 0.0)).xyz);
        vDist = length(clipPos.xyz);
        gl_Position = clipPos;
      }
    """

    private const val FRAGMENT_SHADER = """
      precision mediump float;
      uniform vec4 uColor;
      uniform float uUnlit;
      uniform vec3 uFogColor;
      varying vec3 vWorldNormal;
      varying float vDist;

      void main() {
        vec3 baseColor = uColor.rgb;
        vec3 finalRgb;
        if (uUnlit > 0.5) {
          finalRgb = baseColor;
        } else {
          vec3 sunDir = normalize(vec3(-0.42, 0.84, 0.35));
          float diff = max(dot(normalize(vWorldNormal), sunDir), 0.0);
          float hemi = 0.5 + 0.5 * normalize(vWorldNormal).y;
          vec3 ambient = mix(vec3(0.32, 0.42, 0.28), vec3(0.76, 0.83, 0.88), hemi);
          vec3 lit = baseColor * (ambient * 0.72 + vec3(1.0, 0.98, 0.92) * diff * 0.68);
          finalRgb = lit;
        }
        float fogFactor = clamp((vDist - 68.0) / (230.0 - 68.0), 0.0, 1.0);
        finalRgb = mix(finalRgb, uFogColor, fogFactor);
        gl_FragColor = vec4(finalRgb, uColor.a);
      }
    """
  }
}
