package org.tinywind

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import javax.inject.Inject

@UntrackedTask(because = GraphqlSchemaTask.LIVE_DATABASE)
abstract class GenerateGraphqlSchemaTask @Inject constructor(
    extension: GraphqlSchemaGeneratorExtension,
    driverClasspath: FileCollection,
) : GraphqlSchemaTask(extension, driverClasspath) {

    @TaskAction
    fun generate() {
        val result = generate { generate(extension) }
        logger.lifecycle("Generated ${result.file} (${result.typeCount} types, ${result.enumCount} enums)")
    }
}
