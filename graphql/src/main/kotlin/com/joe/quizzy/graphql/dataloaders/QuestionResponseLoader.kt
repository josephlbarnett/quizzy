package com.joe.quizzy.graphql.dataloaders

import com.joe.quizzy.api.models.Response
import com.joe.quizzy.graphql.auth.UserPrincipal
import com.joe.quizzy.persistence.api.ResponseDAO
import com.trib3.graphql.resources.getInstance
import graphql.GraphQLContext
import org.dataloader.BatchLoaderEnvironment
import java.security.Principal
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
        val principal = environment.getContext<GraphQLContext>().getInstance<Principal>()
        return if (principal is UserPrincipal) {
            val id = principal.user.id
            require(id != null)
            responseDAO.byUserQuestions(id, keys.toList())
        } else {
            emptyMap()
        }
    }
}
