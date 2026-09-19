package org.tinywind

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Internal
import org.tinywind.graphqlschemagenerator.ClasspathDriverResolver
import org.tinywind.graphqlschemagenerator.GenerationException
import org.tinywind.graphqlschemagenerator.GraphqlSchemaGenerator
import javax.inject.Inject

/** Shared wiring of the generate and verify tasks: the extension, the driver classpath and the live-database caveats. */
abstract class GraphqlSchemaTask @Inject constructor(
    @get:Internal val extension: GraphqlSchemaGeneratorExtension,
    @get:Classpath val driverClasspath: FileCollection,
) : DefaultTask() {

    init {
        outputs.upToDateWhen { false }
        notCompatibleWithConfigurationCache("The task reads live database state through the configured JDBC connection.")
    }

    protected fun <T> generate(action: GraphqlSchemaGenerator.() -> T): T {
        val generator = GraphqlSchemaGenerator(ClasspathDriverResolver(driverClasspath.files))
        return try {
            generator.action()
        } catch (e: GenerationException) {
            throw GradleException(e.message ?: "GraphQL schema generation failed", e)
        }
    }
}
