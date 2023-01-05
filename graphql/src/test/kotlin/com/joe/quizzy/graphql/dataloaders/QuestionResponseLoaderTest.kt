package com.joe.quizzy.graphql.dataloaders

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.graphql.auth.UserPrincipal
import com.joe.quizzy.persistence.api.ResponseDAO
import com.trib3.graphql.resources.getGraphQLContextMap
import com.trib3.testing.LeakyMock
import graphql.GraphQLContext
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.dataloader.BatchLoaderEnvironment
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.util.UUID

class QuestionResponseLoaderTest {
    @Test
    fun testQuestionResponseLoader() = runBlocking {
        val responseDAO = LeakyMock.mock<ResponseDAO>()
        val mockEnv = LeakyMock.mock<BatchLoaderEnvironment>()
        val loader = QuestionResponseLoader(responseDAO, mapOf<Any, Any>())
        val questionResponses = mapOf(
            UUID.randomUUID() to Response(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "a1", "rr1"),
            UUID.randomUUID() to Response(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "a2", "rr2"),
        )
        val userId = UUID.randomUUID()
        EasyMock.expect(
            responseDAO.byUserQuestions(
                EasyMock.eq(userId) ?: userId,
                EasyMock.anyObject<List<UUID>>() ?: listOf(),
            ),
        ).andReturn(questionResponses)
        EasyMock.expect(mockEnv.getContext<GraphQLContext>()).andReturn(
            GraphQLContext.of(
                getGraphQLContextMap(
                    this,
                    UserPrincipal(
                        User(userId, UUID.randomUUID(), "user", "user@user.com", "", false, ""),
                        null,
                    ),
                ),
            ),
        ).atLeastOnce()
        EasyMock.replay(responseDAO, mockEnv)
        val insts = loader.load(questionResponses.mapNotNull { it.key }.toSet(), mockEnv).await()
        assertThat(insts).isEqualTo(questionResponses)
        EasyMock.verify(responseDAO, mockEnv)
    }

    @Test
    fun testNoContextUser() = runBlocking {
        val responseDAO = LeakyMock.mock<ResponseDAO>()
        val mockEnv = LeakyMock.mock<BatchLoaderEnvironment>()
        val loader = QuestionResponseLoader(responseDAO, mapOf<Any, Any>())
        EasyMock.expect(mockEnv.getContext<GraphQLContext?>())
            .andReturn(GraphQLContext.of(getGraphQLContextMap(this))).atLeastOnce()
        EasyMock.replay(responseDAO, mockEnv)
        val insts = loader.load(setOf(UUID.randomUUID(), UUID.randomUUID()), mockEnv).await()
        assertThat(insts).isEqualTo(mapOf())
        EasyMock.verify(responseDAO, mockEnv)
    }
}
