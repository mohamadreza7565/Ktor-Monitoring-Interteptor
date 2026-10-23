# Ktor Monitoring Interceptor 🚀

An in-app HTTP & WebSocket network inspection library for Android applications using **Ktor Client**, built with **Jetpack Compose**. Inspired by Chucker, **Ktor Monitoring Interceptor** captures network traffic in real-time, displays live system notifications, and provides a powerful, modern UI to inspect requests, responses, headers, cURL commands, and media files.

[![JitPack](https://jitpack.io/v/mohammadrezaallahgholi/KtorInterceptor.svg)](https://jitpack.io/#mohammadrezaallahgholi/KtorInterceptor)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-purple.svg)](https://kotlinlang.org)
[![Ktor](https://img.shields.io/badge/Ktor-3.1.3-orange.svg)](https://ktor.io)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-24-green.svg)](https://developer.android.com)

---

## ✨ Key Features

- 🌐 **Full HTTP Interception**: Log all HTTP traffic (GET, POST, PUT, DELETE, etc.) including request/response headers, query parameters, request/response bodies, status codes, and timing.
- 🔌 **WebSocket Inspection**: Track WebSocket connections, inspect handshake details, monitor incoming & outgoing frames in real-time, and log connection lifecycle states.
- 🔔 **Live System Notifications**: Displays real-time Android notifications for active network calls, allowing one-tap access to the inspector UI.
- 🎨 **Jetpack Compose Material 3 UI**: Clean, responsive UI built natively with Compose, supporting search, filtering, and detail navigation.
- 📋 **cURL Generator**: Instantly format and copy any HTTP request as a ready-to-run `cURL` command for terminal or Postman debugging.
- 🎵 **Inline Audio Player**: Preview audio responses (e.g., MP3, WAV, AAC) directly inside the response detail view using an integrated Compose audio player.
- ⚡ **Zero-Boilerplate Auto-Initializer**: Automatically initializes background utilities and notification channels via `ContentProvider` without requiring `Application` class setup.
- 🧹 **Configurable Log Retention**: Set memory caps on log history to keep your application fast and lightweight during long debugging sessions.

---

## 📦 Installation

### Step 1. Add JitPack Repository

Add JitPack to your project's `settings.gradle.kts` file:

#### Kotlin DSL (`settings.gradle.kts`)
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

#### Groovy DSL (`settings.gradle`)
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

---

### Step 2. Add Dependency

Add the library to your module's `build.gradle.kts` file:

#### Kotlin DSL (`build.gradle.kts`)
```kotlin
dependencies {
    // Ktor Monitoring Interceptor
    debugImplementation("com.github.mohammadrezaallahgholi:KtorInterceptor:latest-version")
    
    // Optional: Use no-op version or keep in debug builds only
    // releaseImplementation(...)
}
```

#### Groovy DSL (`build.gradle`)
```groovy
dependencies {
    debugImplementation 'com.github.mohammadrezaallahgholi:KtorInterceptor:1.0.0'
}
```

---

## 🚀 Quick Start & Usage

### 1. Install `KtorInspector` Plugin

Install `KtorInspector` into your Ktor `HttpClient` instance:

```kotlin
import app.mra.ktor.monitoring.interceptor.plugin.KtorInspector
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets

val httpClient = HttpClient(OkHttp) {
    // Optional: Enable WebSockets plugin if monitoring WebSocket sessions
    install(WebSockets)

    // Install Ktor Inspector Plugin
    install(KtorInspector) {
        enabled = true          // Enable or disable the inspector (Default: true)
        maxRetainedLogs = 100   // Limit in-memory log history (Default: 100)
    }
}
```

---

### 2. Launching the Inspector UI

When requests are performed, a system notification appears in the status bar. Tapping the notification opens the inspector.

You can also launch `InspectorActivity` programmatically from a developer settings screen, debug drawer, or shake gesture handler:

```kotlin
import android.content.Intent
import app.mra.ktor.monitoring.interceptor.ui.InspectorActivity

fun openNetworkInspector(context: Context) {
    val intent = Intent(context, InspectorActivity::class.java)
    context.startActivity(intent)
}
```

---

### 3. Android 13+ Notification Permissions (Optional)

To show live notifications on Android 13 (API 33) and above, ensure your app requests the runtime permission `Manifest.permission.POST_NOTIFICATIONS`:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        REQUEST_CODE
    )
}
```

---

## ⚙️ Advanced Configuration

You can conditionally configure the plugin to run only in Debug builds:

```kotlin
val httpClient = HttpClient {
    install(KtorInspector) {
        // Automatically disable inspector in release builds
        enabled = BuildConfig.DEBUG
        
        // Retain up to 200 requests in memory
        maxRetainedLogs = 200
    }
}
```

---

## 📱 Sample Module

A full working Android application demonstrating HTTP requests, JSON responses, audio streaming inspection, and WebSocket monitoring is provided in the [`:app`](/app) module.

---

## 📄 License

```text
Copyright 2025 Mohammadreza Allahgholi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
