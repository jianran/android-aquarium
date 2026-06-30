package com.aquarium.app

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class AquariumRenderer(private val ctx: Context) : GLSurfaceView.Renderer {

    private var prog = 0
    private val vbo = IntArray(3) // [0]=positions, [1]=EBO, [2]=normals
    private var idxCount = 0

    private var uMVP   = 0
    private var uMV    = 0
    private var uColor = 0
    private var aPos   = 0
    private var aNorm  = 0

    private val projM = FloatArray(16)
    private val viewM = FloatArray(16)
    private val modM  = FloatArray(16)
    private val mvM   = FloatArray(16) // view * model  (used for normal transform)
    private val mvpM  = FloatArray(16) // proj * view * model

    private val N = 8
    private val fx   = FloatArray(N)
    private val fy   = FloatArray(N)
    private val fdx  = IntArray(N)
    private val fdy  = IntArray(N)
    private val fspd = FloatArray(N)
    private val fscl = FloatArray(N)
    private val fcol = Array(N) { FloatArray(3) }

    private var bX = 0.5f
    private var bY = 0.7f

    private val rng = Random.Default

    private val COLORS = arrayOf(
        floatArrayOf(1.0f, 0.45f, 0.00f),
        floatArrayOf(0.10f, 0.50f, 1.00f),
        floatArrayOf(1.0f, 1.00f, 0.00f),
        floatArrayOf(0.10f, 0.80f, 0.30f),
        floatArrayOf(0.75f, 0.00f, 0.75f),
        floatArrayOf(1.0f, 0.15f, 0.15f),
        floatArrayOf(0.0f,  0.90f, 0.90f),
        floatArrayOf(1.0f, 0.40f, 0.70f),
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.7f, 0.9f, 1.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        prog   = buildProg()
        uMVP   = GLES20.glGetUniformLocation(prog, "uMVP")
        uMV    = GLES20.glGetUniformLocation(prog, "uMV")
        uColor = GLES20.glGetUniformLocation(prog, "uColor")
        aPos   = GLES20.glGetAttribLocation(prog, "aPos")
        aNorm  = GLES20.glGetAttribLocation(prog, "aNormal")

        val mesh = OFFLoader.load(ctx, "fish.txt")
        idxCount = mesh.indices.size

        GLES20.glGenBuffers(3, vbo, 0)

        fun uploadFloat(slot: Int, data: FloatArray) {
            val buf = ByteBuffer.allocateDirect(data.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            buf.put(data).position(0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[slot])
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.size * 4, buf, GLES20.GL_STATIC_DRAW)
        }

        uploadFloat(0, mesh.vertices)
        uploadFloat(2, mesh.normals)

        val ib = ByteBuffer.allocateDirect(mesh.indices.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer()
        ib.put(mesh.indices).position(0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vbo[1])
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mesh.indices.size * 2, ib, GLES20.GL_STATIC_DRAW)

        for (i in 0 until N) {
            fx[i]   = rng.nextFloat() * 1.0f - 0.5f
            fy[i]   = rng.nextFloat() * 1.2f - 0.6f
            fdx[i]  = if (rng.nextBoolean()) -1 else 1
            fdy[i]  = if (rng.nextBoolean()) -1 else 1
            fspd[i] = 0.0004f + rng.nextFloat() * 0.0004f
            fscl[i] = 0.024f  + rng.nextFloat() * 0.014f
            fcol[i] = COLORS[i]
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val asp = width.toFloat() / height
        Matrix.perspectiveM(projM, 0, 45f, asp, 0.5f, 10f)
        Matrix.setLookAtM(viewM, 0, 0f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f)
        val halfH = 2.2f * 0.4142f * 0.85f
        bY = halfH
        bX = asp * halfH
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        for (i in 0 until N) {
            if (fx[i] < -bX || fx[i] > bX) fdx[i] = -fdx[i]
            if (fy[i] < -bY || fy[i] > bY) fdy[i] = -fdy[i]
            if (rng.nextInt(10000) < 4) fdx[i] = -fdx[i]
            if (rng.nextInt(10000) < 4) fdy[i] = -fdy[i]
            fx[i] += fspd[i] * fdx[i]
            fy[i] += fspd[i] * 0.5f * fdy[i]
        }

        GLES20.glUseProgram(prog)

        // Bind position attribute
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 12, 0)

        // Bind normal attribute
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[2])
        GLES20.glEnableVertexAttribArray(aNorm)
        GLES20.glVertexAttribPointer(aNorm, 3, GLES20.GL_FLOAT, false, 12, 0)

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vbo[1])

        for (i in 0 until N) {
            Matrix.setIdentityM(modM, 0)
            Matrix.translateM(modM, 0, fx[i], fy[i], -2.2f)
            if (fdx[i] > 0) Matrix.rotateM(modM, 0, 180f, 0f, 1f, 0f)
            Matrix.rotateM(modM, 0, -90f, 1f, 0f, 0f)
            Matrix.scaleM(modM, 0, fscl[i], fscl[i], fscl[i])

            Matrix.multiplyMM(mvM,  0, viewM, 0, modM, 0)
            Matrix.multiplyMM(mvpM, 0, projM, 0, mvM,  0)

            GLES20.glUniformMatrix4fv(uMVP,   1, false, mvpM,  0)
            GLES20.glUniformMatrix4fv(uMV,    1, false, mvM,   0) // for normal transform
            GLES20.glUniform4f(uColor, fcol[i][0], fcol[i][1], fcol[i][2], 1f)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, idxCount, GLES20.GL_UNSIGNED_SHORT, 0)
        }
    }

    private fun buildProg(): Int {
        fun shader(type: Int, src: String) = GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, src); GLES20.glCompileShader(it)
        }
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, shader(GLES20.GL_VERTEX_SHADER,   VERT))
            GLES20.glAttachShader(it, shader(GLES20.GL_FRAGMENT_SHADER, FRAG))
            GLES20.glLinkProgram(it)
        }
    }

    companion object {
        // Transforms position and normal into view space; fragment does Blinn-Phong
        private val VERT = """
            attribute vec3 aPos;
            attribute vec3 aNormal;
            uniform mat4 uMVP;
            uniform mat4 uMV;
            varying vec3 vN;
            varying vec3 vP;
            void main() {
                gl_Position = uMVP * vec4(aPos, 1.0);
                vN = mat3(uMV) * aNormal;
                vP = (uMV * vec4(aPos, 1.0)).xyz;
            }
        """.trimIndent()

        // Blinn-Phong: ambient + diffuse (NdotL) + specular highlight
        private val FRAG = """
            precision mediump float;
            varying vec3 vN;
            varying vec3 vP;
            uniform vec4 uColor;
            void main() {
                vec3 N = normalize(vN);
                vec3 L = normalize(vec3(1.0, 1.5, 1.0));
                float diff = max(dot(N, L), 0.0);
                vec3 V = normalize(-vP);
                vec3 H = normalize(L + V);
                float spec = pow(max(dot(N, H), 0.0), 64.0);
                vec3 col = (0.25 + 0.75 * diff) * uColor.rgb + spec * 0.5;
                gl_FragColor = vec4(col, 1.0);
            }
        """.trimIndent()
    }
}
