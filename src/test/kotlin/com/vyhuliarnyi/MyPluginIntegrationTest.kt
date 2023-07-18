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
    }

    @Test
    fun `plugin configures repositories`() {
        val repoId = "myRepo"
        val repoUrl = "http://localhost"
        val usernameProp = "${repoId}Username"
        val passwordProp = "${repoId}Password"
        val username = "user"
        val password = "pass"

        propertiesFile.writeText(
            """
        $usernameProp=$username
        $passwordProp=$password
    """
        )

        settingsFile.writeText("rootProject.name = \"my-plugin-test\"")
        buildFile.writeText(
            """
        plugins {
            id("project-setup-gradle-plugin")
        }
        
        myProject {
            repos.set(mapOf("$repoId" to "$repoUrl"))
        }
        
          tasks.create("printRepos") {
              doLast {
                  repositories.forEach {
                      if (it is MavenArtifactRepository) {
                          println("Repo: ${'$'}{it.name}, URL: ${'$'}{it.url}")
                      }
                  }
              }
          }
    """
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("printRepos")
            .withPluginClasspath()
            .build()

        assert(result.output.contains("Repo: $repoId, URL: $repoUrl"))
    }

    @Test
    fun `properties are not set`() {
        val repoId = "myRepo"

        settingsFile.writeText("rootProject.name = \"my-plugin-test\"")
        buildFile.writeText(
            """
    plugins {
        id("project-setup-gradle-plugin")
    }
    
    myProject {
        repos.set(mapOf("$repoId" to "http://localhost"))
    }
 
    dependencies {
       compileOnly("some.library:to.download:1.0")
    }
    
    tasks.create("printRepos") {
        doLast {
          repositories.forEach {
              if (it is MavenArtifactRepository) {
                  println("Repo: ${'$'}{it.name}, URL: ${'$'}{it.url}")
              }
          }
        }
    }
"""
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("printRepos")
            .withPluginClasspath()
            .buildAndFail()

        assertTrue(
            result.output.contains("\"${repoId}Username\" is not available")
        )
    }

    @Test
    fun `repos block is not defined`() {
        settingsFile.writeText("rootProject.name = \"my-plugin-test\"")
        buildFile.writeText(
            """
    plugins {
        id("project-setup-gradle-plugin")
    }
"""
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("myTask")
            .withPluginClasspath()
            .build()

        val myTaskResult = result.task(":myTask")
        assertNotNull(myTaskResult)
        assertEquals(TaskOutcome.SUCCESS, myTaskResult.outcome)

        assertTrue(result.output.contains("Hello from MyPlugin!"), "Output should contain 'Hello from MyPlugin!'")
    }

    @Test
    fun `additional repo is defined manually`() {
        val repoId = "myRepo"
        val repoUrl = "http://localhost"
        propertiesFile.writeText(
            """
        ${repoId}Username=user
        ${repoId}Password=pass
    """
        )

        settingsFile.writeText("rootProject.name = \"my-plugin-test\"")
        buildFile.writeText(
            """
    plugins {
        id("project-setup-gradle-plugin")
    }
    
    myProject {
        repos.set(mapOf("$repoId" to "$repoUrl"))
    }
    
    repositories {
        maven {
            name = "Maven2"
            url = uri("http://localhost:3000")
        }
    }
    
    tasks.create("printRepos") {
        doLast {
            repositories.forEach {
                if (it is MavenArtifactRepository) {
                    println("Repo: ${'$'}{it.name}, URL: ${'$'}{it.url}")
                }
            }
        }
    }
"""
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
}