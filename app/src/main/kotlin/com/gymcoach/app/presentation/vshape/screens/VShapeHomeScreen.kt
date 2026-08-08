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
fun VShapeHomeScreen(
    onNavigateToAssessment: () -> Unit,
    onNavigateToChallenge: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToProgress: () -> Unit,
    viewModel: VShapeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("V-Shape Home") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "V-Shape Home Screen - Under Construction",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}