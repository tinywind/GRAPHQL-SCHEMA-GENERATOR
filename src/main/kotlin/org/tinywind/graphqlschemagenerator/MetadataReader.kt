package org.tinywind.graphqlschemagenerator

import org.jooq.meta.Databases
import org.jooq.meta.jaxb.CatalogMappingType
import org.jooq.meta.jaxb.SchemaMappingType
import org.jooq.tools.jdbc.JDBCUtils
import java.sql.Connection
import java.sql.SQLException
import java.util.Properties

/** Reads tables, columns, keys, comments and enums through jOOQ meta, the metadata layer jOOQ's own code generator uses. */
class MetadataReader(private val driverResolver: DriverResolver) {

    fun read(configuration: Configuration): SchemaModel {
        val database = configuration.database
        val url = database.url?.takeIf { it.isNotBlank() } ?: throw GenerationException("database.url is required")
        val driver = driverResolver.resolve(configuration.jdbc, url)
        val properties = Properties().apply {
            database.user?.let { put("user", it) }
            database.password?.let { put("password", it) }
        }
        try {
            val connection = driver.connect(url, properties)
                ?: throw GenerationException("Driver ${driver.javaClass.name} does not accept '${sanitize(url)}'")
            return connection.use { read(it, url, database) }
        } catch (e: SQLException) {
            throw GenerationException("Reading the schema through '${sanitize(url)}' failed: ${e.message}", e)
        }
    }

    private fun read(connection: Connection, url: String, database: Database): SchemaModel {
        val meta = Databases.databaseClass(JDBCUtils.dialect(url)).getDeclaredConstructor().newInstance()
        val schema = SchemaMappingType().withInputSchema(database.inputSchema.trim())
        val catalog = CatalogMappingType().withInputCatalog("")
        catalog.schemata.add(schema)
        meta.connection = connection
        meta.setConfiguredCatalogs(listOf(catalog))
        meta.setConfiguredSchemata(listOf(schema))
        meta.includes = arrayOf(database.includes)
        meta.excludes = database.excludes.takeIf { it.isNotBlank() }?.let { arrayOf(it) } ?: emptyArray()
        meta.setIncludeRelations(true)

        val schemata = meta.schemata
        if (schemata.isEmpty()) {
            throw GenerationException("No schema named '${database.inputSchema}' is visible through '${sanitize(url)}'")
        }
        val enums = schemata.flatMap { s ->
            meta.getEnums(s).map { EnumModel(it.name, it.literals.toList(), it.comment.orBlankToNull()) }
        }
        val tables = schemata.flatMap { s ->
            meta.getTables(s).map { table ->
                TableModel(
                    name = table.name,
                    comment = table.comment.orBlankToNull(),
                    columns = table.columns.map { column ->
                        val type = column.definedType
                        ColumnModel(
                            name = column.name,
                            sqlType = type.type,
                            userType = type.userType,
                            isArray = type.isArray || isNativeArray(type.type),
                            isNullable = type.isNullable,
                            isPrimaryKey = column.primaryKey != null,
                            isForeignKey = column.foreignKeys.isNotEmpty(),
                            comment = column.comment.orBlankToNull(),
                            precision = type.precision,
                            scale = type.scale,
                        )
                    },
                )
            }
        }
        return SchemaModel(schemata.joinToString("|") { it.name }, enums, tables)
    }

    private fun String?.orBlankToNull(): String? = this?.takeIf { it.isNotBlank() }

    /** jOOQ reports a PostgreSQL array column as `<element type> ARRAY`; `isArray()` covers only user-defined array types. */
    private fun isNativeArray(sqlType: String): Boolean = sqlType.trim().uppercase().let { it == "ARRAY" || it.endsWith(" ARRAY") }
}
