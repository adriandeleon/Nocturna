package com.adl.nocturna.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.sin

enum class Glyph { SUN, MOON, CLOUD, PARTLY_DAY, PARTLY_NIGHT, RAIN, SNOW, STORM, FOG, BATTERY, BOLT, ALARM }

/**
 * Small line icons drawn with Canvas so they can take any tint (including night-mode red)
 * without pulling in an icon library.
 */
@Composable
fun GlyphIcon(glyph: Glyph, color: Color, size: Dp, modifier: Modifier = Modifier, level: Float = 1f) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = Stroke(width = s * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (glyph) {
            Glyph.SUN -> sun(color, stroke, Offset(s / 2, s / 2), s * 0.2f, s)
            Glyph.MOON -> moon(color, Offset(s / 2, s / 2), s * 0.32f)
            Glyph.CLOUD -> drawPath(cloud(s, 0f), color, style = stroke)
            Glyph.PARTLY_DAY, Glyph.PARTLY_NIGHT -> {
                val c = cloud(s, s * 0.06f)
                drawPath(c, color, style = stroke)
                clipPath(c, ClipOp.Difference) {
                    if (glyph == Glyph.PARTLY_DAY) sun(color, stroke, Offset(s * 0.36f, s * 0.36f), s * 0.13f, s * 0.62f)
                    else moon(color, Offset(s * 0.36f, s * 0.34f), s * 0.2f)
                }
            }
            Glyph.RAIN -> {
                drawPath(cloud(s, -s * 0.14f), color, style = stroke)
                for (i in 0..2) {
                    val x = s * (0.36f + i * 0.15f)
                    drawLine(color, Offset(x, s * 0.72f), Offset(x - s * 0.06f, s * 0.9f), stroke.width, StrokeCap.Round)
                }
            }
            Glyph.SNOW -> {
                drawPath(cloud(s, -s * 0.14f), color, style = stroke)
                for (i in 0..2) drawCircle(color, s * 0.05f, Offset(s * (0.34f + i * 0.16f), s * (if (i == 1) 0.88f else 0.78f)))
            }
            Glyph.STORM -> {
                drawPath(cloud(s, -s * 0.14f), color, style = stroke)
                val bolt = Path().apply {
                    moveTo(s * 0.56f, s * 0.64f); lineTo(s * 0.44f, s * 0.8f); lineTo(s * 0.56f, s * 0.8f); lineTo(s * 0.46f, s * 0.96f)
                }
                drawPath(bolt, color, style = stroke)
            }
            Glyph.FOG -> for (i in 0..2) {
                val y = s * (0.35f + i * 0.17f)
                val inset = if (i == 1) 0.12f else 0.2f
                drawLine(color, Offset(s * inset, y), Offset(s * (1 - inset), y), stroke.width, StrokeCap.Round)
            }
            Glyph.BATTERY -> {
                val body = Rect(s * 0.08f, s * 0.3f, s * 0.84f, s * 0.7f)
                drawRoundRect(color, body.topLeft, body.size, CornerRadius(s * 0.07f), style = Stroke(s * 0.07f))
                drawRoundRect(color, Offset(s * 0.86f, s * 0.42f), Size(s * 0.07f, s * 0.16f), CornerRadius(s * 0.03f))
                val pad = s * 0.08f
                val w = (body.width - 2 * pad) * level.coerceIn(0f, 1f)
                if (w > 0f) drawRect(color, Offset(body.left + pad, body.top + pad), Size(w, body.height - 2 * pad))
            }
            Glyph.BOLT -> {
                val p = Path().apply {
                    moveTo(s * 0.58f, s * 0.12f); lineTo(s * 0.3f, s * 0.56f); lineTo(s * 0.5f, s * 0.56f)
                    lineTo(s * 0.42f, s * 0.88f); lineTo(s * 0.72f, s * 0.42f); lineTo(s * 0.52f, s * 0.42f); close()
                }
                drawPath(p, color)
            }
            Glyph.ALARM -> {
                val c = Offset(s / 2, s * 0.55f)
                drawCircle(color, s * 0.32f, c, style = stroke)
                drawLine(color, c, Offset(c.x, c.y - s * 0.18f), stroke.width, StrokeCap.Round)
                drawLine(color, c, Offset(c.x + s * 0.13f, c.y + s * 0.06f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(s * 0.12f, s * 0.22f), Offset(s * 0.26f, s * 0.1f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(s * 0.88f, s * 0.22f), Offset(s * 0.74f, s * 0.1f), stroke.width, StrokeCap.Round)
            }
        }
    }
}

private fun DrawScope.sun(color: Color, stroke: Stroke, c: Offset, r: Float, s: Float) {
    drawCircle(color, r, c, style = stroke)
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        val inner = r + s * 0.09f
        val outer = r + s * 0.18f
        drawLine(
            color,
            Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
            Offset(c.x + outer * cos(a).toFloat(), c.y + outer * sin(a).toFloat()),
            stroke.width, StrokeCap.Round,
        )
    }
}

private fun DrawScope.moon(color: Color, c: Offset, r: Float) {
    val disc = Path().apply { addOval(Rect(c, r)) }
    val cut = Path().apply { addOval(Rect(Offset(c.x + r * 0.55f, c.y - r * 0.45f), r * 0.9f)) }
    drawPath(Path.combine(PathOperation.Difference, disc, cut), color)
}

/** A cloud outline built from overlapping circles; [dy] shifts it vertically. */
private fun cloud(s: Float, dy: Float): Path {
    fun circle(x: Float, y: Float, r: Float) = Path().apply { addOval(Rect(Offset(s * x, s * y + dy), s * r)) }
    var p = circle(0.34f, 0.62f, 0.16f)
    p = Path.combine(PathOperation.Union, p, circle(0.54f, 0.52f, 0.21f))
    p = Path.combine(PathOperation.Union, p, circle(0.72f, 0.63f, 0.15f))
    p = Path.combine(PathOperation.Union, p, Path().apply { addRect(Rect(s * 0.34f, s * 0.6f + dy, s * 0.72f, s * 0.78f + dy)) })
    return p
}
