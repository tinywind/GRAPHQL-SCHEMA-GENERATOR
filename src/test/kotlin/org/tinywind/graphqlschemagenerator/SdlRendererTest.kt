package org.tinywind.graphqlschemagenerator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SdlRendererTest {

    @Test
    fun `renders the fixture schema as the expected SDL`() {
        val document = SdlModelBuilder(Fixtures.configuration().generator).build(Fixtures.schema)
        assertEquals(Fixtures.expectedSdl, SdlRenderer.render(document))
    }

    @Test
    fun `header lines become comments and descriptions are escaped`() {
        val document = SdlDocument(
            header = "Generated\n\n# already a comment",
            enums = emptyList(),
            types = listOf(
                SdlType(
                    "Thing",
                    listOf(
                        SdlField("path", "String", true, "C:\\temp"),
                        SdlField("quote", "String", false, "Says \"\"\"hi\"\"\""),
                        SdlField("lines", "[Int!]", false, "first\nsecond\n"),
                    ),
                    "A thing.",
                ),
            ),
        )
        val expected = """
            # Generated

            # already a comment

            "A thing."
            type Thing {
                "C:\\temp"
                path: String!
                ""${'"'}
                Says \""${'"'}hi\""${'"'}
                ""${'"'}
                quote: String
                ""${'"'}
                first
                second
                ""${'"'}
                lines: [Int!]
            }

        """.trimIndent()
        assertEquals(expected, SdlRenderer.render(document))
    }

    @Test
    fun `renders without a header`() {
        val document = SdlDocument(null, listOf(SdlEnum("Mood", listOf("GOOD"), null)), emptyList())
        assertEquals("enum Mood {\n    GOOD\n}\n", SdlRenderer.render(document))
    }
}
