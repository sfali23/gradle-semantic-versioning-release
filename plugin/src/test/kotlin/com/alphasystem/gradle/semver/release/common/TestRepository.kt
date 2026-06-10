package com.alphasystem.gradle.semver.release.common

import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.util.*

class TestRepository(val workingDirectory: File, initialize: Boolean = true) {

    private val adapter = JGitAdapter(workingDirectory, initialize)
    private val repository = adapter.getRepository()
    private val git = adapter.getGit()
    private val random = Random()

    fun getAdapter(): JGitAdapter = adapter

    fun close() {
        repository.close()
    }

    fun commitAndTag(tag: String, annotated: Boolean): TestRepository {
        return commit().tag(tag, annotated)
    }

    fun tag(tag: String): TestRepository {
        return tag(tag, false)
    }

    fun tag(tag: String, annotated: Boolean, message: String? = null): TestRepository {
        adapter.createTag(tag, annotated, message)
        return this
    }

    fun commit(): TestRepository {
        return commit("blah")
    }

    fun commit(message: String): TestRepository {
        try {
            git.commit().setAuthor("Batman", "batman@waynemanor.com").setMessage(message).call()
        } catch (e: Exception) {
            throw RuntimeException("Failed to create commit: $message", e)
        }
        return this
    }

    fun makeChanges(): TestRepository {
        val fileName = "file-" + generateRandomString(5)
        val file = File(workingDirectory, fileName)
        try {
            PrintWriter(file).use { writer ->
                writer.write(generateRandomString(50))
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to write file: $fileName", e)
        }

        try {
            git.add().addFilepattern(fileName).call()
        } catch (e: Exception) {
            throw RuntimeException("Failed to add file: $fileName", e)
        }
        return this
    }

    fun add(filePattern: String): TestRepository {
        try {
            git.add().addFilepattern(filePattern).call()
        } catch (e: Exception) {
            throw RuntimeException("Failed to add file pattern: $filePattern", e)
        }
        return this
    }

    fun checkoutTag(branchName: String, tagName: String): TestRepository {
        try {
            val ref = git.checkout()
                .setCreateBranch(true)
                .setName(branchName)
                .setStartPoint(tagName)
                .call()

            logger.trace("Checkout tag: {}, current branch: {}", ref.name, getBranchName())
        } catch (e: Exception) {
            throw RuntimeException(
                "Failed to checkout tag: $tagName to branch: $branchName", e
            )
        }
        return this
    }

    fun checkoutBranch(branch: String): TestRepository {
        try {
            git.checkout().setName(branch).call()
        } catch (e: Exception) {
            throw RuntimeException("Failed to checkout branch: $branch", e)
        }
        return this
    }

    fun branch(name: String): TestRepository {
        try {
            git.branchCreate().setName(name).call()
        } catch (e: Exception) {
            throw RuntimeException("Failed to create branch: $name", e)
        }
        return this
    }

    fun createAndCheckout(name: String): TestRepository {
        return branch(name).checkoutBranch(name)
    }

    fun getBranchName(): String {
        return try {
            repository.branch
        } catch (e: IOException) {
            throw RuntimeException("Failed to get branch name", e)
        }
    }

    fun merge(target: String): TestRepository {
        try {
            val result = git.merge().include(repository.findRef(target)).call()
            logger.trace(
                "Result of merge from {} to {} was {}",
                getBranchName(),
                target,
                result.mergeStatus.name
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to merge from: $target", e)
        }
        return this
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TestRepository::class.java)
    }

    private fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val sb = StringBuilder()
        for (i in 0 until length) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }
}
