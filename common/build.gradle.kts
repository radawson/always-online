plugins {
    java
    id("com.gradleup.shadow")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.0.1")
    compileOnly("com.github.AshleyThew:mongodb-loader:5.5.1")
    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly("com.google.guava:guava:31.1-jre")
}

tasks.shadowJar {
    archiveClassifier.set("")
    destinationDirectory.set(project.layout.projectDirectory.dir("target"))
    archiveFileName.set("${project.name}-${project.version}-shaded.jar")
}
