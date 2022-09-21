package com.joe.quizzy.graphql.groupme

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.messageContains
import com.fasterxml.jackson.databind.ObjectMapper
import com.joe.quizzy.persistence.api.GroupMeInfo
import com.joe.quizzy.persistence.api.GroupMeInfoDAO
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.testng.annotations.Test
import java.io.ByteArrayInputStream
import java.util.UUID

class GroupMeServiceTest {

    @Test
    fun testService() = runBlocking {
        val goodUUID = UUID.randomUUID()
        val dao = mockk<GroupMeInfoDAO>()
        every {
            dao.get(any())
        } returns null
        every {
            dao.get(goodUUID)
        } returns GroupMeInfo(goodUUID, "groupId", "apiKey")
        val jsonResponse = ObjectMapper().writeValueAsString(UploadResponse(UploadUrls("https://blah.com/image", "")))
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    if (it.url.toString() == "https://image.groupme.com/pictures") {
                        respond(jsonResponse, headers = headersOf("Content-Type", "application/json"))
                    } else {
                        respond("pong")
                    }
                }
            }
            install(ContentNegotiation) {
                jackson()
            }
        }
        assertThat {
            GroupMeService(dao, client, UUID.randomUUID())
        }.isFailure().messageContains("No groupme configured for instance")
        val service = GroupMeService(dao, client, goodUUID)
        service.postMessage("testMessage")
        val requestHistory = (client.engine as MockEngine).requestHistory
        assertThat(requestHistory).hasSize(1)
        assertThat(requestHistory[0].url.toString()).isEqualTo("https://api.groupme.com/v3/groups/groupId/messages")
        assertThat(String(requestHistory[0].body.toByteArray())).contains("testMessage")
        assertThat(service.uploadImage(ByteArrayInputStream("lksdjf".toByteArray()))).isEqualTo(
            "https://blah.com/image"
        )
    }
}
