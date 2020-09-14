package com.joe.quizzy.server.graphql.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.server.auth.UserPrincipal
import com.trib3.graphql.resources.GraphQLResourceContext
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

class ApiResponseTest {
    val now = OffsetDateTime.now()
    val r = ApiResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "answer",
        "references"
    )
    val u = User(UUID.randomUUID(), UUID.randomUUID(), "name", "email", "", false, "")

    @Test
    fun testCopyConstructor() {
        assertThat(r).isEqualTo(ApiResponse(Response(r.id, r.userId, r.questionId, r.response, r.ruleReferences)))
    }

    @Test
    fun testUser() = runBlocking {
        val context = GraphQLResourceContext(UserPrincipal(u, null))
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, User>>()
        EasyMock.expect(mockEnv.getDataLoader<UUID, User>("batchusers")).andReturn(mockDataLoader)
        EasyMock.expect(mockDataLoader.load(r.userId)).andReturn(
            CompletableFuture.completedFuture(u)
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        assertThat(r.user(context, mockEnv).await()).isEqualTo(ApiUser(u))
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testNullUser() = runBlocking {
        val context = GraphQLResourceContext(UserPrincipal(u, null))
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, User>>()
        EasyMock.expect(mockEnv.getDataLoader<UUID, User>("batchusers")).andReturn(mockDataLoader)
        EasyMock.expect(mockDataLoader.load(r.userId)).andReturn(
            CompletableFuture.completedFuture(null)
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        assertThat(r.user(context, mockEnv).await()).isNull()
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testNoContextUser() = runBlocking {
        val context = GraphQLResourceContext(null)
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        EasyMock.replay(mockEnv)
        assertThat(r.user(context, mockEnv).await()).isNull()
        EasyMock.verify(mockEnv)
    }

    @Test
    fun testQuestion() = runBlocking {
        val context = GraphQLResourceContext(UserPrincipal(u, null))
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, Question>>()
        EasyMock.expect(mockEnv.getDataLoader<UUID, Question>("batchquestions")).andReturn(mockDataLoader)
        val q = Question(r.questionId, UUID.randomUUID(), "b", "a", "r", now, now)
        EasyMock.expect(mockDataLoader.load(r.questionId)).andReturn(
            CompletableFuture.completedFuture(q)
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        assertThat(r.question(context, mockEnv).await()).isEqualTo(ApiQuestion(q))
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testNullQuestion() = runBlocking {
        val context = GraphQLResourceContext(UserPrincipal(u, null))
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, Question>>()
        EasyMock.expect(mockEnv.getDataLoader<UUID, Question>("batchquestions")).andReturn(mockDataLoader)
        EasyMock.expect(mockDataLoader.load(r.questionId)).andReturn(
            CompletableFuture.completedFuture(null)
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        assertThat(r.question(context, mockEnv).await()).isNull()
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testNoContextQuestion() = runBlocking {
        val context = GraphQLResourceContext(null)
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        EasyMock.replay(mockEnv)
        assertThat(r.question(context, mockEnv).await()).isNull()
        EasyMock.verify(mockEnv)
    }

    @Test
    fun testGrade() = runBlocking {
        val context = GraphQLResourceContext(UserPrincipal(u, null))
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, Grade>>()
        EasyMock.expect(mockEnv.getDataLoader<UUID, Grade>("responsegrades")).andReturn(mockDataLoader)
        val g = Grade(UUID.randomUUID(), r.id!!, true, 5)
        EasyMock.expect(mockDataLoader.load(r.id)).andReturn(
            CompletableFuture.completedFuture(g)
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        assertThat(r.grade(context, mockEnv).await()).isEqualTo(g)
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testNullGrade() = runBlocking {
        val context = GraphQLResourceContext(UserPrincipal(u, null))
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, Grade>>()
        EasyMock.expect(mockEnv.getDataLoader<UUID, Grade>("responsegrades")).andReturn(mockDataLoader)
        EasyMock.expect(mockDataLoader.load(r.id)).andReturn(
            CompletableFuture.completedFuture(null)
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        assertThat(r.grade(context, mockEnv).await()).isNull()
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testNoContextGrade() = runBlocking {
        val context = GraphQLResourceContext(null)
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        EasyMock.replay(mockEnv)
        assertThat(r.grade(context, mockEnv).await()).isNull()
        EasyMock.verify(mockEnv)
    }

    @Test
    fun testNoIdGrade() = runBlocking {
        val context = GraphQLResourceContext(UserPrincipal(u, null))
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        EasyMock.replay(mockEnv)
        assertThat(r.copy(id = null).grade(context, mockEnv).await()).isNull()
        EasyMock.verify(mockEnv)
    }
}
