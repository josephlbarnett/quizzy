package com.joe.quizzy.graphql.auth

import io.dropwizard.auth.Authorizer
import java.security.Principal

class UserAuthorizer : Authorizer<Principal> {
    override fun authorize(principal: Principal, role: String): Boolean {
        return principal is UserPrincipal && (role != "ADMIN" || principal.user.admin)
    }
}
