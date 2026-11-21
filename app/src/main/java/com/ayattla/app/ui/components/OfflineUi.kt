package com.ayattla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.ayattla.app.data.OfflineLevel
import com.ayattla.app.ui.theme.LocalBrand

/** Colour and icon of each offline level: green works, gold works with saved files, grey needs internet. */
@Composable
fun offlineTint(level: OfflineLevel): Color {
    val b = LocalBrand.current
    return when (level) {
        OfflineLevel.Full -> b.green
        OfflineLevel.Saved -> b.goldInk
        OfflineLevel.Online -> b.muted
    }
}

fun offlineIcon(level: OfflineLevel): ImageVector = when (level) {
    OfflineLevel.Full -> Icons.Outlined.CheckCircle
    OfflineLevel.Saved -> Icons.Outlined.DownloadDone
    OfflineLevel.Online -> Icons.Outlined.CloudOff
}

/** «يعمل بدون إنترنت» chip. */
@Composable
fun OfflineChip(level: OfflineLevel, modifier: Modifier = Modifier) {
    val tint = offlineTint(level)
    Row(
        modifier.clip(CircleShape).background(tint.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(offlineIcon(level), null, tint = tint, modifier = Modifier.size(14.dp))
        Text(level.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = tint)
    }
}

/** Floating strip shown while there is no connection; opens the offline centre. */
@Composable
fun OfflinePill(modifier: Modifier = Modifier, onOpen: () -> Unit, onClose: () -> Unit) {
    val b = LocalBrand.current
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier.padding(horizontal = 18.dp)
            .softShadow(shape, 18.dp, Color(0xFF0F1F17).copy(alpha = if (b.dark) 0.5f else 0.18f), DpOffset(0.dp, 8.dp))
            .clip(shape).background(Color(0xFF14382A)).clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CloudOff, null, tint = Color(0xFFE9C46A), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("أنت بدون إنترنت", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Text("أغلب الأدوات تعمل الآن. اضغط لترى ما يعمل", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
        }
        Box(Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Close, "إخفاء", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        }
    }
}

/** A tool cannot do this without internet: what still works, and the way to the offline centre. */
@Composable
fun OfflineNotice(title: String, message: String, modifier: Modifier = Modifier, action: String = "ما يعمل بدون إنترنت", onAction: (() -> Unit)? = null) {
    val b = LocalBrand.current
    val shape = RoundedCornerShape(Radius.card)
    Column(
        modifier.fillMaxWidth().clip(shape).background(b.surface).border(1.dp, b.line, shape).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(b.goldInk.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.CloudOff, null, tint = b.goldInk, modifier = Modifier.size(26.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = b.ink, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = b.muted, textAlign = TextAlign.Center)
        if (onAction != null) BrandButton(action, Modifier.fillMaxWidth().padding(top = 4.dp), kind = BtnKind.Secondary, onClick = onAction)
    }
}
