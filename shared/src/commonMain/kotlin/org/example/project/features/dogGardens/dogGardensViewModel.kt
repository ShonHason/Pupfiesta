package org.example.project.features.dogGardens

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.project.features.BaseViewModel


class dogGardensViewModel: BaseViewModel() {
    private val _uiState:MutableStateFlow<dogGardensState> = MutableStateFlow(dogGardensState.Loading)
    val uiState:StateFlow<dogGardensState> get() = _uiState
}