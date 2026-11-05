package com.dragon.fm.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SleepTimerManager(private val onTimerExpired: () -> Unit) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private val _remainingSeconds = MutableStateFlow<Int?>(null)
    val remainingSeconds: StateFlow<Int?> = _remainingSeconds.asStateFlow()

    private val _selectedMinutes = MutableStateFlow<Int?>(null)
    val selectedMinutes: StateFlow<Int?> = _selectedMinutes.asStateFlow()

    fun startTimer(minutes: Int) {
        cancelTimer()
        _selectedMinutes.value = minutes
        val totalSec = minutes * 60
        _remainingSeconds.value = totalSec

        timerJob = scope.launch {
            var currentSec = totalSec
            while (isActive && currentSec > 0) {
                delay(1000)
                currentSec -= 1
                _remainingSeconds.value = currentSec
            }
            _remainingSeconds.value = null
            _selectedMinutes.value = null
            onTimerExpired()
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingSeconds.value = null
        _selectedMinutes.value = null
    }

    fun isRunning(): Boolean = timerJob != null && timerJob?.isActive == true
}
