package com.joe.quizzy.graphql.models

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.graphql.auth.UserPrincipal
import com.trib3.graphql.resources.getInstance
import graphql.schema.DataFetchingEnvironment
import java.security.Principal
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Like a [Response] but can dynamically fetch the associated [User]/[Question]/[Grade]
 */
data class ApiResponse(
    val id: UUID?,
    val userId: UUID,
    val questionId: UUID,
    val response: String,
    val ruleReferences: String,
    @GraphQLIgnore
    private val defaultScore: Int
) {
    constructor(response: Response, defaultScore: Int) :
        this(
            response.id,
            response.userId,
            response.questionId,
            response.response,
            response.ruleReferences,
            defaultScore
        )

    fun user(dfe: DataFetchingEnvironment): CompletableFuture<ApiUser?> {
        val principal = dfe.graphQlContext.getInstance<Principal>()
        if (principal is UserPrincipal) {
            return dfe.getDataLoader<UUID, User>("batchusers").load(userId).thenApply {
                it?.let { u -> ApiUser(u, defaultScore) }
            }
        }
        return CompletableFuture.completedFuture(null)
    }

    fun question(dfe: DataFetchingEnvironment): CompletableFuture<ApiQuestion?> {
        val principal = dfe.graphQlContext.getInstance<Principal>()
        if (principal is UserPrincipal) {
            val gradeFuture = dfe.getDataLoader<UUID, Grade>("responsegrades").load(id)
            return dfe.getDataLoader<UUID, Question>("batchquestions").load(questionId)
                .thenCombine(gradeFuture) { question, grade ->
                    question?.let {
                        if (grade == null) {
                            ApiQuestion(it, defaultScore).copy(answer = "", ruleReferences = "")
                        } else {
                            ApiQuestion(it, defaultScore)
                        }
                    }
                }
        }
        return CompletableFuture.completedFuture(null)
    }

    fun grade(dfe: DataFetchingEnvironment): CompletableFuture<ApiGrade?> {
        val principal = dfe.graphQlContext.getInstance<Principal>()
        if (principal is UserPrincipal && id != null) {
            return dfe.getDataLoader<UUID, Grade>("responsegrades").load(id)
                .thenApply { it?.let { g -> ApiGrade(g, defaultScore) } }
        }
        return CompletableFuture.completedFuture(null)
    }
}
