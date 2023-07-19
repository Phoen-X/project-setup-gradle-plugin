package com.vyhuliarnyi

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.kotlin.dsl.create
import java.util.*

data class Repo(val id: String, val url: String)

open class MyProjectExtension {
    val repos = mutableListOf<Repo>()

    // method to create and add repos with auto-generated id
    fun repo(url: String) {
        val id = url.split("/").joinToString("") { it.replaceFirstChar { s -> if (s.isLowerCase()) s.titlecase(Locale.ENGLISH) else s.toString() } }
        repos.add(Repo(id, url))
    }

    // method to create and add repos with provided id
    fun repo(id: String, url: String) {
        repos.add(Repo(id, url))
    }
}

class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(JavaLibraryPlugin::class.java)
        project.plugins.apply(CheckstylePlugin::class.java)
        val myProjectExtension = project.extensions.create<MyProjectExtension>("myProject")

        project.afterEvaluate {
            myProjectExtension.repos.forEach { (id, url) ->
                configureMavenRepo(project, id, url)
            }
        }
    }

    private fun configureMavenRepo(project: Project, id: String, url: String) {
        project.repositories.maven {
            name = id
            setUrl(project.uri(url))
            credentials {
                username = findProperty(project, id, "Username")
                password = findProperty(project, id, "Password")
            }
        }
    }

    private fun findProperty(project: Project, id: String, propertyName: String): String {
        val fullPropertyName = "$id$propertyName"
        return project.findProperty(fullPropertyName) as? String
            ?: throw GradleException("\"$fullPropertyName\" is not available. Please define it in `gradle.properties` file")
    }

}