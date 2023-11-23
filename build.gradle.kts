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
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

sourceSets {
    create("simd") {
        java {
            srcDir("src/simd/java")
        }
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
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

tasks {
    withType<JavaCompile>().configureEach {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    val compileSimd by creating(JavaCompile::class) {
        source = sourceSets["simd"].java
        classpath = sourceSets["simd"].compileClasspath
        destinationDirectory.set(file("$buildDir/classes/java/simd"))
        options.release.set(17)
        options.compilerArgs.addAll(listOf("--add-modules", "jdk.incubator.vector"));
    }

    javadoc {
        dependsOn(compileSimd)
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
    }

    jar {
        from(sourceSets["simd"].output)
        dependsOn(compileSimd)
    }
}
