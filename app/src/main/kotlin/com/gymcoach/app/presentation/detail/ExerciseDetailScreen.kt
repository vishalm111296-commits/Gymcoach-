package com.gymcoach.app.presentation.detail

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.animation.AnimationPhase
import com.gymcoach.app.core.animation.AnimationRepository
import com.gymcoach.app.core.animation.ExerciseAnimationDefinition
import com.gymcoach.app.core.exercise.SubstitutionEngine
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.presentation.components.ExerciseAnimationPlayer
import com.gymcoach.app.presentation.components.ExerciseVideoPlayer
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val substitutionEngine: SubstitutionEngine,
    private val animationRepository: AnimationRepository
) : ViewModel() {

    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise: StateFlow<Exercise?> = _exercise.asStateFlow()

    private val _animationDefinition = MutableStateFlow<ExerciseAnimationDefinition?>(null)
    val animationDefinition: StateFlow<ExerciseAnimationDefinition?> = _animationDefinition.asStateFlow()

    private val _substitutes = MutableStateFlow<List<SubstitutionEngine.SubstitutionResult>>(emptyList())
    val substitutes: StateFlow<List<SubstitutionEngine.SubstitutionResult>> = _substitutes.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    fun loadExercise(id: Long) {
        viewModelScope.launch {
            try {
                repository.getExerciseById(id).collect { ex ->
                    _exercise.value = ex
                    ex?.let {
                        _isFavorite.value = it.isFavorite
                        loadSubstitutes(it)
                        loadAnimation(it)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Graceful degradation on read failure
            }
        }
    }

    private suspend fun loadAnimation(exercise: Exercise) {
        try {
            val anim = animationRepository.getAnimation(
                exerciseId = exercise.animationUrl ?: "",
                exerciseName = exercise.name
            )
            _animationDefinition.value = anim
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _animationDefinition.value = null
        }
    }

    private suspend fun loadSubstitutes(exercise: Exercise) {
        try {
            val results = substitutionEngine.findSubstitutes(
                exerciseId = exercise.id,
                equipmentType = exercise.equipment,
                maxResults = 5
            )
            _substitutes.value = results
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _substitutes.value = emptyList()
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val ex = _exercise.value ?: return@launch
            val updated = ex.copy(isFavorite = !ex.isFavorite)
            try {
                repository.updateExercise(updated)
                _isFavorite.value = updated.isFavorite
                _exercise.value = updated
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep existing favorite state on failure
            }
        }
    }
}

/**
 * Animated muscle group badge with an ambient neon highlight glow aura.
 */
@Composable
private fun MuscleGroupHighlightBadge(
    muscleGroup: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "muscleGlowTransition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val glowSpread by infiniteTransition.animateFloat(
        initialValue = 1.5f,
        targetValue = 5.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowSpread"
    )

    Box(
        modifier = modifier
            .drawBehind {
                val cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                // Outer ambient glow
                drawRoundRect(
                    color = GymCoachColors.Primary.copy(alpha = glowAlpha * 0.35f),
                    cornerRadius = cornerRadius,
                    size = androidx.compose.ui.geometry.Size(
                        size.width + glowSpread * 2,
                        size.height + glowSpread * 2
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(-glowSpread, -glowSpread)
                )
                // Badge background fill
                drawRoundRect(
                    color = GymCoachColors.Primary.copy(alpha = 0.12f + glowAlpha * 0.08f),
                    cornerRadius = cornerRadius,
                    size = size
                )
                // Badge border stroke
                drawRoundRect(
                    color = GymCoachColors.Primary.copy(alpha = 0.35f + glowAlpha * 0.65f),
                    cornerRadius = cornerRadius,
                    size = size,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
                )
            }
            .clip(GymCoachShapes.xs)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        alpha = glowAlpha
                    }
                    .clip(CircleShape)
                    .background(GymCoachColors.Primary)
            )
            Text(
                text = muscleGroup,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = GymCoachColors.Primary
            )
        }
    }
}

/**
 * Animated biomechanical phase badges with color transitions, spring scale,
 * sliding indicator dot, and interactive phase coaching cues.
 */
@Composable
private fun AnimatedPhaseBadgesSection() {
    val phases = listOf(
        Triple(
            AnimationPhase.SETUP,
            "Setup",
            "Establish solid foot placement, abdominal brace, and joint alignment before commencing tension."
        ),
        Triple(
            AnimationPhase.ECCENTRIC,
            "Eccentric",
            "Control the 2–3s lowering phase under active muscular load. Avoid dropping weights abruptly."
        ),
        Triple(
            AnimationPhase.BOTTOM,
            "Bottom",
            "Hold active stretch without losing tension or bouncing at end-range joint positions."
        ),
        Triple(
            AnimationPhase.CONCENTRIC,
            "Concentric",
            "Drive powerfully through the concentric phase while exhaling steadily through the sticking point."
        ),
        Triple(
            AnimationPhase.END,
            "Lockout",
            "Achieve peak contraction and squeeze target muscles without hyperextending joints."
        )
    )

    var activePhase by rememberSaveable { mutableStateOf(AnimationPhase.ECCENTRIC) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.md)
            .border(GymCoachBorders.subtle, GymCoachShapes.md),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Biomechanical Phase Breakdown",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = GymCoachColors.Primary
                    )
                )
                Text(
                    text = "Tap phase to inspect",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = GymCoachColors.TextMuted
                    )
                )
            }

            // Phase badges row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                phases.forEach { (phase, label, _) ->
                    val isSelected = activePhase == phase

                    val phaseColor = when (phase) {
                        AnimationPhase.SETUP -> GymCoachColors.PhaseSetup
                        AnimationPhase.ECCENTRIC -> GymCoachColors.PhaseEccentric
                        AnimationPhase.BOTTOM -> GymCoachColors.PhaseBottom
                        AnimationPhase.CONCENTRIC -> GymCoachColors.PhaseConcentric
                        AnimationPhase.END -> GymCoachColors.PhaseEnd
                        else -> GymCoachColors.Primary
                    }

                    val badgeBg by animateColorAsState(
                        targetValue = if (isSelected) phaseColor.copy(alpha = 0.22f) else GymCoachColors.SurfaceDeep,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "phaseBadgeBg"
                    )
                    val badgeBorder by animateColorAsState(
                        targetValue = if (isSelected) phaseColor else GymCoachColors.BorderSubtle,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "phaseBadgeBorder"
                    )
                    val badgeText by animateColorAsState(
                        targetValue = if (isSelected) phaseColor else GymCoachColors.TextSecondary,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "phaseBadgeText"
                    )
                    val badgeScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "phaseBadgeScale"
                    )

                    Surface(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = badgeScale
                                scaleY = badgeScale
                            }
                            .clip(GymCoachShapes.pill)
                            .clickable { activePhase = phase }
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                        shape = GymCoachShapes.pill,
                        color = badgeBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = if (isSelected) 12.dp else 10.dp,
                                vertical = 6.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(animationSpec = tween(150)) + expandHorizontally(),
                                exit = fadeOut(animationSpec = tween(100)) + shrinkHorizontally()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(phaseColor)
                                )
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = badgeText
                            )
                        }
                    }
                }
            }

            // Animated cue description card for active phase
            AnimatedContent(
                targetState = activePhase,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "phaseCueAnim"
            ) { selected ->
                val activeInfo = phases.firstOrNull { it.first == selected }
                if (activeInfo != null) {
                    val phaseColor = when (selected) {
                        AnimationPhase.SETUP -> GymCoachColors.PhaseSetup
                        AnimationPhase.ECCENTRIC -> GymCoachColors.PhaseEccentric
                        AnimationPhase.BOTTOM -> GymCoachColors.PhaseBottom
                        AnimationPhase.CONCENTRIC -> GymCoachColors.PhaseConcentric
                        AnimationPhase.END -> GymCoachColors.PhaseEnd
                        else -> GymCoachColors.Primary
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GymCoachShapes.sm)
                            .background(phaseColor.copy(alpha = 0.08f))
                            .border(1.dp, phaseColor.copy(alpha = 0.25f), GymCoachShapes.sm)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(phaseColor)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "${activeInfo.second} Execution Cue",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = phaseColor
                            )
                            Text(
                                text = activeInfo.third,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    lineHeight = 18.sp,
                                    fontSize = 12.sp
                                ),
                                color = GymCoachColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    onBackClick: () -> Unit,
    onExerciseClick: (Long) -> Unit = {},
    onViewProgressClick: (exerciseId: Long, exerciseName: String) -> Unit = { _, _ -> },
    onCameraClick: (com.gymcoach.app.core.ml.ExerciseType) -> Unit = {},
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val exercise by viewModel.exercise.collectAsState()
    val animationDefinition by viewModel.animationDefinition.collectAsState()
    val substitutes by viewModel.substitutes.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    val favTint by animateColorAsState(
        targetValue = if (isFavorite) Color(0xFFF43F5E) else GymCoachColors.TextSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "favTint"
    )
    val favScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "favScale"
    )

    Scaffold(
        containerColor = GymCoachColors.PureDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = exercise?.name ?: "Exercise Detail",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GymCoachColors.TextPrimary,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GymCoachColors.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                            tint = favTint,
                            modifier = Modifier.graphicsLayer {
                                scaleX = favScale
                                scaleY = favScale
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GymCoachColors.PureDark)
            )
        }
    ) { padding ->
        exercise?.let { ex ->
            // Smooth animated entry for exercise media and diagram
            var mediaVisible by remember(ex.id) { mutableStateOf(false) }
            LaunchedEffect(ex.id) {
                mediaVisible = true
            }

            val mediaAlpha by animateFloatAsState(
                targetValue = if (mediaVisible) 1f else 0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "mediaAlpha"
            )
            val mediaScale by animateFloatAsState(
                targetValue = if (mediaVisible) 1f else 0.93f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "mediaScale"
            )
            val mediaTranslationY by animateFloatAsState(
                targetValue = if (mediaVisible) 0f else 24f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "mediaTranslationY"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Titles with Muscle Group Highlight Glow
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = ex.name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = GymCoachColors.TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Muscle group badge with glowing animated aura
                        MuscleGroupHighlightBadge(muscleGroup = ex.muscleGroup)

                        Text(
                            text = "•",
                            color = GymCoachColors.TextMuted
                        )
                        Text(
                            text = ex.equipment.ifBlank { "Bodyweight" },
                            style = MaterialTheme.typography.bodySmall,
                            color = GymCoachColors.TextSecondary
                        )
                        if (ex.movementPattern.isNotBlank()) {
                            Text(text = "•", color = GymCoachColors.TextMuted)
                            Text(
                                text = ex.movementPattern,
                                style = MaterialTheme.typography.bodySmall,
                                color = GymCoachColors.TextSecondary
                            )
                        }
                    }
                }

                // 1. HERO MEDIA SECTION: Animation, Video, or Stylized Anatomy Glyph Card
                // Strictly guard: Never show a video player with 0:00 / 0:00 when video is absent/invalid!
                val hasPlayableVideo = !ex.videoUrl.isNullOrBlank() &&
                        (ex.videoUrl.startsWith("http://") || ex.videoUrl.startsWith("https://") || ex.videoUrl.startsWith("android.resource://"))
                val hasAnimation = animationDefinition != null

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = mediaAlpha
                            scaleX = mediaScale
                            scaleY = mediaScale
                            translationY = mediaTranslationY
                        }
                ) {
                    if (hasPlayableVideo && hasAnimation) {
                        var showVideo by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GymCoachShapes.lg)
                                .border(GymCoachBorders.subtle, GymCoachShapes.lg),
                            colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceDeep)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { showVideo = !showVideo }) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircleOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = GymCoachColors.Primary
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (showVideo) "View Stickman Form Animation" else "Watch Video Demo",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = GymCoachColors.Primary
                                        )
                                    }
                                }
                                AnimatedContent(
                                    targetState = showVideo,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(150))
                                    },
                                    label = "videoSwitchAnim"
                                ) { isVideo ->
                                    if (isVideo) {
                                        ExerciseVideoPlayer(
                                            videoUri = Uri.parse(ex.videoUrl),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(GymCoachShapes.md)
                                        )
                                    } else {
                                        ExerciseAnimationPlayer(
                                            definition = animationDefinition!!,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(260.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (hasAnimation) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GymCoachShapes.lg)
                                .border(GymCoachBorders.subtle, GymCoachShapes.lg),
                            colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceDeep)
                        ) {
                            ExerciseAnimationPlayer(
                                definition = animationDefinition!!,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .padding(12.dp)
                            )
                        }
                    } else if (hasPlayableVideo) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GymCoachShapes.lg)
                                .border(GymCoachBorders.subtle, GymCoachShapes.lg),
                            colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceDeep)
                        ) {
                            ExerciseVideoPlayer(
                                videoUri = Uri.parse(ex.videoUrl),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(GymCoachShapes.md)
                            )
                        }
                    } else {
                        // Meaningful Exercise Glyph & Anatomy Placeholder (Zero fake 0:00 / 0:00 players!)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GymCoachShapes.lg)
                                .border(GymCoachBorders.subtle, GymCoachShapes.lg),
                            colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(GymCoachColors.Primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = GymCoachColors.Primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Technical Movement Profile",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GymCoachColors.TextPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Biomechanical form notes and coaching cues detailed below",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GymCoachColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                // 2. BIOMECHANICAL PHASE BADGES SECTION
                AnimatedPhaseBadgesSection()

                // 3. PRIMARY ACTIONS ROW (Start Form Check + View Progress)
                val matchedType = com.gymcoach.app.core.ml.ExerciseType.fromExerciseName(ex.name)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (matchedType != null) {
                        Button(
                            onClick = { onCameraClick(matchedType) },
                            modifier = Modifier.weight(1f),
                            shape = GymCoachShapes.md,
                            colors = ButtonDefaults.buttonColors(containerColor = GymCoachColors.Primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Form Check", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { onViewProgressClick(ex.id, ex.name) },
                        modifier = Modifier.weight(1f),
                        shape = GymCoachShapes.md,
                        border = GymCoachBorders.subtle
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = GymCoachColors.TextPrimary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Analytics", color = GymCoachColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                // 4. QUICK SPECIFICATIONS GRID (Target Reps, Rest, Difficulty, Equipment)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GymCoachShapes.md)
                        .border(GymCoachBorders.subtle, GymCoachShapes.md),
                    colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Quick Specifications",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = GymCoachColors.Primary
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SpecBadge(
                                label = "Target Reps",
                                value = ex.recommendedRepRange.ifBlank { "8–12" },
                                icon = Icons.Default.Refresh
                            )
                            SpecBadge(
                                label = "Rest Interval",
                                value = ex.recommendedRestTime.ifBlank { "90s" },
                                icon = Icons.Default.Timer
                            )
                            SpecBadge(
                                label = "Difficulty",
                                value = ex.difficulty.replaceFirstChar { it.uppercase() },
                                icon = Icons.Default.LocalFireDepartment
                            )
                        }
                    }
                }

                // 5. OVERVIEW / DESCRIPTION
                if (ex.description.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GymCoachShapes.md)
                            .border(GymCoachBorders.subtle, GymCoachShapes.md),
                        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Overview",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GymCoachColors.TextPrimary
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = ex.description,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = GymCoachColors.TextSecondary
                            )
                        }
                    }
                }

                // 6. COACHING CUES (Bullet Points with Green Checkmarks)
                val cuesList = ex.tips.split(";").map { it.trim() }.filter { it.isNotBlank() }
                if (cuesList.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GymCoachShapes.md)
                            .border(GymCoachBorders.subtle, GymCoachShapes.md),
                        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Coaching Cues",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GymCoachColors.Success
                                )
                            )
                            cuesList.forEach { cue ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GymCoachColors.Success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                         text = cue,
                                         style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                         color = GymCoachColors.TextPrimary
                                     )
                                 }
                            }
                        }
                    }
                }

                // 7. HOW TO PERFORM / EXECUTION GUIDE
                val setup = ex.setupInstructions.ifBlank { "" }
                val execution = ex.executionInstructions.ifBlank { ex.instructions }
                if (setup.isNotBlank() || execution.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GymCoachShapes.md)
                            .border(GymCoachBorders.subtle, GymCoachShapes.md),
                        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Form & Execution Guide",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GymCoachColors.TextPrimary
                                )
                            )

                            if (setup.isNotBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Setup Position",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GymCoachColors.Primary
                                    )
                                    Text(
                                        text = setup,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                        color = GymCoachColors.TextSecondary
                                    )
                                }
                            }

                            if (execution.isNotBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Rep Execution",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GymCoachColors.Primary
                                    )
                                    Text(
                                        text = execution,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                        color = GymCoachColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // 8. COMMON MISTAKES
                val mistakesList = ex.commonMistakes.split(";").map { it.trim() }.filter { it.isNotBlank() }
                if (mistakesList.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GymCoachShapes.md)
                            .border(GymCoachBorders.subtle, GymCoachShapes.md),
                        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = GymCoachColors.Warning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Common Mistakes to Avoid",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = GymCoachColors.Warning
                                    )
                                )
                            }
                            mistakesList.forEach { mistake ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "•",
                                        color = GymCoachColors.Warning,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = mistake,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                        color = GymCoachColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // 9. TARGET MUSCLES & V-TAPER
                VTaperScoresSection(exercise = ex)

                // 10. SUGGESTED SUBSTITUTIONS
                if (substitutes.isNotEmpty()) {
                    SubstitutionSection(
                        substitutes = substitutes,
                        onExerciseClick = onExerciseClick
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Exercise not found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GymCoachColors.TextSecondary
                )
            }
        }
    }
}

// --- Spec Badge Component ---

@Composable
private fun SpecBadge(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GymCoachColors.TextMuted,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = GymCoachColors.TextMuted
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = GymCoachColors.TextPrimary
        )
    }
}

// --- V-Taper Scores Section with Muscle Highlight Glow ---

@Composable
private fun VTaperScoresSection(exercise: Exercise) {
    val scores = listOf(
        "Lats" to exercise.vtaperLat,
        "Lateral Delt" to exercise.vtaperLateralDelt,
        "Upper Chest" to exercise.vtaperUpperChest,
        "Rear Delt" to exercise.vtaperRearDelt
    ).filter { it.second > 0 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.md)
            .border(GymCoachBorders.subtle, GymCoachShapes.md),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = "Anatomy",
                    tint = GymCoachColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Target Muscles & V-Taper Impact",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = GymCoachColors.TextPrimary
                )
            }

            if (exercise.secondaryMuscles.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Secondary:",
                        style = MaterialTheme.typography.bodySmall,
                        color = GymCoachColors.TextSecondary
                    )
                    exercise.secondaryMuscles.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { secMuscle ->
                        Box(
                            modifier = Modifier
                                .clip(GymCoachShapes.pill)
                                .background(GymCoachColors.SurfaceDeep)
                                .border(1.dp, GymCoachColors.Primary.copy(alpha = 0.35f), GymCoachShapes.pill)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = secMuscle,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = GymCoachColors.TextPrimary
                            )
                        }
                    }
                }
            }

            if (scores.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                scores.forEach { (label, score) ->
                    VTaperBar(label = label, score = score, maxScore = 10)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun VTaperBar(label: String, score: Int, maxScore: Int) {
    val progress = score.toFloat() / maxScore
    val isHighImpact = score >= 7

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "vTaperProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "barGlowTransition")
    val barGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barGlowAlpha"
    )

    val color = when {
        score >= 8 -> GymCoachColors.Success
        score >= 5 -> GymCoachColors.Primary
        else -> GymCoachColors.Warning
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isHighImpact) FontWeight.SemiBold else FontWeight.Normal
            ),
            modifier = Modifier.width(95.dp),
            color = if (isHighImpact) GymCoachColors.TextPrimary else GymCoachColors.TextSecondary
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(CircleShape)
                .then(
                    if (isHighImpact) {
                        Modifier.drawWithContent {
                            drawContent()
                            drawRoundRect(
                                color = color.copy(alpha = barGlowAlpha * 0.7f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                        }
                    } else Modifier
                ),
            color = color,
            trackColor = GymCoachColors.SurfaceDeep
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "$score/$maxScore",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (isHighImpact) color else GymCoachColors.TextPrimary
            ),
            modifier = Modifier.width(36.dp)
        )
    }
}

// --- Substitution Section ---

@Composable
private fun SubstitutionSection(
    substitutes: List<SubstitutionEngine.SubstitutionResult>,
    onExerciseClick: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.md)
            .border(GymCoachBorders.subtle, GymCoachShapes.md),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Substitutions",
                    tint = GymCoachColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Suggested Substitutions",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = GymCoachColors.TextPrimary
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "Targeting identical muscle recruitment with alternate equipment:",
                style = MaterialTheme.typography.bodySmall,
                color = GymCoachColors.TextSecondary
            )

            Spacer(Modifier.height(12.dp))

            substitutes.forEach { result ->
                SubstitutionItem(
                    substitute = result.substitute,
                    score = result.preservationScore,
                    reason = result.reason,
                    onClick = { onExerciseClick(result.substitute.id) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SubstitutionItem(
    substitute: com.gymcoach.app.data.local.entity.ExerciseEntity,
    score: Int,
    reason: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.sm)
            .border(GymCoachBorders.subtle, GymCoachShapes.sm)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceDeep),
        shape = GymCoachShapes.sm
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = substitute.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = GymCoachColors.TextPrimary
                )
                Text(
                    text = "${substitute.muscleGroup} • ${substitute.equipment}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GymCoachColors.TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$score% match",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = GymCoachColors.Primary
                )
                Text(
                    text = reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = GymCoachColors.TextMuted
                )
            }
        }
    }
}
