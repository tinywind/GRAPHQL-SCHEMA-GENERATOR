package org.tinywind.graphqlschemagenerator

import org.jooq.tools.StringUtils

private val GRAPHQL_NAME = Regex("[_A-Za-z][_0-9A-Za-z]*")
private val RESERVED_TYPE_NAMES = setOf("Int", "Float", "String", "Boolean", "ID", "Query", "Mutation", "Subscription")
private val RESERVED_ENUM_VALUES = setOf("true", "false", "null")

/** The name jOOQ gives the generated class of a table or enum: `access_log` becomes `AccessLog`. */
fun typeName(databaseName: String): String = StringUtils.toCamelCase(databaseName)

/** The name jOOQ gives the generated property of a column: `created_at` becomes `createdAt`. */
fun fieldName(columnName: String): String = StringUtils.toCamelCaseLC(columnName)

fun validateTypeName(name: String, source: String) {
    validateName(name, source)
    if (name in RESERVED_TYPE_NAMES) {
        throw GenerationException("$source maps to the GraphQL name '$name', which is reserved; rename the object or exclude it")
    }
}

fun validateFieldName(name: String, source: String) = validateName(name, source)

fun validateEnumValue(value: String, source: String) {
    if (!GRAPHQL_NAME.matches(value) || value in RESERVED_ENUM_VALUES) {
        throw GenerationException("$source has the literal '$value', which is not a valid GraphQL enum value")
    }
}

private fun validateName(name: String, source: String) {
    if (!GRAPHQL_NAME.matches(name) || name.startsWith("__")) {
        throw GenerationException("$source maps to '$name', which is not a valid GraphQL name")
    }
}
