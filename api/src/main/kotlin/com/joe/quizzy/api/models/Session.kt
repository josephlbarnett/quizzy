package com.joe.quizzy.api.models

import java.time.OffsetDateTime
import java.util.UUID

data class Session(
    val id: UUID?,
    val userId: UUID,
    val createdAt: OffsetDateTime,
    val lastUsedAt: OffsetDateTime
)
