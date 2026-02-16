plugins {
    java
    id("com.gradleup.shadow") version "8.3.9" apply false
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19" apply false
}

extra["plVersion"] = "7.0.0"

// Single platform: Paper (and Velocity in same jar). Root build = build the plugin.
tasks.named("build") {
    dependsOn(":implementation:build")
}

allprojects {
    group = "me.dablakbandit"
    version = rootProject.extra["plVersion"]!!

    repositories {
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://ci.lucko.me/plugin/repository/everything/") }
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://libraries.minecraft.net/") }
    }
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.compileJava {
        options.encoding = "UTF-8"
    }

    extra["aoVersion"] = rootProject.extra["plVersion"]
}
