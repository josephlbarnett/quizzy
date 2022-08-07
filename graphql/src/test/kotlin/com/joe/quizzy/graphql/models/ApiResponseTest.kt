package com.joe.quizzy.graphql.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.api.models.Question
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

class ApiResponseTest {
    val scope = CoroutineScope(Dispatchers.Default)
    val now = OffsetDateTime.now()
    val r = ApiResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "answer",
        "references",
        15
    )
    val u = User(UUID.randomUUID(), UUID.randomUUID(), "name", "email", "", false, "")

    @Test
    fun testCopyConstructor() {
        assertThat(r).isEqualTo(ApiResponse(Response(r.id, r.userId, r.questionId, r.response, r.ruleReferences), 15))
    }

    @Test
    fun testUser() = runBlocking {
        val context = GraphQLContext.of(
            getGraphQLContextMap(scope, UserPrincipal(u, null))
        )
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, User>>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.expect(mockEnv.getDataLoader<UUID, User>("batchusers")).andReturn(mockDataLoader)
        EasyMock.expect(mockDataLoader.load(r.userId)).andReturn(
            CompletableFuture.completedFuture(u)
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        assertThat(r.user(mockEnv).await()).isEqualTo(ApiUser(u, 15))
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testNullUser() = runBlocking {
        val context = GraphQLContext.of(
            getGraphQLContextMap(scope, UserPrincipal(u, null))
        )
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, User>>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.expect(mockEnv.getDataLoader<UUID, User>("batchusers")).andReturn(mockDataLoader)
        EasyMock.expect(mockDataLoader.load(r.userId)).andReturn(
            CompletableFuture.completedFuture(null)
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        assertThat(r.user(mockEnv).await()).isNull()
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testNoContextUser() = runBlocking {
        val context = GraphQLContext.of(getGraphQLContextMap(scope))
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.replay(mockEnv)
        assertThat(r.user(mockEnv).await()).isNull()
        EasyMock.verify(mockEnv)
    }

    @Test
    fun testQuestionNullGrade() = runBlocking {
        val context = GraphQLContext.of(
            getGraphQLContextMap(scope, UserPrincipal(u, null))
        )
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, Question>>()
        val mockGradeLoader = LeakyMock.mock<DataLoader<UUID, Grade>>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.expect(mockEnv.getDataLoader<UUID, Question>("batchquestions")).andReturn(mockDataLoader)
        EasyMock.expect(mockEnv.getDataLoader<UUID, Grade>("responsegrades")).andReturn(mockGradeLoader)
        val q = Question(r.questionId, UUID.randomUUID(), "b", "a", "r", now, now)
        EasyMock.expect(mockDataLoader.load(r.questionId)).andReturn(
            CompletableFuture.completedFuture(q)
        )
        EasyMock.expect(mockGradeLoader.load(r.id)).andReturn(
            CompletableFuture.completedFuture(null)
        )
        EasyMock.replay(mockEnv, mockDataLoader, mockGradeLoader)
        assertThat(r.question(mockEnv).await()).isEqualTo(ApiQuestion(q, 15).copy(answer = "", ruleReferences = ""))
        EasyMock.verify(mockEnv, mockDataLoader, mockGradeLoader)
    }

    @Test
    fun testQuestionWithGrade() = runBlocking {
        val context = GraphQLContext.of(
            getGraphQLContextMap(scope, UserPrincipal(u, null))
        )
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, Question>>()
        val mockGradeLoader = LeakyMock.mock<DataLoader<UUID, Grade>>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.expect(mockEnv.getDataLoader<UUID, Question>("batchquestions")).andReturn(mockDataLoader)
        EasyMock.expect(mockEnv.getDataLoader<UUID, Grade>("responsegrades")).andReturn(mockGradeLoader)
        val q = Question(r.questionId, UUID.randomUUID(), "b", "a", "r", now, now)
        EasyMock.expect(mockDataLoader.load(r.questionId)).andReturn(
            CompletableFuture.completedFuture(q)
        )
        EasyMock.expect(mockGradeLoader.load(r.id)).andReturn(
            CompletableFuture.completedFuture(Grade(null, r.id!!, true, 0))
        )
        EasyMock.replay(mockEnv, mockDataLoader, mockGradeLoader)
        assertThat(r.question(mockEnv).await()).isEqualTo(ApiQuestion(q, 15))
        EasyMock.verify(mockEnv, mockDataLoader, mockGradeLoader)
    }

    @Test
    fun testNullQuestion() = runBlocking {
        val context = GraphQLContext.of(
            getGraphQLContextMap(scope, UserPrincipal(u, null))
        )
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, Question>>()
        val mockGradeLoader = LeakyMock.mock<DataLoader<UUID, Grade>>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.expect(mockEnv.getDataLoader<UUID, Question>("batchquestions")).andReturn(mockDataLoader)
        EasyMock.expect(mockEnv.getDataLoader<UUID, Grade>("responsegrades")).andReturn(mockGradeLoader)
        EasyMock.expect(mockDataLoader.load(r.questionId)).andReturn(
            CompletableFuture.completedFuture(null)
        )
        EasyMock.expect(mockGradeLoader.load(r.id)).andReturn(
            CompletableFuture.completedFuture(null)
        )
        EasyMock.replay(mockEnv, mockDataLoader, mockGradeLoader)
        assertThat(r.question(mockEnv).await()).isNull()
        EasyMock.verify(mockEnv, mockDataLoader, mockGradeLoader)
    }

    @Test
    fun testNoContextQuestion() = runBlocking {
        val context = GraphQLContext.of(getGraphQLContextMap(scope))
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.replay(mockEnv)
        assertThat(r.question(mockEnv).await()).isNull()
        EasyMock.verify(mockEnv)
    }

    @Test
    fun testGrade() = runBlocking {
        val context = GraphQLContext.of(
            getGraphQLContextMap(scope, UserPrincipal(u, null))
        )
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, Grade>>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.expect(mockEnv.getDataLoader<UUID, Grade>("responsegrades")).andReturn(mockDataLoader)
        val g = Grade(UUID.randomUUID(), r.id!!, true, 5)
        EasyMock.expect(mockDataLoader.load(r.id)).andReturn(
            CompletableFuture.completedFuture(g)
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        assertThat(r.grade(mockEnv).await()).isEqualTo(ApiGrade(g, 15))
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testNullGrade() = runBlocking {
        val context = GraphQLContext.of(
            getGraphQLContextMap(scope, UserPrincipal(u, null))
        )
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        val mockDataLoader = LeakyMock.mock<DataLoader<UUID, Grade>>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.expect(mockEnv.getDataLoader<UUID, Grade>("responsegrades")).andReturn(mockDataLoader)
        EasyMock.expect(mockDataLoader.load(r.id)).andReturn(
            CompletableFuture.completedFuture(null)
        )
        EasyMock.replay(mockEnv, mockDataLoader)
        assertThat(r.grade(mockEnv).await()).isNull()
        EasyMock.verify(mockEnv, mockDataLoader)
    }

    @Test
    fun testNoContextGrade() = runBlocking {
        val context = GraphQLContext.of(getGraphQLContextMap(scope))
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.replay(mockEnv)
        assertThat(r.grade(mockEnv).await()).isNull()
        EasyMock.verify(mockEnv)
    }

    @Test
    fun testNoIdGrade() = runBlocking {
        val context = GraphQLContext.of(
            getGraphQLContextMap(scope, UserPrincipal(u, null))
        )
        val mockEnv = LeakyMock.mock<DataFetchingEnvironment>()
        EasyMock.expect(mockEnv.graphQlContext).andReturn(context)
        EasyMock.replay(mockEnv)
        assertThat(r.copy(id = null).grade(mockEnv).await()).isNull()
        EasyMock.verify(mockEnv)
    }
}
