package com.joe.quizzy.graphql.auth

import io.dropwizard.auth.Authorizer
import java.security.Principal
import javax.ws.rs.container.ContainerRequestContext

class UserAuthorizer : Authorizer<Principal> {
    override fun authorize(principal: Principal, role: String, requestContext: ContainerRequestContext?): Boolean {
        return principal is UserPrincipal && (role != "ADMIN" || principal.user.admin)
    }
}
