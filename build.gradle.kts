plugins {
    id("java-library")
    id("maven-publish")
}

group = "com.bergerkiller.bukkit.colorconversionhelper"
version = "1.02"

repositories {
    mavenCentral()
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

sourceSets {
    create("simd") {
        java.srcDir("src/simd/java")
    }
}

val compileSimd by tasks.registering(JavaCompile::class) {
    source = sourceSets["simd"].java
    classpath = sourceSets["main"].output + sourceSets["simd"].compileClasspath
    destinationDirectory.set(file("$buildDir/classes/java/simd"))
    options.release.set(17)
    options.compilerArgs.addAll(listOf("--add-modules", "jdk.incubator.vector"))
}

tasks {
    compileJava {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    test {
        useJUnitPlatform()
        dependsOn(compileSimd)

        testLogging {
            showStandardStreams = true
        }

        classpath += files(compileSimd.get().destinationDirectory)

        jvmArgs("--add-modules", "jdk.incubator.vector")
    }

    javadoc {
        dependsOn(compileSimd)
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
    }

    jar {
        from(sourceSets["main"].output)
        from(compileSimd.get().outputs.files)
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
