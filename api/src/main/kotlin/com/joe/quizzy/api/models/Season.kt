package com.joe.quizzy.api.models

import java.time.OffsetDateTime
import java.util.UUID

data class Season(
    val id: UUID?,
    val instanceId: UUID,
    val name: String,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
)
