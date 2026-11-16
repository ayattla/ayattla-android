package com.ayattla.app.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset

/** Elements taller or wider than this (in pixels) never get a bitmap shadow. */
internal const val SOFT_SHADOW_MAX_PX = 3000

/**
 * The site's soft shadow, safe at any size. Compose draws `dropShadow` into one bitmap as large as
 * the element, so a very long card (a whole list in one card) asks for hundreds of megabytes and the
 * system refuses to draw it ("Canvas: trying to draw too large bitmap"). Normal cards keep the same
 * soft shadow; very large ones use the platform shadow, which needs no bitmap. Until the size is
 * known (the first frame) no shadow is drawn, so an oversized bitmap is never requested.
 */
fun Modifier.softShadow(shape: Shape, radius: Dp, color: Color, offset: DpOffset = DpOffset.Zero): Modifier = composed {
    var big by remember { mutableStateOf<Boolean?>(null) }
    onSizeChanged { big = it.width > SOFT_SHADOW_MAX_PX || it.height > SOFT_SHADOW_MAX_PX }
        .then(
            when (big) {
                false -> Modifier.dropShadow(shape, Shadow(radius = radius, color = color, offset = offset))
                true -> Modifier.shadow(radius / 3, shape, clip = false, ambientColor = color, spotColor = color)
                null -> Modifier
            }
        )
}
