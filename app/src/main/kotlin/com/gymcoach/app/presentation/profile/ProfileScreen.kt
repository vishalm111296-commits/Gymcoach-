package com.gymcoach.app.presentation.profile

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import com.gymcoach.app.ui.GymCoachBottomNav
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val bodyMeasurementDao: com.gymcoach.app.data.local.dao.BodyMeasurementDao
) : ViewModel() {

    constructor(userProfileRepository: UserProfileRepository) : this(
        userProfileRepository,
        object : com.gymcoach.app.data.local.dao.BodyMeasurementDao {
            override suspend fun insert(measurement: com.gymcoach.app.data.local.entity.BodyMeasurementEntity): Long = 0L
            override suspend fun insertAll(measurements: List<com.gymcoach.app.data.local.entity.BodyMeasurementEntity>) {}
            override suspend fun update(measurement: com.gymcoach.app.data.local.entity.BodyMeasurementEntity): Int = 0
            override fun getAll() = kotlinx.coroutines.flow.flowOf(emptyList<com.gymcoach.app.data.local.entity.BodyMeasurementEntity>())
            override fun getLatest() = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun getById(id: Long): com.gymcoach.app.data.local.entity.BodyMeasurementEntity? = null
            override suspend fun deleteById(id: Long): Int = 0
        }
    )

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
            _profile.value = userProfileRepository.getLatestProfile().firstOrNull()
            _isLoading.value = false
        }
    }

    fun updateProfile(
        age: Int,
        sex: String,
        heightCm: Double,
        weightKg: Double,
        trainingDays: Int,
        sessionLength: Int,
        experience: String,
        goal: String,
        equipment: String
    ) {
        viewModelScope.launch {
            val current = _profile.value ?: UserProfileEntity()
            val updated = current.copy(
                age = age,
                sex = sex,
                heightCm = heightCm,
                weightKg = weightKg,
                trainingDaysPerWeek = trainingDays,
                sessionLengthMinutes = sessionLength,
                experience = experience,
                goal = goal,
                equipmentType = equipment
            )
            userProfileRepository.saveProfile(updated)
            if (weightKg > 0) {
                bodyMeasurementDao.insert(
                    com.gymcoach.app.data.local.entity.BodyMeasurementEntity(
                        weightKg = weightKg,
                        notes = "Profile update"
                    )
                )
            }
            load()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onNavigateBottomBar: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showEditSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            GymCoachBottomNav(currentRoute = "profile", onNavigate = onNavigateBottomBar)
        },
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEditSheet = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
            profile == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No profile found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val p = profile!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(8.dp))

                    // Profile header
                    Text(
                        text = "Your Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Collected during onboarding",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(24.dp))

                    // Personal Info
                    SectionHeader("Personal")
                    Spacer(Modifier.height(8.dp))
                    ProfileInfoRow(icon = Icons.Default.Cake, label = "Age", value = "${p.age} years")
                    ProfileInfoRow(icon = Icons.Default.SelfImprovement, label = "Sex", value = p.sex.ifBlank { "Not specified" })
                    ProfileInfoRow(icon = Icons.Default.Height, label = "Height", value = if (p.heightCm > 0) "%.0f cm".format(p.heightCm) else "Not specified")
                    ProfileInfoRow(icon = Icons.Default.MonitorWeight, label = "Weight", value = if (p.weightKg > 0) "%.1f kg".format(p.weightKg) else "Not specified")

                    Spacer(Modifier.height(24.dp))

                    // Training Info
                    SectionHeader("Training")
                    Spacer(Modifier.height(8.dp))
                    ProfileInfoRow(icon = Icons.Default.FitnessCenter, label = "Experience", value = p.experience.ifBlank { "Not specified" })
                    ProfileInfoRow(icon = Icons.Default.FitnessCenter, label = "Training Days/Week", value = "${p.trainingDaysPerWeek}")
                    ProfileInfoRow(icon = Icons.Default.Timer, label = "Session Length", value = "${p.sessionLengthMinutes} minutes")
                    ProfileInfoRow(icon = Icons.AutoMirrored.Filled.DirectionsRun, label = "Goal", value = p.goal.ifBlank { "Not specified" })

                    Spacer(Modifier.height(24.dp))

                    // Equipment
                    SectionHeader("Equipment")
                    Spacer(Modifier.height(8.dp))
                    ProfileInfoRow(icon = Icons.Default.FitnessCenter, label = "Equipment", value = p.equipmentType.ifBlank { "Not specified" })

                    if (p.preferredExercises.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        ProfileInfoRow(icon = Icons.Default.FitnessCenter, label = "Preferred Exercises", value = p.preferredExercises)
                    }

                    if (p.exercisesToAvoid.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        ProfileInfoRow(icon = Icons.Default.FitnessCenter, label = "Exercises to Avoid", value = p.exercisesToAvoid)
                    }

                    Spacer(Modifier.height(32.dp))

                    // Data & Backup
                    SectionHeader("Data & Export")
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Workout Data Portability",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Export your completed workouts in standard Strong/Hevy CSV format or complete JSON backup directly from the Workout History screen.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // About
                    SectionHeader("About GymCoach")
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "GymCoach v1.0",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Rule-based fitness coach with smart program generation, V-taper optimization, and progressive overload tracking. Exercise substitutions use deterministic matching, not AI.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }

    if (showEditSheet && profile != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = sheetState
        ) {
            EditProfileBottomSheet(
                profile = profile!!,
                onDismiss = { showEditSheet = false },
                onSave = { a, s, h, w, td, sl, e, g, eq ->
                    viewModel.updateProfile(a, s, h, w, td, sl, e, g, eq)
                }
            )
        }
    }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileBottomSheet(
    profile: UserProfileEntity,
    onDismiss: () -> Unit,
    onSave: (Int, String, Double, Double, Int, Int, String, String, String) -> Unit
) {
    var age by rememberSaveable { mutableStateOf(profile.age.toString()) }
    var height by rememberSaveable { mutableStateOf(if (profile.heightCm > 0) profile.heightCm.toString() else "") }
    var weight by rememberSaveable { mutableStateOf(if (profile.weightKg > 0) profile.weightKg.toString() else "") }
    var trainingDays by rememberSaveable { mutableStateOf(profile.trainingDaysPerWeek.toString()) }
    var sessionLength by rememberSaveable { mutableStateOf(profile.sessionLengthMinutes.toString()) }
    var goal by rememberSaveable { mutableStateOf(if (profile.goal.isNotBlank()) profile.goal else "Hypertrophy") }
    var equipment by rememberSaveable { mutableStateOf(if (profile.equipmentType.isNotBlank()) profile.equipmentType else "gym") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Edit Profile",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = trainingDays,
                onValueChange = { trainingDays = it },
                label = { Text("Days/Week") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = sessionLength,
                onValueChange = { sessionLength = it },
                label = { Text("Minutes/Session") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Text("Primary Goal", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("Hypertrophy", "Strength", "Fat Loss", "Endurance").forEach { g ->
                FilterChip(
                    selected = goal == g,
                    onClick = { goal = g },
                    label = { Text(g) }
                )
            }
        }

        Text("Available Equipment", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = equipment == "gym", onClick = { equipment = "gym" }, label = { Text("Full Gym") })
            FilterChip(selected = equipment == "home", onClick = { equipment = "home" }, label = { Text("Dumbbells/Bands") })
            FilterChip(selected = equipment == "custom", onClick = { equipment = "custom" }, label = { Text("Bodyweight") })
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    onSave(
                        age.toIntOrNull() ?: profile.age,
                        profile.sex,
                        height.toDoubleOrNull() ?: profile.heightCm,
                        weight.toDoubleOrNull() ?: profile.weightKg,
                        trainingDays.toIntOrNull() ?: profile.trainingDaysPerWeek,
                        sessionLength.toIntOrNull() ?: profile.sessionLengthMinutes,
                        profile.experience,
                        goal,
                        equipment
                    )
                    onDismiss()
                }
            ) {
                Text("Save Profile")
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
