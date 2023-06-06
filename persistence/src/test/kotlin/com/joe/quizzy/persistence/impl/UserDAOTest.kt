package com.joe.quizzy.persistence.impl

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.messageContains
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.testing.LeakyMock.Companion.contains
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.util.UUID

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
        assertThat(dao.get(UUID.randomUUID())).isNull()
        val instance = Instance(null, "group", "ACTIVE")
        val instanceId = instanceDao.save(instance).id!!
        val thing = User(null, instanceId, "billy", "billy@gmail.com", null, false, "UTC")
        val nextThing = User(UUID.randomUUID(), instanceId, "jimmy", "jimmy@gmail.com", null, false, "UTC")
        val thingId = dao.save(thing).id!!
        dao.save(nextThing)
        assertThat(dao.getByEmail("billy@gmail.com")).isEqualTo(thing.copy(id = thingId))
        assertThat(dao.get(thingId)?.name).isEqualTo(thing.name)
        assertThat(dao.all().map { it.name }).all {
            contains(thing.name)
            contains(nextThing.name)
        }
        val updateThing = User(thingId, instanceId, "william", "billy@gmail.com", null, false, "UTC")
        dao.save(updateThing)
        dao.stream().use { stream ->
            for (list in listOf(stream.toList(), dao.all())) {
                assertThat(list.filter { it.instanceId == instanceId }.map { it.name }).all {
                    contains(updateThing.name)
                    doesNotContain(thing.name)
                    contains(nextThing.name)
                }
            }
        }
        val all = dao.all().filter { it.instanceId == instanceId }
        assertThat(all.toSet())
            .isEqualTo(
                dao.getByInstance(instanceId).toSet(),
            )
        assertThat(all.toSet()).isEqualTo(dao.get(all.mapNotNull { it.id }).toSet())
    }

    @Test
    fun testPartialUpdateRoundTrip() {
        val instance = Instance(null, "group2", "ACTIVE")
        val instanceId = instanceDao.save(instance).id!!
        val thing = User(null, instanceId, "billy", "billy2@gmail.com", "crypt1", false, "UTC")
        val thingId = dao.save(thing).id!!
        assertThat(dao.get(thingId)?.name).isEqualTo(thing.name)
        assertThat(dao.all().map { it.name }).all {
            contains(thing.name)
        }
        val updateThing = User(thingId, instanceId, "william", "billy2@gmail.com", null, false, "UTC")
        dao.save(updateThing)
        dao.stream().use { stream ->
            for (list in listOf(stream.toList(), dao.all())) {
                assertThat(list.filter { it.instanceId == instanceId }.map { it.name }).all {
                    contains(updateThing.name)
                    doesNotContain(thing.name)
                }
            }
        }
        assertThat(dao.get(thingId)?.authCrypt).isEqualTo("crypt1")
        dao.savePassword(updateThing, "crypt2")
        assertThat(dao.get(thingId)?.authCrypt).isEqualTo("crypt2")

        assertFailure {
            dao.savePassword(updateThing.copy(id = UUID.randomUUID()), "crypt3")
        }.messageContains("rolling back")
    }
}
