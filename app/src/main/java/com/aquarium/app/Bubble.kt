package com.aquarium.app

data class Bubble(
    var x: Float,
    var y: Float,
    val radius: Float,
    val speed: Float,
    var wobble: Float = 0f
)
