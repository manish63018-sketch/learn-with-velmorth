package com.example.learnwithvelmorth.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnwithvelmorth.data.DataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Success(val data: List<String>) : MainScreenUiState
}

/** ViewModel for the main screen. */
@HiltViewModel
class MainScreenViewModel @Inject constructor(
    dataRepository: DataRepository
) : ViewModel() {
    val uiState: StateFlow<MainScreenUiState> = dataRepository.data
        .map { MainScreenUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainScreenUiState.Loading
        )
}
