package com.github.luckywheel.ui.effects

import com.github.luckywheel.ui.themes.DecorationKind
import com.github.luckywheel.ui.themes.WinEffectKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class ParticleType { BEAN, SNOW, PETAL, LANTERN, HEART, BAT, CONFETTI, FLAKE, STAR, ENVELOPE, SPARK, CANDY }

/** One moving shape. Positions, sizes and speeds are in pixels; times in seconds. */
class Particle(
    val type: ParticleType,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Float,
    val color: Int = 0,
    val alpha: Float = 1f,
    var rotation: Float = 0f,
    val spin: Float = 0f,
    var phase: Float = 0f,
    val gravity: Float = 0f,
    val drag: Float = 1f,
    var life: Float = Float.MAX_VALUE,
    var age: Float = 0f,
    val star: Boolean = false,
)

private const val TAU = (2 * PI).toFloat()
private val PETAL_COLORS = intArrayOf(0xFFFFD54F.toInt(), 0xFFF8A5C2.toInt())
private val HEART_COLORS = intArrayOf(0xFFEC407A.toInt(), 0xFFF48FB1.toInt(), 0xFFD81B60.toInt(), 0xFFFFFFFF.toInt())

/**
 * Idle decorations drifting across the customer screen (TH-03, §6), ported from the prototype. [unit] is
 * pixels per design pixel, so the 1280×800 tuning carries over to any screen.
 */
class DecorationSystem(
    val kind: DecorationKind,
    private var width: Float,
    private var height: Float,
    private val unit: Float,
    private val random: Random = Random.Default,
) {
    val particles: MutableList<Particle> = MutableList(countFor(kind)) { spawn(initial = true) }

    /** Twinkling stars for the snow theme: x, y, size, phase. */
    val stars: List<FloatArray> = if (kind == DecorationKind.SNOW) {
        List(14) { floatArrayOf(r() * width, r() * height * 0.475f, (3 + r() * 4) * unit, r() * TAU) }
    } else {
        emptyList()
    }

    fun resize(w: Float, h: Float) {
        width = w
        height = h
    }

    fun update(dt: Float) {
        for (i in particles.indices) {
            val p = particles[i]
            p.phase += dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rotation += p.spin * dt
            val off = p.y < -90 * unit || p.y > height + 100 * unit || p.x > width + 80 * unit || p.x < -90 * unit
            if (off) particles[i] = spawn(initial = false)
        }
    }

    private fun r() = random.nextFloat()

    private fun spawn(initial: Boolean): Particle {
        val u = unit
        return when (kind) {
            DecorationKind.BEANS -> Particle(
                ParticleType.BEAN, r() * width, if (initial) r() * height else height + 30 * u,
                vx = (r() - 0.5f) * 10 * u, vy = -(8 + r() * 14) * u, size = (7 + r() * 7) * u,
                alpha = 0.16f + r() * 0.2f, rotation = r() * TAU, spin = (r() - 0.5f) * 0.6f,
            )
            DecorationKind.SNOW -> Particle(
                ParticleType.SNOW, r() * width, if (initial) r() * height else -10 * u,
                vx = 0f, vy = (25 + r() * 45) * u, size = (1.2f + r() * 3.2f) * u, alpha = 0.5f + r() * 0.5f, phase = r() * TAU,
            )
            DecorationKind.PETALS -> Particle(
                ParticleType.PETAL, r() * width, if (initial) r() * height else -20 * u,
                vx = (10 + r() * 20) * u, vy = (30 + r() * 35) * u, size = (6 + r() * 6) * u,
                color = PETAL_COLORS[if (r() < 0.5f) 0 else 1], alpha = 0.85f,
                rotation = r() * TAU, spin = (r() - 0.5f) * 1.5f, phase = r() * TAU,
            )
            DecorationKind.LANTERNS -> Particle(
                ParticleType.LANTERN, r() * width, if (initial) r() * height else height + 60 * u,
                vx = (r() - 0.5f) * 6 * u, vy = -(12 + r() * 16) * u, size = (12 + r() * 13) * u,
                alpha = 0.9f, phase = r() * TAU, star = r() < 0.35f,
            )
            DecorationKind.HEARTS -> Particle(
                ParticleType.HEART, r() * width, if (initial) r() * height else height + 30 * u,
                vx = (r() - 0.5f) * 10 * u, vy = -(15 + r() * 25) * u, size = (8 + r() * 12) * u,
                color = HEART_COLORS[random.nextInt(HEART_COLORS.size)], alpha = 0.35f + r() * 0.4f, phase = r() * TAU,
            )
            DecorationKind.BATS -> Particle(
                ParticleType.BAT, if (initial) r() * width else -60 * u, height * (0.075f + r() * 0.5f),
                vx = (40 + r() * 60) * u, vy = 0f, size = (14 + r() * 14) * u, color = 0xFF07030C.toInt(), phase = r() * TAU,
            )
        }
    }

    private companion object {
        fun countFor(kind: DecorationKind) = when (kind) {
            DecorationKind.BEANS -> 16
            DecorationKind.SNOW -> 60
            DecorationKind.PETALS -> 32
            DecorationKind.LANTERNS -> 14
            DecorationKind.HEARTS -> 22
            DecorationKind.BATS -> 7
        }
    }
}

/**
 * The burst shown with a result (RS-02, §6): theme effects for a real prize, a short soft glow for
 * "better luck next time". Every effect is over within [MAX_SECONDS].
 */
class WinEffectSystem private constructor(
    private val width: Float,
    private val height: Float,
    private val unit: Float,
    private val random: Random,
) {
    val particles = ArrayList<Particle>(240)
    private val scheduled = ArrayList<Pair<Float, () -> Unit>>()
    private var time = 0f

    val isFinished: Boolean get() = particles.isEmpty() && scheduled.isEmpty()

    fun update(dt: Float) {
        time += dt
        if (scheduled.isNotEmpty()) {
            val due = scheduled.filter { it.first <= time }
            scheduled.removeAll(due)
            due.forEach { it.second() }
        }
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.vx *= p.drag
            p.vy = p.vy * p.drag + p.gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rotation += p.spin * dt
            p.life -= dt
            p.age += dt
            if (p.life <= 0) it.remove()
        }
    }

    private fun r() = random.nextFloat()

    private fun add(p: Particle) {
        // A little under the limit so frame-time rounding can't stretch the effect past it.
        p.life = min(p.life, MAX_SECONDS - 0.05f - time)
        particles += p
    }

    private val centre get() = Pair(width / 2, height * 0.475f)

    private fun burst(
        n: Int, type: ParticleType, speedMin: Float, speedMax: Float, gravity: Float, life: Float, size: Float,
        colors: IntArray, cx: Float = centre.first, cy: Float = centre.second,
    ) {
        repeat(n) { i ->
            val angle = r() * TAU
            val speed = (speedMin + r() * (speedMax - speedMin)) * unit
            add(
                Particle(
                    type, cx, cy, cos(angle) * speed, sin(angle) * speed - speed * 0.35f,
                    size = size * (0.6f + r() * 0.8f) * unit, color = colors[i % colors.size],
                    rotation = r() * TAU, spin = (r() - 0.5f) * 6, phase = r() * TAU,
                    gravity = gravity * unit, drag = 0.985f, life = life * (0.7f + r() * 0.5f),
                ),
            )
        }
    }

    private fun rain(n: Int, type: ParticleType, size: Float, colors: IntArray) {
        repeat(n) { i ->
            add(
                Particle(
                    type, r() * width, (-40 - r() * 500) * unit, (r() - 0.5f) * 60 * unit, (140 + r() * 200) * unit,
                    size = size * (0.7f + r() * 0.6f) * unit, color = colors[i % colors.size],
                    rotation = r() * TAU, spin = (r() - 0.5f) * 6, phase = r() * TAU, gravity = 40 * unit, life = 4f,
                ),
            )
        }
    }

    private fun spawnWin(kind: WinEffectKind, palette: List<Int>) {
        when (kind) {
            WinEffectKind.CONFETTI -> burst(150, ParticleType.CONFETTI, 300f, 1000f, 700f, 3.4f, 12f, palette.toIntArray())
            WinEffectKind.SNOW_STAR -> {
                burst(60, ParticleType.FLAKE, 200f, 750f, 220f, 3.2f, 12f, intArrayOf(0xFFFFFFFF.toInt()))
                burst(40, ParticleType.STAR, 200f, 850f, 220f, 3.2f, 11f, intArrayOf(0xFFFFE082.toInt(), 0xFFF9A825.toInt(), 0xFFFFFFFF.toInt()))
            }
            WinEffectKind.LIXI -> {
                rain(30, ParticleType.ENVELOPE, 20f, intArrayOf(0xFFD32F2F.toInt()))
                val sparks = intArrayOf(0xFFFFE082.toInt(), 0xFFFBC02D.toInt(), 0xFFFF8F00.toInt(), 0xFFFFFFFF.toInt())
                listOf(0.1f, 0.8f, 1.5f, 2.1f).forEach { delay ->
                    scheduled += delay to {
                        burst(46, ParticleType.SPARK, 120f, 340f, 140f, 1.4f, 6f, sparks,
                            cx = width * (0.156f + r() * 0.6875f), cy = height * (0.15f + r() * 0.325f))
                    }
                }
            }
            WinEffectKind.LANTERNS -> {
                repeat(22) {
                    add(
                        Particle(
                            ParticleType.LANTERN, r() * width, height + (20 + r() * 300) * unit,
                            (r() - 0.5f) * 30 * unit, -(260 + r() * 220) * unit, size = (16 + r() * 14) * unit,
                            phase = r() * TAU, life = 3.6f, star = r() < 0.35f,
                        ),
                    )
                }
                burst(70, ParticleType.SPARK, 150f, 600f, 120f, 2.6f, 6f, intArrayOf(0xFFFFD54F.toInt(), 0xFFFFF8E1.toInt(), 0xFFFFB300.toInt()))
            }
            WinEffectKind.HEARTS -> {
                burst(70, ParticleType.HEART, 250f, 850f, 380f, 3.2f, 14f,
                    intArrayOf(0xFFEC407A.toInt(), 0xFFF48FB1.toInt(), 0xFFFFFFFF.toInt(), 0xFFD81B60.toInt()))
                burst(36, ParticleType.PETAL, 200f, 700f, 300f, 3.2f, 10f, intArrayOf(0xFFF8BBD0.toInt(), 0xFFFFFFFF.toInt()))
            }
            WinEffectKind.BATS -> {
                burst(18, ParticleType.BAT, 320f, 760f, 0f, 2.6f, 24f, intArrayOf(0xFF120818.toInt()))
                rain(46, ParticleType.CANDY, 12f,
                    intArrayOf(0xFFFF6F00.toInt(), 0xFF9CCC65.toInt(), 0xFFAB47BC.toInt(), 0xFFFFA726.toInt()))
            }
        }
    }

    private fun spawnLose(glow: Int) {
        repeat(28) {
            add(
                Particle(
                    ParticleType.SPARK, width / 2 + (r() - 0.5f) * 520 * unit, height * (0.65f + r() * 0.2f),
                    (r() - 0.5f) * 30 * unit, -(50 + r() * 70) * unit, size = (5 + r() * 4) * unit,
                    color = glow, life = 1.8f + r() * 0.6f,
                ),
            )
        }
    }

    companion object {
        const val MAX_SECONDS = 4f

        fun win(kind: WinEffectKind, width: Float, height: Float, unit: Float, palette: List<Int>, random: Random = Random.Default) =
            WinEffectSystem(width, height, unit, random).apply { spawnWin(kind, palette) }

        fun lose(width: Float, height: Float, unit: Float, glow: Int, random: Random = Random.Default) =
            WinEffectSystem(width, height, unit, random).apply { spawnLose(glow) }
    }
}
