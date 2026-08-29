import org.gradle.api.Project

/**
 * Helper function to retrieve configuration variable values
 */
fun Project.getProperty(name: String): String? {
	// sample.var --> SAMPLE_VAR
	val environmentName = name.uppercase().replace(".", "_")

	return findProperty(name)?.toString()?.takeIf { it.isNotBlank() }
		?: System.getenv(environmentName)?.takeIf { it.isNotBlank() }
}
