package com.gymcoach.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoCameraBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.core.animation.AnimationRepository
import com.gymcoach.app.core.animation.ExerciseAnimationDefinition
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes

/**
 * Contained modal bottom sheet for inspecting exercise technique and biomechanical animations
 * directly within active workout sessions or library lists without disrupting logging.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTechniqueBottomSheet(
    exercise: Exercise,
    animationRepository: AnimationRepository,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ExerciseTechniqueBottomSheet(
        exerciseName = exercise.name,
        isCustomExercise = exercise.isCustom,
        muscleGroup = exercise.muscleGroup,
        instructions = exercise.instructions,
        setupInstructions = exercise.setupInstructions,
        executionInstructions = exercise.executionInstructions,
        breathingInstructions = exercise.breathingInstructions,
        tempoGuidance = exercise.tempoGuidance,
        commonMistakes = exercise.commonMistakes,
        tips = exercise.tips,
        safetyNotes = exercise.safetyNotes,
        movementPattern = exercise.movementPattern,
        equipment = exercise.equipment,
        difficulty = exercise.difficulty,
        secondaryMuscles = exercise.secondaryMuscles,
        animationRepository = animationRepository,
        onDismiss = onDismiss,
        sheetState = sheetState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTechniqueBottomSheet(
    exerciseName: String,
    isCustomExercise: Boolean = false,
    muscleGroup: String = "",
    instructions: String = "",
    setupInstructions: String = "",
    executionInstructions: String = "",
    breathingInstructions: String = "",
    tempoGuidance: String = "",
    commonMistakes: String = "",
    tips: String = "",
    safetyNotes: String = "",
    movementPattern: String = "",
    equipment: String = "",
    difficulty: String = "",
    secondaryMuscles: String = "",
    animationRepository: AnimationRepository,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var animationDefinition by remember { mutableStateOf<ExerciseAnimationDefinition?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    // Allow user to bypass the unavailability notice to still use camera for rep counting
    var showCameraAnyway by remember { mutableStateOf(false) }

    LaunchedEffect(exerciseName) {
        isLoading = true
        animationDefinition = animationRepository.getAnimation(exerciseName = exerciseName)
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GymCoachColors.SurfaceDeep,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Exercise title, badges, dismiss button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = GymCoachColors.TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (muscleGroup.isNotBlank()) {
                            BadgePill(text = muscleGroup.uppercase(), color = GymCoachColors.Primary)
                        }
                        if (movementPattern.isNotBlank()) {
                            BadgePill(text = movementPattern.replace("_", " ").uppercase(), color = GymCoachColors.CyanAccent)
                        }
                        if (equipment.isNotBlank()) {
                            BadgePill(text = equipment.uppercase(), color = GymCoachColors.TextSecondary)
                        }
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Technique Guide",
                        tint = GymCoachColors.TextSecondary
                    )
                }
            }

            // Animation Player or Loading / Kinetic Biomechanical Card
            // Determine if this exercise supports AI form analysis
            val isFormAnalysisUnsupported = remember(exerciseName, isCustomExercise) {
                isCustomExercise || com.gymcoach.app.core.ml.ExerciseType.fromExerciseName(exerciseName) == null
            }
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GymCoachColors.Primary)
                    }
                }
                isFormAnalysisUnsupported && !showCameraAnyway -> {
                    // "Form Analysis Not Available" notice for custom or unrecognised exercises
                    Surface(
                        shape = GymCoachShapes.md,
                        color = GymCoachColors.SurfaceCardElevated,
                        border = GymCoachBorders.subtle
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoCameraBack,
                                contentDescription = "Form analysis unavailable",
                                tint = GymCoachColors.TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Form Analysis Not Available",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = GymCoachColors.TextPrimary
                            )
                            Text(
                                text = "AI-powered form analysis is available for 9 standard exercises: squats, deadlifts, bench press, overhead press, bent-over rows, bicep curls, push-ups, lateral raises, and planks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GymCoachColors.TextSecondary,
                                lineHeight = 18.sp
                            )
                            TextButton(onClick = { showCameraAnyway = true }) {
                                Text(
                                    "Use Camera for Rep Counting",
                                    color = GymCoachColors.Primary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
                animationDefinition != null -> {
                    ExerciseAnimationPlayer(
                        definition = animationDefinition!!,
                        compact = true,
                        autoPlay = true
                    )
                }
                else -> {
                    // Biomechanical movement overview hero card for exercises without skeletal keyframes
                    Surface(
                        shape = GymCoachShapes.md,
                        color = GymCoachColors.SurfaceCardElevated,
                        border = GymCoachBorders.subtle
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(GymCoachColors.Primary.copy(alpha = 0.18f), shape = GymCoachShapes.sm),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = GymCoachColors.Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "BIOMECHANICAL TARGET",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            letterSpacing = 1.sp
                                        ),
                                        color = GymCoachColors.Primary
                                    )
                                    Text(
                                        text = if (secondaryMuscles.isNotBlank()) {
                                            "Target: ${muscleGroup.ifBlank { "Primary" }} (Secondary: $secondaryMuscles)"
                                        } else {
                                            "Target Muscle: ${muscleGroup.ifBlank { "Primary" }}"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = GymCoachColors.TextPrimary
                                    )
                                }
                            }

                            if (difficulty.isNotBlank() || movementPattern.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (difficulty.isNotBlank()) {
                                        Text(
                                            text = "Level: ${difficulty.replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GymCoachColors.TextSecondary
                                        )
                                    }
                                    if (movementPattern.isNotBlank()) {
                                        Text(
                                            text = "• Pattern: ${movementPattern.replace("_", " ")}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GymCoachColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Resolved Setup Instructions
            val resolvedSetup = setupInstructions.ifBlank {
                deriveSetupCue(muscleGroup, movementPattern, equipment)
            }
            if (resolvedSetup.isNotBlank()) {
                TechniqueSectionCard(
                    icon = Icons.Default.CheckCircle,
                    iconTint = GymCoachColors.Success,
                    title = "Setup & Posture",
                    content = resolvedSetup
                )
            }

            // Resolved Execution Instructions
            val resolvedExecution = executionInstructions.ifBlank {
                instructions.ifBlank {
                    deriveExecutionCue(muscleGroup, movementPattern)
                }
            }
            if (resolvedExecution.isNotBlank()) {
                TechniqueSectionCard(
                    icon = Icons.Default.PlayArrow,
                    iconTint = GymCoachColors.CyanAccent,
                    title = "Execution & Form Cues",
                    content = resolvedExecution
                )
            }

            // Breathing & Tempo
            val resolvedBreathing = breathingInstructions.ifBlank { deriveBreathingCue(movementPattern) }
            val resolvedTempo = tempoGuidance.ifBlank { "3-0-1-0 (3s controlled eccentric, 1s explosive drive)" }
            TechniqueSectionCard(
                icon = Icons.Default.Air,
                iconTint = GymCoachColors.Primary,
                title = "Breathing & Tempo",
                content = "$resolvedBreathing\n\nTempo: $resolvedTempo"
            )

            // Common Mistakes & Faults
            val resolvedMistakes = commonMistakes.ifBlank {
                deriveMistakesCue(muscleGroup, movementPattern)
            }
            if (resolvedMistakes.isNotBlank()) {
                Surface(
                    shape = GymCoachShapes.md,
                    color = GymCoachColors.SurfaceCardElevated,
                    border = BorderStroke(1.dp, GymCoachColors.Warning.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = GymCoachColors.Warning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Common Faults to Avoid",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = GymCoachColors.Warning
                            )
                        }
                        Text(
                            text = resolvedMistakes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GymCoachColors.TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Pro Coach Tips
            if (tips.isNotBlank()) {
                TechniqueSectionCard(
                    icon = Icons.Default.Info,
                    iconTint = GymCoachColors.GoldAccent,
                    title = "Pro Coach Tips",
                    content = tips
                )
            }

            // Safety Notes & Joint Alignment
            if (safetyNotes.isNotBlank()) {
                TechniqueSectionCard(
                    icon = Icons.Default.Shield,
                    iconTint = GymCoachColors.GoldAccent,
                    title = "Safety & Joint Alignment",
                    content = safetyNotes
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BadgePill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(GymCoachShapes.xs)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            ),
            color = color
        )
    }
}

@Composable
private fun TechniqueSectionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    content: String
) {
    Surface(
        shape = GymCoachShapes.md,
        color = GymCoachColors.SurfaceCard,
        border = BorderStroke(1.dp, GymCoachColors.BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = GymCoachColors.TextPrimary
                )
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = GymCoachColors.TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

private fun deriveSetupCue(muscleGroup: String, pattern: String, equipment: String): String {
    val mg = muscleGroup.lowercase()
    val eq = equipment.lowercase()
    val pat = pattern.lowercase()
    return when {
        mg.contains("chest") || pat.contains("push") -> {
            if (eq.contains("dumbbell")) {
                "Sit upright on bench, kick dumbbells to shoulders, and lie back with feet flat on the floor. Retract scapulae and stabilize shoulder girdle."
            } else {
                "Plant feet flat on floor. Retract and depress shoulder blades to establish an arched base. Grip with thumbs wrapped securely."
            }
        }
        mg.contains("back") || pat.contains("pull") -> {
            "Set a hip-width or shoulder-width stance. Brace core, engage lats, and pull shoulders down away from ears before initiating."
        }
        mg.contains("leg") || mg.contains("quad") || mg.contains("glute") || pat.contains("squat") -> {
            "Position feet shoulder-width apart with toes flared 15-30°. Brace abdominal wall and establish full three-point foot contact (heel, big toe, pinky toe)."
        }
        mg.contains("shoulder") -> {
            "Brace core and glutes to prevent lumbar hyperextension. Position elbows slightly in front of shoulders in the scapular plane."
        }
        mg.contains("arm") || mg.contains("bicep") || mg.contains("tricep") -> {
            "Pin elbows to your ribcage. Maintain neutral wrists and an upright posture without swinging."
        }
        else -> "Establish a balanced stance with core braced and spine neutral before beginning repetitions."
    }
}

private fun deriveExecutionCue(muscleGroup: String, pattern: String): String {
    val pat = pattern.lowercase()
    val mg = muscleGroup.lowercase()
    return when {
        pat.contains("push") || mg.contains("chest") -> "Lower under control keeping elbows at approximately 45-75° from your torso. Drive upward explosively without unlocking shoulder blades."
        pat.contains("pull") || mg.contains("back") -> "Initiate pull with the lats and elbows, squeezing shoulder blades together at peak contraction. Return with a controlled 2-3s negative stretch."
        pat.contains("squat") || mg.contains("quad") -> "Descend by breaking at hips and knees simultaneously. Push knees outward tracking toes. Drive through mid-foot to standing lockout."
        pat.contains("hinge") || mg.contains("hamstring") -> "Hinge back at the hips keeping shins vertical and spine flat. Squeeze glutes and thrust hips forward to full lockout."
        else -> "Perform each repetition with controlled eccentric cadence and powerful, intentional concentric contraction."
    }
}

private fun deriveBreathingCue(pattern: String): String {
    val pat = pattern.lowercase()
    return if (pat.contains("isometric") || pat.contains("hold") || pat.contains("plank")) {
        "Maintain steady, controlled diaphragmatic breaths throughout the hold without releasing intra-abdominal core engagement."
    } else {
        "Inhale deeply into your belly and brace intra-abdominal pressure during the eccentric phase. Exhale forcefully through the sticking point of the concentric drive."
    }
}

private fun deriveMistakesCue(muscleGroup: String, pattern: String): String {
    val mg = muscleGroup.lowercase()
    val pat = pattern.lowercase()
    return when {
        mg.contains("chest") || pat.contains("push") -> "• Flaring elbows out 90° (places high shear stress on shoulders)\n• Bouncing weights off sternum\n• Lifting glutes or feet off the bench/floor"
        mg.contains("back") || pat.contains("pull") -> "• Using momentum or torso swing to yank the weight\n• Shrugging traps upward instead of pulling down with lats\n• Rounding the lumbar spine during rows"
        mg.contains("leg") || pat.contains("squat") -> "• Knees caving inward (valgus collapse)\n• Rising onto toes and letting heels lift\n• Rounding lower back at depth (butt wink)"
        mg.contains("hinge") || mg.contains("hamstring") -> "• Rounding lower back during the lift\n• Letting barbell or dumbbells drift away from legs\n• Hyperextending lower back at top lockout"
        mg.contains("shoulder") -> "• Excessive lower-back arching to press weight\n• Flaring elbows backward outside the scapular plane\n• Cutting range of motion short"
        else -> "• Rushing repetitions without full range of motion\n• Sacrificing joint alignment for heavier load\n• Inconsistent breathing and lack of core bracing"
    }
}
