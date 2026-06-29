package com.aquarium.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class AquariumView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val fishList = mutableListOf<Fish>()
    private val bubbles = mutableListOf<Bubble>()
    private val corals = mutableListOf<Coral>()
    private val seaweeds = mutableListOf<Seaweed>()
    // marine snow: x, y, size, speed, alpha
    private val snow = ArrayList<FloatArray>()

    private var waterGrad: LinearGradient? = null
    private var sandGrad: LinearGradient? = null
    private var time = 0f

    private val sandH get() = height * 0.14f
    private val surfaceH get() = height * 0.04f

    init { setLayerType(LAYER_TYPE_HARDWARE, null) }

    // ── Init ──────────────────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        waterGrad = LinearGradient(0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                Color.parseColor("#020B18"),
                Color.parseColor("#041E3A"),
                Color.parseColor("#073B6F"),
                Color.parseColor("#0B5FA5"),
                Color.parseColor("#1A8FD1")
            ),
            floatArrayOf(0f, 0.18f, 0.45f, 0.75f, 1f), Shader.TileMode.CLAMP)
        sandGrad = LinearGradient(0f, h - sandH, 0f, h.toFloat(),
            intArrayOf(Color.parseColor("#E8C87A"), Color.parseColor("#C9A84C"), Color.parseColor("#A07830")),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        initFish(w, h); initBubbles(w, h); initCoral(w, h); initSeaweed(w, h); initSnow(w, h)
    }

    private fun initFish(w: Int, h: Int) {
        fishList.clear()
        val types = FishType.values()
        val yTop = surfaceH + 40f; val yBot = h - sandH - 40f
        // Specific sizes per species
        val sizes = mapOf(
            FishType.CLOWNFISH to (28f..42f), FishType.BLUE_TANG to (38f..55f),
            FishType.MANDARIN to (22f..34f), FishType.MOORISH_IDOL to (40f..60f),
            FishType.PARROTFISH to (44f..65f), FishType.LIONFISH to (38f..56f),
            FishType.YELLOW_TANG to (36f..52f), FishType.BETTA to (32f..50f)
        )
        repeat(14) { i ->
            val t = types[i % types.size]
            val r = sizes[t]!!
            fishList.add(Fish(
                x = Random.nextFloat() * w,
                y = Random.nextFloat() * (yBot - yTop) + yTop,
                size = r.start + Random.nextFloat() * (r.endInclusive - r.start),
                speed = 0.6f + Random.nextFloat() * 1.8f,
                type = t,
                direction = if (Random.nextBoolean()) 1 else -1,
                wobbleSpeed = 0.03f + Random.nextFloat() * 0.04f,
                wobble = Random.nextFloat() * 2f * PI.toFloat()
            ))
        }
    }

    private fun initBubbles(w: Int, h: Int) {
        bubbles.clear()
        repeat(30) {
            bubbles.add(Bubble(
                Random.nextFloat() * w, Random.nextFloat() * h,
                1.5f + Random.nextFloat() * 7f, 0.3f + Random.nextFloat() * 1.1f,
                Random.nextFloat() * 2f * PI.toFloat()
            ))
        }
    }

    private fun initCoral(w: Int, h: Int) {
        corals.clear()
        val baseY = h - sandH + 5f
        val branchColors = listOf(
            Pair(Color.parseColor("#FF6B6B"), Color.parseColor("#FF8E8E")),
            Pair(Color.parseColor("#FF8C42"), Color.parseColor("#FFAA70")),
            Pair(Color.parseColor("#C77DFF"), Color.parseColor("#E0AAFF")),
            Pair(Color.parseColor("#F72585"), Color.parseColor("#FF6BAE"))
        )
        val brainColors = listOf(
            Pair(Color.parseColor("#8B6914"), Color.parseColor("#C4A235")),
            Pair(Color.parseColor("#5A7A3A"), Color.parseColor("#7AAA55")),
            Pair(Color.parseColor("#8B4513"), Color.parseColor("#CD853F"))
        )
        // Branch corals
        repeat(6) { i ->
            val c = branchColors[i % branchColors.size]
            corals.add(Coral(CoralType.BRANCH,
                w * (i + 0.5f) / 6f + (Random.nextFloat() - 0.5f) * 40f, baseY,
                55f + Random.nextFloat() * 55f, c.first, c.second,
                Random.nextFloat() * 2f * PI.toFloat()))
        }
        // Brain corals
        repeat(4) { i ->
            val c = brainColors[i % brainColors.size]
            corals.add(Coral(CoralType.BRAIN,
                w * (i + 0.5f) / 4f + (Random.nextFloat() - 0.5f) * 30f, baseY,
                30f + Random.nextFloat() * 28f, c.first, c.second))
        }
        // Fan corals
        repeat(4) { i ->
            val fc = if (i % 2 == 0)
                Pair(Color.parseColor("#9B2D8E"), Color.parseColor("#C44BB8"))
            else
                Pair(Color.parseColor("#E85D04"), Color.parseColor("#F48C06"))
            corals.add(Coral(CoralType.FAN,
                w * (i * 0.22f + 0.1f), baseY,
                60f + Random.nextFloat() * 50f, fc.first, fc.second,
                Random.nextFloat() * 2f * PI.toFloat()))
        }
        // Anemones
        repeat(3) { i ->
            corals.add(Coral(CoralType.ANEMONE,
                w * (i + 0.5f) / 3.5f + (Random.nextFloat() - 0.5f) * 25f, baseY,
                35f + Random.nextFloat() * 20f,
                Color.parseColor("#FF6B9D"), Color.parseColor("#C77DFF"),
                Random.nextFloat() * 2f * PI.toFloat()))
        }
    }

    private fun initSeaweed(w: Int, h: Int) {
        seaweeds.clear()
        val cols = listOf(Color.parseColor("#1B5E20"), Color.parseColor("#2E7D32"),
            Color.parseColor("#388E3C"), Color.parseColor("#33691E"), Color.parseColor("#558B2F"))
        repeat(10) { i ->
            seaweeds.add(Seaweed(
                w * (i + 0.5f) / 10f + (Random.nextFloat() - 0.5f) * 15f,
                h - sandH + 8f, 40f + Random.nextFloat() * 80f,
                cols[i % cols.size], Random.nextInt(5, 9),
                Random.nextFloat() * 2f * PI.toFloat()))
        }
    }

    private fun initSnow(w: Int, h: Int) {
        snow.clear()
        repeat(60) {
            snow.add(floatArrayOf(
                Random.nextFloat() * w, Random.nextFloat() * h,
                0.8f + Random.nextFloat() * 2.5f,
                0.15f + Random.nextFloat() * 0.4f,
                (30 + Random.nextInt(50)).toFloat()
            ))
        }
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        time += 0.016f

        drawBackground(canvas)
        drawLightShafts(canvas)
        drawCaustics(canvas)
        drawSand(canvas)
        drawSeaweed(canvas)
        drawCoral(canvas)
        drawSnow(canvas)
        drawBubbles(canvas)
        drawFishAll(canvas)
        drawWaterSurface(canvas)

        updatePhysics()
        postInvalidateOnAnimation()
    }

    private fun drawBackground(canvas: Canvas) {
        paint.shader = waterGrad; paint.alpha = 255; paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
    }

    private fun drawLightShafts(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        for (i in 0..4) {
            val shift = sin(time * 0.18f + i * 1.4f) * 35f
            val cx = width * (i + 0.5f) / 5f + shift
            val rw = width * 0.1f
            val grad = LinearGradient(cx, 0f, cx, height * 0.6f,
                intArrayOf(Color.argb(30, 180, 230, 255), Color.argb(0, 180, 230, 255)),
                null, Shader.TileMode.CLAMP)
            paint.shader = grad
            path.reset()
            path.moveTo(cx - rw * 0.12f, 0f)
            path.lineTo(cx + rw * 0.12f, 0f)
            path.lineTo(cx + rw, height * 0.6f)
            path.lineTo(cx - rw, height * 0.6f)
            path.close()
            canvas.drawPath(path, paint)
        }
        paint.shader = null
    }

    private fun drawCaustics(canvas: Canvas) {
        val baseY = height - sandH - 30f
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(28, 255, 250, 180)
        val cell = 70f
        var gx = 0f
        while (gx < width + cell) {
            var gy = 0f
            while (gy < 4) {
                val cx = gx + sin(time * 0.7f + gy * 1.3f + gx * 0.01f) * 18f
                val cy = baseY - gy * 45f + cos(time * 0.5f + gx * 0.01f + gy * 0.9f) * 12f
                val rw = 14f + sin(time * 1.1f + gx * 0.02f + gy) * 7f
                val rh = 8f + cos(time * 0.8f + gy + gx * 0.015f) * 4f
                canvas.drawOval(cx - rw, cy - rh, cx + rw, cy + rh, paint)
                gy++
            }
            gx += cell
        }
    }

    private fun drawSand(canvas: Canvas) {
        val top = height - sandH
        paint.shader = sandGrad; paint.style = Paint.Style.FILL; paint.alpha = 255
        path.reset(); path.moveTo(0f, top)
        var x = 0f
        while (x <= width) { path.lineTo(x, top + sin(x * 0.03f + time * 0.1f) * 6f); x += 8f }
        path.lineTo(width.toFloat(), height.toFloat()); path.lineTo(0f, height.toFloat()); path.close()
        canvas.drawPath(path, paint); paint.shader = null
        // Ripple lines
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.2f
        paint.color = Color.argb(40, 180, 140, 60)
        for (i in 0..8) { val y = top + 10f + i * 9f; canvas.drawLine(8f + i*5f, y, width - 8f - i*5f, y, paint) }
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
        // Pebbles
        val rng = Random(99)
        repeat(22) { i ->
            val px = rng.nextFloat() * width; val py = top + 5f + rng.nextFloat() * 14f
            val pr = 2.5f + rng.nextFloat() * 6f
            paint.color = when (i % 4) {
                0 -> Color.parseColor("#8D6E63"); 1 -> Color.parseColor("#A1887F")
                2 -> Color.parseColor("#6D4C41"); else -> Color.parseColor("#BCAAA4")
            }
            canvas.drawOval(px - pr, py - pr * 0.55f, px + pr, py + pr * 0.55f, paint)
        }
        paint.alpha = 255
    }

    private fun drawSeaweed(canvas: Canvas) {
        seaweeds.forEach { sw ->
            sw.phase += 0.016f
            val segH = sw.height / sw.segments
            paint.color = sw.color; paint.alpha = 210
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 4.5f; paint.strokeCap = Paint.Cap.ROUND
            path.reset(); path.moveTo(sw.x, sw.baseY)
            for (s in 0 until sw.segments) {
                val bx = sin(sw.phase + s * 0.85f) * 14f
                path.quadTo(sw.x + bx * 2f, sw.baseY - segH * s - segH * 0.5f, sw.x + bx, sw.baseY - segH * (s + 1))
            }
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL; paint.alpha = 190
            for (s in 0 until sw.segments step 2) {
                val lx = sw.x + sin(sw.phase + s * 0.85f) * 14f
                val ly = sw.baseY - segH * s - segH * 0.5f
                canvas.drawOval(lx - 9f, ly - 4f, lx + 9f, ly + 4f, paint)
            }
            paint.alpha = 255
        }
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
    }

    private fun drawCoral(canvas: Canvas) {
        corals.forEach { coral ->
            coral.phase += 0.015f
            when (coral.type) {
                CoralType.BRANCH -> drawBranchCoral(canvas, coral)
                CoralType.BRAIN -> drawBrainCoral(canvas, coral)
                CoralType.FAN -> drawFanCoral(canvas, coral)
                CoralType.ANEMONE -> drawAnemone(canvas, coral)
            }
        }
    }

    private fun drawBranchCoral(canvas: Canvas, c: Coral) {
        fun drawBranch(cx: Float, cy: Float, len: Float, angle: Float, depth: Int) {
            if (depth == 0 || len < 4f) return
            val endX = cx + cos(angle) * len; val endY = cy - sin(angle) * len
            paint.color = if (depth > 2) c.primaryColor else c.secondaryColor
            paint.style = Paint.Style.STROKE; paint.strokeWidth = depth * 1.8f + 1f; paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(cx, cy, endX, endY, paint)
            val sway = sin(c.phase + depth * 0.6f) * 0.15f
            drawBranch(endX, endY, len * 0.68f, angle + 0.45f + sway, depth - 1)
            drawBranch(endX, endY, len * 0.65f, angle - 0.40f + sway, depth - 1)
            if (depth == 1) {
                paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
                paint.color = c.secondaryColor
                canvas.drawCircle(endX, endY, 3.5f, paint)
            }
        }
        drawBranch(c.x, c.baseY, c.size, PI.toFloat() / 2f + sin(c.phase * 0.3f) * 0.08f, 4)
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
    }

    private fun drawBrainCoral(canvas: Canvas, c: Coral) {
        val r = c.size
        // Base dome gradient
        val grad = RadialGradient(c.x - r * 0.25f, c.baseY - r * 0.6f, r * 0.4f,
            intArrayOf(c.secondaryColor, c.primaryColor, Color.argb(255, 60, 35, 10)),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        paint.shader = grad; paint.style = Paint.Style.FILL
        canvas.drawOval(c.x - r, c.baseY - r * 1.1f, c.x + r, c.baseY, paint)
        paint.shader = null
        // Groove pattern
        paint.color = Color.argb(80, 0, 0, 0)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.8f
        for (i in -4..4) {
            val ox = i * r * 0.22f
            path.reset(); path.moveTo(c.x + ox, c.baseY)
            path.cubicTo(c.x + ox - r * 0.1f, c.baseY - r * 0.5f,
                c.x + ox + r * 0.1f, c.baseY - r * 0.9f, c.x + ox, c.baseY - r * 1.1f)
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
    }

    private fun drawFanCoral(canvas: Canvas, c: Coral) {
        val sway = sin(c.phase) * 6f
        val h = c.size; val w = c.size * 0.75f
        paint.color = c.primaryColor; paint.alpha = 160
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.2f
        // Vertical branches
        val steps = 9
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val bx = c.x - w / 2 + t * w + sway * sin(t * PI.toFloat()) * 0.5f
            canvas.drawLine(bx, c.baseY, bx + sway * t, c.baseY - h * (0.4f + sin(t * PI.toFloat()) * 0.5f), paint)
        }
        // Horizontal mesh
        for (j in 1..7) {
            val frac = j / 8f
            val y = c.baseY - h * frac
            val hw = w * sin(frac * PI.toFloat()) * 0.55f
            canvas.drawLine(c.x - hw + sway * frac, y, c.x + hw + sway * frac, y, paint)
        }
        // Stem
        paint.alpha = 255; paint.strokeWidth = 3.5f
        paint.color = c.primaryColor
        canvas.drawLine(c.x, c.baseY, c.x + sway * 0.3f, c.baseY - h * 0.15f, paint)
        paint.alpha = 255; paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
    }

    private fun drawAnemone(canvas: Canvas, c: Coral) {
        // Base
        paint.color = Color.parseColor("#2ECC71"); paint.alpha = 200
        canvas.drawOval(c.x - c.size * 0.45f, c.baseY - 10f, c.x + c.size * 0.45f, c.baseY, paint)
        // Tentacles
        val count = 12
        for (i in 0 until count) {
            val angle = (i.toFloat() / count) * 2f * PI.toFloat()
            val sway = sin(c.phase + i * 0.7f) * 12f
            val tx = c.x + cos(angle) * c.size * 0.3f
            val ty = c.baseY - c.size * 0.85f
            val cx1 = tx + sway; val cy1 = c.baseY - c.size * 0.45f
            paint.color = if (i % 2 == 0) c.primaryColor else c.secondaryColor
            paint.alpha = 200; paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f; paint.strokeCap = Paint.Cap.ROUND
            path.reset(); path.moveTo(c.x + cos(angle) * 8f, c.baseY - 5f)
            path.quadTo(cx1, cy1, tx, ty)
            canvas.drawPath(path, paint)
            // Bulb tip
            paint.style = Paint.Style.FILL; paint.strokeWidth = 0f; paint.alpha = 255
            paint.color = Color.parseColor("#FF6B9D")
            canvas.drawCircle(tx, ty, 4.5f, paint)
        }
        paint.alpha = 255
    }

    private fun drawSnow(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        snow.forEach { p ->
            paint.color = Color.argb(p[4].toInt(), 220, 235, 255)
            canvas.drawCircle(p[0], p[1], p[2], paint)
        }
    }

    private fun drawBubbles(canvas: Canvas) {
        bubbles.forEach { b ->
            val bx = b.x + sin(b.wobble) * 6f
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(35, 160, 220, 255)
            canvas.drawCircle(bx, b.y, b.radius, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.8f
            paint.color = Color.argb(60, 255, 255, 255)
            canvas.drawCircle(bx, b.y, b.radius, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(90, 255, 255, 255)
            canvas.drawCircle(bx - b.radius * 0.3f, b.y - b.radius * 0.35f, b.radius * 0.22f, paint)
        }
        paint.strokeWidth = 0f
    }

    private fun drawFishAll(canvas: Canvas) {
        fishList.sortedBy { it.size }.forEach { fish ->
            canvas.save()
            val tilt = sin(fish.wobble * 0.5f) * 6f
            val bobY = sin(fish.wobble) * 4f
            canvas.translate(fish.x, fish.y + bobY)
            canvas.rotate(tilt * fish.direction)
            if (fish.direction < 0) canvas.scale(-1f, 1f)
            drawFishBody(canvas, fish)
            canvas.restore()
        }
    }

    private fun drawFishBody(canvas: Canvas, fish: Fish) {
        when (fish.type) {
            FishType.CLOWNFISH    -> drawClownfish(canvas, fish)
            FishType.BLUE_TANG    -> drawBlueTang(canvas, fish)
            FishType.MANDARIN     -> drawMandarinFish(canvas, fish)
            FishType.MOORISH_IDOL -> drawMoorishIdol(canvas, fish)
            FishType.PARROTFISH   -> drawParrotfish(canvas, fish)
            FishType.LIONFISH     -> drawLionfish(canvas, fish)
            FishType.YELLOW_TANG  -> drawYellowTang(canvas, fish)
            FishType.BETTA        -> drawBetta(canvas, fish)
        }
    }

    // ── Fish body helpers ─────────────────────────────────────────────────────

    private fun fishBodyPath(bW: Float, bH: Float): Path {
        val p = Path()
        p.moveTo(bW, 0f)
        p.cubicTo(bW * 0.65f, -bH * 0.55f, bW * 0.1f, -bH, -bW * 0.55f, -bH * 0.88f)
        p.cubicTo(-bW * 0.78f, -bH * 0.55f, -bW * 0.82f, -bH * 0.18f, -bW * 0.75f, 0f)
        p.cubicTo(-bW * 0.82f, bH * 0.18f, -bW * 0.78f, bH * 0.55f, -bW * 0.55f, bH * 0.88f)
        p.cubicTo(bW * 0.1f, bH, bW * 0.65f, bH * 0.55f, bW, 0f)
        p.close(); return p
    }

    private fun drawScales(canvas: Canvas, bodyPath: Path, bW: Float, bH: Float, scaleColor: Int) {
        canvas.save()
        canvas.clipPath(bodyPath)
        paint.color = scaleColor; paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.6f
        val sx = bW * 0.13f; val sy = bH * 0.15f
        var row = -4
        while (row <= 4) {
            var col = -6
            while (col <= 5) {
                val ox = col * sx + if (row % 2 == 0) 0f else sx * 0.5f
                val oy = row * sy
                path.reset()
                path.addArc(RectF(ox - sx * 0.55f, oy, ox + sx * 0.55f, oy + sy * 0.85f), 180f, 180f)
                canvas.drawPath(path, paint)
                col++
            }
            row++
        }
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
        canvas.restore()
    }

    private fun drawEye(canvas: Canvas, ex: Float, ey: Float, r: Float, irisColor: Int) {
        paint.color = Color.WHITE; canvas.drawCircle(ex, ey, r, paint)
        paint.color = irisColor; canvas.drawCircle(ex + r * 0.12f, ey, r * 0.68f, paint)
        paint.color = Color.argb(230, 0, 0, 0); canvas.drawCircle(ex + r * 0.18f, ey, r * 0.38f, paint)
        paint.color = Color.argb(200, 255, 255, 255); canvas.drawCircle(ex - r * 0.05f, ey - r * 0.32f, r * 0.22f, paint)
    }

    private fun drawForkTail(canvas: Canvas, attachX: Float, spread: Float, depth: Float, color: Int, wobble: Float) {
        canvas.save()
        canvas.translate(attachX, 0f)
        canvas.rotate(sin(wobble * 1.5f) * 18f)
        paint.color = color; paint.alpha = 220
        path.reset()
        path.moveTo(0f, 0f)
        path.cubicTo(-depth * 0.5f, -spread * 0.3f, -depth * 0.9f, -spread * 0.7f, -depth, -spread)
        path.cubicTo(-depth * 0.85f, -spread * 0.3f, -depth * 0.6f, 0f, 0f, 0f)
        path.cubicTo(-depth * 0.6f, 0f, -depth * 0.85f, spread * 0.3f, -depth, spread)
        path.cubicTo(-depth * 0.9f, spread * 0.7f, -depth * 0.5f, spread * 0.3f, 0f, 0f)
        path.close(); canvas.drawPath(path, paint)
        paint.alpha = 255; canvas.restore()
    }

    // ── Clownfish ─────────────────────────────────────────────────────────────

    private fun drawClownfish(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.48f; val bH = s * 0.30f
        val bodyPath = fishBodyPath(bW, bH)
        // Tail
        drawForkTail(canvas, -bW * 0.75f, bH * 0.9f, s * 0.32f, Color.parseColor("#FF4500"), fish.wobble)
        // Body gradient: orange
        val bg = LinearGradient(0f, -bH, 0f, bH,
            intArrayOf(Color.parseColor("#CC3300"), Color.parseColor("#FF5500"), Color.parseColor("#FF8C42")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawPath(bodyPath, paint); paint.shader = null
        // White bars clipped to body
        canvas.save(); canvas.clipPath(bodyPath)
        paint.color = Color.WHITE
        canvas.drawRect(s * 0.20f, -bH, s * 0.34f, bH, paint)   // head bar
        canvas.drawRect(-s * 0.07f, -bH, s * 0.08f, bH, paint)  // mid bar
        canvas.drawRect(-s * 0.32f, -bH, -s * 0.22f, bH, paint) // tail bar
        // Black outlines on bars
        paint.color = Color.argb(180, 0, 0, 0); paint.style = Paint.Style.STROKE; paint.strokeWidth = s * 0.028f
        for (x in listOf(s * 0.20f, -s * 0.07f, -s * 0.32f)) {
            canvas.drawRect(x, -bH, x + s * 0.14f, bH, paint)
        }
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
        canvas.restore()
        // Body outline
        paint.color = Color.argb(120, 80, 20, 0); paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.2f
        canvas.drawPath(bodyPath, paint); paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
        // Dorsal fin
        paint.color = Color.parseColor("#FF4500"); paint.alpha = 200
        path.reset(); path.moveTo(-bW * 0.08f, -bH); path.quadTo(bW * 0.12f, -bH - s * 0.3f, bW * 0.36f, -bH)
        canvas.drawPath(path, paint); paint.alpha = 255
        // Eye
        drawEye(canvas, bW * 0.5f, -bH * 0.2f, s * 0.11f, Color.parseColor("#CC5500"))
    }

    // ── Blue Tang ─────────────────────────────────────────────────────────────

    private fun drawBlueTang(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.46f; val bH = s * 0.38f
        val bodyPath = fishBodyPath(bW, bH)
        // Yellow tail
        drawForkTail(canvas, -bW * 0.75f, bH * 0.85f, s * 0.30f, Color.parseColor("#FFD600"), fish.wobble)
        // Body gradient: royal blue
        val bg = LinearGradient(0f, -bH, 0f, bH,
            intArrayOf(Color.parseColor("#0033BB"), Color.parseColor("#1155DD"), Color.parseColor("#2277EE")),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawPath(bodyPath, paint); paint.shader = null
        // Black palette marking
        canvas.save(); canvas.clipPath(bodyPath)
        paint.color = Color.argb(200, 0, 0, 0)
        path.reset()
        path.moveTo(bW * 0.4f, -bH * 0.08f)
        path.cubicTo(bW * 0.0f, -bH * 0.75f, -bW * 0.65f, -bH * 0.6f, -bW * 0.7f, 0f)
        path.cubicTo(-bW * 0.65f, bH * 0.15f, bW * 0.0f, bH * 0.1f, bW * 0.4f, -bH * 0.08f)
        path.close(); canvas.drawPath(path, paint)
        canvas.restore()
        // Scales
        drawScales(canvas, bodyPath, bW, bH, Color.argb(25, 0, 30, 100))
        // Dorsal
        paint.color = Color.parseColor("#1144CC"); paint.alpha = 200
        path.reset(); path.moveTo(-bW * 0.1f, -bH); path.quadTo(bW * 0.2f, -bH - s * 0.35f, bW * 0.42f, -bH)
        canvas.drawPath(path, paint); paint.alpha = 255
        // Anal fin
        paint.color = Color.parseColor("#FFD600"); paint.alpha = 185
        path.reset(); path.moveTo(-bW * 0.15f, bH); path.quadTo(bW * 0.1f, bH + s * 0.22f, bW * 0.35f, bH)
        canvas.drawPath(path, paint); paint.alpha = 255
        drawEye(canvas, bW * 0.5f, -bH * 0.12f, s * 0.115f, Color.parseColor("#2255BB"))
    }

    // ── Mandarin Fish ─────────────────────────────────────────────────────────

    private fun drawMandarinFish(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.42f; val bH = s * 0.30f
        val bodyPath = fishBodyPath(bW, bH)
        // Tail: fan-shaped
        paint.color = Color.parseColor("#FF6B00"); paint.alpha = 210
        path.reset(); path.moveTo(-bW * 0.75f, 0f)
        path.cubicTo(-bW * 1.2f, -bH * 0.85f, -bW * 1.35f, -bH * 0.55f, -bW * 1.3f, 0f)
        path.cubicTo(-bW * 1.35f, bH * 0.55f, -bW * 1.2f, bH * 0.85f, -bW * 0.75f, 0f)
        path.close(); canvas.drawPath(path, paint); paint.alpha = 255
        // Blue base
        val bg = LinearGradient(0f, -bH, 0f, bH,
            intArrayOf(Color.parseColor("#003580"), Color.parseColor("#0055BB"), Color.parseColor("#0077CC")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawPath(bodyPath, paint); paint.shader = null
        // Psychedelic orange/green wavy patterns clipped
        canvas.save(); canvas.clipPath(bodyPath)
        paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND
        val waveColors = listOf(Color.parseColor("#FF6B00"), Color.parseColor("#FF8C00"),
            Color.parseColor("#00CC88"), Color.parseColor("#FF3366"), Color.parseColor("#FFCC00"))
        for (wi in 0 until 6) {
            paint.color = waveColors[wi % waveColors.size]; paint.strokeWidth = s * 0.055f; paint.alpha = 200
            path.reset()
            val yBase = -bH + bH * 2f * wi / 5f
            path.moveTo(-bW * 0.8f, yBase)
            for (xi in -7..6) {
                val xp = xi * bW * 0.22f
                val yp = yBase + sin(xi * 0.9f + wi * 1.2f + time * 0.5f) * bH * 0.28f
                path.lineTo(xp, yp)
            }
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f; paint.alpha = 255
        canvas.restore()
        // Spiny dorsal
        paint.color = Color.parseColor("#FF6B00"); paint.alpha = 220
        path.reset(); path.moveTo(-bW * 0.15f, -bH)
        for (si in 0..5) {
            val sx = -bW * 0.15f + si * bW * 0.2f
            val sy = -bH - s * (0.15f + sin(si.toFloat()) * 0.08f)
            path.lineTo(sx, sy); path.lineTo(sx + bW * 0.1f, -bH)
        }
        canvas.drawPath(path, paint); paint.alpha = 255
        drawEye(canvas, bW * 0.5f, -bH * 0.1f, s * 0.10f, Color.parseColor("#FF8800"))
    }

    // ── Moorish Idol ──────────────────────────────────────────────────────────

    private fun drawMoorishIdol(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.38f; val bH = s * 0.50f
        val bodyPath = fishBodyPath(bW, bH)
        drawForkTail(canvas, -bW * 0.75f, bH * 0.65f, s * 0.25f, Color.parseColor("#1A1A1A"), fish.wobble)
        // White body base
        paint.color = Color.WHITE; canvas.drawPath(bodyPath, paint)
        // Bands clipped
        canvas.save(); canvas.clipPath(bodyPath)
        paint.color = Color.parseColor("#1A1A1A")
        // Black band near head
        canvas.drawRect(bW * 0.2f, -bH, bW * 0.52f, bH, paint)
        // Black band near tail
        canvas.drawRect(-bW * 0.6f, -bH, -bW * 0.18f, bH, paint)
        // Yellow snout/face
        paint.color = Color.parseColor("#FFD600")
        canvas.drawOval(bW * 0.5f, -bH * 0.55f, bW * 1.1f, bH * 0.55f, paint)
        canvas.restore()
        // Very long dorsal filament
        paint.color = Color.parseColor("#1A1A1A"); paint.alpha = 240; paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f; paint.strokeCap = Paint.Cap.ROUND
        val filSway = sin(time * 0.8f) * 12f
        path.reset(); path.moveTo(bW * 0.1f, -bH)
        path.cubicTo(bW * 0.25f + filSway, -bH - s * 1.4f, bW * 0.0f + filSway, -bH - s * 1.8f,
            -bW * 0.1f + filSway, -bH - s * 2.0f)
        canvas.drawPath(path, paint); paint.style = Paint.Style.FILL; paint.strokeWidth = 0f; paint.alpha = 255
        // Dorsal fin wide triangle
        paint.color = Color.parseColor("#1A1A1A"); paint.alpha = 200
        path.reset(); path.moveTo(bW * 0.1f, -bH); path.lineTo(bW * 0.28f, -bH - s * 0.6f)
        path.lineTo(bW * 0.28f, -bH); path.close(); canvas.drawPath(path, paint); paint.alpha = 255
        drawEye(canvas, bW * 0.62f, -bH * 0.08f, s * 0.105f, Color.parseColor("#8B6914"))
    }

    // ── Parrotfish ────────────────────────────────────────────────────────────

    private fun drawParrotfish(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.54f; val bH = s * 0.28f
        val bodyPath = fishBodyPath(bW, bH)
        // Rounded tail
        paint.color = Color.parseColor("#00BFA5"); paint.alpha = 200
        canvas.drawOval(-bW * 1.15f, -bH * 0.65f, -bW * 0.68f, bH * 0.65f, paint); paint.alpha = 255
        // Body gradient: turquoise
        val bg = LinearGradient(0f, -bH, 0f, bH,
            intArrayOf(Color.parseColor("#006064"), Color.parseColor("#00897B"), Color.parseColor("#4DB6AC")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawPath(bodyPath, paint); paint.shader = null
        // Pink face gradient
        val fg = RadialGradient(bW * 0.55f, 0f, bW * 0.55f,
            intArrayOf(Color.parseColor("#FF6B9D"), Color.parseColor("#00897B")),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        canvas.save(); canvas.clipPath(bodyPath); paint.shader = fg
        canvas.drawOval(bW * 0.0f, -bH, bW * 1.1f, bH, paint)
        paint.shader = null; canvas.restore()
        drawScales(canvas, bodyPath, bW, bH, Color.argb(30, 0, 80, 70))
        // Beak mouth
        paint.color = Color.parseColor("#F4A460")
        canvas.drawOval(bW * 0.8f, -bH * 0.18f, bW * 1.08f, bH * 0.18f, paint)
        paint.color = Color.argb(150, 80, 40, 0)
        canvas.drawLine(bW * 0.82f, 0f, bW * 1.06f, 0f, paint)
        // Dorsal fin
        paint.color = Color.parseColor("#FF6B9D"); paint.alpha = 185
        path.reset(); path.moveTo(-bW * 0.1f, -bH); path.quadTo(bW * 0.2f, -bH - s * 0.22f, bW * 0.45f, -bH)
        canvas.drawPath(path, paint); paint.alpha = 255
        drawEye(canvas, bW * 0.58f, -bH * 0.2f, s * 0.10f, Color.parseColor("#006064"))
    }

    // ── Lionfish ──────────────────────────────────────────────────────────────

    private fun drawLionfish(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.46f; val bH = s * 0.34f
        val bodyPath = fishBodyPath(bW, bH)
        // Fan pectoral fins
        val wag = sin(fish.wobble * 0.8f) * 10f
        paint.alpha = 160
        for (side in listOf(-1f, 1f)) {
            for (ray in 0..7) {
                val rayAngle = (ray / 7f) * 130f - 65f + wag * side
                val rayLen = bW * (0.9f + sin(ray.toFloat()) * 0.15f)
                paint.color = if (ray % 2 == 0) Color.parseColor("#CC2200") else Color.parseColor("#EE4422")
                canvas.save()
                canvas.translate(bW * 0.05f, 0f); canvas.rotate(90f + rayAngle * side)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = s * 0.035f; paint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(0f, 0f, 0f, -rayLen, paint)
                canvas.restore()
            }
        }
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f; paint.alpha = 255
        // Tail
        drawForkTail(canvas, -bW * 0.75f, bH * 0.75f, s * 0.28f, Color.parseColor("#AA1100"), fish.wobble)
        // Body gradient: dark red/brown
        val bg = LinearGradient(0f, -bH, 0f, bH,
            intArrayOf(Color.parseColor("#8B1A00"), Color.parseColor("#CC2200"), Color.parseColor("#FF4422")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawPath(bodyPath, paint); paint.shader = null
        // White + red vertical stripes
        canvas.save(); canvas.clipPath(bodyPath)
        val stripeW = bW * 0.10f
        for (si in -5..5) {
            val sx = si * stripeW * 2.2f
            if (si % 2 == 0) {
                paint.color = Color.argb(140, 255, 255, 255)
                canvas.drawRect(sx, -bH, sx + stripeW, bH, paint)
            }
        }
        canvas.restore()
        // Dorsal spines
        paint.color = Color.parseColor("#AA1100"); paint.alpha = 220
        for (si in 0..6) {
            val sx = -bW * 0.25f + si * bW * 0.18f; val sh = s * (0.28f + sin(si.toFloat()) * 0.12f)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.2f
            canvas.drawLine(sx, -bH, sx + sin(si * 0.5f) * 6f, -bH - sh, paint)
        }
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f; paint.alpha = 255
        drawEye(canvas, bW * 0.5f, -bH * 0.18f, s * 0.10f, Color.parseColor("#CC2200"))
    }

    // ── Yellow Tang ───────────────────────────────────────────────────────────

    private fun drawYellowTang(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.43f; val bH = s * 0.40f
        val bodyPath = fishBodyPath(bW, bH)
        drawForkTail(canvas, -bW * 0.75f, bH * 0.80f, s * 0.28f, Color.parseColor("#FFB300"), fish.wobble)
        val bg = LinearGradient(0f, -bH, 0f, bH,
            intArrayOf(Color.parseColor("#F9A825"), Color.parseColor("#FFD600"), Color.parseColor("#FFF176")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawPath(bodyPath, paint); paint.shader = null
        drawScales(canvas, bodyPath, bW, bH, Color.argb(28, 150, 100, 0))
        // White peduncle spine mark
        paint.color = Color.argb(180, 255, 255, 255)
        canvas.drawOval(-bW * 0.72f, -s * 0.05f, -bW * 0.58f, s * 0.05f, paint)
        // Dorsal fin
        paint.color = Color.parseColor("#F9A825"); paint.alpha = 200
        path.reset(); path.moveTo(-bW * 0.05f, -bH); path.quadTo(bW * 0.2f, -bH - s * 0.34f, bW * 0.4f, -bH)
        canvas.drawPath(path, paint)
        // Anal fin
        paint.color = Color.parseColor("#FFB300"); paint.alpha = 185
        path.reset(); path.moveTo(-bW * 0.1f, bH); path.quadTo(bW * 0.15f, bH + s * 0.22f, bW * 0.38f, bH)
        canvas.drawPath(path, paint); paint.alpha = 255
        drawEye(canvas, bW * 0.5f, -bH * 0.1f, s * 0.115f, Color.parseColor("#B8860B"))
    }

    // ── Betta ─────────────────────────────────────────────────────────────────

    private fun drawBetta(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.50f; val bH = s * 0.22f
        val bodyPath = fishBodyPath(bW, bH)
        val sway = sin(fish.wobble * 0.8f)
        // Flowing caudal fin
        paint.alpha = 180
        path.reset(); path.moveTo(-bW * 0.75f, 0f)
        path.cubicTo(-bW * 1.0f + sway * 10f, -bH * 2.2f, -bW * 1.6f + sway * 15f, -bH * 2.5f, -bW * 1.9f + sway * 18f, -bH * 1.8f)
        path.cubicTo(-bW * 1.5f, -bH * 1.0f, -bW * 1.1f, -bH * 0.3f, -bW * 0.75f, 0f)
        path.cubicTo(-bW * 1.1f, bH * 0.3f, -bW * 1.5f, bH * 1.0f, -bW * 1.9f + sway * 18f, bH * 1.8f)
        path.cubicTo(-bW * 1.6f + sway * 15f, bH * 2.5f, -bW * 1.0f + sway * 10f, bH * 2.2f, -bW * 0.75f, 0f)
        path.close()
        val tg = LinearGradient(-bW * 0.75f, 0f, -bW * 1.9f, 0f,
            intArrayOf(Color.parseColor("#6A0DAD"), Color.parseColor("#E53935"), Color.argb(100, 229, 57, 53)),
            null, Shader.TileMode.CLAMP)
        paint.shader = tg; canvas.drawPath(path, paint); paint.shader = null; paint.alpha = 255
        // Body
        val bg = LinearGradient(0f, -bH, 0f, bH,
            intArrayOf(Color.parseColor("#4A0080"), Color.parseColor("#6A0DAD"), Color.parseColor("#9C27B0")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawPath(bodyPath, paint); paint.shader = null
        // Iridescent sheen
        canvas.save(); canvas.clipPath(bodyPath)
        val sg = LinearGradient(bW * 0.3f, -bH, bW * 0.6f, bH,
            intArrayOf(Color.argb(80, 0, 200, 255), Color.argb(0, 0, 200, 255)), null, Shader.TileMode.CLAMP)
        paint.shader = sg; canvas.drawOval(bW * 0.1f, -bH, bW * 0.8f, bH, paint); paint.shader = null
        canvas.restore()
        // Long flowing dorsal
        paint.color = Color.parseColor("#9C27B0"); paint.alpha = 185
        path.reset(); path.moveTo(-bW * 0.3f, -bH)
        path.cubicTo(bW * 0.0f + sway * 8f, -bH - s * 0.45f, bW * 0.3f + sway * 6f, -bH - s * 0.3f, bW * 0.42f, -bH)
        canvas.drawPath(path, paint)
        // Pelvic fins (long, drooping)
        paint.color = Color.parseColor("#E53935"); paint.alpha = 170
        path.reset(); path.moveTo(bW * 0.15f, bH)
        path.cubicTo(bW * 0.0f, bH + s * 0.5f, -bW * 0.2f + sway * 10f, bH + s * 0.7f, -bW * 0.3f + sway * 12f, bH + s * 0.6f)
        canvas.drawPath(path, paint); paint.alpha = 255
        drawEye(canvas, bW * 0.5f, -bH * 0.1f, s * 0.10f, Color.parseColor("#7B1FA2"))
    }

    // ── Water surface ─────────────────────────────────────────────────────────

    private fun drawWaterSurface(canvas: Canvas) {
        val grad = LinearGradient(0f, 0f, 0f, 30f,
            intArrayOf(Color.argb(80, 20, 80, 180), Color.argb(0, 20, 80, 180)), null, Shader.TileMode.CLAMP)
        paint.shader = grad; paint.style = Paint.Style.FILL
        path.reset(); path.moveTo(0f, 0f)
        var x = 0f
        while (x <= width) { path.lineTo(x, sin(x * 0.020f + time * 2.0f) * 9f + 18f); x += 6f }
        path.lineTo(width.toFloat(), 0f); path.close()
        canvas.drawPath(path, paint); paint.shader = null
    }

    // ── Physics ───────────────────────────────────────────────────────────────

    private fun updatePhysics() {
        val w = width.toFloat(); val swimTop = surfaceH + 25f; val swimBot = height - sandH - 25f
        fishList.forEach { f ->
            f.wobble += f.wobbleSpeed; f.x += f.speed * f.direction
            if (f.x > w + f.size) f.x = -f.size
            if (f.x < -f.size) f.x = w + f.size
            if (Random.nextFloat() < 0.012f) f.y += (Random.nextFloat() - 0.5f) * 4f
            f.y = f.y.coerceIn(swimTop, swimBot)
            if (Random.nextFloat() < 0.003f) f.direction *= -1
        }
        bubbles.forEach { b ->
            b.wobble += 0.038f; b.y -= b.speed
            if (b.y + b.radius < 0f) { b.y = height + b.radius; b.x = Random.nextFloat() * width }
        }
        seaweeds.forEach { it.phase += 0.016f }
        snow.forEach { p ->
            p[1] += p[3]; p[0] += sin(time * 0.3f + p[1] * 0.02f) * 0.3f
            if (p[1] > height + 5f) { p[1] = -5f; p[0] = Random.nextFloat() * width }
        }
    }
}
