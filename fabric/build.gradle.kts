plugins {
    `java-library`
    id("net.fabricmc.fabric-loom") version "1.17.13"
}

group = rootProject.group
version = rootProject.version

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}

repositories {
    maven("https://maven.fabricmc.net/")
}

dependencies {
    implementation(project(":common"))

    implementation("com.mojang:minecraft:${rootProject.findProperty("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${rootProject.findProperty("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${rootProject.findProperty("fabric_api_version")}")
    
    // Adventure API for consistent text handling
    modImplementation(include("net.kyori:adventure-platform-fabric:6.9.0")!!) // Compatible with MC 1.21.2+
}

tasks.jar {
    archiveBaseName.set("${rootProject.name}-${project.name}")
    from(rootProject.file("LICENSE"))
}
