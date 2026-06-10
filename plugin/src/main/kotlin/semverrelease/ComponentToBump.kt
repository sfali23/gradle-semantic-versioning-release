package semverrelease

/**
 * Represents the components of a version that can be incremented during a versioning operation.
 *
 * This enum is typically used in semantic versioning processes to identify whether
 * the major, minor, or patch version should be incremented based on specific changes
 * or updates in the application.
 */
enum class ComponentToBump {
    NONE, MAJOR, MINOR, PATCH
}
