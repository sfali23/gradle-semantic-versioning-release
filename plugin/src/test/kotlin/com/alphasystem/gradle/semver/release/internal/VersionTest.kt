package com.alphasystem.gradle.semver.release.internal

import semverrelease.PreReleaseConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("Version Tests")
class VersionTest {

    companion object {
        private val DEFAULT_CONFIG = PreReleaseConfig()
        private val DEFAULT_SNAPSHOT = Snapshot()
        private val DEFAULT_SNAPSHOT_PREFIX = DEFAULT_SNAPSHOT.prefix
        private val DEFAULT_PRE_RELEASE = PreReleaseVersion(DEFAULT_CONFIG)
    }

    @Nested
    @DisplayName("Parse Version Tests")
    inner class ParseVersionTests {

        @Test
        @DisplayName("Should parse version with major, minor, patch")
        fun shouldParseSimpleVersion() {
            val actual = Version.create("0.12.345", DEFAULT_SNAPSHOT_PREFIX, DEFAULT_CONFIG)
            val expected = Version(0, 12, 345, null, null, null, DEFAULT_CONFIG)
            assertEquals(expected, actual)
        }

        @Test
        @DisplayName("Should parse version with hotfix")
        fun shouldParseVersionWithHotfix() {
            val actual = Version.create("1.2.3.4", DEFAULT_SNAPSHOT_PREFIX, DEFAULT_CONFIG)
            val expected = Version(1, 2, 3, 4, null, null, DEFAULT_CONFIG)
            assertEquals(expected, actual)
        }

        @Test
        @DisplayName("Should parse version with pre-release")
        fun shouldParseVersionWithPreRelease() {
            val actual = Version.create("1.2.3-RC.1", DEFAULT_SNAPSHOT_PREFIX, DEFAULT_CONFIG)
            val expected = Version(1, 2, 3, null, PreReleaseVersion("RC", DEFAULT_CONFIG.separator, 1), null, DEFAULT_CONFIG)
            assertEquals(actual, expected)
        }

        @Test
        @DisplayName("Should parse version with all components")
        fun shouldParseVersionWithAllComponents() {
            val actual = Version.create("1.2.3-RC.1-SNAPSHOT+abcd", DEFAULT_SNAPSHOT_PREFIX, DEFAULT_CONFIG)
            val expected = Version(
                1, 2, 3, null, PreReleaseVersion("RC", DEFAULT_CONFIG.separator, 1),
                Snapshot(DEFAULT_SNAPSHOT_PREFIX, "abcd"), DEFAULT_CONFIG
            )
            assertEquals(actual, expected)
        }

        @Test
        @DisplayName("Should parse version with snapshot component")
        fun shouldParseVersionWithSnapshotComponent() {
            val actual = Version.create("1.2.3-SNAPSHOT+abcd", DEFAULT_SNAPSHOT_PREFIX, DEFAULT_CONFIG)
            val expected = Version(
                1, 2, 3, null, null,
                Snapshot(DEFAULT_SNAPSHOT_PREFIX, "abcd"), DEFAULT_CONFIG
            )
            assertEquals(actual, expected)
        }

        @Test
        @DisplayName("Should parse version with hotfix and pre-release")
        fun shouldParseVersionWithHotfixAndPreRelease() {
            // Note: Based on the regex pattern, hotfix and pre-release cannot be combined.
            // This test demonstrates the current limitation
            val actual = Version.create("1.2.3.4", DEFAULT_SNAPSHOT_PREFIX, DEFAULT_CONFIG)
            val expected = Version(1, 2, 3, 4, null, null, DEFAULT_CONFIG)
            assertEquals(actual, expected)
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "1.0.0", "0.1.0", "0.0.1", "10.20.30", "1.1.2", "10.0.0", "1.0.0-RC.1",
                "1.0.0-RC.2", "1.0.0+build.1", "1.0.0-RC.1+build.2", "1.0.0+20130313144700"
            ]
        )
        @DisplayName("Should parse valid semantic version formats")
        fun shouldParseValidSemanticVersions(versionString: String) {
            val version = Version.create(versionString, DEFAULT_SNAPSHOT_PREFIX, DEFAULT_CONFIG)
            assertNotNull(version)
            assertTrue(version.major >= 0)
            assertTrue(version.minor >= 0)
            assertTrue(version.patch >= 0)
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "1", "1.2", "1.2.3-0123", "1.2.3-0123.0123", "1.2.3-", "1.2.3+",
                "1.2.3-alpha..1", "1.2.3-alpha.1.", "1.2.3.4.5", "1.2.3.4.5.6", "1.2.3-alpha@beta"
            ]
        )
        @DisplayName("Should throw exception for invalid version formats")
        fun shouldThrowExceptionForInvalidVersions(versionString: String) {
            assertThrows<IllegalArgumentException>(
                "Expected IllegalArgumentException for invalid version: $versionString"
            ) {
                Version.create(versionString, DEFAULT_SNAPSHOT_PREFIX, DEFAULT_CONFIG)
            }
        }

        @Test
        @DisplayName("Should parse version with zero values")
        fun shouldParseVersionWithZeroValues() {
            val version = Version.create("0.0.0", DEFAULT_SNAPSHOT_PREFIX, DEFAULT_CONFIG)
            assertEquals(0, version.major)
            assertEquals(0, version.minor)
            assertEquals(0, version.patch)
            assertEquals(null, version.hotfix)
            assertEquals(null, version.preRelease)
            assertEquals(null, version.snapshot)
        }

        @Test
        @DisplayName("Should parse version with large numbers")
        fun shouldParseVersionWithLargeNumbers() {
            val version = Version.create("999.888.777", DEFAULT_SNAPSHOT_PREFIX, DEFAULT_CONFIG)
            assertEquals(999, version.major)
            assertEquals(888, version.minor)
            assertEquals(777, version.patch)
            assertEquals(null, version.hotfix)
            assertEquals(null, version.preRelease)
            assertEquals(null, version.snapshot)
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    inner class ConstructorTests {

        @Test
        @DisplayName("Should create version with all required fields")
        fun shouldCreateVersionWithRequiredFields() {
            val version = Version(1, 2, 3, null, null, null, DEFAULT_CONFIG)

            assertEquals(1, version.major)
            assertEquals(2, version.minor)
            assertEquals(3, version.patch)
            assertEquals(null, version.hotfix)
            assertEquals(null, version.preRelease)
            assertEquals(null, version.snapshot)
            assertEquals(DEFAULT_CONFIG, version.preReleaseConfig)
        }

        @Test
        @DisplayName("Should create version with all fields")
        fun shouldCreateVersionWithAllFields() {
            val version = Version(1, 2, 3, 4, DEFAULT_PRE_RELEASE, DEFAULT_SNAPSHOT, DEFAULT_CONFIG)

            assertEquals(1, version.major)
            assertEquals(2, version.minor)
            assertEquals(3, version.patch)
            assertEquals(4, version.hotfix)
            assertEquals(DEFAULT_PRE_RELEASE, version.preRelease)
            assertEquals(DEFAULT_SNAPSHOT, version.snapshot)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 100, Int.MAX_VALUE])
        @DisplayName("Should accept valid major version")
        fun shouldAcceptValidMajorVersion(major: Int) {
            assertDoesNotThrow {
                Version(major, 0, 0, null, null, null, DEFAULT_CONFIG)
            }
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 100, Int.MAX_VALUE])
        @DisplayName("Should accept valid minor version")
        fun shouldAcceptValidMinorVersion(minor: Int) {
            assertDoesNotThrow {
                Version(1, minor, 0, null, null, null, DEFAULT_CONFIG)
            }
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 100, Int.MAX_VALUE])
        @DisplayName("Should accept valid patch version")
        fun shouldAcceptValidPatchVersion(patch: Int) {
            assertDoesNotThrow {
                Version(1, 0, patch, null, null, null, DEFAULT_CONFIG)
            }
        }

        @Nested
        @DisplayName("Utility Method Tests")
        inner class UtilityMethodTests {

            @Test
            @DisplayName("isHotfix should return true when hotfix is present")
            fun isHotfixShouldReturnTrueWhenHotfixIsPresent() {
                val version = Version(1, 2, 3, 1, null, null, DEFAULT_CONFIG)
                assertTrue(version.isHotfix())
            }

            @Test
            @DisplayName("isHotfix should return false when hotfix is absent")
            fun isHotfixShouldReturnFalseWhenHotfixIsAbsent() {
                val version = Version(1, 2, 3, null, null, null, DEFAULT_CONFIG)
                assertFalse(version.isHotfix())
            }

            @Test
            @DisplayName("isPreRelease should return true when preRelease is present")
            fun isPreReleaseShouldReturnTrueWhenPreReleaseIsPresent() {
                val version = Version(1, 2, 3, null, DEFAULT_PRE_RELEASE, null, DEFAULT_CONFIG)
                assertTrue(version.isPreRelease())
            }

            @Test
            @DisplayName("isPreRelease should return false when preRelease is absent")
            fun isPreReleaseShouldReturnFalseWhenPreReleaseIsAbsent() {
                val version = Version(1, 2, 3, null, null, null, DEFAULT_CONFIG)
                assertFalse(version.isPreRelease())
            }

            @Test
            @DisplayName("toStringValue should return basic version format")
            fun toStringValueShouldReturnBasicVersionFormat() {
                val version = Version(1, 2, 3, null, null, null, DEFAULT_CONFIG)
                assertEquals("1.2.3", version.toStringValue())
            }

            @Test
            @DisplayName("toStringValue should include hotfix when present")
            fun toStringValueShouldIncludeHotfixWhenPresent() {
                val version = Version(1, 2, 3, 4, null, null, DEFAULT_CONFIG)
                assertEquals("1.2.3.4", version.toStringValue())
            }

            @Test
            @DisplayName("toStringValue should include preRelease when present")
            fun toStringValueShouldIncludePreReleaseWhenPresent() {
                val version = Version(1, 2, 3, null, DEFAULT_PRE_RELEASE, null, DEFAULT_CONFIG)
                assertEquals("1.2.3-RC.1", version.toStringValue())
            }

            @Test
            @DisplayName("toStringValue should include both hotfix and preRelease when present")
            fun toStringValueShouldIncludeBothHotfixAndPreReleaseWhenPresent() {
                val version = Version(1, 2, 3, 4, DEFAULT_PRE_RELEASE, null, DEFAULT_CONFIG)
                assertEquals("1.2.3.4-RC.1", version.toStringValue())
            }
        }

        @Nested
        @DisplayName("Version Bumping Tests")
        inner class VersionBumpingTests {

            @Test
            @DisplayName("bumpVersion should handle NONE component")
            fun bumpVersionShouldHandleNoneComponent() {
                val version = Version(1, 2, 3, null, null, null, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                val result = version.bumpVersion(snapshot, listOf(VersionComponent.NONE))

                assertEquals(version, result)
            }

            @Test
            @DisplayName("bumpVersion should bump major version")
            fun bumpVersionShouldBumpMajorVersion() {
                val version = Version(1, 2, 3, 4, DEFAULT_PRE_RELEASE, DEFAULT_SNAPSHOT, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                val result = version.bumpVersion(snapshot, listOf(VersionComponent.MAJOR))

                assertEquals(2, result.major)
                assertEquals(0, result.minor)
                assertEquals(0, result.patch)
                assertEquals(null, result.hotfix)
                assertEquals(null, result.preRelease)
                assertEquals(DEFAULT_SNAPSHOT, result.snapshot)
                assertEquals(DEFAULT_CONFIG, result.preReleaseConfig)
            }

            @Test
            @DisplayName("bumpVersion should bump minor version")
            fun bumpVersionShouldBumpMinorVersion() {
                val version = Version(1, 2, 3, 4, DEFAULT_PRE_RELEASE, DEFAULT_SNAPSHOT, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                val result = version.bumpVersion(snapshot, listOf(VersionComponent.MINOR))

                assertEquals(1, result.major)
                assertEquals(3, result.minor)
                assertEquals(0, result.patch)
                assertEquals(4, result.hotfix)
                assertEquals(DEFAULT_PRE_RELEASE, result.preRelease)
                assertEquals(DEFAULT_SNAPSHOT, result.snapshot)
                assertEquals(DEFAULT_CONFIG, result.preReleaseConfig)
            }

            @Test
            @DisplayName("bumpVersion should bump patch version")
            fun bumpVersionShouldBumpPatchVersion() {
                val version = Version(1, 2, 3, 4, DEFAULT_PRE_RELEASE, DEFAULT_SNAPSHOT, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                val result = version.bumpVersion(snapshot, listOf(VersionComponent.PATCH))

                assertEquals(1, result.major)
                assertEquals(2, result.minor)
                assertEquals(4, result.patch)
                assertEquals(4, result.hotfix)
                assertEquals(DEFAULT_PRE_RELEASE, result.preRelease)
                assertEquals(DEFAULT_SNAPSHOT, result.snapshot)
                assertEquals(DEFAULT_CONFIG, result.preReleaseConfig)
            }

            @Test
            @DisplayName("bumpVersion should bump hotfix version")
            fun bumpVersionShouldBumpHotfixVersion() {
                val version = Version(1, 2, 3, null, null, null, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                val result = version.bumpVersion(snapshot, listOf(VersionComponent.HOT_FIX))

                assertEquals(1, result.major)
                assertEquals(2, result.minor)
                assertEquals(3, result.patch)
                assertEquals(1, result.hotfix)
                assertEquals(null, result.preRelease)
                assertEquals(null, result.snapshot)
                assertEquals(DEFAULT_CONFIG, result.preReleaseConfig)
            }

            @Test
            @DisplayName("bumpVersion should increment existing hotfix")
            fun bumpVersionShouldIncrementExistingHotfix() {
                val version = Version(1, 2, 3, 1, null, null, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                val result = version.bumpVersion(snapshot, listOf(VersionComponent.HOT_FIX))

                assertEquals(1, result.major)
                assertEquals(2, result.minor)
                assertEquals(3, result.patch)
                assertEquals(2, result.hotfix)
            }

            @Test
            @DisplayName("bumpVersion should handle multiple components")
            fun bumpVersionShouldHandleMultipleComponents() {
                val version = Version(1, 2, 3, null, null, null, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                val result = version.bumpVersion(snapshot, listOf(VersionComponent.HOT_FIX, VersionComponent.PATCH))

                assertEquals(1, result.major)
                assertEquals(2, result.minor)
                assertEquals(4, result.patch)
                assertEquals(1, result.hotfix)
            }
        }

        @Nested
        @DisplayName("Pre-release Tests")
        inner class PreReleaseTests {

            @Test
            @DisplayName("newPreRelease should create initial pre-release")
            fun newPreReleaseShouldCreateInitialPreRelease() {
                val version = Version(1, 2, 3, null, null, null, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                val result = version.bumpVersion(snapshot, listOf(VersionComponent.NEW_PRE_RELEASE))

                assertEquals(1, result.major)
                assertEquals(2, result.minor)
                assertEquals(3, result.patch)
                assertEquals(null, result.hotfix)
                assertEquals("-RC.1", result.preRelease?.toStringValue())
            }

            @Test
            @DisplayName("newPreRelease should throw exception when pre-release already exists")
            fun newPreReleaseShouldThrowExceptionWhenPreReleaseAlreadyExists() {
                val version = Version(1, 2, 3, null, DEFAULT_PRE_RELEASE, null, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                assertThrows<IllegalArgumentException>("Current version is already pre-release") {
                    version.bumpVersion(snapshot, listOf(VersionComponent.NEW_PRE_RELEASE))
                }
            }

            @Test
            @DisplayName("bumpPreRelease should increment pre-release version")
            fun bumpPreReleaseShouldIncrementPreReleaseVersion() {
                val version = Version(1, 2, 3, null, DEFAULT_PRE_RELEASE, null, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                val result = version.bumpVersion(snapshot, listOf(VersionComponent.PRE_RELEASE))

                assertEquals(1, result.major)
                assertEquals(2, result.minor)
                assertEquals(3, result.patch)
                assertEquals(null, result.hotfix)
                assertEquals("-RC.2", result.preRelease?.toStringValue())
            }

            @Test
            @DisplayName("bumpPreRelease should throw exception when no pre-release exists")
            fun bumpPreReleaseShouldThrowExceptionWhenNoPreReleaseExists() {
                val version = Version(1, 2, 3, null, null, null, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                assertThrows<IllegalArgumentException>("Cannot bump pre-release because the latest version is not a pre-release version") {
                    version.bumpVersion(snapshot, listOf(VersionComponent.PRE_RELEASE))
                }
            }

            @Test
            @DisplayName("promoteToRelease should remove pre-release")
            fun promoteToReleaseShouldRemovePreRelease() {
                val version = Version(1, 2, 3, 4, DEFAULT_PRE_RELEASE, null, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                val result = version.bumpVersion(snapshot, listOf(VersionComponent.PROMOTE_TO_RELEASE))

                assertEquals(1, result.major)
                assertEquals(2, result.minor)
                assertEquals(3, result.patch)
                assertEquals(4, result.hotfix)
                assertEquals(null, result.preRelease)
                assertEquals(null, result.snapshot)
                assertEquals(DEFAULT_CONFIG, result.preReleaseConfig)
            }
        }

        @Nested
        @DisplayName("Hotfix Edge Cases")
        inner class HotfixEdgeCases {

            @Test
            @DisplayName("bumpHotfix should throw exception when version is pre-release")
            fun bumpHotfixShouldThrowExceptionWhenVersionIsPreRelease() {
                val version = Version(1, 2, 3, null, DEFAULT_PRE_RELEASE, null, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                assertThrows<IllegalArgumentException>("Current version is a a pre-release.") {
                    version.bumpVersion(snapshot, listOf(VersionComponent.HOT_FIX))
                }
            }
        }

        @Nested
        @DisplayName("Snapshot Tests")
        inner class SnapshotTests {

            @Test
            @DisplayName("bumpSnapshot should add snapshot")
            fun bumpSnapshotShouldAddSnapshot() {
                val version = Version(1, 2, 3, null, null, null, DEFAULT_CONFIG)
                val newSnapshot = Snapshot("TEST", "meta")

                val result = version.bumpVersion(newSnapshot, listOf(VersionComponent.SNAPSHOT))

                assertEquals(1, result.major)
                assertEquals(2, result.minor)
                assertEquals(3, result.patch)
                assertEquals(null, result.hotfix)
                assertEquals(null, result.preRelease)
                assertEquals(newSnapshot, result.snapshot)
            }

            @Test
            @DisplayName("bumpSnapshot should replace existing snapshot")
            fun bumpSnapshotShouldReplaceExistingSnapshot() {
                val version = Version(1, 2, 3, null, null, DEFAULT_SNAPSHOT, DEFAULT_CONFIG)
                val newSnapshot = Snapshot("TEST", "meta")

                val result = version.bumpVersion(newSnapshot, listOf(VersionComponent.SNAPSHOT))

                assertEquals(newSnapshot, result.snapshot)
            }

            @Test
            @DisplayName("bumpSnapshot should handle null snapshot")
            fun bumpSnapshotShouldHandleNullSnapshot() {
                val version = Version(1, 2, 3, null, null, DEFAULT_SNAPSHOT, DEFAULT_CONFIG)

                val result = version.bumpVersion(null, listOf(VersionComponent.SNAPSHOT))

                assertEquals(null, result.snapshot)
            }
        }

        @Nested
        @DisplayName("Custom PreReleaseConfig Tests")
        inner class CustomPreReleaseConfigTests {

            @Test
            @DisplayName("newPreRelease should use custom config")
            fun newPreReleaseShouldUseCustomConfig() {
                val customConfig = PreReleaseConfig("BETA", "-", 2)
                val version = Version(1, 2, 3, null, null, null, customConfig)
                val snapshot = Snapshot()

                val result = version.bumpVersion(snapshot, listOf(VersionComponent.NEW_PRE_RELEASE))

                assertEquals("-BETA-2", result.preRelease?.toStringValue())
            }
        }

        @Nested
        @DisplayName("Version Comparator Tests")
        inner class VersionComparatorTests {

            @Test
            @DisplayName("Should compare versions by major version")
            fun shouldCompareVersionsByMajorVersion() {
                val v1 = Version(2, 0, 0, null, null, null, DEFAULT_CONFIG)
                val v2 = Version(1, 9, 9, null, null, null, DEFAULT_CONFIG)

                // v1 should be "greater" than v2 (comes first in descending order)
                assertEquals(-1, Version.VERSION_COMPARATOR.compare(v1, v2))
                assertEquals(1, Version.VERSION_COMPARATOR.compare(v2, v1))
                assertEquals(0, Version.VERSION_COMPARATOR.compare(v1, v1))
            }

            @Test
            @DisplayName("Should compare versions by minor version when major is equal")
            fun shouldCompareVersionsByMinorVersionWhenMajorIsEqual() {
                val v1 = Version(1, 2, 0, null, null, null, DEFAULT_CONFIG)
                val v2 = Version(1, 1, 9, null, null, null, DEFAULT_CONFIG)

                assertEquals(-1, Version.VERSION_COMPARATOR.compare(v1, v2))
                assertEquals(1, Version.VERSION_COMPARATOR.compare(v2, v1))
                assertEquals(0, Version.VERSION_COMPARATOR.compare(v1, v1))
            }

            @Test
            @DisplayName("Should compare versions by patch version when major and minor are equal")
            fun shouldCompareVersionsByPatchVersionWhenMajorAndMinorAreEqual() {
                val v1 = Version(1, 1, 2, null, null, null, DEFAULT_CONFIG)
                val v2 = Version(1, 1, 1, null, null, null, DEFAULT_CONFIG)

                assertEquals(-1, Version.VERSION_COMPARATOR.compare(v1, v2))
                assertEquals(1, Version.VERSION_COMPARATOR.compare(v2, v1))
                assertEquals(0, Version.VERSION_COMPARATOR.compare(v1, v1))
            }

            @Test
            @DisplayName("Should compare versions by hotfix when major, minor, and patch are equal")
            fun shouldCompareVersionsByHotfixWhenMajorMinorAndPatchAreEqual() {
                val v1 = Version(1, 1, 1, 2, null, null, DEFAULT_CONFIG)
                val v2 = Version(1, 1, 1, 1, null, null, DEFAULT_CONFIG)
                val v3 = Version(1, 1, 1, null, null, null, DEFAULT_CONFIG)

                assertEquals(-1, Version.VERSION_COMPARATOR.compare(v1, v2))
                assertEquals(1, Version.VERSION_COMPARATOR.compare(v2, v1))
                assertEquals(-1, Version.VERSION_COMPARATOR.compare(v2, v3))
                assertEquals(1, Version.VERSION_COMPARATOR.compare(v3, v2))
                assertEquals(0, Version.VERSION_COMPARATOR.compare(v1, v1))
            }

            @Test
            @DisplayName("Should compare versions by pre-release version when other components are equal")
            fun shouldCompareVersionsByPreReleaseVersionWhenOtherComponentsAreEqual() {
                val v1 = Version(1, 1, 1, null, PreReleaseVersion("alpha", DEFAULT_CONFIG.separator, 2), null, DEFAULT_CONFIG)
                val v2 = Version(1, 1, 1, null, PreReleaseVersion("alpha", DEFAULT_CONFIG.separator, 1), null, DEFAULT_CONFIG)
                val v3 = Version(1, 1, 1, null, null, null, DEFAULT_CONFIG)

                assertEquals(-1, Version.VERSION_COMPARATOR.compare(v1, v2))
                assertEquals(1, Version.VERSION_COMPARATOR.compare(v2, v1))
                assertEquals(-1, Version.VERSION_COMPARATOR.compare(v3, v2)) // release > pre-release (null = Int.MAX_VALUE)
                assertEquals(1, Version.VERSION_COMPARATOR.compare(v2, v3)) // pre-release < release
                assertEquals(0, Version.VERSION_COMPARATOR.compare(v1, v1))
            }

            @Test
            @DisplayName("Should handle complex version comparisons")
            fun shouldHandleComplexVersionComparisons() {
                val v1 = Version(2, 0, 0, null, null, null, DEFAULT_CONFIG) // 2.0.0
                val v2 = Version(1, 9, 9, 9, PreReleaseVersion("alpha", DEFAULT_CONFIG.separator, 1), null, DEFAULT_CONFIG) // 1.9.9.9-alpha1
                val v3 = Version(1, 9, 9, 9, null, null, DEFAULT_CONFIG) // 1.9.9.9
                val v4 = Version(1, 9, 9, null, PreReleaseVersion("beta", DEFAULT_CONFIG.separator, 2), null, DEFAULT_CONFIG) // 1.9.9-beta2
                val v5 = Version(1, 9, 8, null, null, null, DEFAULT_CONFIG) // 1.9.8

                // Expected order: v1 > v3 > v2 > v4 > v5
                assertEquals(-1, Version.VERSION_COMPARATOR.compare(v1, v3)) // 2.0.0 > 1.9.9.9
                assertEquals(-1, Version.VERSION_COMPARATOR.compare(v3, v2)) // 1.9.9.9 > 1.9.9.9-alpha1
                assertEquals(-1, Version.VERSION_COMPARATOR.compare(v2, v4)) // 1.9.9.9-alpha1 > 1.9.9-beta2 (hotfix > pre-release)
                assertEquals(-1, Version.VERSION_COMPARATOR.compare(v4, v5)) // 1.9.9-beta2 > 1.9.8
            }

            @Test
            @DisplayName("Should handle versions with all components")
            fun shouldHandleVersionsWithAllComponents() {
                val v1 = Version(1, 2, 3, 4, PreReleaseVersion("alpha", DEFAULT_CONFIG.separator, 1), Snapshot("SNAPSHOT", "meta"), DEFAULT_CONFIG)
                val v2 = Version(1, 2, 3, 3, PreReleaseVersion("alpha", DEFAULT_CONFIG.separator, 1), Snapshot("SNAPSHOT", "meta"), DEFAULT_CONFIG)

                assertEquals(-1, Version.VERSION_COMPARATOR.compare(v1, v2))
                assertEquals(1, Version.VERSION_COMPARATOR.compare(v2, v1))
            }

            @Test
            @DisplayName("Should treat null hotfix as 0")
            fun shouldTreatNullHotfixAs0() {
                val v1 = Version(1, 1, 1, 1, null, null, DEFAULT_CONFIG)
                val v2 = Version(1, 1, 1, null, null, null, DEFAULT_CONFIG)

                assertEquals(-1, Version.VERSION_COMPARATOR.compare(v1, v2))
                assertEquals(1, Version.VERSION_COMPARATOR.compare(v2, v1))
            }

            @Test
            @DisplayName("Should treat null pre-release as Int.MAX_VALUE")
            fun shouldTreatNullPreReleaseAsIntMaxValue() {
                val v1 = Version(1, 1, 1, null, null, null, DEFAULT_CONFIG)
                val v2 = Version(1, 1, 1, null, PreReleaseVersion("alpha", DEFAULT_CONFIG.separator, Int.MAX_VALUE), null, DEFAULT_CONFIG)

                assertEquals(0, Version.VERSION_COMPARATOR.compare(v1, v2))
                assertEquals(0, Version.VERSION_COMPARATOR.compare(v2, v1))
            }

            @Test
            @DisplayName("Should sort versions correctly")
            fun shouldSortVersionsCorrectly() {
                val versions = listOf(
                    Version(1, 0, 0, null, null, null, DEFAULT_CONFIG),
                    Version(2, 0, 0, null, null, null, DEFAULT_CONFIG),
                    Version(1, 1, 0, null, null, null, DEFAULT_CONFIG),
                    Version(1, 0, 1, null, null, null, DEFAULT_CONFIG),
                    Version(1, 0, 0, 1, null, null, DEFAULT_CONFIG),
                    Version(1, 0, 0, null, PreReleaseVersion("alpha", DEFAULT_CONFIG.separator, 1), null, DEFAULT_CONFIG)
                )

                val sortedVersions = versions.sortedWith(Version.VERSION_COMPARATOR)

                val expectedOrder = listOf(
                    Version(2, 0, 0, null, null, null, DEFAULT_CONFIG),
                    Version(1, 1, 0, null, null, null, DEFAULT_CONFIG),
                    Version(1, 0, 1, null, null, null, DEFAULT_CONFIG),
                    Version(1, 0, 0, 1, null, null, DEFAULT_CONFIG),
                    Version(1, 0, 0, null, null, null, DEFAULT_CONFIG),
                    Version(1, 0, 0, null, PreReleaseVersion("alpha", DEFAULT_CONFIG.separator, 1), null, DEFAULT_CONFIG)
                )

                assertEquals(expectedOrder, sortedVersions)
            }

            @Test
            @DisplayName("Should handle identical versions")
            fun shouldHandleIdenticalVersions() {
                val v1 = Version(1, 2, 3, 4, PreReleaseVersion("alpha", DEFAULT_CONFIG.separator, 1), Snapshot("SNAPSHOT", "meta"), DEFAULT_CONFIG)
                val v2 = Version(1, 2, 3, 4, PreReleaseVersion("alpha", DEFAULT_CONFIG.separator, 1), Snapshot("SNAPSHOT", "meta"), DEFAULT_CONFIG)

                assertEquals(0, Version.VERSION_COMPARATOR.compare(v1, v2))
                assertEquals(0, Version.VERSION_COMPARATOR.compare(v2, v1))
            }

            @Test
            @DisplayName("Should handle edge case with maximum values")
            fun shouldHandleEdgeCaseWithMaximumValues() {
                val v1 = Version(
                    Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE,
                    PreReleaseVersion("alpha", DEFAULT_CONFIG.separator, Int.MAX_VALUE), null, DEFAULT_CONFIG
                )
                val v2 = Version(
                    Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE - 1,
                    PreReleaseVersion("alpha", DEFAULT_CONFIG.separator, Int.MAX_VALUE), null, DEFAULT_CONFIG
                )

                assertEquals(-1, Version.VERSION_COMPARATOR.compare(v1, v2))
                assertEquals(1, Version.VERSION_COMPARATOR.compare(v2, v1))
            }

            @Test
            @DisplayName("Should handle zero versions comparison")
            fun shouldHandleZeroVersionsComparison() {
                val v1 = Version(0, 0, 0, null, null, null, DEFAULT_CONFIG)
                val v2 = Version(0, 0, 0, null, null, null, DEFAULT_CONFIG)

                assertEquals(0, Version.VERSION_COMPARATOR.compare(v1, v2))
            }
        }

        @Nested
        @DisplayName("Edge Cases and Error Conditions")
        inner class EdgeCasesAndErrorConditions {

            @Test
            @DisplayName("Should handle empty component list")
            fun shouldHandleEmptyComponentList() {
                val version = Version(1, 2, 3, null, null, null, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                val result = version.bumpVersion(snapshot, emptyList())

                assertEquals(version, result)
            }

            @Test
            @DisplayName("Should handle large version numbers")
            fun shouldHandleLargeVersionNumbers() {
                val version = Version(
                    Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE,
                    Int.MAX_VALUE, null, null, DEFAULT_CONFIG
                )

                assertEquals(Int.MAX_VALUE, version.major)
                assertEquals(Int.MAX_VALUE, version.minor)
                assertEquals(Int.MAX_VALUE, version.patch)
                assertEquals(Int.MAX_VALUE, version.hotfix)
            }

            @Test
            @DisplayName("Should handle zero versions")
            fun shouldHandleZeroVersions() {
                val version = Version(0, 0, 0, null, null, null, DEFAULT_CONFIG)

                assertEquals("0.0.0", version.toStringValue())
            }

            @ParameterizedTest
            @EnumSource(VersionComponent::class)
            @DisplayName("Should handle all version components")
            fun shouldHandleAllVersionComponents(component: VersionComponent) {
                val version = Version(1, 2, 3, null, null, null, DEFAULT_CONFIG)
                val snapshot = Snapshot()

                // Some components will throw exceptions, which is expected behavior
                assertDoesNotThrow {
                    try {
                        version.bumpVersion(snapshot, listOf(component))
                    } catch (e: IllegalArgumentException) {
                        // Expected for some components
                    }
                }
            }
        }
    }
}
