package app.mra.ktor.monitoring.interceptor.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

object JsonFormatter {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun prettyPrint(jsonString: String?): String {
        if (jsonString.isNullOrBlank()) return ""
        return runCatching {
            val element = JsonParser.parseString(jsonString)
            gson.toJson(element)
        }.getOrDefault(jsonString)
    }
}
