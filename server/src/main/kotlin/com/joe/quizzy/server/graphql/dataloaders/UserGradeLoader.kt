package com.joe.quizzy.server.graphql.dataloaders

import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.persistence.api.GradeDAO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.coroutines.slf4j.MDCContext
import org.dataloader.MappedBatchLoader
import java.util.UUID
import java.util.concurrent.CompletionStage

/**
 * Batch load User ID -> List<Grade>
 */
class UserGradeLoader(private val gradeDAO: GradeDAO) :
    MappedBatchLoader<UUID, List<Grade>> {
    override fun load(userIds: Set<UUID>): CompletionStage<Map<UUID, List<Grade>>> {
        return CoroutineScope(Dispatchers.IO + MDCContext())
            .future {
                gradeDAO.forUsers(userIds.toList())
            }
    }
}
