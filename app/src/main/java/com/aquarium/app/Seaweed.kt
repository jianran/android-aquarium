package com.aquarium.app

data class Seaweed(
    val x: Float,
    val baseY: Float,
    val height: Float,
    val color: Int,
    val segments: Int = 6,
    var phase: Float = 0f
)
