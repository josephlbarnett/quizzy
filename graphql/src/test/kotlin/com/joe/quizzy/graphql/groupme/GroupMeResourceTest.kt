package com.joe.quizzy.graphql.groupme

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.joe.quizzy.api.models.Session
import com.joe.quizzy.api.models.User
import com.joe.quizzy.graphql.auth.UserPrincipal
import com.trib3.testing.server.ResourceTestBase
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.basic.BasicCredentialAuthFilter
import io.dropwizard.testing.common.Resource
import io.mockk.coEvery
import io.mockk.mockk
import jakarta.ws.rs.client.Entity
import org.testng.annotations.Test
import java.security.Principal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

val validInstanceUUID = UUID.randomUUID()
val failUploadUUID = UUID.randomUUID()

class GroupMeResourceTest : ResourceTestBase<GroupMeResource>() {
    override fun getResource(): GroupMeResource {
        return GroupMeResource(
            object : GroupMeServiceFactory {
                override fun create(instanceId: UUID): GroupMeService? {
                    return when (instanceId) {
                        validInstanceUUID -> {
                            val mockService = mockk<GroupMeService>()
                            coEvery {
                                mockService.uploadImage(any())
                            } returns "testUrl"
                            mockService
                        }

                        failUploadUUID -> {
                            val mockService = mockk<GroupMeService>()
                            coEvery {
                                mockService.uploadImage(any())
                            } returns null
                            mockService
                        }

                        else -> null
                    }
                }
            },
        )
    }

    override fun buildAdditionalResources(resourceBuilder: Resource.Builder<*>) {
        val userId = UUID.randomUUID()
        resourceBuilder.addProvider(
            AuthDynamicFeature(
                BasicCredentialAuthFilter.Builder<Principal>()
                    .setAuthenticator {
                        val instanceUUID =
                            when (it.username) {
                                "a" -> validInstanceUUID
                                "c" -> failUploadUUID
                                else -> UUID.randomUUID()
                            }
                        if (it.username == "d") {
                            Optional.of(Principal { "otherPrincipal Type" })
                        } else {
                            Optional.of(
                                UserPrincipal(
                                    User(userId, instanceUUID, "joe", "joe@joe.com", null, false, ""),
                                    Session(UUID.randomUUID(), userId, OffsetDateTime.now(), OffsetDateTime.now()),
                                ),
                            )
                        }
                    }
                    .buildAuthFilter(),
            ),
        )
    }

    @Test
    fun testImageUpload() {
        val returnedUrl =
            resource.target("/image/upload").request()
                .header("Authorization", "Basic YTpiCg==") // a:b
                .post(Entity.text("test"))
                .readEntity(String::class.java)
        assertThat(returnedUrl).isEqualTo("testUrl")
    }

    @Test
    fun testNoInstanceImageUpload() {
        val returnedUrl =
            resource.target("/image/upload").request()
                .header("Authorization", "Basic YjpiCg==") // b:b
                .post(Entity.text("test"))
                .readEntity(String::class.java)
        assertThat(returnedUrl).contains("There was an error processing your request.")
    }

    @Test
    fun testFailImageUpload() {
        val returnedUrl =
            resource.target("/image/upload").request()
                .header("Authorization", "Basic YzpiCg==") // c:b
                .post(Entity.text("test"))
                .readEntity(String::class.java)
        assertThat(returnedUrl).contains("There was an error processing your request.")
    }

    @Test
    fun testInvalidPrincipalImageUpload() {
        val returnedUrl =
            resource.target("/image/upload").request()
                .header("Authorization", "Basic ZDpiCg==") // d:b
                .post(Entity.text("test"))
                .readEntity(String::class.java)
        assertThat(returnedUrl).contains("There was an error processing your request.")
    }
}
