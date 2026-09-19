package org.tinywind.graphqlschemagenerator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.tinywind.graphqlschemagenerator.Fixtures.column

class TypeMapperTest {
    private val enums = mapOf("board_type" to "BoardType")
    private val mapper = TypeMapper(emptyMap())

    @ParameterizedTest
    @CsvSource(
        "uuid, UUID",
        "character varying, String",
        "text, String",
        "integer, Int",
        "smallint, Int",
        "bigint, Long",
        "boolean, Boolean",
        "double precision, Float",
        "numeric, BigDecimal",
        "timestamp with time zone, DateTime",
        "timestamp without time zone, LocalDateTime",
        "date, Date",
        "jsonb, JSON",
        "inet, String",
    )
    fun `maps the SQL types a PostgreSQL schema reports`(sqlType: String, expected: String) {
        assertEquals(MappedType(expected), mapper.map(column("c", sqlType), enums))
    }

    @Test
    fun `is case insensitive and tolerant of whitespace`() {
        assertEquals(MappedType("String"), mapper.map(column("c", " Character Varying "), enums))
    }

    @Test
    fun `resolves a user-defined column to its enum`() {
        assertEquals(MappedType("BoardType", "board_type"), mapper.map(column("c", "USER-DEFINED", userType = "board_type"), enums))
    }

    @Test
    fun `leaves an unknown user-defined type unmapped`() {
        assertNull(mapper.map(column("c", "USER-DEFINED", userType = "geometry"), enums))
        assertNull(mapper.map(column("c", "USER-DEFINED"), enums))
    }

    @Test
    fun `maps a configured user-defined type by its name`() {
        val configured = TypeMapper(mapOf("geometry" to "GeoJSON"))
        assertEquals(MappedType("GeoJSON"), configured.map(column("c", "USER-DEFINED", userType = "geometry"), enums))
    }

    @Test
    fun `configured SQL types replace the defaults`() {
        val configured = TypeMapper(mapOf("UUID" to "ID", "bytea" to "Base64"))
        assertEquals(MappedType("ID"), configured.map(column("c", "uuid"), enums))
        assertEquals(MappedType("Base64"), configured.map(column("c", "bytea"), enums))
    }

    @Test
    fun `zero-scale decimals become the integer types jOOQ generates`() {
        assertEquals(MappedType("Int"), mapper.map(column("c", "numeric", precision = 9, scale = 0), enums))
        assertEquals(MappedType("Long"), mapper.map(column("c", "numeric", precision = 18, scale = 0), enums))
        assertEquals(MappedType("BigInteger"), mapper.map(column("c", "decimal", precision = 19, scale = 0), enums))
        assertEquals(MappedType("BigDecimal"), mapper.map(column("c", "numeric", precision = 12, scale = 2), enums))
        assertEquals(MappedType("BigDecimal"), mapper.map(column("c", "numeric"), enums))
        val configured = TypeMapper(mapOf("numeric" to "Decimal"))
        assertEquals(MappedType("Decimal"), configured.map(column("c", "numeric", precision = 12, scale = 0), enums))
    }

    @Test
    fun `leaves binary and interval columns unmapped by default`() {
        assertNull(mapper.map(column("c", "bytea"), enums))
        assertNull(mapper.map(column("c", "INTERVAL DAY TO SECOND"), enums))
    }

    @Test
    fun `falls back to the user type of a plain column`() {
        assertEquals(MappedType("String"), mapper.map(column("c", "unknown", userType = "varchar"), enums))
    }

    @Test
    fun `renders arrays as non-null element lists`() {
        assertEquals(MappedType("[String!]"), mapper.map(column("c", "text ARRAY", userType = "text", isArray = true), enums))
        assertEquals(MappedType("[[Int!]!]"), mapper.map(column("c", "int4 ARRAY ARRAY", userType = "int4", isArray = true), enums))
        assertEquals(MappedType("[BoardType!]", "board_type"), mapper.map(column("c", "board_type ARRAY", userType = "board_type", isArray = true), enums))
    }

    @Test
    fun `leaves an array of an unmapped element unmapped`() {
        assertNull(mapper.map(column("c", "bytea ARRAY", userType = "bytea", isArray = true), enums))
        assertNull(mapper.map(column("c", "ARRAY", isArray = true), enums))
    }
}
