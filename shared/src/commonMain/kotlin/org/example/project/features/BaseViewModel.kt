package org.example.project.features

import kotlinx.coroutines.CoroutineScope

expect open class BaseViewModel()
{
     val scope: CoroutineScope
}