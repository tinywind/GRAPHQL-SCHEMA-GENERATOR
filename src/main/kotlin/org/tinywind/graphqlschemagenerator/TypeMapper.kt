package org.tinywind.graphqlschemagenerator

data class MappedType(val expression: String, val enumName: String? = null)

/** Resolves the GraphQL type of a column from its SQL type, the schema enums and the configured mappings. */
class TypeMapper(customSqlTypes: Map<String, String>) {
    private val customSqlTypes: Map<String, String> = customSqlTypes.mapKeys { normalize(it.key) }
    private val sqlTypes: Map<String, String> = DEFAULT_SQL_TYPES + this.customSqlTypes

    /** Returns null when neither the defaults nor the configuration map the column. */
    fun map(column: ColumnModel, enumTypeNames: Map<String, String>): MappedType? {
        if (column.isArray) return mapArray(column, enumTypeNames)
        val sqlType = normalize(column.sqlType)
        if (sqlType == USER_DEFINED) {
            val userType = column.userType ?: return null
            enumTypeNames[userType]?.let { return MappedType(it, userType) }
            return scalar(userType)
        }
        return zeroScaleDecimal(sqlType, column) ?: scalar(sqlType) ?: column.userType?.let { scalar(it) }
    }

    /** jOOQ generates integer properties for zero-scale decimals; the same widths keep the field in step with the POJO. */
    private fun zeroScaleDecimal(sqlType: String, column: ColumnModel): MappedType? {
        if (sqlType !in DECIMAL_TYPES || sqlType in customSqlTypes || column.scale != 0 || column.precision <= 0) return null
        return MappedType(
            when {
                column.precision <= INT_PRECISION -> "Int"
                column.precision <= LONG_PRECISION -> "Long"
                else -> "BigInteger"
            },
        )
    }

    private fun mapArray(column: ColumnModel, enumTypeNames: Map<String, String>): MappedType? {
        val elementType = column.userType ?: return null
        val element = enumTypeNames[elementType]?.let { MappedType(it, elementType) } ?: scalar(elementType) ?: return null
        val dimensions = ARRAY_SUFFIX.findAll(column.sqlType).count().coerceAtLeast(1)
        val expression = (1..dimensions).fold(element.expression) { inner, _ -> "[$inner!]" }
        return MappedType(expression, element.enumName)
    }

    private fun scalar(type: String): MappedType? = sqlTypes[normalize(type)]?.let { MappedType(it) }

    private fun normalize(type: String): String = type.trim().lowercase()

    companion object {
        private const val USER_DEFINED = "user-defined"
        private const val INT_PRECISION = 9
        private const val LONG_PRECISION = 18
        private val DECIMAL_TYPES = setOf("numeric", "decimal")
        private val ARRAY_SUFFIX = Regex(" array", RegexOption.IGNORE_CASE)

        val DEFAULT_SQL_TYPES: Map<String, String> = mapOf(
            "uuid" to "UUID",
            "character varying" to "String",
            "varchar" to "String",
            "character" to "String",
            "bpchar" to "String",
            "char" to "String",
            "text" to "String",
            "name" to "String",
            "citext" to "String",
            "inet" to "String",
            "cidr" to "String",
            "macaddr" to "String",
            "macaddr8" to "String",
            "integer" to "Int",
            "int4" to "Int",
            "smallint" to "Int",
            "int2" to "Int",
            "bigint" to "Long",
            "int8" to "Long",
            "boolean" to "Boolean",
            "bool" to "Boolean",
            "real" to "Float",
            "float4" to "Float",
            "double precision" to "Float",
            "float8" to "Float",
            "numeric" to "BigDecimal",
            "decimal" to "BigDecimal",
            "timestamp with time zone" to "DateTime",
            "timestamptz" to "DateTime",
            "timestamp without time zone" to "LocalDateTime",
            "timestamp" to "LocalDateTime",
            "date" to "Date",
            "time with time zone" to "Time",
            "timetz" to "Time",
            "time without time zone" to "LocalTime",
            "time" to "LocalTime",
            "json" to "JSON",
            "jsonb" to "JSON",
        )
    }
}
