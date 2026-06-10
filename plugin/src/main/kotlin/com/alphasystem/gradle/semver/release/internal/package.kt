package com.alphasystem.gradle.semver.release.internal

import com.alphasystem.gradle.semver.release.common.JGitAdapter
import semverrelease.ComponentToBump
import semverrelease.PreReleaseConfig

fun PreReleaseConfig.toPreReleaseVersion(version: String): PreReleaseVersion {
    val result = PreReleaseVersion(this)

    val matcher = this.preReleasePartPattern().matcher(version)
    val groupCount = matcher.groupCount()
    if (!matcher.matches() && groupCount < 4) {
        throw IllegalArgumentException("Invalid pre-release version: $version")
    }

    val prefix = matcher.group(1)
    if (this.prefix != prefix) {
        throw IllegalArgumentException("Invalid pre-release version: $version, expected prefix: ${this.prefix}, actual: $prefix")
    }

    val separator = matcher.group(2)
    if (this.separator != separator) {
        throw IllegalArgumentException("Invalid pre-release version: $version, expected separator: ${this.separator}, actual: $separator")
    }

    val versionValue = matcher.group(3)
    if (!isNumeric(versionValue)) {
        throw IllegalArgumentException("Invalid pre-release version: $version, version has to be a number: $versionValue")
    }

    return result.copy(version = versionValue.toInt())
}

private fun isNumeric(str: String?): Boolean {
    return str != null && str.matches("-?\\d+".toRegex())
}

fun ComponentToBump.toVersionComponent() = VersionComponent.valueOf(this.name)

fun JGitAdapter.pushTag(): Int {
    val processBuilder = ProcessBuilder("git", "push", "--tags")
    processBuilder.directory(this.workingDir)
    processBuilder.inheritIO()
    return processBuilder.start().waitFor()
}
