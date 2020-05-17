package com.joe.quizzy.server.graphql.models

import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.server.auth.UserPrincipal
import com.trib3.graphql.resources.GraphQLResourceContext
import graphql.schema.DataFetchingEnvironment
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * like a [Question] but can fetch current [User]'s [Response]s
 */
data class ApiQuestion(
    val id: UUID?,
    val authorId: UUID,
    val body: String,
    val answer: String,
    val ruleReferences: String,
    val activeAt: OffsetDateTime,
    val closedAt: OffsetDateTime
) {
    constructor(
        question: Question
    ) : this(
        question.id,
        question.authorId,
        question.body,
        question.answer,
        question.ruleReferences,
        question.activeAt,
        question.closedAt
    )

    fun response(context: GraphQLResourceContext, dfe: DataFetchingEnvironment): CompletableFuture<ApiResponse?> {
        val principal = context.principal
        if (principal is UserPrincipal && id != null) {
            return dfe.getDataLoader<UUID, Response>("questionresponses")
                .load(id)
                .thenApply { it?.let(::ApiResponse) }
        }
        return CompletableFuture.completedFuture(null)
    }

    fun author(dfe: DataFetchingEnvironment): CompletableFuture<ApiUser?> {
        return dfe.getDataLoader<UUID, User>("batchusers").load(authorId).thenApply { it?.let(::ApiUser) }
    }
}
