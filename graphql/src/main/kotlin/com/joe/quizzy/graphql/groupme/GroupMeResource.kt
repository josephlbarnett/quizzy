package com.joe.quizzy.graphql.groupme

import com.joe.quizzy.graphql.auth.UserPrincipal
import io.dropwizard.auth.Auth
import jakarta.inject.Inject
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Context
import java.security.Principal

@Path("/image")
class GroupMeResource @Inject constructor(
    val factory: GroupMeServiceFactory,
) {
    @Path("/upload")
    @POST
    suspend fun uploadImage(@Auth principal: Principal, @Context request: ContainerRequestContext): String {
        if (principal is UserPrincipal) {
            val groupMeService = factory.create(principal.user.instanceId)
            val url = groupMeService?.uploadImage(request.entityStream)
            if (url != null) {
                return url
            }
        }
        error("Could not upload image")
    }
}
