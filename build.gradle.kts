// ROOT build.gradle.kts
plugins {
    // This just "prepares" the plugins without applying them to the root
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}