package org.tinywind

import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.tinywind.graphqlschemagenerator.Verification
import javax.inject.Inject

@UntrackedTask(because = GraphqlSchemaTask.LIVE_DATABASE)
abstract class VerifyGraphqlSchemaTask @Inject constructor(
    extension: GraphqlSchemaGeneratorExtension,
    driverClasspath: FileCollection,
) : GraphqlSchemaTask(extension, driverClasspath) {

    @TaskAction
    fun verify() {
        when (val result = generate { verify(extension) }) {
            is Verification.Match -> logger.lifecycle("${result.file} matches the database schema")
            is Verification.Missing -> throw GradleException(
                "${result.file} does not exist; run ${GraphqlSchemaGeneratorPlugin.GENERATE_TASK_NAME} and commit the result",
            )
            is Verification.Different -> throw GradleException(
                "${result.file} differs from the database schema at line ${result.line}\n" +
                    "  expected: ${result.expected ?: "<end of file>"}\n" +
                    "  actual:   ${result.actual ?: "<end of file>"}\n" +
                    "Run ${GraphqlSchemaGeneratorPlugin.GENERATE_TASK_NAME} and commit the result.",
            )
        }
    }
}
