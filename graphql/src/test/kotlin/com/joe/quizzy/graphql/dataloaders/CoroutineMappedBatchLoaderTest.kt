package com.joe.quizzy.graphql.dataloaders

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isLessThan
import assertk.assertions.messageContains
import com.trib3.testing.LeakyMock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.dataloader.BatchLoaderEnvironment
import org.easymock.EasyMock
import org.testng.annotations.Test

class CoroutineMappedBatchLoaderTest {
    @Test
    fun testLoad() = runBlocking {
        val loader = object : CoroutineMappedBatchLoader<String, String>() {
            override suspend fun loadSuspend(
                keys: Set<String>,
                environment: BatchLoaderEnvironment
            ): Map<String, String> {
                return keys.associateBy { it }
            }
        }
        val mockEnv = LeakyMock.mock<BatchLoaderEnvironment>()
        EasyMock.expect(mockEnv.getContext<Any?>()).andReturn(null)
        EasyMock.replay(mockEnv)
        val loaded = loader.load(setOf("1", "2", "3"), mockEnv).await()
        assertThat(loaded).isEqualTo(mapOf("1" to "1", "2" to "2", "3" to "3"))
        EasyMock.verify(mockEnv)
    }

    @Test
    fun testCancellation() = runBlocking(Dispatchers.Unconfined) {
        val loader = object : CoroutineMappedBatchLoader<String, String>() {
            override suspend fun loadSuspend(
                keys: Set<String>,
                environment: BatchLoaderEnvironment
            ): Map<String, String> {
                delay(20000)
                throw IllegalStateException("Should not get here")
            }
        }
        val mockEnv = LeakyMock.mock<BatchLoaderEnvironment>()
        EasyMock.expect(mockEnv.getContext<Any?>()).andReturn(this)
        EasyMock.replay(mockEnv)
        val loading = loader.load(setOf("1", "2", "3"), mockEnv)
        this.coroutineContext[Job]?.cancelChildren()
        val startAwaitTime = System.currentTimeMillis()
        assertThat {
            loading.await()
        }.isFailure().messageContains("was cancelled")
        // ensure the delay() is not hit, but allow for slow test machines
        assertThat(System.currentTimeMillis() - startAwaitTime).isLessThan(19000)
        EasyMock.verify(mockEnv)
    }
}
