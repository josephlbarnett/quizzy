package com.joe.quizzy.graphql.dataloaders

import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.persistence.api.GradeDAO
import com.trib3.graphql.execution.CoroutineMappedBatchLoader
import org.dataloader.BatchLoaderEnvironment
import java.util.UUID

/**
 * Batch load User ID -> List<Grade>
 */
class UserGradeLoader(private val gradeDAO: GradeDAO, contextMap: Map<*, Any>) :
    CoroutineMappedBatchLoader<UUID, List<Grade>>(contextMap) {
    override val dataLoaderName = "usergrades"

    override suspend fun loadSuspend(keys: Set<UUID>, environment: BatchLoaderEnvironment): Map<UUID, List<Grade>> {
        return gradeDAO.forUsers(keys.toList())
    }
}
