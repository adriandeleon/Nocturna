package com.adl.nocturna.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A [ControlButton]'s colours, at rest and while it is on. */
@Immutable
data class ControlColors(
    val container: Color,
    val content: Color,
    val border: Color,
    val onContainer: Color,
    val onContent: Color,
    val onBorder: Color,
)

object ControlDefaults {
    /** Tall enough to find by touch in the dark. */
    val Height: Dp = 48.dp
    val Shape: Shape = RoundedCornerShape(14.dp)

    /**
     * See-through and drawn in [color], for the black night clock and the alarm screen: a faint outline at
     * rest, and a wash of [color] with a firm outline while on, so it stays legible in night-mode red.
     */
    fun night(color: Color) = ControlColors(
        container = Color.Transparent,
        content = color,
        border = color.copy(alpha = 0.3f),
        onContainer = color.copy(alpha = 0.18f),
        onContent = color,
        onBorder = color.copy(alpha = 0.75f),
    )

    /**
     * Over day mode's bright background see-through buttons vanish, so these are filled in the desk clock's
     * navy, and in the theme's warm yellow while on.
     */
    val Day = ControlColors(
        container = DeskNavy,
        content = Color.White,
        border = Color.Transparent,
        onContainer = DayGold,
        onContent = DayGoldInk,
        onBorder = Color.Transparent,
    )

    /** A solid button in [color], for the one action a screen is there for (dismissing an alarm). */
    fun solid(color: Color) = ControlColors(color, Color.Black, Color.Transparent, color, Color.Black, Color.Transparent)

    /** A soft wash of [color] with no outline, for the action next to a [solid] one. */
    fun tonal(color: Color) = ControlColors(
        color.copy(alpha = 0.14f), color, Color.Transparent,
        color.copy(alpha = 0.14f), color, Color.Transparent,
    )
}

/**
 * The button Nocturna's own screens use: a [glyph] beside (or, [stacked], above) its [label].
 *
 * Give [checked] for a switch, such as the radio or dimming: it is announced as a switch, and while it is on
 * it takes the `on` colours of [colors], so its state shows without reading the label. Leave it null for a
 * plain button.
 */
@Composable
fun ControlButton(
    label: String,
    colors: ControlColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: Glyph? = null,
    checked: Boolean? = null,
    enabled: Boolean = true,
    stacked: Boolean = false,
    shape: Shape = ControlDefaults.Shape,
    fontSize: TextUnit = 15.sp,
    glyphSize: Dp = 20.dp,
) {
    val on = checked == true
    val content = if (on) colors.onContent else colors.content
    val border = if (on) colors.onBorder else colors.border
    val action = if (checked != null) {
        Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch) { onClick() }
    } else {
        Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    }
    Box(
        modifier
            .defaultMinSize(minHeight = ControlDefaults.Height)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(shape)
            .background(if (on) colors.onContainer else colors.container)
            .then(if (border.alpha > 0f) Modifier.border(1.dp, border, shape) else Modifier)
            .then(action)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val text = @Composable { Text(label, color = content, fontSize = fontSize, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (stacked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                glyph?.let { GlyphIcon(it, content, glyphSize) }
                text()
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                glyph?.let { GlyphIcon(it, content, glyphSize) }
                text()
            }
        }
    }
}
