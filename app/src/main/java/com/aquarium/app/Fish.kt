package com.aquarium.app

data class Fish(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    val type: FishType,
    var direction: Int = 1,
    var wobble: Float = 0f,
    val wobbleSpeed: Float = 0.03f
)

enum class FishType(
    val bodyColor: Long,
    val finColor: Long,
    val eyeColor: Long = 0xFF111111L,
    val stripeColor: Long? = null
) {
    CLOWNFISH(0xFFFF6B35L, 0xFFFF8C00L, 0xFF111111L, 0xFFFFFFFFL),
    BLUE_TANG(0xFF1A73E8L, 0xFF0D47A1L, 0xFF111111L, 0xFFFFD600L),
    ANGEL_FISH(0xFFFFD700L, 0xFFFF8C00L, 0xFF111111L, 0xFFFFFFFFL),
    GUPPY(0xFF9C27B0L, 0xFFE91E63L, 0xFF111111L),
    PUFFERFISH(0xFF4CAF50L, 0xFF2E7D32L, 0xFF111111L),
    BUTTERFLY(0xFFFFF176L, 0xFFFFA000L, 0xFF111111L, 0xFF212121L),
    TETRA(0xFF29B6F6L, 0xFFE53935L, 0xFF111111L),
    GOLDFISH(0xFFFFB300L, 0xFFFF6F00L, 0xFF111111L)
}
