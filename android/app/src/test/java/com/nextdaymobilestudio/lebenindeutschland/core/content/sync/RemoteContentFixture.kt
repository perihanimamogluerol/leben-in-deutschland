package com.nextdaymobilestudio.lebenindeutschland.core.content.sync

import com.nextdaymobilestudio.lebenindeutschland.core.content.remote.RemoteContentDocument

internal data class RemoteContentFixture(
    val manifest: RemoteContentDocument,
    val release: RemoteContentDocument,
    val packages: Map<String, RemoteContentDocument>,
    val chunks: Map<String, List<RemoteContentDocument>>,
)

internal fun remoteContentFixture(): RemoteContentFixture {
    val questionsByLanguage = listOf("de", "en").associateWith(::questionsForLanguage)
    val packageHashes = questionsByLanguage.mapValues { (_, questions) ->
        CanonicalJson.sha256(questions)
    }
    val catalogSha256 = CanonicalJson.sha256(packageHashes)

    val releaseFields = mapOf<String, Any?>(
        "releaseId" to RELEASE_ID,
        "schemaVersion" to 3L,
        "datasetVersion" to "fixture-1",
        "packageStrategy" to "SELF_CONTAINED_LANGUAGE_PACK",
        "questionCount" to 1L,
        "languages" to listOf("de", "en"),
        "languageCount" to 2L,
        "chunkSize" to 10L,
        "sourceCatalogSha256" to "source-hash",
        "catalogSha256" to catalogSha256,
        "packageHashes" to packageHashes,
        "qualityIssueCount" to 0L,
        "status" to "READY",
        "createdAt" to CREATED_AT,
    )

    val manifestFields = mapOf<String, Any?>(
        "activeReleaseId" to RELEASE_ID,
        "previousReleaseId" to "previous-release",
        "schemaVersion" to 3L,
        "datasetVersion" to "fixture-1",
        "packageStrategy" to "SELF_CONTAINED_LANGUAGE_PACK",
        "questionCount" to 1L,
        "languages" to listOf("de", "en"),
        "languageCount" to 2L,
        "catalogSha256" to catalogSha256,
        "sourceCatalogSha256" to "source-hash",
        "releaseCreatedAt" to CREATED_AT,
    )

    val chunks = questionsByLanguage.mapValues { (languageCode, questions) ->
        val chunkFields = linkedMapOf<String, Any?>(
            "index" to 0L,
            "languageCode" to languageCode,
            "firstQuestionId" to "general_001",
            "lastQuestionId" to "general_001",
            "questionCount" to 1L,
            "questions" to questions,
            "sha256" to CanonicalJson.sha256(questions),
        )
        chunkFields["jsonBytes"] = CanonicalJson
            .encode(chunkFields)
            .toByteArray(Charsets.UTF_8)
            .size
            .toLong()
        listOf(RemoteContentDocument(id = "0000", fields = chunkFields))
    }

    val packages = questionsByLanguage.mapValues { (languageCode, questions) ->
        RemoteContentDocument(
            id = languageCode,
            fields = mapOf(
                "releaseId" to RELEASE_ID,
                "schemaVersion" to 3L,
                "packageStrategy" to "SELF_CONTAINED_LANGUAGE_PACK",
                "languageCode" to languageCode,
                "questionCount" to 1L,
                "chunkCount" to 1L,
                "chunkSize" to 10L,
                "packageSha256" to packageHashes.getValue(languageCode),
                "jsonBytes" to CanonicalJson
                    .encode(questions)
                    .toByteArray(Charsets.UTF_8)
                    .size
                    .toLong(),
                "qualityIssueCount" to 0L,
                "explanationReady" to true,
                "status" to "READY",
                "createdAt" to CREATED_AT,
            ),
        )
    }

    return RemoteContentFixture(
        manifest = RemoteContentDocument(id = "current", fields = manifestFields),
        release = RemoteContentDocument(id = RELEASE_ID, fields = releaseFields),
        packages = packages,
        chunks = chunks,
    )
}

private fun questionsForLanguage(languageCode: String): List<Map<String, Any?>> {
    val languageName = if (languageCode == "de") "Deutsch" else "English"
    val study = linkedMapOf<String, Any?>(
        "languageCode" to languageCode,
        "questionText" to "$languageName question",
        "explanation" to "$languageName explanation",
        "answers" to ANSWER_IDS.map { answerId ->
            mapOf("id" to answerId, "text" to "$languageName answer $answerId")
        },
        "explanationStatus" to "AI_GENERATED_NEEDS_REVIEW",
        "reviewed" to false,
    )
    if (languageCode == "de") {
        study["textStatus"] = "SOURCE"
    } else {
        study["translationStatus"] = "AI_GENERATED_NEEDS_REVIEW"
        study["audit"] = mapOf(
            "provider" to "fixture-provider",
            "sourceRecordNumber" to "38",
            "sourceRecordId" to "source-record-id",
            "sourceCommit" to "source-commit",
            "questionMatchScore" to 0.97,
            "answerSetMatchScore" to 0.99,
            "answerMatchScores" to listOf(1.0, 0.98, 0.99, 1.0),
            "fallbackAnswerIds" to listOf("B"),
        )
    }

    return listOf(
        mapOf(
            "id" to "general_001",
            "metadata" to mapOf(
                "sourceId" to "general-001",
                "officialNumber" to 1L,
                "scope" to "GENERAL",
                "stateCode" to null,
                "category" to null,
                "categoryStatus" to "UNASSIGNED",
                "sourceVersion" to "fixture-1",
                "sourcePdfSha256" to "pdf-hash",
                "source" to mapOf(
                    "page" to 2L,
                    "bamfInternalId" to "18476",
                ),
            ),
            "correctAnswerId" to "A",
            "german" to mapOf(
                "questionText" to "German question",
                "memoryKey" to mapOf(
                    "questionCue" to "Frage erkennen",
                    "answerCue" to "Antwort A",
                    "status" to "AI_GENERATED_NEEDS_REVIEW",
                ),
                "questionImage" to mapOf(
                    "sourcePath" to "images/question.jpeg",
                    "sourceUrl" to "https://example.com/question.jpeg",
                    "storagePath" to "question-images/question.jpeg",
                    "sha256" to "image-hash",
                    "width" to 1200L,
                    "height" to 857L,
                    "mimeType" to "image/jpeg",
                ),
                "answers" to ANSWER_IDS.map { answerId ->
                    mapOf(
                        "id" to answerId,
                        "text" to "German answer $answerId",
                        "image" to if (answerId == "D") {
                            mapOf(
                                "sourcePath" to "images/answer-D.png",
                                "sourceUrl" to "https://example.com/answer-D.png",
                                "storagePath" to "question-images/answer-D.png",
                                "sha256" to "answer-image-hash",
                                "width" to 375L,
                                "height" to 488L,
                                "mimeType" to "image/png",
                            )
                        } else {
                            null
                        },
                    )
                },
            ),
            "study" to study,
        ),
    )
}

internal const val RELEASE_ID = "release-1"
private const val CREATED_AT = "2026-10-05T10:33:27Z"
private val ANSWER_IDS = listOf("A", "B", "C", "D")
