package com.ayattla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ayattla.app.ui.theme.LocalBrand

val CardShape = RoundedCornerShape(20.dp)
val SmallShape = RoundedCornerShape(12.dp)

/** The site's card: white surface, soft emerald shadow, hairline border. */
@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val b = LocalBrand.current
    Column(
        modifier
            .fillMaxWidth()
            .shadow(if (b.dark) 0.dp else 6.dp, CardShape, ambientColor = b.primary.copy(alpha = 0.07f), spotColor = b.primary.copy(alpha = 0.10f))
            .clip(CardShape)
            .background(b.surface)
            .border(1.dp, b.line, CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Emerald hero panel with the thin gold frame, as on the site. */
@Composable
fun EmeraldPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val b = LocalBrand.current
    Box(
        modifier
            .clip(CardShape)
            .background(Brush.linearGradient(listOf(b.g0, b.primary, b.deep)))
            .padding(6.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, b.accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    val b = LocalBrand.current
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = b.ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = b.muted)
        }
        trailing?.invoke()
    }
}

/** Tool tile: gold-soft icon square, title, short line; text centered (owner's rule). */
@Composable
fun ToolTile(title: String, subtitle: String?, icon: ImageVector, modifier: Modifier = Modifier, accent: Color? = null, onClick: () -> Unit) {
    val b = LocalBrand.current
    AppCard(modifier, onClick = onClick, padding = 14.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(46.dp).clip(SmallShape).background(accent?.copy(alpha = 0.14f) ?: b.goldSoft),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = accent ?: b.goldInk, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.size(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = b.ink, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(
                subtitle, style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    val b = LocalBrand.current
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = b.background, titleContentColor = b.ink, navigationIconContentColor = b.ink, actionIconContentColor = b.ink),
    )
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val b = LocalBrand.current
    Box(
        modifier
            .clip(CircleShape)
            .background(if (selected) b.primary else b.surface)
            .border(1.dp, if (selected) b.primary else b.lineStrong, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) b.onPrimary else b.ink)
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    val b = LocalBrand.current
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = b.ink, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = b.muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LocalBrand.current.accent) }
}

/** Small gold label pill. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier) {
    val b = LocalBrand.current
    Text(
        text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = b.goldInk,
        modifier = modifier.clip(CircleShape).background(b.goldSoft).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))
