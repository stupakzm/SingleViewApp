package com.example.singleviewapp

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.singleviewapp.ui.theme.SingleViewAppTheme
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AppControlService", "MainActivity onCreate")
        // Check if the Accessibility Service is enabled
        if (!isAccessibilityServiceEnabled(AppControlAccessibilityService::class.java)) {
            Log.d("AppControlService", "Please enable the Accessibility")
            // Show a Toast and redirect to Accessibility Settings
            Toast.makeText(
                this,
                "Please enable the Accessibility Service for proper functionality",
                Toast.LENGTH_LONG
            ).show()

            // Open Accessibility Settings
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        setContent {
            SingleViewAppTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    AppContent(modifier = Modifier.padding(innerPadding), context = this)
                }
            }
        }
    }

//    private fun startGlobalTimer() {
//        CoroutineScope(Dispatchers.Default).launch {
//            while (true) {
//                delay(1000) // Decrement every second
//                InstalledAppsFetcher.decrementGlobalTimer()
//            }
//        }
//    }

    // Helper function to check if the Accessibility Service is enabled
    private fun isAccessibilityServiceEnabled(serviceClass: Class<out AccessibilityService>): Boolean {
        val serviceName = packageName + "/" + serviceClass.canonicalName
        val enabledServices =
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val accessibilityEnabled = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)

        if (accessibilityEnabled == 1 && !TextUtils.isEmpty(enabledServices)) {
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val enabledService = colonSplitter.next()
                if (enabledService.equals(serviceName, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }
}



@Composable
fun AppContent(modifier: Modifier = Modifier, context: Context) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    if (isPortrait) {
        // Portrait Layout: Top and Bottom Sections
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            SectorOne(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))
            SectorTwo(modifier = Modifier.weight(1f), context = context)
        }
    } else {
        // Landscape Layout: Left and Right Sections
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectorOne(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(16.dp))
            SectorTwo(modifier = Modifier.weight(1f), context = context)
        }
    }
}

@Composable
fun SectorOne(modifier: Modifier = Modifier) {
    val isRunning = TimerController.isRunning
    val remainingTime = TimerController.remainingTime
    val accessAttempts = AppControlAccessibilityService.accessAttemptsFlow.collectAsState(0)

    LaunchedEffect(Unit) {
        AppControlAccessibilityService.updateAccessAttemptsFlow()
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Display accessAttempts as a visual indicator
        Text(
            text = "Blocked App Attempts: ${accessAttempts.value}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )
        if (isRunning.value) {
            // Show Countdown Timer
            CountdownTimerDisplay(remainingTime = remainingTime.value)
        } else {
            // Show Buttons
            GridButtons()
        }
    }
}

@Composable
fun SectorTwo(modifier: Modifier = Modifier, context: Context) {
    val selectedApp = remember { mutableStateOf<String?>(null) }
    val selectedApps = remember { mutableStateOf(listOf<String>()) }
    val displayText = remember { mutableStateOf("Select an App") } // Dynamic text for dropdown
    val resetTextTrigger = remember { mutableStateOf(false) } // Trigger text reset

    // Load selected apps initially
    LaunchedEffect(Unit) {
        selectedApps.value = InstalledAppsFetcher.getSelectedApps()
    }

    // Reset display text after 2 seconds when triggered
    LaunchedEffect(resetTextTrigger.value) {
        if (resetTextTrigger.value) {
            kotlinx.coroutines.delay(2000)
            displayText.value = "Select an App"
            resetTextTrigger.value = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select App:",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        DropdownMenuExample(
            context = context,
            onAppSelected = { appName ->
                selectedApp.value = appName // Store the selected app
            },
            displayText = displayText.value, // Current display text
            setDisplayText = { newText ->
                displayText.value = newText // Update the display text
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (selectedApp.value != null) {
            Button(
                onClick = {
                    selectedApp.value?.let { app ->
                        InstalledAppsFetcher.addSelectedApp(app) // Add to selected apps
                        selectedApp.value = null // Reset the selected app
                        selectedApps.value = InstalledAppsFetcher.getSelectedApps() // Refresh list
                        displayText.value = "$app added"
                        resetTextTrigger.value = true // Trigger the reset of display text
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text(text = "Confirm", fontSize = 16.sp, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        SelectedAppsDropdown(selectedApps = selectedApps.value)
    }
}


@Composable
fun GridButtons() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StyledButton(
                buttonId = 1, // 1-minute button
                text = "1 Min",
                color = Color.LightGray,
                onClick = {
                    TimerController.startTimer(1) {  InstalledAppsFetcher.getSelectedApps().forEach { appName ->
                        Log.d("AppControl", "Checking and blocking app after 1 minute: $appName")
                    } }
                }
            )
            StyledButton(
                buttonId = 2, // 5-minute button
                text = "5 Min",
                color = Color.LightGray,
                onClick = {
                    TimerController.startTimer(5) {  InstalledAppsFetcher.getSelectedApps().forEach { appName ->
                        Log.d("AppControl", "Checking and blocking app after 5 minutes: $appName")
                    } }
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StyledButton(
                buttonId = 3, // 10-minute button
                text = "10 Min",
                color = Color.DarkGray,
                textColor = Color.White,
                onClick = {
                    TimerController.startTimer(10) {  InstalledAppsFetcher.getSelectedApps().forEach { appName ->
                        Log.d("AppControl", "Checking and blocking app after 10 minutes: $appName")
                    } }
                }
            )
        }
    }
}


@Composable
fun StyledButton(
    buttonId: Int,
    text: String,
    color: Color,
    textColor: Color = Color.Black,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val usageCount = ButtonUsageManager.getUsageCountState(buttonId).value
    //val resetTimeLeft by remember { ButtonUsageManager.getResetTimeState(buttonId) }

    //var formattedTime by remember { mutableStateOf("") }

    //LaunchedEffect(resetTimeLeft) {
       // while (resetTimeLeft > 0) {
            //formattedTime = formatTime(ButtonUsageManager.getTimeLeft(buttonId))
           // delay(60000) // Update every 1 minute
        //}
    //}

    Button(
        onClick = {
            if (ButtonUsageManager.canUseButton(buttonId)) {
                ButtonUsageManager.useButton(context, buttonId)
                onClick()
            }
        },
        enabled = usageCount > 0,
        modifier = Modifier
            .size(120.dp)
            .background(color, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (usageCount >= 0) {
                Text(text = "Uses left: $usageCount", fontSize = 12.sp, color = Color.Gray)
            } else {
                //Text(text = "Closest reset in $formattedTime", fontSize = 12.sp, color = Color.Red)
            }
        }
    }
}

fun formatTime(secondsLeft: Long): String {
    val hours = secondsLeft / 3600
    val minutes = (secondsLeft % 3600) / 60
    return String.format("%02d:%02d", hours, minutes)
}

@Composable
fun CountdownTimerDisplay(remainingTime: Pair<Int, Int>) {
    Text(
        text = "${remainingTime.first}:${remainingTime.second.toString().padStart(2, '0')}",
        fontSize = 64.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}


@Composable
fun DropdownMenuExample(
    context: Context,
    onAppSelected: (String?) -> Unit,
    displayText: String,
    setDisplayText: (String) -> Unit
) {
    val apps = remember { mutableStateOf(listOf<String>()) }
    val expanded = remember { mutableStateOf(false) }

    // Load the installed apps
    LaunchedEffect(Unit) {
        apps.value = InstalledAppsFetcher.getInstalledApps(context)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
    ) {
        Button(onClick = { expanded.value = true }, enabled = apps.value.isNotEmpty()) {
            Text(text = displayText) // Display dynamic text
        }
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            apps.value.forEach { app ->
                DropdownMenuItem(
                    onClick = {
                        expanded.value = false
                        setDisplayText(app) // Update the display text
                        onAppSelected(app) // Notify parent about app selection
                    },
                    text = { Text(app) }
                )
            }
        }
    }
}

@Composable
fun SelectedAppsDropdown(selectedApps: List<String>) {
    // State to manage dropdown visibility
    val expanded = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp), // Add padding for better alignment
        contentAlignment = Alignment.Center
    ) {
        // Dropdown button
        Button(
            onClick = { expanded.value = true },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
        ) {
            Text(text = "Selected Apps: ${selectedApps.size}", color = Color.Black)
        }

        // Dropdown menu to display selected apps
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }, // Close dropdown when clicked outside
            modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp))
        ) {
            selectedApps.forEach { app ->
                DropdownMenuItem(
                    onClick = {}, // No interaction
                    text = { Text(app, color = Color.Black) }
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PortraitPreview() {
    val mockContext = androidx.compose.ui.platform.LocalContext.current // Mock context for preview
    SingleViewAppTheme {
        AppContent(context = mockContext)
    }
}

@Preview(showBackground = true, widthDp = 640, heightDp = 360)
@Composable
fun LandscapePreview() {
    val mockContext = androidx.compose.ui.platform.LocalContext.current // Mock context for preview
    SingleViewAppTheme {
        AppContent(context = mockContext)
    }
}

