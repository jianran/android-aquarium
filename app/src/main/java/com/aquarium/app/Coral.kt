package com.aquarium.app

data class Coral(
    val type: CoralType,
    val x: Float,
    val baseY: Float,
    val size: Float,
    val primaryColor: Int,
    val secondaryColor: Int,
    var phase: Float = 0f
)

enum class CoralType { BRANCH, BRAIN, FAN, ANEMONE }
