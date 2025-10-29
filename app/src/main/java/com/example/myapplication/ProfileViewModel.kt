package com.example.myapplication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel(assistedFactory = ProfileViewModel.ProfileViewModelFactory::class)
class ProfileViewModel @AssistedInject constructor(savedStateHandle: SavedStateHandle, @Assisted val id: String) : ViewModel() {

    init {
        println("ProfileViewModel contructor")
    }

    // Private mutable state flow to hold the UI state
    private val _uiState = MutableStateFlow(id)

    // Public immutable state flow that composables can collect
    val uiState = _uiState.asStateFlow()

    @AssistedFactory
    interface ProfileViewModelFactory {
        fun create(id: String): ProfileViewModel
    }
}