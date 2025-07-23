package org.example.project.presentation.features.dogGardens

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.project.presentation.features.BaseViewModel


class dogGardensViewModel: BaseViewModel() {
    private val _uiState:MutableStateFlow<dogGardensState> = MutableStateFlow(dogGardensState.Loading)
    val uiState:StateFlow<dogGardensState> get() = _uiState
}