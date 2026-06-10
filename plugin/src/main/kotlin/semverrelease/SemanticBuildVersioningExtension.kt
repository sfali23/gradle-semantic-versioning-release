package semverrelease

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

abstract class SemanticBuildVersioningExtension(project: Project) {

    private val objects = project.objects

    // This option defines the starting version of the build in case there is no tag available to determine the next version.
    // The default value is "0.1.0".
    @Input
    val startingVersion: Property<String> = objects.property(String::class.java)

    @Input
    val tagPrefix: Property<String> = objects.property(String::class.java)

    @Input
    val forceBump: Property<Boolean> = objects.property(Boolean::class.java)

    @Input
    val newPreRelease: Property<Boolean> = objects.property(Boolean::class.java)

    @Input
    val promoteToRelease: Property<Boolean> = objects.property(Boolean::class.java)

    @Input
    val snapshot: Property<Boolean> = objects.property(Boolean::class.java)

    @Input
    val defaultBumpLevel: Property<ComponentToBump> = objects.property(ComponentToBump::class.java)

    @Input
    val componentToBump: Property<ComponentToBump> = objects.property(ComponentToBump::class.java)

    @get:Nested
    val autoBump: Property<AutoBump> = objects.property(AutoBump::class.java)

    @get:Nested
    val snapshotConfig: Property<SnapshotConfig> = objects.property(SnapshotConfig::class.java)

    @get:Nested
    val preReleaseConfig: Property<PreReleaseConfig> = objects.property(PreReleaseConfig::class.java)

    @Input
    val hotfixBranchPattern: Property<Regex> = objects.property(Regex::class.java)

    @Input
    val extraReleaseBranches: ListProperty<String> = objects.listProperty(String::class.java)

    @Input
    @Optional
    val releaseTagComment: Property<String> = objects.property(String::class.java).convention("Releasing")

    @Input
    val addUnReleasedCommitsToTagComment: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

}
