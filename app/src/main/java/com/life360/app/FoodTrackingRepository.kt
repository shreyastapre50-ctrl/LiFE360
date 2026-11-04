
package com.life360.app

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

@Serializable
data class MealImpactResult(
    val food_name: String,
    val quantity: Double,
    val quantity_unit: String,
    val co2e_kg: Double? = null
)

@Serializable
data class MealHistoryLog(
    val id: String,
    val meal_type: String,
    val consumed_at: String,
    val notes: String? = null
)

@Serializable
data class MealHistoryItem(
    val id: String,
    val meal_log_id: String,
    val food_name: String,
    val quantity: Double,
    val quantity_unit: String,
    val co2e_kg: Double? = null
)

object FoodTrackingRepository {

    // Save a meal and retrieve its calculated carbon emissions.
    suspend fun saveMeal(
        mealType: String,
        foodName: String,
        quantity: Double,
        unit: String,
        notes: String
    ): MealImpactResult {

        val user = supabase.auth.currentUserOrNull()
            ?: throw IllegalStateException(
                "Your session expired. Please sign in again."
            )

        require(mealType in setOf("breakfast", "lunch", "dinner", "snack")) {
            "Please choose a valid meal type."
        }

        require(foodName.isNotBlank()) {
            "Enter a food name."
        }

        require(quantity > 0.0) {
            "Quantity must be greater than zero."
        }

        require(unit.isNotBlank()) {
            "Enter a quantity unit."
        }

        // Save the meal and receive its meal-log UUID.
        val mealId = supabase.postgrest.rpc(
            function = "life360_add_meal",
            parameters = buildJsonObject {
                put("p_meal_type", mealType)
                put("p_consumed_at", Instant.now().toString())
                put("p_food_name", foodName.trim())
                put("p_quantity", quantity)
                put("p_unit", unit.trim())
                put("p_notes", notes.trim())
            }
        ).decodeAs<String>()

        // Retrieve the carbon result calculated by Supabase.
        val result = supabase.postgrest
            .from("meal_items")
            .select {
                filter {
                    eq("meal_log_id", mealId)
                }
            }
            .decodeSingle<MealImpactResult>()

        return result
    }

    // Retrieve meal history for the signed-in user.
    suspend fun getMealHistory():
            Pair<List<MealHistoryLog>, List<MealHistoryItem>> {

        val user = supabase.auth.currentUserOrNull()
            ?: throw IllegalStateException(
                "Your session expired. Please sign in again."
            )

        // RLS restricts these results to records the user can access.
        val logs = supabase.postgrest
            .from("meal_logs")
            .select()
            .decodeList<MealHistoryLog>()

        val items = supabase.postgrest
            .from("meal_items")
            .select()
            .decodeList<MealHistoryItem>()

        return Pair(logs, items)
    }
}
