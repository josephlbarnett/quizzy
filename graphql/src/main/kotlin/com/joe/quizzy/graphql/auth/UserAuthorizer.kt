package com.joe.quizzy.graphql.auth

import io.dropwizard.auth.Authorizer

class UserAuthorizer : Authorizer<UserPrincipal> {
    override fun authorize(principal: UserPrincipal, role: String): Boolean {
        return role != "ADMIN" || principal.user.admin
    }
}
