package org.example.project.data.firebase

import kotlinx.serialization.Serializable
import org.example.project.data.Error

@Serializable
data class AuthError(
    override val message: String
) : Error
