package com.pupil.app.core.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pupil.app.core.domain.model.PaymentAppConfig
import com.pupil.app.core.domain.model.Transaction
import com.pupil.app.core.domain.model.TransactionStatus
import com.pupil.app.core.ui.theme.Teal
import com.pupil.app.core.ui.util.DateUtils
import com.pupil.app.core.ui.util.Formatters

@Composable
fun TransactionCard(
    transaction: Transaction,
    onDelete: ((Long) -> Unit)? = null,
    onMarkCompleted: ((Long) -> Unit)? = null,
    onMarkFailed: ((Long) -> Unit)? = null,
    onEdit: ((Long) -> Unit)? = null
) {
    val isPending = transaction.status == TransactionStatus.PENDING
    val isFailed = transaction.status == TransactionStatus.FAILED

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPending) 4.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = transaction.merchantName, style = MaterialTheme.typography.titleMedium)
                        if (isPending) {
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(label = "Pending", color = Color(0xFFFF9800))
                        }
                        if (isFailed) {
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(label = "Failed", color = Color(0xFFE53935))
                        }
                    }
                    Text(text = transaction.reason, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = "\u20B9${Formatters.formatPaise(transaction.amountPaise)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Timestamp display
            Text(
                text = DateUtils.formatTimestamp(transaction.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryBadge(label = transaction.categoryName)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = transaction.paymentApp, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.weight(1f))
                // Edit button - always visible
                if (onEdit != null) {
                    IconButton(onClick = { onEdit(transaction.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
                // Delete button - always visible
                if (onDelete != null) {
                    IconButton(onClick = { onDelete(transaction.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
                // Mark complete/fail for pending — distinct icons for clear UX
                if (isPending && onMarkCompleted != null && onMarkFailed != null) {
                    IconButton(onClick = { onMarkCompleted(transaction.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Mark completed", tint = Teal, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onMarkFailed(transaction.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Cancel, contentDescription = "Mark failed", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBadge(label: String) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.small,
        color = Teal.copy(alpha = 0.08f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Teal
        )
    }
}

@Composable
fun StatusBadge(label: String, color: Color) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TypeChip(selected: Boolean, onClick: () -> Unit, label: String) {
    Surface(
        tonalElevation = if (selected) 4.dp else 0.dp,
        shape = MaterialTheme.shapes.extraSmall,
        color = if (selected) Teal else MaterialTheme.colorScheme.surface,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PaymentAppCard(app: PaymentAppConfig, selected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val appIconBitmap = remember(app.packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(app.packageName)
            val bitmap = Bitmap.createBitmap(
                (drawable.intrinsicWidth).coerceAtLeast(1),
                (drawable.intrinsicHeight).coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            com.pupil.app.core.ui.util.AppLogger.w("UI", "Failed to load app icon for ${app.packageName}", e)
            null
        }
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (selected) 6.dp else 1.dp,
        color = if (selected) Teal else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (appIconBitmap != null) {
                Icon(
                    painter = BitmapPainter(image = appIconBitmap),
                    contentDescription = app.displayName,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Unspecified  // Don't tint icons — green background already indicates selection
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = app.displayName, style = MaterialTheme.typography.bodyLarge, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
                Text(text = app.packageName, style = MaterialTheme.typography.bodySmall, color = if (selected) Color.White.copy(alpha = 0.8f) else Color.Gray)
            }
        }
    }
}
