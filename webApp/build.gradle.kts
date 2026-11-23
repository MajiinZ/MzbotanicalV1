plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm {
        // kotlinOptions is an error since Kotlin 2.2; -Xcontext-receivers was removed (unused here).
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            optIn.add("kotlin.RequiresOptIn")
        }
    }




    js(IR) {
        val cssModules = true
        browser {
            commonWebpackConfig {
                outputFileName = "webApp.js"
                cssSupport {
                    enabled.set(true)


                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.html.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.html.testUtils)
        }
    }
}
