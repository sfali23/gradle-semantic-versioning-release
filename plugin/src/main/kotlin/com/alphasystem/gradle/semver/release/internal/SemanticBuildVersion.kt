package com.alphasystem.gradle.semver.release.internal

import com.alphasystem.gradle.semver.release.common.JGitAdapter
import org.slf4j.LoggerFactory
import semverrelease.nonEmpty

import java.io.File

class SemanticBuildVersion(workingDir: File, val baseConfig: SemanticBuildVersionConfiguration) {

    private val logger = LoggerFactory.getLogger(SemanticBuildVersion::class.java)
    private val adapter: JGitAdapter = JGitAdapter(workingDir)
    private val preReleaseConfig = baseConfig.preReleaseConfig
    private val snapshotConfig = baseConfig.snapshotConfig
    private val snapshotSuffix = snapshotConfig.prefix
    private val tagPrefix = baseConfig.tagPrefix
    private val startingVersion = Version.create(baseConfig.startingVersion, snapshotSuffix, preReleaseConfig)

    fun latestVersion(): Version? {
        return adapter.getTagsForCurrentBranch()
            .filter { it.startsWith(tagPrefix) }
            .map { it.replace(tagPrefix, "") }
            .mapNotNull { version -> runCatching { Version.create(version, snapshotSuffix, preReleaseConfig) }.getOrNull() }
            .sortedWith(Version.VERSION_COMPARATOR).getOrNull(0)
    }

    fun getUnReleasedCommits(): List<String> {
        val version = (latestVersion() ?: startingVersion).toStringValue()
        return adapter.getUnReleasedCommits("$tagPrefix$version")
    }

    fun determineVersion(): String {
        if (adapter.hasUncommittedChanges()) {
            throw IllegalArgumentException("Cannot determine next version, there are uncommitted changes.")
        }
        val currentBranch = adapter.getCurrentBranch()
        val hotfixRequired = baseConfig.hotfixBranchPattern.nonEmpty(currentBranch)
        val snapshotRequired = snapshotRequired(currentBranch, hotfixRequired)
        val maybeLatestVersion = latestVersion()
        val currentVersion = maybeLatestVersion ?: startingVersion
        val newVersion = determineVersion(currentVersion, hotfixRequired, snapshotRequired, maybeLatestVersion)
        if (currentVersion == newVersion && maybeLatestVersion != null) {
            throw IllegalArgumentException("Couldn't determine next version, tag (${newVersion.toStringValue()}) is already exists.")
        }
        return newVersion.toStringValue()
    }

    private fun snapshotRequired(currentBranch: String, hotfixRequired: Boolean): Boolean {
        val notAReleaseBranch = !baseConfig.isReleaseBranch(currentBranch)
        val snapshotFlag = baseConfig.snapshot
        if (notAReleaseBranch && !hotfixRequired) {
            logger.warn(
                "Current configuration doesn't allow to create new tag from current branch ({}), creating snapshot version",
                currentBranch
            )
        }
        if (snapshotFlag) {
            logger.warn("Snapshot flag is set to true, creating snapshot version")
        }
        return (notAReleaseBranch || snapshotFlag) && !hotfixRequired
    }

    private fun determineVersion(
        currentVersion: Version,
        hotfixRequired: Boolean,
        snapshotRequired: Boolean,
        maybeLatestVersion: Version? = null
    ): Version {
        if (maybeLatestVersion == null) {
            return if (snapshotRequired) startingVersion.bumpSnapshot(getSnapshotInfo()) else startingVersion
        } else if (baseConfig.forceBump) {
            val versionComponents =
                SetupVersionComponentsForBump()
                    .addComponentIfRequired(
                        baseConfig.toVersionComponentToBump(),
                        addDefaultComponent(baseConfig.toVersionComponentToBump())
                    )
                    .addComponentIfRequired(VersionComponent.PROMOTE_TO_RELEASE) { baseConfig.promoteToRelease }
                    .addComponentIfRequired(VersionComponent.NEW_PRE_RELEASE) { baseConfig.newPreRelease }

            return bumpVersion(
                forcePush = false,
                currentVersion = currentVersion,
                hotfixRequired = hotfixRequired,
                snapshotRequired = snapshotRequired,
                versionComponents = versionComponents
            )
        } else {
            val tuple = maybeLatestVersion.let { version ->
                // if last tag exists then get commits between last tag and current head, otherwise get all commits
                val commits = runCatching { adapter.getCommitBetween("$tagPrefix${version.toStringValue()}") }.getOrDefault(listOf())
                Tuple(!commits.isEmpty(), commits)
            }

            val versionComponents =
                tuple.commitMessages.fold(SetupVersionComponentsForBump()) { versionComponent, commitMessage ->
                    versionComponent.parseMessage(commitMessage, baseConfig.autoBump)
                }

            // if we have auto bump enabled, there is(are) previous tag(s), and if there are commits added between last tag
            // and current head, then if components to bump are empty at the end of process, then bump default configured bump level
            val forcePush = baseConfig.isAutoBumpEnabled() && (tuple.hasCommits || !versionComponents.hasEssentialComponents())

            return bumpVersion(forcePush, currentVersion, hotfixRequired, snapshotRequired, versionComponents)
        }
    }

    /**
     * Bumps the version of the current build based on specified conditions and version components.
     *
     * @param forcePush A flag indicating whether to force the version bump, even in cases where the default bump level needs to be used.
     * @param currentVersion The current version of the build that needs to be bumped.
     * @param hotfixRequired A flag indicating whether a hotfix bump is required.
     * @param snapshotRequired A flag indicating whether the next version should include a snapshot designation.
     * @param versionComponents The components and configuration used to determine how the version should be bumped.
     * @return The updated version after applying the required changes.
     */
    private fun bumpVersion(
        forcePush: Boolean,
        currentVersion: Version,
        hotfixRequired: Boolean,
        snapshotRequired: Boolean,
        versionComponents: SetupVersionComponentsForBump
    ): Version {
        if (hotfixRequired || currentVersion.isHotfix()) {
            versionComponents
                .addHotFix()
                .removeMajor()
                .removeMinor()
                .removePatch()
                .removePreRelease()
                .removePromoteToRelease()
            if (currentVersion.isHotfix()) versionComponents.removeNewPreRelease()
        }

        if (currentVersion.isPreRelease()) {
            versionComponents
                .addPreRelease()
                .removeHotFix()
                .removeNewPreRelease()
                .removeMajor()
                .removeMinor()
                .removePatch()
        }

        if (versionComponents.hasPromoteToRelease()) {
            if (currentVersion.isPreRelease()) {
                // when current version is pre-release and promote to release is set, ignore any other flags
                logger.warn("Promoting to release flag is set and current version is pre-release, ignoring any other flag.")
                versionComponents.reset().addPromoteToRelease()
            } else {
                logger.warn("Promoting to release flag is set but current version is not a pre-release version, ignoring it.")
                versionComponents.removePromoteToRelease()
            }
        }

        if (versionComponents.hasMajor()) {
            versionComponents.removeMinor().removePatch()
        }

        if (versionComponents.hasMinor()) {
            versionComponents.removePatch()
        }

        if (snapshotRequired) {
            // This is special case, we figured this is snapshot version, but we don't know which component to bump, bump defaultBumpLevel
            if (
                !versionComponents.hasMandatoryComponents() && !versionComponents.hasPreRelease() && !versionComponents.hasPromoteToRelease()
            )
                versionComponents
                    .addComponentIfRequired(
                        baseConfig.toDefaultVersionComponent(),
                        addDefaultComponent(baseConfig.toDefaultVersionComponent())
                    )

            versionComponents.addSnapshot()
        }

        if (!versionComponents.hasEssentialComponents()) {
            if (forcePush) {
                // We don't have any defined bump level use defaultBumpLevel
                versionComponents.addComponentIfRequired(baseConfig.toDefaultVersionComponent()) { forcePush }
            } else if (baseConfig.forceBump) {
                throw IllegalArgumentException(
                    "Couldn't determine next version, tag (${currentVersion.toStringValue()}) is already exists."
                )
            }
        }

        return currentVersion.bumpVersion(getSnapshotInfo(), versionComponents.getVersionComponents())
    }

    private fun getSnapshotInfo(): Snapshot {
        val hash = if (snapshotConfig.appendCommitHash) {
            if (snapshotConfig.useShortHash) {
                runCatching { adapter.getShortHash() }.getOrNull()
            } else {
                runCatching { adapter.getFullHash() }.getOrNull()
            }
        } else null

        return Snapshot(snapshotSuffix, hash)
    }

    private fun addDefaultComponent(versionComponent: VersionComponent): () -> Boolean {
        return {
            listOf(
                VersionComponent.MAJOR,
                VersionComponent.MINOR,
                VersionComponent.PATCH
            ).contains(versionComponent)
        }
    }

    companion object {
        private data class Tuple(val hasCommits: Boolean, val commitMessages: List<String>)
    }

}
