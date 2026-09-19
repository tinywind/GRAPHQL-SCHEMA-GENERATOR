package org.tinywind.graphqlschemagenerator

/** The part of a database schema the generator reads, detached from the metadata source. */
data class SchemaModel(
    val schemaName: String,
    val enums: List<EnumModel>,
    val tables: List<TableModel>,
)

data class EnumModel(
    val name: String,
    val literals: List<String>,
    val comment: String?,
)

data class TableModel(
    val name: String,
    val comment: String?,
    val columns: List<ColumnModel>,
)

data class ColumnModel(
    val name: String,
    /** The SQL type as the database reports it, such as `character varying`, `USER-DEFINED` or `text ARRAY`. */
    val sqlType: String,
    /** The user-defined type behind `USER-DEFINED` columns and the element type of arrays. */
    val userType: String?,
    val isArray: Boolean,
    val isNullable: Boolean,
    val isPrimaryKey: Boolean,
    val isForeignKey: Boolean,
    val comment: String?,
    /** Numeric precision and scale as reported by the database; zero when the type carries none. */
    val precision: Int = 0,
    val scale: Int = 0,
)
