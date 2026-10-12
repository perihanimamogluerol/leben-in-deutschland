package com.nextdaymobilestudio.lebenindeutschland.core.content.remote

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.nextdaymobilestudio.lebenindeutschland.core.content.sync.ContentSyncException
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class FirestoreContentDataSource(
    private val firestore: FirebaseFirestore,
) : RemoteContentDataSource {
    override suspend fun getActiveManifest(): RemoteContentDocument = firestore
        .document(ACTIVE_MANIFEST_PATH)
        .get(Source.SERVER)
        .awaitResult()
        .toRemoteDocument(ACTIVE_MANIFEST_PATH)

    override suspend fun getRelease(releaseId: String): RemoteContentDocument = firestore
        .document("$RELEASES_COLLECTION/$releaseId")
        .get(Source.SERVER)
        .awaitResult()
        .toRemoteDocument("release $releaseId")

    override suspend fun getPackage(
        releaseId: String,
        languageCode: String,
    ): RemoteContentDocument = firestore
        .document(packagePath(releaseId, languageCode))
        .get(Source.SERVER)
        .awaitResult()
        .toRemoteDocument("package $releaseId/$languageCode")

    override suspend fun getChunks(
        releaseId: String,
        languageCode: String,
    ): List<RemoteContentDocument> = firestore
        .collection("${packagePath(releaseId, languageCode)}/chunks")
        .get(Source.SERVER)
        .awaitResult()
        .toRemoteDocuments()

    private fun packagePath(releaseId: String, languageCode: String): String =
        "$RELEASES_COLLECTION/$releaseId/languagePacks/$languageCode"

    private fun DocumentSnapshot.toRemoteDocument(label: String): RemoteContentDocument {
        if (!exists()) {
            throw ContentSyncException("Firestore $label document does not exist")
        }
        return RemoteContentDocument(
            id = id,
            fields = data ?: throw ContentSyncException("Firestore $label document is empty"),
        )
    }

    private fun QuerySnapshot.toRemoteDocuments(): List<RemoteContentDocument> = documents
        .map { snapshot ->
            snapshot.toRemoteDocument("chunk ${snapshot.id}")
        }
        .sortedBy(RemoteContentDocument::id)

    private suspend fun <T : Any> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { completedTask ->
                if (!continuation.isActive) return@addOnCompleteListener

                when {
                    completedTask.isCanceled -> continuation.resumeWithException(
                        CancellationException("Firestore request was cancelled"),
                    )
                    completedTask.exception != null -> continuation.resumeWithException(
                        completedTask.exception
                            ?: ContentSyncException("Firestore request failed"),
                    )
                    else -> {
                        val result = completedTask.result
                        if (result != null) {
                            continuation.resume(result)
                        } else {
                            continuation.resumeWithException(
                                ContentSyncException("Firestore request returned no result"),
                            )
                        }
                    }
                }
            }
        }

    private companion object {
        const val ACTIVE_MANIFEST_PATH = "contentManifest/current"
        const val RELEASES_COLLECTION = "contentReleases"
    }
}
