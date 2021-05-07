package com.joe.quizzy.graphql.dataloaders

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.MappedBatchLoaderWithContext
import java.util.concurrent.CompletionStage

abstract class CoroutineMappedBatchLoader<K, V> : MappedBatchLoaderWithContext<K, V> {

    private fun scope(environment: BatchLoaderEnvironment): CoroutineScope {
        return (environment.getContext() as? CoroutineScope) ?: GlobalScope
    }

    abstract suspend fun loadSuspend(keys: Set<K>, environment: BatchLoaderEnvironment): Map<K, V>

    override fun load(keys: Set<K>, environment: BatchLoaderEnvironment): CompletionStage<Map<K, V>> {
        return scope(environment).future {
            loadSuspend(keys, environment)
        }
    }
}
