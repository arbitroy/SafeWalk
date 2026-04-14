package com.example.safewalk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.local.SafeWalkRepository
import com.example.safewalk.data.model.CheckIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: SafeWalkRepository,
) : ViewModel() {

    val history: Flow<List<CheckIn>> = repository.getAllCheckIns()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val completedCount: Flow<Int> = repository.getCompletedCheckInCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val missedCount: Flow<Int> = repository.getMissedCheckInCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val sosCount: Flow<Int> = repository.getSosCheckInCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val totalCount: Flow<Int> = repository.getTotalCheckInCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun getSuccessRate(completed: Int, total: Int): Int =
        if (total > 0) (completed * 100) / total else 0
}
