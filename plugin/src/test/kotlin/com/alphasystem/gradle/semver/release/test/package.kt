package com.alphasystem.gradle.semver.release.test

import com.alphasystem.gradle.semver.release.internal.SemanticBuildVersionConfiguration
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import semverrelease.AutoBump
import semverrelease.AutoBump.Companion.DefaultMajorPattern
import semverrelease.AutoBump.Companion.DefaultMinorPattern
import semverrelease.AutoBump.Companion.DefaultNewPreReleasePattern
import semverrelease.AutoBump.Companion.DefaultPatchPattern
import semverrelease.AutoBump.Companion.DefaultPromoteToReleasePattern
import semverrelease.*

fun Config.readFailSafeString(path: String, defaultValue: String): String =
    if (this.hasPath(path)) this.getString(path) else defaultValue

fun Config.readFailSafeInt(path: String, defaultValue: Int): Int =
    if (this.hasPath(path)) this.getInt(path) else defaultValue

fun Config.readFailSafeBoolean(path: String, defaultValue: Boolean): Boolean =
    if (this.hasPath(path)) this.getBoolean(path) else defaultValue

fun Config.readFailSafeRegex(path: String, defaultValue: Regex): Regex =
    if (this.hasPath(path)) this.getString(path).toRegex() else defaultValue

fun Config.toAutoBump(): AutoBump =
    AutoBump(
        majorPattern = this.readFailSafeRegex("majorPattern", DefaultMajorPattern),
        minorPattern = this.readFailSafeRegex("minorPattern", DefaultMinorPattern),
        patchPattern = this.readFailSafeRegex("patchPattern", DefaultPatchPattern),
        newPreReleasePattern = this.readFailSafeRegex("newPreReleasePattern", DefaultNewPreReleasePattern),
        promoteToReleasePattern = this.readFailSafeRegex("promoteToReleasePattern", DefaultPromoteToReleasePattern)
    )

fun Config.toPreReleaseConfig(): PreReleaseConfig =
    PreReleaseConfig(
        prefix = this.readFailSafeString("prefix", DefaultPreReleasePrefix),
        separator = this.readFailSafeString("separator", DefaultPreReleaseSeparator),
        startingVersion = this.readFailSafeInt("startingVersion", DefaultPreReleaseStartingVersion)
    )

fun Config.toSnapshotConfig(): SnapshotConfig =
    SnapshotConfig(
        prefix = this.readFailSafeString("prefix", DefaultSnapshotPrefix),
        appendCommitHash = this.readFailSafeBoolean("appendCommitHash", defaultValue = true),
        useShortHash = this.readFailSafeBoolean("useShortHash", defaultValue = true)
    )

fun Config.toSemanticBuildVersionConfiguration() =
    SemanticBuildVersionConfiguration(
        startingVersion = this.readFailSafeString("startingVersion", DefaultStartingVersion),
        tagPrefix = this.readFailSafeString("tagPrefix", DefaultTagPrefix),
        forceBump = this.readFailSafeBoolean("forceBump", DefaultBooleanValue),
        promoteToRelease = this.readFailSafeBoolean("promoteToRelease", DefaultBooleanValue),
        snapshot = this.readFailSafeBoolean("snapshot", DefaultBooleanValue),
        newPreRelease = this.readFailSafeBoolean("newPreRelease", DefaultBooleanValue),
        autoBump = if (this.hasPath("autoBump")) this.getConfig("autoBump").toAutoBump() else AutoBump(),
        defaultBumpLevel =
            ComponentToBump.valueOf(this.readFailSafeString("defaultBumpLevel", DefaultBumpLevel.name)),
        componentToBump =
            ComponentToBump.valueOf(this.readFailSafeString("componentToBump", DefaultComponentToBump.name)),
        snapshotConfig =
            if (this.hasPath("snapshotConfig")) this.getConfig("snapshotConfig").toSnapshotConfig() else SnapshotConfig(),
        preReleaseConfig =
            if (this.hasPath("preReleaseConfig")) this.getConfig("preReleaseConfig").toPreReleaseConfig()
            else PreReleaseConfig(),
        hotfixBranchPattern = this.readFailSafeRegex("hotfixBranchPattern", DefaultHotfixBranchPattern),
        extraReleaseBranches = listOf()
    )

fun toSemanticBuildVersionConfiguration(src: String): SemanticBuildVersionConfiguration =
    ConfigFactory.parseString(src).toSemanticBuildVersionConfiguration()
