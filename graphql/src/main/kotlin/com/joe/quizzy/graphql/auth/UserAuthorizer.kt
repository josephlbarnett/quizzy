package com.joe.quizzy.graphql.auth

import io.dropwizard.auth.Authorizer
import jakarta.ws.rs.container.ContainerRequestContext
import java.security.Principal

class UserAuthorizer : Authorizer<Principal> {
    override fun authorize(
        principal: Principal,
        role: String,
        requestContext: ContainerRequestContext?,
    ): Boolean = principal is UserPrincipal && (role != "ADMIN" || principal.user.admin)
}
