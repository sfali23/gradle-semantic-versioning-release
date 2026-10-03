package semverrelease

import com.alphasystem.gradle.semver.release.internal.SemanticBuildVersion
import com.alphasystem.gradle.semver.release.internal.SemanticBuildVersionConfiguration
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.BasePlugin
import semverrelease.tasks.CreateTagTask
import semverrelease.tasks.PushTagTask

abstract class SemanticBuildVersioningPlugin : Plugin<Settings> {

    override fun apply(settings: Settings) {
        val extension =
            settings.extensions.create(
                "semverrelease",
                SemanticBuildVersioningExtension::class.java,
            )
        extension.releaseTagComment.convention("Releasing")
        extension.addUnReleasedCommitsToTagComment.convention(false)

        settings.gradle.projectsLoaded { gradle ->
            val config = buildConfig(extension)
            val version = SemanticBuildVersion(settings.rootDir, config).determineVersion()
            gradle.allprojects { it.version = version }
        }

        settings.gradle.rootProject { rootProject ->
            if (!rootProject.plugins.hasPlugin(BasePlugin::class.java)) {
                rootProject.plugins.apply(BasePlugin::class.java)
            }

            rootProject.tasks.register("printVersion") {
                it.group = RELEASE_GROUP
                it.description = "Print the current version"
                it.doLast {
                    println("Project version is: $ANSI_GREEN${it.project.version}$ANSI_RESET")
                }
            }

            rootProject.tasks.register("createTag", CreateTagTask::class.java) { it ->
                it.config.set(buildConfig(extension))
                it.releaseTagComment.set(extension.releaseTagComment)
                it.addUnReleasedCommitsToTagComment.set(extension.addUnReleasedCommitsToTagComment)
                it.workingDirectory.set(rootProject.projectDir)
            }

            rootProject.tasks.register("pushTag", PushTagTask::class.java) {
                it.workingDirectory.set(rootProject.projectDir)
            }
        }
    }

    private fun buildConfig(extension: SemanticBuildVersioningExtension): SemanticBuildVersionConfiguration {
        var config = SemanticBuildVersionConfiguration()
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
        if (extension.autoBump.isPresent) {
            config = config.copy(autoBump = extension.autoBump.get())
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
        return config
    }
}
