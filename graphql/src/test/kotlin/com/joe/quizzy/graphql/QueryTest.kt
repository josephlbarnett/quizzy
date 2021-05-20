package com.joe.quizzy.graphql

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.graphql.auth.UserPrincipal
import com.joe.quizzy.graphql.models.ApiQuestion
import com.joe.quizzy.graphql.models.ApiResponse
import com.joe.quizzy.graphql.models.ApiUser
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.graphql.resources.GraphQLResourceContext
import com.trib3.testing.LeakyMock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.time.OffsetDateTime
import java.util.UUID

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
        EasyMock.expect(qDAO.get(EasyMock.anyObject() ?: UUID.randomUUID())).andReturn(question)
        EasyMock.expect(qDAO.active(user)).andReturn(listOf(question))
        EasyMock.expect(qDAO.closed(user)).andReturn(listOf(question))
        EasyMock.expect(qDAO.future(EasyMock.anyObject() ?: user)).andReturn(listOf(question))
        EasyMock.expect(uDAO.get(uUUID)).andReturn(user)
        EasyMock.expect(uDAO.getByInstance(iUUID)).andReturn(listOf(user))
        EasyMock.expect(rDAO.get(EasyMock.anyObject() ?: UUID.randomUUID())).andReturn(response)
        EasyMock.expect(rDAO.forInstance(iUUID, true)).andReturn(listOf(response, gradedResponse))
        EasyMock.expect(rDAO.forInstance(iUUID, false)).andReturn(listOf(response))
        EasyMock.replay(
            qDAO,
            uDAO,
            sDAO,
            rDAO
        )
        query = Query(qDAO, uDAO, rDAO)
    }

    @Test
    fun testCurrentUserQuery() {
        val apiUser = query.user(GraphQLResourceContext(UserPrincipal(user, null), scope))
        assertThat(apiUser?.id).isEqualTo(uUUID)
        assertThat(apiUser?.name).isEqualTo("billy")

        val noUser = query.user(GraphQLResourceContext(null, scope))
        assertThat(noUser).isNull()
    }

    @Test
    fun testUsersQuery() {
        val apiUsers = query.users(GraphQLResourceContext(UserPrincipal(user, null), scope))
        assertThat(apiUsers).contains(ApiUser(user))

        val noUsers = query.users(GraphQLResourceContext(null, scope))
        assertThat(noUsers).isEmpty()
    }

    @Test
    fun testActiveQuestions() {
        val apiQuestions = query.activeQuestions(GraphQLResourceContext(UserPrincipal(user, null), scope))
        assertThat(apiQuestions).contains(ApiQuestion(question.copy(answer = "", ruleReferences = "")))

        val noQuestions = query.activeQuestions(GraphQLResourceContext(null, scope))
        assertThat(noQuestions).isEmpty()
    }

    @Test
    fun testClosedQuestions() {
        val apiQuestions = query.closedQuestions(GraphQLResourceContext(UserPrincipal(user, null), scope))
        assertThat(apiQuestions).contains(ApiQuestion(question))

        val noQuestions = query.closedQuestions(GraphQLResourceContext(null, scope))
        assertThat(noQuestions).isEmpty()
    }

    @Test
    fun testFutureQuestions() {
        val apiQuestions = query.futureQuestions(
            GraphQLResourceContext(UserPrincipal(user.copy(admin = true), null), scope)
        )
        assertThat(apiQuestions.first()).isEqualTo(ApiQuestion(question))

        val noPermissionsQuestions = query.futureQuestions(GraphQLResourceContext(UserPrincipal(user, null), scope))
        assertThat(noPermissionsQuestions).isEmpty()

        val noQuestions = query.futureQuestions(GraphQLResourceContext(null, scope))
        assertThat(noQuestions).isEmpty()
    }

    @Test
    fun testResponses() {
        val withGraded = query.responses(
            GraphQLResourceContext(UserPrincipal(user.copy(admin = true), null), scope),
            true
        )
        assertThat(withGraded).contains(ApiResponse(response))
        assertThat(withGraded).contains(ApiResponse(gradedResponse))

        val withoutGraded = query.responses(
            GraphQLResourceContext(UserPrincipal(user.copy(admin = true), null), scope),
            false
        )
        assertThat(withoutGraded).contains(ApiResponse(response))
        assertThat(withoutGraded).doesNotContain(ApiResponse(gradedResponse))

        val noPermissionsResponses = query.responses(GraphQLResourceContext(UserPrincipal(user, null), scope), true)
        assertThat(noPermissionsResponses).isEmpty()

        val noResponses = query.responses(GraphQLResourceContext(null, scope), true)
        assertThat(noResponses).isEmpty()
    }
}
