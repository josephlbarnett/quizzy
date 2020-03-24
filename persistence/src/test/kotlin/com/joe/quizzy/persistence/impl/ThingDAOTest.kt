package com.joe.quizzy.persistence.impl

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Thing
import com.trib3.testing.db.DAOTestBase
import kotlin.streams.toList
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

/**
 * Test the ThingDAO
 */
class ThingDAOTest : DAOTestBase() {
    lateinit var dao: ThingDAOJooq

    @BeforeClass
    override fun setUp() {
        super.setUp()
        dao = ThingDAOJooq(ctx)
    }

    @Test
    fun testRoundTrip() {
        val thing = Thing(null, "billy")
        val nextThing = Thing(null, "jimmy")
        dao.save(thing)
        dao.save(nextThing)
        assertThat(dao.get(1)?.name).isEqualTo(thing.name)
        assertThat(dao.all().map { it.name }).all {
            contains(thing.name)
            contains(nextThing.name)
        }
        val updateThing = Thing(1, "william")
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
