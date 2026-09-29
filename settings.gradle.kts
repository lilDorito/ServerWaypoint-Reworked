pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://repo.spongepowered.org/repository/maven-public/") { name = "SpongePowered" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create("mods") {
        fun mc(loader: String, vararg versions: String) {
            for (minecraftVersion in versions) {
                version("$minecraftVersion-$loader", minecraftVersion)
            }
        }
        mc("fabric", "26.1.2")
        mc("forge", "1.20.1")

        mapBuilds { _, data ->
            val loader = data.project.substringAfterLast('-')
            when (data.project) {
                "1.20.2-neoforge" -> "neogradle.gradle.kts"
                "26.1.2-fabric" -> "fabric-unobfuscated.gradle.kts"
                "26.2-fabric" -> "fabric-unobfuscated.gradle.kts"
                else -> "$loader.gradle.kts"
            }
        }
    }
}

rootProject.name = "server_waypoint"
include("common")
include("mods")
