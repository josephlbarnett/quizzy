package com.joe.quizzy.graphql.dataloaders

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.persistence.api.GradeDAO
import com.trib3.testing.LeakyMock
import graphql.GraphQLContext
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.dataloader.BatchLoaderEnvironment
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.util.UUID

class ResponseGradeLoaderTest {
    @Test
    fun testResponseGradeLoader() = runBlocking {
        val gradeDAO = LeakyMock.mock<GradeDAO>()
        val mockEnv = LeakyMock.mock<BatchLoaderEnvironment>()
        val loader = ResponseGradeLoader(gradeDAO)
        val grades = mapOf(
            UUID.randomUUID() to Grade(UUID.randomUUID(), UUID.randomUUID(), true, 1),
            UUID.randomUUID() to Grade(UUID.randomUUID(), UUID.randomUUID(), true, 2)
        )
        EasyMock.expect(gradeDAO.forResponses(EasyMock.anyObject<List<UUID>>() ?: listOf())).andReturn(grades)
        EasyMock.expect(mockEnv.getContext<Any?>()).andReturn(GraphQLContext.newContext().build())
        EasyMock.replay(gradeDAO, mockEnv)
        val gs = loader.load(grades.mapNotNull { it.key }.toSet(), mockEnv).await()
        assertThat(gs).isEqualTo(grades)
        EasyMock.verify(gradeDAO, mockEnv)
    }
}
