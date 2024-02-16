package com.joe.quizzy.graphql.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.User
import com.joe.quizzy.graphql.dataloaders.UserTimePeriod
import com.trib3.testing.LeakyMock
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.dataloader.DataLoader
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CompletableFuture

class ApiUserTest {
    val u = ApiUser(UUID.randomUUID(), UUID.randomUUID(), "name", "email", false, "", false, 15)

    @Test
    fun testCopyConstructor() {
        assertThat(u).isEqualTo(
            ApiUser(
                User(
                    u.id,
                    u.instanceId,
                    u.name,
                    u.email,
                    "",
                    u.admin,
                    u.timeZoneId,
                    u.notifyViaEmail,
                ),
                15,
            ),
        )
    }

    @Test
    fun testScore() =
        runBlocking {
            val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
            val mockDataLoader = LeakyMock.mock<DataLoader<UserTimePeriod, List<Grade>>>()
            val t1 = OffsetDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            val t2 = OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            EasyMock.expect(mockEnv.getDataLoader<UserTimePeriod, List<Grade>>("usergrades")).andReturn(mockDataLoader)
                .anyTimes()
            EasyMock.expect(mockDataLoader.load(UserTimePeriod(u.id!!, null, null))).andReturn(
                CompletableFuture.completedFuture(
                    listOf(
                        Grade(UUID.randomUUID(), UUID.randomUUID(), true, 5),
                        Grade(UUID.randomUUID(), UUID.randomUUID(), false, 1),
                    ),
                ),
            )
            EasyMock.expect(mockDataLoader.load(UserTimePeriod(u.id!!, t1, t2))).andReturn(
                CompletableFuture.completedFuture(
                    listOf(
                        Grade(UUID.randomUUID(), UUID.randomUUID(), true, 2),
                        Grade(UUID.randomUUID(), UUID.randomUUID(), false, 1),
                    ),
                ),
            )
            EasyMock.replay(mockEnv, mockDataLoader)
            assertThat(u.score(mockEnv).await()).isEqualTo(20)
            assertThat(u.score(mockEnv, t1, t2).await()).isEqualTo(17)
            assertThat(u.copy(id = null).score(mockEnv).await()).isEqualTo(0)
            EasyMock.verify(mockEnv, mockDataLoader)
        }

    @Test
    fun testNullScore() =
        runBlocking {
            val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
            val mockDataLoader = LeakyMock.mock<DataLoader<UserTimePeriod, List<Grade>>>()
            EasyMock.expect(mockEnv.getDataLoader<UserTimePeriod, List<Grade>>("usergrades")).andReturn(mockDataLoader)
            EasyMock.expect(mockDataLoader.load(UserTimePeriod(u.id!!, null, null))).andReturn(
                CompletableFuture.completedFuture(
                    null,
                ),
            )
            EasyMock.replay(mockEnv, mockDataLoader)
            assertThat(u.score(mockEnv).await()).isEqualTo(0)
            EasyMock.verify(mockEnv, mockDataLoader)
        }

    @Test
    fun testInstance() =
        runBlocking {
            val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
            val mockDataLoader = LeakyMock.mock<DataLoader<UUID, Instance>>()
            EasyMock.expect(mockEnv.getDataLoader<UUID, Instance>("batchinstances")).andReturn(mockDataLoader)
            val i = Instance(u.instanceId, "Test inst", "ACTIVE")
            EasyMock.expect(mockDataLoader.load(u.instanceId)).andReturn(
                CompletableFuture.completedFuture(i),
            )
            EasyMock.replay(mockEnv, mockDataLoader)
            assertThat(u.instance(mockEnv).await()).isEqualTo(ApiInstance(i))
            EasyMock.verify(mockEnv, mockDataLoader)
        }
}
