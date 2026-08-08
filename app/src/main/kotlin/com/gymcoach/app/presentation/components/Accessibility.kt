package com.gymcoach.app.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

fun Modifier.buttonSemantics(
    description: String,
    role: Role = Role.Button
): Modifier = this.semantics {
    this.contentDescription = description
    this.role = role
}

fun Modifier.screenSemantics(
    title: String
): Modifier = this.semantics {
    this.contentDescription = "Screen: $title"
}
