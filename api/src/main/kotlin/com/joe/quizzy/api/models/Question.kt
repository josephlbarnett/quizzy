package com.joe.quizzy.api.models

import java.time.OffsetDateTime
import java.util.UUID

data class Question(
    val id: UUID?,
    val authorId: UUID,
    val body: String,
    val answer: String,
    val ruleReferences: String,
    val activeAt: OffsetDateTime,
    val closedAt: OffsetDateTime,
    val sentReminder: Boolean = false,
    val sentAnswer: Boolean = false
)
