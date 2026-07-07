plugins {
    id("java")
}

allprojects {
    apply(plugin = "java")

    group = "xyz.qincai"

    repositories {
        mavenCentral()
    }
}

subprojects {
    tasks.withType<Javadoc> {
        options.encoding = "UTF-8"
    }
}
