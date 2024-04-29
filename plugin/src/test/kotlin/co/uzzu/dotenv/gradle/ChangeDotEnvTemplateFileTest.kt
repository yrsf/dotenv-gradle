package co.uzzu.dotenv.gradle

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import assertk.assertions.messageContains
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ChangeDotEnvTemplateFileTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun changeFileByGradleProperties() {
        RootProject(projectDir) {
            settingsGradle()
            buildGradle(
                """
                plugins {
                    base
                    id("co.uzzu.dotenv.gradle")
                }
                println("PIYO is present: ${'$'}{env.PIYO.isPresent}")
                """.trimIndent()
            )
            file(
                ".env.example",
                """
                PIYO=
                """.trimIndent()
            )
            file(
                "gradle.properties",
                """
                dotenv.template.filename=.env.example
                """.trimIndent()
            )
        }

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .build()

        assertThat(result.output).contains("PIYO is present: false")
    }

    @Test
    fun changeFileOnlySubProject() {
        RootProject(projectDir) {
            settingsGradle(
                """
                include("sub")
                """.trimIndent()
            )
            buildGradle(
                """
                plugins {
                    base
                    id("co.uzzu.dotenv.gradle")
                }
                println("[root] FOO: ${'$'}{env.FOO.value}")
                """.trimIndent()
            )
            file(
                ".env.template",
                """
                FOO=
                """.trimIndent()
            )
            file(
                ".env",
                """
                FOO=100
                """.trimIndent()
            )
            directory("sub")
            file(
                "sub/build.gradle",
                """
                println("[sub] BAR: ${'$'}{env.BAR.orNull()}")
                println("[sub] BAZ: ${'$'}{env.BAZ.value}")
                """.trimIndent()
            )
            file(
                "sub/.env.example",
                """
                BAR=
                """.trimIndent()
            )
            file(
                "sub/.env",
                """
                BAZ=200
                """.trimIndent()
            )
            file(
                "sub/gradle.properties",
                """
                dotenv.template.filename=.env.example
                """.trimIndent()
            )
        }

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .build()

        assertAll {
            assertThat(result.output).contains("[root] FOO: 100")
            assertThat(result.output).contains("[sub] BAR: null")
            assertThat(result.output).contains("[sub] BAZ: 200")
        }
    }

    @Test
    fun changeFileWithIgnoreParent() {
        RootProject(projectDir) {
            settingsGradle(
                """
                include("sub")
                """.trimIndent()
            )
            buildGradle(
                """
                plugins {
                    base
                    id("co.uzzu.dotenv.gradle")
                }
                println("[root] FOO: ${'$'}{env.FOO.orNull()}")
                """.trimIndent()
            )
            file(
                ".env.example",
                """
                FOO=
                """.trimIndent()
            )
            file(
                "gradle.properties",
                """
                dotenv.template.filename=.env.example
                """.trimIndent()
            )
            directory("sub")
            file(
                "sub/build.gradle",
                """
                println("[sub] BAR: ${'$'}{env.BAR.orNull()}")
                """.trimIndent()
            )
            file(
                "sub/.env.template",
                """
                BAR=
                """.trimIndent()
            )
        }

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .build()

        assertAll {
            assertThat(result.output).contains("[root] FOO: null")
            assertThat(result.output).contains("[sub] BAR: null")
        }
    }

    @Test
    fun changeFileWithoutIgnoringParent() {
        RootProject(projectDir) {
            settingsGradle(
                """
                include("sub")
                """.trimIndent()
            )
            buildGradle(
                """
                plugins {
                    base
                    id("co.uzzu.dotenv.gradle")
                }
                println("[root] FOO: ${'$'}{env.FOO.orNull()}")
                """.trimIndent()
            )
            file(
                ".env.example",
                """
                FOO=
                """.trimIndent()
            )
            file(
                "gradle.properties",
                """
                dotenv.template.filename.ignore.parent=false
                dotenv.template.filename=.env.example
                """.trimIndent()
            )
            directory("sub")
            file(
                "sub/build.gradle",
                """
                println("[sub] BAR: ${'$'}{env.BAR.orNull()}")
                """.trimIndent()
            )
            file(
                "sub/.env.example",
                """
                BAR=
                """.trimIndent()
            )
        }

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .build()

        assertAll {
            assertThat(result.output).contains("[root] FOO: null")
            assertThat(result.output).contains("[sub] BAR: null")
        }
    }
}
