package com.aquarium.app

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object OFFLoader {
    data class Mesh(val vertices: FloatArray, val indices: ShortArray)

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

        // Pre-allocate worst-case: all quads → 6 indices each
        val idx = ShortArray(nf * 6)
        var cnt = 0
        repeat(nf) {
            val p = br.readLine().trim().split("\\s+".toRegex())
            val n = p[0].toInt()
            // fan triangulate: (v0, vj, vj+1) for j in 1..n-2
            for (j in 1 until n - 1) {
                idx[cnt++] = p[1].toShort()
                idx[cnt++] = p[j + 1].toShort()
                idx[cnt++] = p[j + 2].toShort()
            }
        }
        br.close()
        return Mesh(verts, idx.copyOf(cnt))
    }
}
