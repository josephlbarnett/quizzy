package com.joe.quizzy.api.models

import java.util.UUID

data class Response(
    val id: UUID?,
    val userId: UUID,
    val questionId: UUID,
    val response: String,
    val ruleReferences: String,
    val correct: Boolean?,
    val bonus: Int?
)
