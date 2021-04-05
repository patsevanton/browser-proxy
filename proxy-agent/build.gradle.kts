plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

repositories {
    mavenCentral()
}

val javassistVersion: String by extra

dependencies {
    api("org.javassist:javassist:$javassistVersion")
    implementation(kotlin("stdlib"))
}

tasks.shadowJar{
    archiveFileName.set("agent-shadow.jar")
    group = "shadow"
    manifest {
        attributes["Premain-Class"] = "com.epam.drill.proxy.agent.Agent"
    }
}
