
package com.life360.app

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

object JourneyRepository {

    private const val TAG = "LiFE360_JourneySave"

    suspend fun saveJourney(journey: JourneyInsert) {

        Log.d(TAG, "Starting journey save operation")

        try {
            // Check the currently signed-in Supabase user.
            val currentUser = supabase.auth.currentUserOrNull()
                ?: throw IllegalStateException(
                    "No authenticated user found. Please sign in again."
                )

            Log.d(TAG, "Authenticated user found")

            // Ensure the journey belongs to the signed-in user.
            if (journey.userId != currentUser.id) {
                throw IllegalArgumentException(
                    "Journey user ID does not match the signed-in user."
                )
            }

            require(journey.distanceKm >= 0) {
                "Journey distance cannot be negative."
            }

            require(journey.durationSeconds >= 0) {
                "Journey duration cannot be negative."
            }

            Log.d(TAG, "Journey validation successful")
            Log.d(TAG, "Sending journey insert request to Supabase")

            // Insert the journey into Supabase.
            supabase.postgrest["journeys"].insert(journey)

            Log.d(TAG, "Journey insert request completed successfully")

        } catch (e: Exception) {

            Log.e(TAG, "Journey save failed: ${e.message}", e)

            throw e
        }
    }
}
