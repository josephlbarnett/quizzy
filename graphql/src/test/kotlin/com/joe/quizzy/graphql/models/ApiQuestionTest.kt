package com.joe.quizzy.graphql.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.QuestionType
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.graphql.auth.UserPrincipal
import com.trib3.graphql.resources.getGraphQLContextMap
import com.trib3.testing.LeakyMock
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.dataloader.DataLoader
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

class ApiQuestionTest {
    val scope = CoroutineScope(Dispatchers.Default)
    val now = OffsetDateTime.now()
    val q = ApiQuestion(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "q1",
        "a1",
        "r1",
        now,
        now,
        QuestionType.SHORT_ANSWER,
        listOf(),
        null,
        15,
    )
    val u = User(UUID.randomUUID(), UUID.randomUUID(), "name", "email", "", false, "")

    @Test
    fun testCopyConstructor() {
        assertThat(q).isEqualTo(
            ApiQuestion(
                Question(
                    q.id,
                    q.authorId,
                    q.body,
                    q.answer,
                    q.ruleReferences,
                    q.activeAt,
                    q.closedAt,
                    q.type,
                    q.answerChoices,
                ),
                15,
            ),
        )
    }

    @Test
    fun testResponse() = runBlocking {
        val context = GraphQLContext.of(
            getGraphQLContextMap(scope, UserPrincipal(u, null)),
        )
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, Response>>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.expect(mockEnv.getDataLoader<UUID, Response>("questionresponses")).andReturn(mockDataLoader)
        EasyMock.expect(mockDataLoader.load(q.id)).andReturn(
            CompletableFuture.completedFuture(
                Response(
                    UUID.randomUUID(),
                    u.id!!,
                    q.id!!,
                    "answer",
                    "reference",
                ),
            ),
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        val resp = q.response(mockEnv).await()
        assertThat(resp?.questionId).isEqualTo(q.id)
        assertThat(resp?.response).isEqualTo("answer")
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testNullResponse() = runBlocking {
        val context = GraphQLContext.of(
            getGraphQLContextMap(scope, UserPrincipal(u, null)),
        )
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, Response>>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.expect(mockEnv.getDataLoader<UUID, Response>("questionresponses")).andReturn(mockDataLoader)
        EasyMock.expect(mockDataLoader.load(q.id)).andReturn(
            CompletableFuture.completedFuture(null),
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        val resp = q.response(mockEnv).await()
        assertThat(resp).isNull()
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testNullContextUser() = runBlocking {
        val context = GraphQLContext.of(
            getGraphQLContextMap(scope),
        )
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.replay(mockEnv)
        val resp = q.response(mockEnv).await()
        assertThat(resp).isNull()
        EasyMock.verify(mockEnv)
    }

    @Test
    fun testNullId() = runBlocking {
        val context = GraphQLContext.of(
            getGraphQLContextMap(scope, UserPrincipal(u, null)),
        )
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.replay(mockEnv)
        val resp = q.copy(id = null).response(mockEnv).await()
        assertThat(resp).isNull()
        EasyMock.verify(mockEnv)
    }

    @Test
    fun testAuthor() = runBlocking {
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, User>>()
        EasyMock.expect(mockEnv.getDataLoader<UUID, User>("batchusers")).andReturn(mockDataLoader)
        EasyMock.expect(mockDataLoader.load(q.authorId)).andReturn(CompletableFuture.completedFuture(u))
        EasyMock.replay(mockEnv, mockDataLoader)
        val resp = q.author(mockEnv).await()
        assertThat(resp).isEqualTo(ApiUser(u, 15))
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testNullAuthor() = runBlocking {
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, User>>()
        EasyMock.expect(mockEnv.getDataLoader<UUID, User>("batchusers")).andReturn(mockDataLoader)
        EasyMock.expect(mockDataLoader.load(q.authorId)).andReturn(
            CompletableFuture.completedFuture(null),
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        val resp = q.author(mockEnv).await()
        assertThat(resp).isNull()
        EasyMock.verify(mockEnv, mockDataLoader)
    }
}
