package com.example.singleviewapp

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ButtonUsageManager {
    // Tracks remaining usage counts as observable states
    private val usageCounts = mutableStateMapOf(
        1 to mutableIntStateOf(5), // 1-minute button: 5 usages
        2 to mutableIntStateOf(3), // 5-minute button: 3 usages
        3 to mutableIntStateOf(1)  // 10-minute button: 1 usage
    )

    private val maxUsages = mapOf(
        1 to 5, // Maximum usage for 1-minute button
        2 to 3, // Maximum usage for 5-minute button
        3 to 1  // Maximum usage for 10-minute button
    )

    private val resetTimestamps = mutableStateMapOf<Int, MutableState<Long>>(
        1 to mutableStateOf(0L),
        2 to mutableStateOf(0L),
        3 to mutableStateOf(0L)
    )

    fun getTimeLeft(buttonId: Int): Long {
        val resetTime = resetTimestamps[buttonId]?.value ?: 0L
        val currentTime = System.currentTimeMillis()
        return if (resetTime > currentTime) (resetTime - currentTime) / 1000 else 0L
    }

    // Restore usage count for a button
    fun restoreUsage(buttonId: Int) {
        val currentUsage = usageCounts[buttonId]?.intValue ?: 0
        val maxUsage = maxUsages[buttonId] ?: 0

        if (currentUsage < maxUsage) {
            usageCounts[buttonId]?.intValue = currentUsage + 1
            Log.d("ButtonUsageManager", "Button $buttonId usage restored to ${currentUsage + 1}")
        } else {
            Log.d("ButtonUsageManager", "Button $buttonId usage cannot be restored beyond max limit of $maxUsage")
        }
    }

    // Check if a button has remaining usages
    fun canUseButton(buttonId: Int): Boolean {
        return (usageCounts[buttonId]?.intValue ?: 0) > 0
    }

    // Decrement usage count and schedule a reset
    fun useButton(context: Context, buttonId: Int) {
        if (!canUseButton(buttonId)) return // No remaining usages

        usageCounts[buttonId]?.intValue = (usageCounts[buttonId]?.intValue ?: 0) - 1
        Log.d("ButtonUsageManager", "Button $buttonId used.")

        // Schedule reset using WorkManager
        val resetDuration = when (buttonId) {
            1 -> 2L * 60 * 60 // 2 hours in seconds
            2 -> 7L * 60 * 60 // 7 hours in seconds
            3 -> 20L * 60 * 60 // 20 hours in seconds
            else -> return
        }

        scheduleReset(context, buttonId, resetDuration)
    }

    // Schedule a reset task
    private fun scheduleReset(context: Context, buttonId: Int, delayInSeconds: Long) {
        val data = Data.Builder()
            .putInt("buttonId", buttonId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ResetTimerWorker>()
            .setInitialDelay(delayInSeconds, TimeUnit.SECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        Log.d("ButtonUsageManager", "Scheduled reset for button $buttonId in $delayInSeconds seconds")
    }

    // Get the current usage count as State for a button
    fun getUsageCountState(buttonId: Int): State<Int> {
        return usageCounts[buttonId] ?: mutableIntStateOf(0)
    }

    fun getResetTimeState(buttonId: Int): State<Long> {
        return resetTimestamps[buttonId] ?: mutableStateOf(0L)
    }
}
