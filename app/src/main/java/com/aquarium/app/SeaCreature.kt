package com.aquarium.app

data class Octopus(
    var x: Float,
    var y: Float,
    val size: Float,
    var direction: Int = 1,
    val speed: Float = 0.55f,
    var wobble: Float = 0f,
    // directional counters (mirrors reference repo idle logic)
    var cntX: Int = 0,
    var cntY: Int = 0,
    var velX: Int = 1,
    var velY: Int = 1
)

data class SeaUrchin(
    var x: Float,
    val sandY: Float,
    val size: Float,
    val bodyColor: Int,
    val spineColor: Int,
    var spinAngle: Float = 0f,   // rotates as it "walks" on spines
    var direction: Int = 1,
    val speed: Float = 0.22f,
    var cntX: Int = 0
)

data class FoodPellet(
    var x: Float,
    var y: Float,
    var vy: Float = 2.0f,
    var alpha: Float = 255f,
    var resting: Boolean = false
)

data class Seashell(
    val x: Float,
    val sandY: Float,
    val size: Float,
    val color: Int,
    val rotation: Float
)
