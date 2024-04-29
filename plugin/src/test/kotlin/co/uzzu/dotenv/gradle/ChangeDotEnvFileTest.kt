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

@Suppress("UnstableApiUsage") // GradleRunner#withPluginClasspath
class ChangeDotEnvFileTest {

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
                println("Result: ${'$'}{env.HOGE.value}")
                """.trimIndent()
            )
            file(
                ".env.template",
                """
                HOGE=
                """.trimIndent()
            )
            file(
                ".env",
                """
                HOGE=100
                """.trimIndent()
            )
            file(
                ".env.staging",
                """
                HOGE=200
                """.trimIndent()
            )
            file(
                "gradle.properties",
                """
                dotenv.filename=.env.staging
                """.trimIndent()
            )
        }

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .build()

        assertThat(result.output).contains("Result: 200")
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
            file(
                ".env.staging",
                """
                FOO=1000
                """.trimIndent()
            )
            directory("sub")
            file(
                "sub/build.gradle",
                """
                println("[sub] BAR: ${'$'}{env.BAR.value}")
                """.trimIndent()
            )
            file(
                "sub/.env.template",
                """
                BAR=
                """.trimIndent()
            )
            file(
                "sub/.env",
                """
                BAR=200
                """.trimIndent()
            )
            file(
                "sub/.env.staging",
                """
                BAR=2000
                """.trimIndent()
            )
            file(
                "sub/gradle.properties",
                """
                dotenv.filename=.env.staging
                """.trimIndent()
            )
        }

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .build()

        assertAll {
            assertThat(result.output).contains("[root] FOO: 100")
            assertThat(result.output).contains("[sub] BAR: 2000")
        }
    }

    @Test
    fun changeFileWithIgnoringParent() {
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
                "env/.env.staging",
                """
                FOO=1000
                """.trimIndent()
            )
            file(
                "gradle.properties",
                """
                dotenv.filename=./env/.env.staging
                """.trimIndent()
            )
            directory("sub")
            file(
                "sub/build.gradle",
                """
                println("[sub] BAR: ${'$'}{env.BAR.value}")
                """.trimIndent()
            )
            file(
                "sub/.env.template",
                """
                BAR=
                """.trimIndent()
            )
            file(
                "sub/.env",
                """
                BAR=200
                """.trimIndent()
            )
        }

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .build()

        assertAll {
            assertThat(result.output).contains("[root] FOO: 1000")
            assertThat(result.output).contains("[sub] BAR: 200")
        }
    }

    @Test
    fun changeFileWithIgnoringParentByUsingCliOption() {
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
            file(
                ".env.local",
                """
                FOO=1000
                """.trimIndent()
            )
            directory("sub")
            file(
                "sub/build.gradle",
                """
                println("[sub] BAR: ${'$'}{env.BAR.value}")
                """.trimIndent()
            )
            file(
                "sub/.env.template",
                """
                BAR=
                """.trimIndent()
            )
            file(
                "sub/.env",
                """
                BAR=200
                """.trimIndent()
            )
        }

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("-Pdotenv.filename=.env.local")
            .build()

        assertAll {
            assertThat(result.output).contains("[root] FOO: 1000")
            assertThat(result.output).contains("[sub] BAR: 200")
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
                "env/.env.staging",
                """
                FOO=1000
                """.trimIndent()
            )
            file(
                "gradle.properties",
                """
                dotenv.filename.ignore.parent=false
                dotenv.filename=./env/.env.staging
                """.trimIndent()
            )
            directory("sub")
            file(
                "sub/build.gradle",
                """
                println("[sub] BAR: ${'$'}{env.BAR.value}")
                """.trimIndent()
            )
            file(
                "sub/.env.template",
                """
                BAR=
                """.trimIndent()
            )
            directory("sub/env")
            file(
                "sub/env/.env.staging",
                """
                BAR=2000
                """.trimIndent()
            )
        }

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .build()

        assertAll {
            assertThat(result.output).contains("[root] FOO: 1000")
            assertThat(result.output).contains("[sub] BAR: 2000")
        }
    }
}
