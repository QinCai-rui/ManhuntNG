plugins {
    id("manhunt.common-conventions")
    id("com.gradleup.shadow") version "9.4.2"
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":common"))
    compileOnly("io.papermc.paper:paper-api:${rootProject.findProperty("paper_version")}")
}

tasks.shadowJar {
    archiveBaseName.set("${rootProject.name}")
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
