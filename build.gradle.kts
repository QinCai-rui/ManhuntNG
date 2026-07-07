plugins {
    id("java")
    id("maven-publish")
}

allprojects {
    apply(plugin = "java")

    group = "xyz.qincai"

    java {
        withSourcesJar()
        withJavadocJar()
    }

    repositories {
        mavenCentral()
    }
}

subprojects {
    tasks.withType<Javadoc> {
        options.encoding = "UTF-8"
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
