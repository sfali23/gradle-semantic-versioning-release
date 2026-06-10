package semverrelease

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SemanticBuildVersioningPluginTest {

    @Test fun `plugin registers extension`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.sfali23.gradle-semantic-versioning-release")
        assertNotNull(project.extensions.findByName("semverrelease"))

        val extension = project.extensions.getByName("semverrelease") as SemanticBuildVersioningExtension
        extension.startingVersion.set("1.0.0")

        assertEquals("1.0.0", extension.startingVersion.get(), "Starting version is not set correctly")
    }
}
