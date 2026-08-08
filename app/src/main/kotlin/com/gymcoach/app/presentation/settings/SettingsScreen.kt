package com.gymcoach.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.util.PlateCalculator
import com.gymcoach.app.presentation.history.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedSection by remember { mutableStateOf<SettingsScreenSection?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        modifier = Modifier.semantics {
                            contentDescription = "App settings"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.height(48.dp).width(48.dp)) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back to previous screen"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
        ) {
            item {
                ThemeSection(
                    isDarkMode = state.isDarkMode,
                    onDarkModeToggle = { viewModel.setDarkMode(it) },
                    isAnimatedTransitions = state.isAnimatedTransitions,
                    onTransitionsToggle = { viewModel.setAnimatedTransitions(it) }
                )
            }

            item {
                Separator()
            }

            item {
                NotificationSection(
                    isNotificationsEnabled = state.isNotificationsEnabled,
                    onNotificationsToggle = { viewModel.setNotificationsEnabled(it) },
                    volumeEnabled = state.volumeEnabled,
                    onVolumeToggle = { viewModel.setVolumeEnabled(it) },
                    vibrationEnabled = state.isVibrationEnabled,
                    onVibrationToggle = { viewModel.setVibrationEnabled(it) },
                    notificationSoundUri = state.notificationSoundUri,
                    onSoundClick = {}
                )
            }

            item {
                Separator()
            }

            item {
                RestTimerSection(
                    defaultRestTimerSeconds = state.defaultRestTimerSeconds,
                    onRestTimerChange = { viewModel.setDefaultRestTimer(it) },
                    isAutoStartRestTimer = state.isAutoStartRestTimer,
                    onAutoStartRestTimerToggle = { viewModel.setAutoStartRestTimer(it) },
                    isVibrationEnabled = state.isVibrationEnabled,
                    onVibrationToggle = { viewModel.setVibrationEnabled(it) }
                )
            }

            item {
                Separator()
            }

            item {
                WorkoutSection(
                    isSetCountEnabled = state.isSetCountEnabled,
                    onSetCountToggle = { viewModel.setSetCountEnabled(it) }
                )
            }

            item {
                Separator()
            }

            item {
                UnitsSection(
                    isMetricUnits = state.isMetricUnits,
                    onMetricUnitsToggle = { viewModel.setMetricUnits(it) }
                )
            }

            item {
                Separator()
            }

            item {
                PlateCalculatorSettings(
                    isMetricUnits = state.isMetricUnits,
                    plateUnit = state.plateUnit,
                    onMetricUnitsToggle = { viewModel.setMetricUnits(it) },
                    onPlateUnitChange = { viewModel.setPlateUnit(it) }
                )
            }

            item {
                Separator()
            }

            item {
                HistorySection(
                    isBackupEnabled = state.isBackupEnabled,
                    onBackupToggle = { viewModel.setBackupEnabled(it) },
                    onBackupClick = {}
                )
            }

            item {
                Separator()
            }

            item {
                DeveloperSection(
                    isDebugMode = state.isDebugMode,
                    onDebugToggle = { viewModel.setDebugMode(it) },
                    onLogsClick = {}
                )
            }

            item {
                Separator()
            }

            item {
                AboutSection(
                    versionName = state.versionName,
                    versionCode = state.versionCode
                )
            }
        }
    }
}

@Composable
private fun Separator() {
    Divider()
}

@Composable
private fun ThemeSection(
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    isAnimatedTransitions: Boolean,
    onTransitionsToggle: (Boolean) -> Unit
) {
    CategorySection(title = "Appearance") {
        SettingsToggleItem(
            title = "Dark Mode",
            icon = Icons.Default.DarkMode,
            description = "Toggle dark theme",
            checked = isDarkMode,
            onCheckedChange = onDarkModeToggle
        )
        SettingsToggleItem(
            title = "Animations",
            icon = Icons.Default.AppRegistration,
            description = "Enable or disable smooth transitions",
            checked = isAnimatedTransitions,
            onCheckedChange = onTransitionsToggle
        )
    }
}

@Composable
private fun NotificationSection(
    isNotificationsEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    volumeEnabled: Boolean,
    onVolumeToggle: (Boolean) -> Unit,
    vibrationEnabled: Boolean,
    onVibrationToggle: (Boolean) -> Unit,
    notificationSoundUri: String?,
    onSoundClick: () -> Unit
) {
    CategorySection(title = "Notifications") {
        SettingsToggleItem(
            title = "Push Notifications",
            icon = Icons.Default.Notifications,
            description = "Receive push notifications",
            checked = isNotificationsEnabled,
            onCheckedChange = onNotificationsToggle
        )
        SettingsToggleItem(
            title = "Sound",
            icon = Icons.Default.VolumeOff,
            description = "Play sound for timer completion",
            checked = volumeEnabled,
            onCheckedChange = onVolumeToggle
        )
        SettingsToggleItem(
            title = "Vibration",
            icon = Icons.Default.Notifications,
            description = "Vibrate on timer completion",
            checked = vibrationEnabled,
            onCheckedChange = onVibrationToggle
        )
    }
}

@Composable
private fun RestTimerSection(
    defaultRestTimerSeconds: Int,
    onRestTimerChange: (Int) -> Unit,
    isAutoStartRestTimer: Boolean,
    onAutoStartRestTimerToggle: (Boolean) -> Unit,
    isVibrationEnabled: Boolean,
    onVibrationToggle: (Boolean) -> Unit
) {
    CategorySection(title = "Rest Timer") {
        SettingsSliderItem(
            title = "Default Duration",
            icon = Icons.Default.RestartAlt,
            value = defaultRestTimerSeconds,
            valueRange = 30f..300f,
            steps = 9,
            onValueChange = { onRestTimerChange(it.toInt()) },
            description = "$defaultRestTimerSeconds seconds"
        )
        SettingsToggleItem(
            title = "Auto-start Timer",
            icon = Icons.Default.RestartAlt,
            description = "Automatically start rest timer after set completion",
            checked = isAutoStartRestTimer,
            onCheckedChange = onAutoStartRestTimerToggle
        )
    }
}

@Composable
private fun WorkoutSection(
    isSetCountEnabled: Boolean,
    onSetCountToggle: (Boolean) -> Unit
) {
    CategorySection(title = "Workout") {
        SettingsToggleItem(
            title = "Set Counter",
            icon = Icons.Default.MenuBook,
            description = "Automatically count and display sets",
            checked = isSetCountEnabled,
            onCheckedChange = onSetCountToggle
        )
    }
}

@Composable
private fun UnitsSection(
    isMetricUnits: Boolean,
    onMetricUnitsToggle: (Boolean) -> Unit
) {
    CategorySection(title = "Units") {
        SettingsToggleItem(
            title = "Metric Units",
            icon = Icons.Default.Scale,
            description = "Use kilograms and meters",
            checked = isMetricUnits,
            onCheckedChange = onMetricUnitsToggle
        )
    }
}

@Composable
private fun PlateCalculatorSettings(
    isMetricUnits: Boolean,
    plateUnit: PlateUnit,
    onMetricUnitsToggle: (Boolean) -> Unit,
    onPlateUnitChange: (PlateUnit) -> Unit
) {
    CategorySection(title = "Plate Calculator") {
        SettingsToggleItem(
            title = "Metric Units",
            icon = Icons.Default.Scale,
            description = "Use kilograms for plate calculation",
            checked = isMetricUnits,
            onCheckedChange = onMetricUnitsToggle
        )
        val unitOptions = listOf(
            PlateUnit.STANDARD_PLATES to "Standard (45kg)",
            PlateUnit.KG_PLATES to "KG (25kg)",
            PlateUnit.OLYMPIC_PLATES to "Olympic (20kg)",
            PlateUnit.METRIC_PLATES to "Metric (12.5kg)"
        )
        val selectedItem = unitOptions.find { it.first == plateUnit }
        SettingsDropdownItem(
            title = "Plate Type",
            icon = Icons.Default.Layers,
            description = "Select your plate type",
            selectedItem = selectedItem?.second,
            options = unitOptions.map { it.second },
            onItemSelected = { selected ->
                onPlateUnitChange(
                    unitOptions.find { it.second == selected }?.first ?: PlateUnit.STANDARD_PLATES
                )
            }
        )
    }
}

@Composable
private fun HistorySection(
    isBackupEnabled: Boolean,
    onBackupToggle: (Boolean) -> Unit,
    onBackupClick: () -> Unit
) {
    CategorySection(title = "History & Backup") {
        SettingsToggleItem(
            title = "Auto Backup",
            icon = Icons.Default.Backup,
            description = "Automatically backup workout history",
            checked = isBackupEnabled,
            onCheckedChange = onBackupToggle
        )
        SettingsButtonItem(
            title = "Manual Backup",
            icon = Icons.Default.AppRegistration,
            description = "Backup your current data manually",
            onClick = onBackupClick
        )
    }
}

@Composable
private fun DeveloperSection(
    isDebugMode: Boolean,
    onDebugToggle: (Boolean) -> Unit,
    onLogsClick: () -> Unit
) {
    CategorySection(title = "Developer") {
        SettingsToggleItem(
            title = "Debug Mode",
            icon = Icons.Default.Code,
            description = "Enable debug logging and options",
            checked = isDebugMode,
            onCheckedChange = onDebugToggle
        )
        SettingsButtonItem(
            title = "View Logs",
            icon = Icons.Default.Terminal,
            description = "Access debug logs",
            onClick = onLogsClick
        )
    }
}

@Composable
private fun AboutSection(
    versionName: String,
    versionCode: Int
) {
    CategorySection(title = "About") {
        SettingsButtonItem(
            title = "Rate App",
            icon = Icons.Default.AppRegistration,
            description = "Rate GymCoach on app store"
        )
        SettingsButtonItem(
            title = "Privacy Policy",
            icon = Icons.Default.Info,
            description = "View privacy policy"
        )
        SettingsButtonItem(
            title = "Terms of Service",
            icon = Icons.Default.Info,
            description = "View terms and conditions"
        )
        SettingsButtonItem(
            title = "Contact Support",
            icon = Icons.Default.Tune,
            description = "Get help from support team"
        )
        SettingsButtonItem(
            title = "Version",
            icon = Icons.Default.Info,
            description = "GymCoach v$versionName",
            enabled = false
        )
    }
}

@Composable
private fun CategorySection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        SubMenu(content = content)
    }
}

@Composable
private fun SubMenu(content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = "${title} icon",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.height(48.dp).width(56.dp)
            )
        }
    )
}

@Composable
private fun SettingsSliderItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    description: String
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = "${title} icon",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Column(
                modifier = Modifier.semantics { contentDescription = "$title: $value seconds" }
            ) {
                Slider(
                    value = value.toFloat(),
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    steps = steps,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
        }
    )
}

@Composable
private fun SettingsDropdownItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selectedItem: String?,
    options: List<String>,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = "${title} icon",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Box(
                modifier = Modifier.wrapContentSize(Alignment.TopEnd)
            ) {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(40.dp).height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Select ${title}",
                        modifier = Modifier.size(24.dp)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    for (option in options) {
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onItemSelected(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun SettingsButtonItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit = {},
    enabled: Boolean = true
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = "${title} icon",
                modifier = Modifier.size(24.dp),
                tint = if (!enabled)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (!enabled)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    )
}