package com.joe.quizzy.graphql.models

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.extensions.get
import com.joe.quizzy.api.models.AnswerChoice
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.QuestionType
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.graphql.auth.UserPrincipal
import com.trib3.graphql.execution.GraphQLAuth
import graphql.schema.DataFetchingEnvironment
import java.security.Principal
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

private const val PERCENT = 100.0

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
    val closedAt: OffsetDateTime,
    val type: QuestionType,
    val answerChoices: List<AnswerChoice>?,
    val imageUrl: String? = null,
    @GraphQLIgnore
    private val defaultScore: Int,
) {
    constructor(
        question: Question,
        defaultScore: Int,
    ) : this(
        question.id,
        question.authorId,
        question.body,
        question.answer,
        question.ruleReferences,
        question.activeAt,
        question.closedAt,
        question.type,
        question.answerChoices,
        question.imageUrl,
        defaultScore,
    )

    fun response(dfe: DataFetchingEnvironment): CompletableFuture<ApiResponse?> {
        val principal = dfe.graphQlContext.get<Principal>()
        if (principal is UserPrincipal && id != null) {
            return dfe
                .getDataLoader<UUID, Response>("questionresponses")
                ?.load(id)
                ?.thenApply { it?.let { r -> ApiResponse(r, defaultScore) } } ?: CompletableFuture.completedFuture(null)
        }
        return CompletableFuture.completedFuture(null)
    }

    fun author(dfe: DataFetchingEnvironment): CompletableFuture<ApiUser?> =
        dfe
            .getDataLoader<UUID, User>("batchusers")
            ?.load(authorId)
            ?.thenApply { it?.let { u -> ApiUser(u, defaultScore) } } ?: CompletableFuture.completedFuture(null)

    @GraphQLAuth(["ADMIN"])
    fun percentCorrect(dfe: DataFetchingEnvironment): CompletableFuture<Double?> =
        dfe
            .getDataLoader<UUID, Pair<Int, Int>>("questionstats")
            ?.load(id)
            ?.thenApply { it?.let { p -> PERCENT * p.second / p.first } } ?: CompletableFuture.completedFuture(null)
}
