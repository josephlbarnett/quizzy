package com.joe.quizzy.server.graphql.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.User
import com.trib3.testing.LeakyMock
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.dataloader.DataLoader
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.util.UUID
import java.util.concurrent.CompletableFuture

class ApiUserTest {

    val u = ApiUser(UUID.randomUUID(), UUID.randomUUID(), "name", "email", false, "", false)

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
                    u.notifyViaEmail
                )
            )
        )
    }

    @Test
    fun testScore() = runBlocking {
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, List<Grade>>>()
        EasyMock.expect(mockEnv.getDataLoader<UUID, List<Grade>>("usergrades")).andReturn(mockDataLoader)
        EasyMock.expect(mockDataLoader.load(u.id)).andReturn(
            CompletableFuture.completedFuture(
                listOf(
                    Grade(UUID.randomUUID(), UUID.randomUUID(), true, 5),
                    Grade(UUID.randomUUID(), UUID.randomUUID(), false, 1)
                )
            )
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        assertThat(u.score(mockEnv).await()).isEqualTo(20)
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testNullScore() = runBlocking {
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, List<Grade>>>()
        EasyMock.expect(mockEnv.getDataLoader<UUID, List<Grade>>("usergrades")).andReturn(mockDataLoader)
        EasyMock.expect(mockDataLoader.load(u.id)).andReturn(
            CompletableFuture.completedFuture(
                null
            )
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        assertThat(u.score(mockEnv).await()).isEqualTo(0)
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testInstance() = runBlocking {
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, Instance>>()
        EasyMock.expect(mockEnv.getDataLoader<UUID, Instance>("batchinstances")).andReturn(mockDataLoader)
        val i = Instance(u.instanceId, "Test inst", "ACTIVE")
        EasyMock.expect(mockDataLoader.load(u.instanceId)).andReturn(
            CompletableFuture.completedFuture(i)
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        assertThat(u.instance(mockEnv).await()).isEqualTo(i)
        EasyMock.verify(mockEnv, mockDataLoader)
    }
}
