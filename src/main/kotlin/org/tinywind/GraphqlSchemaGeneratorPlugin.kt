package org.tinywind

import org.gradle.api.Plugin
import org.gradle.api.Project

open class GraphqlSchemaGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, GraphqlSchemaGeneratorExtension::class.java)
        val driverClasspath = project.configurations.create(CONFIGURATION_NAME).apply {
            description = "The classpath the GraphQL schema generator loads the JDBC driver from."
            isCanBeConsumed = false
            isCanBeResolved = true
        }

        project.tasks.register(GENERATE_TASK_NAME, GenerateGraphqlSchemaTask::class.java, extension, driverClasspath).configure {
            it.group = TASK_GROUP
            it.description = "Generates GraphQL SDL types and enums from the database schema"
        }
        project.tasks.register(VERIFY_TASK_NAME, VerifyGraphqlSchemaTask::class.java, extension, driverClasspath).configure {
            it.group = TASK_GROUP
            it.description = "Fails when the generated GraphQL schema file no longer matches the database schema"
        }
    }

    companion object {
        const val EXTENSION_NAME = "graphqlSchema"
        const val CONFIGURATION_NAME = "graphqlSchema"
        const val GENERATE_TASK_NAME = "generateGraphqlSchema"
        const val VERIFY_TASK_NAME = "verifyGraphqlSchema"
        const val TASK_GROUP = "graphql schema"
    }
}
