package com.joe.quizzy.graphql.dataloaders

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.graphql.auth.UserPrincipal
import com.joe.quizzy.persistence.api.ResponseDAO
import com.trib3.graphql.execution.GraphQLRequest
import com.trib3.graphql.resources.GraphQLResourceContext
import com.trib3.testing.mock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.easymock.EasyMock
import org.easymock.EasyMockSupport
import org.testng.annotations.Test
import java.util.UUID

class DataLoaderRegistryFactoryProviderTest : EasyMockSupport() {
    @Test
    fun testRegistry() {
        val responseDAO = mock<ResponseDAO>()
        val factoryProvider = DataLoaderRegistryFactoryProvider(
            mock(),
            mock(),
            mock(),
            responseDAO,
            mock()
        )
        val userUUID = UUID.randomUUID()
        val questionUUID = UUID.randomUUID()
        // expect `byUserQuestions` to be called with context user UUID
        EasyMock.expect(
            responseDAO.byUserQuestions(EasyMock.eq(userUUID) ?: userUUID, EasyMock.anyObject() ?: listOf())
        ).andReturn(mapOf(questionUUID to Response(UUID.randomUUID(), userUUID, questionUUID, "r", "rr")))
        replayAll()
        val factory = factoryProvider.get()
        val scope = CoroutineScope(Dispatchers.Default)
        val registry = factory(
            GraphQLRequest("{}", mapOf(), null),
            GraphQLResourceContext(
                UserPrincipal(
                    User(userUUID, UUID.randomUUID(), "name", "email", "", false, ""),
                    null
                ),
                scope
            )
        )

        val batchQuestionLoader = registry.getDataLoader<UUID, Question>("batchquestions")
        assertThat(batchQuestionLoader).isNotNull()

        val responseGradesLoader = registry.getDataLoader<UUID, Grade>("responsegrades")
        assertThat(responseGradesLoader).isNotNull()

        val userGradesLoader = registry.getDataLoader<UUID, Grade>("usergrades")
        assertThat(userGradesLoader).isNotNull()

        val batchUserLoader = registry.getDataLoader<UUID, User>("batchusers")
        assertThat(batchUserLoader).isNotNull()

        val questionResponseLoader = registry.getDataLoader<UUID, Response>("questionresponses")
        assertThat(questionResponseLoader).isNotNull()
        val loadedQR = questionResponseLoader.load(questionUUID)
        questionResponseLoader.dispatch()
        runBlocking {
            assertThat(loadedQR.await().response).isEqualTo("r")
        }

        val batchInstanceLoader = registry.getDataLoader<UUID, Instance>("batchinstances")
        assertThat(batchInstanceLoader).isNotNull()
        verifyAll()
    }
}
