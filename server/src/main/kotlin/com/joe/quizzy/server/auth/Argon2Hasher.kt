package com.joe.quizzy.server.auth

import de.mkammerer.argon2.Argon2Factory

class Argon2Hasher : Hasher {
    private val argon = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)
    override fun hash(password: String): String {
        return argon.hash(100, 1024, 2, password.toCharArray())
    }

    override fun verify(hash: String, password: String): Boolean {
        return argon.verify(hash, password.toCharArray())
    }
}
