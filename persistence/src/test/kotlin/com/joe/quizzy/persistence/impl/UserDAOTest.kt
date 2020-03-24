package com.joe.quizzy.persistence.impl

import assertk.all
import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.testing.LeakyMock.Companion.contains
import kotlin.streams.toList
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

/**
 * Test the ThingDAO
 */
class UserDAOTest : PostgresDAOTestBase() {
    lateinit var dao: UserDAO
    lateinit var instanceDao: InstanceDAO

    @BeforeClass
    override fun setUp() {
        super.setUp()
        dao = UserDAOJooq(ctx)
        instanceDao = InstanceDAOJooq(ctx)
    }

    @Test
    fun testRoundTrip() {
        val instance = Instance(null, "group", "ACTIVE")
        val instanceId = instanceDao.save(instance).id!!
        val thing = User(null, instanceId, "billy", "billy@gmail.com", null, false, "UTC")
        val nextThing = User(null, instanceId, "jimmy", "jimmy@gmail.com", null, false, "UTC")
        val thingId = dao.save(thing).id!!
        dao.save(nextThing)
        assertThat(dao.get(thingId)?.name).isEqualTo(thing.name)
        assertThat(dao.all().map { it.name }).all {
            contains(thing.name)
            contains(nextThing.name)
        }
        val updateThing = User(thingId, instanceId, "william", "billy@gmail.com", null, false, "UTC")
        dao.save(updateThing)
        dao.stream().use { stream ->
            for (list in listOf(stream.toList(), dao.all())) {
                assertThat(list.map { it.name }).all {
                    contains(updateThing.name)
                    doesNotContain(thing.name)
                    contains(nextThing.name)
                }
            }
        }
    }
}
