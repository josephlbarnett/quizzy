package com.joe.quizzy.graphql.dataloaders

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.testing.LeakyMock
import graphql.GraphQLContext
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.dataloader.BatchLoaderEnvironment
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.util.UUID

class BatchUserLoaderTest {
    @Test
    fun testUserLoader() = runBlocking {
        val userDAO = LeakyMock.mock<UserDAO>()
        val mockEnv = LeakyMock.mock<BatchLoaderEnvironment>()
        val loader = BatchUserLoader(userDAO)
        val users = listOf(
            User(UUID.randomUUID(), UUID.randomUUID(), "joe", "joe@joe.com", "", false, ""),
            User(UUID.randomUUID(), UUID.randomUUID(), "bill", "bill@bill.com", "", false, ""),
        )
        EasyMock.expect(mockEnv.getContext<GraphQLContext>()).andReturn(GraphQLContext.getDefault())
        EasyMock.expect(userDAO.get(EasyMock.anyObject<List<UUID>>() ?: listOf())).andReturn(users)
        EasyMock.replay(userDAO, mockEnv)
        val us = loader.load(users.mapNotNull { it.id }.toSet(), mockEnv).await()
        assertThat(us).isEqualTo(users.associateBy { it.id })
        EasyMock.verify(userDAO, mockEnv)
    }
}
