package com.deepkush.reprange.ui.screens.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Placeholder for Task 4 wizard. Task 3 only needs the route to exist. */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Onboarding coming soon", style = MaterialTheme.typography.titleMedium)
    }
}
