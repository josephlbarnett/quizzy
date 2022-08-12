package com.joe.quizzy.graphql

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.graphql.auth.UserPrincipal
import com.joe.quizzy.graphql.models.ApiQuestion
import com.joe.quizzy.graphql.models.ApiResponse
import com.joe.quizzy.graphql.models.ApiUser
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.graphql.resources.getGraphQLContextMap
import com.trib3.testing.LeakyMock
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.time.OffsetDateTime
import java.util.UUID

fun getDFE(principal: UserPrincipal?, scope: CoroutineScope): DataFetchingEnvironment {
    val dfe = LeakyMock.mock<DataFetchingEnvironment>()
    EasyMock.expect(dfe.graphQlContext).andReturn(
        GraphQLContext.of(
            getGraphQLContextMap(scope, principal)
        )
    ).anyTimes()
    EasyMock.replay(dfe)
    return dfe
}

class QueryTest {
    val scope = CoroutineScope(Dispatchers.Default)
    val query: Query
    val uUUID = UUID.randomUUID()
    val iUUID = UUID.randomUUID()
    val qUUID = UUID.randomUUID()
    val rUUID = UUID.randomUUID()
    val r2UUID = UUID.randomUUID()
    val user = User(uUUID, iUUID, "billy", "billy@gmail.com", "", false, "UTC")
    val question = Question(
        qUUID,
        uUUID,
        "question",
        "answer",
        "refs",
        OffsetDateTime.now(),
        OffsetDateTime.now()
    )
    val response = Response(rUUID, uUUID, qUUID, "response", "responseRefs")
    val gradedResponse = Response(r2UUID, uUUID, qUUID, "response2", "responseRefs2")

    init {
        val qDAO = LeakyMock.mock<QuestionDAO>()
        val uDAO = LeakyMock.mock<UserDAO>()
        val sDAO = LeakyMock.mock<SessionDAO>()
        val rDAO = LeakyMock.mock<ResponseDAO>()
        val iDAO = LeakyMock.mock<InstanceDAO>()
        EasyMock.expect(qDAO.get(EasyMock.anyObject() ?: UUID.randomUUID())).andReturn(question)
        EasyMock.expect(qDAO.active(user)).andReturn(listOf(question))
        EasyMock.expect(qDAO.closed(user)).andReturn(listOf(question))
        EasyMock.expect(qDAO.future(EasyMock.anyObject() ?: user)).andReturn(listOf(question))
        EasyMock.expect(uDAO.get(uUUID)).andReturn(user)
        EasyMock.expect(uDAO.getByInstance(iUUID)).andReturn(listOf(user))
        EasyMock.expect(rDAO.get(EasyMock.anyObject() ?: UUID.randomUUID())).andReturn(response)
        EasyMock.expect(rDAO.forInstance(iUUID, true)).andReturn(listOf(response, gradedResponse))
        EasyMock.expect(rDAO.forInstance(iUUID, false)).andReturn(listOf(response))
        EasyMock.expect(iDAO.get(iUUID)).andReturn(Instance(iUUID, "instance", "ACTIVE", defaultScore = 15)).anyTimes()
        EasyMock.replay(
            qDAO,
            uDAO,
            sDAO,
            rDAO,
            iDAO
        )
        query = Query(qDAO, uDAO, rDAO, iDAO)
    }

    @Test
    fun testCurrentUserQuery() {
        val apiUser = query.user(getDFE(UserPrincipal(user, null), scope))
        assertThat(apiUser?.id).isEqualTo(uUUID)
        assertThat(apiUser?.name).isEqualTo("billy")

        val noUser = query.user(getDFE(null, scope))
        assertThat(noUser).isNull()
    }

    @Test
    fun testUsersQuery() {
        val apiUsers = query.users(getDFE(UserPrincipal(user, null), scope))
        assertThat(apiUsers).contains(ApiUser(user, 15))

        val noUsers = query.users(getDFE(null, scope))
        assertThat(noUsers).isEmpty()
    }

    @Test
    fun testActiveQuestions() {
        val apiQuestions = query.activeQuestions(getDFE(UserPrincipal(user, null), scope))
        assertThat(apiQuestions).contains(ApiQuestion(question.copy(answer = "", ruleReferences = ""), 15))

        val noQuestions = query.activeQuestions(getDFE(null, scope))
        assertThat(noQuestions).isEmpty()
    }

    @Test
    fun testClosedQuestions() {
        val apiQuestions = query.closedQuestions(getDFE(UserPrincipal(user, null), scope))
        assertThat(apiQuestions).contains(ApiQuestion(question, 15))

        val noQuestions = query.closedQuestions(getDFE(null, scope))
        assertThat(noQuestions).isEmpty()
    }

    @Test
    fun testFutureQuestions() {
        val apiQuestions = query.futureQuestions(
            getDFE(UserPrincipal(user.copy(admin = true), null), scope)
        )
        assertThat(apiQuestions.first()).isEqualTo(ApiQuestion(question, 15))

        val noPermissionsQuestions = query.futureQuestions(getDFE(UserPrincipal(user, null), scope))
        assertThat(noPermissionsQuestions).isEmpty()

        val noQuestions = query.futureQuestions(getDFE(null, scope))
        assertThat(noQuestions).isEmpty()
    }

    @Test
    fun testResponses() {
        val withGraded = query.responses(
            getDFE(UserPrincipal(user.copy(admin = true), null), scope),
            true
        )
        assertThat(withGraded).contains(ApiResponse(response, 15))
        assertThat(withGraded).contains(ApiResponse(gradedResponse, 15))

        val withoutGraded = query.responses(
            getDFE(UserPrincipal(user.copy(admin = true), null), scope),
            false
        )
        assertThat(withoutGraded).contains(ApiResponse(response, 15))
        assertThat(withoutGraded).doesNotContain(ApiResponse(gradedResponse, 15))

        val noPermissionsResponses = query.responses(getDFE(UserPrincipal(user, null), scope), true)
        assertThat(noPermissionsResponses).isEmpty()

        val noResponses = query.responses(getDFE(null, scope), true)
        assertThat(noResponses).isEmpty()
    }
}
