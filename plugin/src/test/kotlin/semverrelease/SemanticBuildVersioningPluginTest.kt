package semverrelease

import com.alphasystem.gradle.semver.release.common.TestRepository
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class SemanticBuildVersioningPluginTest {

    @Test
    fun `settings plugin propagates computed version to all projects`() {
        val projectDir = Files.createTempDirectory("settings-plugin-test").toFile()
        val repository = TestRepository(projectDir)
        try {
            repository.makeChanges().commit("initial commit")

            projectDir.resolve("settings.gradle.kts").writeText(
                """
                plugins {
                    id("io.github.sfali23.gradle-semantic-versioning-release")
                }

                semverrelease {
                    addUnReleasedCommitsToTagComment.set(false)
                }

                rootProject.name = "root"
                include(":sub")
                """.trimIndent(),
            )

            projectDir.resolve("build.gradle.kts").writeText(
                """
                tasks.register("printRootVersion") {
                    doLast { println("ROOT_VERSION=${'$'}{project.version}") }
                }
                """.trimIndent(),
            )

            projectDir.resolve("sub").mkdirs()
            projectDir.resolve("sub/build.gradle.kts").writeText(
                """
                tasks.register("printSubVersion") {
                    doLast { println("SUB_VERSION=${'$'}{project.version}") }
                }
                """.trimIndent(),
            )

            val result =
                GradleRunner
                    .create()
                    .withProjectDir(projectDir)
                    .withPluginClasspath()
                    .withArguments("printRootVersion", "sub:printSubVersion", "--quiet")
                    .build()

            val output = result.output
            assertTrue(output.contains("ROOT_VERSION=0.1.0"), "Expected root version 0.1.0 but output was:\n$output")
            assertTrue(output.contains("SUB_VERSION=0.1.0"), "Expected subproject version 0.1.0 but output was:\n$output")
        } finally {
            repository.close()
            projectDir.deleteRecursively()
        }
    }
}
