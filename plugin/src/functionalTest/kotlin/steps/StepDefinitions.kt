package steps

import com.alphasystem.gradle.semver.release.test.*
import com.alphasystem.gradle.semver.release.common.TestRepository
import com.alphasystem.gradle.semver.release.internal.SemanticBuildVersion
import com.alphasystem.gradle.semver.release.internal.SemanticBuildVersionConfiguration
import io.cucumber.java.ParameterType
import io.cucumber.java.en.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.nio.file.Files
import java.util.UUID

class StepDefinitions {

    private val workingDirectory = Files.createTempDirectory(UUID.randomUUID().toString()).toFile()
    private val repository = TestRepository(workingDirectory)
    private val adapter = repository.getAdapter()
    private var config: SemanticBuildVersionConfiguration = SemanticBuildVersionConfiguration()
    private var mainBranchName = ""

    @ParameterType(".*", name = "bool")
    fun toBooleanParameter(value: String?): Boolean = value?.toBoolean() ?: false

    @Given("Load semantic build config from \\({})")
    fun loadSemanticBuildVersionConfiguration(conf: String) {
        config = toSemanticBuildVersionConfiguration(conf)
    }

    @Given("Record main branch")
    fun recordMainBranchName() {
        mainBranchName = repository.getBranchName()
    }

    @Given("Following annotated: {bool} tags \\({}) has been created")
    fun commitAndTag(annotated: Boolean, tags: String) = tags.split(",").forEach { tag -> repository.commitAndTag(tag, annotated) }

    @When("Branch {string} is created and checked out")
    fun createAndCheckout(branchName: String) = repository.createAndCheckout(branchName)

    @When("Main branch is checked out")
    fun checkoutMainBranch() = repository.checkoutBranch(mainBranchName)

    @When("Branch {string} is checked out")
    fun checkoutBranch(branchName: String) = repository.checkoutBranch(branchName)

    @When("Merge branch {string} into current branch")
    fun mergeBranch(branchName: String) = repository.merge(branchName)

    @When("Make some changes")
    fun makeChanges() = repository.makeChanges()

    @When("Commit with message: {string}")
    fun commitWithMessage(commitMessage: String) = repository.commit(commitMessage)

    @When("Make changes and commit with message: {string}")
    fun makeChangesAndCommit(commitMessage: String) = repository.makeChanges().commit(commitMessage)

    @When("A Tag {string} has been checked out")
    fun checkoutTag(tag: String) = repository.checkoutTag("$tag+", tag)

    @When("A tag with annotated: \\({bool}) flag is created")
    fun createAnnotatedTag(annotated: Boolean) {
        repository.tag(SemanticBuildVersion(workingDirectory, config).determineVersion(), annotated)
    }

    @When("No changes made to repository")
    fun noOp() {
    }

    @Then("Exception '{}' should be thrown when creating new tag")
    fun exceptionIsThrown(message: String) {
        try {
            repository.tag(SemanticBuildVersion(workingDirectory, config).determineVersion())
        } catch (e: Exception) {
            assertEquals(message, e.message)
        }
    }

    @Then("Generated version should be {string}")
    fun assertVersion(expectedVersion: String) {
        val snapshotConfig = config.snapshotConfig
        val currentTag = adapter.getCurrentHeadTag(
            tagPrefix = config.tagPrefix,
            snapshotSuffix = snapshotConfig.prefix,
            preReleaseConfig = config.preReleaseConfig
        )
        val result =
            if (currentTag?.contains(snapshotConfig.prefix) == true) {
                if (snapshotConfig.appendCommitHash) {
                    val hash =
                        if (snapshotConfig.useShortHash) adapter.getShortHash()
                        else adapter.getFullHash()

                    "$expectedVersion+$hash"
                } else expectedVersion
            } else expectedVersion

        assertEquals(result, currentTag)
    }

    @Then("Close resources")
    fun close() {
        runCatching { repository.close() }
        runCatching { workingDirectory.deleteRecursively() }
    }
}
