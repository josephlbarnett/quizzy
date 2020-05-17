package com.joe.quizzy.server.graphql.dataloaders

import com.joe.quizzy.api.models.Response
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.server.auth.UserPrincipal
import com.trib3.graphql.resources.GraphQLResourceContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.coroutines.slf4j.MDCContext
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.MappedBatchLoaderWithContext
import java.util.UUID
import java.util.concurrent.CompletionStage

/**
 * Batch load Question ID -> Response for context User
 */
class QuestionResponseLoader(private val responseDAO: ResponseDAO) :
    MappedBatchLoaderWithContext<UUID, Response> {
    override fun load(
        keys: Set<UUID>,
        environment: BatchLoaderEnvironment
    ): CompletionStage<Map<UUID, Response>> {
        return CoroutineScope(Dispatchers.IO + MDCContext())
            .future {
                val principal = environment.getContext<GraphQLResourceContext>().principal
                if (principal is UserPrincipal) {
                    responseDAO.byUserQuestions(principal.user.id!!, keys.toList())
                } else {
                    emptyMap()
                }
            }
    }
}
