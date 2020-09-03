package com.joe.quizzy.persistence.impl

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.each
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.EmailNotification
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.NotificationType
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.EmailNotificationDAO
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.UserDAO
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.time.OffsetDateTime

class EmailNotificationDAOTest : PostgresDAOTestBase() {

    lateinit var userDao: UserDAO
    lateinit var instanceDao: InstanceDAO
    lateinit var questionDao: QuestionDAO
    lateinit var dao: EmailNotificationDAO

    @BeforeClass
    override fun setUp() {
        super.setUp()
        questionDao = QuestionDAOJooq(ctx)
        userDao = UserDAOJooq(ctx)
        instanceDao = InstanceDAOJooq(ctx)
        dao = EmailNotificationDAOJooq(ctx)
    }

    @Test
    fun testMarkNotified() {
        val instance = Instance(null, "emailNotificationDAOInstance", "ACTIVE")
        val instanceId = instanceDao.save(instance).id!!
        val user = User(null, instanceId, "abc", "abc@gmail.com", null, false, "UTC")
        val userId = userDao.save(user).id!!
        val questions = listOf(
            Question(null, userId, "q1", "a1", "r1", OffsetDateTime.now(), OffsetDateTime.now()),
            Question(null, userId, "q2", "a2", "r2", OffsetDateTime.now(), OffsetDateTime.now()),
            Question(null, userId, "q3", "a3", "r3", OffsetDateTime.now(), OffsetDateTime.now()),
        ).map {
            questionDao.save(it)
        }
        dao.markNotified(NotificationType.REMINDER, listOf())
        assertThat(dao.all()).isEmpty()

        dao.markNotified(NotificationType.REMINDER, listOfNotNull(questions[0].id))
        assertThat(dao.all()).hasSize(1)
        assertThat(dao.all().first().questionId).isEqualTo(questions[0].id)
        assertThat(dao.all().first().notificationType).isEqualTo(NotificationType.REMINDER)

        dao.markNotified(NotificationType.ANSWER, listOfNotNull(questions[0].id))
        assertThat(dao.all()).hasSize(2)
        assertThat(dao.all().map { it.questionId }).each {
            it.isEqualTo(questions[0].id)
        }
        assertThat(dao.all().map { it.notificationType }).contains(NotificationType.REMINDER)
        assertThat(dao.all().map { it.notificationType }).contains(NotificationType.ANSWER)

        dao.markNotified(NotificationType.REMINDER, listOfNotNull(questions[0].id))
        dao.markNotified(NotificationType.ANSWER, listOfNotNull(questions[0].id))
        assertThat(dao.all()).hasSize(2)
        assertThat(dao.all().map { it.questionId }).each {
            it.isEqualTo(questions[0].id)
        }
        assertThat(dao.all().map { it.notificationType }).contains(NotificationType.REMINDER)
        assertThat(dao.all().map { it.notificationType }).contains(NotificationType.ANSWER)

        dao.markNotified(NotificationType.REMINDER, questions.slice(0..1).mapNotNull { it.id })
        assertThat(dao.all()).hasSize(3)
        assertThat(dao.all().map { it.copy(id = null) }).all {
            contains(
                EmailNotification(
                    null,
                    NotificationType.ANSWER,
                    questions[0].id!!
                )
            )
            contains(
                EmailNotification(
                    null,
                    NotificationType.REMINDER,
                    questions[0].id!!
                )
            )
            contains(
                EmailNotification(
                    null,
                    NotificationType.REMINDER,
                    questions[1].id!!
                )
            )
        }

        dao.markNotified(NotificationType.ANSWER, questions.slice(0..1).mapNotNull { it.id })
        assertThat(dao.all()).hasSize(4)
        assertThat(dao.all().map { it.copy(id = null) }).all {
            for (question in questions.slice(0..1)) {
                contains(EmailNotification(null, NotificationType.ANSWER, question.id!!))
                contains(EmailNotification(null, NotificationType.REMINDER, question.id!!))
            }
        }

        dao.markNotified(NotificationType.REMINDER, questions.mapNotNull { it.id })
        dao.markNotified(NotificationType.ANSWER, questions.mapNotNull { it.id })
        assertThat(dao.all()).hasSize(6)
        assertThat(dao.all().map { it.copy(id = null) }).all {
            for (question in questions) {
                contains(EmailNotification(null, NotificationType.ANSWER, question.id!!))
                contains(EmailNotification(null, NotificationType.REMINDER, question.id!!))
            }
        }
    }
}
