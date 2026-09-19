package org.tinywind.graphqlschemagenerator

data class SdlDocument(
    val header: String?,
    val enums: List<SdlEnum>,
    val types: List<SdlType>,
)

data class SdlEnum(
    val name: String,
    val values: List<String>,
    val description: String?,
)

data class SdlType(
    val name: String,
    val fields: List<SdlField>,
    val description: String?,
)

data class SdlField(
    val name: String,
    val type: String,
    val nonNull: Boolean,
    val description: String?,
)
