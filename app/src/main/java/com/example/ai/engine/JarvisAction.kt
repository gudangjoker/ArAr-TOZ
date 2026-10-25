package com.example.ai.engine

import com.google.gson.JsonObject

enum class JarvisActionType {
    ADD_TRANSACTION,
    ADD_WISHLIST,
    PAY_BILL,
    GET_SUMMARY,
    CHAT
}

data class JarvisResponse(
    val action: JarvisActionType,
    val params: JsonObject = JsonObject(),
    val reply: String,
    val rawJson: String = "",
    val executedSuccessfully: Boolean = false,
    val executionDetails: String? = null
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: JarvisActionType? = null,
    val actionExecutionDetails: String? = null,
    val isSuccess: Boolean = true,
    val isInferring: Boolean = false
)
