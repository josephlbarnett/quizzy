package com.joe.quizzy.graphql.dataloaders

import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.persistence.api.GradeDAO
import com.trib3.graphql.execution.CoroutineMappedBatchLoader
import org.dataloader.BatchLoaderEnvironment
import java.util.UUID

/**
 * Batch load Response ID -> Grade
 */
class ResponseGradeLoader(private val gradeDAO: GradeDAO) :
    CoroutineMappedBatchLoader<UUID, Grade>() {
    override val dataLoaderName = "responsegrades"

    override suspend fun loadSuspend(
        keys: Set<UUID>,
        environment: BatchLoaderEnvironment,
    ): Map<UUID, Grade> {
        return gradeDAO.forResponses(keys.toList())
    }
}
