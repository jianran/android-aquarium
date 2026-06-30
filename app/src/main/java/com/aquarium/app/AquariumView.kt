package com.aquarium.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class AquariumView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    // Creatures
    private val fishList = mutableListOf<Fish>()
    private val bubbles = mutableListOf<Bubble>()
    private val corals = mutableListOf<Coral>()
    private val seaweeds = mutableListOf<Seaweed>()
    private val octopi = mutableListOf<Octopus>()
    private val urchins = mutableListOf<SeaUrchin>()
    private val shells = mutableListOf<Seashell>()
    private val foodPellets = mutableListOf<FoodPellet>()
    private val snow = ArrayList<FloatArray>()

    // Touch / feed state (mirrors reference repo feed_flag)
    private var isTouching = false
    private var touchX = 0f
    private var touchY = 0f
    private var feedHintAlpha = 220f
    private var rippleR = 0f
    private var rippleA = 0f

    // Peach the starfish (on right glass wall)
    private var peachY = 0f
    private var peachDir = 1f

    // Gradients
    private var waterGrad: LinearGradient? = null
    private var sandGrad: LinearGradient? = null

    private var time = 0f
    private val sandH get() = height * 0.13f
    private val surfaceH get() = height * 0.04f

    init { setLayerType(LAYER_TYPE_HARDWARE, null) }

    // ── Init ──────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Finding Nemo Great Barrier Reef palette — warm tropical blue
        waterGrad = LinearGradient(0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                Color.parseColor("#011F40"),
                Color.parseColor("#013A6B"),
                Color.parseColor("#015E9C"),
                Color.parseColor("#0288D1"),
                Color.parseColor("#29B6E8")
            ),
            floatArrayOf(0f, 0.2f, 0.45f, 0.75f, 1f), Shader.TileMode.CLAMP)
        sandGrad = LinearGradient(0f, h - sandH, 0f, h.toFloat(),
            intArrayOf(
                Color.parseColor("#F5E6A3"),
                Color.parseColor("#E8C97A"),
                Color.parseColor("#C8A840")
            ),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        peachY = h * 0.35f
        initFish(w, h); initBubbles(w, h); initCoral(w, h)
        initSeaweed(w, h); initOctopus(w, h); initUrchins(w, h)
        initShells(w, h); initSnow(w, h)
    }

    private fun initFish(w: Int, h: Int) {
        fishList.clear()
        val yTop = surfaceH + 40f; val yBot = h - sandH - 44f
        // Named Nemo characters first, then fill with other types
        val characters = listOf(
            Triple(FishType.CLOWNFISH,    34f, true),   // Nemo — lucky fin
            Triple(FishType.CLOWNFISH,    44f, false),  // Marlin
            Triple(FishType.BLUE_TANG,    46f, false),  // Dory
            Triple(FishType.MOORISH_IDOL, 50f, false),  // Gill
            Triple(FishType.YELLOW_TANG,  40f, false),  // Bubbles
            Triple(FishType.MANDARIN,     28f, false),
            Triple(FishType.PARROTFISH,   52f, false),
            Triple(FishType.LIONFISH,     44f, false),
            Triple(FishType.BETTA,        38f, false),
            Triple(FishType.BLUE_TANG,    36f, false),
            Triple(FishType.CLOWNFISH,    30f, false),
            Triple(FishType.YELLOW_TANG,  44f, false),
            Triple(FishType.MANDARIN,     26f, false),
            Triple(FishType.MOORISH_IDOL, 42f, false)
        )
        characters.forEach { (type, baseSize, lucky) ->
            fishList.add(Fish(
                x = Random.nextFloat() * w,
                y = Random.nextFloat() * (yBot - yTop) + yTop,
                size = baseSize + Random.nextFloat() * 8f,
                speed = 0.6f + Random.nextFloat() * 1.8f,
                type = type,
                direction = if (Random.nextBoolean()) 1 else -1,
                wobbleSpeed = 0.03f + Random.nextFloat() * 0.045f,
                wobble = Random.nextFloat() * 2f * PI.toFloat(),
                luckyFin = lucky
            ))
        }
    }

    private fun initBubbles(w: Int, h: Int) {
        bubbles.clear()
        // Main bubbles
        repeat(28) {
            bubbles.add(Bubble(Random.nextFloat() * w, Random.nextFloat() * h,
                1.5f + Random.nextFloat() * 7f, 0.3f + Random.nextFloat() * 1.1f,
                Random.nextFloat() * 2f * PI.toFloat()))
        }
        // Filter stream at bottom-right (Ring of Fire homage)
        repeat(10) {
            val fx = w * 0.88f + (Random.nextFloat() - 0.5f) * 20f
            bubbles.add(Bubble(fx, h - sandH - Random.nextFloat() * h * 0.4f,
                1f + Random.nextFloat() * 3f, 0.8f + Random.nextFloat() * 0.8f,
                Random.nextFloat() * 2f * PI.toFloat()))
        }
    }

    private fun initCoral(w: Int, h: Int) {
        corals.clear()
        val baseY = h - sandH + 5f
        val branchPalette = listOf(
            0xFF6B6B to 0xFF8E8E, 0xFF8C42 to 0xFFAA70, 0xC77DFF to 0xE0AAFF,
            0xF72585 to 0xFF6BAE, 0xFF4500 to 0xFF6347
        ).map { (a, b) ->
            Pair(Color.rgb((a shr 16) and 0xFF, (a shr 8) and 0xFF, a and 0xFF),
                 Color.rgb((b shr 16) and 0xFF, (b shr 8) and 0xFF, b and 0xFF))
        }
        repeat(7) { i ->
            val c = branchPalette[i % branchPalette.size]
            corals.add(Coral(CoralType.BRANCH,
                w * (i + 0.5f) / 7f + (Random.nextFloat() - 0.5f) * 35f, baseY,
                50f + Random.nextFloat() * 55f, c.first, c.second,
                Random.nextFloat() * 2f * PI.toFloat()))
        }
        val brainPalette = listOf(
            Color.parseColor("#8B6914") to Color.parseColor("#C4A235"),
            Color.parseColor("#5A7A3A") to Color.parseColor("#7AAA55"),
            Color.parseColor("#8B4513") to Color.parseColor("#CD853F")
        )
        repeat(4) { i ->
            val c = brainPalette[i % brainPalette.size]
            corals.add(Coral(CoralType.BRAIN,
                w * (i + 0.5f) / 4f + (Random.nextFloat() - 0.5f) * 25f, baseY,
                28f + Random.nextFloat() * 26f, c.first, c.second))
        }
        repeat(4) { i ->
            val fc = if (i % 2 == 0)
                Pair(Color.parseColor("#9B2D8E"), Color.parseColor("#C44BB8"))
            else Pair(Color.parseColor("#E85D04"), Color.parseColor("#F48C06"))
            corals.add(Coral(CoralType.FAN,
                w * (i * 0.22f + 0.08f), baseY,
                58f + Random.nextFloat() * 48f, fc.first, fc.second,
                Random.nextFloat() * 2f * PI.toFloat()))
        }
        repeat(3) { i ->
            corals.add(Coral(CoralType.ANEMONE,
                w * (i + 0.5f) / 3.8f + (Random.nextFloat() - 0.5f) * 22f, baseY,
                32f + Random.nextFloat() * 18f,
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
                w * (i + 0.5f) / 10f + (Random.nextFloat() - 0.5f) * 14f,
                h - sandH + 8f, 38f + Random.nextFloat() * 78f,
                cols[i % cols.size], Random.nextInt(5, 9),
                Random.nextFloat() * 2f * PI.toFloat()))
        }
    }

    private fun initOctopus(w: Int, h: Int) {
        octopi.clear()
        // Pearl — baby pink octopus, wanders near bottom
        octopi.add(Octopus(
            x = w * 0.3f, y = h - sandH - 55f,
            size = 42f, direction = 1, speed = 0.5f,
            wobble = 0f, cntX = 0, cntY = 0, velX = 1, velY = 1
        ))
    }

    private fun initUrchins(w: Int, h: Int) {
        urchins.clear()
        val sandY = h - sandH
        val configs = listOf(
            Triple(w * 0.2f, Color.parseColor("#4A0080"), Color.parseColor("#9C27B0")),
            Triple(w * 0.55f, Color.parseColor("#1A237E"), Color.parseColor("#3949AB")),
            Triple(w * 0.82f, Color.parseColor("#3E2723"), Color.parseColor("#6D4C41"))
        )
        configs.forEach { (x, body, spine) ->
            urchins.add(SeaUrchin(x, sandY.toFloat(), 20f + Random.nextFloat() * 10f,
                body, spine, direction = if (Random.nextBoolean()) 1 else -1))
        }
    }

    private fun initShells(w: Int, h: Int) {
        shells.clear()
        val sandY = (h - sandH).toFloat()
        val configs = listOf(
            Triple(w * 0.12f, Color.parseColor("#FF8A65"), -15f),
            Triple(w * 0.42f, Color.parseColor("#FFD54F"), 8f),
            Triple(w * 0.70f, Color.parseColor("#CE93D8"), -22f),
            Triple(w * 0.91f, Color.parseColor("#80CBC4"), 12f)
        )
        configs.forEach { (x, c, rot) ->
            shells.add(Seashell(x.toFloat(), sandY, 14f + Random.nextFloat() * 8f, c, rot))
        }
    }

    private fun initSnow(w: Int, h: Int) {
        snow.clear()
        repeat(55) {
            snow.add(floatArrayOf(Random.nextFloat() * w, Random.nextFloat() * h,
                0.6f + Random.nextFloat() * 2f, 0.12f + Random.nextFloat() * 0.35f,
                (25 + Random.nextInt(45)).toFloat()))
        }
    }

    // ── Touch (mirrors reference repo mouse callback + feed_flag) ──

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isTouching = true; touchX = event.x; touchY = event.y
                rippleR = 0f; rippleA = 180f
                feedHintAlpha = 0f
                if (foodPellets.size < 10)
                    foodPellets.add(FoodPellet(event.x, (surfaceH + 10f)))
            }
            MotionEvent.ACTION_MOVE -> {
                touchX = event.x; touchY = event.y
                if (foodPellets.size < 10 && Random.nextFloat() < 0.12f)
                    foodPellets.add(FoodPellet(event.x, surfaceH + 10f))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { isTouching = false }
        }
        return true
    }

    // ── Draw ──────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        time += 0.016f

        drawBackground(canvas)
        drawLightShafts(canvas)
        drawCaustics(canvas)
        drawSand(canvas)
        drawSeashells(canvas)
        drawSeaweed(canvas)
        drawCoral(canvas)
        drawUrchins(canvas)
        drawSnow(canvas)
        drawBubbles(canvas)
        drawFoodPellets(canvas)
        drawOctopi(canvas)
        drawFishAll(canvas)
        drawPeachStarfish(canvas)
        drawGlassFrame(canvas)
        drawWaterSurface(canvas)
        drawFeedHint(canvas)
        drawTouchRipple(canvas)

        updatePhysics()
        postInvalidateOnAnimation()
    }

    // ── Environment ───────────────────────────────────────────────

    private fun drawBackground(canvas: Canvas) {
        paint.shader = waterGrad; paint.alpha = 255; paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
    }

    private fun drawLightShafts(canvas: Canvas) {
        for (i in 0..5) {
            val shift = sin(time * 0.18f + i * 1.4f) * 30f
            val cx = width * (i + 0.5f) / 6f + shift
            val rw = width * 0.09f
            val grad = LinearGradient(cx, 0f, cx, height * 0.65f,
                intArrayOf(Color.argb(28, 200, 255, 150), Color.argb(0, 200, 255, 150)),
                null, Shader.TileMode.CLAMP)
            paint.shader = grad; paint.style = Paint.Style.FILL
            path.reset()
            path.moveTo(cx - rw * 0.1f, 0f); path.lineTo(cx + rw * 0.1f, 0f)
            path.lineTo(cx + rw, height * 0.65f); path.lineTo(cx - rw, height * 0.65f)
            path.close(); canvas.drawPath(path, paint)
        }
        paint.shader = null
    }

    private fun drawCaustics(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(25, 210, 255, 140)
        val cell = 68f; val baseY = height - sandH - 20f
        var gx = 0f
        while (gx < width + cell) {
            for (gy in 0..3) {
                val cx = gx + sin(time * 0.7f + gy * 1.3f + gx * 0.01f) * 16f
                val cy = baseY - gy * 42f + cos(time * 0.5f + gx * 0.01f + gy * 0.9f) * 11f
                val rw = 13f + sin(time * 1.1f + gx * 0.02f + gy) * 6f
                val rh = 7f + cos(time * 0.8f + gy + gx * 0.015f) * 3.5f
                canvas.drawOval(cx - rw, cy - rh, cx + rw, cy + rh, paint)
            }
            gx += cell
        }
    }

    private fun drawSand(canvas: Canvas) {
        val top = height - sandH
        paint.shader = sandGrad; paint.style = Paint.Style.FILL; paint.alpha = 255
        path.reset(); path.moveTo(0f, top)
        var x = 0f
        while (x <= width) { path.lineTo(x, top + sin(x * 0.03f + time * 0.1f) * 5f); x += 8f }
        path.lineTo(width.toFloat(), height.toFloat()); path.lineTo(0f, height.toFloat())
        path.close(); canvas.drawPath(path, paint); paint.shader = null
        // Ripple lines
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.1f
        paint.color = Color.argb(38, 170, 130, 50)
        for (i in 0..8) { val y = top + 9f + i * 9f; canvas.drawLine(6f + i*5f, y, width - 6f - i*5f, y, paint) }
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
        // Pebbles
        val rng = Random(99)
        repeat(22) { i ->
            val px = rng.nextFloat() * width; val py = top + 4f + rng.nextFloat() * 13f
            val pr = 2.5f + rng.nextFloat() * 5.5f
            paint.color = when (i % 4) {
                0 -> Color.parseColor("#8D6E63"); 1 -> Color.parseColor("#A1887F")
                2 -> Color.parseColor("#6D4C41"); else -> Color.parseColor("#BCAAA4")
            }
            canvas.drawOval(px - pr, py - pr * 0.55f, px + pr, py + pr * 0.55f, paint)
        }
    }

    private fun drawSeashells(canvas: Canvas) {
        shells.forEach { sh ->
            canvas.save()
            canvas.translate(sh.x, sh.sandY - sh.size * 0.25f)
            canvas.rotate(sh.rotation)
            // Shell body (spiral approximation)
            paint.color = sh.color; paint.style = Paint.Style.FILL; paint.alpha = 230
            path.reset()
            path.moveTo(0f, 0f)
            path.cubicTo(-sh.size * 0.55f, -sh.size * 0.2f, -sh.size * 0.85f, sh.size * 0.3f, -sh.size * 0.35f, sh.size * 0.5f)
            path.cubicTo(sh.size * 0.05f, sh.size * 0.72f, sh.size * 0.55f, sh.size * 0.42f, sh.size * 0.42f, 0f)
            path.cubicTo(sh.size * 0.32f, -sh.size * 0.4f, sh.size * 0.12f, -sh.size * 0.35f, 0f, 0f)
            path.close(); canvas.drawPath(path, paint)
            // Shell lines
            paint.color = Color.argb(90, 0, 0, 0); paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.9f
            for (sc in 1..4) {
                val f = sc / 4.5f
                path.reset(); path.moveTo(0f, 0f)
                path.cubicTo(-sh.size * 0.55f * f, -sh.size * 0.2f * f,
                    -sh.size * 0.85f * f, sh.size * 0.3f * f, -sh.size * 0.35f * f, sh.size * 0.5f * f)
                canvas.drawPath(path, paint)
            }
            paint.style = Paint.Style.FILL; paint.strokeWidth = 0f; paint.alpha = 255
            canvas.restore()
        }
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
            paint.style = Paint.Style.FILL; paint.alpha = 185
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
            coral.phase += 0.014f
            when (coral.type) {
                CoralType.BRANCH -> drawBranchCoral(canvas, coral)
                CoralType.BRAIN  -> drawBrainCoral(canvas, coral)
                CoralType.FAN    -> drawFanCoral(canvas, coral)
                CoralType.ANEMONE -> drawAnemone(canvas, coral)
            }
        }
    }

    private fun drawBranchCoral(canvas: Canvas, c: Coral) {
        fun branch(cx: Float, cy: Float, len: Float, angle: Float, depth: Int) {
            if (depth == 0 || len < 5f) return
            val ex = cx + cos(angle) * len; val ey = cy - sin(angle) * len
            paint.color = if (depth > 2) c.primaryColor else c.secondaryColor
            paint.style = Paint.Style.STROKE; paint.strokeWidth = depth * 1.7f + 1f; paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(cx, cy, ex, ey, paint)
            val sw = sin(c.phase + depth * 0.6f) * 0.12f
            branch(ex, ey, len * 0.68f, angle + 0.44f + sw, depth - 1)
            branch(ex, ey, len * 0.65f, angle - 0.38f + sw, depth - 1)
            if (depth == 1) {
                paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
                paint.color = c.secondaryColor; canvas.drawCircle(ex, ey, 3.5f, paint)
            }
        }
        branch(c.x, c.baseY, c.size, PI.toFloat() / 2f + sin(c.phase * 0.28f) * 0.07f, 4)
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
    }

    private fun drawBrainCoral(canvas: Canvas, c: Coral) {
        val r = c.size
        val grad = RadialGradient(c.x - r * 0.25f, c.baseY - r * 0.6f, r * 0.4f,
            intArrayOf(c.secondaryColor, c.primaryColor, Color.argb(255, 55, 30, 8)),
            floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP)
        paint.shader = grad; paint.style = Paint.Style.FILL
        canvas.drawOval(c.x - r, c.baseY - r * 1.05f, c.x + r, c.baseY, paint)
        paint.shader = null
        paint.color = Color.argb(75, 0, 0, 0); paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.8f
        for (i in -4..4) {
            val ox = i * r * 0.22f; path.reset()
            path.moveTo(c.x + ox, c.baseY)
            path.cubicTo(c.x + ox - r * 0.1f, c.baseY - r * 0.5f, c.x + ox + r * 0.1f, c.baseY - r * 0.9f, c.x + ox, c.baseY - r * 1.05f)
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
    }

    private fun drawFanCoral(canvas: Canvas, c: Coral) {
        val sway = sin(c.phase) * 7f; val h = c.size; val w = c.size * 0.72f
        paint.color = c.primaryColor; paint.alpha = 155; paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.2f
        for (i in 0..9) {
            val t = i / 9f; val bx = c.x - w / 2 + t * w + sway * sin(t * PI.toFloat()) * 0.5f
            canvas.drawLine(bx, c.baseY, bx + sway * t, c.baseY - h * (0.35f + sin(t * PI.toFloat()) * 0.55f), paint)
        }
        for (j in 1..7) {
            val frac = j / 8f; val y = c.baseY - h * frac; val hw = w * sin(frac * PI.toFloat()) * 0.52f
            canvas.drawLine(c.x - hw + sway * frac, y, c.x + hw + sway * frac, y, paint)
        }
        paint.alpha = 255; paint.strokeWidth = 3.5f; paint.color = c.primaryColor
        canvas.drawLine(c.x, c.baseY, c.x + sway * 0.3f, c.baseY - h * 0.14f, paint)
        paint.alpha = 255; paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
    }

    private fun drawAnemone(canvas: Canvas, c: Coral) {
        paint.color = Color.parseColor("#2ECC71"); paint.alpha = 200
        canvas.drawOval(c.x - c.size * 0.44f, c.baseY - 10f, c.x + c.size * 0.44f, c.baseY, paint)
        for (i in 0 until 12) {
            val angle = (i.toFloat() / 12f) * 2f * PI.toFloat()
            val sway = sin(c.phase + i * 0.7f) * 11f
            val tx = c.x + cos(angle) * c.size * 0.28f; val ty = c.baseY - c.size * 0.82f
            paint.color = if (i % 2 == 0) c.primaryColor else c.secondaryColor
            paint.alpha = 195; paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f; paint.strokeCap = Paint.Cap.ROUND
            path.reset(); path.moveTo(c.x + cos(angle) * 8f, c.baseY - 5f)
            path.quadTo(tx + sway, c.baseY - c.size * 0.42f, tx, ty)
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL; paint.strokeWidth = 0f; paint.alpha = 255
            paint.color = Color.parseColor("#FF6B9D"); canvas.drawCircle(tx, ty, 4f, paint)
        }
        paint.alpha = 255
    }

    private fun drawUrchins(canvas: Canvas) {
        urchins.forEach { ur ->
            canvas.save()
            canvas.translate(ur.x, ur.sandY - ur.size * 0.28f)
            canvas.rotate(ur.spinAngle)
            val spineCount = 26
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.4f; paint.strokeCap = Paint.Cap.ROUND
            paint.color = ur.spineColor
            for (i in 0 until spineCount) {
                val a = (i.toFloat() / spineCount) * 2f * PI.toFloat()
                val inner = ur.size * 0.36f; val outer = ur.size * (0.72f + sin(i * 2.3f) * 0.14f)
                canvas.drawLine(cos(a) * inner, sin(a) * inner, cos(a) * outer, sin(a) * outer, paint)
            }
            paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
            val bg = RadialGradient(0f, -ur.size * 0.1f, ur.size * 0.4f,
                intArrayOf(Color.argb(255, 100, 50, 90), ur.bodyColor, Color.argb(255, 20, 10, 30)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            paint.shader = bg; canvas.drawCircle(0f, 0f, ur.size * 0.36f, paint); paint.shader = null
            paint.color = Color.argb(70, 255, 200, 255)
            for (i in 0..4) {
                val a = i * (2f * PI / 5f).toFloat()
                canvas.drawCircle(cos(a) * ur.size * 0.2f, sin(a) * ur.size * 0.2f, ur.size * 0.055f, paint)
            }
            canvas.restore()
        }
    }

    private fun drawSnow(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        snow.forEach { p ->
            paint.color = Color.argb(p[4].toInt(), 210, 235, 255)
            canvas.drawCircle(p[0], p[1], p[2], paint)
        }
    }

    private fun drawBubbles(canvas: Canvas) {
        bubbles.forEach { b ->
            val bx = b.x + sin(b.wobble) * 5f
            paint.style = Paint.Style.FILL; paint.color = Color.argb(35, 160, 220, 255)
            canvas.drawCircle(bx, b.y, b.radius, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.8f
            paint.color = Color.argb(55, 255, 255, 255); canvas.drawCircle(bx, b.y, b.radius, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(85, 255, 255, 255)
            canvas.drawCircle(bx - b.radius * 0.3f, b.y - b.radius * 0.35f, b.radius * 0.21f, paint)
        }
        paint.strokeWidth = 0f
    }

    private fun drawFoodPellets(canvas: Canvas) {
        foodPellets.forEach { p ->
            val a = p.alpha.toInt().coerceIn(0, 255)
            paint.color = Color.argb(a, 195, 115, 35)
            canvas.drawCircle(p.x, p.y, 6f, paint)
            paint.color = Color.argb((a * 0.7f).toInt(), 240, 180, 75)
            canvas.drawCircle(p.x - 1.5f, p.y - 1.5f, 2.8f, paint)
        }
    }

    private fun drawOctopi(canvas: Canvas) { octopi.forEach { drawOctopus(canvas, it) } }

    private fun drawOctopus(canvas: Canvas, oct: Octopus) {
        val s = oct.size; val bob = sin(oct.wobble * 0.65f) * 3.5f
        canvas.save()
        canvas.translate(oct.x, oct.y + bob)
        if (oct.direction < 0) canvas.scale(-1f, 1f)

        // 8 tentacles hanging below body
        for (i in 0 until 8) {
            val baseAngle = (i - 3.5f) * 0.28f  // spread at bottom of body
            val tentPhase = oct.wobble * 1.1f + i * 0.8f
            val sx = cos(baseAngle) * s * 0.35f; val sy = s * 0.28f
            val wave = sin(tentPhase) * s * 0.22f
            val endX = sx + wave; val endY = sy + s * 0.82f + cos(tentPhase * 0.7f) * s * 0.1f
            paint.color = if (i % 2 == 0) Color.parseColor("#F48FB1") else Color.parseColor("#FF80AB")
            paint.style = Paint.Style.STROKE; paint.strokeWidth = s * 0.09f - i * 0.3f; paint.strokeCap = Paint.Cap.ROUND
            path.reset(); path.moveTo(sx, sy)
            path.cubicTo(sx + wave * 0.4f, sy + s * 0.35f, endX - wave * 0.3f, endY - s * 0.2f, endX, endY)
            canvas.drawPath(path, paint)
            // Curled tip
            paint.strokeWidth = s * 0.045f
            path.reset(); path.moveTo(endX, endY)
            path.quadTo(endX + wave * 0.2f, endY + s * 0.1f,
                endX + cos(tentPhase + 2f) * s * 0.12f, endY + s * 0.05f)
            canvas.drawPath(path, paint)
            // Sucker dots
            paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
            paint.color = Color.argb(120, 255, 120, 170)
            canvas.drawCircle(sx + (endX - sx) * 0.55f + wave * 0.2f,
                sy + (endY - sy) * 0.55f, s * 0.03f, paint)
        }

        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f

        // Body (mantle)
        val bodyGrad = RadialGradient(-s * 0.12f, -s * 0.22f, s * 0.52f,
            intArrayOf(Color.parseColor("#FFB3C6"), Color.parseColor("#FF4081"), Color.parseColor("#E91E8C")),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bodyGrad
        canvas.drawOval(-s * 0.42f, -s * 0.5f, s * 0.42f, s * 0.32f, paint)
        paint.shader = null

        // Pointed mantle top
        paint.color = Color.parseColor("#FF4081")
        path.reset(); path.moveTo(-s * 0.22f, -s * 0.48f)
        path.quadTo(0f, -s * 0.82f, s * 0.22f, -s * 0.48f)
        canvas.drawPath(path, paint)

        // Big cute eyes
        paint.color = Color.WHITE
        canvas.drawCircle(-s * 0.17f, -s * 0.16f, s * 0.14f, paint)
        canvas.drawCircle(s * 0.17f, -s * 0.16f, s * 0.14f, paint)
        paint.color = Color.parseColor("#1A237E")
        canvas.drawCircle(-s * 0.13f, -s * 0.13f, s * 0.078f, paint)
        canvas.drawCircle(s * 0.21f, -s * 0.13f, s * 0.078f, paint)
        paint.color = Color.argb(220, 0, 0, 0)
        canvas.drawCircle(-s * 0.11f, -s * 0.11f, s * 0.042f, paint)
        canvas.drawCircle(s * 0.23f, -s * 0.11f, s * 0.042f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(-s * 0.09f, -s * 0.15f, s * 0.022f, paint)
        canvas.drawCircle(s * 0.25f, -s * 0.15f, s * 0.022f, paint)
        // Blush
        paint.color = Color.argb(70, 255, 80, 140)
        canvas.drawOval(-s * 0.34f, -s * 0.04f, -s * 0.18f, s * 0.06f, paint)
        canvas.drawOval(s * 0.18f, -s * 0.04f, s * 0.34f, s * 0.06f, paint)

        canvas.restore()
    }

    // ── Fish ──────────────────────────────────────────────────────

    private fun drawFishAll(canvas: Canvas) {
        fishList.sortedBy { it.size }.forEach { fish ->
            canvas.save()
            val tilt = sin(fish.wobble * 0.5f) * 5.5f
            val bobY = sin(fish.wobble) * 3.8f
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

    private fun fishBodyPath(bW: Float, bH: Float): Path {
        val p = Path()
        p.moveTo(bW, 0f)
        p.cubicTo(bW * 0.65f, -bH * 0.55f, bW * 0.1f, -bH, -bW * 0.55f, -bH * 0.88f)
        p.cubicTo(-bW * 0.78f, -bH * 0.55f, -bW * 0.82f, -bH * 0.18f, -bW * 0.75f, 0f)
        p.cubicTo(-bW * 0.82f, bH * 0.18f, -bW * 0.78f, bH * 0.55f, -bW * 0.55f, bH * 0.88f)
        p.cubicTo(bW * 0.1f, bH, bW * 0.65f, bH * 0.55f, bW, 0f)
        p.close(); return p
    }

    private fun drawScales(canvas: Canvas, bp: Path, bW: Float, bH: Float, c: Int) {
        canvas.save(); canvas.clipPath(bp)
        paint.color = c; paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.6f
        val sx = bW * 0.13f; val sy = bH * 0.16f
        for (row in -4..4) for (col in -6..5) {
            val ox = col * sx + if (row % 2 == 0) 0f else sx * 0.5f; val oy = row * sy
            path.reset()
            path.addArc(RectF(ox - sx * 0.55f, oy, ox + sx * 0.55f, oy + sy * 0.85f), 180f, 180f)
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f; canvas.restore()
    }

    private fun drawEye(canvas: Canvas, ex: Float, ey: Float, r: Float, iris: Int) {
        paint.color = Color.WHITE; canvas.drawCircle(ex, ey, r, paint)
        paint.color = iris; canvas.drawCircle(ex + r * 0.12f, ey, r * 0.68f, paint)
        paint.color = Color.argb(225, 0, 0, 0); canvas.drawCircle(ex + r * 0.18f, ey, r * 0.38f, paint)
        paint.color = Color.argb(195, 255, 255, 255); canvas.drawCircle(ex - r * 0.04f, ey - r * 0.32f, r * 0.22f, paint)
    }

    private fun drawForkTail(canvas: Canvas, ax: Float, spread: Float, depth: Float, color: Int, wobble: Float) {
        canvas.save(); canvas.translate(ax, 0f); canvas.rotate(sin(wobble * 1.5f) * 18f)
        paint.color = color; paint.alpha = 215
        path.reset(); path.moveTo(0f, 0f)
        path.cubicTo(-depth * 0.5f, -spread * 0.3f, -depth * 0.9f, -spread * 0.7f, -depth, -spread)
        path.cubicTo(-depth * 0.85f, -spread * 0.3f, -depth * 0.6f, 0f, 0f, 0f)
        path.cubicTo(-depth * 0.6f, 0f, -depth * 0.85f, spread * 0.3f, -depth, spread)
        path.cubicTo(-depth * 0.9f, spread * 0.7f, -depth * 0.5f, spread * 0.3f, 0f, 0f)
        path.close(); canvas.drawPath(path, paint)
        paint.alpha = 255; canvas.restore()
    }

    private fun drawClownfish(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.48f; val bH = s * 0.30f
        val bp = fishBodyPath(bW, bH)
        drawForkTail(canvas, -bW * 0.75f, bH * 0.9f, s * 0.32f, Color.parseColor("#FF4500"), fish.wobble)
        val bg = LinearGradient(0f, -bH, 0f, bH,
            intArrayOf(Color.parseColor("#CC3300"), Color.parseColor("#FF5500"), Color.parseColor("#FF8C42")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawPath(bp, paint); paint.shader = null
        canvas.save(); canvas.clipPath(bp)
        paint.color = Color.WHITE
        canvas.drawRect(s * 0.20f, -bH, s * 0.34f, bH, paint)
        canvas.drawRect(-s * 0.07f, -bH, s * 0.08f, bH, paint)
        canvas.drawRect(-s * 0.32f, -bH, -s * 0.22f, bH, paint)
        paint.color = Color.argb(175, 0, 0, 0); paint.style = Paint.Style.STROKE; paint.strokeWidth = s * 0.028f
        for (rx in listOf(s * 0.20f, -s * 0.07f, -s * 0.32f))
            canvas.drawRect(rx, -bH, rx + s * 0.14f, bH, paint)
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f; canvas.restore()
        paint.color = Color.parseColor("#FF4500"); paint.alpha = 195
        path.reset(); path.moveTo(-bW * 0.08f, -bH); path.quadTo(bW * 0.12f, -bH - s * 0.3f, bW * 0.36f, -bH)
        canvas.drawPath(path, paint)
        // Nemo's lucky fin — right pectoral is smaller
        val finScale = if (fish.luckyFin) 0.42f else 1f
        paint.alpha = 155
        path.reset(); path.moveTo(bW * 0.08f, bH * 0.15f)
        path.quadTo(bW * 0.45f * finScale, bH + s * 0.22f * finScale, bW * 0.6f * finScale, bH * 0.1f)
        canvas.drawPath(path, paint); paint.alpha = 255
        drawEye(canvas, bW * 0.5f, -bH * 0.2f, s * 0.11f, Color.parseColor("#CC5500"))
    }

    private fun drawBlueTang(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.46f; val bH = s * 0.38f
        val bp = fishBodyPath(bW, bH)
        drawForkTail(canvas, -bW * 0.75f, bH * 0.85f, s * 0.30f, Color.parseColor("#FFD600"), fish.wobble)
        val bg = LinearGradient(0f, -bH, 0f, bH,
            intArrayOf(Color.parseColor("#0033BB"), Color.parseColor("#1155DD"), Color.parseColor("#2277EE")),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawPath(bp, paint); paint.shader = null
        canvas.save(); canvas.clipPath(bp)
        paint.color = Color.argb(200, 0, 0, 0)
        path.reset()
        path.moveTo(bW * 0.4f, -bH * 0.08f)
        path.cubicTo(bW * 0.0f, -bH * 0.75f, -bW * 0.65f, -bH * 0.6f, -bW * 0.7f, 0f)
        path.cubicTo(-bW * 0.65f, bH * 0.15f, bW * 0.0f, bH * 0.1f, bW * 0.4f, -bH * 0.08f)
        path.close(); canvas.drawPath(path, paint); canvas.restore()
        drawScales(canvas, bp, bW, bH, Color.argb(22, 0, 30, 100))
        paint.color = Color.parseColor("#1144CC"); paint.alpha = 195
        path.reset(); path.moveTo(-bW * 0.1f, -bH); path.quadTo(bW * 0.2f, -bH - s * 0.35f, bW * 0.42f, -bH)
        canvas.drawPath(path, paint)
        paint.color = Color.parseColor("#FFD600"); paint.alpha = 180
        path.reset(); path.moveTo(-bW * 0.15f, bH); path.quadTo(bW * 0.1f, bH + s * 0.22f, bW * 0.35f, bH)
        canvas.drawPath(path, paint); paint.alpha = 255
        drawEye(canvas, bW * 0.5f, -bH * 0.12f, s * 0.115f, Color.parseColor("#2255BB"))
    }

    private fun drawMandarinFish(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.42f; val bH = s * 0.30f
        val bp = fishBodyPath(bW, bH)
        paint.color = Color.parseColor("#FF6B00"); paint.alpha = 205
        path.reset(); path.moveTo(-bW * 0.75f, 0f)
        path.cubicTo(-bW * 1.22f, -bH * 0.88f, -bW * 1.38f, -bH * 0.55f, -bW * 1.32f, 0f)
        path.cubicTo(-bW * 1.38f, bH * 0.55f, -bW * 1.22f, bH * 0.88f, -bW * 0.75f, 0f)
        path.close(); canvas.drawPath(path, paint); paint.alpha = 255
        val bg = LinearGradient(0f, -bH, 0f, bH,
            intArrayOf(Color.parseColor("#003580"), Color.parseColor("#0055BB"), Color.parseColor("#0077CC")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawPath(bp, paint); paint.shader = null
        canvas.save(); canvas.clipPath(bp)
        paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND
        val wcs = listOf(Color.parseColor("#FF6B00"), Color.parseColor("#FF8C00"),
            Color.parseColor("#00CC88"), Color.parseColor("#FF3366"), Color.parseColor("#FFCC00"))
        for (wi in 0 until 6) {
            paint.color = wcs[wi % wcs.size]; paint.strokeWidth = s * 0.052f; paint.alpha = 195
            path.reset(); val yb = -bH + bH * 2f * wi / 5f; path.moveTo(-bW * 0.8f, yb)
            for (xi in -7..6) {
                path.lineTo(xi * bW * 0.22f, yb + sin(xi * 0.9f + wi * 1.2f + time * 0.5f) * bH * 0.27f)
            }
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f; paint.alpha = 255; canvas.restore()
        paint.color = Color.parseColor("#FF6B00"); paint.alpha = 215
        path.reset(); path.moveTo(-bW * 0.15f, -bH)
        for (si in 0..5) {
            val sx = -bW * 0.15f + si * bW * 0.2f
            path.lineTo(sx, -bH - s * (0.14f + sin(si.toFloat()) * 0.07f)); path.lineTo(sx + bW * 0.1f, -bH)
        }
        canvas.drawPath(path, paint); paint.alpha = 255
        drawEye(canvas, bW * 0.5f, -bH * 0.1f, s * 0.10f, Color.parseColor("#FF8800"))
    }

    private fun drawMoorishIdol(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.38f; val bH = s * 0.50f
        val bp = fishBodyPath(bW, bH)
        drawForkTail(canvas, -bW * 0.75f, bH * 0.65f, s * 0.25f, Color.parseColor("#1A1A1A"), fish.wobble)
        paint.color = Color.WHITE; canvas.drawPath(bp, paint)
        canvas.save(); canvas.clipPath(bp)
        paint.color = Color.parseColor("#1A1A1A")
        canvas.drawRect(bW * 0.2f, -bH, bW * 0.52f, bH, paint)
        canvas.drawRect(-bW * 0.6f, -bH, -bW * 0.18f, bH, paint)
        paint.color = Color.parseColor("#FFD600")
        canvas.drawOval(bW * 0.5f, -bH * 0.55f, bW * 1.1f, bH * 0.55f, paint)
        canvas.restore()
        paint.color = Color.parseColor("#1A1A1A"); paint.alpha = 235
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.4f; paint.strokeCap = Paint.Cap.ROUND
        val filSway = sin(time * 0.78f) * 11f
        path.reset(); path.moveTo(bW * 0.1f, -bH)
        path.cubicTo(bW * 0.25f + filSway, -bH - s * 1.35f, bW * 0.0f + filSway, -bH - s * 1.82f,
            -bW * 0.1f + filSway, -bH - s * 2.0f)
        canvas.drawPath(path, paint); paint.style = Paint.Style.FILL; paint.strokeWidth = 0f; paint.alpha = 255
        paint.color = Color.parseColor("#1A1A1A"); paint.alpha = 195
        path.reset(); path.moveTo(bW * 0.1f, -bH); path.lineTo(bW * 0.28f, -bH - s * 0.58f)
        path.lineTo(bW * 0.28f, -bH); path.close(); canvas.drawPath(path, paint); paint.alpha = 255
        drawEye(canvas, bW * 0.62f, -bH * 0.08f, s * 0.105f, Color.parseColor("#8B6914"))
    }

    private fun drawParrotfish(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.54f; val bH = s * 0.28f
        val bp = fishBodyPath(bW, bH)
        paint.color = Color.parseColor("#00BFA5"); paint.alpha = 195
        canvas.drawOval(-bW * 1.15f, -bH * 0.65f, -bW * 0.68f, bH * 0.65f, paint); paint.alpha = 255
        val bg = LinearGradient(0f, -bH, 0f, bH,
            intArrayOf(Color.parseColor("#006064"), Color.parseColor("#00897B"), Color.parseColor("#4DB6AC")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawPath(bp, paint); paint.shader = null
        val fg = RadialGradient(bW * 0.55f, 0f, bW * 0.55f,
            intArrayOf(Color.parseColor("#FF6B9D"), Color.parseColor("#00897B")), null, Shader.TileMode.CLAMP)
        canvas.save(); canvas.clipPath(bp); paint.shader = fg
        canvas.drawOval(bW * 0.0f, -bH, bW * 1.1f, bH, paint); paint.shader = null; canvas.restore()
        drawScales(canvas, bp, bW, bH, Color.argb(28, 0, 80, 70))
        paint.color = Color.parseColor("#F4A460")
        canvas.drawOval(bW * 0.8f, -bH * 0.18f, bW * 1.08f, bH * 0.18f, paint)
        paint.color = Color.argb(145, 80, 40, 0); paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f
        canvas.drawLine(bW * 0.82f, 0f, bW * 1.06f, 0f, paint); paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
        paint.color = Color.parseColor("#FF6B9D"); paint.alpha = 182
        path.reset(); path.moveTo(-bW * 0.1f, -bH); path.quadTo(bW * 0.2f, -bH - s * 0.22f, bW * 0.45f, -bH)
        canvas.drawPath(path, paint); paint.alpha = 255
        drawEye(canvas, bW * 0.58f, -bH * 0.2f, s * 0.10f, Color.parseColor("#006064"))
    }

    private fun drawLionfish(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.46f; val bH = s * 0.34f
        val bp = fishBodyPath(bW, bH)
        val wag = sin(fish.wobble * 0.78f) * 9f
        paint.alpha = 155
        for (side in listOf(-1f, 1f)) {
            for (ray in 0..7) {
                val rA = (ray / 7f) * 130f - 65f + wag * side
                val rL = bW * (0.88f + sin(ray.toFloat()) * 0.13f)
                paint.color = if (ray % 2 == 0) Color.parseColor("#CC2200") else Color.parseColor("#EE4422")
                canvas.save(); canvas.translate(bW * 0.05f, 0f); canvas.rotate(90f + rA * side)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = s * 0.032f; paint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(0f, 0f, 0f, -rL, paint); canvas.restore()
            }
        }
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f; paint.alpha = 255
        drawForkTail(canvas, -bW * 0.75f, bH * 0.75f, s * 0.28f, Color.parseColor("#AA1100"), fish.wobble)
        val bg = LinearGradient(0f, -bH, 0f, bH,
            intArrayOf(Color.parseColor("#8B1A00"), Color.parseColor("#CC2200"), Color.parseColor("#FF4422")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawPath(bp, paint); paint.shader = null
        canvas.save(); canvas.clipPath(bp)
        val sw2 = bW * 0.10f
        for (si in -5..5) {
            val sx = si * sw2 * 2.2f
            if (si % 2 == 0) { paint.color = Color.argb(138, 255, 255, 255); canvas.drawRect(sx, -bH, sx + sw2, bH, paint) }
        }
        canvas.restore()
        paint.color = Color.parseColor("#AA1100"); paint.alpha = 218
        for (si in 0..6) {
            val sx = -bW * 0.25f + si * bW * 0.18f
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.1f
            canvas.drawLine(sx, -bH, sx + sin(si * 0.5f) * 5f, -bH - s * (0.28f + sin(si.toFloat()) * 0.12f), paint)
        }
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f; paint.alpha = 255
        drawEye(canvas, bW * 0.5f, -bH * 0.18f, s * 0.10f, Color.parseColor("#CC2200"))
    }

    private fun drawYellowTang(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.43f; val bH = s * 0.40f
        val bp = fishBodyPath(bW, bH)
        drawForkTail(canvas, -bW * 0.75f, bH * 0.80f, s * 0.28f, Color.parseColor("#FFB300"), fish.wobble)
        val bg = LinearGradient(0f, -bH, 0f, bH,
            intArrayOf(Color.parseColor("#F9A825"), Color.parseColor("#FFD600"), Color.parseColor("#FFF176")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawPath(bp, paint); paint.shader = null
        drawScales(canvas, bp, bW, bH, Color.argb(26, 150, 100, 0))
        paint.color = Color.argb(175, 255, 255, 255)
        canvas.drawOval(-bW * 0.72f, -s * 0.05f, -bW * 0.58f, s * 0.05f, paint)
        paint.color = Color.parseColor("#F9A825"); paint.alpha = 195
        path.reset(); path.moveTo(-bW * 0.05f, -bH); path.quadTo(bW * 0.2f, -bH - s * 0.34f, bW * 0.4f, -bH)
        canvas.drawPath(path, paint)
        paint.color = Color.parseColor("#FFB300"); paint.alpha = 182
        path.reset(); path.moveTo(-bW * 0.1f, bH); path.quadTo(bW * 0.15f, bH + s * 0.22f, bW * 0.38f, bH)
        canvas.drawPath(path, paint); paint.alpha = 255
        drawEye(canvas, bW * 0.5f, -bH * 0.1f, s * 0.115f, Color.parseColor("#B8860B"))
    }

    private fun drawBetta(canvas: Canvas, fish: Fish) {
        val s = fish.size; val bW = s * 0.50f; val bH = s * 0.22f
        val bp = fishBodyPath(bW, bH); val sway = sin(fish.wobble * 0.8f)
        path.reset(); path.moveTo(-bW * 0.75f, 0f)
        path.cubicTo(-bW * 1.0f + sway * 10f, -bH * 2.2f, -bW * 1.6f + sway * 15f, -bH * 2.5f, -bW * 1.9f + sway * 18f, -bH * 1.8f)
        path.cubicTo(-bW * 1.5f, -bH * 1.0f, -bW * 1.1f, -bH * 0.3f, -bW * 0.75f, 0f)
        path.cubicTo(-bW * 1.1f, bH * 0.3f, -bW * 1.5f, bH * 1.0f, -bW * 1.9f + sway * 18f, bH * 1.8f)
        path.cubicTo(-bW * 1.6f + sway * 15f, bH * 2.5f, -bW * 1.0f + sway * 10f, bH * 2.2f, -bW * 0.75f, 0f)
        path.close()
        val tg = LinearGradient(-bW * 0.75f, 0f, -bW * 1.9f, 0f,
            intArrayOf(Color.parseColor("#6A0DAD"), Color.parseColor("#E53935"), Color.argb(95, 229, 57, 53)),
            null, Shader.TileMode.CLAMP)
        paint.shader = tg; paint.alpha = 175; canvas.drawPath(path, paint); paint.shader = null; paint.alpha = 255
        val bg = LinearGradient(0f, -bH, 0f, bH,
            intArrayOf(Color.parseColor("#4A0080"), Color.parseColor("#6A0DAD"), Color.parseColor("#9C27B0")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawPath(bp, paint); paint.shader = null
        canvas.save(); canvas.clipPath(bp)
        val sg = LinearGradient(bW * 0.3f, -bH, bW * 0.6f, bH,
            intArrayOf(Color.argb(75, 0, 200, 255), Color.argb(0, 0, 200, 255)), null, Shader.TileMode.CLAMP)
        paint.shader = sg; canvas.drawOval(bW * 0.1f, -bH, bW * 0.8f, bH, paint); paint.shader = null; canvas.restore()
        paint.color = Color.parseColor("#9C27B0"); paint.alpha = 182
        path.reset(); path.moveTo(-bW * 0.3f, -bH)
        path.cubicTo(bW * 0.0f + sway * 7f, -bH - s * 0.44f, bW * 0.3f + sway * 5f, -bH - s * 0.3f, bW * 0.42f, -bH)
        canvas.drawPath(path, paint)
        paint.color = Color.parseColor("#E53935"); paint.alpha = 165
        path.reset(); path.moveTo(bW * 0.15f, bH)
        path.cubicTo(bW * 0.0f, bH + s * 0.5f, -bW * 0.2f + sway * 10f, bH + s * 0.7f, -bW * 0.3f + sway * 12f, bH + s * 0.6f)
        canvas.drawPath(path, paint); paint.alpha = 255
        drawEye(canvas, bW * 0.5f, -bH * 0.1f, s * 0.10f, Color.parseColor("#7B1FA2"))
    }

    // ── Peach & Glass (Finding Nemo tank) ─────────────────────────

    private fun drawPeachStarfish(canvas: Canvas) {
        val r = 32f; val x = width.toFloat() - 28f
        canvas.save(); canvas.translate(x, peachY)
        canvas.rotate(sin(time * 0.28f) * 4f)
        // Body
        paint.color = Color.argb(200, 255, 140, 100); paint.style = Paint.Style.FILL
        path.reset()
        for (i in 0..4) {
            val oa = i * (2f * PI / 5f).toFloat() - PI.toFloat() / 2f
            val ia = oa + PI.toFloat() / 5f
            val px = cos(oa) * r; val py = sin(oa) * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            path.lineTo(cos(ia) * r * 0.38f, sin(ia) * r * 0.38f)
        }
        path.close(); canvas.drawPath(path, paint)
        // Texture bumps
        paint.color = Color.argb(100, 200, 90, 60)
        for (i in 0..4) {
            val a = i * (2f * PI / 5f).toFloat()
            canvas.drawCircle(cos(a) * r * 0.58f, sin(a) * r * 0.58f, 4.5f, paint)
            canvas.drawCircle(cos(a) * r * 0.28f, sin(a) * r * 0.28f, 2.5f, paint)
        }
        // Eyes (Peach watches the dentist)
        paint.color = Color.argb(200, 30, 20, 0)
        canvas.drawCircle(-5.5f, -5f, 3f, paint); canvas.drawCircle(5.5f, -5f, 3f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(-4.5f, -6f, 1.2f, paint); canvas.drawCircle(6.5f, -6f, 1.2f, paint)
        canvas.restore()
    }

    private fun drawGlassFrame(canvas: Canvas) {
        val fw = 13f
        // Left + right glass edge shimmer
        val lGrad = LinearGradient(0f, 0f, fw * 3.5f, 0f,
            intArrayOf(Color.argb(70, 220, 245, 255), Color.argb(0, 220, 245, 255)), null, Shader.TileMode.CLAMP)
        paint.shader = lGrad; paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, fw * 3.5f, height.toFloat(), paint)
        val rGrad = LinearGradient(width.toFloat(), 0f, width - fw * 3.5f, 0f,
            intArrayOf(Color.argb(70, 220, 245, 255), Color.argb(0, 220, 245, 255)), null, Shader.TileMode.CLAMP)
        paint.shader = rGrad; canvas.drawRect(width - fw * 3.5f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        // Metal frame border
        val frameGrad = LinearGradient(0f, 0f, fw, 0f,
            intArrayOf(Color.argb(220, 90, 110, 140), Color.argb(220, 50, 65, 90)), null, Shader.TileMode.CLAMP)
        paint.shader = frameGrad; paint.style = Paint.Style.STROKE; paint.strokeWidth = fw
        canvas.drawRect(fw / 2, fw / 2, width - fw / 2, height - fw / 2, paint)
        paint.shader = null
        // Inner bright highlight line
        paint.color = Color.argb(55, 220, 245, 255); paint.strokeWidth = 1.5f
        canvas.drawRect(fw + 0.5f, fw + 0.5f, width - fw - 0.5f, height - fw - 0.5f, paint)
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
        // Corner screws
        paint.color = Color.argb(210, 70, 90, 115)
        val corners = listOf(fw * 2f to fw * 2f, (width - fw * 2f) to fw * 2f,
            fw * 2f to (height - fw * 2f), (width - fw * 2f) to (height - fw * 2f))
        corners.forEach { (cx, cy) ->
            canvas.drawCircle(cx, cy, fw * 0.72f, paint)
            paint.color = Color.argb(100, 30, 40, 60); paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.4f
            canvas.drawLine(cx - 3.5f, cy, cx + 3.5f, cy, paint)
            canvas.drawLine(cx, cy - 3.5f, cx, cy + 3.5f, paint)
            paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
            paint.color = Color.argb(210, 70, 90, 115)
        }
        // Filter label bottom-right
        paint.color = Color.argb(140, 80, 100, 130)
        canvas.drawRoundRect(width - fw - 38f, height - fw - 22f,
            width - fw - 2f, height - fw - 4f, 3f, 3f, paint)
        paint.color = Color.argb(90, 200, 220, 255)
        canvas.drawRoundRect(width - fw - 36f, height - fw - 20f,
            width - fw - 20f, height - fw - 12f, 2f, 2f, paint)
    }

    private fun drawWaterSurface(canvas: Canvas) {
        val grad = LinearGradient(0f, 0f, 0f, 28f,
            intArrayOf(Color.argb(75, 20, 100, 200), Color.argb(0, 20, 100, 200)), null, Shader.TileMode.CLAMP)
        paint.shader = grad; paint.style = Paint.Style.FILL
        path.reset(); path.moveTo(0f, 0f)
        var x = 0f
        while (x <= width) { path.lineTo(x, sin(x * 0.020f + time * 2.0f) * 8f + 16f); x += 6f }
        path.lineTo(width.toFloat(), 0f); path.close()
        canvas.drawPath(path, paint); paint.shader = null
    }

    private fun drawFeedHint(canvas: Canvas) {
        if (feedHintAlpha <= 0f) return
        paint.color = Color.argb(feedHintAlpha.toInt().coerceIn(0, 190), 255, 255, 255)
        paint.textSize = (if (width > 900) 28f else 20f)
        paint.textAlign = Paint.Align.CENTER; paint.style = Paint.Style.FILL
        canvas.drawText("Tap to feed the fish!", width / 2f, surfaceH + 44f, paint)
        paint.textAlign = Paint.Align.LEFT
        feedHintAlpha = (feedHintAlpha - 0.6f).coerceAtLeast(0f)
    }

    private fun drawTouchRipple(canvas: Canvas) {
        if (rippleA <= 0f) return
        paint.color = Color.argb(rippleA.toInt().coerceIn(0, 180), 255, 255, 255)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
        canvas.drawCircle(touchX, touchY, rippleR, paint)
        rippleR += 3.5f; rippleA -= 5f
        paint.style = Paint.Style.FILL; paint.strokeWidth = 0f
    }

    // ── Physics (mirrors reference repo idle callback) ─────────────

    private fun updatePhysics() {
        val w = width.toFloat()
        val swimTop = surfaceH + 28f; val swimBot = height - sandH - 28f
        val activePellets = foodPellets.filter { !it.resting }

        // Fish AI — feed flag mirrors reference repo: when isTouching swim to cursor
        fishList.forEach { fish ->
            fish.wobble += fish.wobbleSpeed
            if (isTouching || activePellets.isNotEmpty()) {
                // Feeding mode: swim toward touch or nearest pellet
                val (tx, ty) = if (isTouching) Pair(touchX, touchY)
                else { val p = activePellets.minByOrNull { p ->
                    val dx = p.x - fish.x; val dy = p.y - fish.y; dx*dx + dy*dy }
                    if (p != null) Pair(p.x, p.y) else Pair(fish.x, fish.y) }
                val dx = tx - fish.x; val dy = ty - fish.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > 8f && dist < 320f) {
                    fish.direction = if (dx >= 0f) 1 else -1
                    fish.x += (dx / dist) * fish.speed * 2.2f
                    fish.y += (dy / dist) * fish.speed * 1.5f
                    activePellets.filter { p ->
                        val pdx = p.x - fish.x; val pdy = p.y - fish.y
                        pdx*pdx + pdy*pdy < 28f * 28f }.forEach { it.alpha = 0f }
                }
            } else {
                // Normal random wandering (reference repo idle random counter logic)
                fish.x += fish.speed * fish.direction
                if (Random.nextFloat() < 0.012f) fish.y += (Random.nextFloat() - 0.5f) * 4f
                if (Random.nextFloat() < 0.003f) fish.direction *= -1
                if (fish.x > w + fish.size) fish.x = -fish.size
                if (fish.x < -fish.size) fish.x = w + fish.size
            }
            fish.y = fish.y.coerceIn(swimTop, swimBot)
        }

        // Octopus AI — moves to cursor when feeding (reference repo octopus feed_flag)
        octopi.forEach { oct ->
            oct.wobble += 0.038f
            if (isTouching) {
                val dx = touchX - oct.x; val dy = touchY - oct.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > 6f) {
                    oct.direction = if (dx >= 0f) 1 else -1
                    oct.x += (dx / dist) * oct.speed * 1.85f
                    oct.y += (dy / dist) * oct.speed * 1.6f
                }
            } else {
                // Random wandering with directional counters (mirrors reference)
                oct.cntX += 1
                if (oct.cntX > 180 + Random.nextInt(120)) { oct.velX *= -1; oct.cntX = 0 }
                oct.cntY += 1
                if (oct.cntY > 200 + Random.nextInt(100)) { oct.velY *= -1; oct.cntY = 0 }
                oct.x += oct.speed * oct.velX; oct.y += oct.speed * oct.velY * 0.4f
                oct.direction = oct.velX
            }
            oct.x = oct.x.coerceIn(oct.size, w - oct.size)
            oct.y = oct.y.coerceIn(swimTop + 20f, height - sandH - oct.size)
        }

        // Urchins — walk on spines (reference repo rotate_angle walking)
        urchins.forEach { ur ->
            ur.spinAngle += ur.speed * ur.direction * 3.5f  // rotation = walking on spines
            ur.cntX += 1
            if (ur.cntX > 300 + Random.nextInt(200)) { ur.direction *= -1; ur.cntX = 0 }
            ur.x += ur.speed * ur.direction
            ur.x = ur.x.coerceIn(ur.size * 1.2f, w - ur.size * 1.2f)
        }

        // Bubbles
        bubbles.forEach { b ->
            b.wobble += 0.036f; b.y -= b.speed
            if (b.y + b.radius < 0f) { b.y = height + b.radius; b.x = Random.nextFloat() * width }
        }

        // Food pellets — sink with gravity, fish eat them
        foodPellets.removeAll { it.alpha <= 0f }
        foodPellets.forEach { p ->
            if (!p.resting) {
                p.y += p.vy; p.vy = (p.vy + 0.04f).coerceAtMost(3.5f)
                p.x += sin(time * 1.2f + p.y * 0.03f) * 0.4f  // gentle flutter
                if (p.y >= height - sandH - 12f) { p.resting = true; p.y = height - sandH - 12f }
            } else {
                p.alpha -= 2.5f
            }
        }

        // Peach starfish slides slowly up/down right glass
        peachY += peachDir * 0.18f
        if (peachY > height * 0.75f) peachDir = -1f
        if (peachY < height * 0.18f) peachDir = 1f

        // Marine snow
        snow.forEach { p ->
            p[1] += p[3]; p[0] += sin(time * 0.28f + p[1] * 0.02f) * 0.28f
            if (p[1] > height + 5f) { p[1] = -5f; p[0] = Random.nextFloat() * width }
        }
    }
}
