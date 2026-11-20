package com.ayattla.app.studio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ayattla.app.ui.theme.LocalBrand

/** Colour palettes as real swatches (gradient with its accent ring), scrolling sideways. */
@Composable
fun PalettePicker(selected: CardStyle, onPick: (CardStyle) -> Unit) {
    val b = LocalBrand.current
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CardStyle.entries.forEach { st ->
            val on = st == selected
            Column(
                Modifier.width(64.dp).clip(RoundedCornerShape(14.dp)).clickable { onPick(st) }
                    .semantics { contentDescription = "ألوان ${st.label}"; this.selected = on }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(st.top), Color(st.bottom))))
                        .border(if (on) 3.dp else 1.dp, if (on) b.green else Color(st.accent).copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (on) Icon(Icons.Filled.Check, null, tint = Color(st.text), modifier = Modifier.size(22.dp))
                    else Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(Color(st.accent)))
                }
                Text(st.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                    color = if (on) b.primaryInk else b.muted, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Decorations shown as small live previews drawn in the current palette. */
@Composable
fun OrnamentPicker(selected: Ornament, palette: CardStyle, onPick: (Ornament) -> Unit) {
    val b = LocalBrand.current
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Ornament.entries.forEach { o ->
            val on = o == selected
            Column(
                Modifier.width(72.dp).clip(RoundedCornerShape(14.dp)).clickable { onPick(o) }
                    .semantics { contentDescription = "زخرفة ${o.label}"; this.selected = on }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(width = 56.dp, height = 72.dp).clip(RoundedCornerShape(10.dp))
                        .background(Brush.verticalGradient(listOf(Color(palette.top), Color(palette.bottom))))
                        .border(if (on) 3.dp else 1.dp, if (on) b.green else b.line, RoundedCornerShape(10.dp)),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val k = size.width / 1080f
                        drawIntoCanvas { Ornaments.draw(it.nativeCanvas, size.width, size.height, k * 2.2f, palette.accent, o) }
                    }
                }
                Text(o.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                    color = if (on) b.primaryInk else b.muted, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}


/** Built-in looks as visual thumbnails: palette, decoration and a line of sample shapes for the text. */
@Composable
fun TemplatePicker(templates: List<Template>, current: Style, onPick: (Style) -> Unit) {
    val b = LocalBrand.current
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        templates.forEach { t ->
            val on = t.style == current
            val st = t.style.colors
            Column(
                Modifier.width(92.dp).clip(RoundedCornerShape(14.dp)).clickable { onPick(t.style) }
                    .semantics { contentDescription = "قالب ${t.name}"; this.selected = on }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(width = 76.dp, height = 104.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(listOf(Color(st.top), Color(st.bottom))))
                        .border(if (on) 3.dp else 1.dp, if (on) b.green else b.line, RoundedCornerShape(12.dp)),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val k = size.width / 1080f * 2.4f
                        if (t.style.frame) drawIntoCanvas { Ornaments.draw(it.nativeCanvas, size.width, size.height, k, st.accent, t.style.ornament) }
                        // Three lines standing for the text, and a short one for the reference.
                        val tc = Color(st.text).copy(alpha = 0.85f)
                        val cy = size.height * (0.46f + t.style.text.posY * 0.2f)
                        listOf(0.62f, 0.72f, 0.5f).forEachIndexed { i, f ->
                            val lw = size.width * f
                            drawRoundRect(tc, androidx.compose.ui.geometry.Offset((size.width - lw) / 2, cy + (i - 1) * size.height * 0.08f), androidx.compose.ui.geometry.Size(lw, size.height * 0.035f), androidx.compose.ui.geometry.CornerRadius(4f))
                        }
                        val ac = Color(st.accent)
                        drawRoundRect(ac, androidx.compose.ui.geometry.Offset(size.width * 0.34f, size.height * 0.78f), androidx.compose.ui.geometry.Size(size.width * 0.32f, size.height * 0.03f), androidx.compose.ui.geometry.CornerRadius(4f))
                    }
                }
                Text(t.name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                    color = if (on) b.primaryInk else b.muted, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
