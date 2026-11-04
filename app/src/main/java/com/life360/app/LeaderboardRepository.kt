package com.life360.app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

@Serializable
data class LeaderboardEntry(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("total_points") val totalPoints: Long,
    val rank: Long
)

@Serializable
data class CommunityGroup(
    val id: String,
    val name: String,
    @SerialName("invite_code") val inviteCode: String
)

object LeaderboardRepository {
    suspend fun global(periodStart: String?): List<LeaderboardEntry> =
        supabase.postgrest.rpc(
            "life360_global_leaderboard",
            buildJsonObject { if (periodStart == null) put("p_period_start", kotlinx.serialization.json.JsonNull) else put("p_period_start", periodStart) }
        ).decodeList()

    suspend fun groups(): List<CommunityGroup> =
        supabase.postgrest.rpc("life360_my_groups").decodeList()

    suspend fun group(groupId: String, periodStart: String?): List<LeaderboardEntry> =
        supabase.postgrest.rpc(
            "life360_group_leaderboard",
            buildJsonObject {
                put("p_group_id", groupId)
                if (periodStart == null) put("p_period_start", kotlinx.serialization.json.JsonNull) else put("p_period_start", periodStart)
            }
        ).decodeList()

    suspend fun createGroup(name: String): CommunityGroup =
        supabase.postgrest.rpc(
            "life360_create_group",
            buildJsonObject { put("p_name", name) }
        ).decodeList<CommunityGroup>().first()

    suspend fun joinGroup(code: String): String =
        supabase.postgrest.rpc(
            "life360_join_group",
            buildJsonObject { put("p_invite_code", code.trim().uppercase()) }
        ).decodeSingle()
}
