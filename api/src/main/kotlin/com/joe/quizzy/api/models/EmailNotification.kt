package com.joe.quizzy.api.models

import java.util.UUID

enum class NotificationType {
    REMINDER,
    ANSWER
}

data class EmailNotification(
    val id: UUID?,
    val notificationType: NotificationType,
    val questionId: UUID
)
