package com.joe.quizzy.graphql.auth

import com.joe.quizzy.api.models.Session
import com.joe.quizzy.api.models.User
import java.security.Principal

data class UserPrincipal(
    val user: User,
    val session: Session?,
) : Principal {
    override fun getName(): String = user.name
}
