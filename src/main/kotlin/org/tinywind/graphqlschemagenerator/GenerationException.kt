package org.tinywind.graphqlschemagenerator

/** A configuration or schema condition that stops generation. The message names the object and the rule to change. */
class GenerationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
