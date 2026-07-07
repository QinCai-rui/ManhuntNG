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

    minecraft("com.mojang:minecraft:${rootProject.findProperty("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${rootProject.findProperty("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${rootProject.findProperty("fabric_api_version")}")
}

tasks.jar {
    archiveBaseName.set("${rootProject.name}-${project.name}")
    from(rootProject.file("LICENSE"))
}
