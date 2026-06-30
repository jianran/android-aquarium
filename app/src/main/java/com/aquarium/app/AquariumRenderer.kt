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

    private var bgProg  = 0
    private val bgVbo   = IntArray(1)
    private var bgAPos  = 0
    private var bgUTime = 0
    private val startMs = System.currentTimeMillis()

    private var fishProg    = 0
    private val vbo         = IntArray(3)
    private var idxCount    = 0
    private var uMVP        = 0
    private var uMV         = 0
    private var uFishType   = 0
    private var aPos        = 0
    private var aNorm       = 0

    private val projM = FloatArray(16)
    private val viewM = FloatArray(16)
    private val modM  = FloatArray(16)
    private val mvM   = FloatArray(16)
    private val mvpM  = FloatArray(16)

    // 8 Nemo characters: 0=Nemo 1=Dory 2=Marlin 3=Gill 4=Bubbles 5=Deb 6=Gurgle 7=Bloat
    private val N     = 8
    private val fx    = FloatArray(N)
    private val fy    = FloatArray(N)
    private val fdx   = IntArray(N)
    private val fdy   = IntArray(N)
    private val fspd  = FloatArray(N)
    private val fscl  = floatArrayOf(0.022f, 0.032f, 0.026f, 0.034f,
                                     0.030f, 0.022f, 0.026f, 0.036f)
    private val ftype = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7)

    private var bX = 0.5f
    private var bY = 0.7f
    private val rng = Random.Default

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        bgProg  = compile(BG_VERT, BG_FRAG)
        bgAPos  = GLES20.glGetAttribLocation(bgProg, "aPos")
        bgUTime = GLES20.glGetUniformLocation(bgProg, "uTime")
        val quad = floatArrayOf(-1f,-1f, 1f,-1f, 1f,1f, -1f,-1f, 1f,1f, -1f,1f)
        GLES20.glGenBuffers(1, bgVbo, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bgVbo[0])
        val qb = ByteBuffer.allocateDirect(48).order(ByteOrder.nativeOrder()).asFloatBuffer()
        qb.put(quad).position(0)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 48, qb, GLES20.GL_STATIC_DRAW)

        fishProg  = compile(FISH_VERT, FISH_FRAG)
        uMVP      = GLES20.glGetUniformLocation(fishProg, "uMVP")
        uMV       = GLES20.glGetUniformLocation(fishProg, "uMV")
        uFishType = GLES20.glGetUniformLocation(fishProg, "uFishType")
        aPos      = GLES20.glGetAttribLocation(fishProg, "aPos")
        aNorm     = GLES20.glGetAttribLocation(fishProg, "aNormal")

        val mesh = OFFLoader.load(ctx, "fish.txt")
        idxCount = mesh.indices.size
        GLES20.glGenBuffers(3, vbo, 0)
        uploadF(vbo[0], mesh.vertices)
        uploadF(vbo[2], mesh.normals)
        val ib = ByteBuffer.allocateDirect(mesh.indices.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer()
        ib.put(mesh.indices).position(0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vbo[1])
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mesh.indices.size * 2, ib, GLES20.GL_STATIC_DRAW)

        for (i in 0 until N) {
            fx[i]   = rng.nextFloat() * 1.2f - 0.6f
            fy[i]   = rng.nextFloat() * 1.0f - 0.5f
            fdx[i]  = if (rng.nextBoolean()) -1 else 1
            fdy[i]  = if (rng.nextBoolean()) -1 else 1
            fspd[i] = 0.0003f + rng.nextFloat() * 0.0004f
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val asp = width.toFloat() / height
        Matrix.perspectiveM(projM, 0, 45f, asp, 0.5f, 10f)
        Matrix.setLookAtM(viewM, 0, 0f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f)
        val halfH = 2.2f * 0.4142f * 0.85f
        bY = halfH; bX = asp * halfH
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val t = (System.currentTimeMillis() - startMs) / 1000f

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glUseProgram(bgProg)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bgVbo[0])
        GLES20.glEnableVertexAttribArray(bgAPos)
        GLES20.glVertexAttribPointer(bgAPos, 2, GLES20.GL_FLOAT, false, 8, 0)
        GLES20.glUniform1f(bgUTime, t)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        for (i in 0 until N) {
            if (fx[i] < -bX || fx[i] > bX) fdx[i] = -fdx[i]
            if (fy[i] < -bY || fy[i] > bY) fdy[i] = -fdy[i]
            if (rng.nextInt(10000) < 4) fdx[i] = -fdx[i]
            if (rng.nextInt(10000) < 4) fdy[i] = -fdy[i]
            fx[i] += fspd[i] * fdx[i]
            fy[i] += fspd[i] * 0.5f * fdy[i]
        }

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
            GLES20.glUniformMatrix4fv(uMVP, 1, false, mvpM, 0)
            GLES20.glUniformMatrix4fv(uMV,  1, false, mvM,  0)
            GLES20.glUniform1f(uFishType, ftype[i].toFloat())
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, idxCount, GLES20.GL_UNSIGNED_SHORT, 0)
        }
    }

    private fun uploadF(id: Int, data: FloatArray) {
        val b = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        b.put(data).position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.size * 4, b, GLES20.GL_STATIC_DRAW)
    }

    private fun compile(v: String, f: String): Int {
        fun sh(t: Int, s: String) = GLES20.glCreateShader(t).also {
            GLES20.glShaderSource(it, s); GLES20.glCompileShader(it)
        }
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, sh(GLES20.GL_VERTEX_SHADER, v))
            GLES20.glAttachShader(it, sh(GLES20.GL_FRAGMENT_SHADER, f))
            GLES20.glLinkProgram(it)
        }
    }

    companion object {

        // ── Background: P. Sherman dentist-tank ──────────────────────────────
        private val BG_VERT = """
            attribute vec2 aPos;
            varying vec2 vUV;
            void main() { gl_Position=vec4(aPos,0.0,1.0); vUV=aPos*0.5+0.5; }
        """.trimIndent()

        private val BG_FRAG = """
            precision mediump float;
            varying vec2 vUV;
            uniform float uTime;
            void main() {
                float t = uTime * 0.45;
                vec2 uv = vUV;

                // Warm tropical blue water (bright aquarium lamp)
                vec3 col = mix(vec3(0.02,0.18,0.42), vec3(0.10,0.50,0.78),
                               uv.y*0.85+0.15);

                // Golden caustics from lamp above
                float cx=uv.x*9.0+sin(uv.y*4.5+t*1.1)*0.9;
                float cy=uv.y*7.0+sin(uv.x*3.5-t*0.8)*0.7;
                float ca=sin(cx+t*0.9)*sin(cy-t*0.65);
                float cb=sin(cx*0.7-t*0.5)*sin(cy*1.3+t*0.4);
                col += pow(max(ca*cb+0.5,0.0),3.0)*0.60*vec3(0.85,1.0,0.55);

                // Bright tank lamp shafts
                float shaft=pow(sin(uv.x*3.8+t*0.10)*0.5+0.5,4.0)*(1.0-uv.y)*0.28;
                col += shaft*vec3(0.75,1.0,0.70);

                // Sandy floor (bottom 18%)
                float floorT=max(0.0,0.18-uv.y)/0.18;
                vec3 sand=vec3(0.88,0.80,0.58);
                sand += sin(uv.x*28.0+t*0.04)*0.06+sin(uv.x*15.0)*0.04;
                col=mix(col,sand,floorT*floorT);

                // Orange branching coral x≈0.10
                float c1=max(0.0,0.34-uv.y)*(1.0-smoothstep(0.0,0.09,abs(uv.x-0.10)))
                        *(0.6+0.4*sin(uv.y*38.0+uv.x*20.0));
                col=mix(col,vec3(0.98,0.30,0.06),clamp(c1*3.0,0.0,1.0));

                // Pink coral x≈0.38
                float c2=max(0.0,0.40-uv.y)*(1.0-smoothstep(0.0,0.12,abs(uv.x-0.38)))
                        *(0.55+0.45*sin(uv.y*30.0+uv.x*24.0));
                col=mix(col,vec3(0.95,0.12,0.52),clamp(c2*2.5,0.0,1.0));

                // Purple sea-fan (animated sway) x≈0.60
                float fanOff=sin(uv.y*6.0+t*0.9)*0.018;
                float c3=max(0.0,0.36-uv.y)*(1.0-smoothstep(0.0,0.13,abs(uv.x-0.60+fanOff)))
                        *(0.5+0.5*sin(uv.y*22.0+t*0.8));
                col=mix(col,vec3(0.52,0.05,0.78),clamp(c3*2.8,0.0,1.0));

                // Red coral x≈0.84
                float c4=max(0.0,0.30-uv.y)*(1.0-smoothstep(0.0,0.07,abs(uv.x-0.84)))
                        *(0.65+0.35*sin(uv.y*42.0+uv.x*18.0));
                col=mix(col,vec3(0.92,0.18,0.12),clamp(c4*3.5,0.0,1.0));

                // Nemo's anemone (waving orange tentacles) x≈0.50
                float aw=sin(uv.y*14.0+t*1.8)*0.018;
                float anem=max(0.0,0.24-uv.y)*(1.0-smoothstep(0.0,0.065,abs(uv.x-0.50+aw)))
                          *(0.5+0.5*sin(uv.y*24.0));
                col=mix(col,vec3(0.98,0.48,0.08),clamp(anem*4.0,0.0,1.0));

                // Swaying seaweed
                float sw1=max(0.0,0.28-uv.y)*(1.0-smoothstep(0.0,0.007,
                           abs(uv.x-0.24+sin(uv.y*5.5+t*1.3)*0.022)));
                float sw2=max(0.0,0.22-uv.y)*(1.0-smoothstep(0.0,0.006,
                           abs(uv.x-0.74+sin(uv.y*6.0+t*1.0+1.2)*0.026)));
                float sw3=max(0.0,0.25-uv.y)*(1.0-smoothstep(0.0,0.007,
                           abs(uv.x-0.48+sin(uv.y*4.8+t*1.5+2.4)*0.020)));
                col=mix(col,vec3(0.08,0.48,0.12),clamp((sw1+sw2+sw3)*40.0,0.0,1.0));

                // Bubbles (5, unrolled for GLSL ES compatibility)
                float bs=0.009;
                float bx0=0.18+sin(fract(t*0.09+0.00)*7.0+0.0)*0.012;
                float by0=fract(t*0.090+0.00);
                float bx1=0.34+sin(fract(t*0.075+0.20)*8.0+1.0)*0.014;
                float by1=fract(t*0.075+0.20);
                float bx2=0.50+sin(fract(t*0.110+0.40)*6.0+2.0)*0.010;
                float by2=fract(t*0.110+0.40);
                float bx3=0.66+sin(fract(t*0.082+0.60)*7.5+3.0)*0.013;
                float by3=fract(t*0.082+0.60);
                float bx4=0.82+sin(fract(t*0.095+0.80)*6.5+4.0)*0.011;
                float by4=fract(t*0.095+0.80);
                float bd0=length(uv-vec2(bx0,by0));
                float bd1=length(uv-vec2(bx1,by1));
                float bd2=length(uv-vec2(bx2,by2));
                float bd3=length(uv-vec2(bx3,by3));
                float bd4=length(uv-vec2(bx4,by4));
                float bub=smoothstep(bs+0.003,bs-0.001,bd0)+smoothstep(bs+0.003,bs-0.001,bd1)
                         +smoothstep(bs+0.003,bs-0.001,bd2)+smoothstep(bs+0.003,bs-0.001,bd3)
                         +smoothstep(bs+0.003,bs-0.001,bd4);
                float ring=(smoothstep(bs+0.004,bs+0.002,bd0)-smoothstep(bs+0.002,bs,bd0))
                          +(smoothstep(bs+0.004,bs+0.002,bd1)-smoothstep(bs+0.002,bs,bd1))
                          +(smoothstep(bs+0.004,bs+0.002,bd2)-smoothstep(bs+0.002,bs,bd2))
                          +(smoothstep(bs+0.004,bs+0.002,bd3)-smoothstep(bs+0.002,bs,bd3))
                          +(smoothstep(bs+0.004,bs+0.002,bd4)-smoothstep(bs+0.002,bs,bd4));
                col=mix(col,vec3(0.65,0.88,1.0),clamp(bub,0.0,1.0)*0.30);
                col+=clamp(ring,0.0,1.0)*0.55*vec3(1.0);

                // Glass tank frame
                float fw=0.028;
                float frame=max(max(step(1.0-fw,uv.x),step(uv.x,fw)),
                                max(step(1.0-fw,uv.y),step(uv.y,fw)));
                col=mix(col,vec3(0.20,0.17,0.13),frame);
                float glow=max(smoothstep(fw*3.0,fw,abs(uv.x-fw*0.5)),
                               smoothstep(fw*3.0,fw,abs(uv.x-(1.0-fw*0.5))));
                col+=glow*0.18*vec3(0.55,0.80,1.0)*(1.0-frame);

                // Peach the starfish on the right glass wall
                float pY=0.55+sin(t*0.12)*0.22;
                vec2 pUV=uv-vec2(1.0-fw*0.5,pY);
                float pR=length(pUV);
                float star5=cos(atan(pUV.y,pUV.x)*5.0)*0.5+0.5;
                col=mix(col,vec3(0.98,0.60,0.58),step(pR,0.028+star5*0.018));

                gl_FragColor=vec4(col,1.0);
            }
        """.trimIndent()

        // ── Fish vertex shader ────────────────────────────────────────────────
        private val FISH_VERT = """
            attribute vec3 aPos;
            attribute vec3 aNormal;
            uniform mat4 uMVP;
            uniform mat4 uMV;
            varying vec3 vN;
            varying vec3 vP;
            varying vec3 vObj;
            void main() {
                gl_Position=uMVP*vec4(aPos,1.0);
                vN  =mat3(uMV)*aNormal;
                vP  =(uMV*vec4(aPos,1.0)).xyz;
                vObj=aPos;
            }
        """.trimIndent()

        // ── Fish fragment: 8 Nemo characters ─────────────────────────────────
        private val FISH_FRAG = """
            precision mediump float;
            varying vec3 vN;
            varying vec3 vP;
            varying vec3 vObj;
            uniform float uFishType;
            void main() {
                float bodyT  =clamp((vObj.x+12.0)/20.0,0.0,1.0); // 0=head 1=tail
                float finDist=clamp(abs(vObj.z)/2.8,0.0,1.0);
                float finEdge=clamp((finDist-0.45)/0.55,0.0,1.0);

                // Scale shimmer (all species)
                float sc=(sin(vObj.x*3.3)*sin(vObj.z*5.2)
                         +sin(vObj.x*3.3+1.57)*sin(vObj.z*5.2+1.57))*0.09+0.91;

                vec3 skin=vec3(0.5);
                vec3 blk=vec3(0.04,0.02,0.02);
                vec3 white=vec3(1.0,0.97,0.93);

                // 0 & 2 — Clownfish (Nemo / Marlin)
                if(uFishType<0.5||(uFishType>1.5&&uFishType<2.5)){
                    vec3 org=vec3(0.95,0.36,0.02);
                    float bw=0.075;
                    float b1=smoothstep(bw,0.0,abs(bodyT-0.12));
                    float b2=smoothstep(bw,0.0,abs(bodyT-0.42));
                    float b3=smoothstep(bw,0.0,abs(bodyT-0.70));
                    float bands=clamp(b1+b2+b3,0.0,1.0);
                    float bk1=smoothstep(bw*1.55,0.0,abs(bodyT-0.12));
                    float bk2=smoothstep(bw*1.55,0.0,abs(bodyT-0.42));
                    float bk3=smoothstep(bw*1.55,0.0,abs(bodyT-0.70));
                    float bkB=clamp(bk1+bk2+bk3,0.0,1.0);
                    skin=mix(org,blk,bkB*(1.0-bands));
                    skin=mix(skin,white,bands);

                // 1 — Dory (Blue Tang)
                }else if(uFishType<1.5){
                    vec3 royal=vec3(0.07,0.22,0.88);
                    float tail=smoothstep(0.22,0.10,bodyT);
                    float edge=finEdge*0.70;
                    float curve=smoothstep(0.12,0.0,abs(bodyT-0.52)-finDist*0.30);
                    skin=royal;
                    skin=mix(skin,vec3(0.98,0.82,0.00),tail);
                    skin=mix(skin,blk,clamp(edge+curve*0.45,0.0,1.0)*(1.0-tail));

                // 3 — Gill (Moorish Idol)
                }else if(uFishType<3.5){
                    float wB=smoothstep(0.06,0.12,bodyT)*(1.0-smoothstep(0.62,0.68,bodyT));
                    float yB=smoothstep(0.68,0.74,bodyT)*(1.0-smoothstep(0.90,0.96,bodyT));
                    skin=blk;
                    skin=mix(skin,white,wB*0.92);
                    skin=mix(skin,vec3(0.94,0.76,0.00),yB);

                // 4 — Bubbles (Yellow Tang)
                }else if(uFishType<4.5){
                    skin=vec3(1.00,0.90,0.03);
                    skin=mix(skin,vec3(0.96,0.96,0.92),
                             smoothstep(0.025,0.005,abs(vObj.y-(-1.1)))*0.85);

                // 5 — Deb (Black & White Damselfish)
                }else if(uFishType<5.5){
                    float s=step(0.5,fract(bodyT*5.0));
                    skin=mix(vec3(0.04,0.03,0.04),white,s);
                    skin=mix(skin,vec3(0.15,0.30,0.72),(1.0-s)*0.25);

                // 6 — Gurgle (Royal Gramma)
                }else if(uFishType<6.5){
                    skin=mix(vec3(0.52,0.06,0.82),vec3(0.98,0.78,0.02),
                             smoothstep(0.45,0.55,bodyT));

                // 7 — Bloat (Pufferfish)
                }else{
                    vec3 puff=vec3(0.22,0.65,0.88);
                    float sx=fract(vObj.x*2.2);
                    float sz=fract(vObj.z*3.0+floor(vObj.x*2.2)*0.5);
                    float spot=1.0-smoothstep(0.10,0.22,length(vec2(sx-0.5,sz-0.5)));
                    skin=mix(puff,vec3(0.94,0.94,0.90),smoothstep(-1.8,-0.4,vObj.y)*0.55);
                    skin=mix(skin,vec3(0.06,0.18,0.36),spot*0.55);
                }

                skin*=sc*(1.0-finEdge*0.38);

                // Blinn-Phong
                vec3 N=normalize(vN);
                vec3 L=normalize(vec3(0.6,1.6,0.9));
                float diff=max(dot(N,L),0.0);
                vec3 V=normalize(-vP);
                vec3 H=normalize(L+V);
                float spec=pow(max(dot(N,H),0.0),88.0);
                vec3 col=(0.20+0.80*diff)*skin+spec*0.62*mix(vec3(1.0),vec3(0.45,0.88,1.0),0.6);
                col=mix(col,vec3(0.03,0.14,0.32),clamp(-vP.z/5.0,0.0,1.0)*0.28);
                gl_FragColor=vec4(col,1.0);
            }
        """.trimIndent()
    }
}
