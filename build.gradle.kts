import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.10"
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "2.0.0"
    jacoco
}

group = "org.tinywind"
version = "0.1.0"

// jOOQ 3.21 needs Java 21, so Gradle itself must run on JDK 21 or later. gradle/gradle-daemon-jvm.properties selects
// such a daemon for this build; a consumer only needs its own Gradle to run on JDK 21 or later.
check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_21)) {
    "GRAPHQL-SCHEMA-GENERATOR needs Gradle to run on JDK 21 or later because jOOQ 3.21 requires it; the current JVM is ${JavaVersion.current()}."
}

repositories {
    mavenCentral()
}

val postgresqlDriverVersion = "42.7.13"

dependencies {
    implementation(gradleApi())
    implementation("org.jooq:jooq-meta:3.21.8")

    testImplementation(gradleTestKit())
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
    testImplementation("org.postgresql:postgresql:$postgresqlDriverVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

gradlePlugin {
    website.set("https://github.com/tinywind/GRAPHQL-SCHEMA-GENERATOR")
    vcsUrl.set("https://github.com/tinywind/GRAPHQL-SCHEMA-GENERATOR")
    plugins {
        create("graphqlSchemaGenerator") {
            id = "org.tinywind.graphql-schema-generator"
            implementationClass = "org.tinywind.GraphqlSchemaGeneratorPlugin"
            displayName = "GraphQL schema generator plugin"
            description = "Generates GraphQL SDL types and enums from a database schema through jOOQ meta"
            tags.set(listOf("graphql", "jooq", "database", "schema", "codegen"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
    // The functional test's consumer build resolves the driver by these coordinates.
    systemProperty("postgresql.driver.coordinates", "org.postgresql:postgresql:$postgresqlDriverVersion")
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
