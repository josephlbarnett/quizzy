package com.joe.quizzy.graphql.dataloaders

import com.joe.quizzy.graphql.groupme.GroupMeService
import com.joe.quizzy.graphql.groupme.GroupMeServiceFactory
import com.trib3.graphql.execution.CoroutineMappedBatchLoader
import io.github.oshai.kotlinlogging.KotlinLogging
import org.dataloader.BatchLoaderEnvironment
import java.util.UUID

private val log = KotlinLogging.logger { }

/**
 * Batch load GroupMeService by instance ID
 */
class GroupMeServiceLoader(
    private val factory: GroupMeServiceFactory,
) : CoroutineMappedBatchLoader<UUID, GroupMeService?>() {
    override val dataLoaderName = "groupmeservice"

    override suspend fun loadSuspend(
        keys: Set<UUID>,
        environment: BatchLoaderEnvironment,
    ): Map<UUID, GroupMeService?> =
        keys.associateWith {
            try {
                factory.create(it)
            } catch (e: IllegalStateException) {
                log.trace(e) { "Invalid groupme for instance: $it" }
                null
            }
        }
}
