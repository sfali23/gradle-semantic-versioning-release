package semverrelease

import com.alphasystem.gradle.semver.release.internal.SemanticBuildVersionConfiguration
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import semverrelease.tasks.SetReleaseVersionTask
import semverrelease.tasks.PushTagTask
import semverrelease.tasks.CreateTagTask

abstract class SemanticBuildVersioningPlugin : Plugin<Project> {

    private var config = SemanticBuildVersionConfiguration()

    override fun apply(project: Project) {
        // ensure the base plugin is applied
        if (!project.plugins.hasPlugin(BasePlugin::class.java)) {
            project.plugins.apply(BasePlugin::class.java)
        }

        val extension = project.extensions.create("semverrelease", SemanticBuildVersioningExtension::class.java, project)

        project.afterEvaluate {
            buildConfig(extension)
        }

        val setReleaseVersion = project.tasks.register("setReleaseVersion", SetReleaseVersionTask::class.java) {
            it.config.set(config)
            it.workingDirectory.set(project.projectDir)
        }

        project.tasks.register("printVersion") { it ->
            it.group = RELEASE_GROUP
            it.description = "Print the current version"
            it.dependsOn(setReleaseVersion)
            it.doLast {
                println("Projected version is: $ANSI_GREEN${it.project.version}$ANSI_RESET")
            }
        }

        project.tasks.register("createTag", CreateTagTask::class.java) { it ->
            it.config.set(config)
            it.releaseTagComment.set(extension.releaseTagComment)
            it.addUnReleasedCommitsToTagComment.set(extension.addUnReleasedCommitsToTagComment)
            it.workingDirectory.set(project.projectDir)
        }

        project.tasks.register("pushTag", PushTagTask::class.java) {
            it.workingDirectory.set(project.projectDir)
        }
    }

    private fun buildConfig(extension: SemanticBuildVersioningExtension) {
        if (extension.startingVersion.isPresent) {
            config = config.copy(startingVersion = extension.startingVersion.get())
        }
        if (extension.tagPrefix.isPresent) {
            config = config.copy(tagPrefix = extension.tagPrefix.get())
        }
        if (extension.forceBump.isPresent) {
            config = config.copy(forceBump = extension.forceBump.get())
        }
        if (extension.newPreRelease.isPresent) {
            config = config.copy(newPreRelease = extension.newPreRelease.get())
        }
        if (extension.promoteToRelease.isPresent) {
            config = config.copy(promoteToRelease = extension.promoteToRelease.get())
        }
        if (extension.snapshot.isPresent) {
            config = config.copy(snapshot = extension.snapshot.get())
        }
        if (extension.defaultBumpLevel.isPresent) {
            config = config.copy(defaultBumpLevel = extension.defaultBumpLevel.get())
        }
        if (extension.componentToBump.isPresent) {
            config = config.copy(componentToBump = extension.componentToBump.get())
        }
        if (extension.snapshotConfig.isPresent) {
            config = config.copy(snapshotConfig = extension.snapshotConfig.get())
        }
        if (extension.preReleaseConfig.isPresent) {
            config = config.copy(preReleaseConfig = extension.preReleaseConfig.get())
        }
        if (extension.hotfixBranchPattern.isPresent) {
            config = config.copy(hotfixBranchPattern = extension.hotfixBranchPattern.get())
        }
        if (extension.extraReleaseBranches.isPresent) {
            config = config.copy(extraReleaseBranches = extension.extraReleaseBranches.get())
        }
    }
}
