package com.joe.quizzy.server.graphql

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Thing
import com.joe.quizzy.persistence.api.ThingDAO
import org.easymock.EasyMock
import org.testng.annotations.Test

class QueryTest {
    @Test
    fun testQuery() {
        val mockDAO = EasyMock.createMock<ThingDAO>(ThingDAO::class.java)
        EasyMock.expect(mockDAO.get(1)).andReturn(Thing(1, "billy"))
        EasyMock.replay(mockDAO)
        val query = Query(mockDAO)
        val result = query.thing(1)
        assertThat(result?.id).isEqualTo(1)
        assertThat(result?.name).isEqualTo("billy")
    }
}
