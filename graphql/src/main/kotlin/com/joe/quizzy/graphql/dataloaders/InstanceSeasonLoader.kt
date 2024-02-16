package com.joe.quizzy.graphql.dataloaders

import com.joe.quizzy.api.models.Season
import com.joe.quizzy.persistence.api.SeasonDAO
import com.trib3.graphql.execution.CoroutineMappedBatchLoader
import org.dataloader.BatchLoaderEnvironment
import java.time.OffsetDateTime
import java.util.UUID

data class InstanceTimePeriod(val instanceId: UUID, val startTime: OffsetDateTime?, val endTime: OffsetDateTime?)

class InstanceSeasonLoader(private val seasonDAO: SeasonDAO) :
    CoroutineMappedBatchLoader<InstanceTimePeriod, List<Season>>() {
    override val dataLoaderName = "instanceseasons"

    override suspend fun loadSuspend(
        keys: Set<InstanceTimePeriod>,
        environment: BatchLoaderEnvironment,
    ): Map<InstanceTimePeriod, List<Season>> {
        val groupedIds = keys.groupBy { it.startTime to it.endTime }
        val all =
            groupedIds.map { groupedEntry ->
                seasonDAO.getSeasons(
                    groupedEntry.value.map { it.instanceId },
                    groupedEntry.key.first,
                    groupedEntry.key.second,
                )
                    .mapKeys { InstanceTimePeriod(it.key, groupedEntry.key.first, groupedEntry.key.second) }
            }
        return all.reduce { a, b ->
            a + b
        }
    }
}
