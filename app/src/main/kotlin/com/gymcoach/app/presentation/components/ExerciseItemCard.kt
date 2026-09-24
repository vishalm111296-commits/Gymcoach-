package com.gymcoach.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * Premium, high-contrast exercise card component.
 * Features a structured typographic hierarchy and rich motion physics:
 * - Dominant exercise name + tactile animated favorite toggle with spring bounce
 * - Smooth touch-press spring scale feedback
 * - Secondary metadata with animated muscle tag and clean equipment/movement string
 * - Animated tag badges (custom, animation prompt, difficulty)
 * - Expandable quick details section with spring expand/collapse
 * - Bottom status bar with guaranteed single-line difficulty badge
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
    hasCameraCoach: Boolean = false,
    isCustom: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardPressScale"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = if (isPressed) AccentBlue.copy(alpha = 0.5f) else GymCoachColors.BorderSubtle,
        animationSpec = tween(150),
        label = "cardBorderColor"
    )

    val diffLower = difficulty.lowercase().trim()
    val (diffColor, diffBg) = when (diffLower) {
        "beginner" -> Pair(Color(0xFF34D399), Color(0x2210B981))
        "advanced" -> Pair(Color(0xFFF87171), Color(0x22EF4444))
        else -> Pair(Color(0xFFFBBF24), Color(0x22F59E0B)) // Intermediate / default
    }

    val animatedDiffColor by animateColorAsState(
        targetValue = diffColor,
        animationSpec = tween(250),
        label = "diffColor"
    )
    val animatedDiffBg by animateColorAsState(
        targetValue = diffBg,
        animationSpec = tween(250),
        label = "diffBg"
    )

    val favoriteTint by animateColorAsState(
        targetValue = if (isFavorite) Color(0xFFF43F5E) else TextTertiary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "favTint"
    )
    val favoriteScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "favScale"
    )

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chevronRot"
    )

    val muscleBg by animateColorAsState(
        targetValue = AccentBlue.copy(alpha = if (isPressed) 0.22f else 0.14f),
        label = "muscleBg"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(GymCoachShapes.md)
            .border(1.dp, cardBorderColor, GymCoachShapes.md)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        shape = GymCoachShapes.md
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
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
                    AnimatedVisibility(
                        visible = isCustom,
                        enter = fadeIn(animationSpec = tween(200)) + expandHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ),
                        exit = fadeOut(animationSpec = tween(150)) + shrinkHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
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
                                .graphicsLayer {
                                    scaleX = favoriteScale
                                    scaleY = favoriteScale
                                }
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
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer {
                                    scaleX = favoriteScale
                                    scaleY = favoriteScale
                                }
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
                // Muscle group pill badge with animated background
                Box(
                    modifier = Modifier
                        .clip(GymCoachShapes.xs)
                        .background(muscleBg)
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

            // Row 3: Bottom Status Bar (Difficulty Badge, Animation Prompt, Expand Toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Difficulty Badge: Animated colors with guaranteed single-line explicit minWidth
                Box(
                    modifier = Modifier
                        .clip(GymCoachShapes.xs)
                        .background(animatedDiffBg)
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
                        color = animatedDiffColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnimatedVisibility(
                        visible = hasAnimation,
                        enter = fadeIn(animationSpec = tween(200)) + expandHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ),
                        exit = fadeOut(animationSpec = tween(150)) + shrinkHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
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

                    AnimatedVisibility(
                        visible = hasCameraCoach,
                        enter = fadeIn(animationSpec = tween(200)) + expandHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ),
                        exit = fadeOut(animationSpec = tween(150)) + shrinkHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(GymCoachShapes.pill)
                                .background(GymCoachColors.SurfaceCardElevated)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "AI Camera Form Coach available",
                                tint = GymCoachColors.CyanAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "AI Coach",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = TextPrimary
                            )
                        }
                    }

                    // Expand/collapse chevron button
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse details" else "Expand details",
                            tint = TextTertiary,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer {
                                    rotationZ = chevronRotation
                                }
                        )
                    }
                }
            }

            // Expandable Details Preview
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(200)) + expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(GymCoachShapes.sm)
                        .background(DarkSurface.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Movement Specs",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue
                            )
                        )
                        Text(
                            text = "Tap card for full biomechanics",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = TextTertiary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (equipment.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(GymCoachShapes.xs)
                                    .background(GymCoachColors.SurfaceCardElevated)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Equipment: ${equipment.replace(",", ", ")}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                        }
                        if (movementPattern.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(GymCoachShapes.xs)
                                    .background(GymCoachColors.SurfaceCardElevated)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Pattern: $movementPattern",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
