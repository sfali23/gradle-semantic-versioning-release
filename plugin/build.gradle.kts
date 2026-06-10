plugins {
    `java-gradle-plugin`
    jacoco

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publish)
    alias(libs.plugins.spotless)
    alias(libs.plugins.gradle.semver.release)
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

group = "io.github.sfali23"

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.35.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        // Custom rule to replace 3+ newlines with just 2
        replaceRegex("Remove extra newlines", "\\n\\n\\n+", "\n\n")
    }

    kotlin {
        target("src/**/*.kt")
        trimTrailingWhitespace()
        endWithNewline()
        // Custom rule to replace 3+ newlines with just 2
        replaceRegex("Remove extra newlines", "\\n\\n\\n+", "\n\n")
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}

val createPluginClasspathFile by tasks.registering {
    inputs.files(sourceSets.main.get().runtimeClasspath)
    outputs.dir(temporaryDir)
    doLast {
        file("$temporaryDir/plugin-classpath.txt").writeText(
            sourceSets.main
                .get()
                .runtimeClasspath
                .joinToString("\n"),
        )
    }
}

configurations {
    create("jacocoRuntime")
}

val createJacocoAgentClasspathFile by tasks.registering {
    inputs.files(configurations["jacocoRuntime"])
    outputs.dir(temporaryDir)
    doLast {
        val jacocoAgentClasspathFile = file("$temporaryDir/jacoco-agent-classpath.txt")
        jacocoAgentClasspathFile.writeText(
            """|${configurations["jacocoRuntime"].asPath}
               |${tasks.jacocoTestReport.get().reports.xml.outputLocation.get().asFile.absolutePath}
            """.trimMargin(),
        )
    }
}

dependencies {
    implementation(libs.jgit)

    testImplementation(libs.jgit.junit)
    testImplementation(libs.typesafe)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.suit)
    testRuntimeOnly(libs.junit.platform.launcher)

    add("jacocoRuntime", "org.jacoco:org.jacoco.agent:${jacoco.toolVersion}:runtime")

    testRuntimeOnly(files(createPluginClasspathFile.get()))
    testRuntimeOnly(files(createJacocoAgentClasspathFile.get()))
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Kotlin Test test framework
            useKotlinTest("2.2.0")
        }

        // Create a new test suite
        val functionalTest by registering(JvmTestSuite::class) {
            // Use Kotlin Test test framework
            useKotlinTest("2.2.0")

            dependencies {
                // functionalTest test suite depends on the production code in tests
                implementation(project())
                implementation(sourceSets.test.get().output)
                implementation(libs.jgit)
                implementation(libs.typesafe)
                implementation(libs.cucumber.java)
                implementation(libs.cucumber.expressions)
                implementation(libs.cucumber.junit.platform.engine)
                implementation(libs.junit.platform.suit)
            }

            targets {
                all {
                    // This test suite should run after the built-in test suite has run its tests
                    testTask.configure { shouldRunAfter(test) }
                }
            }
        }
    }
}

gradlePlugin {
    website = "https://github.com/sfali23/gradle-semantic-versioning-release"
    vcsUrl = "https://github.com/sfali23/gradle-semantic-versioning-release"
    plugins {
        create("semanticBuildVersioningPlugin") {
            id = "io.github.sfali23.gradle-semantic-versioning-release"
            implementationClass = "semverrelease.SemanticBuildVersioningPlugin"
            displayName = "Gradle Semantic Build Versioning Plugin"
            description =
                "This is a Gradle settings-plugin that provides support for semantic versioning of builds. It is quite easy to use and extremely configurable. The plugin allows you to bump the major, minor, patch or pre-release version based on the latest version, which is identified from a git tag. It also allows you to bump pre-release versions based on a scheme that you define. The version can be bumped by using version-component-specific project properties or can be bumped automatically based on the contents of a commit message. If no manual bumping is done via commit message or project property, the plugin will increment the version-component with the lowest precedence; this is usually the patch version, but can be the pre-release version if the latest version is a pre-release one. The plugin does its best to ensure that you do not accidentally violate semver rules while generating your versions; in cases where this might happen the plugin forces you to be explicit about violating these rules. As this is a settings plugin, it is applied to settings.gradle and version calculation is therefore performed right at the start of the build, before any projects are configured. This means that the project version is immediately available (almost as if it were set explicitly - which it effectively is), and will never change during the build (barring some other, external task that attempts to modify the version during the build). While the build is running, tagging or changing the project properties will not influence the version that was calculated at the start of the build."
            tags.set(listOf("versioning", "semantic-versioning", "git", "build-versioning", "auto-versioning", "version"))
        }
    }
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}

tasks.processTestResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Task>("check") {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("functionalTest"))
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates("io.github.sfali23", "gradle-semantic-versioning-release")

    pom {
        name.set("Gradle Semantic Build Versioning Plugin")
        description.set("This is a Gradle settings-plugin that provides support for semantic versioning of builds.")
        url.set("https://github.com/sfali23/gradle-semantic-versioning-release")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("sfali23")
                name.set("Syed Farhan Ali")
                email.set("f.syed.ali@gmail.com")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/sfali23/gradle-semantic-versioning-release.git")
            developerConnection.set("scm:git:ssh://github.com:sfali23/gradle-semantic-versioning-release.git")
            url.set("https://github.com/sfali23/gradle-semantic-versioning-release/tree/main")
        }
    }
}
