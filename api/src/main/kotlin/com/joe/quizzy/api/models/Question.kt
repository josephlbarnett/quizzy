package com.joe.quizzy.api.models

import java.time.LocalDateTime
import java.util.UUID

data class Question(
    val id: UUID?,
    val authorId: UUID,
    val body: String,
    val answer: String,
    val ruleReferences: String,
    val activeAt: LocalDateTime,
    val closedAt: LocalDateTime
)
