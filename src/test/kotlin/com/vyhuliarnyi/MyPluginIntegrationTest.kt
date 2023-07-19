package com.vyhuliarnyi

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MyPluginIntegrationTest {

    @TempDir
    lateinit var testProjectDir: File
    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var propertiesFile: File

    @BeforeEach
    fun setup() {
        settingsFile = File(testProjectDir, "settings.gradle.kts")
        buildFile = File(testProjectDir, "build.gradle.kts")
        propertiesFile = File(testProjectDir, "gradle.properties")

        settingsFile.writeText("rootProject.name = \"my-plugin-test\"")
    }

    @Test
    fun `plugin configures repositories`() {
        propertiesFile.writeText(
            """
                myRepoUsername=user
                myRepoPassword=pass
                """.trimIndent()
        )

        buildFile.writeText(
            scriptContents(
                adjustment = """
                    myProject {
                        repo("myRepo", "http://localhost")
                    }                
            """.trimIndent()
            )
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("printRepos")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("Repo: myRepo, URL: http://localhost"))
    }

    @Test
    fun `properties are not set`() {
        val repoId = "myRepo"
        propertiesFile.writeText("${repoId}Password=some_pass")
        buildFile.writeText(
            scriptContents(
                adjustment = """
                    myProject {
                        repo("$repoId", "http://localhost")
                    }
                    """.trimIndent()
            )
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("printRepos")
            .withPluginClasspath()
            .buildAndFail()

        assertTrue(result.output.contains("\"${repoId}Username\" is not available"))
    }

    @Test
    fun `repos block is not defined`() {
        buildFile.writeText(
            """
                plugins {
                    id("project-setup-gradle-plugin")
                }
                
                tasks.create("hello") {
                    doLast {
                        println("Hello")
                    }
                }
                """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("hello")
            .withPluginClasspath()
            .build()

        val myTaskResult = result.task(":hello")
        assertNotNull(myTaskResult)
        assertEquals(TaskOutcome.SUCCESS, myTaskResult.outcome)

        assertTrue(result.output.contains("Hello"), "Output should contain 'Hello'")
    }

    @Test
    fun `additional repo is defined manually`() {
        val repoId = "myRepo"
        val repoUrl = "http://localhost"
        propertiesFile.writeText(
            """
                ${repoId}Username=user
                ${repoId}Password=pass
                """.trimIndent()
        )

        buildFile.writeText(
            scriptContents(
                adjustment =
                """
                    myProject {
                        repo("$repoId", "$repoUrl")
                    }
                    
                    repositories {
                        maven {
                            name = "Maven2"
                            url = uri("http://localhost:3000")
                        }
                    }
                """.trimIndent()
            )
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("printRepos")
            .withPluginClasspath()
            .build()

        val printReposResult = result.task(":printRepos")
        assertNotNull(printReposResult)
        assertEquals(TaskOutcome.SUCCESS, printReposResult.outcome)
        val expectedOutput = "Repo: Maven2, URL: http://localhost:3000"
        assertTrue(result.output.contains(expectedOutput), "Output should contain '$expectedOutput'")
    }

    private fun scriptContents(
        path: String = "scripts/successful_config/build.gradle.kts.template",
        adjustment: String
    ) =
        this::class.java.classLoader.getResource(path)!!
            .readText()
            .replace("//%CONTENT%", adjustment)
}