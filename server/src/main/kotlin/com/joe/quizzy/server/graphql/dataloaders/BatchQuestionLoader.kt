package com.joe.quizzy.server.graphql.dataloaders

import com.joe.quizzy.api.models.Question
import com.joe.quizzy.persistence.api.QuestionDAO
import org.dataloader.BatchLoaderEnvironment
import java.util.UUID

/**
 * Batch load Questions by ID
 */
class BatchQuestionLoader(private val questionDAO: QuestionDAO) :
    CoroutineMappedBatchLoader<UUID, Question>() {
    override suspend fun loadSuspend(keys: Set<UUID>, environment: BatchLoaderEnvironment): Map<UUID, Question> {
        return questionDAO.get(keys.toList()).associateBy {
            val id = it.id
            require(id != null)
            id
        }
    }
}
