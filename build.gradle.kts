plugins {
    kotlin("jvm") version "2.4.10"
    `java-gradle-plugin`
    jacoco
}

group = "org.tinywind"
version = "0.1.0"

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

kotlin {
    jvmToolchain(21)
}

gradlePlugin {
    plugins {
        create("graphqlSchemaGenerator") {
            id = "org.tinywind.graphql-schema-generator"
            implementationClass = "org.tinywind.GraphqlSchemaGeneratorPlugin"
            displayName = "GraphQL schema generator plugin"
            description = "Generates GraphQL SDL types and enums from a database schema through jOOQ meta"
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
