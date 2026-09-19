package org.tinywind.graphqlschemagenerator

/** Writes a document as GraphQL SDL with four-space indentation and one blank line between declarations. */
object SdlRenderer {
    private const val INDENT = "    "

    fun render(document: SdlDocument): String = buildString {
        document.header?.let { append(comment(it)).append("\n\n") }
        val declarations = document.enums.map { renderEnum(it) } + document.types.map { renderType(it) }
        append(declarations.joinToString("\n\n"))
        append("\n")
    }

    private fun comment(header: String): String =
        header.trimEnd().lines().joinToString("\n") { line -> if (line.isBlank() || line.startsWith("#")) line else "# $line" }

    private fun renderEnum(enum: SdlEnum): String = buildString {
        description(enum.description, "")
        append("enum ${enum.name} {\n")
        enum.values.forEach { append(INDENT).append(it).append("\n") }
        append("}")
    }

    private fun renderType(type: SdlType): String = buildString {
        description(type.description, "")
        append("type ${type.name} {\n")
        type.fields.forEach { field ->
            description(field.description, INDENT)
            append(INDENT).append(field.name).append(": ").append(field.type)
            if (field.nonNull) append("!")
            append("\n")
        }
        append("}")
    }

    private fun StringBuilder.description(text: String?, indent: String) {
        if (text.isNullOrBlank()) return
        if ('\n' in text || '"' in text) {
            append(indent).append("\"\"\"\n")
            text.trimEnd().lines().forEach { append(indent).append(it.replace("\"\"\"", "\\\"\"\"")).append("\n") }
            append(indent).append("\"\"\"\n")
        } else {
            append(indent).append('"').append(text.trim().replace("\\", "\\\\")).append("\"\n")
        }
    }
}
