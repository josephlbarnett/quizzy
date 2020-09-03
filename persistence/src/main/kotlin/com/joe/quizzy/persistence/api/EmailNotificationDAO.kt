package com.joe.quizzy.persistence.api

import com.joe.quizzy.api.models.EmailNotification
import com.joe.quizzy.api.models.NotificationType
import java.util.UUID

interface EmailNotificationDAO {
    fun markNotified(notificationType: NotificationType, questionUUIDs: List<UUID>)
    fun all(): List<EmailNotification>
}
