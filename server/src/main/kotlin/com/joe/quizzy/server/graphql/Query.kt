package com.joe.quizzy.server.graphql

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.joe.quizzy.server.auth.UserPrincipal
import com.trib3.graphql.resources.GraphQLResourceContext
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * like a [Question] but can fetch current user's responses
 */
data class ApiQuestion(
    val id: UUID?,
    val authorId: UUID,
    val body: String,
    val answer: String,
    val ruleReferences: String,
    val activeAt: OffsetDateTime,
    val closedAt: OffsetDateTime,

    @GraphQLIgnore val responseDAO: ResponseDAO
) {
    constructor(question: Question, responseDAO: ResponseDAO) : this(
        question.id,
        question.authorId,
        question.body,
        question.answer,
        question.ruleReferences,
        question.activeAt,
        question.closedAt,
        responseDAO
    )

    fun response(context: GraphQLResourceContext): Response? {
        val principal = context.principal
        if (principal is UserPrincipal && id != null) {
            return responseDAO.byUserQuestion(principal.user, id)
        }
        return null
    }
}

/**
 * GraphQL entry point for queries.  Maps the DAO interfaces to the GraphQL models.
 */
class Query @Inject constructor(
    private val questionDAO: QuestionDAO,
    private val userDAO: UserDAO,
    private val responseDAO: ResponseDAO
) : GraphQLQueryResolver {

    fun user(context: GraphQLResourceContext): User? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return principal.user
        }
        return null
    }

    fun users(context: GraphQLResourceContext): List<User> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return userDAO.getByInstance(principal.user.instanceId)
        }
        return emptyList()
    }

    fun response(context: GraphQLResourceContext, question: UUID): Response? {
        log.info("$context")
        return responseDAO.get(question)
    }

    fun activeQuestions(context: GraphQLResourceContext): List<ApiQuestion> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return questionDAO.active(principal.user).map {
                ApiQuestion(
                    id = it.id,
                    authorId = it.authorId,
                    body = it.body,
                    answer = "",
                    ruleReferences = "",
                    activeAt = it.activeAt,
                    closedAt = it.closedAt,
                    responseDAO = responseDAO
                )
            }
        }
        return emptyList()
    }

    fun closedQuestions(context: GraphQLResourceContext): List<ApiQuestion> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return questionDAO.closed(principal.user).map {
                ApiQuestion(it, responseDAO)
            }
        }
        return emptyList()
    }
}
