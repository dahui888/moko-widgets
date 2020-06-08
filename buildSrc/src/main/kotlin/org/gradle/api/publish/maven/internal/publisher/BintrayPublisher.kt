/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package org.gradle.api.publish.maven.internal.publisher

import org.gradle.internal.Factory
import org.gradle.internal.resource.ExternalResource
import org.gradle.internal.resource.ExternalResourceName
import org.gradle.internal.resource.ExternalResourceRepository
import org.gradle.internal.resource.ExternalResourceWriteResult
import org.gradle.internal.resource.ReadableContent
import org.gradle.internal.resource.transport.http.HttpErrorStatusCodeException
import org.gradle.util.BuildCommencedTimeProvider
import java.io.File
import java.net.URI

open class BintrayPublisher : MavenRemotePublisher {

    constructor(factory: Factory<File>) : super(factory, BuildCommencedTimeProvider())

    override fun publish(
        publication: MavenNormalizedPublication,
        repository: ExternalResourceRepository,
        rootUri: URI?,
        localRepo: Boolean
    ) {
        val myRepo = object : ExternalResourceRepository by repository {
            override fun withProgressLogging(): ExternalResourceRepository {
                return this
            }

            override fun resource(resource: ExternalResourceName?): ExternalResource {
                return repository.resource(resource).let(::createWrappedResource)
            }

            override fun resource(resource: ExternalResourceName?, revalidate: Boolean): ExternalResource {
                return repository.resource(resource, revalidate).let(::createWrappedResource)
            }
        }
        super.publish(publication, myRepo, rootUri, localRepo)
    }

    private fun createWrappedResource(resource: ExternalResource): ExternalResource {
        return object : ExternalResource by resource {
            override fun put(source: ReadableContent?): ExternalResourceWriteResult {
                val file = uri.path.substringAfterLast('/')

                val skip = when {
                    file.startsWith("maven-metadata.xml") -> true
                    file.endsWith(".md5") -> true
                    file.endsWith(".sha1") -> true
                    else -> false
                }
                if(skip) {
                    println("$displayName skip")
                    return ExternalResourceWriteResult(0)
                }

                return try {
                    resource.put(source).also { println("$displayName success") }
                } catch (httpExc: HttpErrorStatusCodeException) {
                    if (httpExc.statusCode == 409) {
                        println("$displayName failed with 409 - skip")
                        ExternalResourceWriteResult(0)
                    } else {
                        println("$displayName failed with ${httpExc.statusCode} - error")
                        throw httpExc
                    }
                }
            }
        }
    }
}