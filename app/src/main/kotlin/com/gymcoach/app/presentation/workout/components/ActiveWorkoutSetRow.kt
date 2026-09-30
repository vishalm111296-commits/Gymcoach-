package com.gymcoach.app.presentation.workout.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.core.preferences.WeightUnit
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes

typealias WorkoutSetUiModel = WorkoutSet

@Composable
fun ActiveWorkoutSetRow(
    set: WorkoutSetUiModel,
    previousPerformance: String? = null,
    weightUnit: WeightUnit = WeightUnit.KG,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onRpeChange: ((Double) -> Unit)? = null,
    onSetTypeChange: ((SetType) -> Unit)? = null,
    onCompleteToggle: () -> Unit,
    onQuickFill: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var weightInput by remember {
        mutableStateOf(if (set.weight > 0) (if (set.weight % 1.0 == 0.0) set.weight.toInt().toString() else set.weight.toString()) else "")
    }
    var repsInput by remember {
        mutableStateOf(if (set.reps > 0) set.reps.toString() else "")
    }
    var showQuickChips by remember { mutableStateOf(false) }

    // Synchronize local input buffers with external updates (camera rep counter, quick-fill, recommendations)
    LaunchedEffect(set.weight) {
        val currentParsed = weightInput.toDoubleOrNull() ?: 0.0
        if (currentParsed != set.weight) {
            weightInput = if (set.weight > 0) {
                if (set.weight % 1.0 == 0.0) set.weight.toInt().toString() else set.weight.toString()
            } else ""
        }
    }

    LaunchedEffect(set.reps) {
        val currentParsed = repsInput.toIntOrNull() ?: 0
        if (currentParsed != set.reps) {
            repsInput = if (set.reps > 0) set.reps.toString() else ""
        }
    }

    val haptic = LocalHapticFeedback.current
    val isImperial = weightUnit == WeightUnit.LBS
    val weightStep = if (isImperial) 5.0 else 2.5
    val quickIncrements = if (isImperial) listOf(2.5, 5.0, 10.0) else listOf(1.25, 2.5, 5.0)

    val setTypeColor = when (set.setType) {
        SetType.WARMUP -> GymCoachColors.SetWarmup
        SetType.DROP -> GymCoachColors.SetDrop
        SetType.FAILURE -> GymCoachColors.SetFailure
        else -> GymCoachColors.Primary
    }
    val setTypeText = when (set.setType) {
        SetType.WARMUP -> "W"
        SetType.DROP -> "D"
        SetType.FAILURE -> "F"
        else -> "${set.setNumber}"
    }

    // Tactile checkmark pop
    val checkScale = remember { Animatable(1.0f) }
    LaunchedEffect(set.completed) {
        if (set.completed) {
            checkScale.snapTo(0.8f)
            checkScale.animateTo(1.2f, tween(durationMillis = 110, easing = FastOutSlowInEasing))
            checkScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        } else {
            checkScale.animateTo(1.0f, tween(durationMillis = 150))
        }
    }

    val rowBackgroundColor by animateColorAsState(
        targetValue = if (set.completed) GymCoachColors.Success.copy(alpha = 0.09f) else GymCoachColors.SurfaceDeep,
        animationSpec = tween(durationMillis = 280),
        label = "rowBgColor"
    )
    val rowBorderColor by animateColorAsState(
        targetValue = if (set.completed) GymCoachColors.Success.copy(alpha = 0.65f) else GymCoachColors.BorderSubtle,
        animationSpec = tween(durationMillis = 280),
        label = "rowBorderColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.sm)
            .background(rowBackgroundColor)
            .border(1.dp, rowBorderColor, GymCoachShapes.sm)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Inline Previous Performance / Quick-fill row
        if (previousPerformance != null && !set.completed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prev: $previousPerformance",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = GymCoachColors.TextSecondary
                )
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (onQuickFill != null) {
                            onQuickFill()
                        } else {
                            // Inline parse fallback: e.g. "100kg × 8" or "100 lbs x 8"
                            val parts = previousPerformance.split(Regex("[x×]"))
                            if (parts.size == 2) {
                                val w = parts[0].replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                                val r = parts[1].replace(Regex("[^0-9]"), "").toIntOrNull()
                                if (w != null) onWeightChange(w)
                                if (r != null) onRepsChange(r)
                            }
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    color = GymCoachColors.Primary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, GymCoachColors.Primary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "QUICK-FILL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        color = GymCoachColors.Primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Primary dense set logging row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Set type button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(GymCoachShapes.xs)
                    .background(setTypeColor.copy(alpha = 0.15f))
                    .clickable(role = Role.Button) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val nextType = when (set.setType) {
                            SetType.NORMAL -> SetType.WARMUP
                            SetType.WARMUP -> SetType.DROP
                            SetType.DROP -> SetType.FAILURE
                            SetType.FAILURE -> SetType.NORMAL
                        }
                        onSetTypeChange?.invoke(nextType)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = setTypeText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = setTypeColor
                )
            }

            // Compact Weight Stepper Group
            Row(
                modifier = Modifier
                    .weight(1.2f)
                    .height(38.dp)
                    .clip(GymCoachShapes.xs)
                    .background(GymCoachColors.SurfaceInput)
                    .border(1.dp, GymCoachColors.BorderSubtle, GymCoachShapes.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val current = set.weight
                        val newVal = (current - weightStep).coerceAtLeast(0.0)
                        onWeightChange(newVal)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease weight",
                        tint = GymCoachColors.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showQuickChips = !showQuickChips },
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = weightInput,
                        onValueChange = { newVal ->
                            weightInput = newVal
                            newVal.toDoubleOrNull()?.let { onWeightChange(it) }
                        },
                        textStyle = TextStyle(
                            color = GymCoachColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        cursorBrush = SolidColor(GymCoachColors.Primary),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                    )
                    if (weightInput.isEmpty()) {
                        Text(
                            text = "0",
                            style = TextStyle(
                                color = GymCoachColors.TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val current = set.weight
                        onWeightChange(current + weightStep)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase weight",
                        tint = GymCoachColors.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Compact Reps Stepper Group
            Row(
                modifier = Modifier
                    .weight(1.0f)
                    .height(38.dp)
                    .clip(GymCoachShapes.xs)
                    .background(GymCoachColors.SurfaceInput)
                    .border(1.dp, GymCoachColors.BorderSubtle, GymCoachShapes.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (set.reps > 0) onRepsChange(set.reps - 1)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease reps",
                        tint = GymCoachColors.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = repsInput,
                        onValueChange = { newVal ->
                            repsInput = newVal
                            newVal.toIntOrNull()?.let { onRepsChange(it) }
                        },
                        textStyle = TextStyle(
                            color = GymCoachColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(GymCoachColors.Primary),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                    )
                    if (repsInput.isEmpty()) {
                        Text(
                            text = "0",
                            style = TextStyle(
                                color = GymCoachColors.TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }

                IconButton(
                    onClick = {
                        onRepsChange(set.reps + 1)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase reps",
                        tint = GymCoachColors.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Tactile Completion Checkbox Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (set.completed) GymCoachColors.Success else GymCoachColors.SurfaceCardElevated)
                    .border(1.dp, if (set.completed) GymCoachColors.Success else GymCoachColors.BorderSubtle, RoundedCornerShape(8.dp))
                    .clickable(role = Role.Checkbox) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCompleteToggle()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = checkScale.value
                            scaleY = checkScale.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = if (set.completed) "Mark incomplete" else "Mark complete",
                        tint = if (set.completed) Color.White else GymCoachColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Quick Increment & RPE Chips (expandable when tapping weight)
        AnimatedVisibility(
            visible = showQuickChips,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick +:",
                        style = MaterialTheme.typography.labelSmall,
                        color = GymCoachColors.TextSecondary
                    )
                    quickIncrements.forEach { inc ->
                        Surface(
                            onClick = {
                                onWeightChange(set.weight + inc)
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = GymCoachColors.SurfaceCardElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GymCoachColors.BorderSubtle)
                        ) {
                            Text(
                                text = "+$inc",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                color = GymCoachColors.Primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (onRpeChange != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RPE:",
                            style = MaterialTheme.typography.labelSmall,
                            color = GymCoachColors.TextSecondary
                        )
                        listOf(7.0, 8.0, 9.0, 10.0).forEach { rpeVal ->
                            val isSelected = set.rpe == rpeVal
                            Surface(
                                onClick = {
                                    val newRpe = if (isSelected) 0.0 else rpeVal
                                    onRpeChange(newRpe)
                                },
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) GymCoachColors.Primary.copy(alpha = 0.2f) else GymCoachColors.SurfaceCardElevated,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) GymCoachColors.Primary else GymCoachColors.BorderSubtle
                                )
                            ) {
                                Text(
                                    text = if (rpeVal % 1.0 == 0.0) rpeVal.toInt().toString() else rpeVal.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                    color = if (isSelected) GymCoachColors.Primary else GymCoachColors.TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@JvmName("ActiveWorkoutSetRowWithFloatRpe")
@Composable
fun ActiveWorkoutSetRow(
    set: WorkoutSetUiModel,
    previousPerformance: String?,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onRpeChange: (Float?) -> Unit,
    onCompleteToggle: () -> Unit,
    onQuickFill: () -> Unit,
    modifier: Modifier = Modifier
) {
    ActiveWorkoutSetRow(
        set = set,
        previousPerformance = previousPerformance,
        weightUnit = WeightUnit.KG,
        onWeightChange = onWeightChange,
        onRepsChange = onRepsChange,
        onRpeChange = { rpeVal -> onRpeChange(rpeVal.toFloat()) },
        onSetTypeChange = null,
        onCompleteToggle = onCompleteToggle,
        onQuickFill = onQuickFill,
        modifier = modifier
    )
}
