package org.tinywind.graphqlschemagenerator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class NamingTest {

    @ParameterizedTest
    @CsvSource(
        "user, User",
        "access_log, AccessLog",
        "board_type, BoardType",
        "USER_STATUS, UserStatus",
        "user_feed_board_setting, UserFeedBoardSetting",
    )
    fun `type names follow jOOQ's generated class names`(databaseName: String, expected: String) {
        assertEquals(expected, typeName(databaseName))
    }

    @ParameterizedTest
    @CsvSource(
        "id, id",
        "board_type, boardType",
        "cohort_number, cohortNumber",
        "attestation_client_data_json, attestationClientDataJson",
        "uv_initialized, uvInitialized",
    )
    fun `field names follow jOOQ's generated property names`(columnName: String, expected: String) {
        assertEquals(expected, fieldName(columnName))
    }

    @Test
    fun `reserved and invalid type names are rejected`() {
        listOf("Query", "Mutation", "String", "ID", "__Meta", "2fa").forEach { name ->
            val error = assertThrows(GenerationException::class.java) { validateTypeName(name, "Table $name") }
            assertTrue(error.message!!.contains(name), error.message)
        }
        validateTypeName("Board", "Table board")
    }

    @Test
    fun `invalid field names are rejected`() {
        assertThrows(GenerationException::class.java) { validateFieldName("__typename", "Column t.__typename") }
        assertThrows(GenerationException::class.java) { validateFieldName("1st", "Column t.1st") }
        validateFieldName("createdAt", "Column t.created_at")
    }

    @Test
    fun `enum literals must be valid GraphQL enum values`() {
        listOf("true", "false", "null", "in-progress", "").forEach { literal ->
            assertThrows(GenerationException::class.java) { validateEnumValue(literal, "Enum e") }
        }
        validateEnumValue("IN_PROGRESS", "Enum e")
    }
}
