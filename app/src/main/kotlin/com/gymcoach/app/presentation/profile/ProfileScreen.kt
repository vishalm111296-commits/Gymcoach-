package com.gymcoach.app.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.domain.repository.UserProfileRepository
import com.gymcoach.app.presentation.components.PremiumEmptyState
import com.gymcoach.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfileEntity?>(null)
    val profile: StateFlow<UserProfileEntity?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _profile.value = userProfileRepository.getLatestProfile().first()
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("PROFILE & SETTINGS", fontWeight = FontWeight.Black, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            }
            profile == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    PremiumEmptyState("NO PROFILE FOUND", "Please complete onboarding first.")
                }
            }
            else -> {
                val p = profile!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "PERSONAL DATA",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextTertiary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            ProfileRow(icon = Icons.Default.Cake, label = "Age", value = p.age.toString())
                            Divider()
                            ProfileRow(icon = Icons.Default.MonitorWeight, label = "Weight", value = "${p.weightKg} kg")
                            Divider()
                            ProfileRow(icon = Icons.Default.Height, label = "Height", value = "${p.heightCm} cm")
                            Divider()
                            ProfileRow(icon = Icons.Default.SelfImprovement, label = "Sex", value = p.sex)
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Text(
                        text = "TRAINING PREFERENCES",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextTertiary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            ProfileRow(icon = Icons.Default.FitnessCenter, label = "Goal", value = p.goal)
                            Divider()
                            ProfileRow(icon = Icons.Default.School, label = "Experience", value = p.experience)
                            Divider()
                            ProfileRow(icon = Icons.Default.DirectionsRun, label = "Days/Week", value = p.trainingDaysPerWeek.toString())
                            Divider()
                            ProfileRow(icon = Icons.Default.Timer, label = "Schedule", value = p.preferredSchedule)
                            Divider()
                            ProfileRow(icon = Icons.Default.Info, label = "Limitations", value = p.limitationsPreferences)
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Text(
                        text = "ABOUT GYMCOACH",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextTertiary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "OFFLINE-FIRST ARCHITECTURE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Rule-based fitness coach with smart program generation, V-taper optimization, and progressive overload tracking. Local database ensures 100% offline functionality. No artificial intelligence is used to fake recommendations.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = DarkSurfaceVariant)
}
