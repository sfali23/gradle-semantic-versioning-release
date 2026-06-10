package semverrelease.tasks

import com.alphasystem.gradle.semver.release.common.JGitAdapter
import com.alphasystem.gradle.semver.release.internal.SemanticBuildVersion
import com.alphasystem.gradle.semver.release.internal.SemanticBuildVersionConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import semverrelease.ANSI_GREEN
import semverrelease.ANSI_RESET
import semverrelease.RELEASE_GROUP

/**
 * An abstract task implementation for creating a Git tag in a project repository
 * based on the semantic versioning configuration and project state.
 *
 * The task is part of the release process and encapsulates logic to determine the tag
 * name, tag message, and whether any additional commit information should be appended to
 * the tag message.
 *
 * This task works in conjunction with `SemanticBuildVersionConfiguration` to manage
 * versioning and tagging conventions.
 *
 * The created tag will adhere to the tag prefix and versioning rules defined in the
 * semantic version configuration.
 */
@UntrackedTask(because = "Git tag creation involves external state and should not be cached")
abstract class CreateTagTask : DefaultTask() {

    @get:Internal
    abstract val releaseTagComment: Property<String>

    @get:Internal
    abstract val addUnReleasedCommitsToTagComment: Property<Boolean>

    @get:Internal
    abstract val config: Property<SemanticBuildVersionConfiguration>

    @get:Internal
    abstract val workingDirectory: RegularFileProperty

    init {
        group = RELEASE_GROUP
        description = "Create a tag"
    }

    @TaskAction
    fun createTag() {
        val workingDir = workingDirectory.get().asFile
        val baseConfig = config.get()
        val semanticBuildVersion = SemanticBuildVersion(workingDir, baseConfig)
        val tagPrefix = baseConfig.tagPrefix
        val version = project.version.toString()
        val message = getTagComment(semanticBuildVersion)
        val tag = "$tagPrefix$version"
        println("${ANSI_GREEN}Creating tag: $tag$ANSI_RESET")
        JGitAdapter(workingDir).createTag(tag, message.isNotBlank(), message)
    }

    private fun getTagComment(semanticBuildVersion: SemanticBuildVersion): String {
        val releaseTagComment = releaseTagComment.get()

        val unreleasedCommits =
            if (addUnReleasedCommitsToTagComment.get()) semanticBuildVersion.getUnReleasedCommits()
            else listOf()
        val defaultComment = if (releaseTagComment.isNotBlank()) "$releaseTagComment: ${project.version}" else ""

        return listOf(defaultComment, *unreleasedCommits.toTypedArray()).joinToString(System.lineSeparator())
    }
}
