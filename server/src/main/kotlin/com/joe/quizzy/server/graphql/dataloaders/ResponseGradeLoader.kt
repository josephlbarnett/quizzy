package com.joe.quizzy.server.graphql.dataloaders

import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.persistence.api.GradeDAO
import org.dataloader.BatchLoaderEnvironment
import java.util.UUID

/**
 * Batch load Response ID -> Grade
 */
class ResponseGradeLoader(private val gradeDAO: GradeDAO) :
    CoroutineMappedBatchLoader<UUID, Grade>() {
    override suspend fun loadSuspend(keys: Set<UUID>, environment: BatchLoaderEnvironment): Map<UUID, Grade> {
        return gradeDAO.forResponses(keys.toList())
    }
}
