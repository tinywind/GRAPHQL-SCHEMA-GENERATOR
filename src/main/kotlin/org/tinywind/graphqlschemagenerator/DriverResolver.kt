package org.tinywind.graphqlschemagenerator

import java.io.File
import java.net.URLClassLoader
import java.sql.Driver
import java.util.ServiceLoader

/** Supplies the JDBC driver for the configured URL. */
fun interface DriverResolver {
    fun resolve(jdbc: Jdbc, url: String): Driver
}

/** Loads the driver from a separate classpath, such as a Gradle configuration holding the driver artifact. */
class ClasspathDriverResolver(private val classpath: Collection<File>) : DriverResolver {
    override fun resolve(jdbc: Jdbc, url: String): Driver {
        val loader = URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), javaClass.classLoader)
        val driverClass = jdbc.driverClass?.takeIf { it.isNotBlank() }
        if (driverClass != null) {
            val loaded = try {
                loader.loadClass(driverClass)
            } catch (e: ClassNotFoundException) {
                throw GenerationException("jdbc.driverClass '$driverClass' is not on the driver classpath; add the driver artifact to the graphqlSchema configuration")
            }
            if (!Driver::class.java.isAssignableFrom(loaded)) throw GenerationException("jdbc.driverClass '$driverClass' does not implement java.sql.Driver")
            return loaded.asSubclass(Driver::class.java).getDeclaredConstructor().newInstance()
        }
        return ServiceLoader.load(Driver::class.java, loader).firstOrNull { it.acceptsURL(url) }
            ?: throw GenerationException("No JDBC driver on the driver classpath accepts '${sanitize(url)}'; add the driver artifact or set jdbc.driverClass")
    }
}

/** Removes the query string, where JDBC URLs may carry credentials, before a URL appears in a message. */
internal fun sanitize(url: String): String = url.substringBefore('?')
