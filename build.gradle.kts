plugins {
    id("java")
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("maven-publish")
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "fabric-loom")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
            vendor.set(JvmVendorSpec.AZUL)
        }
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
    }
    
    tasks.withType<JavaExec>().configureEach {
        javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    }

    version = rootProject.version
    group = rootProject.group

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.parchmentmc.org")
    }

    dependencies {
        modImplementation("net.fabricmc:fabric-loader:${rootProject.extra["loader_version"]}")
    }

    tasks.processResources {
        filesMatching("fabric.mod.json") {
            expand(rootProject.properties)
        }
    }
}

subprojects {
    loom {
        runs {
            named("client") {
                client()
                ideConfigGenerated(true)
                runDir("run")
            }
            named("server") {
                server()
                ideConfigGenerated(true)
                runDir("run")
            }
        }
    }

    dependencies {
        implementation(project(":")) {
            exclude(group = "net.fabricmc", module = "fabric-loader") // prevent duplicate fabric-loader on run
        }
    }
}

subprojects.forEach { subproject ->
    rootProject.tasks.named("remapJar").configure {
        dependsOn("${subproject.path}:remapJar")
    }
}

tasks.remapJar.configure {
    subprojects.forEach { subproject ->
        subproject.tasks.matching { it.name == "remapJar" }.configureEach {
            nestedJars.from(this)
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.4")
    mappings(loom.officialMojangMappings())
}

loom {
    accessWidenerPath = file("src/main/resources/wtem.accesswidener")
}

defaultTasks("clean", "build")
