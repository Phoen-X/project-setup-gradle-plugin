plugins {
    kotlin("jvm") version "1.8.20"
    `java-gradle-plugin`
    `kotlin-dsl`
}

group = "com.vyhuliarnyi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:3.20.2")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create(project.name) {
            id = project.name
            group = project.group
            implementationClass = "com.vyhuliarnyi.MyPlugin"
        }
    }
}

kotlin {
    jvmToolchain(17)
}