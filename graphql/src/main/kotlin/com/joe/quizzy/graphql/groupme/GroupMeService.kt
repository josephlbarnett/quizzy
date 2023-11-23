package com.joe.quizzy.graphql.groupme

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.google.inject.assistedinject.Assisted
import com.joe.quizzy.persistence.api.GroupMeInfoDAO
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import jakarta.inject.Inject
import mu.KotlinLogging
import java.io.InputStream
import java.util.UUID

private val log = KotlinLogging.logger { }

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class UploadUrls(val url: String?, val pictureUrl: String?)
data class UploadResponse(val payload: UploadUrls?)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class Message(val sourceGuid: UUID, val text: String)
data class MessageRequest(val message: Message)

interface GroupMeServiceFactory {
    fun create(instanceId: UUID): GroupMeService?
}

open class GroupMeService @Inject constructor(
    groupMeInfoDAO: GroupMeInfoDAO,
    private val client: HttpClient,
    @Assisted instanceId: UUID,
) {
    private val groupId: String
    private val apiKey: String

    init {
        val groupMeInfo = groupMeInfoDAO.get(instanceId)
        if (groupMeInfo != null) {
            groupId = groupMeInfo.groupId
            apiKey = groupMeInfo.apiKey
        } else {
            error("No groupme configured for instance $instanceId")
        }
    }

    suspend fun uploadImage(inputStream: InputStream): String? {
        val resp = client.post {
            url("https://image.groupme.com/pictures")
            header("X-Access-Token", apiKey)
            header("Content-Type", "image/jpeg")
            setBody(inputStream.toByteReadChannel())
        }
        return resp.body<UploadResponse>().payload?.url
    }

    open suspend fun postMessage(message: String) {
        val resp = client.post {
            url("https://api.groupme.com/v3/groups/$groupId/messages")
            header("X-Access-Token", apiKey)
            header("Content-Type", "application/json")
            setBody(MessageRequest(Message(UUID.randomUUID(), message)))
        }
        log.trace(resp.body<String>())
    }
}
