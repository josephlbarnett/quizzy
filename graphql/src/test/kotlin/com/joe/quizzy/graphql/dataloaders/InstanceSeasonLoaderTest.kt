package com.joe.quizzy.graphql.dataloaders

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Season
import com.joe.quizzy.persistence.api.SeasonDAO
import com.trib3.testing.LeakyMock
import kotlinx.coroutines.runBlocking
import org.dataloader.BatchLoaderEnvironment
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.time.OffsetDateTime
import java.util.UUID

class InstanceSeasonLoaderTest {
    @Test
    fun testLoader() = runBlocking {
        val mockDAO = LeakyMock.mock<SeasonDAO>()
        val mockEnv = LeakyMock.mock<BatchLoaderEnvironment>()
        val loader = InstanceSeasonLoader(mockDAO, emptyMap<Any, Any>())
        val seasons = mapOf(
            UUID.randomUUID() to listOf(
                Season(UUID.randomUUID(), UUID.randomUUID(), "s1", OffsetDateTime.now(), OffsetDateTime.now()),
                Season(UUID.randomUUID(), UUID.randomUUID(), "s2", OffsetDateTime.now(), OffsetDateTime.now()),
            ),
        )
        val moreSeasons = mapOf(
            UUID.randomUUID() to listOf(
                Season(UUID.randomUUID(), UUID.randomUUID(), "s3", OffsetDateTime.now(), OffsetDateTime.now()),
                Season(UUID.randomUUID(), UUID.randomUUID(), "s4", OffsetDateTime.now(), OffsetDateTime.now()),
            ),
        )
        EasyMock.expect(mockDAO.getSeasons(seasons.keys.toList())).andReturn(seasons)
        EasyMock.expect(mockDAO.getSeasons(moreSeasons.keys.toList(), OffsetDateTime.MIN, OffsetDateTime.MAX))
            .andReturn(moreSeasons)
        EasyMock.replay(mockDAO, mockEnv)
        val loaded = loader.loadSuspend(
            (
                seasons.keys.map {
                    InstanceTimePeriod(it, null, null)
                } +
                    moreSeasons.keys.map {
                        InstanceTimePeriod(it, OffsetDateTime.MIN, OffsetDateTime.MAX)
                    }
                ).toSet(),
            mockEnv,
        )
        assertThat(loaded).isEqualTo(
            seasons.mapKeys { InstanceTimePeriod(it.key, null, null) } +
                moreSeasons.mapKeys { InstanceTimePeriod(it.key, OffsetDateTime.MIN, OffsetDateTime.MAX) },
        )
        EasyMock.verify(mockDAO, mockEnv)
    }
}
