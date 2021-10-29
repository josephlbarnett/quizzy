package com.joe.quizzy.graphql

import com.joe.quizzy.graphql.auth.UserPrincipal
import com.joe.quizzy.graphql.models.ApiQuestion
import com.joe.quizzy.graphql.models.ApiResponse
import com.joe.quizzy.graphql.models.ApiUser
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.graphql.resources.getInstance
import graphql.schema.DataFetchingEnvironment
import java.security.Principal
import javax.inject.Inject

/**
 * GraphQL entry point for queries.  Maps the DAO interfaces to the GraphQL models.
 */
class Query @Inject constructor(
    private val questionDAO: QuestionDAO,
    private val userDAO: UserDAO,
    private val responseDAO: ResponseDAO
) : com.expediagroup.graphql.server.operations.Query {

    fun user(dfe: DataFetchingEnvironment): ApiUser? {
        val principal = dfe.graphQlContext.getInstance<Principal>()
        if (principal is UserPrincipal) {
            return ApiUser(principal.user)
        }
        return null
    }

    fun users(
        dfe: DataFetchingEnvironment
    ): List<ApiUser> {
        val principal = dfe.graphQlContext.getInstance<Principal>()
        if (principal is UserPrincipal) {
            return userDAO.getByInstance(principal.user.instanceId).map { ApiUser(it) }
        }
        return emptyList()
    }

    fun activeQuestions(dfe: DataFetchingEnvironment): List<ApiQuestion> {
        val principal = dfe.graphQlContext.getInstance<Principal>()
        if (principal is UserPrincipal) {
            return questionDAO.active(principal.user).map {
                ApiQuestion(
                    id = it.id,
                    authorId = it.authorId,
                    body = it.body,
                    answer = "",
                    ruleReferences = "",
                    activeAt = it.activeAt,
                    closedAt = it.closedAt
                )
            }
        }
        return emptyList()
    }

    fun closedQuestions(dfe: DataFetchingEnvironment): List<ApiQuestion> {
        val principal = dfe.graphQlContext.getInstance<Principal>()
        if (principal is UserPrincipal) {
            return questionDAO.closed(principal.user).map { ApiQuestion(it) }
        }
        return emptyList()
    }

    fun futureQuestions(dfe: DataFetchingEnvironment): List<ApiQuestion> {
        val principal = dfe.graphQlContext.getInstance<Principal>()
        if (principal is UserPrincipal) {
            if (principal.user.admin) {
                return questionDAO.future(principal.user).map { ApiQuestion(it) }
            }
        }
        return emptyList()
    }

    fun responses(dfe: DataFetchingEnvironment, includeGraded: Boolean): List<ApiResponse> {
        val principal = dfe.graphQlContext.getInstance<Principal>()
        if (principal is UserPrincipal) {
            if (principal.user.admin) {
                return responseDAO.forInstance(principal.user.instanceId, includeGraded).map { ApiResponse(it) }
            }
        }
        return emptyList()
    }
}
