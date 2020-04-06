package com.joe.quizzy.server.auth

import com.joe.quizzy.api.models.Session
import com.joe.quizzy.api.models.User
import java.security.Principal

data class UserPrincipal(val user: User, val session: Session?) : Principal {
    override fun getName(): String {
        return user.name
    }
}
