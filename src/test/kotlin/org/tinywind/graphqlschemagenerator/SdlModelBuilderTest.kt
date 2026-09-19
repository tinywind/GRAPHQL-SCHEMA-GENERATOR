package org.tinywind.graphqlschemagenerator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tinywind.graphqlschemagenerator.Fixtures.column
import org.tinywind.graphqlschemagenerator.Fixtures.configuration
import org.tinywind.graphqlschemagenerator.Fixtures.schema

class SdlModelBuilderTest {

    private fun build(model: SchemaModel = schema, configure: Generator.() -> Unit = {}): SdlDocument =
        SdlModelBuilder(configuration(configure).generator).build(model)

    private fun failure(model: SchemaModel = schema, configure: Generator.() -> Unit = {}): String =
        assertThrows(GenerationException::class.java) { build(model, configure) }.message!!

    private fun SdlDocument.type(name: String): SdlType = types.first { it.name == name }

    private fun SdlType.fieldNames(): List<String> = fields.map { it.name }

    @Test
    fun `tables are selected only through explicit include patterns`() {
        val message = assertThrows(GenerationException::class.java) {
            SdlModelBuilder(Configuration().generator).build(schema)
        }.message!!
        assertTrue(message.contains("generator.tables.include"), message)
    }

    @Test
    fun `selected tables become types sorted by name with fields in column order`() {
        val document = build()
        assertEquals(listOf("Board", "Committee", "EventRsvp", "User"), document.types.map { it.name })
        assertEquals(listOf("id", "name", "boardType", "cohortNumber", "active", "displayOrder", "createdAt"), document.type("Board").fieldNames())
    }

    @Test
    fun `include patterns are regular expressions matched against the whole name`() {
        val document = build { tables.includes.clear(); columns.excludes.clear(); types.columnTypes.clear(); tables { include("board.*", "committee") } }
        assertEquals(listOf("Board", "Committee"), document.types.map { it.name })
    }

    @Test
    fun `a table exclude removes a table the include selected`() {
        val document = build { columns.excludes.clear(); types.columnTypes.clear(); tables { exclude("user") } }
        assertEquals(listOf("Board", "Committee", "EventRsvp"), document.types.map { it.name })
    }

    @Test
    fun `foreign key columns are dropped unless they belong to the primary key`() {
        val document = build()
        assertFalse(document.type("Board").fieldNames().contains("committeeId"))
        assertFalse(document.type("User").fieldNames().contains("referrerId"))
        assertEquals(listOf("eventId", "userId", "status"), document.type("EventRsvp").fieldNames())
    }

    @Test
    fun `keepPrimaryKeys false drops foreign key columns inside the primary key`() {
        val document = build { columns.keepPrimaryKeys = false }
        assertEquals(listOf("status"), document.type("EventRsvp").fieldNames())
    }

    @Test
    fun `excludeForeignKeys false keeps every foreign key column`() {
        val document = build { columns.excludeForeignKeys = false }
        assertTrue(document.type("Board").fieldNames().contains("committeeId"))
    }

    @Test
    fun `a column include keeps a foreign key column`() {
        val document = build { columns { include("board.committee_id") } }
        assertEquals("UUID", document.type("Board").fields.first { it.name == "committeeId" }.type)
    }

    @Test
    fun `a column exclude wins over an include`() {
        val document = build { columns { include("user.password_hash") } }
        assertFalse(document.type("User").fieldNames().contains("passwordHash"))
    }

    @Test
    fun `nullability follows the column unless overridden`() {
        val document = build { types { nullable("board.name"); nonNull("board.cohort_number") } }
        val board = document.type("Board")
        assertFalse(board.fields.first { it.name == "name" }.nonNull)
        assertTrue(board.fields.first { it.name == "cohortNumber" }.nonNull)
        assertTrue(board.fields.first { it.name == "id" }.nonNull)
        assertFalse(document.type("User").fields.first { it.name == "birthDate" }.nonNull)
    }

    @Test
    fun `a column cannot be both nullable and non-null`() {
        val message = failure { types { nullable("board.name"); nonNull("board.name") } }
        assertTrue(message.contains("board.name"), message)
    }

    @Test
    fun `type overrides and SQL type mappings change the field type`() {
        val document = build { types { map("uuid", "ID"); override("user.view_count", "Int") } }
        assertEquals("ID", document.type("Board").fields.first { it.name == "id" }.type)
        assertEquals("Int", document.type("User").fields.first { it.name == "viewCount" }.type)
        assertEquals("String", document.type("User").fields.first { it.name == "profile" }.type)
    }

    @Test
    fun `the first matching override wins`() {
        val document = build { types { override("user\\..*", "Any"); override("user.login_id", "Never") } }
        assertEquals("Any", document.type("User").fields.first { it.name == "loginId" }.type)
    }

    @Test
    fun `an unmapped SQL type names the column and the rules that fix it`() {
        val blob = TableModel("blob", null, listOf(column("id", "uuid", primaryKey = true), column("data", "bytea")))
        val message = failure(schema.copy(tables = schema.tables + blob)) { tables { include("blob") } }
        assertTrue(message.contains("blob.data"), message)
        assertTrue(message.contains("bytea"), message)
        assertTrue(message.contains("generator.types.map"), message)
        assertTrue(message.contains("generator.columns.exclude"), message)
    }

    @Test
    fun `only referenced enums are emitted by default`() {
        assertEquals(listOf("BoardType", "ReviewStatus"), build().enums.map { it.name })
    }

    @Test
    fun `includeUnreferenced emits every enum`() {
        assertEquals(listOf("BoardType", "DraftType", "ReviewStatus"), build { enums.includeUnreferenced = true }.enums.map { it.name })
    }

    @Test
    fun `an enum include pattern forces an enum`() {
        assertEquals(listOf("BoardType", "DraftType", "ReviewStatus"), build { enums { include("draft_.*") } }.enums.map { it.name })
    }

    @Test
    fun `enum values keep the database order and text`() {
        assertEquals(listOf("NOTICE", "GENERAL", "COMMITTEE"), build().enums.first().values)
    }

    @Test
    fun `patterns that match nothing fail generation`() {
        assertTrue(failure { tables { include("boards") } }.contains("generator.tables.include pattern 'boards'"))
        assertTrue(failure { tables { exclude("nothing") } }.contains("generator.tables.exclude pattern 'nothing'"))
        assertTrue(failure { columns { exclude("user.secret") } }.contains("generator.columns.exclude pattern 'user.secret'"))
        assertTrue(failure { columns { include("feed_mention.feed_id") } }.contains("generator.columns.include pattern"))
        assertTrue(failure { types { override("user.missing", "String") } }.contains("generator.types.override pattern 'user.missing'"))
        assertTrue(failure { types { nullable("board.missing") } }.contains("generator.types.nullable pattern"))
        assertTrue(failure { types { nonNull("board.missing") } }.contains("generator.types.nonNull pattern"))
        assertTrue(failure { enums { include("missing_enum") } }.contains("generator.enums.include pattern 'missing_enum'"))
    }

    @Test
    fun `an invalid regular expression names the setting`() {
        val message = failure { tables { include("board(") } }
        assertTrue(message.contains("generator.tables.include pattern 'board('"), message)
    }

    @Test
    fun `a table that keeps no column fails generation`() {
        val message = failure { tables { include("feed_mention") } }
        assertTrue(message.contains("feed_mention"), message)
    }

    @Test
    fun `two objects with the same GraphQL name fail generation`() {
        val clash = schema.copy(enums = schema.enums + EnumModel("board", listOf("A"), null))
        val message = failure(clash) { enums.includeUnreferenced = true }
        assertTrue(message.contains("'Board'"), message)
    }

    @Test
    fun `two columns that map to one field fail generation`() {
        val twins = TableModel("twins", null, listOf(column("user_id", "uuid"), column("USER_ID", "uuid")))
        val message = failure(schema.copy(tables = schema.tables + twins)) { tables.includes.clear(); tables { include("twins") } }
        assertTrue(message.contains("twins"), message)
    }

    @Test
    fun `an invalid enum literal fails generation`() {
        val bad = schema.copy(enums = schema.enums + EnumModel("mood", listOf("so-so"), null))
        val message = failure(bad) { enums.includeUnreferenced = true }
        assertTrue(message.contains("so-so"), message)
    }

    @Test
    fun `an empty enum fails generation`() {
        val empty = schema.copy(enums = schema.enums + EnumModel("nothing", emptyList(), null))
        assertTrue(failure(empty) { enums.includeUnreferenced = true }.contains("nothing"))
    }

    @Test
    fun `descriptions come from comments and can be switched off`() {
        val withDescriptions = build()
        assertEquals("Standing committees.", withDescriptions.type("Committee").description)
        assertEquals("Board categories.", withDescriptions.enums.first().description)
        assertTrue(withDescriptions.type("Board").fields.first { it.name == "boardType" }.description!!.startsWith("NOTICE"))

        val without = build { descriptions = false }
        assertNull(without.type("Committee").description)
        assertNull(without.enums.first().description)
        assertNull(without.type("Board").fields.first { it.name == "boardType" }.description)
    }

    @Test
    fun `the header defaults to a comment naming the schema`() {
        assertEquals("# Generated by GRAPHQL-SCHEMA-GENERATOR from database schema \"public\". Do not edit.", build().header)
        assertEquals("# mine", build { header = "# mine" }.header)
        assertNull(build { header = " " }.header)
    }
}
