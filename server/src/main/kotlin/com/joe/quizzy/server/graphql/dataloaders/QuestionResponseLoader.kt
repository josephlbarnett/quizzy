package com.joe.quizzy.server.graphql.dataloaders

import com.joe.quizzy.api.models.Response
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.server.auth.UserPrincipal
import com.trib3.graphql.resources.GraphQLResourceContext
import org.dataloader.BatchLoaderEnvironment
import java.util.UUID

/**
 * Batch load Question ID -> Response for context User
 */
class QuestionResponseLoader(private val responseDAO: ResponseDAO) :
    CoroutineMappedBatchLoader<UUID, Response>() {
    override suspend fun loadSuspend(
        keys: Set<UUID>,
        environment: BatchLoaderEnvironment
    ): Map<UUID, Response> {
        val principal = environment.getContext<GraphQLResourceContext>().principal
        return if (principal is UserPrincipal) {
            val id = principal.user.id
            require(id != null)
            responseDAO.byUserQuestions(id, keys.toList())
        } else {
            emptyMap()
        }
    }
}
