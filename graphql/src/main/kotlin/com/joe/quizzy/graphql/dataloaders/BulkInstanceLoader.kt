package com.joe.quizzy.graphql.dataloaders

import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.persistence.api.InstanceDAO
import com.trib3.graphql.execution.CoroutineMappedBatchLoader
import org.dataloader.BatchLoaderEnvironment
import java.util.UUID

/**
 * Batch load Instance by Id
 */
class BulkInstanceLoader(private val instanceDAO: InstanceDAO, contextMap: Map<*, Any>) :
    CoroutineMappedBatchLoader<UUID, Instance>(contextMap) {
    override val dataLoaderName = "batchinstances"

    override suspend fun loadSuspend(keys: Set<UUID>, environment: BatchLoaderEnvironment): Map<UUID, Instance> {
        return instanceDAO.get(keys.toList()).associateBy {
            val id = it.id
            requireNotNull(id)
            id
        }
    }
}
