package com.joe.quizzy.graphql.dataloaders

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.joe.quizzy.graphql.groupme.GroupMeService
import com.joe.quizzy.graphql.groupme.GroupMeServiceFactory
import com.trib3.testing.LeakyMock
import kotlinx.coroutines.runBlocking
import org.dataloader.BatchLoaderEnvironment
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.util.UUID

class GroupMeServiceLoaderTest {
    @Test
    fun testLoader() = runBlocking {
        val factory = LeakyMock.mock<GroupMeServiceFactory>()
        val mockEnv = LeakyMock.mock<BatchLoaderEnvironment>()
        val loader = GroupMeServiceLoader(factory, emptyMap<Any, Any>())
        val instances = listOf(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
        )
        val mockService = LeakyMock.mock<GroupMeService>()
        EasyMock.expect(factory.create(instances[0])).andReturn(null)
        EasyMock.expect(factory.create(instances[1])).andReturn(mockService)
        EasyMock.expect(factory.create(instances[2])).andThrow(IllegalStateException("ise"))
        EasyMock.replay(factory, mockEnv, mockService)
        val loaded = loader.loadSuspend(
            instances.toSet(),
            mockEnv,
        )
        assertThat(loaded).isEqualTo(
            mapOf(
                instances[0] to null,
                instances[1] to mockService,
                instances[2] to null,
            ),
        )
        EasyMock.verify(factory, mockEnv, mockService)
    }
}
