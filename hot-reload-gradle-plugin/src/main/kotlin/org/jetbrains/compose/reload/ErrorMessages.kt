package org.jetbrains.compose.reload

internal object ErrorMessages {
    fun missingMainClassProperty() = """
        Missing 'mainClass' property. Please invoke the task with '-PmainClass=...`
        Example: ./gradlew runHot -PmainClass=my.package.MainKt
    """.trimIndent()
}