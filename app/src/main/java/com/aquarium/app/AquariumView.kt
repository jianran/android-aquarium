package com.aquarium.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class AquariumView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val fishList = mutableListOf<Fish>()
    private val bubbles = mutableListOf<Bubble>()
    private val seaweeds = mutableListOf<Seaweed>()

    private var waterGradient: LinearGradient? = null
    private var sandGradient: LinearGradient? = null
    private var time = 0f

    private val sandH get() = height * 0.13f
    private val surfaceH get() = height * 0.04f

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupGradients(w, h)
        initFish(w, h)
        initBubbles(w, h)
        initSeaweed(w, h)
    }

    private fun setupGradients(w: Int, h: Int) {
        waterGradient = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                Color.parseColor("#0A1F44"),
                Color.parseColor("#0D3575"),
                Color.parseColor("#1155AA"),
                Color.parseColor("#1976D2"),
                Color.parseColor("#42A5F5")
            ),
            floatArrayOf(0f, 0.2f, 0.45f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        sandGradient = LinearGradient(
            0f, h - sandH, 0f, h.toFloat(),
            intArrayOf(
                Color.parseColor("#F5DEB3"),
                Color.parseColor("#DEB887"),
                Color.parseColor("#B8860B")
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private fun initFish(w: Int, h: Int) {
        fishList.clear()
        val types = FishType.values()
        val yTop = surfaceH + 30f
        val yBot = h - sandH - 30f
        repeat(12) { i ->
            fishList.add(Fish(
                x = Random.nextFloat() * w,
                y = Random.nextFloat() * (yBot - yTop) + yTop,
                size = 28f + Random.nextFloat() * 38f,
                speed = 0.7f + Random.nextFloat() * 2.0f,
                type = types[i % types.size],
                direction = if (Random.nextBoolean()) 1 else -1,
                wobbleSpeed = 0.03f + Random.nextFloat() * 0.05f
            ))
        }
    }

    private fun initBubbles(w: Int, h: Int) {
        bubbles.clear()
        repeat(28) {
            bubbles.add(Bubble(
                x = Random.nextFloat() * w,
                y = Random.nextFloat() * h,
                radius = 2f + Random.nextFloat() * 8f,
                speed = 0.35f + Random.nextFloat() * 1.1f,
                wobble = Random.nextFloat() * (2f * PI).toFloat()
            ))
        }
    }

    private fun initSeaweed(w: Int, h: Int) {
        seaweeds.clear()
        val colors = intArrayOf(
            Color.parseColor("#2E7D32"),
            Color.parseColor("#388E3C"),
            Color.parseColor("#1B5E20"),
            Color.parseColor("#558B2F"),
            Color.parseColor("#33691E")
        )
        repeat(14) { i ->
            seaweeds.add(Seaweed(
                x = (i + 0.5f) * (w / 14f) + (Random.nextFloat() - 0.5f) * 18f,
                baseY = h - sandH + 10f,
                height = 38f + Random.nextFloat() * 85f,
                color = colors[i % colors.size],
                segments = Random.nextInt(4, 9),
                phase = Random.nextFloat() * (2f * PI).toFloat()
            ))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        time += 0.016f

        drawWater(canvas)
        drawLightRays(canvas)
        drawSand(canvas)
        drawSandDetail(canvas)
        drawSeaweed(canvas)
        drawBubbles(canvas)
        drawFish(canvas)
        drawWaterSurface(canvas)

        updatePhysics()
        postInvalidateOnAnimation()
    }

    private fun drawWater(canvas: Canvas) {
        paint.shader = waterGradient
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
    }

    private fun drawLightRays(canvas: Canvas) {
        paint.color = Color.WHITE
        paint.alpha = 10
        paint.style = Paint.Style.FILL
        val rw = width * 0.14f
        for (i in 0..5) {
            val shift = sin(time * 0.22f + i * 1.1f) * 28f
            val cx = width * (i + 0.5f) / 6f + shift
            path.reset()
            path.moveTo(cx - rw * 0.15f, 0f)
            path.lineTo(cx + rw * 0.15f, 0f)
            path.lineTo(cx + rw, height * 0.6f)
            path.lineTo(cx - rw, height * 0.6f)
            path.close()
            canvas.drawPath(path, paint)
        }
        paint.alpha = 255
    }

    private fun drawSand(canvas: Canvas) {
        val top = height - sandH
        paint.shader = sandGradient
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        path.reset()
        path.moveTo(0f, top)
        var x = 0f
        while (x <= width) {
            path.lineTo(x, top + sin(x * 0.032f + time * 0.12f) * 5f)
            x += 8f
        }
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()
        canvas.drawPath(path, paint)
        paint.shader = null
    }

    private fun drawSandDetail(canvas: Canvas) {
        val top = height - sandH
        // Ripples
        paint.color = Color.parseColor("#C9A84C")
        paint.alpha = 45
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        for (i in 0..7) {
            val y = top + 10f + i * 9f
            canvas.drawLine(12f + i * 6f, y, width - 12f - i * 6f, y, paint)
        }
        // Pebbles
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        val rng = Random(77)
        val pebbleColors = intArrayOf(
            Color.parseColor("#8D6E63"),
            Color.parseColor("#A1887F"),
            Color.parseColor("#BCAAA4"),
            Color.parseColor("#795548")
        )
        repeat(18) { i ->
            val px = rng.nextFloat() * width
            val py = top + 6f + rng.nextFloat() * 12f
            val pr = 2.5f + rng.nextFloat() * 5.5f
            paint.color = pebbleColors[i % pebbleColors.size]
            canvas.drawOval(px - pr, py - pr * 0.55f, px + pr, py + pr * 0.55f, paint)
        }
    }

    private fun drawSeaweed(canvas: Canvas) {
        seaweeds.forEach { sw ->
            sw.phase += 0.017f
            val segH = sw.height / sw.segments

            paint.color = sw.color
            paint.alpha = 215
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            paint.strokeCap = Paint.Cap.ROUND

            path.reset()
            path.moveTo(sw.x, sw.baseY)
            for (s in 0 until sw.segments) {
                val bx = sin(sw.phase + s * 0.85f) * 15f
                val midY = sw.baseY - segH * s - segH * 0.5f
                val topY = sw.baseY - segH * (s + 1)
                path.quadTo(sw.x + bx * 2f, midY, sw.x + bx, topY)
            }
            canvas.drawPath(path, paint)

            // Leaf ovals
            paint.style = Paint.Style.FILL
            paint.alpha = 185
            for (s in 0 until sw.segments) {
                if (s % 2 == 0) {
                    val lx = sw.x + sin(sw.phase + s * 0.85f) * 15f
                    val ly = sw.baseY - segH * s - segH * 0.5f
                    canvas.drawOval(lx - 10f, ly - 4.5f, lx + 10f, ly + 4.5f, paint)
                }
            }
            paint.alpha = 255
        }
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
    }

    private fun drawBubbles(canvas: Canvas) {
        bubbles.forEach { b ->
            val bx = b.x + sin(b.wobble) * 7f
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(40, 180, 230, 255)
            canvas.drawCircle(bx, b.y, b.radius, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.9f
            paint.color = Color.argb(55, 255, 255, 255)
            canvas.drawCircle(bx, b.y, b.radius, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.argb(95, 255, 255, 255)
            canvas.drawCircle(bx - b.radius * 0.32f, b.y - b.radius * 0.38f, b.radius * 0.2f, paint)
        }
        paint.strokeWidth = 0f
    }

    private fun drawFish(canvas: Canvas) {
        fishList.sortedBy { it.size }.forEach { fish ->
            canvas.save()
            val bobY = sin(fish.wobble) * 3.8f
            canvas.translate(fish.x, fish.y + bobY)
            if (fish.direction < 0) canvas.scale(-1f, 1f)
            drawFishBody(canvas, fish)
            canvas.restore()
        }
    }

    private fun drawFishBody(canvas: Canvas, fish: Fish) {
        val s = fish.size
        val t = fish.type
        val bodyColor = t.bodyColor.toInt()
        val finColor = t.finColor.toInt()

        val bodyW = if (t == FishType.PUFFERFISH) s * 0.5f else s * 0.52f
        val bodyH = when (t) {
            FishType.PUFFERFISH -> s * 0.48f
            FishType.ANGEL_FISH -> s * 0.44f
            else -> s * 0.34f
        }

        // ── Tail ──────────────────────────────────────────────────
        paint.color = finColor
        paint.alpha = 215
        path.reset()
        when (t) {
            FishType.GOLDFISH, FishType.ANGEL_FISH -> {
                path.moveTo(-bodyW + s * 0.1f, 0f)
                path.lineTo(-bodyW - s * 0.55f, -s * 0.52f)
                path.lineTo(-bodyW - s * 0.35f, 0f)
                path.lineTo(-bodyW - s * 0.55f, s * 0.52f)
                path.close()
            }
            else -> {
                path.moveTo(-bodyW + s * 0.06f, 0f)
                path.lineTo(-bodyW - s * 0.42f, -s * 0.38f)
                path.lineTo(-bodyW - s * 0.25f, 0f)
                path.lineTo(-bodyW - s * 0.42f, s * 0.38f)
                path.close()
            }
        }
        canvas.drawPath(path, paint)

        // ── Body ───────────────────────────────────────────────────
        paint.color = bodyColor
        paint.alpha = 255
        canvas.drawOval(-bodyW, -bodyH, bodyW + s * 0.06f, bodyH, paint)

        // ── Stripes ────────────────────────────────────────────────
        t.stripeColor?.let { sc ->
            paint.color = sc.toInt()
            paint.alpha = 168
            val sw2 = s * 0.058f
            listOf(-0.08f, 0.2f).forEach { sx ->
                canvas.drawRect(s * sx - sw2, -bodyH, s * sx + sw2, bodyH, paint)
            }
        }

        // ── Dorsal fin ─────────────────────────────────────────────
        paint.color = finColor
        paint.alpha = 185
        path.reset()
        path.moveTo(-bodyW * 0.15f, -bodyH)
        path.quadTo(bodyW * 0.15f, -bodyH - s * 0.35f, bodyW * 0.38f, -bodyH)
        canvas.drawPath(path, paint)

        // ── Pectoral fin ───────────────────────────────────────────
        paint.alpha = 155
        path.reset()
        path.moveTo(bodyW * 0.08f, bodyH * 0.15f)
        path.quadTo(bodyW * 0.45f, bodyH + s * 0.22f, bodyW * 0.6f, bodyH * 0.1f)
        canvas.drawPath(path, paint)

        // ── Eye ────────────────────────────────────────────────────
        val eyeX = bodyW * 0.52f
        val eyeY = -bodyH * 0.18f
        val eyeR = s * 0.125f

        paint.color = Color.WHITE
        paint.alpha = 255
        canvas.drawCircle(eyeX, eyeY, eyeR, paint)

        paint.color = t.eyeColor.toInt()
        canvas.drawCircle(eyeX + eyeR * 0.2f, eyeY, eyeR * 0.58f, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(eyeX + eyeR * 0.08f, eyeY - eyeR * 0.32f, eyeR * 0.2f, paint)

        // ── Smile ──────────────────────────────────────────────────
        paint.color = Color.argb(130, 0, 0, 0)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.032f
        path.reset()
        path.addArc(
            RectF(bodyW * 0.42f, -bodyH * 0.06f, bodyW * 0.72f, bodyH * 0.38f),
            180f, 180f
        )
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.alpha = 255
    }

    private fun drawWaterSurface(canvas: Canvas) {
        paint.color = Color.WHITE
        paint.alpha = 18
        paint.style = Paint.Style.FILL
        path.reset()
        path.moveTo(0f, 0f)
        var x = 0f
        while (x <= width) {
            path.lineTo(x, sin(x * 0.022f + time * 1.9f) * 8f + 16f)
            x += 6f
        }
        path.lineTo(width.toFloat(), 0f)
        path.close()
        canvas.drawPath(path, paint)
        paint.alpha = 255
    }

    private fun updatePhysics() {
        val w = width.toFloat()
        val swimTop = surfaceH + 18f
        val swimBot = height - sandH - 18f

        fishList.forEach { fish ->
            fish.wobble += fish.wobbleSpeed
            fish.x += fish.speed * fish.direction

            if (fish.x > w + fish.size) fish.x = -fish.size
            if (fish.x < -fish.size) fish.x = w + fish.size

            if (Random.nextFloat() < 0.012f) {
                fish.y += (Random.nextFloat() - 0.5f) * 4f
            }
            fish.y = fish.y.coerceIn(swimTop, swimBot)

            if (Random.nextFloat() < 0.003f) fish.direction *= -1
        }

        bubbles.forEach { b ->
            b.wobble += 0.038f
            b.y -= b.speed
            if (b.y + b.radius < 0f) {
                b.y = height + b.radius
                b.x = Random.nextFloat() * width
            }
        }
    }
}
