
package com.life360.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FoodTrackingScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val green = Color(0xFF176B45)

    var mealType by remember { mutableStateOf("breakfast") }
    var foodName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("100") }
    var unit by remember { mutableStateOf("g") }
    var notes by remember { mutableStateOf("") }

    var saving by remember { mutableStateOf(false) }
    var loadingHistory by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var mealMenuExpanded by remember { mutableStateOf(false) }

    var carbonResult by remember { mutableStateOf<Double?>(null) }
    var mealLogs by remember { mutableStateOf<List<MealHistoryLog>>(emptyList()) }
    var mealItems by remember { mutableStateOf<List<MealHistoryItem>>(emptyList()) }

    fun loadHistory() {
        scope.launch {
            loadingHistory = true
            try {
                val history = FoodTrackingRepository.getMealHistory()
                mealLogs = history.first
                mealItems = history.second
            } catch (error: Exception) {
                message = "Could not load meal history: ${error.message ?: "Unknown error"}"
            } finally {
                loadingHistory = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadHistory()
    }

    val today = LocalDate.now(ZoneId.systemDefault())

    val todayLogIds = mealLogs.filter { log ->
        try {
            Instant.parse(log.consumed_at)
                .atZone(ZoneId.systemDefault())
                .toLocalDate() == today
        } catch (_: Exception) {
            false
        }
    }.map { it.id }.toSet()

    val todayItems = mealItems.filter { it.meal_log_id in todayLogIds }

    val dailyCarbon = todayItems.sumOf { it.co2e_kg ?: 0.0 }

    val dateFormatter = DateTimeFormatter.ofPattern(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF6F8F6)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            TextButton(onClick = onBack) {
                Text("← Back to dashboard")
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Food Tracking",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = green
            )

            Text(
                "Track meals and monitor their estimated carbon emissions.",
                color = Color.DarkGray
            )

            Spacer(Modifier.height(18.dp))

            // Daily carbon summary
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5EC)
                )
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        "Today's Food Carbon",
                        fontWeight = FontWeight.SemiBold,
                        color = green
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        String.format(Locale.US, "%.3f kg CO₂e", dailyCarbon),
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = green
                    )

                    Text(
                        "${todayItems.size} food item(s) recorded today",
                        color = Color.DarkGray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Add meal form
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        "Add a meal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(14.dp))

                    Text("Meal type", fontWeight = FontWeight.Medium)

                    Box {
                        OutlinedButton(
                            onClick = { mealMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(mealType.replaceFirstChar { it.uppercase() })
                        }

                        DropdownMenu(
                            expanded = mealMenuExpanded,
                            onDismissRequest = { mealMenuExpanded = false }
                        ) {
                            listOf(
                                "breakfast", "lunch", "dinner", "snack"
                            ).forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(option.replaceFirstChar { it.uppercase() })
                                    },
                                    onClick = {
                                        mealType = option
                                        mealMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text("Food name") },
                        placeholder = { Text("e.g. Rice") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val parsedQuantity = quantity.toDoubleOrNull()

                            if (
                                foodName.isBlank() ||
                                parsedQuantity == null ||
                                parsedQuantity <= 0 ||
                                unit.isBlank()
                            ) {
                                message = "Enter a food name, a valid quantity, and a unit."
                            } else {
                                saving = true
                                message = "Saving meal..."
                                carbonResult = null

                                scope.launch {
                                    try {
                                        val result =
                                            FoodTrackingRepository.saveMeal(
                                                mealType,
                                                foodName,
                                                parsedQuantity,
                                                unit,
                                                notes
                                            )

                                        carbonResult = result.co2e_kg
                                        message = if (result.co2e_kg != null) {
                                            "Meal saved successfully."
                                        } else {
                                            "Meal saved, but no carbon estimate is available for this food or unit yet."
                                        }

                                        foodName = ""
                                        quantity = "100"
                                        unit = "g"
                                        notes = ""

                                        // Refresh history after saving.
                                        val history =
                                            FoodTrackingRepository.getMealHistory()
                                        mealLogs = history.first
                                        mealItems = history.second

                                    } catch (error: Exception) {
                                        message =
                                            "Could not save or refresh meal: ${error.message ?: "Unknown error"}"
                                    } finally {
                                        saving = false
                                    }
                                }
                            }
                        },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = green
                        )
                    ) {
                        Text(if (saving) "Saving..." else "Save meal")
                    }

                    if (message.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            message,
                            color = if (message.startsWith("Could not") ||
                                message.startsWith("Enter")
                            ) {
                                MaterialTheme.colorScheme.error
                            } else {
                                green
                            }
                        )
                    }

                    if (carbonResult != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "This meal: ${
                                String.format(
                                    Locale.US,
                                    "%.3f kg CO₂e",
                                    carbonResult
                                )
                            }",
                            fontWeight = FontWeight.Bold,
                            color = green
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // Meal history
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Meal History",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = { loadHistory() },
                    enabled = !loadingHistory
                ) {
                    Text(if (loadingHistory) "Loading..." else "Refresh")
                }
            }

            if (mealLogs.isEmpty() && !loadingHistory) {
                Text(
                    "No meals recorded yet.",
                    color = Color.DarkGray
                )
            }

            mealLogs.sortedByDescending { it.consumed_at }.forEach { log ->

                val logItems = mealItems.filter {
                    it.meal_log_id == log.id
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            log.meal_type.replaceFirstChar { it.uppercase() },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = green
                        )

                        Text(
                            try {
                                Instant.parse(log.consumed_at)
                                    .atZone(ZoneId.systemDefault())
                                    .format(dateFormatter)
                            } catch (_: Exception) {
                                log.consumed_at
                            },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(Modifier.height(10.dp))

                        if (logItems.isEmpty()) {
                            Text("No food items found.")
                        }

                        logItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        item.food_name,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${item.quantity} ${item.quantity_unit}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }

                                Text(
                                    item.co2e_kg?.let {
                                        String.format(
                                            Locale.US,
                                            "%.3f kg",
                                            it
                                        )
                                    } ?: "Unavailable",
                                    fontWeight = FontWeight.SemiBold,
                                    color = green
                                )
                            }
                        }

                        if (!log.notes.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Notes: ${log.notes}",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Carbon values are estimates based on available food emission factors. " +
                        "The current factors use global-average data, not India-specific measurements.",
                fontSize = 12.sp,
                color = Color.DarkGray
            )
        }
    }
}
