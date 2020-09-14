package com.joe.quizzy.server.graphql.dataloaders

import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.persistence.api.GradeDAO
import org.dataloader.BatchLoaderEnvironment
import java.util.UUID

/**
 * Batch load User ID -> List<Grade>
 */
class UserGradeLoader(private val gradeDAO: GradeDAO) :
    CoroutineMappedBatchLoader<UUID, List<Grade>>() {
    override suspend fun loadSuspend(keys: Set<UUID>, environment: BatchLoaderEnvironment): Map<UUID, List<Grade>> {
        return gradeDAO.forUsers(keys.toList())
    }
}
