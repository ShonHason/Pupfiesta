package org.example.project.features.dogGardens

import org.example.project.models.DogGardens

public sealed class dogGardensState {
    data object Loading : dogGardensState()
    data class Loaded(
        val dogGardens: DogGardens
    ): dogGardensState()
    data class Error(
        val errorMessage: String
    ): dogGardensState()
}