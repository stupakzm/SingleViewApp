package com.example.singleviewapp

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*

object TimerController {
    private var timerJob: Job? = null
    var remainingTime: MutableState<Pair<Int, Int>> = mutableStateOf(0 to 0)
    var isRunning: MutableState<Boolean> = mutableStateOf(false)

    fun startTimer(minutes: Int, onComplete: () -> Unit) {
        InstalledAppsFetcher.setGlobalTimer(minutes,0)
        val totalSeconds = minutes * 60
        remainingTime.value = totalSeconds / 60 to totalSeconds % 60
        isRunning.value = true

        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            for (seconds in totalSeconds downTo 0) {
                delay(1000)
                remainingTime.value = seconds / 60 to seconds % 60
                if (seconds == 0) {
                    isRunning.value = false
                    onComplete()
                }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        isRunning.value = false
    }
}
