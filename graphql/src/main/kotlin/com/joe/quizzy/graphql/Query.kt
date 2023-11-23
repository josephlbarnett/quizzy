package com.joe.quizzy.graphql

import com.expediagroup.graphql.generator.extensions.get
import com.joe.quizzy.graphql.auth.UserPrincipal
import com.joe.quizzy.graphql.models.ApiQuestion
import com.joe.quizzy.graphql.models.ApiResponse
import com.joe.quizzy.graphql.models.ApiUser
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.UserDAO
import graphql.schema.DataFetchingEnvironment
import jakarta.inject.Inject
import java.security.Principal
import java.time.OffsetDateTime

/**
 * GraphQL entry point for queries.  Maps the DAO interfaces to the GraphQL models.
 */
class Query @Inject constructor(
    private val questionDAO: QuestionDAO,
    private val userDAO: UserDAO,
    private val responseDAO: ResponseDAO,
    private val instanceDAO: InstanceDAO,
) : com.expediagroup.graphql.server.operations.Query {

    private fun getDefaultScore(userPrincipal: UserPrincipal): Int {
        val instance = instanceDAO.get(userPrincipal.user.instanceId)
        requireNotNull(instance)
        return instance.defaultScore
    }

    fun user(dfe: DataFetchingEnvironment): ApiUser? {
        val principal = dfe.graphQlContext.get<Principal>()
        if (principal is UserPrincipal) {
            return ApiUser(principal.user, getDefaultScore(principal))
        }
        return null
    }

    fun users(
        dfe: DataFetchingEnvironment,
    ): List<ApiUser> {
        val principal = dfe.graphQlContext.get<Principal>()
        if (principal is UserPrincipal) {
            val defaultScore = getDefaultScore(principal)
            return userDAO.getByInstance(principal.user.instanceId).map { ApiUser(it, defaultScore) }
        }
        return emptyList()
    }

    fun activeQuestions(dfe: DataFetchingEnvironment): List<ApiQuestion> {
        val principal = dfe.graphQlContext.get<Principal>()
        if (principal is UserPrincipal) {
            val defaultScore = getDefaultScore(principal)
            return questionDAO.active(principal.user).map {
                ApiQuestion(
                    it.copy(
                        answer = "",
                        ruleReferences = "",
                    ),
                    defaultScore = defaultScore,
                )
            }
        }
        return emptyList()
    }

    fun closedQuestions(
        dfe: DataFetchingEnvironment,
        startTime: OffsetDateTime? = null,
        endTime: OffsetDateTime? = null,
    ): List<ApiQuestion> {
        val principal = dfe.graphQlContext.get<Principal>()
        if (principal is UserPrincipal) {
            val defaultScore = getDefaultScore(principal)
            return questionDAO.closed(principal.user, startTime, endTime).map { ApiQuestion(it, defaultScore) }
        }
        return emptyList()
    }

    fun futureQuestions(dfe: DataFetchingEnvironment): List<ApiQuestion> {
        val principal = dfe.graphQlContext.get<Principal>()
        if (principal is UserPrincipal) {
            if (principal.user.admin) {
                val defaultScore = getDefaultScore(principal)
                return questionDAO.future(principal.user).map { ApiQuestion(it, defaultScore) }
            }
        }
        return emptyList()
    }

    fun responses(
        dfe: DataFetchingEnvironment,
        includeGraded: Boolean,
        startTime: OffsetDateTime? = null,
        endTime: OffsetDateTime? = null,
    ): List<ApiResponse> {
        val principal = dfe.graphQlContext.get<Principal>()
        if (principal is UserPrincipal) {
            if (principal.user.admin) {
                val defaultScore = getDefaultScore(principal)
                return responseDAO.forInstance(principal.user.instanceId, includeGraded, startTime, endTime)
                    .map { ApiResponse(it, defaultScore) }
            }
        }
        return emptyList()
    }
}
