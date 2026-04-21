package com.example.safewalk.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.safewalk.data.firebase.PhoneFirebaseSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val syncManager: PhoneFirebaseSyncManager,
) : ViewModel() {

    private val _sessionCode = MutableStateFlow(syncManager.sessionCode)
    val sessionCode: StateFlow<String> = _sessionCode.asStateFlow()

    fun regenerateCode() {
        _sessionCode.value = syncManager.regenerateSessionCode()
    }
}
