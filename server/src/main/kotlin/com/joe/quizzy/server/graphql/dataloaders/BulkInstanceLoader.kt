package com.joe.quizzy.server.graphql.dataloaders

import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.persistence.api.InstanceDAO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.coroutines.slf4j.MDCContext
import org.dataloader.MappedBatchLoader
import java.util.UUID
import java.util.concurrent.CompletionStage

/**
 * Batch load Instance by Id
 */
class BulkInstanceLoader(private val instanceDAO: InstanceDAO) :
    MappedBatchLoader<UUID, Instance> {
    override fun load(keys: Set<UUID>): CompletionStage<Map<UUID, Instance>> {
        return CoroutineScope(Dispatchers.IO + MDCContext())
            .future {
                instanceDAO.get(keys.toList()).associateBy { it.id!! }
            }
    }
}
