package com.alphasystem.gradle.semver.release.internal

import semverrelease.PreReleaseConfig

/**
 * Represents a pre-release version component in semantic versioning.
 *
 * This data class encapsulates a pre-release version with two parts:
 * - A mandatory prefix representing the type or identifier of the pre-release version.
 * - A mandatory version number, which must be an integer.
 *
 * Key Characteristics:
 * - The prefix must not be null or empty.
 * - The version number must not be null.
 * - The class is immutable and ensures validation of the prefix and version during instantiation.
 *
 * Usage Scenarios:
 * - Representing a pre-release component as part of a semantic version.
 * - Bumping the version number for iterative pre-release versions.
 * - Generating a string representation of the pre-release version.
 *
 * Key Behaviors:
 * - Instantiation with validation
 */
data class PreReleaseVersion(
    val prefix: String,
    val separator: String,
    val version: Int
) {

    init {
        require(prefix.isNotBlank()) { "prefix cannot be null or empty string" }
        require(separator.isNotBlank()) { "separator cannot be null or empty string" }
        require(version >= 0) { "version number must be non-negative" }
    }

    constructor(preReleaseConfig: PreReleaseConfig) : this(
        preReleaseConfig.prefix,
        preReleaseConfig.separator,
        preReleaseConfig.startingVersion
    )

    fun bumpVersion(): PreReleaseVersion = copy(version = version + 1)

    fun toStringValue(): String = "-$prefix$separator$version"
}
