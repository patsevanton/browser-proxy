rootProject.name = "browser-proxy"

include("proxy-agent")
pluginManagement {
    val kotlinVersion: String by extra
    val jibVersion: String by extra
    val shadowJarVersion: String by extra
    val licenseVersion: String by extra

    plugins {
        kotlin("jvm") version kotlinVersion
        id("com.google.cloud.tools.jib") version jibVersion
        id("com.github.johnrengelman.shadow") version shadowJarVersion
        id("com.github.hierynomus.license") version licenseVersion

        repositories {
            gradlePluginPortal()
            maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
        }

    }
}
