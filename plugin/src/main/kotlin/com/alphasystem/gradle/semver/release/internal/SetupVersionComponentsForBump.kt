package com.alphasystem.gradle.semver.release.internal

import semverrelease.AutoBump

class SetupVersionComponentsForBump {

    private var result: Int = VersionComponent.NONE.index

    fun parseMessage(commitMessage: String, autoBump: AutoBump): SetupVersionComponentsForBump {
        if (autoBump.major(commitMessage)) addMajor()
        if (autoBump.minor(commitMessage)) addMinor()
        if (autoBump.patch(commitMessage)) addPatch()
        if (autoBump.newPreRelease(commitMessage)) addNewPreRelease()
        if (autoBump.promoteToRelease(commitMessage)) addPromoteToRelease()
        return this
    }

    private fun addMajor(): SetupVersionComponentsForBump = addComponent(VersionComponent.MAJOR)
    fun removeMajor(): SetupVersionComponentsForBump = removeComponent(VersionComponent.MAJOR)

    private fun addMinor(): SetupVersionComponentsForBump = addComponent(VersionComponent.MINOR)
    fun removeMinor(): SetupVersionComponentsForBump = removeComponent(VersionComponent.MINOR)

    fun addPatch(): SetupVersionComponentsForBump = addComponent(VersionComponent.PATCH)
    fun removePatch(): SetupVersionComponentsForBump = removeComponent(VersionComponent.PATCH)

    fun addHotFix(): SetupVersionComponentsForBump = addComponent(VersionComponent.HOT_FIX)
    fun removeHotFix(): SetupVersionComponentsForBump = removeComponent(VersionComponent.HOT_FIX)

    private fun addNewPreRelease(): SetupVersionComponentsForBump = addComponent(VersionComponent.NEW_PRE_RELEASE)
    fun removeNewPreRelease(): SetupVersionComponentsForBump = removeComponent(VersionComponent.NEW_PRE_RELEASE)

    fun addPreRelease(): SetupVersionComponentsForBump = addComponent(VersionComponent.PRE_RELEASE)
    fun removePreRelease(): SetupVersionComponentsForBump = removeComponent(VersionComponent.PRE_RELEASE)

    fun addPromoteToRelease(): SetupVersionComponentsForBump = addComponent(VersionComponent.PROMOTE_TO_RELEASE)
    fun removePromoteToRelease(): SetupVersionComponentsForBump = removeComponent(VersionComponent.PROMOTE_TO_RELEASE)

    fun addSnapshot(): SetupVersionComponentsForBump = addComponent(VersionComponent.SNAPSHOT)

    fun hasMajor(): Boolean = hasGivenComponent(VersionComponent.MAJOR)
    fun hasMinor(): Boolean = hasGivenComponent(VersionComponent.MINOR)
    fun hasPatch(): Boolean = hasGivenComponent(VersionComponent.PATCH)
    fun hasPromoteToRelease(): Boolean = hasGivenComponent(VersionComponent.PROMOTE_TO_RELEASE)
    fun hasPreRelease(): Boolean = hasGivenComponent(VersionComponent.PRE_RELEASE)
    fun hasMandatoryComponents(): Boolean = hasMajor() || hasMinor() || hasPatch()
    fun hasEssentialComponents(): Boolean = hasMajor() || hasMinor() || hasPatch() || hasPromoteToRelease() ||
            hasPreRelease() || hasGivenComponent(VersionComponent.HOT_FIX)

    fun reset(): SetupVersionComponentsForBump {
        result = VersionComponent.NONE.index
        return this
    }

    fun addComponentIfRequired(versionComponent: VersionComponent, condition: () -> Boolean): SetupVersionComponentsForBump =
        if (condition()) addComponent(versionComponent) else this

    fun getVersionComponents(): List<VersionComponent> =
        setOf(
            VersionComponent.fromIndex(result and VersionComponent.MAJOR.index),
            VersionComponent.fromIndex(result and VersionComponent.MINOR.index),
            VersionComponent.fromIndex(result and VersionComponent.PATCH.index),
            VersionComponent.fromIndex(result and VersionComponent.HOT_FIX.index),
            VersionComponent.fromIndex(result and VersionComponent.NEW_PRE_RELEASE.index),
            VersionComponent.fromIndex(result and VersionComponent.PRE_RELEASE.index),
            VersionComponent.fromIndex(result and VersionComponent.PROMOTE_TO_RELEASE.index),
            VersionComponent.fromIndex(result and VersionComponent.SNAPSHOT.index)
        ).toList().filterNotNull()

    private fun addComponent(versionComponent: VersionComponent): SetupVersionComponentsForBump {
        result = result or (versionComponent.index or VersionComponent.NONE.index)
        return this
    }

    private fun removeComponent(versionComponent: VersionComponent): SetupVersionComponentsForBump {
        result = result and (versionComponent.index or VersionComponent.NONE.index).inv()
        return this
    }

    private fun hasGivenComponent(versionComponent: VersionComponent) =
        (result and versionComponent.index) == versionComponent.index
}
