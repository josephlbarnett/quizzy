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
import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject

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

    @GraphQLIgnore private val responseDAO: ResponseDAO
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

    suspend fun response(context: GraphQLResourceContext): Response? {
        val principal = context.principal
        if (principal is UserPrincipal && id != null) {
            return responseDAO.byUserQuestion(principal.user, id)
        }
        return null
    }
}

data class ApiResponse(
    val id: UUID?,
    val userId: UUID,
    val questionId: UUID,
    val response: String,
    val ruleReferences: String,
    val correct: Boolean?,
    val bonus: Int?,

    @GraphQLIgnore private val userDAO: UserDAO,
    @GraphQLIgnore private val questionDAO: QuestionDAO
) {
    constructor(response: Response, userDAO: UserDAO, questionDAO: QuestionDAO) :
        this(
            response.id,
            response.userId,
            response.questionId,
            response.response,
            response.ruleReferences,
            response.correct,
            response.bonus,
            userDAO,
            questionDAO
        )

    suspend fun user(context: GraphQLResourceContext): User? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return userDAO.get(userId)
        }
        return null
    }

    suspend fun question(context: GraphQLResourceContext): Question? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return questionDAO.get(questionId)
        }
        return null
    }
}

data class ApiUser(
    val id: UUID?,
    val instanceId: UUID,
    val name: String,
    val email: String,
    val authCrypt: String?,
    val admin: Boolean,
    val timeZoneId: String,

    @GraphQLIgnore private val responseDAO: ResponseDAO
) {
    constructor(
        user: User,
        responseDAO: ResponseDAO
    ) : this(
        user.id,
        user.instanceId,
        user.name,
        user.email,
        user.authCrypt,
        user.admin,
        user.timeZoneId,
        responseDAO
    )

    suspend fun score(context: GraphQLResourceContext): Int {
        val principal = context.principal
        if (principal is UserPrincipal) {
            if (principal.user.admin && id != null) {
                return responseDAO.forUser(id).fold(0) { score, response ->
                    score + if (response.correct == true) {
                        (response.bonus ?: 0) + 15
                    } else {
                        0
                    }
                }
            }
        }
        return 0
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

    fun users(context: GraphQLResourceContext, dfe: DataFetchingEnvironment): List<ApiUser> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            log.trace("$dfe")
            return userDAO.getByInstance(principal.user.instanceId).map { ApiUser(it, responseDAO) }
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

    fun futureQuestions(context: GraphQLResourceContext): List<Question> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            if (principal.user.admin) {
                return questionDAO.future(principal.user)
            }
        }
        return emptyList()
    }

    fun responses(context: GraphQLResourceContext, includeGraded: Boolean): List<ApiResponse> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            if (principal.user.admin) {
                return responseDAO.forInstance(principal.user, includeGraded)
                    .map { ApiResponse(it, userDAO, questionDAO) }
            }
        }
        return emptyList()
    }
}
