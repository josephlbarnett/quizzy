package com.joe.quizzy.persistence.impl

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.joe.quizzy.api.models.Grade
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

class GradeDAOTest : PostgresDAOTestBase() {
    lateinit var userDao: UserDAO
    lateinit var instanceDao: InstanceDAO
    lateinit var questionDao: QuestionDAO
    lateinit var responseDao: ResponseDAO
    lateinit var dao: GradeDAOJooq

    @BeforeClass
    override fun setUp() {
        super.setUp()
        userDao = UserDAOJooq(ctx)
        instanceDao = InstanceDAOJooq(ctx)
        questionDao = QuestionDAOJooq(ctx)
        dao = GradeDAOJooq(ctx)
        responseDao = ResponseDAOJooq(ctx, dao, instanceDao, questionDao, userDao)
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
        val r2 = Response(null, userId2, qId, "response2", "with references2")
        val rId = responseDao.save(r1).id!!
        val r2Id = responseDao.save(r2).id!!
        val g1 = Grade(null, rId, true, 2)
        val g2 = Grade(UUID.randomUUID(), r2Id, false, 0)
        val gId = dao.save(g1).id!!
        val g2Id = dao.save(g2).id!!
        assertThat(dao.get(gId)?.bonus).isEqualTo(g1.bonus)
        assertThat(dao.all().map { it.bonus }).all {
            contains(g1.bonus)
            contains(g2.bonus)
        }
        val updateThing =
            Grade(
                gId,
                rId,
                true,
                3
            )
        dao.save(updateThing)
        dao.stream().use { stream ->
            for (list in listOf(stream.toList(), dao.all())) {
                assertThat(list.map { it.bonus }).all {
                    contains(updateThing.bonus)
                    doesNotContain(g1.bonus)
                    contains(g2.bonus)
                }
            }
        }
        assertThat(dao.forUser(userId)).isEqualTo(listOf(updateThing))
        val userGrades = dao.forUsers(listOf(userId, userId2))
        assertThat(userGrades[userId]).isEqualTo(listOf(updateThing))
        assertThat(userGrades[userId2]).isEqualTo(listOf(g2.copy(id = g2Id)))
        assertThat(dao.forResponse(rId)).isEqualTo(updateThing)
        assertThat(dao.forResponses(listOf(rId, r2Id))).isEqualTo(
            mapOf(
                rId to updateThing,
                r2Id to g2.copy(id = g2Id)
            )
        )
    }
}
