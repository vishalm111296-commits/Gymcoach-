package com.gymcoach.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.TextPrimary
import com.gymcoach.app.ui.theme.TextSecondary
import com.gymcoach.app.ui.theme.TextTertiary

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.scale

/**
 * Premium, high-contrast exercise card component.
 * Features a structured typographic hierarchy:
 * - Dominant exercise name + tactile animated favorite toggle
 * - Secondary metadata with muscle tag and clean equipment/movement string
 * - Bottom status bar with guaranteed single-line difficulty badge and subtle animation prompt
 */
@Composable
fun ExerciseItemCard(
    name: String,
    muscleGroup: String,
    difficulty: String,
    modifier: Modifier = Modifier,
    equipment: String = "",
    movementPattern: String = "",
    isFavorite: Boolean = false,
    hasAnimation: Boolean = false,
    isCustom: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val diffLower = difficulty.lowercase().trim()
    val (diffColor, diffBg) = when (diffLower) {
        "beginner" -> Pair(Color(0xFF34D399), Color(0x2210B981))
        "advanced" -> Pair(Color(0xFFF87171), Color(0x22EF4444))
        else -> Pair(Color(0xFFFBBF24), Color(0x22F59E0B)) // Intermediate / default
    }

    val favoriteTint by animateColorAsState(
        targetValue = if (isFavorite) Color(0xFFF43F5E) else TextTertiary,
        label = "favTint"
    )
    val favoriteScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "favScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.md)
            .border(GymCoachBorders.subtle, GymCoachShapes.md)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        shape = GymCoachShapes.md
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Exercise Name & Actions (Custom tag, Favorite toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isCustom) {
                        Box(
                            modifier = Modifier
                                .clip(GymCoachShapes.xs)
                                .background(Color(0x3300E5FF))
                                .border(1.dp, Color(0x5500E5FF), GymCoachShapes.xs)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CUSTOM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }

                    if (onFavoriteToggle != null) {
                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier
                                .size(36.dp)
                                .scale(favoriteScale)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                                tint = favoriteTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else if (isFavorite) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Row 2: Secondary Metadata (Muscle tag + Equipment / Movement Pattern)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Muscle group pill badge
                Box(
                    modifier = Modifier
                        .clip(GymCoachShapes.xs)
                        .background(AccentBlue.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = muscleGroup,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = AccentBlue
                    )
                }

                val metadataDetails = buildList {
                    if (equipment.isNotBlank()) add(equipment.replace(",", ", "))
                    if (movementPattern.isNotBlank()) add(movementPattern)
                }.joinToString(" • ")

                if (metadataDetails.isNotBlank()) {
                    Text(
                        text = metadataDetails,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 3: Bottom Status Bar (Single-line Guaranteed Difficulty + Optional Animation Prompt)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Difficulty Badge: Never wraps, guaranteed single-line with explicit minWidth
                Box(
                    modifier = Modifier
                        .clip(GymCoachShapes.xs)
                        .background(diffBg)
                        .widthIn(min = 60.dp)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = difficulty.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = diffColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (hasAnimation) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(GymCoachShapes.pill)
                            .background(GymCoachColors.SurfaceCardElevated)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleFilled,
                            contentDescription = "Animation available",
                            tint = AccentBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Animation",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

