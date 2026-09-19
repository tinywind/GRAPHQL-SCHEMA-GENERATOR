package org.tinywind.graphqlschemagenerator

import java.io.File

class Jdbc {
    /** JDBC driver class. When null, the first driver on the driver classpath that accepts the URL is used. */
    var driverClass: String? = null
}

class Database {
    var url: String? = null
    var user: String? = null
    var password: String? = null

    /** Schema whose tables and enums are read. Blank reads every schema the connection can see. */
    var inputSchema: String = ""

    /** Regular expression of the tables to load from the database, as in jOOQ's `includes`. */
    var includes: String = ".*"

    /** Regular expression of the tables never loaded, as in jOOQ's `excludes`. */
    var excludes: String = ""
}

class TableRules {
    val includes: MutableList<String> = mutableListOf()
    val excludes: MutableList<String> = mutableListOf()

    /** Tables that become GraphQL types. Patterns are regular expressions matched against the whole table name. */
    fun include(vararg patterns: String) {
        includes += patterns
    }

    fun exclude(vararg patterns: String) {
        excludes += patterns
    }
}

class ColumnRules {
    /** Drops columns that take part in a foreign key; the consumer exposes the relation instead. */
    var excludeForeignKeys: Boolean = true

    /** Keeps foreign-key columns that also form the primary key, such as the columns of a composite key. */
    var keepPrimaryKeys: Boolean = true

    val includes: MutableList<String> = mutableListOf()
    val excludes: MutableList<String> = mutableListOf()

    /** Columns kept although the foreign-key rule would drop them. Patterns match `table.column`. */
    fun include(vararg patterns: String) {
        includes += patterns
    }

    /** Columns never exposed. An exclusion wins over every other column rule. Patterns match `table.column`. */
    fun exclude(vararg patterns: String) {
        excludes += patterns
    }
}

class TypeRules {
    /** SQL type name, as the database reports it, to GraphQL type name. */
    val sqlTypes: MutableMap<String, String> = linkedMapOf()

    /** `table.column` pattern to GraphQL type expression, applied in declaration order. */
    val columnTypes: MutableMap<String, String> = linkedMapOf()

    val nullable: MutableList<String> = mutableListOf()
    val nonNull: MutableList<String> = mutableListOf()

    fun map(sqlType: String, graphqlType: String) {
        sqlTypes[sqlType] = graphqlType
    }

    /** Replaces the mapped base type of matching columns; nullability still follows the column and the overrides below. */
    fun override(columnPattern: String, graphqlType: String) {
        columnTypes[columnPattern] = graphqlType
    }

    fun nullable(vararg patterns: String) {
        nullable += patterns
    }

    fun nonNull(vararg patterns: String) {
        nonNull += patterns
    }
}

class EnumRules {
    /** Emits every enum of the schema instead of only the enums a generated field refers to. */
    var includeUnreferenced: Boolean = false

    val includes: MutableList<String> = mutableListOf()

    /** Enums emitted even when no generated field refers to them. Patterns match the database enum name. */
    fun include(vararg patterns: String) {
        includes += patterns
    }
}

class Generator {
    var outputFile: File? = null

    /** Text written before the first declaration. Null writes the default comment; blank writes nothing. */
    var header: String? = null

    /** Writes table, column and enum comments as GraphQL descriptions. */
    var descriptions: Boolean = true

    val tables: TableRules = TableRules()
    val columns: ColumnRules = ColumnRules()
    val types: TypeRules = TypeRules()
    val enums: EnumRules = EnumRules()

    fun tables(action: TableRules.() -> Unit) = tables.action()

    fun columns(action: ColumnRules.() -> Unit) = columns.action()

    fun types(action: TypeRules.() -> Unit) = types.action()

    fun enums(action: EnumRules.() -> Unit) = enums.action()
}

open class Configuration {
    val jdbc: Jdbc = Jdbc()
    val database: Database = Database()
    val generator: Generator = Generator()

    fun jdbc(action: Jdbc.() -> Unit) = jdbc.action()

    fun database(action: Database.() -> Unit) = database.action()

    fun generator(action: Generator.() -> Unit) = generator.action()
}
