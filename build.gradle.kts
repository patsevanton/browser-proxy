import com.google.cloud.tools.jib.gradle.*
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

val littleProxyVersion: String by extra

dependencies {
    implementation(kotlin("stdlib"))
    implementation("xyz.rogfam:littleproxy:$littleProxyVersion")
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

val agentDir = "$buildDir/proxy-agent"

application {
    mainClassName = appMainClassName
    applicationDefaultJvmArgs = appJvmArgs + "-javaagent:$agentDir/agent-shadow.jar"
}

tasks {
    val makeAgentDir by registering(Copy::class) {
        group = "build"
        val agentJar = project("proxy-agent").buildDir
            .resolve("libs")
            .resolve("agent-shadow.jar")
        dependsOn(project("proxy-agent").tasks.named("shadowJar"))
        from(agentJar)
        into(agentDir)
    }

    val makeJibDirs by registering(Copy::class){
        group = "jib"
        into(agentDir)
        from("littleproxy_keystore.jks")
        dependsOn(makeAgentDir)
    }

    withType<JibTask> {
        dependsOn(makeJibDirs)
    }

    (run){
        dependsOn(makeAgentDir)
    }
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
        jvmFlags = appJvmArgs + "-javaagent:agent-shadow.jar"
    }
    extraDirectories {
        setPaths(agentDir)
        permissions = mapOf("/build/proxy-agent" to "775")
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
