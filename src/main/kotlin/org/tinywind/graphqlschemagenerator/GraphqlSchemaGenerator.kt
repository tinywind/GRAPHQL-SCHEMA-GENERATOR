package org.tinywind.graphqlschemagenerator

import java.io.File

data class GeneratedSchema(val file: File, val typeCount: Int, val enumCount: Int)

sealed interface Verification {
    val file: File

    data class Match(override val file: File) : Verification

    data class Missing(override val file: File) : Verification

    data class Different(override val file: File, val line: Int, val expected: String?, val actual: String?) : Verification
}

/** Reads the schema, applies the rules and writes or checks the SDL file. */
class GraphqlSchemaGenerator(private val reader: (Configuration) -> SchemaModel) {

    constructor(driverResolver: DriverResolver) : this({ MetadataReader(driverResolver).read(it) })

    fun build(configuration: Configuration): SdlDocument = SdlModelBuilder(configuration.generator).build(reader(configuration))

    fun render(configuration: Configuration): String = SdlRenderer.render(build(configuration))

    fun generate(configuration: Configuration): GeneratedSchema {
        val file = outputFile(configuration)
        val document = build(configuration)
        file.absoluteFile.parentFile?.mkdirs()
        file.writeText(SdlRenderer.render(document))
        return GeneratedSchema(file, document.types.size, document.enums.size)
    }

    fun verify(configuration: Configuration): Verification {
        val file = outputFile(configuration)
        val expected = render(configuration)
        if (!file.isFile) return Verification.Missing(file)
        val actual = file.readText()
        if (actual == expected) return Verification.Match(file)
        val expectedLines = expected.lines()
        val actualLines = actual.lines()
        val index = (0 until maxOf(expectedLines.size, actualLines.size)).first { expectedLines.getOrNull(it) != actualLines.getOrNull(it) }
        return Verification.Different(file, index + 1, expectedLines.getOrNull(index), actualLines.getOrNull(index))
    }

    private fun outputFile(configuration: Configuration): File =
        configuration.generator.outputFile ?: throw GenerationException("generator.outputFile is required")
}
