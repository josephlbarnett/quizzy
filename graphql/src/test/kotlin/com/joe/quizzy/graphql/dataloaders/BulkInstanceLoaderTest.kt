package com.joe.quizzy.graphql.dataloaders

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.persistence.api.InstanceDAO
import com.trib3.testing.LeakyMock
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.dataloader.BatchLoaderEnvironment
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.util.UUID

class BulkInstanceLoaderTest {
    @Test
    fun testInstanceLoader() = runBlocking {
        val instanceDAO = LeakyMock.mock<InstanceDAO>()
        val mockEnv = LeakyMock.mock<BatchLoaderEnvironment>()
        val loader = BulkInstanceLoader(instanceDAO)
        val instances = listOf(
            Instance(UUID.randomUUID(), "i1", "ACTIVE", ""),
            Instance(UUID.randomUUID(), "i2", "ACTIVE", "")
        )
        EasyMock.expect(instanceDAO.get(EasyMock.anyObject<List<UUID>>() ?: listOf())).andReturn(instances)
        EasyMock.expect(mockEnv.getContext<Any?>()).andReturn(null)
        EasyMock.replay(instanceDAO, mockEnv)
        val insts = loader.load(instances.mapNotNull { it.id }.toSet(), mockEnv).await()
        assertThat(insts).isEqualTo(instances.associateBy { it.id })
        EasyMock.verify(instanceDAO, mockEnv)
    }
}
