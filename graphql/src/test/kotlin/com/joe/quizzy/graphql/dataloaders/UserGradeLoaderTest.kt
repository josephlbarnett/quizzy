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
import java.time.OffsetDateTime
import java.util.UUID

class UserGradeLoaderTest {
    @Test
    fun testResponseGradeLoader() =
        runBlocking {
            val gradeDAO = LeakyMock.mock<GradeDAO>()
            val mockEnv = LeakyMock.mock<BatchLoaderEnvironment>()
            val loader = UserGradeLoader(gradeDAO)
            val grades =
                mapOf(
                    UUID.randomUUID() to listOf(Grade(UUID.randomUUID(), UUID.randomUUID(), true, 1)),
                    UUID.randomUUID() to
                        listOf(
                            Grade(UUID.randomUUID(), UUID.randomUUID(), true, 2),
                            Grade(UUID.randomUUID(), UUID.randomUUID(), true, 3),
                        ),
                )
            val moreGrades =
                mapOf(
                    UUID.randomUUID() to listOf(Grade(UUID.randomUUID(), UUID.randomUUID(), true, 1)),
                    UUID.randomUUID() to
                        listOf(
                            Grade(UUID.randomUUID(), UUID.randomUUID(), true, 2),
                            Grade(UUID.randomUUID(), UUID.randomUUID(), true, 3),
                        ),
                )
            EasyMock
                .expect(
                    gradeDAO.forUsers(
                        EasyMock.anyObject<List<UUID>>() ?: listOf(),
                        EasyMock.anyObject(),
                        EasyMock.anyObject(),
                    ),
                ).andReturn(grades)
                .once()
            EasyMock
                .expect(
                    gradeDAO.forUsers(
                        EasyMock.anyObject<List<UUID>>() ?: listOf(),
                        EasyMock.anyObject(),
                        EasyMock.anyObject(),
                    ),
                ).andReturn(moreGrades)
                .once()
            EasyMock.expect(mockEnv.getContext<GraphQLContext>()).andReturn(GraphQLContext.getDefault())
            EasyMock.replay(gradeDAO, mockEnv)
            val gs =
                loader
                    .load(
                        (
                            grades.mapNotNull { UserTimePeriod(it.key, null, null) } +
                                moreGrades.mapNotNull { UserTimePeriod(it.key, OffsetDateTime.MIN, OffsetDateTime.MAX) }
                        ).toSet(),
                        mockEnv,
                    ).await()
            assertThat(gs.mapKeys { it.key.userId }).isEqualTo(grades + moreGrades)
            EasyMock.verify(gradeDAO, mockEnv)
        }
}
