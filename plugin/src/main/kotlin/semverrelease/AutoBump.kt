package semverrelease

import java.util.Optional

@JvmRecord
data class AutoBump(
    val majorPattern: Regex? = DefaultMajorPattern,
    val minorPattern: Regex? = DefaultMinorPattern,
    val patchPattern: Regex? = DefaultPatchPattern,
    val newPreReleasePattern: Regex? = DefaultNewPreReleasePattern,
    val promoteToReleasePattern: Regex? = DefaultPromoteToReleasePattern
) {

    companion object {
        val DefaultMajorPattern: Regex = "\\[major]".toRegex()
        val DefaultMinorPattern: Regex = "\\[minor]".toRegex()
        val DefaultPatchPattern: Regex = "\\[patch]".toRegex()
        val DefaultNewPreReleasePattern: Regex = "\\[new-pre-release]".toRegex()
        val DefaultPromoteToReleasePattern: Regex = "\\[promote]".toRegex()

        private fun matchValue(input: String, pattern: Regex?): Boolean {
            // git command line put commit message withing quotation mark and if regex has '^', then matching doesn't work
            // remove starting and end quotation (")
            val value =
                Optional.ofNullable(input.trim())
                    .map { value -> if (value.startsWith("\"")) value.drop(1) else value }
                    .map { value -> if (value.endsWith("\"")) value.dropLast(1) else value }
                    .orElse("")

            return pattern?.nonEmpty(value!!) == true
        }
    }

    fun major(input: String): Boolean = matchValue(input, majorPattern)

    fun minor(input: String): Boolean = matchValue(input, minorPattern)

    fun patch(input: String): Boolean = matchValue(input, patchPattern)

    fun newPreRelease(input: String): Boolean = matchValue(input, newPreReleasePattern)

    fun promoteToRelease(input: String): Boolean = matchValue(input, promoteToReleasePattern)

    fun isEnabled(): Boolean =
        majorPattern?.pattern?.isNotBlank() == true ||
                minorPattern?.pattern?.isNotBlank() == true || patchPattern?.pattern?.isNotBlank() == true ||
                newPreReleasePattern?.pattern?.isNotBlank() == true ||
                promoteToReleasePattern?.pattern?.isNotBlank() == true
}
