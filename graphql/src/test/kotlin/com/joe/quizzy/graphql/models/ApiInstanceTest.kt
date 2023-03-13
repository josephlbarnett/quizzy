package com.joe.quizzy.graphql.models

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.QuestionType
import com.joe.quizzy.api.models.Season
import com.joe.quizzy.graphql.dataloaders.InstanceTimePeriod
import com.joe.quizzy.graphql.groupme.GroupMeService
import com.trib3.testing.LeakyMock
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.dataloader.DataLoader
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

class ApiInstanceTest {
    val i = ApiInstance(UUID.randomUUID(), "instance", "ACTIVE", QuestionType.MULTIPLE_CHOICE, false, 15)

    @Test
    fun testCopyConstructor() {
        assertThat(i).isEqualTo(
            ApiInstance(
                Instance(i.id, i.name, i.status, "", i.defaultQuestionType, i.autoGrade, i.defaultScore),
            ),
        )
    }

    @Test
    fun testNullId() = runBlocking {
        val mockDfe = LeakyMock.mock<DataFetchingEnvironment>()
        EasyMock.replay(mockDfe)
        assertThat(i.copy(id = null).seasons(mockDfe).await()).isEmpty()
        assertThat(i.copy(id = null).supportsGroupMe(mockDfe).await()).isFalse()
        EasyMock.verify(mockDfe)
    }

    @Test
    fun testSeasonLoader() = runBlocking {
        val mockDfe = LeakyMock.mock<DataFetchingEnvironment>()
        val mockLoader = LeakyMock.mock<DataLoader<InstanceTimePeriod, List<Season>>>()
        EasyMock.expect(mockDfe.getDataLoader<InstanceTimePeriod, List<Season>>("instanceseasons"))
            .andReturn(mockLoader)
        EasyMock.expect(mockLoader.load(InstanceTimePeriod(i.id!!, null, null)))
            .andReturn(
                CompletableFuture.completedFuture(
                    listOf(
                        Season(UUID.randomUUID(), i.id!!, "s1", OffsetDateTime.now(), OffsetDateTime.now()),
                        Season(UUID.randomUUID(), i.id!!, "s2", OffsetDateTime.now(), OffsetDateTime.now()),
                    ),
                ),
            )
        EasyMock.replay(mockDfe, mockLoader)
        val seasons = i.seasons(mockDfe, null, null)
        assertThat(seasons.await().map { it.name }).isEqualTo(listOf("s1", "s2"))
        EasyMock.verify(mockDfe, mockLoader)
    }

    @Test
    fun testGroupMeLoader() = runBlocking {
        val mockDfe = LeakyMock.mock<DataFetchingEnvironment>()
        val mockLoader = LeakyMock.mock<DataLoader<UUID, GroupMeService?>>()
        EasyMock.expect(mockDfe.getDataLoader<UUID, GroupMeService?>("groupmeservice"))
            .andReturn(mockLoader)
        EasyMock.expect(mockLoader.load(i.id))
            .andReturn(
                CompletableFuture.completedFuture(
                    null,
                ),
            )
        EasyMock.replay(mockDfe, mockLoader)
        assertThat(i.supportsGroupMe(mockDfe).await()).isFalse()
        EasyMock.verify(mockDfe, mockLoader)
    }
}
