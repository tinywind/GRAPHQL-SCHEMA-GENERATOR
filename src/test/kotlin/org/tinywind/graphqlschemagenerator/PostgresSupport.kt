package org.tinywind.graphqlschemagenerator

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.DriverManager

/** A PostgreSQL container loaded with `fixture.sql`; tests that need it are skipped when Docker is not reachable. */
class PostgresSupport {
    private val container = PostgreSQLContainer("postgres:18-alpine").withPassword("pw-9f3a1c")

    val jdbcUrl: String get() = container.jdbcUrl
    val username: String get() = container.username
    val password: String get() = container.password

    fun start() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable, "Docker is not reachable; the PostgreSQL tests are skipped")
        container.start()
        val ddl = javaClass.getResource("/fixture.sql")!!.readText()
        DriverManager.getConnection(jdbcUrl, username, password).use { connection ->
            connection.createStatement().use { it.execute(ddl) }
        }
    }

    fun stop() {
        if (container.isRunning) container.stop()
    }

    fun configuration(configure: Generator.() -> Unit = {}): Configuration = Fixtures.configuration(configure).apply {
        database {
            url = jdbcUrl
            user = username
            password = this@PostgresSupport.password
            inputSchema = "public"
        }
    }

    companion object {
        /** Resolves the driver from the test classpath instead of a Gradle configuration. */
        val testClasspathDriver = DriverResolver { _, _ -> org.postgresql.Driver() }
    }
}
