package com.gymcoach.app.presentation.vshape.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.presentation.vshape.VShapeViewModel

@Composable
fun VShapeProgressScreen(
    onNavigateBack: () -> Unit,
    viewModel: VShapeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("V-Shape Progress") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress Overview Card
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "30-Day Challenge",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Track your V-Shape transformation journey",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            // Progress Stats Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Days Completed Card
                ProgressStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Days Completed",
                    value = uiState.completedDays.toString(),
                    subtitle = "out of 30",
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Current Ratio Card
                ProgressStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Current Ratio",
                    value = String.format("%.2f", uiState.ratio),
                    subtitle = "Shoulder ÷ Waist",
                    color = when (uiState.ratio) {
                        >= 1.5f -> MaterialTheme.colorScheme.primary
                        >= 1.3f -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                
                // Streak Card
                ProgressStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Current Streak",
                    value = (uiState.currentChallenge?.streak ?: 0).toString(),
                    subtitle = "consecutive days",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            // Completion Timeline
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Completion Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Timeline visualization
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (day in 1..30) {
                            TimelineDay(
                                day = day,
                                completed = uiState.challengeCompletions.any { it.day == day && it.completed },
                                isCurrent = day == uiState.currentChallenge?.currentDay,
                                isRestDay = isRestDay(day)
                            )
                        }
                    }
                }
            }
            
            // Volume Progress (Placeholder)
            uiState.weeklyVolume?.let { volumeData ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Volume Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            volumeData.forEach { (muscle, volume) ->
                                VolumeProgressBar(muscle, volume, volume * 10) // Estimate weekly target
                            }
                        }
                    }
                }
            }
            
            // Goal Achievement
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Goal Progress",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = "Complete 30 consecutive days to achieve your V-Shape goals!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimelineDay(
    day: Int,
    completed: Boolean,
    isCurrent: Boolean,
    isRestDay: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day number
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        
        // Status indicator
        if (isRestDay) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Rest day",
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF4CAF50)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        if (completed) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                        shape = CircleShape
                    )
            )
        }
        
        // Day type
        Text(
            text = if (isRestDay) "REST" else if (completed) "✓" else "",
            style = MaterialTheme.typography.bodySmall,
            color = if (isRestDay) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Spacer for visual balance
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun VolumeProgressBar(
    muscle: String,
    currentVolume: Float,
    targetVolume: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = muscle.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${currentVolume.toInt()}/${target.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        LinearProgress(
            progress = { currentVolume / targetVolume.takeIf { it > 0 } ?: 1f },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun isRestDay(day: Int): Boolean {
    // Rest days on Sundays (7, 14, 21, 28)
    return day % 7 == 0
}