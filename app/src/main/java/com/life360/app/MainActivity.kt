
package com.life360.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.SystemClock

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.content.ContextCompat

import com.life360.app.ui.theme.LiFE360CopyTheme

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import java.time.Instant
import java.util.Locale


// ---------------------------------------------------------
// MAIN ACTIVITY WITH SUPABASE DEEP-LINK HANDLING
// ---------------------------------------------------------

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle Supabase email confirmation deep links
        supabase.handleDeeplinks(intent)

        enableEdgeToEdge()

        setContent {
            LiFE360CopyTheme {
                LiFE360App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Handle confirmation links when the app is already open
        supabase.handleDeeplinks(intent)
    }
}


// ---------------------------------------------------------
// MAIN APP: CHECK SUPABASE LOGIN SESSION
// ---------------------------------------------------------

@Composable
fun LiFE360App() {

    var isCheckingSession by remember {
        mutableStateOf(true)
    }

    var isAuthenticated by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {

        supabase.auth.sessionStatus.collect { sessionStatus ->

            when (sessionStatus) {

                is SessionStatus.Authenticated -> {
                    isAuthenticated = true
                    isCheckingSession = false
                }

                is SessionStatus.NotAuthenticated -> {
                    isAuthenticated = false
                    isCheckingSession = false
                }

                SessionStatus.Initializing -> {
                    isCheckingSession = true
                }

                is SessionStatus.RefreshFailure -> {
                    isAuthenticated = false
                    isCheckingSession = false
                }
            }
        }
    }

    when {

        isCheckingSession -> {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        isAuthenticated -> {
            TransportationDashboard()
        }

        else -> {

            AuthScreen(
                onLoginSuccess = {
                    isAuthenticated = true
                    isCheckingSession = false
                }
            )
        }
    }
}


// ---------------------------------------------------------
// TRANSPORTATION DASHBOARD
// ---------------------------------------------------------

@Composable
fun TransportationDashboard() {

    val context = LocalContext.current

    val locationManager = remember {
        LocationManager(context)
    }

    val coroutineScope = rememberCoroutineScope()

    val darkGreen = Color(0xFF176B45)
    val lightGreen = Color(0xFFE8F5EC)

    var isTracking by remember {
        mutableStateOf(false)
    }

    var status by remember {
        mutableStateOf("GPS not connected")
    }

    var latitude by remember {
        mutableStateOf("—")
    }

    var longitude by remember {
        mutableStateOf("—")
    }

    var accuracy by remember {
        mutableStateOf("—")
    }

    // Journey distance calculation
    var totalDistanceMeters by remember {
        mutableFloatStateOf(0f)
    }

    var previousLocation by remember {
        mutableStateOf<Location?>(null)
    }

    // Journey duration variables
    var journeyStartTime by remember {
        mutableLongStateOf(0L)
    }

    var elapsedSeconds by remember {
        mutableLongStateOf(0L)
    }

    var journeyStartedAtIso by remember {
        mutableStateOf<String?>(null)
    }

    var firstJourneyLocation by remember {
        mutableStateOf<Location?>(null)
    }

    var lastJourneyLocation by remember {
        mutableStateOf<Location?>(null)
    }

    var isSavingJourney by remember {
        mutableStateOf(false)
    }

    var showLeaderboard by remember {
        mutableStateOf(false)
    }

    // Food Tracking navigation
    var showFoodTracking by remember {
        mutableStateOf(false)
    }

    // Hardware Monitoring navigation
    var showHardwareMonitoring by remember {
        mutableStateOf(false)
    }

    // AI Chatbot navigation
    var showChatbot by remember {
        mutableStateOf(false)
    }

    // Live timer: updates every second while tracking
    LaunchedEffect(isTracking, journeyStartTime) {

        while (isTracking) {

            elapsedSeconds =
                (SystemClock.elapsedRealtime() - journeyStartTime) / 1000L

            delay(1000L)
        }
    }

    // Start GPS tracking and timer
    fun startTracking() {

        previousLocation = null
        totalDistanceMeters = 0f

        journeyStartTime = SystemClock.elapsedRealtime()
        journeyStartedAtIso = Instant.now().toString()

        firstJourneyLocation = null
        lastJourneyLocation = null

        elapsedSeconds = 0L

        isTracking = true
        status = "Starting GPS tracking..."

        locationManager.startLocationUpdates(

            onLocationReceived = { location ->

                latitude = String.format(
                    Locale.US,
                    "%.6f",
                    location.latitude
                )

                longitude = String.format(
                    Locale.US,
                    "%.6f",
                    location.longitude
                )

                accuracy = String.format(
                    Locale.US,
                    "%.1f metres",
                    location.accuracy
                )

                // Keep accurate GPS fixes for the journey record.
                if (location.accuracy <= 30f) {

                    if (firstJourneyLocation == null) {
                        firstJourneyLocation = location
                    }

                    lastJourneyLocation = location

                    previousLocation?.let { previous ->

                        val segmentDistance =
                            previous.distanceTo(location)

                        // Filter small GPS movements and large jumps
                        if (
                            segmentDistance > 3f &&
                            segmentDistance < 1000f
                        ) {
                            totalDistanceMeters += segmentDistance
                        }
                    }

                    previousLocation = location
                }

                status = "GPS tracking active"
            },

            onError = { error ->

                locationManager.stopLocationUpdates()

                isTracking = false
                status = error
            }
        )
    }

    // Location permission request
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] ?: false

            val coarseGranted =
                permissions[
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ] ?: false

            if (fineGranted || coarseGranted) {
                startTracking()
            } else {
                status = "Location permission denied"
            }
        }

    // Stop GPS updates when dashboard is removed
    DisposableEffect(Unit) {

        onDispose {
            locationManager.stopLocationUpdates()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF6F8F6)
    ) {

        when {

            // Existing leaderboard screen
            showLeaderboard -> {

                LeaderboardScreen(
                    onBack = {
                        showLeaderboard = false
                    }
                )
            }

            // Existing Food Tracking screen
            showFoodTracking -> {

                FoodTrackingScreen(
                    onBack = {
                        showFoodTracking = false
                    }
                )
            }

            // Hardware Monitoring screen
            showHardwareMonitoring -> {

                HardwareMonitoringScreen(
                    onBack = {
                        showHardwareMonitoring = false
                    }
                )
            }

            // AI Chatbot screen
            showChatbot -> {

                ChatbotScreen(
                    onBack = {
                        showChatbot = false
                    }
                )
            }

            // Main dashboard
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(24.dp))

                // App heading
                Text(
                    text = "LiFE360",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )

                Text(
                    text = "TRANSPORTATION INTELLIGENCE",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Existing leaderboard button
                OutlinedButton(
                    onClick = {
                        showLeaderboard = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🏆 Points & Leaderboard")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Existing Food Tracking button
                OutlinedButton(
                    onClick = {
                        showFoodTracking = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🥗 Food Tracking")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hardware Monitoring button
                OutlinedButton(
                    onClick = {
                        showHardwareMonitoring = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⚡ Hardware Energy Monitoring")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // AI Sustainability Assistant button
                OutlinedButton(
                    onClick = {
                        showChatbot = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🤖 AI Sustainability Assistant")
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Journey status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Text(
                            text = "Journey Status",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "● $status",
                            color = if (isTracking) {
                                darkGreen
                            } else {
                                Color.Gray
                            },
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Start Journey
                        Button(
                            onClick = {

                                val fineGranted =
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                val coarseGranted =
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                if (fineGranted || coarseGranted) {

                                    startTracking()

                                } else {

                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },

                            enabled = !isTracking,

                            modifier = Modifier.fillMaxWidth(),

                            shape = RoundedCornerShape(12.dp),

                            colors = ButtonDefaults.buttonColors(
                                containerColor = darkGreen,
                                disabledContainerColor = lightGreen,
                                disabledContentColor = darkGreen
                            )
                        ) {

                            Text(
                                text = "Start Journey",
                                modifier = Modifier.padding(6.dp)
                            )
                        }

                        // Stop Journey and save to Supabase
                        OutlinedButton(
                            onClick = {

                                elapsedSeconds =
                                    (SystemClock.elapsedRealtime() - journeyStartTime) / 1000L

                                locationManager.stopLocationUpdates()

                                isTracking = false
                                isSavingJourney = true

                                status = "Journey stopped. Saving to Supabase..."

                                val startedAt = journeyStartedAtIso
                                val endedAt = Instant.now().toString()

                                val distanceKm =
                                    totalDistanceMeters.toDouble() / 1000.0

                                val duration = elapsedSeconds

                                val startLocation = firstJourneyLocation
                                val endLocation = lastJourneyLocation

                                coroutineScope.launch {

                                    try {

                                        val user =
                                            supabase.auth.currentUserOrNull()
                                                ?: throw IllegalStateException(
                                                    "Your session expired. Please sign in again."
                                                )

                                        if (startedAt == null) {
                                            throw IllegalStateException(
                                                "Journey start time is missing. Please start again."
                                            )
                                        }

                                        val journey = JourneyInsert(
                                            userId = user.id,
                                            startedAt = startedAt,
                                            endedAt = endedAt,
                                            transportMode = "unknown",
                                            modeSource = "manual",
                                            distanceKm = distanceKm,
                                            durationSeconds = duration,
                                            startLat = startLocation?.latitude,
                                            startLon = startLocation?.longitude,
                                            endLat = endLocation?.latitude,
                                            endLon = endLocation?.longitude,
                                            co2eKg = null,
                                            emissionsStatus = "not_calculated"
                                        )

                                        JourneyRepository.saveJourney(journey)

                                        status = "Journey saved successfully to Supabase"

                                    } catch (error: Exception) {

                                        status =
                                            "Journey save failed: ${error.message ?: "Unknown error"}"

                                    } finally {

                                        isSavingJourney = false
                                    }
                                }
                            },

                            enabled = isTracking && !isSavingJourney,

                            modifier = Modifier.fillMaxWidth(),

                            shape = RoundedCornerShape(12.dp)

                        ) {

                            Text(
                                if (isSavingJourney) {
                                    "Saving..."
                                } else {
                                    "Stop Journey"
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Journey distance card
                Card(
                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(18.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),

                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Text(
                            text = "Journey Distance",
                            fontSize = 15.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = String.format(
                                Locale.US,
                                "%.3f km",
                                totalDistanceMeters / 1000f
                            ),

                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkGreen
                        )

                        Text(
                            text = "GPS-based distance estimate",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Journey duration card
                Card(
                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(18.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),

                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Text(
                            text = "Journey Duration",
                            fontSize = 15.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val hours = elapsedSeconds / 3600L
                        val minutes = (elapsedSeconds % 3600L) / 60L
                        val seconds = elapsedSeconds % 60L

                        Text(
                            text = String.format(
                                Locale.US,
                                "%02d:%02d:%02d",
                                hours,
                                minutes,
                                seconds
                            ),

                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkGreen
                        )

                        Text(
                            text = "Hours : Minutes : Seconds",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Live GPS information
                Text(
                    text = "Live GPS Information",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                GpsInfoCard(
                    title = "Latitude",
                    value = latitude
                )

                GpsInfoCard(
                    title = "Longitude",
                    value = longitude
                )

                GpsInfoCard(
                    title = "GPS Accuracy",
                    value = accuracy
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Small journeys. Sustainable future.",
                    color = darkGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}


// ---------------------------------------------------------
// REUSABLE GPS INFORMATION CARD
// ---------------------------------------------------------

@Composable
fun GpsInfoCard(
    title: String,
    value: String
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),

        shape = RoundedCornerShape(14.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                color = Color.Gray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF176B45)
            )
        }
    }
}
