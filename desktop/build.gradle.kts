plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.compose")
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("moe.ouom.neriplayer.desktop.MainKt")
}

compose.desktop {
    application {
        mainClass = "moe.ouom.neriplayer.desktop.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "NeriPlayer Desktop"
            packageVersion = "1.0.0"
        }
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
}
