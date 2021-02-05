import java.net.*

plugins {
    kotlin("jvm")
    application
    `maven-publish`
    id("com.google.cloud.tools.jib")
    id("com.github.johnrengelman.shadow")
    id("com.github.hierynomus.license")
}

val scriptUrl: String by extra

apply(from = "$scriptUrl/git-version.gradle.kts")
repositories {
    mavenCentral()
}

val browserupProxyVersion: String by extra

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.browserup:browserup-proxy-core:$browserupProxyVersion")
    implementation("io.netty:netty-all:4.1.44.Final")
}

val appMainClassName by extra("com.epam.drill.proxy.ProxyKt")

val appJvmArgs = listOf(
    "-server",
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005",
    "-Djava.awt.headless=true",
    "-Xms128m",
    "-Xmx2g",
    "-XX:+UseG1GC",
    "-XX:MaxGCPauseMillis=100"
)

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClassName = appMainClassName
    applicationDefaultJvmArgs = appJvmArgs
}

jib {
    from {
        image = "gcr.io/distroless/java:8-debug"
    }
    to {
        image = "drill4j/browser-proxy"
        tags = setOf("${project.version}")
    }
    container {
        ports = listOf("7777", "5005")
        mainClass = appMainClassName
        jvmFlags = appJvmArgs
    }
}

val licenseFormatSettings by tasks.registering(com.hierynomus.gradle.license.tasks.LicenseFormat::class) {
    source = fileTree(project.projectDir).also {
        include("**/*.kt", "**/*.java", "**/*.groovy")
        exclude("**/.idea")
    }.asFileTree
}
license {
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
}

tasks["licenseFormat"].dependsOn(licenseFormatSettings)
