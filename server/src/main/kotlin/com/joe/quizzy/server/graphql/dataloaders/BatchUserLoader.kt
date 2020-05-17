package com.joe.quizzy.server.graphql.dataloaders

import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.UserDAO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.coroutines.slf4j.MDCContext
import org.dataloader.MappedBatchLoader
import java.util.UUID
import java.util.concurrent.CompletionStage

/**
 * Batch load Users by ID
 */
class BatchUserLoader(private val userDAO: UserDAO) :
    MappedBatchLoader<UUID, User> {
    override fun load(keys: Set<UUID>): CompletionStage<Map<UUID, User>> {
        return CoroutineScope(Dispatchers.IO + MDCContext())
            .future {
                userDAO.get(keys.toList()).associateBy { it.id!! }
            }
    }
}
