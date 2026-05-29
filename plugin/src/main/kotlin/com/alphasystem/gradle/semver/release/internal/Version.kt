package com.alphasystem.gradle.semver.release.internal

import semverrelease.PreReleaseConfig
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Represents a version with components for major, minor, patch, optional hotfix, optional pre-release,
 * optional snapshot, and a required pre-release configuration.
 *
 * Instances of this class are immutable and follow semantic versioning principles.
 *
 * The class enforces validation of the version components during instantiation to ensure correctness.
 */
@JvmRecord
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val hotfix: Int?,
    val preRelease: PreReleaseVersion?,
    val snapshot: Snapshot?,
    val preReleaseConfig: PreReleaseConfig
) {
    companion object {
        private val VERSION_REGEX =
            Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?(?:\\.(0|[1-9]\\d*))?$")

        private class VersionComparator : Comparator<Version> {
            override fun compare(v1: Version, v2: Version): Int {
                val list = mutableListOf<Tuple>()
                list.add(Tuple(v2.major, v1.major))
                list.add(Tuple(v2.minor, v1.minor))
                list.add(Tuple(v2.patch, v1.patch))
                list.add(Tuple(v2.hotfix ?: 0, v1.hotfix ?: 0))
                list.add(Tuple(v2.preRelease?.version ?: Int.MAX_VALUE, v1.preRelease?.version ?: Int.MAX_VALUE))
                return compareTo(list)
            }

            private fun compareTo(tuples: List<Tuple>): Int {
                if (tuples.isEmpty()) {
                    return 0
                }
                val head = tuples.first()
                val comparison = head.first.compareTo(head.second)
                return if (comparison == 0) {
                    compareTo(tuples.subList(1, tuples.size))
                } else {
                    comparison
                }
            }
        }

        private data class Tuple(val first: Int, val second: Int)

        val VERSION_COMPARATOR = Comparator<Version> { v1, v2 ->
            VersionComparator().compare(v1, v2)
        }

        /**
         * Applies transformations to a version string, parsing it into components of a {@link Version} object.
         *
         * The method processes a version string, identifying and extracting major, minor, patch, hotfix,
         * pre-release, and snapshot components based on the provided snapshot suffix and pre-release configuration.
         * If the input version string does not adhere to the expected format, an {@link IllegalArgumentException} is thrown.
         *
         * @param version the input version string to be processed, which must adhere to the expected version format.
         * @param snapshotSuffix the suffix used to identify snapshot versions (e.g., "SNAPSHOT").
         * @param preReleaseConfig the configuration for handling pre-release versions,
         *         including prefix, separator, and versioning rules.
         *
         * @return a constructed {@link Version} object containing the parsed version components.
         *
         * @throws IllegalArgumentException if the provided version format is invalid or does not match the expected pattern.
         */
        fun create(version: String, snapshotSuffix: String, preReleaseConfig: PreReleaseConfig): Version {
            val matcher = VERSION_REGEX.matcher(version)

            // Check if the version string matches the expected pattern
            if (!matcher.matches()) {
                throw IllegalArgumentException("Invalid version format: $version")
            }

            // Group 0 will be the entire version string, Groups 1 - 3 will be major, minor, and patch versions, so there must be
            // at least four groups. There will be a total of 6 groups.

            if (matcher.groupCount() < 4) {
                throw IllegalArgumentException("Invalid version format: $version")
            }

            // pre-release and snapshot
            val maybePreReleaseOrSnapshot = getMatchedGroup(matcher, 4)
            val metaInfo = getMatchedGroup(matcher, 5)

            val preReleaseVersion =
                maybePreReleaseOrSnapshot?.replace(snapshotSuffix, "")
                    ?.let { sanitizePreReleaseOrSnapshotVersion(it) }
                    ?.let { preReleaseConfig.toPreReleaseVersion(it) }

            val snapshot =
                maybePreReleaseOrSnapshot?.replace(preReleaseConfig.preReleasePartPattern().pattern(), "")
                    ?.let { if (it.contains(snapshotSuffix)) Snapshot(snapshotSuffix, metaInfo) else null }

            val hotfix = getMatchedGroup(matcher, 6)?.toInt()

            return Version(
                matcher.group(1).toInt(),
                matcher.group(2).toInt(),
                matcher.group(3).toInt(),
                hotfix,
                preReleaseVersion,
                snapshot,
                preReleaseConfig
            )
        }

        private fun getMatchedGroup(matcher: Matcher, group: Int): String? {
            return try {
                matcher.group(group)
            } catch (_: Exception) {
                null
            }
        }

        /**
         * We use "-" as a separator between pre-release and snapshot version. If both versions are present then we need to remove the
         * trailing "-" to make it a legal pre-release version
         */
        private fun sanitizePreReleaseOrSnapshotVersion(src: String): String? {
            return if (src.isBlank()) null
            else if (src.endsWith("-")) {
                src.substring(0, src.length - 1)
            } else src
        }

        private fun bumpVersion(snapshot: Snapshot?): (Version, VersionComponent) -> Version {
            return { version, c ->
                when (c) {
                    VersionComponent.NONE -> version
                    VersionComponent.MAJOR -> version.bumpMajor()
                    VersionComponent.MINOR -> version.bumpMinor()
                    VersionComponent.PATCH -> version.bumpPatch()
                    VersionComponent.HOT_FIX -> version.bumpHotfix()
                    VersionComponent.NEW_PRE_RELEASE -> version.newPreRelease()
                    VersionComponent.PRE_RELEASE -> version.bumpPreRelease()
                    VersionComponent.PROMOTE_TO_RELEASE -> version.promoteToRelease()
                    VersionComponent.SNAPSHOT -> version.bumpSnapshot(snapshot)
                }
            }
        }
    }

    fun isHotfix(): Boolean {
        return hotfix != null
    }

    fun isPreRelease(): Boolean {
        return preRelease != null
    }

    fun bumpVersion(snapshot: Snapshot?, componentsToBump: List<VersionComponent>): Version {
        return componentsToBump.fold(this) { version, component -> bumpVersion(snapshot)(version, component) }
    }

    fun toStringValue(): String {
        val hotfixV = hotfix?.let { ".$it" } ?: ""
        val preReleaseV = preRelease?.toStringValue() ?: ""
        val snapshotV = snapshot?.toStringValue() ?: ""
        return "$major.$minor.$patch$hotfixV$preReleaseV$snapshotV"
    }

    private fun bumpMajor(): Version {
        return Version(major + 1, 0, 0, null, null, snapshot, preReleaseConfig)
    }

    private fun bumpMinor(): Version {
        return Version(major, minor + 1, 0, hotfix, preRelease, snapshot, preReleaseConfig)
    }

    private fun bumpPatch(): Version {
        return Version(major, minor, patch + 1, hotfix, preRelease, snapshot, preReleaseConfig)
    }

    private fun bumpHotfix(): Version {
        if (preRelease != null) {
            throw IllegalArgumentException("Current version is a a pre-release.")
        }
        return Version(major, minor, patch, hotfix?.let { it + 1 } ?: 1, preRelease, snapshot, preReleaseConfig)
    }

    private fun bumpPreRelease(): Version {
        if (preRelease == null) {
            throw IllegalArgumentException(
                "Cannot bump pre-release because the latest version is not a pre-release version." +
                        " To create a new pre-release version, use newPreRelease instead"
            )
        }
        return Version(major, minor, patch, hotfix, preRelease.bumpVersion(), snapshot, preReleaseConfig)
    }

    private fun newPreRelease(): Version {
        if (preRelease != null) {
            throw IllegalArgumentException("Current version is already pre-release")
        }
        return Version(major, minor, patch, hotfix, PreReleaseVersion(preReleaseConfig), snapshot, preReleaseConfig)
    }

    private fun promoteToRelease(): Version {
        return Version(major, minor, patch, hotfix, null, snapshot, preReleaseConfig)
    }

    fun bumpSnapshot(snapshot: Snapshot?): Version {
        return Version(major, minor, patch, hotfix, preRelease, snapshot, preReleaseConfig)
    }
}
