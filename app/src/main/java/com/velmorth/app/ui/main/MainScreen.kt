package com.velmorth.app.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.velmorth.app.theme.LearnWithVelmorthTheme

/** Main screen displaying a list of strings. */
@Composable
fun MainScreen(data: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        data.forEach { item ->
            Text(text = "Hello $item!")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    LearnWithVelmorthTheme {
        MainScreen(data = listOf("Sample 1", "Sample 2"))
    }
}
