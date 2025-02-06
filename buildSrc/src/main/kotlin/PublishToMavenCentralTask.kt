/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.basicAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headers
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import kotlin.io.path.createDirectories

/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class PublishToMavenCentralTask : DefaultTask() {

    @Input
    val deploymentName = project.objects.property<String>()
        .convention(project.provider { "compose-hot-reload-${project.version}" })

    @InputFile
    val deploymentBundle = project.objects.fileProperty()

    @OutputFile
    val deploymentIdFile = project.objects.fileProperty()
        .convention(project.layout.buildDirectory.file("mavenCentral.deploy.id.txt"))

    @TaskAction
    fun publish() {
        runBlocking {
            HttpClient(CIO).use { client ->
                client.publish()
            }
        }
    }

    private suspend fun HttpClient.publish() {
        val response = submitForm {
            url("https://central.sonatype.com/api/v1/publisher/upload")
            parameter("name", deploymentName.get())
            parameter("publishingType", "USER_MANAGED")
            basicAuth("XlIAZQrK", "Wfp84kVgOmhIJfOPZDEm93+2jLkGy//ebS0fJ6ENVXIc")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("bundle", deploymentBundle.get().asFile.readBytes(), headers {
                            contentType(ContentType.Application.Zip)
                            append(HttpHeaders.ContentDisposition, "filename=\"bundle.zip\"")
                        })
                    }
                )
            )
            onUpload { bytesSentTotal, contentLength ->
                logger.info("Sent $bytesSentTotal bytes from $contentLength")
            }
        }

        if (response.status != HttpStatusCode.Created) {
            error("Deployment failed (${response.status}):\n ${response.bodyAsText()}")
        }

        response.bodyAsText().trim().let { deploymentId ->
            deploymentIdFile.get().asFile.parentFile.toPath().createDirectories()
            deploymentIdFile.get().asFile.writeText(deploymentId)
            logger.quiet("Deployment ID: $deploymentId")
        }
    }
}
