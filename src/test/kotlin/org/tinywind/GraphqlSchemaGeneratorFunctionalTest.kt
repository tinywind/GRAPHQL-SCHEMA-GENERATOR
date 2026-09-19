package org.tinywind

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.tinywind.graphqlschemagenerator.Fixtures
import org.tinywind.graphqlschemagenerator.PostgresSupport
import java.io.File
import java.nio.file.Files

/** Runs a consumer build with real Gradle distributions and JDKs, as SCHEME-REPORTER's functional test does. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GraphqlSchemaGeneratorFunctionalTest {
    private val postgres = PostgresSupport()

    @BeforeAll
    fun start() = postgres.start()

    @AfterAll
    fun stop() = postgres.stop()

    @ParameterizedTest(name = "{2}")
    @MethodSource("toolchains")
    fun `generateGraphqlSchema writes the file and verifyGraphqlSchema guards it`(gradleVersion: String, javaHome: String, label: String) {
        val projectDir = Files.createTempDirectory("graphql-schema-generator-test").toFile()
        try {
            val output = File(projectDir, "src/main/resources/graphql/generated.graphqls")
            File(projectDir, "settings.gradle.kts").writeText("rootProject.name = \"consumer\"\n")
            File(projectDir, "build.gradle.kts").writeText(buildScript(output))

            val generated = run(projectDir, gradleVersion, javaHome, GraphqlSchemaGeneratorPlugin.GENERATE_TASK_NAME)
            assertEquals(TaskOutcome.SUCCESS, generated.task(":${GraphqlSchemaGeneratorPlugin.GENERATE_TASK_NAME}")?.outcome, label)
            assertEquals(Fixtures.expectedSdl, output.readText(), label)
            assertTrue(generated.output.contains("4 types, 2 enums"), generated.output)

            val verified = run(projectDir, gradleVersion, javaHome, GraphqlSchemaGeneratorPlugin.VERIFY_TASK_NAME)
            assertEquals(TaskOutcome.SUCCESS, verified.task(":${GraphqlSchemaGeneratorPlugin.VERIFY_TASK_NAME}")?.outcome, label)

            output.appendText("\ntype Extra {\n    id: ID!\n}\n")
            val failed = runner(projectDir, gradleVersion, javaHome, GraphqlSchemaGeneratorPlugin.VERIFY_TASK_NAME).buildAndFail()
            assertEquals(TaskOutcome.FAILED, failed.task(":${GraphqlSchemaGeneratorPlugin.VERIFY_TASK_NAME}")?.outcome, label)
            assertTrue(failed.output.contains("differs from the database schema"), failed.output)
        } finally {
            projectDir.deleteRecursively()
        }
    }

    private fun buildScript(output: File): String = """
        plugins {
            id("org.tinywind.graphql-schema-generator")
        }

        repositories {
            mavenCentral()
        }

        dependencies {
            graphqlSchema("${System.getProperty("postgresql.driver.coordinates")}")
        }

        graphqlSchema {
            jdbc {
                driverClass = "org.postgresql.Driver"
            }
            database {
                url = "${postgres.jdbcUrl}"
                user = "${postgres.username}"
                password = "${postgres.password}"
                inputSchema = "public"
            }
            generator {
                outputFile = file("${output.absolutePath.replace("\\", "/")}")
                tables {
                    include("board", "committee", "user", "event_rsvp")
                }
                columns {
                    exclude("user.password_hash")
                }
                types {
                    override("user.profile", "String")
                }
            }
        }
    """.trimIndent()

    private fun run(projectDir: File, gradleVersion: String, javaHome: String, task: String): BuildResult =
        runner(projectDir, gradleVersion, javaHome, task).build()

    private fun runner(projectDir: File, gradleVersion: String, javaHome: String, task: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withGradleVersion(gradleVersion)
            .withArguments(task, "--stacktrace")
            .withEnvironment(System.getenv() + ("JAVA_HOME" to javaHome))
            .forwardOutput()

    companion object {
        private val sdkmanJava = File(System.getenv("SDKMAN_DIR") ?: "${System.getProperty("user.home")}/.sdkman", "candidates/java")

        private fun jdk(prefix: String): String? = sdkmanJava.listFiles()
            ?.filter { it.isDirectory && it.name != "current" && it.name.startsWith(prefix) }
            ?.maxByOrNull { it.name }
            ?.absolutePath

        @JvmStatic
        fun toolchains(): List<Arguments> {
            val jdk21 = jdk("21")
            val jdk25 = jdk("25")
            val toolchains = buildList {
                if (jdk21 != null) {
                    add(Arguments.of("8.14.3", jdk21, "Gradle 8.14.3 + JDK 21"))
                    add(Arguments.of("9.4.0", jdk21, "Gradle 9.4.0 + JDK 21"))
                }
                if (jdk25 != null) add(Arguments.of("9.4.0", jdk25, "Gradle 9.4.0 + JDK 25"))
            }
            assumeTrue(toolchains.isNotEmpty(), "No SDKMAN JDK 21 or 25 found; the functional test is skipped")
            return toolchains
        }
    }
}
