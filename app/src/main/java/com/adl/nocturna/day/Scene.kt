package com.adl.nocturna.day

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import java.time.LocalTime
import java.time.Month
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** One month's colours: the scene behind the clock and the accent of the calendar page. */
class MonthTheme(
    val skyTop: Color,
    val skyBottom: Color,
    /** Far, middle and near hill. */
    val hills: List<Color>,
    val accent: Color,
    val flowers: List<Color> = emptyList(),
    val snow: Boolean = false,
    val trees: Boolean = false,
)

fun monthTheme(month: Month): MonthTheme = THEMES[month.value - 1]

private fun c(argb: Long) = Color(argb)

private val THEMES = listOf(
    // January: fresh snow.
    MonthTheme(c(0xFF5FB4EC), c(0xFFDDF1FF), listOf(c(0xFFD3E5F5), c(0xFFE8F2FB), c(0xFFFFFFFF)), c(0xFF2F7BD8), snow = true),
    // February: pink skies.
    MonthTheme(c(0xFF6EB9EE), c(0xFFFFE1EC), listOf(c(0xFFB6DDC4), c(0xFF8ECFA4), c(0xFF6ABF86)), c(0xFFE8457A), listOf(c(0xFFFF8FB5), c(0xFFFFFFFF))),
    // March: first green.
    MonthTheme(c(0xFF4DB1EE), c(0xFFDDF6FF), listOf(c(0xFFAEE07A), c(0xFF83CC55), c(0xFF5BB23E)), c(0xFF22A563), listOf(c(0xFFFFE066), c(0xFFFFFFFF))),
    // April: blossoms.
    MonthTheme(c(0xFF5CBDF5), c(0xFFFFEEF5), listOf(c(0xFFA3DB80), c(0xFF79C95C), c(0xFF52AF46)), c(0xFFEC5FA0), listOf(c(0xFFFF9EC8), c(0xFFFFD1E3), c(0xFFFFFFFF))),
    // May: wildflowers.
    MonthTheme(c(0xFF38A9EE), c(0xFFD6F2FF), listOf(c(0xFF93DB62), c(0xFF60C449), c(0xFF3AA83B)), c(0xFFFF7F2A), listOf(c(0xFFFFD93D), c(0xFFFF7FAA), c(0xFFB28DFF))),
    // June: early summer.
    MonthTheme(c(0xFF2CA0EE), c(0xFFBDE9FF), listOf(c(0xFF80D95A), c(0xFF4DC04B), c(0xFF2E9E45)), c(0xFFF5A300), listOf(c(0xFFFF6B6B), c(0xFFFFD93D), c(0xFFFFFFFF))),
    // July: golden fields.
    MonthTheme(c(0xFF2697E6), c(0xFFFFE6B5), listOf(c(0xFF8BCF5A), c(0xFFF2CF63), c(0xFF4CAE50)), c(0xFFFF4F5E), listOf(c(0xFFFF4F5E), c(0xFFFFD23F))),
    // August: sunflowers.
    MonthTheme(c(0xFF35A3E6), c(0xFFFFD8A6), listOf(c(0xFF6FBF5A), c(0xFFF4C46C), c(0xFFE6A94E)), c(0xFFFF7417), listOf(c(0xFFFFC21C))),
    // September: late summer.
    MonthTheme(c(0xFF4FA8E3), c(0xFFFFE0C0), listOf(c(0xFFC7D86A), c(0xFF9BC45A), c(0xFF6EA64E)), c(0xFFDE8222), listOf(c(0xFFFFB347), c(0xFFFFFFFF))),
    // October: autumn leaves.
    MonthTheme(c(0xFF62AEE6), c(0xFFFFD4AC), listOf(c(0xFFF2A54C), c(0xFFE07A30), c(0xFFB9582B)), c(0xFFE85A24), listOf(c(0xFFFFD166), c(0xFFC0392B))),
    // November: warm and cosy.
    MonthTheme(c(0xFF5FAAE0), c(0xFFFFE3CA), listOf(c(0xFFF6C27A), c(0xFFEDA25E), c(0xFFDB8550)), c(0xFFD2553B), listOf(c(0xFFFFE08A), c(0xFFFF8C69), c(0xFFFFFFFF))),
    // December: snow and fir trees.
    MonthTheme(c(0xFF5E9FE3), c(0xFFE6F1FF), listOf(c(0xFFD0E1F2), c(0xFFE6EFF9), c(0xFFFFFFFF)), c(0xFFD7343A), snow = true, trees = true),
)

/** Where the sky's gradient ends, behind the hills. */
private const val HORIZON = 0.66f

/** The hills: (base height, wave height, waves across the screen, phase), far to near, as fractions. */
private val HILLS = listOf(
    floatArrayOf(0.64f, 0.035f, 1.3f, 0.6f),
    floatArrayOf(0.74f, 0.045f, 0.9f, 2.4f),
    floatArrayOf(0.85f, 0.035f, 1.6f, 4.1f),
)

private fun hillY(hill: FloatArray, x: Float): Float =
    hill[0] - hill[1] * sin(2 * PI * (hill[2] * x) + hill[3]).toFloat() - hill[1] * 0.4f * sin(2 * PI * (hill[2] * 2.7f * x) + 1f).toFloat()

/**
 * A cheerful, flat illustration for day mode: sky, sun, drifting clouds and rolling hills in [theme]'s
 * colours. [time] turns the sun's rays and moves the clouds along.
 */
@Composable
fun SceneBackground(theme: MonthTheme, time: LocalTime, seed: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(theme.skyTop, theme.skyBottom), endY = h * HORIZON))

        // The sun peeks in from the top corner, out of the clock's way; its rays turn slowly.
        val seconds = time.toSecondOfDay().toFloat()
        val sunR = minOf(w, h) * 0.13f
        val (wx, wy) = sunWobble(seconds)
        val sun = Offset(sunR * (wx - 0.15f), sunR * (wy - 0.15f))
        drawCircle(Brush.radialGradient(listOf(Color(0x88FFF3B0), Color.Transparent), sun, sunR * 2.6f), sunR * 2.6f, sun)
        rotate(seconds * 0.5f % 360f, sun) {
            for (i in 0 until 16) {
                rotate(i * 360f / 16, sun) {
                    drawLine(Color(0xFFFFD43B), Offset(sun.x, sun.y + sunR * 1.25f), Offset(sun.x, sun.y + sunR * 1.6f), sunR * 0.09f, StrokeCap.Round)
                }
            }
        }
        drawCircle(Color(0xFFFFE066), sunR * 1.1f, sun)
        drawCircle(Color(0xFFFFD43B), sunR, sun)

        // Clouds cross the screen in about three hours.
        val cloudW = minOf(w, h) * 0.32f
        listOf(Triple(0.1f, 0.16f, 1f), Triple(0.55f, 0.3f, 0.75f), Triple(0.8f, 0.1f, 0.6f)).forEach { (start, y, scale) ->
            val span = w + cloudW * 2
            val x = ((start * span + seconds * span / 10_800f * scale) % span) - cloudW
            cloud(x, y * h, cloudW * scale)
        }

        if (theme.snow) {
            val rnd = Random(seed)
            repeat(70) {
                val fx = rnd.nextFloat()
                val speed = 0.4f + rnd.nextFloat() * 0.6f
                val fy = (rnd.nextFloat() + seconds / 900f * speed) % 1f
                drawCircle(Color.White, minOf(w, h) * (0.003f + rnd.nextFloat() * 0.004f), Offset(fx * w, fy * h * 0.9f), alpha = 0.85f)
            }
        }

        HILLS.forEachIndexed { i, hill ->
            val path = Path().apply {
                moveTo(0f, h)
                val steps = 64
                for (s in 0..steps) {
                    val fx = s / steps.toFloat()
                    // The hills scroll very slowly, so their edges never sit on the same pixels for long.
                    lineTo(fx * w, hillY(hill, fx + hillShift(seconds, i)) * h)
                }
                lineTo(w, h)
                close()
            }
            drawPath(path, theme.hills[i])
        }

        val rnd = Random(seed * 31 + 7)
        if (theme.trees) {
            // On the near hill, so the page and the note don't hide them; sorted so lower trees draw in front.
            List(14) { rnd.nextFloat() to rnd.nextFloat() }.sortedBy { it.second }.forEach { (start, depth) ->
                val fx = travel(start, seconds)
                val top = nearHillY(fx, seconds)
                tree(fx * w, (top + 0.02f + depth * (1f - top - 0.04f)) * h, minOf(w, h) * (0.07f + depth * 0.07f))
            }
        }
        if (theme.flowers.isNotEmpty()) {
            repeat(55) {
                val fx = travel(rnd.nextFloat(), seconds)
                val top = nearHillY(fx, seconds)
                val fy = top + 0.015f + rnd.nextFloat() * (1f - top - 0.03f)
                // Nearer (lower) flowers are bigger.
                val r = minOf(w, h) * (0.006f + 0.012f * (fy - top) / (1f - top))
                flower(Offset(fx * w, fy * h), r, theme.flowers[rnd.nextInt(theme.flowers.size)])
            }
        }
    }
}

/** Where something on the near hill that started at [start] has travelled to, wrapping around the screen. */
private fun travel(start: Float, seconds: Float): Float = ((start - hillShift(seconds, 2)) % 1f + 1f) % 1f

/** The near hill's top at screen position [x], as it has scrolled by [seconds]. */
private fun nearHillY(x: Float, seconds: Float): Float = hillY(HILLS[2], x + hillShift(seconds, 2))

private fun DrawScope.cloud(x: Float, y: Float, w: Float) {
    val white = Color.White.copy(alpha = 0.92f)
    drawCircle(white, w * 0.2f, Offset(x - w * 0.2f, y))
    drawCircle(white, w * 0.27f, Offset(x + w * 0.02f, y - w * 0.1f))
    drawCircle(white, w * 0.18f, Offset(x + w * 0.26f, y + w * 0.02f))
    drawRoundRect(white, Offset(x - w * 0.42f, y), Size(w * 0.86f, w * 0.2f), CornerRadius(w * 0.1f))
}

private fun DrawScope.flower(center: Offset, r: Float, petals: Color) {
    for (i in 0 until 5) {
        val a = 2 * PI * i / 5
        drawCircle(petals, r * 0.55f, Offset(center.x + r * 0.6f * kotlin.math.cos(a).toFloat(), center.y + r * 0.6f * sin(a).toFloat()))
    }
    drawCircle(Color(0xFFFFB300), r * 0.4f, center)
}

private fun DrawScope.tree(x: Float, base: Float, height: Float) {
    drawRect(Color(0xFF7A5230), Offset(x - height * 0.05f, base - height * 0.15f), Size(height * 0.1f, height * 0.15f))
    for (i in 0 until 3) {
        val top = base - height * (0.35f + 0.25f * i) - height * 0.15f
        val half = height * (0.32f - 0.08f * i)
        val tier = Path().apply {
            moveTo(x, top)
            lineTo(x - half, top + height * 0.4f)
            lineTo(x + half, top + height * 0.4f)
            close()
        }
        drawPath(tier, Color(0xFF2E8B57))
    }
    drawCircle(Color.White, height * 0.06f, Offset(x, base - height * 1.0f))
}
