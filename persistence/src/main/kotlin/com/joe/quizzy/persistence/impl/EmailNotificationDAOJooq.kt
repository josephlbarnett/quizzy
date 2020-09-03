package com.joe.quizzy.persistence.impl

import com.joe.quizzy.api.models.EmailNotification
import com.joe.quizzy.api.models.NotificationType
import com.joe.quizzy.persistence.api.EmailNotificationDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import org.jooq.DSLContext
import java.util.UUID
import javax.inject.Inject

class EmailNotificationDAOJooq @Inject constructor(
    private val ctx: DSLContext
) : EmailNotificationDAO {
    override fun markNotified(notificationType: NotificationType, questionUUIDs: List<UUID>) {
        if (questionUUIDs.isNotEmpty()) {
            ctx.transaction { config ->
                val firstRecord = config.dsl().insertInto(Tables.EMAIL_NOTIFICATIONS)
                    .set(Tables.EMAIL_NOTIFICATIONS.NOTIFICATION_TYPE, notificationType.name)
                    .set(Tables.EMAIL_NOTIFICATIONS.QUESTION_ID, questionUUIDs.first())
                questionUUIDs.slice(1 until questionUUIDs.size)
                    .fold(firstRecord) { step, questionUUID ->
                        step.newRecord()
                            .set(Tables.EMAIL_NOTIFICATIONS.NOTIFICATION_TYPE, notificationType.name)
                            .set(Tables.EMAIL_NOTIFICATIONS.QUESTION_ID, questionUUID)
                    }
                    .onDuplicateKeyIgnore().execute()
            }
        }
    }

    override fun all(): List<EmailNotification> {
        return ctx.select().from(Tables.EMAIL_NOTIFICATIONS).fetchInto(EmailNotification::class.java)
    }
}
