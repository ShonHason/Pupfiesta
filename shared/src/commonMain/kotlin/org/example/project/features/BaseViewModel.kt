package org.example.project.presentation.features

import kotlinx.coroutines.CoroutineScope

expect open class BaseViewModel()
{
     val scope: CoroutineScope
}