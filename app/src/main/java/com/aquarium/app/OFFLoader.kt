package com.aquarium.app

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.sqrt

object OFFLoader {
    data class Mesh(val vertices: FloatArray, val normals: FloatArray, val indices: ShortArray)

    fun load(context: Context, asset: String): Mesh {
        val br = BufferedReader(InputStreamReader(context.assets.open(asset)), 65536)
        br.readLine() // "OFF"
        val counts = br.readLine().trim().split("\\s+".toRegex())
        val nv = counts[0].toInt()
        val nf = counts[1].toInt()

        val verts = FloatArray(nv * 3)
        repeat(nv) { i ->
            val p = br.readLine().trim().split("\\s+".toRegex())
            verts[i * 3]     = p[0].toFloat()
            verts[i * 3 + 1] = p[1].toFloat()
            verts[i * 3 + 2] = p[2].toFloat()
        }

        val idx = ShortArray(nf * 6)
        var cnt = 0
        repeat(nf) {
            val p = br.readLine().trim().split("\\s+".toRegex())
            val n = p[0].toInt()
            for (j in 1 until n - 1) {
                idx[cnt++] = p[1].toShort()
                idx[cnt++] = p[j + 1].toShort()
                idx[cnt++] = p[j + 2].toShort()
            }
        }
        br.close()

        val finalIdx = idx.copyOf(cnt)
        return Mesh(verts, computeNormals(verts, finalIdx), finalIdx)
    }

    private fun computeNormals(v: FloatArray, idx: ShortArray): FloatArray {
        val n = FloatArray(v.size) // accumulated normals (same layout as vertices)
        val tris = idx.size / 3
        for (t in 0 until tris) {
            val i0 = idx[t * 3].toInt().and(0xFFFF) * 3
            val i1 = idx[t * 3 + 1].toInt().and(0xFFFF) * 3
            val i2 = idx[t * 3 + 2].toInt().and(0xFFFF) * 3

            val ax = v[i1] - v[i0]; val ay = v[i1+1] - v[i0+1]; val az = v[i1+2] - v[i0+2]
            val bx = v[i2] - v[i0]; val by = v[i2+1] - v[i0+1]; val bz = v[i2+2] - v[i0+2]

            // face normal = a × b (area-weighted for better averaging)
            val nx = ay*bz - az*by
            val ny = az*bx - ax*bz
            val nz = ax*by - ay*bx

            n[i0]+=nx; n[i0+1]+=ny; n[i0+2]+=nz
            n[i1]+=nx; n[i1+1]+=ny; n[i1+2]+=nz
            n[i2]+=nx; n[i2+1]+=ny; n[i2+2]+=nz
        }

        // normalize each vertex normal
        for (i in 0 until v.size / 3) {
            val j = i * 3
            val len = sqrt((n[j]*n[j] + n[j+1]*n[j+1] + n[j+2]*n[j+2]).toDouble()).toFloat()
            if (len > 0f) { n[j] /= len; n[j+1] /= len; n[j+2] /= len }
        }
        return n
    }
}
