package co.uzzu.dotenv.gradle

import co.uzzu.dotenv.DotEnvParser
import org.gradle.api.Project
import java.nio.charset.Charset
import java.util.Collections

internal class DotEnvResolver(project: Project) {

    private val rootProject: Project
    private val dotenvCache: MutableMap<Project, Map<String, String?>> = mutableMapOf()

    init {
        rootProject = project.rootProject
    }

    fun resolve(project: Project): Map<String, String?> {
        val dotenvList = mutableListOf<Map<String, String?>>()

        var current = project
        while (true) {
            val dotenv = current.dotenv()
            dotenvList.add(0, dotenv)
            if (current == rootProject) {
                break
            }
            if (current.parent == null) {
                break
            }
            current = checkNotNull(current.parent)
        }
        return dotenvList
            .fold(mutableMapOf<String, String?>()) { destination, dotenv -> destination.apply { putAll(dotenv) } }
            .toMap()
    }

    private fun Project.dotenv(): Map<String, String?> {
        require(this@dotenv.rootProject == this@DotEnvResolver.rootProject)
        val config = ConfigurationResolver(this).resolve()
        if (dotenvCache[this] == null) {
            val dotenvTemplate = dotenvTemplate(config)
            val dotenvSource = dotenvSource(config)
            val variables = dotenvTemplate.keys
                .union(dotenvSource.keys)
                .associateWith { dotenvSource[it] }
            dotenvCache[project] = variables
        }
        return checkNotNull(dotenvCache[project])
    }

    private fun Project.dotenvTemplate(config: Configuration): Map<String, String> {
        val filename = config.templateFilename
            .let {
                if (it != DefaultConfiguration.templateFilename) {
                    val templateFile = file(it)
                    if (!templateFile.exists() || !templateFile.canRead()) {
                        return Collections.emptyMap()
                    }
                }
                it
            }
        return readText(filename).let(DotEnvParser::parse)
    }

    private fun Project.dotenvSource(config: Configuration): Map<String, String> {
        val envFilename = config.filename
            .let {
                if (it != DefaultConfiguration.filename) {
                    val envFile = file(it)
                    if (!envFile.exists() || !envFile.canRead()) {
                        return Collections.emptyMap()
                    }
                }
                it
            }
        return readText(envFilename).let(DotEnvParser::parse)
    }

    private fun Project.readText(filename: String): String {
        val file = file(filename)
        return if (file.exists()) {
            file.readText(Charset.forName("UTF-8"))
        } else {
            ""
        }
    }
}
