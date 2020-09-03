package com.joe.quizzy.persistence.impl

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
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
import java.util.UUID
import kotlin.streams.toList

/**
 * Test the ThingDAO
 */
class QuestionDAOTest : PostgresDAOTestBase() {
    lateinit var userDao: UserDAO
    lateinit var instanceDao: InstanceDAO
    lateinit var dao: QuestionDAO
    lateinit var notificationDao: EmailNotificationDAO

    @BeforeClass
    override fun setUp() {
        super.setUp()
        userDao = UserDAOJooq(ctx)
        dao = QuestionDAOJooq(ctx)
        instanceDao = InstanceDAOJooq(ctx)
        notificationDao = EmailNotificationDAOJooq(ctx)
    }

    @Test
    fun testActiveClosedFutureQuestions() {
        val instance = Instance(null, "questiondao active/closed/future", "ACTIVE")
        val instanceId = instanceDao.save(instance).id!!
        val user = User(null, instanceId, "abc", "abc@gmail.com", null, false, "UTC")
        val userId = userDao.save(user).id!!
        val oneHourAgo = OffsetDateTime.now().minusHours(1)
        val oneHourFromNow = OffsetDateTime.now().plusHours(1)
        val questions = listOf(
            Question(null, userId, "current", "", "", oneHourAgo, oneHourFromNow),
            Question(null, userId, "past", "", "", oneHourAgo, oneHourAgo),
            Question(null, userId, "future", "", "", oneHourFromNow, oneHourFromNow)
        )
        questions.forEach { dao.save(it) }
        assertThat(dao.active().filter { it.authorId == userId }.map { it.body }.first()).isEqualTo("current")
        assertThat(dao.closed().filter { it.authorId == userId }.map { it.body }.first()).isEqualTo("past")
        assertThat(dao.active(user.copy(id = userId)).map { it.body }.first()).isEqualTo("current")
        assertThat(dao.closed(user.copy(id = userId)).map { it.body }.first()).isEqualTo("past")
        assertThat(dao.future(user.copy(id = userId)).map { it.body }.first()).isEqualTo("future")
    }

    @Test
    fun testActiveClosedNotifiedQuestions() {
        val instance = Instance(null, "questiondao active/closed notified", "ACTIVE")
        val instanceId = instanceDao.save(instance).id!!
        val user = User(null, instanceId, "abcd", "abcd@gmail.com", null, false, "UTC")
        val userId = userDao.save(user).id!!
        val oneHourAgo = OffsetDateTime.now().minusHours(1)
        val oneHourFromNow = OffsetDateTime.now().plusHours(1)
        val questions = listOf(
            Question(null, userId, "current1", "", "", oneHourAgo, oneHourFromNow),
            Question(null, userId, "past1", "", "", oneHourAgo, oneHourAgo),
            Question(null, userId, "current2", "", "", oneHourAgo, oneHourFromNow),
            Question(null, userId, "past2", "", "", oneHourAgo, oneHourAgo)
        ).map { dao.save(it) }
        notificationDao.markNotified(NotificationType.REMINDER, listOf(questions[0].id!!))
        notificationDao.markNotified(NotificationType.ANSWER, listOf(questions[1].id!!))
        val needReminders = dao.active(NotificationType.REMINDER).filter { it.authorId == userId }
        val needAnswers = dao.closed(NotificationType.ANSWER).filter { it.authorId == userId }
        assertThat(needReminders).hasSize(1)
        assertThat(needAnswers).hasSize(1)
        assertThat(needReminders.map { it.body }.first()).isEqualTo("current2")
        assertThat(needAnswers.map { it.body }.first()).isEqualTo("past2")
    }

    @Test
    fun testRoundTrip() {
        assertThat(dao.get(UUID.randomUUID())).isNull()
        val instance = Instance(null, "question dao round trip", "ACTIVE")
        val instanceId = instanceDao.save(instance).id!!
        val user = User(null, instanceId, "billy", "billy@gmail.com", null, false, "UTC")
        val userId = userDao.save(user).id!!
        val q1 =
            Question(null, userId, "a question", "an answer", "some refs", OffsetDateTime.now(), OffsetDateTime.now())
        val q2 =
            Question(
                UUID.randomUUID(),
                userId,
                "another question",
                "another answer",
                "some more refs",
                OffsetDateTime.now(),
                OffsetDateTime.now()
            )
        val qId = dao.save(q1).id!!
        dao.save(q2)
        assertThat(dao.get(qId)?.body).isEqualTo(q1.body)
        assertThat(dao.all().map { it.body }).all {
            contains(q1.body)
            contains(q2.body)
        }
        val updateThing =
            Question(
                qId,
                userId,
                "an updated question",
                "an answer",
                "some refs",
                OffsetDateTime.now(),
                OffsetDateTime.now()
            )
        dao.save(updateThing)
        dao.stream().use { stream ->
            for (list in listOf(stream.toList(), dao.all())) {
                assertThat(list.map { it.body }).all {
                    contains(updateThing.body)
                    doesNotContain(q1.body)
                    contains(q2.body)
                }
            }
        }
        val all = dao.all()
        assertThat(all.toSet()).isEqualTo(dao.get(all.mapNotNull { it.id }).toSet())
    }
}
