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
    id("com.gradle.plugin-publish") version("1.2.1")
    id("org.cadixdev.licenser") version("0.6.1")
}

val pluginGroup = project.property("plugin.group").toString()
val pluginName = project.property("plugin.name").toString()
val pluginId = project.property("plugin.id").toString()
val pluginDescription = project.property("plugin.description").toString()
val pluginVersion = project.property("plugin.version").toString()

val loomVersion = project.property("loom.version").toString()
val mappingIoVersion = project.property("mapping-io.version").toString()
val junitVersion = project.property("junit.version").toString()

group = pluginGroup
version = "$pluginVersion+${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"

description = pluginDescription

base.archivesName.set(pluginId)

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

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
    implementation("net.fabricmc:fabric-loom:${loomVersion}")
    implementation("net.fabricmc:mapping-io:${mappingIoVersion}")

    testImplementation(platform("org.junit:junit-bom:${junitVersion}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.withType<Jar> {
    from("LICENSE")
}

tasks.test {
    useJUnitPlatform()
}

tasks.javadoc {
    options.encoding = "UTF-8"
}

license {
    header(rootProject.file("LICENSE_HEADER.txt"))
    include("**/dev/galacticraft/**/*.java")
}

gradlePlugin {
    website.set("https://github.com/TeamGalacticraft/Mojarn")
    vcsUrl.set("https://github.com/TeamGalacticraft/Mojarn")

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

