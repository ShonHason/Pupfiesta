package org.example.project.di

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.example.project.data.dogs.DogsViewModel
import org.example.project.features.registration.UserViewModel
import org.example.project.data.dogGardens.DogGardensViewModel

object ServiceLocator : KoinComponent {
    fun dogsVM(): DogsViewModel = get()
    fun userVM(): UserViewModel = get()
    fun gardensVM(): DogGardensViewModel = get()
}
