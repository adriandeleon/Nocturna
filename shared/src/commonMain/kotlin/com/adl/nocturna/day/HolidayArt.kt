package com.adl.nocturna.day

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/*
 * The holidays' decorations over day mode's background, and a meteor shower's shooting stars, drawn by
 * DayOverlay. [t] runs from 0 to OVERLAY_LOOP_SECONDS and starts again, so the trips here take a whole
 * fraction of that minute and come back to where they started. Everything keeps moving, so nothing stays
 * on the same pixels.
 */

private val Red = Color(0xFFFF4F5E)
private val Yellow = Color(0xFFFFC21C)
private val Green = Color(0xFF2FBF71)
private val Blue = Color(0xFF3D8BFF)
private val Purple = Color(0xFFB06BFF)
private val Orange = Color(0xFFFF8A3D)
private val Gold = Color(0xFFFFC83D)

private val FireworkColors = listOf(Red, Yellow, Green, Blue, Purple, Orange, Color.White)

/** Rockets climbing from below the screen and bursting into rings of sparks that droop as they fade. */
internal fun DrawScope.fireworks(t: Float) {
    val periods = floatArrayOf(4f, 5f, 6f, 3f, 5f)
    val dot = size.minDimension * 0.0045f
    for (i in periods.indices) {
        val period = periods[i]
        val shifted = t + i * 1.7f
        val round = (shifted / period).toInt()
        val u = (shifted % period) / period
        val rnd = Random(i * 7919 + round)
        val x = size.width * (0.12f + rnd.nextFloat() * 0.76f)
        val burstY = size.height * (0.12f + rnd.nextFloat() * 0.3f)
        val color = FireworkColors[rnd.nextInt(FireworkColors.size)]
        val reach = size.minDimension * (0.12f + rnd.nextFloat() * 0.08f)
        if (u < 0.3f) {
            // The rocket climbs, slowing near the top, with a short trail.
            val k = u / 0.3f
            val y = size.height - (size.height - burstY) * (1 - (1 - k) * (1 - k))
            drawLine(color.copy(alpha = 0.5f), Offset(x, y + reach * 0.15f), Offset(x, y), dot, StrokeCap.Round)
            drawCircle(Color.White, dot, Offset(x, y))
        } else {
            val k = (u - 0.3f) / 0.7f
            val spread = reach * (1 - (1 - k) * (1 - k) * (1 - k))
            val fade = 1 - k
            val droop = reach * 0.25f * k * k
            for (s in 0 until 28) {
                val a = 2 * PI * s / 28
                val spark = Offset(x + spread * cos(a).toFloat(), burstY + spread * sin(a).toFloat() + droop)
                val tail = Offset(x + spread * 0.82f * cos(a).toFloat(), burstY + spread * 0.82f * sin(a).toFloat() + droop)
                drawLine(color, tail, spark, dot * 0.45f, alpha = fade * 0.6f)
                drawCircle(color, dot, spark, alpha = fade)
            }
            if (k < 0.15f) drawCircle(Color.White, reach * 0.25f * (1 - k / 0.15f), Offset(x, burstY), alpha = 0.5f)
        }
    }
}

/** Star piñatas hanging from the top on strings, swinging, and drifting along. */
internal fun DrawScope.pinatas(t: Float) {
    val colors = listOf(Red, Yellow, Green, Blue, Purple)
    val r = size.minDimension * 0.04f
    for (i in 0 until 3) {
        val fx = (i / 3f + t / OVERLAY_LOOP_SECONDS) % 1f
        val anchor = Offset(fx * (size.width + r * 6) - r * 3, 0f)
        val c = Offset(anchor.x, size.height * (0.13f + 0.05f * i))
        rotate(sin(t * 1.2f + i * 2f) * 10f, anchor) {
            drawLine(Color.White.copy(alpha = 0.7f), anchor, Offset(c.x, c.y - r), 2f)
            // Seven cones around a round body, each with a tassel at its tip.
            for (k in 0 until 7) {
                val a = 2 * PI * k / 7 - PI / 2
                val tip = Offset(c.x + r * 2.1f * cos(a).toFloat(), c.y + r * 2.1f * sin(a).toFloat())
                val left = Offset(c.x + r * 0.8f * cos(a - 0.35).toFloat(), c.y + r * 0.8f * sin(a - 0.35).toFloat())
                val right = Offset(c.x + r * 0.8f * cos(a + 0.35).toFloat(), c.y + r * 0.8f * sin(a + 0.35).toFloat())
                drawPath(Path().apply { moveTo(left.x, left.y); lineTo(tip.x, tip.y); lineTo(right.x, right.y); close() }, colors[(k + i) % colors.size])
                drawLine(colors[(k + i + 2) % colors.size], tip, Offset(tip.x, tip.y + r * 0.6f), r * 0.12f, StrokeCap.Round)
            }
            drawCircle(Color(0xFFFFE08A), r, c)
            drawCircle(colors[i % colors.size], r * 0.55f, c)
        }
    }
}

/** Paper crowns for the Three Kings, tumbling down slowly. */
internal fun DrawScope.crowns(t: Float) {
    val rnd = Random(61)
    repeat(12) {
        val w = size.minDimension * (0.035f + rnd.nextFloat() * 0.02f)
        val x0 = rnd.nextFloat()
        val phase = rnd.nextFloat()
        val y = ((phase + t / 30f) % 1f) * (size.height + w * 2) - w
        val x = x0 * size.width + sin(t * 0.6f + phase * 9f) * w
        rotate(sin(t * 0.8f + phase * 5f) * 15f, Offset(x, y)) { crown(Offset(x, y), w) }
    }
}

private fun DrawScope.crown(c: Offset, w: Float) {
    val h = w * 0.7f
    val path = Path().apply {
        moveTo(c.x - w / 2, c.y + h / 2)
        lineTo(c.x - w / 2, c.y - h / 2)
        lineTo(c.x - w / 4, c.y)
        lineTo(c.x, c.y - h / 2)
        lineTo(c.x + w / 4, c.y)
        lineTo(c.x + w / 2, c.y - h / 2)
        lineTo(c.x + w / 2, c.y + h / 2)
        close()
    }
    drawPath(path, Gold, alpha = 0.95f)
    drawRect(Color(0xFFE0A21B), Offset(c.x - w / 2, c.y + h * 0.25f), Size(w, h * 0.25f))
    drawCircle(Color(0xFFE53950), w * 0.07f, Offset(c.x, c.y + h * 0.37f))
    drawCircle(Green, w * 0.06f, Offset(c.x - w * 0.3f, c.y + h * 0.37f))
    drawCircle(Blue, w * 0.06f, Offset(c.x + w * 0.3f, c.y + h * 0.37f))
    for (px in listOf(-0.5f, 0f, 0.5f)) drawCircle(Color(0xFFFFE9A8), w * 0.05f, Offset(c.x + w * px, c.y - h / 2))
}

/** Balloons rising from the bottom on wavy strings. */
internal fun DrawScope.balloons(t: Float) {
    val colors = listOf(Red, Yellow, Green, Blue, Purple, Orange)
    val rnd = Random(71)
    repeat(12) {
        val r = size.minDimension * (0.03f + rnd.nextFloat() * 0.015f)
        val x0 = rnd.nextFloat()
        val phase = rnd.nextFloat()
        val color = colors[rnd.nextInt(colors.size)]
        val y = size.height + r * 5 - ((phase + t / 20f) % 1f) * (size.height + r * 8)
        val x = x0 * size.width + sin(t * 0.7f + phase * 8f) * r
        val knot = y + r * 1.2f
        val string = Path().apply {
            moveTo(x, knot)
            for (k in 1..6) lineTo(x + (if (k % 2 == 0) -1 else 1) * r * 0.12f, knot + k * r * 0.4f)
        }
        drawPath(string, Color.White.copy(alpha = 0.7f), style = Stroke(1.5f))
        drawPath(Path().apply { moveTo(x, knot - r * 0.1f); lineTo(x - r * 0.12f, knot + r * 0.1f); lineTo(x + r * 0.12f, knot + r * 0.1f); close() }, color)
        drawOval(color, Offset(x - r, y - r * 1.2f), Size(r * 2, r * 2.4f), alpha = 0.9f)
        drawOval(Color.White.copy(alpha = 0.35f), Offset(x - r * 0.6f, y - r * 0.9f), Size(r * 0.45f, r * 0.7f))
    }
}

/** Rose petals drifting down and turning. */
internal fun DrawScope.petals(t: Float) {
    val colors = listOf(Color(0xFFFF6F91), Color(0xFFFFA8C5), Color(0xFFE8457A), Color(0xFFFFD1E3))
    val rnd = Random(83)
    repeat(34) {
        val l = size.minDimension * (0.012f + rnd.nextFloat() * 0.01f)
        val x0 = rnd.nextFloat()
        val phase = rnd.nextFloat()
        val speed = if (rnd.nextBoolean()) 1f else 2f
        val y = ((phase + t / 30f * speed) % 1f) * (size.height + l * 4) - l * 2
        val x = x0 * size.width + sin(t * 0.9f + phase * 10f) * l * 3
        rotate(t * 36f * speed + phase * 360f, Offset(x, y)) {
            drawOval(colors[it % colors.size], Offset(x - l / 2, y - l * 0.3f), Size(l, l * 0.6f), alpha = 0.85f)
        }
    }
}

/** Kites bobbing in the wind with bowed tails, their lines running down off the screen. */
internal fun DrawScope.kites(t: Float) {
    val colors = listOf(Red, Yellow, Blue)
    for (i in 0 until 3) {
        val s = size.minDimension * (0.06f - i * 0.008f)
        val fx = (i / 3f + t / OVERLAY_LOOP_SECONDS) % 1f
        val x = fx * (size.width + s * 6) - s * 3
        val y = size.height * (0.14f + 0.08f * i) + sin(t * 0.8f + i * 2f) * s * 0.4f
        drawLine(Color.White.copy(alpha = 0.6f), Offset(x, y + s), Offset(x - s * 3, size.height), 1.5f)
        rotate(sin(t * 0.6f + i) * 12f, Offset(x, y)) {
            val top = Offset(x, y - s)
            val bottom = Offset(x, y + s)
            val left = Offset(x - s * 0.7f, y - s * 0.2f)
            val right = Offset(x + s * 0.7f, y - s * 0.2f)
            drawPath(Path().apply { moveTo(top.x, top.y); lineTo(right.x, right.y); lineTo(bottom.x, bottom.y); lineTo(left.x, left.y); close() }, colors[i])
            drawPath(Path().apply { moveTo(top.x, top.y); lineTo(right.x, right.y); lineTo(x, y - s * 0.2f); close() }, Color.White.copy(alpha = 0.3f))
            drawLine(Color(0xFF6B4F2E), top, bottom, 2f)
            drawLine(Color(0xFF6B4F2E), left, right, 2f)
            for (k in 1..5) {
                val tail = Offset(bottom.x + sin(t * 3f + k * 0.9f + i) * s * 0.25f, bottom.y + k * s * 0.45f)
                drawCircle(colors[(i + k) % colors.size], s * 0.09f, tail)
            }
        }
    }
}

/** Shamrocks floating down, rocking as they go. */
internal fun DrawScope.shamrocks(t: Float) {
    val greens = listOf(Color(0xFF169B62), Green, Color(0xFF7BD389))
    val rnd = Random(97)
    repeat(16) {
        val r = size.minDimension * (0.014f + rnd.nextFloat() * 0.01f)
        val x0 = rnd.nextFloat()
        val phase = rnd.nextFloat()
        val y = ((phase + t / 30f) % 1f) * (size.height + r * 6) - r * 3
        val x = x0 * size.width + sin(t * 0.7f + phase * 9f) * r * 2
        val c = Offset(x, y)
        val color = greens[it % greens.size]
        rotate(sin(t * 0.7f + phase * 6f) * 25f, c) {
            for (k in 0 until 3) {
                val a = -PI / 2 + 2 * PI * k / 3
                drawCircle(color, r * 0.62f, Offset(c.x + r * 0.6f * cos(a).toFloat(), c.y + r * 0.6f * sin(a).toFloat()), alpha = 0.9f)
            }
            drawLine(color, c, Offset(c.x + r * 0.4f, c.y + r * 1.5f), r * 0.2f, StrokeCap.Round, alpha = 0.9f)
        }
    }
}

/** Autumn leaves spinning down. */
internal fun DrawScope.leaves(t: Float) {
    val colors = listOf(Color(0xFFE85A24), Color(0xFFF2A54C), Color(0xFFC0392B), Color(0xFFB9772B), Yellow)
    val rnd = Random(109)
    repeat(22) {
        val l = size.minDimension * (0.018f + rnd.nextFloat() * 0.012f)
        val x0 = rnd.nextFloat()
        val phase = rnd.nextFloat()
        val speed = if (rnd.nextBoolean()) 1f else 2f
        val y = ((phase + t / 30f * speed) % 1f) * (size.height + l * 4) - l * 2
        val x = x0 * size.width + sin(t * 0.8f + phase * 10f) * l * 2.5f
        val c = Offset(x, y)
        val color = colors[it % colors.size]
        rotate(t * 30f * speed + phase * 360f, c) {
            val leaf = Path().apply {
                moveTo(c.x, c.y - l)
                quadraticTo(c.x + l * 0.7f, c.y, c.x, c.y + l)
                quadraticTo(c.x - l * 0.7f, c.y, c.x, c.y - l)
                close()
            }
            drawPath(leaf, color, alpha = 0.9f)
            drawLine(Color(0xFF7A4A1E), Offset(c.x, c.y - l), Offset(c.x, c.y + l * 1.2f), l * 0.08f, alpha = 0.8f)
        }
    }
}

/** A row of [count] red lanterns hanging along the top, swaying and drifting one place along a minute. */
internal fun DrawScope.lanterns(t: Float, count: Int) {
    val gap = size.width / count
    val top = size.height * 0.02f
    val w = minOf(gap * 0.5f, size.minDimension * 0.07f)
    val h = w * 0.8f
    drawLine(Color(0xFF8B1A1A).copy(alpha = 0.8f), Offset(0f, top), Offset(size.width, top), 2f)
    val shift = (t / OVERLAY_LOOP_SECONDS) * gap
    for (i in -1..count) {
        val x = i * gap + shift + gap / 2
        val anchor = Offset(x, top)
        val c = Offset(x, top + h * (if (i % 2 == 0) 1.1f else 1.6f))
        rotate(sin(t * 1.1f + i * 1.3f) * 6f, anchor) {
            drawLine(Gold, anchor, Offset(x, c.y - h / 2), 1.5f)
            drawOval(Color(0x40FF4D4D), Offset(c.x - w, c.y - h), Size(w * 2, h * 2))
            drawOval(Color(0xFFD7263D), Offset(c.x - w / 2, c.y - h / 2), Size(w, h))
            drawOval(Color(0xFFB01E30), Offset(c.x - w * 0.25f, c.y - h / 2), Size(w * 0.5f, h), style = Stroke(1.5f))
            drawRect(Gold, Offset(c.x - w * 0.22f, c.y - h / 2 - h * 0.08f), Size(w * 0.44f, h * 0.12f))
            drawRect(Gold, Offset(c.x - w * 0.22f, c.y + h / 2 - h * 0.04f), Size(w * 0.44f, h * 0.12f))
            drawLine(Gold, Offset(c.x, c.y + h / 2 + h * 0.08f), Offset(c.x, c.y + h * 0.95f), w * 0.06f, StrokeCap.Round)
        }
    }
}

/** Six-pointed stars in gold, blue and white, drifting down, turning and twinkling. */
internal fun DrawScope.sixPointStars(t: Float) {
    val colors = listOf(Color(0xFFFFD86B), Color(0xFF8DB8F2), Color.White)
    val rnd = Random(127)
    repeat(12) {
        val r = size.minDimension * (0.016f + rnd.nextFloat() * 0.012f)
        val x0 = rnd.nextFloat()
        val phase = rnd.nextFloat()
        val y = ((phase + t / OVERLAY_LOOP_SECONDS) % 1f) * (size.height + r * 4) - r * 2
        val c = Offset(x0 * size.width + sin(t * 0.5f + phase * 9f) * r, y)
        val alpha = 0.6f + 0.4f * sin(t * 2f + phase * 10f)
        rotate(t * 6f + phase * 60f, c) {
            for (flip in listOf(1f, -1f)) {
                val triangle = Path().apply {
                    moveTo(c.x, c.y - r * flip)
                    lineTo(c.x + r * 0.866f, c.y + r * 0.5f * flip)
                    lineTo(c.x - r * 0.866f, c.y + r * 0.5f * flip)
                    close()
                }
                drawPath(triangle, colors[it % colors.size], alpha = alpha, style = Stroke(r * 0.18f))
            }
        }
    }
}

/** Now and then a shooting star streaks down across the upper sky, as bright as the [night] is dark. */
internal fun DrawScope.shootingStars(t: Float, night: Float) {
    val periods = floatArrayOf(12f, 20f, 30f)
    val trip = 0.9f
    for (i in periods.indices) {
        val period = periods[i]
        val shifted = t + i * 4.3f
        val seconds = shifted % period
        if (seconds > trip) continue
        val rnd = Random(i * 31 + (shifted / period).toInt() * 977)
        val start = Offset(size.width * (0.2f + rnd.nextFloat() * 0.8f), size.height * (0.03f + rnd.nextFloat() * 0.3f))
        val angle = PI * (0.72 + rnd.nextDouble() * 0.12)
        val run = size.minDimension * (0.25f + rnd.nextFloat() * 0.2f)
        val k = seconds / trip
        fun along(f: Float) = Offset(start.x + run * f * cos(angle).toFloat(), start.y + run * f * sin(angle).toFloat())
        val head = along(k)
        val tail = along(maxOf(0f, k - 0.35f))
        val fade = night * (if (k < 0.8f) 1f else (1 - k) / 0.2f)
        if (head != tail) drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White), tail, head), tail, head, size.minDimension * 0.004f, StrokeCap.Round, alpha = fade)
        drawCircle(Color.White, size.minDimension * 0.003f, head, alpha = fade)
    }
}
