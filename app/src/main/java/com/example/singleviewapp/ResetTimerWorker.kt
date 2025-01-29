package com.example.singleviewapp

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class ResetTimerWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val buttonId = inputData.getInt("buttonId", -1)

        if (buttonId != -1) {
            Log.d("ResetTimerWorker", "Restoring usage for button $buttonId")
            ButtonUsageManager.restoreUsage(buttonId) // Call the restore method
        } else {
            Log.e("ResetTimerWorker", "Invalid buttonId")
        }

        return Result.success()
    }
}
