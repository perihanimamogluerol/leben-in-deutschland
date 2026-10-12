package com.nextdaymobilestudio.lebenindeutschland.core.content.remote

data class RemoteContentDocument(
    val id: String,
    val fields: Map<String, Any?>,
)

interface RemoteContentDataSource {
    suspend fun getActiveManifest(): RemoteContentDocument

    suspend fun getRelease(releaseId: String): RemoteContentDocument

    suspend fun getPackage(releaseId: String, languageCode: String): RemoteContentDocument

    suspend fun getChunks(releaseId: String, languageCode: String): List<RemoteContentDocument>
}
