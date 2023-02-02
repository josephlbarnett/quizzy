package com.joe.quizzy.graphql.dataloaders

import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.persistence.api.GradeDAO
import com.trib3.graphql.execution.CoroutineMappedBatchLoader
import org.dataloader.BatchLoaderEnvironment
import java.time.OffsetDateTime
import java.util.UUID

data class UserTimePeriod(val userId: UUID, val startTime: OffsetDateTime?, val endTime: OffsetDateTime?)

/**
 * Batch load User ID -> List<Grade>
 */
class UserGradeLoader(private val gradeDAO: GradeDAO, contextMap: Map<*, Any>) :
    CoroutineMappedBatchLoader<UserTimePeriod, List<Grade>>(contextMap) {
    override val dataLoaderName = "usergrades"

    override suspend fun loadSuspend(
        keys: Set<UserTimePeriod>,
        environment: BatchLoaderEnvironment,
    ): Map<UserTimePeriod, List<Grade>> {
        val groupedIds = keys.groupBy { it.startTime to it.endTime }
        val all = groupedIds.map { groupedEntry ->
            gradeDAO.forUsers(groupedEntry.value.map { it.userId }, groupedEntry.key.first, groupedEntry.key.second)
                .mapKeys { UserTimePeriod(it.key, groupedEntry.key.first, groupedEntry.key.second) }
        }
        return all.reduce { a, b ->
            a + b
        }
    }
}
