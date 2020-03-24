package com.joe.quizzy.api.models

import java.time.LocalDateTime
import java.util.UUID

data class Session(
    val id: UUID?,
    val userId: UUID,
    val createdAt: LocalDateTime,
    val lastUsedAt: LocalDateTime
)
