package com.joe.quizzy.server.graphql

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Thing
import com.joe.quizzy.persistence.api.ThingDAO
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.easymock.EasyMock
import org.testng.annotations.Test

class SubscriptionTest {
    @Test
    fun testSubscription() {
        val mockData = listOf(
            Thing(1, "billy"),
            Thing(2, "jimmy"),
            Thing(3, "sammy")
        )
        val mockDAO = EasyMock.createMock<ThingDAO>(ThingDAO::class.java)
        EasyMock.expect(mockDAO.stream()).andReturn(mockData.stream())
        EasyMock.replay(mockDAO)
        val query = Subscription(mockDAO)
        val result = query.subscribe()
        val testData = mutableListOf<Thing>()
        runBlocking {
            result.asFlow().toCollection(testData)
        }
        assertThat(testData).isEqualTo(mockData)
    }
}
