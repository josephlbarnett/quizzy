package com.joe.quizzy.persistence.impl

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.UserDAO
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.streams.toList

class ResponseDAOTest : PostgresDAOTestBase() {
    lateinit var userDao: UserDAO
    lateinit var instanceDao: InstanceDAO
    lateinit var questionDao: QuestionDAO
    lateinit var dao: ResponseDAO

    @BeforeClass
    override fun setUp() {
        super.setUp()
        userDao = UserDAOJooq(ctx)
        dao = ResponseDAOJooq(ctx)
        instanceDao = InstanceDAOJooq(ctx)
        questionDao = QuestionDAOJooq(ctx)
    }

    @Test
    fun testRoundTrip() {
        assertThat(dao.get(UUID.randomUUID())).isNull()
        val instance = Instance(null, "question dao round trip", "ACTIVE")
        val instanceId = instanceDao.save(instance).id!!
        val user = User(null, instanceId, "billy", "billy@gmail.com", null, false, "UTC")
        val userId = userDao.save(user).id!!
        val user2 = User(null, instanceId, "jimmy", "jimmy@gmail.com", null, false, "UTC")
        val userId2 = userDao.save(user2).id!!
        val q1 =
            Question(null, userId, "a question", "an answer", "some refs", OffsetDateTime.now(), OffsetDateTime.now())
        val qId = questionDao.save(q1).id!!
        val r1 = Response(null, userId, qId, "response1", "with references1")
        val r2 = Response(UUID.randomUUID(), userId2, qId, "response2", "with references2")
        val rId = dao.save(r1).id!!
        dao.save(r2)
        assertThat(dao.get(rId)?.response).isEqualTo(r1.response)
        assertThat(dao.all().map { it.response }).all {
            contains(r1.response)
            contains(r2.response)
        }
        val updateThing =
            Response(
                rId,
                userId,
                qId,
                "updated response1",
                "some refs"
            )
        dao.save(updateThing)
        dao.stream().use { stream ->
            for (list in listOf(stream.toList(), dao.all())) {
                assertThat(list.map { it.response }).all {
                    contains(updateThing.response)
                    doesNotContain(r1.response)
                    contains(r2.response)
                }
            }
        }
        val all = dao.all().toSet()
        assertThat(dao.forInstance(instanceId, true).toSet()).isEqualTo(all)
        assertThat(dao.forInstance(instanceId, false).toSet()).isEqualTo(all)
        assertThat((dao.forUser(userId) + dao.forUser(userId2)).toSet()).isEqualTo(all)
        assertThat(dao.byUserQuestion(userId, qId)).isEqualTo(updateThing)
        assertThat(dao.byUserQuestions(userId, listOf(qId))).contains(qId to updateThing)
    }
}
