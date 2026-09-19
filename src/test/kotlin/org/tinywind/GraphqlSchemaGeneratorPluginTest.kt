package org.tinywind

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.tinywind.graphqlschemagenerator.Fixtures
import org.tinywind.graphqlschemagenerator.PostgresSupport
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GraphqlSchemaGeneratorPluginTest {
    private val postgres = PostgresSupport()

    @Test
    fun `registers the extension, the driver configuration and both tasks`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.tinywind.graphql-schema-generator")

        assertNotNull(project.extensions.findByType(GraphqlSchemaGeneratorExtension::class.java))
        val configuration = project.configurations.getByName(GraphqlSchemaGeneratorPlugin.CONFIGURATION_NAME)
        assertTrue(configuration.isCanBeResolved)
        val generate = project.tasks.getByName(GraphqlSchemaGeneratorPlugin.GENERATE_TASK_NAME)
        val verify = project.tasks.getByName(GraphqlSchemaGeneratorPlugin.VERIFY_TASK_NAME)
        assertEquals(GraphqlSchemaGeneratorPlugin.TASK_GROUP, generate.group)
        assertEquals(GraphqlSchemaGeneratorPlugin.TASK_GROUP, verify.group)
        assertTrue(generate is GenerateGraphqlSchemaTask)
        assertTrue(verify is VerifyGraphqlSchemaTask)
    }

    @Test
    fun `a generation failure surfaces as a build failure with the rule to change`(@TempDir directory: File) {
        val project = ProjectBuilder.builder().withProjectDir(directory).build()
        project.pluginManager.apply("org.tinywind.graphql-schema-generator")
        project.extensions.configure(GraphqlSchemaGeneratorExtension::class.java) {
            it.jdbc { driverClass = "com.example.Missing" }
            it.database { url = "jdbc:postgresql://localhost:1/none" }
            it.generator { outputFile = File(directory, "out.graphqls") }
        }
        val task = project.tasks.getByName(GraphqlSchemaGeneratorPlugin.GENERATE_TASK_NAME) as GenerateGraphqlSchemaTask
        val message = assertThrows(GradleException::class.java) { task.generate() }.message!!
        assertTrue(message.contains("com.example.Missing"), message)
    }

    @BeforeAll
    fun start() = postgres.start()

    @AfterAll
    fun stop() = postgres.stop()

    @Test
    fun `the tasks generate and verify the schema file against PostgreSQL`(@TempDir directory: File) {
        val project = ProjectBuilder.builder().withProjectDir(directory).build()
        project.pluginManager.apply("org.tinywind.graphql-schema-generator")
        val driverJar = File(org.postgresql.Driver::class.java.protectionDomain.codeSource.location.toURI())
        project.dependencies.add(GraphqlSchemaGeneratorPlugin.CONFIGURATION_NAME, project.files(driverJar))
        val output = File(directory, "src/main/resources/graphql/generated.graphqls")
        project.extensions.configure(GraphqlSchemaGeneratorExtension::class.java) {
            it.database {
                url = postgres.jdbcUrl
                user = postgres.username
                password = postgres.password
                inputSchema = "public"
            }
            it.generator {
                outputFile = output
                tables { include("board", "committee", "user", "event_rsvp") }
                columns { exclude("user.password_hash") }
                types { override("user.profile", "String") }
            }
        }
        val generate = project.tasks.getByName(GraphqlSchemaGeneratorPlugin.GENERATE_TASK_NAME) as GenerateGraphqlSchemaTask
        val verify = project.tasks.getByName(GraphqlSchemaGeneratorPlugin.VERIFY_TASK_NAME) as VerifyGraphqlSchemaTask

        val missing = assertThrows(GradleException::class.java) { verify.verify() }.message!!
        assertTrue(missing.contains("does not exist"), missing)

        generate.generate()
        assertEquals(Fixtures.expectedSdl, output.readText())
        verify.verify()

        output.writeText(output.readText().replace("viewCount: Long!", "viewCount: Int!"))
        val differs = assertThrows(GradleException::class.java) { verify.verify() }.message!!
        assertTrue(differs.contains("differs from the database schema at line"), differs)
        assertTrue(differs.contains("viewCount: Long!"), differs)
    }
}
