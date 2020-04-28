package com.joe.quizzy.server.graphql

import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.testing.LeakyMock
import java.time.OffsetDateTime
import java.util.UUID
import org.easymock.EasyMock
import org.testng.annotations.Test

class QueryTest {
    @Test
    fun testQuery() {
        val iUUID = UUID.randomUUID()
        val qUUID = UUID.randomUUID()
        val uUUID = UUID.randomUUID()
        val rUUID = UUID.randomUUID()
        val qDAO = LeakyMock.mock<QuestionDAO>()
        val uDAO = LeakyMock.mock<UserDAO>()
        val sDAO = LeakyMock.mock<SessionDAO>()
        val rDAO = LeakyMock.mock<ResponseDAO>()
        EasyMock.expect(qDAO.get(EasyMock.anyObject() ?: UUID.randomUUID())).andReturn(
            Question(
                qUUID,
                uUUID,
                "question",
                "answer",
                "refs",
                OffsetDateTime.now(),
                OffsetDateTime.now()
            )
        )
        EasyMock.expect(uDAO.get(EasyMock.anyObject() ?: UUID.randomUUID())).andReturn(
            User(uUUID, iUUID, "billy", "billy@gmail.com", "", false, "UTC")
        )
        EasyMock.expect(rDAO.get(EasyMock.anyObject() ?: UUID.randomUUID())).andReturn(
            Response(rUUID, uUUID, qUUID, "response", "responseRefs")
        )
        EasyMock.replay(qDAO, uDAO, sDAO, rDAO)
//        val query = Query(qDAO, uDAO, rDAO)
//        val question = query.question(qUUID)
//        assertThat(question?.id).isEqualTo(qUUID)
//        assertThat(question?.body).isEqualTo("question")
//
//        val user = query.user(GraphQLResourceContext(null), uUUID)
//        assertThat(user?.id).isEqualTo(uUUID)
//        assertThat(user?.name).isEqualTo("billy")
//
//        val response = query.response(rUUID)
//        assertThat(response?.id).isEqualTo(rUUID)
//        assertThat(response?.response).isEqualTo("response")
    }
}
