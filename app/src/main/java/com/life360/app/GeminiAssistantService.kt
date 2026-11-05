package com.life360.app

import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject

object GeminiAssistantService {

    suspend fun sendMessage(message: String): String {
        require(message.isNotBlank()) {
            "Please enter a message."
        }

        val response = supabase.functions.invoke(
            function = "ai-assistant",
            body = buildJsonObject {
                put("message", message)
            }
        )

        val responseText = response.bodyAsText()
        val json = JSONObject(responseText)

        if (json.has("error")) {
            throw Exception(json.getString("error"))
        }

        return json.optString("reply").takeIf {
            it.isNotBlank()
        } ?: throw Exception("The AI assistant returned an empty response.")
    }
}