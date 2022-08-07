package com.joe.quizzy.graphql.dataloaders

import com.joe.quizzy.api.models.Question
import com.joe.quizzy.persistence.api.QuestionDAO
import com.trib3.graphql.execution.CoroutineMappedBatchLoader
import org.dataloader.BatchLoaderEnvironment
import java.util.UUID

/**
 * Batch load Questions by ID
 */
class BatchQuestionLoader(private val questionDAO: QuestionDAO, contextMap: Map<*, Any>) :
    CoroutineMappedBatchLoader<UUID, Question>(contextMap) {
    override val dataLoaderName = "batchquestions"

    override suspend fun loadSuspend(keys: Set<UUID>, environment: BatchLoaderEnvironment): Map<UUID, Question> {
        return questionDAO.get(keys.toList()).associateBy {
            val id = it.id
            requireNotNull(id)
            id
        }
    }
}
