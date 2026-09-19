package org.tinywind.graphqlschemagenerator

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.tinywind.graphqlschemagenerator.PostgresSupport.Companion.testClasspathDriver
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresSchemaTest {
    private val postgres = PostgresSupport()

    @BeforeAll
    fun start() = postgres.start()

    @AfterAll
    fun stop() = postgres.stop()

    @Test
    fun `reads enums, columns, keys and comments through jOOQ meta`() {
        val schema = MetadataReader(testClasspathDriver).read(postgres.configuration())

        assertEquals("public", schema.schemaName)
        assertEquals(listOf("board_type", "draft_type", "review_status"), schema.enums.map { it.name }.sorted())
        val boardType = schema.enums.first { it.name == "board_type" }
        assertEquals(listOf("NOTICE", "GENERAL", "COMMITTEE"), boardType.literals)
        assertEquals("Board categories.", boardType.comment)

        val tables = schema.tables.associateBy { it.name }
        assertEquals(setOf("board", "committee", "event_rsvp", "feed_mention", "flyway_schema_history", "user"), tables.keys)
        assertEquals("Standing committees.", tables.getValue("committee").comment)
        val committee = tables.getValue("committee").columns.associateBy { it.name }
        assertEquals(14 to 0, committee.getValue("budget").precision to committee.getValue("budget").scale)
        assertEquals(5 to 2, committee.getValue("share").precision to committee.getValue("share").scale)

        val board = tables.getValue("board").columns.associateBy { it.name }
        assertEquals(listOf("id", "name", "board_type", "cohort_number", "committee_id", "active", "display_order", "created_at"), tables.getValue("board").columns.map { it.name })
        assertEquals("uuid", board.getValue("id").sqlType)
        assertTrue(board.getValue("id").isPrimaryKey)
        assertFalse(board.getValue("id").isNullable)
        assertEquals("character varying", board.getValue("name").sqlType)
        assertEquals("USER-DEFINED", board.getValue("board_type").sqlType)
        assertEquals("board_type", board.getValue("board_type").userType)
        assertEquals("NOTICE allows guest reading; \"quoted\" words stay.", board.getValue("board_type").comment)
        assertTrue(board.getValue("cohort_number").isNullable)
        assertTrue(board.getValue("committee_id").isForeignKey)
        assertFalse(board.getValue("committee_id").isPrimaryKey)
        assertEquals("timestamp with time zone", board.getValue("created_at").sqlType)
        assertNull(board.getValue("created_at").comment)

        val user = tables.getValue("user").columns.associateBy { it.name }
        assertTrue(user.getValue("tags").isArray)
        assertEquals("text", user.getValue("tags").userType)
        assertEquals("jsonb", user.getValue("profile").sqlType)
        assertEquals("bigint", user.getValue("view_count").sqlType)
        assertEquals("date", user.getValue("birth_date").sqlType)
        assertTrue(user.getValue("referrer_id").isForeignKey)

        val rsvp = tables.getValue("event_rsvp").columns.associateBy { it.name }
        assertTrue(rsvp.getValue("event_id").isPrimaryKey && rsvp.getValue("event_id").isForeignKey)
        assertEquals("review_status", rsvp.getValue("status").userType)
    }

    @Test
    fun `honours the database includes and excludes`() {
        val configuration = postgres.configuration().apply { database.excludes = "flyway_schema_history" }
        val schema = MetadataReader(testClasspathDriver).read(configuration)
        assertFalse(schema.tables.any { it.name == "flyway_schema_history" })

        val only = postgres.configuration().apply { database.includes = "board|committee" }
        assertEquals(listOf("board", "committee"), MetadataReader(testClasspathDriver).read(only).tables.map { it.name }.sorted())
    }

    @Test
    fun `fails clearly for a schema that does not exist`() {
        val configuration = postgres.configuration().apply { database.inputSchema = "missing" }
        val message = assertThrows(GenerationException::class.java) { MetadataReader(testClasspathDriver).read(configuration) }.message!!
        assertTrue(message.contains("missing"), message)
        assertFalse(message.contains(postgres.password), message)
    }

    @Test
    fun `requires a database url`() {
        val configuration = postgres.configuration().apply { database.url = null }
        assertEquals("database.url is required", assertThrows(GenerationException::class.java) { MetadataReader(testClasspathDriver).read(configuration) }.message)
    }

    @Test
    fun `generates the expected SDL from the live schema`() {
        assertEquals(Fixtures.expectedSdl, GraphqlSchemaGenerator(testClasspathDriver).render(postgres.configuration()))
    }

    @Test
    fun `loads the driver from a classpath by class name or discovery`() {
        val driverJar = File(org.postgresql.Driver::class.java.protectionDomain.codeSource.location.toURI())
        val byName = ClasspathDriverResolver(listOf(driverJar)).resolve(Jdbc().apply { driverClass = "org.postgresql.Driver" }, postgres.jdbcUrl)
        assertEquals("org.postgresql.Driver", byName.javaClass.name)
        val discovered = ClasspathDriverResolver(listOf(driverJar)).resolve(Jdbc(), postgres.jdbcUrl)
        assertEquals("org.postgresql.Driver", discovered.javaClass.name)

        assertTrue(assertThrows(GenerationException::class.java) {
            ClasspathDriverResolver(listOf(driverJar)).resolve(Jdbc().apply { driverClass = "com.example.Missing" }, postgres.jdbcUrl)
        }.message!!.contains("com.example.Missing"))
        assertTrue(assertThrows(GenerationException::class.java) {
            ClasspathDriverResolver(listOf(driverJar)).resolve(Jdbc().apply { driverClass = "java.lang.String" }, postgres.jdbcUrl)
        }.message!!.contains("java.sql.Driver"))
        assertTrue(assertThrows(GenerationException::class.java) {
            ClasspathDriverResolver(listOf(driverJar)).resolve(Jdbc(), "jdbc:other://nowhere?password=secret")
        }.message!!.let { it.contains("jdbc:other://nowhere") && !it.contains("secret") })
    }
}
