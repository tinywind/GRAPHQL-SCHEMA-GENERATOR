package org.tinywind.graphqlschemagenerator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GraphqlSchemaGeneratorTest {
    private val generator = GraphqlSchemaGenerator { Fixtures.schema }

    @Test
    fun `generate writes the SDL and creates parent directories`(@TempDir directory: File) {
        val configuration = Fixtures.configuration { outputFile = File(directory, "graphql/generated.graphqls") }
        val result = generator.generate(configuration)
        assertEquals(configuration.generator.outputFile, result.file)
        assertEquals(4, result.typeCount)
        assertEquals(2, result.enumCount)
        assertEquals(Fixtures.expectedSdl, result.file.readText())
        assertEquals(Fixtures.expectedSdl, generator.render(configuration))
    }

    @Test
    fun `verify reports a matching, missing or changed file`(@TempDir directory: File) {
        val configuration = Fixtures.configuration { outputFile = File(directory, "generated.graphqls") }
        assertInstanceOf(Verification.Missing::class.java, generator.verify(configuration))

        generator.generate(configuration)
        assertInstanceOf(Verification.Match::class.java, generator.verify(configuration))

        val file = configuration.generator.outputFile!!
        file.writeText(file.readText().replace("cohortNumber: Int", "cohortNumber: Int!"))
        val changed = assertInstanceOf(Verification.Different::class.java, generator.verify(configuration))
        assertEquals(22, changed.line)
        assertEquals("    cohortNumber: Int", changed.expected)
        assertEquals("    cohortNumber: Int!", changed.actual)

        file.writeText(Fixtures.expectedSdl + "extra\n")
        val longer = assertInstanceOf(Verification.Different::class.java, generator.verify(configuration))
        assertEquals(Fixtures.expectedSdl.lines().size, longer.line)
        assertEquals("extra", longer.actual)
    }

    @Test
    fun `generate and verify require an output file`() {
        val message = assertThrows(GenerationException::class.java) { generator.generate(Fixtures.configuration()) }.message!!
        assertEquals("generator.outputFile is required", message)
        assertThrows(GenerationException::class.java) { generator.verify(Fixtures.configuration()) }
    }
}
