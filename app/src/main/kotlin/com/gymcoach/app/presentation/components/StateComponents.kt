package com.gymcoach.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, cornerRadius: Dp = 8.dp) {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.03f),
        Color.White.copy(alpha = 0.08f),
        Color.White.copy(alpha = 0.03f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate_anim"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(GymCoachColors.SurfaceCard)
            .background(brush)
    )
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    headline: String,
    body: String,
    ctaText: String? = null,
    onCtaClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(GymCoachColors.SurfaceDeep, shape = GymCoachShapes.pill)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GymCoachColors.Primary,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = headline,
            style = MaterialTheme.typography.headlineMedium,
            color = GymCoachColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = GymCoachColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        if (ctaText != null && onCtaClick != null) {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onCtaClick,
                shape = GymCoachShapes.pill,
                colors = ButtonDefaults.buttonColors(containerColor = GymCoachColors.Primary),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Text(
                    text = ctaText,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ErrorStateView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            color = GymCoachColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = GymCoachColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onRetry,
            shape = GymCoachShapes.pill,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GymCoachColors.Primary),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Retry")
        }
    }
}
