package com.joe.quizzy.graphql.dataloaders

import com.joe.quizzy.graphql.groupme.GroupMeService
import com.joe.quizzy.graphql.groupme.GroupMeServiceFactory
import com.trib3.graphql.execution.CoroutineMappedBatchLoader
import mu.KotlinLogging
import org.dataloader.BatchLoaderEnvironment
import java.util.UUID

private val log = KotlinLogging.logger { }

/**
 * Batch load GroupMeService by instance ID
 */
class GroupMeServiceLoader(private val factory: GroupMeServiceFactory, contextMap: Map<*, Any>) :
    CoroutineMappedBatchLoader<UUID, GroupMeService?>(contextMap) {
    override val dataLoaderName = "groupmeservice"

    override suspend fun loadSuspend(keys: Set<UUID>, environment: BatchLoaderEnvironment): Map<UUID, GroupMeService?> {
        return keys.associateWith {
            try {
                factory.create(it)
            } catch (e: IllegalStateException) {
                log.trace("Invalid groupme for instance: $it", e)
                null
            }
        }
    }
}
