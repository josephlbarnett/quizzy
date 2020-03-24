package com.joe.quizzy.persistence.api

import com.joe.quizzy.api.models.Instance
import java.util.UUID
import java.util.stream.Stream

/**
 * DAO for managing Things
 */
interface InstanceDAO {
    fun all(): List<Instance>
    fun stream(): Stream<Instance>
    fun get(id: UUID): Instance?
    fun save(thing: Instance): Instance
}
