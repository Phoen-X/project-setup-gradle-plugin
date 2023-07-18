package com.vyhuliarnyi

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.kotlin.dsl.create

open class MyProjectExtension(project: Project) {
    val repos: MapProperty<String, String> = project.objects.mapProperty(String::class.java, String::class.java)
}

@Suppress("unused")
class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        applyPlugins(project)
        val myProjectExtension = addCustomExtension(project)
        configureReposAfterEvaluation(project, myProjectExtension)
    }

    private fun applyPlugins(project: Project) {
        with(project.pluginManager) {
            apply("java-library")
            apply("jacoco")
        }
    }

    private fun addCustomExtension(project: Project): MyProjectExtension =
        project.extensions.create<MyProjectExtension>("myProject", project)

    private fun configureReposAfterEvaluation(project: Project, myProjectExtension: MyProjectExtension) {
        project.afterEvaluate {
            myProjectExtension.repos.orNull?.forEach { (id, url) ->
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