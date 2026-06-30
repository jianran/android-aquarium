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

    // ── Background ──────────────────────────────────────────────────────────
    private var bgProg     = 0
    private val bgVbo      = IntArray(1)
    private var bgAPos     = 0
    private var bgUTime    = 0
    private val startMs    = System.currentTimeMillis()

    // ── Fish mesh ────────────────────────────────────────────────────────────
    private var fishProg   = 0
    private val vbo        = IntArray(3) // [0]=positions [1]=EBO [2]=normals
    private var idxCount   = 0

    private var uMVP       = 0
    private var uMV        = 0
    private var uColor     = 0
    private var aPos       = 0
    private var aNorm      = 0

    // ── Matrices ─────────────────────────────────────────────────────────────
    private val projM = FloatArray(16)
    private val viewM = FloatArray(16)
    private val modM  = FloatArray(16)
    private val mvM   = FloatArray(16)
    private val mvpM  = FloatArray(16)

    // ── Fish state ───────────────────────────────────────────────────────────
    private val N    = 8
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

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // Background program + quad VBO
        bgProg  = compile(BG_VERT, BG_FRAG)
        bgAPos  = GLES20.glGetAttribLocation(bgProg, "aPos")
        bgUTime = GLES20.glGetUniformLocation(bgProg, "uTime")

        val quad = floatArrayOf(
            -1f, -1f,   1f, -1f,   1f,  1f,
            -1f, -1f,   1f,  1f,  -1f,  1f
        )
        GLES20.glGenBuffers(1, bgVbo, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bgVbo[0])
        val qb = ByteBuffer.allocateDirect(quad.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        qb.put(quad).position(0)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, quad.size * 4, qb, GLES20.GL_STATIC_DRAW)

        // Fish program
        fishProg = compile(FISH_VERT, FISH_FRAG)
        uMVP   = GLES20.glGetUniformLocation(fishProg, "uMVP")
        uMV    = GLES20.glGetUniformLocation(fishProg, "uMV")
        uColor = GLES20.glGetUniformLocation(fishProg, "uColor")
        aPos   = GLES20.glGetAttribLocation(fishProg, "aPos")
        aNorm  = GLES20.glGetAttribLocation(fishProg, "aNormal")

        // Fish mesh
        val mesh = OFFLoader.load(ctx, "fish.txt")
        idxCount = mesh.indices.size
        GLES20.glGenBuffers(3, vbo, 0)
        uploadFloat(vbo[0], mesh.vertices)
        uploadFloat(vbo[2], mesh.normals)
        val ib = ByteBuffer.allocateDirect(mesh.indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        ib.put(mesh.indices).position(0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vbo[1])
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mesh.indices.size * 2, ib, GLES20.GL_STATIC_DRAW)

        // Init fish state
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

        val t = (System.currentTimeMillis() - startMs) / 1000f

        // ── Draw background (no depth writes so fish always appear in front) ──
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glUseProgram(bgProg)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bgVbo[0])
        GLES20.glEnableVertexAttribArray(bgAPos)
        GLES20.glVertexAttribPointer(bgAPos, 2, GLES20.GL_FLOAT, false, 8, 0)
        GLES20.glUniform1f(bgUTime, t)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // ── Animate fish ──────────────────────────────────────────────────────
        for (i in 0 until N) {
            if (fx[i] < -bX || fx[i] > bX) fdx[i] = -fdx[i]
            if (fy[i] < -bY || fy[i] > bY) fdy[i] = -fdy[i]
            if (rng.nextInt(10000) < 4) fdx[i] = -fdx[i]
            if (rng.nextInt(10000) < 4) fdy[i] = -fdy[i]
            fx[i] += fspd[i] * fdx[i]
            fy[i] += fspd[i] * 0.5f * fdy[i]
        }

        // ── Draw fish ─────────────────────────────────────────────────────────
        GLES20.glUseProgram(fishProg)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 12, 0)

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
            GLES20.glUniformMatrix4fv(uMV,    1, false, mvM,   0)
            GLES20.glUniform4f(uColor, fcol[i][0], fcol[i][1], fcol[i][2], 1f)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, idxCount, GLES20.GL_UNSIGNED_SHORT, 0)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun uploadFloat(id: Int, data: FloatArray) {
        val buf = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(data).position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.size * 4, buf, GLES20.GL_STATIC_DRAW)
    }

    private fun compile(vert: String, frag: String): Int {
        fun sh(type: Int, src: String) = GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, src); GLES20.glCompileShader(it)
        }
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, sh(GLES20.GL_VERTEX_SHADER,   vert))
            GLES20.glAttachShader(it, sh(GLES20.GL_FRAGMENT_SHADER, frag))
            GLES20.glLinkProgram(it)
        }
    }

    // ── Shaders ───────────────────────────────────────────────────────────────

    companion object {

        // ── Background: underwater environment ─────────────────────────────────
        private val BG_VERT = """
            attribute vec2 aPos;
            varying vec2 vUV;
            void main() {
                gl_Position = vec4(aPos, 0.0, 1.0);
                vUV = aPos * 0.5 + 0.5;
            }
        """.trimIndent()

        private val BG_FRAG = """
            precision mediump float;
            varying vec2 vUV;
            uniform float uTime;
            void main() {
                float t = uTime * 0.4;

                // Vertical gradient: deep navy at bottom → teal mid → pale cyan at surface
                vec3 deep    = vec3(0.01, 0.04, 0.20);
                vec3 midWater= vec3(0.04, 0.22, 0.48);
                vec3 surface = vec3(0.18, 0.58, 0.72);
                vec3 bg = mix(deep, midWater, vUV.y);
                bg = mix(bg, surface, max(0.0, (vUV.y - 0.55) / 0.45));

                // Animated caustics: two sin-wave interference patterns
                float cx = vUV.x * 7.0 + sin(vUV.y * 3.5 + t) * 0.6;
                float cy = vUV.y * 7.0 + sin(vUV.x * 2.8 - t) * 0.6;
                float caustic = sin(cx + t) * sin(cy - t * 0.8);
                caustic = pow(max(caustic, 0.0), 2.5) * 0.45;
                bg += caustic * vec3(0.45, 0.90, 1.00);

                // God-rays / light shafts from the surface
                float shaft = sin(vUV.x * 4.8 + t * 0.18) * 0.5 + 0.5;
                shaft = pow(shaft, 5.0) * (1.0 - vUV.y) * 0.18;
                bg += shaft * vec3(0.65, 0.95, 1.00);

                // Sandy floor at very bottom (bottom 12% of screen)
                float sandLine = max(0.0, 0.12 - vUV.y) / 0.12;
                vec3  sandColor = vec3(0.70, 0.62, 0.42);
                // Subtle sand ripple texture
                float ripple = sin(vUV.x * 18.0 + t * 0.05) * 0.5 + 0.5;
                sandColor *= (0.88 + 0.12 * ripple);
                bg = mix(bg, sandColor, sandLine * sandLine);

                // Thin water-surface shimmer at very top
                float surfaceLine = max(0.0, vUV.y - 0.92) / 0.08;
                bg = mix(bg, vec3(0.6, 0.9, 1.0), surfaceLine * 0.4);

                gl_FragColor = vec4(bg, 1.0);
            }
        """.trimIndent()

        // ── Fish: skin texture + Blinn-Phong ──────────────────────────────────
        private val FISH_VERT = """
            attribute vec3 aPos;
            attribute vec3 aNormal;
            uniform mat4 uMVP;
            uniform mat4 uMV;
            varying vec3 vN;
            varying vec3 vP;
            varying vec3 vObj;
            void main() {
                gl_Position = uMVP * vec4(aPos, 1.0);
                vN   = mat3(uMV) * aNormal;
                vP   = (uMV * vec4(aPos, 1.0)).xyz;
                vObj = aPos;
            }
        """.trimIndent()

        private val FISH_FRAG = """
            precision mediump float;
            varying vec3 vN;
            varying vec3 vP;
            varying vec3 vObj;
            uniform vec4 uColor;

            void main() {
                // ── Procedural fish skin ──────────────────────────────────────

                // Normalised body position: 0 = tail end, 1 = head
                float bodyT = clamp((vObj.x + 12.0) / 20.0, 0.0, 1.0);

                // Scale-like texture: two offset sine waves on the x-z plane
                // interference creates a hex-ish cell pattern
                float sc1 = sin(vObj.x * 3.2) * sin(vObj.z * 5.0);
                float sc2 = sin(vObj.x * 3.2 + 1.6) * sin(vObj.z * 5.0 + 1.6);
                float scales = (sc1 + sc2) * 0.10 + 0.90;   // subtle ±10 %

                // Stripe markings along body length (tropical fish pattern)
                float stripe = sin(bodyT * 3.14159 * 7.0) * 0.5 + 0.5;
                stripe = smoothstep(0.25, 0.75, stripe) * 0.30 + 0.70;

                // Belly lighter than back (y ≈ 0 → back, y ≈ -2 → belly)
                float belly = clamp(-vObj.y / 2.2, 0.0, 1.0);
                float bellyBright = mix(0.80, 1.35, belly);

                // Fin edge: z-extent beyond ±1.2 is fin, make it slightly
                // translucent/darker to show fin is thin
                float finDark = 1.0 - clamp((abs(vObj.z) - 1.2) / 1.0, 0.0, 1.0) * 0.35;

                vec3 skin = uColor.rgb * scales * stripe * bellyBright * finDark;

                // ── Blinn-Phong lighting ──────────────────────────────────────
                vec3 N = normalize(vN);
                vec3 L = normalize(vec3(1.0, 1.5, 1.0));   // view-space light
                float diff = max(dot(N, L), 0.0);

                vec3 V = normalize(-vP);
                vec3 H = normalize(L + V);
                float spec = pow(max(dot(N, H), 0.0), 96.0);

                // Iridescent scale highlight (slight blue-green tint on spec)
                vec3 specCol = mix(vec3(1.0), vec3(0.55, 0.90, 1.0), 0.55);

                vec3 col = (0.22 + 0.78 * diff) * skin + spec * 0.65 * specCol;

                // Subtle underwater depth fog (push darker as VP.z goes more negative)
                float depth = clamp(-vP.z / 5.0, 0.0, 1.0);
                col = mix(col, vec3(0.03, 0.10, 0.28), depth * 0.35);

                gl_FragColor = vec4(col, 1.0);
            }
        """.trimIndent()
    }
}
