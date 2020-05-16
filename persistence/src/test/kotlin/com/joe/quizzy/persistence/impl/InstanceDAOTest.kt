package com.joe.quizzy.persistence.impl

import assertk.all
import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.persistence.api.InstanceDAO
import com.trib3.testing.LeakyMock.Companion.contains
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.util.UUID
import kotlin.streams.toList

/**
 * Test the ThingDAO
 */
class InstanceDAOTest : PostgresDAOTestBase() {
    lateinit var dao: InstanceDAO

    @BeforeClass
    override fun setUp() {
        super.setUp()
        dao = InstanceDAOJooq(ctx)
    }

    @Test
    fun testRoundTrip() {
        assertThat(dao.get(UUID.randomUUID())).isNull()
        val thing = Instance(null, "group1", "ACTIVE")
        val nextThing = Instance(null, "group2", "ACTIVE")
        val thingId = dao.save(thing).id!!
        dao.save(nextThing)
        assertThat(dao.get(thingId)?.name).isEqualTo(thing.name)
        assertThat(dao.all().map { it.name }).all {
            contains(thing.name)
            contains(nextThing.name)
        }
        val updateThing = Instance(thingId, "group1 renamed", "ACTIVE")
        val newerThing = Instance(UUID.randomUUID(), "group3", "ACTIVE")
        dao.save(updateThing)
        dao.save(newerThing)
        dao.stream().use { stream ->
            for (list in listOf(stream.toList(), dao.all())) {
                assertThat(list.map { it.name }).all {
                    contains(updateThing.name)
                    doesNotContain(thing.name)
                    contains(nextThing.name)
                    contains(newerThing.name)
                }
            }
        }
        assertThat(dao.all().toSet()).isEqualTo(dao.get(dao.all().mapNotNull { it.id }).toSet())
    }
}
