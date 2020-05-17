package com.joe.quizzy.server.graphql.dataloaders

import assertk.assertThat
import assertk.assertions.isNotNull
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.trib3.graphql.execution.GraphQLRequest
import com.trib3.testing.mock
import org.easymock.EasyMockSupport
import org.testng.annotations.Test
import java.util.UUID

class DataLoaderRegistryFactoryProviderTest : EasyMockSupport() {
    @Test
    fun testRegistry() {
        val factoryProvider = DataLoaderRegistryFactoryProvider(
            mock(),
            mock(),
            mock(),
            mock(),
            mock()
        )
        replayAll()
        val factory = factoryProvider.get()
        val registry = factory(GraphQLRequest("{}", mapOf(), null), null)
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

        val batchInstanceLoader = registry.getDataLoader<UUID, Instance>("batchinstances")
        assertThat(batchInstanceLoader).isNotNull()
    }
}
