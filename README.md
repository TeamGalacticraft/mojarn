# Mojarn
Mixes official and file (yarn) mappings for better parameter names.

## Usage
To install mojarn, add the Galacticraft repository to your `settings.gradle.kts` file:
```kotlin
pluginManagement {
    repositories {
//        maven("https://maven.galacticraft.dev/repository/maven-releases/") // currently offline
        maven("https://repo.terradevelopment.net/repository/maven-releases/")
    }
}
```

Then, add the following to your `build.gradle.kts` file:
```kotlin
plugins {
    id("dev.galacticraft.mojarn") version("0.6.0+18")
}
```

Finally, use the mappings in your project:
```kotlin
dependencies {
    mappings(mojarn.mappings("net.fabricmc:yarn:$minecraft+build.$yarn:v2"))
}
```
