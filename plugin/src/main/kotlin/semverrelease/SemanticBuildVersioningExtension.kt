package semverrelease

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

abstract class SemanticBuildVersioningExtension {

    // This option defines the starting version of the build in case there is no tag available to determine the next version.
    // The default value is "0.1.0".
    @get:Input
    abstract val startingVersion: Property<String>

    @get:Input
    abstract val tagPrefix: Property<String>

    @get:Input
    abstract val forceBump: Property<Boolean>

    @get:Input
    abstract val newPreRelease: Property<Boolean>

    @get:Input
    abstract val promoteToRelease: Property<Boolean>

    @get:Input
    abstract val snapshot: Property<Boolean>

    @get:Input
    abstract val defaultBumpLevel: Property<ComponentToBump>

    @get:Input
    abstract val componentToBump: Property<ComponentToBump>

    @get:Nested
    abstract val autoBump: Property<AutoBump>

    @get:Nested
    abstract val snapshotConfig: Property<SnapshotConfig>

    @get:Nested
    abstract val preReleaseConfig: Property<PreReleaseConfig>

    @get:Input
    abstract val hotfixBranchPattern: Property<Regex>

    @get:Input
    abstract val extraReleaseBranches: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val releaseTagComment: Property<String>

    @get:Input
    abstract val addUnReleasedCommitsToTagComment: Property<Boolean>

}
