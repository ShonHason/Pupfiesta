package org.example.project.bridge

import org.example.project.enum.Breed

object SwiftBridge {
    fun allBreeds(): Array<Breed> = Breed.entries.toTypedArray()
}