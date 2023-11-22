plugins {
    id("java-library")
    id("maven-publish")
}

group = "com.bergerkiller.bukkit.colorconversionhelper"
version = "1.0"

repositories {
    mavenCentral()
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_9
    targetCompatibility = JavaVersion.VERSION_1_9
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
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    javadoc {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
    }
}
