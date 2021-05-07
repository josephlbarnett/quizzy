package com.joe.quizzy.graphql.dataloaders

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.persistence.api.GradeDAO
import com.trib3.testing.LeakyMock
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.dataloader.BatchLoaderEnvironment
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.util.UUID

class UserGradeLoaderTest {
    @Test
    fun testResponseGradeLoader() = runBlocking {
        val gradeDAO = LeakyMock.mock<GradeDAO>()
        val mockEnv = LeakyMock.mock<BatchLoaderEnvironment>()
        val loader = UserGradeLoader(gradeDAO)
        val grades = mapOf(
            UUID.randomUUID() to listOf(Grade(UUID.randomUUID(), UUID.randomUUID(), true, 1)),
            UUID.randomUUID() to listOf(
                Grade(UUID.randomUUID(), UUID.randomUUID(), true, 2),
                Grade(UUID.randomUUID(), UUID.randomUUID(), true, 3)
            )
        )
        EasyMock.expect(gradeDAO.forUsers(EasyMock.anyObject<List<UUID>>() ?: listOf())).andReturn(grades)
        EasyMock.expect(mockEnv.getContext<Any?>()).andReturn(null)
        EasyMock.replay(gradeDAO, mockEnv)
        val gs = loader.load(grades.mapNotNull { it.key }.toSet(), mockEnv).await()
        assertThat(gs).isEqualTo(grades)
        EasyMock.verify(gradeDAO, mockEnv)
    }
}
