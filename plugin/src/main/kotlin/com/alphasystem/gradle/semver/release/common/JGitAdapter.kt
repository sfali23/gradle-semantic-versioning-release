package com.alphasystem.gradle.semver.release.common

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.PushResult
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder
import org.eclipse.jgit.util.FS
import java.io.File
import java.io.IOException
import java.util.*
import java.util.stream.Collectors
import java.util.stream.StreamSupport

/**
 * Provides an adapter for interacting with a Git repository using the JGit library. This class
 * supports various Git-related operations such as retrieving commits, branches, tags, and analyzing
 * repository states.
 */
class JGitAdapter(workingDir: File, initialize: Boolean = false) {
    private val repository = initRepository(workingDir, initialize)
    private val git = Git(repository)

    fun getRepository(): Repository = repository

    fun getGit(): Git = git

    @Throws(IOException::class)
    fun getHeadCommit(): ObjectId = repository.resolve(Constants.HEAD)!!

    @Throws(IOException::class)
    private fun getShortHash(id: ObjectId): String = repository.newObjectReader().abbreviate(id).name()

    @Throws(IOException::class)
    fun getShortHash(): String = getShortHash(getHeadCommit())

    @Throws(IOException::class)
    fun getCurrentBranch(): String = repository.branch

    private fun getRevWalk(): RevWalk = RevWalk(repository)

    @Throws(GitAPIException::class)
    fun getTagsForCurrentBranch(): List<String> {
        val tags = git.tagList().call().stream()
            .collect(Collectors.groupingBy { tagRef: Ref -> getRevWalk().parseCommit(getNonNullObjectId(tagRef)).id })

        val ref = repository.resolve(repository.branch)
        if (ref != null) {
            return StreamSupport.stream(git.log().add(ref).call().spliterator(), false)
                .flatMap { rev -> tags.getOrDefault(rev.id, emptyList()).stream() }
                .map { tagRef: Ref -> tagRef.name.replace(Constants.R_TAGS, "") }
                .collect(Collectors.toList())
        }
        return emptyList()
    }

    fun getTag(tag: String): Ref? = repository.findRef("${Constants.R_TAGS}$tag")

    fun createTag(tag: String, annotated: Boolean, message: String?): Ref? =
        git.tag()
            .setName(tag)
            .setForceUpdate(true)
            .setAnnotated(annotated)
            .let { if (annotated) it.setMessage(message) else it }
            .call()

    fun createTag(tag: String, annotated: Boolean): Ref? = createTag(tag, annotated, null)

    fun pushTag(tag: String): List<PushResult> {
        configureJGitSsh()
        val ref = getTag(tag) ?: throw RuntimeException("Tag $tag not found")
        return git.push().add(ref).call().toList().filterNotNull()
    }

    fun getUnReleasedCommits(start: String): List<String> = getUnReleasedCommits(start, Constants.HEAD)

    fun getUnReleasedCommits(start: String, end: String): List<String> {
        return try {
            val startId = repository.resolve(start)!!
            val endId = repository.resolve(end)!!
            val walk = getRevWalk()

            val startCommit = walk.parseCommit(startId)
            val endCommit = walk.parseCommit(endId)

            StreamSupport.stream(
                git.log().addRange(startCommit, endCommit).call().spliterator(), false
            )
                .map { commit: RevCommit ->
                    val shortHash = try {
                        getShortHash(commit.id)
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                    val message = commit.shortMessage
                    "Commit($shortHash, $message)"
                }
                .collect(Collectors.toList())
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getCommitBetween(start: String): List<String> {
        return getCommitBetween(start, Constants.HEAD)
    }

    fun getCommitBetween(start: String, end: String): List<String> {
        return try {
            val startId = repository.resolve(start)!!
            val endId = repository.resolve(end)!!
            val walk = getRevWalk()

            val startCommit = walk.parseCommit(startId)
            val endCommit = walk.parseCommit(endId)

            StreamSupport.stream(
                git.log().addRange(startCommit, endCommit).call().spliterator(), false
            )
                .map { obj: RevCommit -> obj.fullMessage }
                .collect(Collectors.toList())
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun hasUncommittedChanges(): Boolean {
        return try {
            git.status().call().hasUncommittedChanges()
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private fun initRepository(workingDir: File, initialize: Boolean): Repository {
            return try {
                if (initialize) {
                    Git.init().setDirectory(workingDir).call()
                }

                val builder = FileRepositoryBuilder()
                    .setWorkTree(workingDir)
                    .findGitDir(workingDir)

                val gitDir = builder.gitDir
                if (gitDir == null || !gitDir.exists()) {
                    throw RuntimeException(
                        "Unable to find Git repository in: " + workingDir.absolutePath
                    )
                }

                if (gitDir.parentFile.absolutePath != workingDir.absolutePath) {
                    builder.workTree = gitDir.parentFile
                }

                builder.build()
            } catch (e: Exception) {
                throw RuntimeException("Failed to initialize repository", e)
            }
        }

        private fun getNonNullObjectId(ref: Ref): ObjectId {
            return Optional.ofNullable(ref.peeledObjectId).orElseGet { ref.objectId }
        }

        private fun configureJGitSsh() {
            if (SshSessionFactory.getInstance() != null) {
                return
            }

            val sshSessionFactory = SshdSessionFactoryBuilder()
                .setHomeDirectory(FS.DETECTED.userHome())
                .setSshDirectory(FS.DETECTED.userHome().resolve(".ssh"))
                .build(null)

            SshSessionFactory.setInstance(sshSessionFactory)
        }
    }
}
