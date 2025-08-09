plugins {
    id("com.android.application") version "8.6.0" apply false
    kotlin("android") version "2.0.0" apply false
}

tasks.register("cleanAll", Delete::class) {
    delete(rootProject.buildDir)
}