package semverrelease.tasks

import com.alphasystem.gradle.semver.release.common.JGitAdapter
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
 * Represents a Gradle task for pushing Git tags corresponding to the project's version to a remote repository.
 * This task integrates with JGit for Git operations and uses a configuration object to determine
 * the tag prefix and other build-related settings.
 *
 * The task is part of the release automation process and ensures that version tags are consistently pushed
 * to the remote repository based on the current project version and configured semantics.
 *
 * Properties:
 * - `workingDirectory`: The directory where the Git repository is located. This property is used to initialize
 *   the JGit adapter for performing Git operations.
 * - `config`: A configuration object that provides semantic versioning and build details, such as the
 *   tag prefix and additional options that influence versioning decisions.
 *
 * Task Behavior:
 * - On execution, the task retrieves the working directory and configuration properties.
 * - Constructs the tag name by combining the configured tag prefix with the current project version.
 * - Prints a log message showing the tag being pushed for better traceability during the build process.
 * - Uses the JGit adapter to push the constructed tag to the remote repository.
 */
@UntrackedTask(because = "Git tag pushing involves external state and should not be cached")
abstract class PushTagTask: DefaultTask() {

    @get:Internal
    abstract val workingDirectory: RegularFileProperty

    @get:Internal
    abstract val config: Property<SemanticBuildVersionConfiguration>

    init {
        group = RELEASE_GROUP
        description = "Push changes to remote"
    }

    @TaskAction
    fun pushChanges() {
        val workingDir = workingDirectory.get().asFile
        val baseConfig = config.get()
        val tag = "${baseConfig.tagPrefix}${project.version}"
        println("${ANSI_GREEN}Pushing tag: $tag$ANSI_RESET")
        JGitAdapter(workingDir).pushTag(tag)
    }
}
