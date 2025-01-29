package com.example.singleviewapp

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.*


object InstalledAppsFetcher {
    private var timerJob: Job? = null
    private val selectedApps = mutableListOf<String>() // List of selected apps
    private var globalTimer = Pair(0, 0) // Single timer for all selected apps (minutes, seconds)

    fun getInstalledApps(context: Context): List<String> {
        val pm: PackageManager = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        // Get the package name of the current app
        val currentAppPackageName = context.packageName

        // Include YouTube and Play Store explicitly
        val youtubePackageName = "com.google.android.youtube"
        val playStorePackageName = "com.android.vending"
        val youtubeAppInfo = apps.find { it.packageName == youtubePackageName }
        val playStoreAppInfo = apps.find { it.packageName == playStorePackageName }

        val installedApps = apps.filter { app ->
            // Include non-system apps
            ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) &&
                    app.packageName != currentAppPackageName && // Exclude this app
                    !selectedApps.contains(pm.getApplicationLabel(app).toString()) // Exclude selected apps
        }.map { app ->
            pm.getApplicationLabel(app).toString()
        }.sorted() // Sort alphabetically

        val explicitlyIncludedApps = mutableListOf<String>()

        // Add YouTube to the list if it is installed but excluded by the filters
        youtubeAppInfo?.let {
            val youtubeAppLabel = pm.getApplicationLabel(it).toString()
            if (!installedApps.contains(youtubeAppLabel) && !selectedApps.contains(youtubeAppLabel)) {
                explicitlyIncludedApps.add(youtubeAppLabel)
            }
        }

        // Add Play Store to the list if it is installed but excluded by the filters
        playStoreAppInfo?.let {
            val playStoreAppLabel = pm.getApplicationLabel(it).toString()
            if (!installedApps.contains(playStoreAppLabel) && !selectedApps.contains(playStoreAppLabel)) {
                explicitlyIncludedApps.add(playStoreAppLabel)
            }
        }

        return installedApps + explicitlyIncludedApps
    }



    fun addSelectedApp(appName: String) {
        if (!selectedApps.contains(appName)) {
            selectedApps.add(appName)
            Log.d("AppControlService", "App $appName added to selected app list")
        }
    }

    fun getSelectedApps(): List<String> {
        return selectedApps
    }

    // Start the timer coroutine
    private fun startTimer() {
        // Cancel any existing timer
        timerJob?.cancel()

        // Start a new coroutine
        timerJob = CoroutineScope(Dispatchers.Default).launch {
            while (globalTimer.first > 0 || globalTimer.second > 0) {
                delay(1000) // Decrement every second
                decrementGlobalTimer()
            }

            // Stop the timer when it reaches 0:0
            stopTimer()
        }
    }

    // Stop the timer coroutine
    private fun stopTimer() {
        checkAndBlockCurrentApp()
        timerJob?.cancel()
        timerJob = null
        Log.d("AppControlService", "Global timer stopped")
    }

    fun setGlobalTimer(minutes: Int, seconds: Int) {
        Log.d("AppControlService", "Global timer is set: $minutes:$seconds")
        globalTimer = minutes to seconds
        startTimer()
    }

    private fun decrementGlobalTimer() {
        val totalSeconds = globalTimer.first * 60 + globalTimer.second
        if (totalSeconds > 0) {
            globalTimer = (totalSeconds - 1) / 60 to (totalSeconds - 1) % 60
        } else if (totalSeconds == 0) {
            Log.d("AppControlService", "Global timer hits 0:0")
            //checkAndBlockCurrentApp()
        }
    }

    private fun checkAndBlockCurrentApp() {
        val service = AppControlAccessibilityService.instance
        val foregroundApp = AppControlAccessibilityService.currentForegroundApp

        if (service == null) {
            Log.e("AppControlService", "Service instance is null")
            return
        }

        if (foregroundApp == null) {
            Log.e("AppControlService", "No foreground app detected")
            return
        }

        Log.d("AppControlService", "Focused app is $foregroundApp")

        // Get app name from package name
//        val appName = try {
//            service.packageManager.getApplicationLabel(
//                service.packageManager.getApplicationInfo(foregroundApp, PackageManager.GET_META_DATA)
//            ).toString()
//        } catch (e: PackageManager.NameNotFoundException) {
//            null
//        } ?: return

        if (selectedApps.contains(foregroundApp)) {
            Log.d("AppControlService", "Blocking app: $foregroundApp")
            service.sendToHomeScreen() // Send the user to the home screen
        }
    }

    fun getGlobalTimer(): Pair<Int,Int>{
        return globalTimer
    }

    fun isGlobalTimerZero(): Boolean {
        return globalTimer.first == 0 && globalTimer.second == 0
    }
}
