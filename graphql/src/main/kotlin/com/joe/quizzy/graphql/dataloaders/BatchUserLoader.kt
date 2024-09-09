package com.joe.quizzy.graphql.dataloaders

import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.graphql.execution.CoroutineMappedBatchLoader
import org.dataloader.BatchLoaderEnvironment
import java.util.UUID

/**
 * Batch load Users by ID
 */
class BatchUserLoader(
    private val userDAO: UserDAO,
) : CoroutineMappedBatchLoader<UUID, User>() {
    override val dataLoaderName = "batchusers"

    override suspend fun loadSuspend(
        keys: Set<UUID>,
        environment: BatchLoaderEnvironment,
    ): Map<UUID, User> =
        userDAO.get(keys.toList()).associateBy {
            val id = it.id
            requireNotNull(id)
            id
        }
}
