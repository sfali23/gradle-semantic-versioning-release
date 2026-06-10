package semverrelease

data class SnapshotConfig(
    val prefix: String = DefaultSnapshotPrefix,
    val appendCommitHash: Boolean = true,
    val useShortHash: Boolean = true
) {
    init {
        require(prefix.isNotBlank()) { "prefix cannot be null or empty string" }
    }
}
