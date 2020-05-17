package com.joe.quizzy.server.graphql.dataloaders

import com.joe.quizzy.api.models.Question
import com.joe.quizzy.persistence.api.QuestionDAO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.coroutines.slf4j.MDCContext
import org.dataloader.MappedBatchLoader
import java.util.UUID
import java.util.concurrent.CompletionStage

/**
 * Batch load Questions by ID
 */
class BatchQuestionLoader(private val questionDAO: QuestionDAO) :
    MappedBatchLoader<UUID, Question> {
    override fun load(keys: Set<UUID>): CompletionStage<Map<UUID, Question>> {
        return CoroutineScope(Dispatchers.IO + MDCContext())
            .future {
                questionDAO.get(keys.toList()).associateBy { it.id!! }
            }
    }
}
