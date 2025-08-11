// commonMain/src/commonMain/kotlin/org/example/project/utils/LocationUtils.kt
package org.example.project.utils

// commonMain
expect suspend fun getLocation(): Location
data class Location(val latitude: Double, val longitude: Double)
