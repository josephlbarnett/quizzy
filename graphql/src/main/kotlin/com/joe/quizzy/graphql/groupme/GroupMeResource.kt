package com.joe.quizzy.graphql.groupme

import com.joe.quizzy.graphql.auth.UserPrincipal
import io.dropwizard.auth.Auth
import java.security.Principal
import javax.inject.Inject
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.Context

@Path("/image")
class GroupMeResource @Inject constructor(
    val factory: GroupMeServiceFactory
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
