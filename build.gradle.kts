/*
 * Copyright (c) 2024 Team Galacticraft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version("1.3.1")
    id("com.diffplug.spotless") version("7.0.4")
}

val pluginGroup = project.property("plugin.group").toString()
val pluginName = project.property("plugin.name").toString()
val pluginId = project.property("plugin.id").toString()
val pluginDescription = project.property("plugin.description").toString()
val pluginVersion = project.property("plugin.version").toString()

val loomVersion = project.property("loom.version").toString()
val mappingIoVersion = project.property("mapping-io.version").toString()

group = pluginGroup
version = "$pluginVersion+${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"

description = pluginDescription

base.archivesName.set(pluginId)

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    withSourcesJar()
    withJavadocJar()
}

repositories {
    maven("https://maven.fabricmc.net") {
        name = "Fabric"
        content {
            includeGroup("net.fabricmc")
        }
    }

    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    compileOnly("net.fabricmc:fabric-loom:${loomVersion}")
    implementation("net.fabricmc:mapping-io:${mappingIoVersion}")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<Jar> {
    from("LICENSE")
}

tasks.javadoc {
    options.encoding = "UTF-8"
}

spotless {
    lineEndings = com.diffplug.spotless.LineEnding.UNIX

    java {
        licenseHeader(processLicenseHeader(rootProject.file("LICENSE")))
        leadingTabsToSpaces()
        removeUnusedImports()
        trimTrailingWhitespace()
    }
}

gradlePlugin {
    website.set("https://github.com/TeamGalacticraft/mojarn")
    vcsUrl.set("https://github.com/TeamGalacticraft/mojarn")

    plugins {
        create(pluginId) {
            id = "${pluginGroup}.${pluginId}"
            displayName = pluginName
            description = pluginDescription
            implementationClass = "dev.galacticraft.mojarn.impl.MojarnPlugin"
        }
    }
}

publishing {
    repositories {
        if (System.getenv().containsKey("NEXUS_REPOSITORY_URL")) {
            maven(System.getenv("NEXUS_REPOSITORY_URL")!!) {
                credentials {
                    username = System.getenv("NEXUS_USER")
                    password = System.getenv("NEXUS_PASSWORD")
                }
            }
        }
    }
}

fun processLicenseHeader(license: File): String {
    val text = license.readText()
    return "/*\n * " + text.substring(text.indexOf("Copyright"))
        .replace("\n", "\n * ")
        .replace("* \n", "*\n")
        .trim() + "/\n\n"
}
