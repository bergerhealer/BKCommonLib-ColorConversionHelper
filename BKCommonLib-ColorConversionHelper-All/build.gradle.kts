plugins {
    id("java-library")
    id("maven-publish")
}

group = rootProject.group
version = rootProject.version

val copyDependencies = configurations.create("copyDependencies")

repositories {
    mavenCentral()
}

dependencies {
    api(rootProject)

    copyDependencies(project(":BKCommonLib-ColorConversionHelper-SIMD")) {
        exclude(module = "BKCommonLib-ColorConversionHelper")
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    register<Copy>("copyDependenciesToBuild") {
        configurations["copyDependencies"].resolve().forEach { file ->
            from(zipTree(file))
        }
        exclude("META-INF/**")
        into("$buildDir/classes/java/main")
        dependsOn(":BKCommonLib-ColorConversionHelper-SIMD:jar")
    }

    classes {
        dependsOn("copyDependenciesToBuild")
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
