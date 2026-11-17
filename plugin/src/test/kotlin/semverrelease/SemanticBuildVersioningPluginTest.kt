package semverrelease

import com.alphasystem.gradle.semver.release.common.TestRepository
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
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

            repository.add(".").commit("add build files")
            repository.makeChanges()
            val tagsBeforeBuild = repository.getAdapter().getGit().tagList().call().map { it.name }

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
            assertEquals(tagsBeforeBuild, repository.getAdapter().getGit().tagList().call().map { it.name })

            val failedRelease = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("createTag", "--quiet")
                .buildAndFail()
            assertTrue(failedRelease.output.contains("Cannot create tag, there are uncommitted changes."))
            assertEquals(tagsBeforeBuild, repository.getAdapter().getGit().tagList().call().map { it.name })

            repository.commit("commit pending changes")
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("createTag", "--quiet")
                .build()
            assertTrue(repository.getAdapter().getGit().tagList().call().any { it.name == "refs/tags/v0.1.0" })
        } finally {
            repository.close()
            projectDir.deleteRecursively()
        }
    }
}
