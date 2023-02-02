package com.joe.quizzy.persistence.impl

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.impl.jooq.Tables.SEASONS
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.time.OffsetDateTime

class SeasonDAOTest : PostgresDAOTestBase() {
    lateinit var dao: SeasonDAOJooq
    lateinit var instanceDAO: InstanceDAO

    @BeforeClass
    override fun setUp() {
        super.setUp()
        instanceDAO = InstanceDAOJooq(ctx)
        dao = SeasonDAOJooq(ctx)
    }

    @Test
    fun testQuery() {
        val instanceId = instanceDAO.save(Instance(null, "season", "ACTIVE")).id!!
        val instanceId2 = instanceDAO.save(Instance(null, "season2", "ACTIVE")).id!!
        ctx.transaction { config ->
            config.dsl().insertInto(SEASONS)
                .columns(SEASONS.INSTANCE_ID, SEASONS.NAME, SEASONS.START_TIME, SEASONS.END_TIME)
                .values(instanceId, "s1", OffsetDateTime.now().minusDays(5), OffsetDateTime.now().minusDays(4))
                .values(instanceId, "s2", OffsetDateTime.now().plusDays(5), OffsetDateTime.now().plusDays(6))
                .values(instanceId2, "s3", OffsetDateTime.now().minusDays(5), OffsetDateTime.now().minusDays(4))
                .values(instanceId2, "s4", OffsetDateTime.now().plusDays(5), OffsetDateTime.now().plusDays(6))
                .execute()
        }
        assertThat(dao.getSeasons(instanceId).map { it.name }).containsExactlyInAnyOrder("s1", "s2")
        assertThat(
            dao.getSeasons(listOf(instanceId, instanceId2))
                .flatMap { it.value.map { s -> s.name } },
        ).containsExactlyInAnyOrder("s1", "s2", "s3", "s4")
        assertThat(dao.getSeasons(instanceId, OffsetDateTime.now(), OffsetDateTime.now())).isEmpty()
        assertThat(
            dao.getSeasons(listOf(instanceId, instanceId2), OffsetDateTime.now(), null)
                .flatMap { it.value.map { s -> s.name } },
        ).containsExactlyInAnyOrder("s2", "s4")
        assertThat(
            dao.getSeasons(listOf(instanceId, instanceId2), null, OffsetDateTime.now())
                .flatMap { it.value.map { s -> s.name } },
        ).containsExactlyInAnyOrder("s1", "s3")
    }
}
