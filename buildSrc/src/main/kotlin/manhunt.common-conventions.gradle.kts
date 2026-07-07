plugins {
    `java-library`
}

group = rootProject.group
version = rootProject.version

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.jar {
    archiveBaseName.set("${rootProject.name}-${project.name}")
}
