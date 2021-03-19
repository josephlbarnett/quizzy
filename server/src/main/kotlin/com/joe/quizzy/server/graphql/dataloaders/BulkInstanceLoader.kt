package com.joe.quizzy.server.graphql.dataloaders

import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.persistence.api.InstanceDAO
import org.dataloader.BatchLoaderEnvironment
import java.util.UUID

/**
 * Batch load Instance by Id
 */
class BulkInstanceLoader(private val instanceDAO: InstanceDAO) :
    CoroutineMappedBatchLoader<UUID, Instance>() {
    override suspend fun loadSuspend(keys: Set<UUID>, environment: BatchLoaderEnvironment): Map<UUID, Instance> {
        return instanceDAO.get(keys.toList()).associateBy {
            val id = it.id
            require(id != null)
            id
        }
    }
}
