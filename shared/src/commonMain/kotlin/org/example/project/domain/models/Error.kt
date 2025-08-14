package org.example.project.domain.models

import kotlinx.serialization.Serializable
import org.example.project.data.local.Error

@Serializable
data class AuthError(
    override val message: String
) : Error

@Serializable
data class dogError(
    override val message: String
) : Error

@Serializable
data class dbError(
    override val message: String
) : Error

