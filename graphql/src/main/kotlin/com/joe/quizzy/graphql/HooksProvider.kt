package com.joe.quizzy.graphql

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import com.expediagroup.graphql.plugin.schema.hooks.SchemaGeneratorHooksProvider
import com.trib3.graphql.execution.LeakyCauldronHooks

class HooksProvider : SchemaGeneratorHooksProvider {
    override fun hooks(): SchemaGeneratorHooks = LeakyCauldronHooks()
}
