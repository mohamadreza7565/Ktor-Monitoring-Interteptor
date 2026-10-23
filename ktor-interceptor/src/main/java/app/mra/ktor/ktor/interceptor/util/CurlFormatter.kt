package app.mra.ktor.ktor.interceptor.util

import app.mra.ktor.ktor.interceptor.model.NetworkLog

object CurlFormatter {
    fun format(log: NetworkLog.HttpLog): String {
        val builder = StringBuilder("curl -X ${log.method}")

        log.requestHeaders.forEach { (key, values) ->
            values.forEach { value ->
                builder.append(" -H \"$key: $value\"")
            }
        }

        if (!log.requestBody.isNullOrBlank()) {
            val escapedBody = log.requestBody.replace("\"", "\\\"")
            builder.append(" -d \"$escapedBody\"")
        }

        builder.append(" \"${log.url}\"")
        return builder.toString()
    }
}
