package com.aquarium.app

data class Fish(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    val type: FishType,
    var direction: Int = 1,
    var wobble: Float = 0f,
    val wobbleSpeed: Float = 0.04f
)

enum class FishType {
    CLOWNFISH, BLUE_TANG, MANDARIN, MOORISH_IDOL, PARROTFISH, LIONFISH, YELLOW_TANG, BETTA
}
