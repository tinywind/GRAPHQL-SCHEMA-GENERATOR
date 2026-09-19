plugins {
    // Lets the wrapper provision the JDK named in gradle/gradle-daemon-jvm.properties on a machine without it.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "graphql-schema-generator"
