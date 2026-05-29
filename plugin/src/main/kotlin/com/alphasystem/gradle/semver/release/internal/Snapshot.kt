package com.alphasystem.gradle.semver.release.internal

import org.eclipse.jgit.util.StringUtils
import semverrelease.DefaultSnapshotPrefix

/**
 * Represents a snapshot version prefix that can optionally include metadata.
 *
 * This data class is immutable and ensures that the snapshot prefix is non-empty.
 * The snapshot version may also include optional metadata, which is formatted
 * as part of the prefix string during string representation.
 *
 * Instances of this class are commonly used to denote a snapshot version in
 * versioning systems that follow semantic versioning.
 *
 * Constraints:
 * - The prefix must not be null or empty.
 *
 * Usage Scenarios:
 * - Constructing a snapshot version (e.g., "-SNAPSHOT" or "-SNAPSHOT+meta").
 * - Building snapshot components as part of a larger versioning scheme.
 *
 * Key Behaviors:
 * - Validation of the snapshot prefix during instantiation.
 * - A default constructor that initializes the prefix to "SNAPSHOT" with null metadata.
 * - A formatted string representation combining the prefix and optional metadata.
 */
data class Snapshot(
    val prefix: String,
    val meta: String?
) {
    init {
        require(prefix.isNotBlank()) { "prefix cannot be null or empty string" }
    }

    constructor() : this(DefaultSnapshotPrefix, null)

    fun toStringValue(): String {
        var metaValue = ""
        if (!StringUtils.isEmptyOrNull(meta)) {
            metaValue = "+$meta"
        }
        return String.format("-%s%s", prefix, metaValue)
    }
}
