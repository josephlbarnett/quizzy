// package com.joe.quizzy.server.graphql
//
// import assertk.assertThat
// import com.joe.quizzy.persistence.api.QuestionDAO
// import kotlinx.coroutines.reactive.asFlow
// import kotlinx.coroutines.runBlocking
// import org.easymock.EasyMock
// import org.testng.annotations.Test
//
// class SubscriptionTest {
//    @Test
//    fun testSubscription() {
//        val mockData = listOf(
//            Thing(1, "billy"),
//            Thing(2, "jimmy"),
//            Thing(3, "sammy")
//        )
//        val mockDAO = EasyMock.createMock<QuestionDAO>(QuestionDAO::class.java)
//        EasyMock.expect(mockDAO.stream()).andReturn(mockData.stream())
//        EasyMock.replay(mockDAO)
//        val query = Subscription(mockDAO)
//        val result = query.subscribe()
//        val testData = mutableListOf<Thing>()
//        runBlocking {
//            result.asFlow().toCollection(testData)
//        }
//        assertThat(testData).isEqualTo(mockData)
//    }
// }
