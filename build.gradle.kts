plugins {
    id("java-library")
    id("maven-publish")
    /* https://github.com/bergerhealer/gradle-simd-plugin */
    id("com.bergerkiller.gradle.simd") version "1.0.0"
}

group = "com.bergerkiller.bukkit.colorconversionhelper"
version = "1.04"

repositories {
    mavenCentral()
}

simd {
    sourceDir.set(layout.projectDirectory.dir("src/simd/java"))
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks {
    compileJava {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    test {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
        }

        jvmArgs("--add-modules", "jdk.incubator.vector")
    }

    javadoc {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
    }
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
