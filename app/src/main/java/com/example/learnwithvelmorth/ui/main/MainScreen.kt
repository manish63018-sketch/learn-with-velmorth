package com.example.learnwithvelmorth.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.learnwithvelmorth.theme.LearnWithVelmorthTheme

/** Placeholder main screen — navigation is handled by Navigation.kt */
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    // Navigation is managed centrally via MainNavigation() in Navigation.kt.
    // This composable is reserved for future shell/scaffold usage.
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    LearnWithVelmorthTheme { MainScreen() }
}
