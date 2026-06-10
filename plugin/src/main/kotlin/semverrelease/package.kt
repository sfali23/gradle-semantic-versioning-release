package semverrelease

const val DefaultBooleanValue = false
const val DefaultStartingVersion: String = "0.1.0"
const val DefaultTagPrefix: String = "v"
const val DefaultSnapshotPrefix: String = "SNAPSHOT"
const val DefaultPreReleasePrefix: String = "RC"
const val DefaultPreReleaseSeparator: String = "."
const val DefaultPreReleaseStartingVersion: Int = 1
val DefaultBumpLevel: ComponentToBump = ComponentToBump.PATCH
val DefaultComponentToBump: ComponentToBump = ComponentToBump.NONE
val DefaultHotfixBranchPattern: Regex = initializeHotfixBranchPattern()
val DefaultReleaseBranches: List<String> = listOf("main", "master")

const val RELEASE_GROUP = "Release"

const val ANSI_RESET: String = "\u001B[0m"
const val ANSI_GREEN = "\u001B[32m"
const val ANSI_RED: String = "\u001B[31m"

private fun initializeHotfixBranchPattern(tagPrefix: String = DefaultTagPrefix): Regex =
    "^$tagPrefix(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\+$".toRegex()

fun Regex.nonEmpty(input: String) = this.find(input) != null
