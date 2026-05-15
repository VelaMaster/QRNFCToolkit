package com.colectivobarrios.qrnfctoolkit.ui.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun EntradaAnimada(
    visibleState: MutableTransitionState<Boolean>,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(
            animationSpec = spring(stiffness = Spring.StiffnessLow)
        ) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarjetaHerramienta(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier,
    windowSize: Dp = 260.dp,
    cornerColor: Color = MaterialTheme.colorScheme.primary,
    laserColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val rectSizePx = windowSize.toPx()
        
        val left = (width - rectSizePx) / 2
        val top = (height - rectSizePx) / 2
        val right = left + rectSizePx
        val bottom = top + rectSizePx

        // Cutout path
        val rectPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(left, top, right, bottom),
                    cornerRadius = CornerRadius(24.dp.toPx())
                )
            )
        }

        // Background with cutout
        clipPath(rectPath, clipOp = ClipOp.Difference) {
            drawRect(
                color = Color.Black.copy(alpha = 0.6f)
            )
        }

        // Animated Laser Line
        val laserPosition = top + (rectSizePx * laserYOffset)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    laserColor.copy(alpha = 0.6f),
                    Color.Transparent
                )
            ),
            topLeft = Offset(left, laserPosition - 20.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(rectSizePx, 40.dp.toPx())
        )
        
        drawLine(
            color = laserColor,
            start = Offset(left + 8.dp.toPx(), laserPosition),
            end = Offset(right - 8.dp.toPx(), laserPosition),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Corners
        val strokeWidth = 5.dp.toPx()
        val cornerLength = 32.dp.toPx()
        val radius = 24.dp.toPx()

        // Top-left
        drawPath(
            path = Path().apply {
                moveTo(left, top + cornerLength)
                lineTo(left, top + radius)
                arcTo(
                    rect = Rect(left, top, left + radius * 2, top + radius * 2),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(left + cornerLength, top)
            },
            color = cornerColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Top-right
        drawPath(
            path = Path().apply {
                moveTo(right - cornerLength, top)
                lineTo(right - radius, top)
                arcTo(
                    rect = Rect(right - radius * 2, top, right, top + radius * 2),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(right, top + cornerLength)
            },
            color = cornerColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Bottom-left
        drawPath(
            path = Path().apply {
                moveTo(left, bottom - cornerLength)
                lineTo(left, bottom - radius)
                arcTo(
                    rect = Rect(left, bottom - radius * 2, left + radius * 2, bottom),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )
                lineTo(left + cornerLength, bottom)
            },
            color = cornerColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Bottom-right
        drawPath(
            path = Path().apply {
                moveTo(right - cornerLength, bottom)
                lineTo(right - radius, bottom)
                arcTo(
                    rect = Rect(right - radius * 2, bottom - radius * 2, right, bottom),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )
                lineTo(right, bottom - cornerLength)
            },
            color = cornerColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
