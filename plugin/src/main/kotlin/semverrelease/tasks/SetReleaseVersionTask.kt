package semverrelease.tasks

import com.alphasystem.gradle.semver.release.internal.SemanticBuildVersion
import com.alphasystem.gradle.semver.release.internal.SemanticBuildVersionConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import semverrelease.RELEASE_GROUP

/**
 * A task that determines the next release version of the project based on the provided configuration
 * and the state of the repository. The determined version is assigned to the `project.version` property.
 *
 * This task uses `SemanticBuildVersion` to calculate the version by analyzing the working directory
 * and the configuration of semantic versioning parameters.
 *
 * Tasks details:
 * - Group: `RELEASE_GROUP`
 * - Description: "Determine next the version"
 *
 * Properties:
 * - `config`: A property that holds the semantic versioning configuration, specifying the rules and parameters
 *   used to determine the next version. The configuration includes options for version bumping, release branches,
 *   snapshot setup, and other versioning-specific settings.
 * - `workingDirectory`: A property that specifies the working directory used by the task to analyze the state
 *   of the repository for version determination.
 *
 * Task action:
 * The `determineVersion` method calculates the next release version by constructing a `SemanticBuildVersion`
 * object with the provided `workingDirectory` and `config`. The calculated version is then assigned to `project.version`.
 */
@UntrackedTask(because = "Version determination involves external Git state and should not be cached")
abstract class SetReleaseVersionTask : DefaultTask() {

    init {
        group = RELEASE_GROUP
        description = "Determine next the version"
    }

    @get:Internal
    abstract val config: Property<SemanticBuildVersionConfiguration>

    @get:Internal
    abstract val workingDirectory: RegularFileProperty

    @TaskAction
    fun determineVersion() {
        val determineVersion = SemanticBuildVersion(workingDirectory.get().asFile, config.get()).determineVersion()
        project.version = determineVersion
    }
}
