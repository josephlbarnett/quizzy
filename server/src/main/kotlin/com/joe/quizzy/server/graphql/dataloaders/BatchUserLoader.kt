package com.joe.quizzy.server.graphql.dataloaders

import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.UserDAO
import org.dataloader.BatchLoaderEnvironment
import java.util.UUID

/**
 * Batch load Users by ID
 */
class BatchUserLoader(private val userDAO: UserDAO) :
    CoroutineMappedBatchLoader<UUID, User>() {
    override suspend fun loadSuspend(keys: Set<UUID>, environment: BatchLoaderEnvironment): Map<UUID, User> {
        return userDAO.get(keys.toList()).associateBy { it.id!! }
    }
}
