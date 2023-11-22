plugins {
    id("java-library")
    id("maven-publish")
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(rootProject)
}

publishing {
    repositories {
        maven("https://ci.mg-dev.eu/plugin/repository/everything") {
            name = "MGDev"
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.compilerArgs.addAll(listOf("--add-modules", "jdk.incubator.vector"));
    }
}
