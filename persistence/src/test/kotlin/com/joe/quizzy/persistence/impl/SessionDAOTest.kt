package com.joe.quizzy.persistence.impl

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.Session
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import kotlin.streams.toList
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

/**
 * Test the ThingDAO
 */
class SessionDAOTest : PostgresDAOTestBase() {
    lateinit var userDao: UserDAO
    lateinit var instanceDao: InstanceDAO
    lateinit var dao: SessionDAO

    @BeforeClass
    override fun setUp() {
        super.setUp()
        userDao = UserDAOJooq(ctx)
        dao = SessionDAOJooq(ctx)
        instanceDao = InstanceDAOJooq(ctx)
    }

    @Test
    fun testRoundTrip() {
        val instance = Instance(null, "group", "ACTIVE")
        val instanceId = instanceDao.save(instance).id!!
        val user = User(null, instanceId, "billy", "billy@gmail.com", null, false, "UTC")
        val userId = userDao.save(user).id!!
        val hourAgo = OffsetDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MILLIS)
        val twoHoursAgo = OffsetDateTime.now().minusHours(2).truncatedTo(ChronoUnit.MILLIS)
        val threeHoursAgo = OffsetDateTime.now().minusHours(3).truncatedTo(ChronoUnit.MILLIS)
        val s1 = Session(null, userId, hourAgo, hourAgo)
        val s2 = Session(null, userId, twoHoursAgo, twoHoursAgo)
        val sId = dao.save(s1).id!!
        dao.save(s2)
        assertThat(dao.get(sId)?.createdAt).isEqualTo(hourAgo)
        assertThat(dao.all().map { it.createdAt }).all {
            contains(hourAgo)
            contains(twoHoursAgo)
        }
        val updateThing =
            Session(sId, userId, threeHoursAgo, threeHoursAgo)
        dao.save(updateThing)
        dao.stream().use { stream ->
            for (list in listOf(stream.toList(), dao.all())) {
                assertThat(list.map { it.createdAt }).all {
                    contains(threeHoursAgo)
                    doesNotContain(hourAgo)
                    contains(twoHoursAgo)
                }
            }
        }
    }
}
