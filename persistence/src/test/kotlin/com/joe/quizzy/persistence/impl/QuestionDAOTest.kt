package com.joe.quizzy.persistence.impl

import assertk.all
import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.testing.LeakyMock.Companion.contains
import java.time.LocalDateTime
import kotlin.streams.toList
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

/**
 * Test the ThingDAO
 */
class QuestionDAOTest : PostgresDAOTestBase() {
    lateinit var userDao: UserDAO
    lateinit var instanceDao: InstanceDAO
    lateinit var dao: QuestionDAO

    @BeforeClass
    override fun setUp() {
        super.setUp()
        userDao = UserDAOJooq(ctx)
        dao = QuestionDAOJooq(ctx)
        instanceDao = InstanceDAOJooq(ctx)
    }

    @Test
    fun testRoundTrip() {
        val instance = Instance(null, "group", "ACTIVE")
        val instanceId = instanceDao.save(instance).id!!
        val user = User(null, instanceId, "billy", "billy@gmail.com", null, false, "UTC")
        val userId = userDao.save(user).id!!
        val q1 =
            Question(null, userId, "a question", "an answer", "some refs", LocalDateTime.now(), LocalDateTime.now())
        val q2 =
            Question(
                null,
                userId,
                "another question",
                "another answer",
                "some more refs",
                LocalDateTime.now(),
                LocalDateTime.now()
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
                LocalDateTime.now(),
                LocalDateTime.now()
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
    }
}
