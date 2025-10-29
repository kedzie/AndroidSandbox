package com.example.myapplication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val profile = savedStateHandle.toRoute<Route.Profile>()

    // Private mutable state flow to hold the UI state
    private val _uiState = MutableStateFlow(profile.id)

    // Public immutable state flow that composables can collect
    val uiState = _uiState.asStateFlow()
}