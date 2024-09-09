package com.joe.quizzy.graphql.dataloaders

import com.expediagroup.graphql.generator.extensions.get
import com.joe.quizzy.graphql.auth.UserPrincipal
import com.joe.quizzy.persistence.api.ResponseDAO
import com.trib3.graphql.execution.CoroutineMappedBatchLoader
import graphql.GraphQLContext
import org.dataloader.BatchLoaderEnvironment
import java.security.Principal
import java.util.UUID

/**
 * Batch load Question ID -> Response for context User
 */
class QuestionStatsLoader(
    private val responseDAO: ResponseDAO,
) : CoroutineMappedBatchLoader<UUID, Pair<Int, Int>>() {
    override val dataLoaderName = "questionstats"

    override suspend fun loadSuspend(
        keys: Set<UUID>,
        environment: BatchLoaderEnvironment,
    ): Map<UUID, Pair<Int, Int>> {
        val principal = environment.getContext<GraphQLContext>().get<Principal>()
        return if (principal is UserPrincipal) {
            responseDAO.statsForQuestions(principal.user.instanceId, keys.toList())
        } else {
            emptyMap()
        }
    }
}
