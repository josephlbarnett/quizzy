package com.joe.quizzy.graphql.dataloaders

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.persistence.api.QuestionDAO
import com.trib3.testing.LeakyMock
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.dataloader.BatchLoaderEnvironment
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.time.OffsetDateTime
import java.util.UUID

class BatchQuestionLoaderTest {
    @Test
    fun testQuestionLoader() = runBlocking {
        val questionDAO = LeakyMock.mock<QuestionDAO>()
        val mockEnv = LeakyMock.mock<BatchLoaderEnvironment>()
        val loader = BatchQuestionLoader(questionDAO, mapOf<Any, Any>())
        val now = OffsetDateTime.now()
        val questions = listOf(
            Question(UUID.randomUUID(), UUID.randomUUID(), "q1", "a1", "r1", now, now),
            Question(UUID.randomUUID(), UUID.randomUUID(), "q2", "a2", "r2", now, now),
            Question(UUID.randomUUID(), UUID.randomUUID(), "q3", "a3", "r3", now, now)
        )
        EasyMock.expect(questionDAO.get(EasyMock.anyObject<List<UUID>>() ?: listOf())).andReturn(questions)
        EasyMock.replay(questionDAO, mockEnv)
        val qs = loader.load(questions.mapNotNull { it.id }.toSet(), mockEnv).await()
        assertThat(qs).isEqualTo(questions.associateBy { it.id })
        EasyMock.verify(questionDAO, mockEnv)
    }
}
