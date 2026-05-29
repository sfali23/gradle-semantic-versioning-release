package semverrelease

import java.util.regex.Pattern

/**
 * Configuration class representing pre-release settings for semantic versioning.
 *
 * This data class encapsulates the configuration for generating and parsing
 * pre-release version components in semantic versioning.
 * It includes details such as a prefix, separator, and starting version for the pre-release versions.
 *
 * Key Characteristics:
 * - Immutable by design as a data class with @JvmRecord annotation.
 * - Validates the provided values during instantiation to ensure correctness.
 * - Supports default initialization with standard values.
 *
 * Fields:
 * - `prefix`: The prefix to denote the type of pre-release (e.g., "RC", "alpha").
 * - `separator`: The separator between the prefix and the numeric version.
 * - `startingVersion`: The initial version number for pre-release versions, which must be greater than zero.
 *
 * Key Behaviors:
 * - Validation of the prefix, separator, and starting version during instantiation.
 * - A default constructor that initializes the prefix to "RC", the separator to ".", and the starting version to 1.
 * - Provides a method to compile a regex pattern for matching and parsing pre-release version components based on the configuration.
 *
 * Usage Scenarios:
 * - Defining configurations for semantic version pre-release components.
 * - Parsing and validating pre-release versions based on configured patterns.
 *
 * Constraints:
 * - `prefix` must not be null or empty.
 * - `separator` must not be null or empty.
 * - `startingVersion` must be a positive integer greater than zero.
 */
@JvmRecord
data class PreReleaseConfig(
    val prefix: String = DefaultPreReleasePrefix,
    val separator: String = DefaultPreReleaseSeparator,
    val startingVersion: Int = DefaultPreReleaseStartingVersion
) {
    init {
        require(prefix.isNotBlank()) { "prefix cannot be null or empty string" }
        require(startingVersion > 0) { "startingVersion must be positive integer greater than 0" }
        require(separator.isNotBlank()) { "separator cannot be null or empty string" }
    }

    fun preReleasePartPattern(): Pattern {
        val escapedSeparator = separator.replace(".", "\\.")
        val pattern = "^(?i)($prefix)($escapedSeparator)([1-9]\\d*)$"
        return Pattern.compile(pattern)
    }
}
