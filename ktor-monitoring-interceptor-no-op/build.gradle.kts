import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "app.mra.ktor.monitoring.interceptor"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.mohamadreza7565"
                artifactId = "ktor-monitoring-interceptor-no-op"
                version = "1.0.9"

                pom {
                    name.set("Ktor Monitoring Interceptor No-Op")
                    description.set("No-Op variant for Ktor Monitoring Interceptor on Android")
                    url.set("https://github.com/mohamadreza7565/Ktor-Monitoring-Interteptor")

                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("mohamadreza7565")
                            name.set("Mohammadreza Allahgholi")
                            email.set("mohamadreza7565@gmail.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com/mohamadreza7565/Ktor-Monitoring-Interteptor.git")
                        developerConnection.set("scm:git:ssh://github.com/mohamadreza7565/Ktor-Monitoring-Interteptor.git")
                        url.set("https://github.com/mohamadreza7565/Ktor-Monitoring-Interteptor")
                    }
                }
            }
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.ktor.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlin.coroutines.core)
}
