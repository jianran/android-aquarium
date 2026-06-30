package com.aquarium.app

import android.content.Context
import android.opengl.GLSurfaceView

class AquariumGLView(context: Context) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        setRenderer(AquariumRenderer(context))
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}
