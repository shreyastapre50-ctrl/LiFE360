
package com.life360.app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JourneyInsert(

    @SerialName("user_id")
    val userId: String,

    @SerialName("started_at")
    val startedAt: String,

    @SerialName("ended_at")
    val endedAt: String,

    @SerialName("transport_mode")
    val transportMode: String = "unknown",

    @SerialName("mode_source")
    val modeSource: String = "manual",

    @SerialName("distance_km")
    val distanceKm: Double,

    @SerialName("duration_seconds")
    val durationSeconds: Long,

    @SerialName("start_lat")
    val startLat: Double? = null,

    @SerialName("start_lon")
    val startLon: Double? = null,

    @SerialName("end_lat")
    val endLat: Double? = null,

    @SerialName("end_lon")
    val endLon: Double? = null,

    @SerialName("co2e_kg")
    val co2eKg: Double? = null,

    @SerialName("emissions_status")
    val emissionsStatus: String = "not_calculated"
)
