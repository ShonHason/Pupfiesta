package org.example.project.features

import kotlinx.coroutines.CoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope


actual open class BaseViewModel: ViewModel(){
    actual val scope: CoroutineScope = viewModelScope
}