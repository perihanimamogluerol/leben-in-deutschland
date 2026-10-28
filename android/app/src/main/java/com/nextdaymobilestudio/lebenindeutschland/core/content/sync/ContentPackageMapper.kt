package com.nextdaymobilestudio.lebenindeutschland.core.content.sync

import com.nextdaymobilestudio.lebenindeutschland.core.content.remote.RemoteContentDocument
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.AnswerEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentPackageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseLanguageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.QuestionEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyAnswerEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyContentEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.TranslationAnswerAuditEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.TranslationAuditEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.ContentPackageSnapshot

internal data class ValidatedRemoteRelease(
    val entity: ContentReleaseEntity,
    val languages: List<ContentReleaseLanguageEntity>,
    val packageHashes: Map<String, String>,
)

internal class ContentPackageMapper {
    fun validateRelease(
        manifestDocument: RemoteContentDocument,
        releaseDocument: RemoteContentDocument,
        installedAtEpochMillis: Long,
    ): ValidatedRemoteRelease {
        val manifest = manifestDocument.fields
        val release = releaseDocument.fields
        val activeReleaseId = manifest.requiredString("activeReleaseId")
        val releaseId = release.requiredString("releaseId")

        syncRequire(releaseDocument.id == releaseId) {
            "Release document ID does not match releaseId"
        }
        syncRequire(activeReleaseId == releaseId) {
            "Active manifest and release document do not match"
        }

        val schemaVersion = release.requiredInt("schemaVersion")
        val packageStrategy = release.requiredString("packageStrategy")
        val questionCount = release.requiredInt("questionCount")
        val languages = release.requiredStringList("languages")
        val packageHashes = release.requiredStringMap("packageHashes")
        val catalogSha256 = release.requiredString("catalogSha256")

        syncRequire(schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Only Schema $SUPPORTED_SCHEMA_VERSION releases are supported"
        }
        syncRequire(release.requiredString("status") == READY_STATUS) {
            "Only READY releases can be synchronized"
        }
        syncRequire(release.requiredInt("qualityIssueCount") == 0) {
            "Release contains content-quality issues"
        }
        syncRequire(languages.isNotEmpty() && languages.size == languages.toSet().size) {
            "Release languages must be non-empty and unique"
        }
        syncRequire(GERMAN_LANGUAGE_CODE in languages) {
            "Release does not contain the German package"
        }
        syncRequire(packageHashes.keys == languages.toSet()) {
            "Release packageHashes do not exactly match release languages"
        }
        syncRequire(CanonicalJson.sha256(packageHashes) == catalogSha256) {
            "Release catalogSha256 does not match packageHashes"
        }

        validateManifest(
            manifest = manifest,
            release = release,
            activeReleaseId = activeReleaseId,
            schemaVersion = schemaVersion,
            packageStrategy = packageStrategy,
            questionCount = questionCount,
            languages = languages,
            catalogSha256 = catalogSha256,
        )

        val releaseEntity = ContentReleaseEntity(
            releaseId = releaseId,
            schemaVersion = schemaVersion,
            datasetVersion = release.requiredString("datasetVersion"),
            packageStrategy = packageStrategy,
            status = release.requiredString("status"),
            createdAt = release.requiredString("createdAt"),
            sourceCatalogSha256 = release.requiredString("sourceCatalogSha256"),
            catalogSha256 = catalogSha256,
            questionCount = questionCount,
            chunkSize = release.requiredInt("chunkSize"),
            languageCount = release.requiredInt("languageCount"),
            qualityIssueCount = release.requiredInt("qualityIssueCount"),
            installedAtEpochMillis = installedAtEpochMillis,
        )

        syncRequire(releaseEntity.languageCount == languages.size) {
            "Release languageCount does not match languages"
        }

        return ValidatedRemoteRelease(
            entity = releaseEntity,
            languages = languages.mapIndexed { index, languageCode ->
                ContentReleaseLanguageEntity(
                    releaseId = releaseId,
                    languageCode = languageCode,
                    displayOrder = index,
                    packageSha256 = packageHashes.getValue(languageCode),
                )
            },
            packageHashes = packageHashes,
        )
    }

    fun mapPackage(
        release: ValidatedRemoteRelease,
        packageDocument: RemoteContentDocument,
        chunkDocuments: List<RemoteContentDocument>,
        installedAtEpochMillis: Long,
    ): ContentPackageSnapshot {
        val packageFields = packageDocument.fields
        val releaseId = packageFields.requiredString("releaseId")
        val languageCode = packageFields.requiredString("languageCode")

        syncRequire(packageDocument.id == languageCode) {
            "Package document ID does not match languageCode"
        }
        syncRequire(releaseId == release.entity.releaseId) {
            "Package releaseId does not match active release"
        }
        syncRequire(packageFields.requiredInt("schemaVersion") == release.entity.schemaVersion) {
            "Package schemaVersion does not match release"
        }
        syncRequire(
            packageFields.requiredString("packageStrategy") == release.entity.packageStrategy,
        ) { "Package strategy does not match release" }
        syncRequire(packageFields.requiredString("status") == READY_STATUS) {
            "Only READY packages can be synchronized"
        }
        syncRequire(packageFields.requiredInt("qualityIssueCount") == 0) {
            "Package contains content-quality issues"
        }
        syncRequire(packageFields.requiredBoolean("explanationReady")) {
            "Package explanations are not ready"
        }

        val packageHash = packageFields.requiredString("packageSha256")
        syncRequire(release.packageHashes[languageCode] == packageHash) {
            "Package hash does not match the release manifest"
        }

        val chunkCount = packageFields.requiredInt("chunkCount")
        val questionCount = packageFields.requiredInt("questionCount")
        val orderedChunks = validateAndOrderChunks(
            chunkDocuments = chunkDocuments,
            expectedChunkCount = chunkCount,
            languageCode = languageCode,
        )
        val rawQuestions = orderedChunks.flatMap { chunk ->
            chunk.fields.requiredObjectList("questions")
        }

        syncRequire(rawQuestions.size == questionCount) {
            "Package questionCount does not match downloaded questions"
        }
        syncRequire(questionCount == release.entity.questionCount) {
            "Package questionCount does not match release"
        }
        syncRequire(CanonicalJson.sha256(rawQuestions) == packageHash) {
            "Downloaded package hash does not match packageSha256"
        }
        syncRequire(
            CanonicalJson.encode(rawQuestions).toByteArray(Charsets.UTF_8).size.toLong() ==
                packageFields.requiredLong("jsonBytes"),
        ) { "Downloaded package jsonBytes does not match package metadata" }

        val mappedQuestions = rawQuestions.map { rawQuestion ->
            mapQuestion(releaseId, languageCode, rawQuestion)
        }

        val snapshot = ContentPackageSnapshot(
            release = release.entity,
            releaseLanguages = release.languages,
            packageMetadata = ContentPackageEntity(
                releaseId = releaseId,
                languageCode = languageCode,
                schemaVersion = packageFields.requiredInt("schemaVersion"),
                packageStrategy = packageFields.requiredString("packageStrategy"),
                status = packageFields.requiredString("status"),
                createdAt = packageFields.requiredString("createdAt"),
                packageSha256 = packageHash,
                questionCount = questionCount,
                chunkCount = chunkCount,
                chunkSize = packageFields.requiredInt("chunkSize"),
                jsonBytes = packageFields.requiredLong("jsonBytes"),
                qualityIssueCount = packageFields.requiredInt("qualityIssueCount"),
                explanationReady = packageFields.requiredBoolean("explanationReady"),
                installedAtEpochMillis = installedAtEpochMillis,
            ),
            questions = mappedQuestions.map(MappedQuestion::question),
            answers = mappedQuestions.flatMap(MappedQuestion::answers),
            studyContent = mappedQuestions.map(MappedQuestion::studyContent),
            studyAnswers = mappedQuestions.flatMap(MappedQuestion::studyAnswers),
            translationAudits = mappedQuestions.mapNotNull(MappedQuestion::translationAudit),
            translationAnswerAudits = mappedQuestions.flatMap(
                MappedQuestion::translationAnswerAudits,
            ),
        )
        try {
            snapshot.requireValid()
        } catch (exception: IllegalArgumentException) {
            throw ContentSyncException(
                "Downloaded package failed local database validation",
                exception,
            )
        }
        return snapshot
    }

    private fun validateManifest(
        manifest: Map<String, Any?>,
        release: Map<String, Any?>,
        activeReleaseId: String,
        schemaVersion: Int,
        packageStrategy: String,
        questionCount: Int,
        languages: List<String>,
        catalogSha256: String,
    ) {
        syncRequire(activeReleaseId == release.requiredString("releaseId")) {
            "Manifest activeReleaseId does not match release"
        }
        syncRequire(manifest.requiredInt("schemaVersion") == schemaVersion) {
            "Manifest schemaVersion does not match release"
        }
        syncRequire(manifest.requiredString("packageStrategy") == packageStrategy) {
            "Manifest packageStrategy does not match release"
        }
        syncRequire(manifest.requiredInt("questionCount") == questionCount) {
            "Manifest questionCount does not match release"
        }
        syncRequire(manifest.requiredStringList("languages") == languages) {
            "Manifest languages do not match release"
        }
        syncRequire(manifest.requiredInt("languageCount") == languages.size) {
            "Manifest languageCount does not match release"
        }
        syncRequire(manifest.requiredString("catalogSha256") == catalogSha256) {
            "Manifest catalogSha256 does not match release"
        }
        syncRequire(
            manifest.requiredString("datasetVersion") == release.requiredString("datasetVersion"),
        ) { "Manifest datasetVersion does not match release" }
        syncRequire(
            manifest.requiredString("sourceCatalogSha256") ==
                release.requiredString("sourceCatalogSha256"),
        ) { "Manifest sourceCatalogSha256 does not match release" }
        syncRequire(
            manifest.requiredString("releaseCreatedAt") == release.requiredString("createdAt"),
        ) { "Manifest releaseCreatedAt does not match release" }
    }

    private fun validateAndOrderChunks(
        chunkDocuments: List<RemoteContentDocument>,
        expectedChunkCount: Int,
        languageCode: String,
    ): List<RemoteContentDocument> {
        syncRequire(expectedChunkCount > 0) { "Package chunkCount must be positive" }
        syncRequire(chunkDocuments.size == expectedChunkCount) {
            "Downloaded chunk count does not match package metadata"
        }

        val ordered = chunkDocuments.sortedBy(RemoteContentDocument::id)
        ordered.forEachIndexed { expectedIndex, document ->
            val expectedId = expectedIndex.toString().padStart(CHUNK_ID_WIDTH, '0')
            syncRequire(document.id == expectedId) {
                "Unexpected chunk ID ${document.id}; expected $expectedId"
            }
            val fields = document.fields
            val questions = fields.requiredObjectList("questions")
            syncRequire(fields.requiredInt("index") == expectedIndex) {
                "Chunk $expectedId index is invalid"
            }
            syncRequire(fields.requiredString("languageCode") == languageCode) {
                "Chunk $expectedId languageCode does not match package"
            }
            syncRequire(fields.requiredInt("questionCount") == questions.size) {
                "Chunk $expectedId questionCount does not match questions"
            }
            syncRequire(questions.isNotEmpty()) { "Chunk $expectedId is empty" }
            syncRequire(
                fields.requiredString("firstQuestionId") == questions.first().requiredString("id"),
            ) { "Chunk $expectedId firstQuestionId is invalid" }
            syncRequire(
                fields.requiredString("lastQuestionId") == questions.last().requiredString("id"),
            ) { "Chunk $expectedId lastQuestionId is invalid" }
            syncRequire(fields.requiredString("sha256") == CanonicalJson.sha256(questions)) {
                "Chunk $expectedId SHA-256 verification failed"
            }
            val chunkWithoutJsonBytes = fields - "jsonBytes"
            syncRequire(
                CanonicalJson.encode(chunkWithoutJsonBytes)
                    .toByteArray(Charsets.UTF_8)
                    .size
                    .toLong() == fields.requiredLong("jsonBytes"),
            ) { "Chunk $expectedId jsonBytes does not match chunk metadata" }
        }
        return ordered
    }

    private fun mapQuestion(
        releaseId: String,
        languageCode: String,
        raw: Map<String, Any?>,
    ): MappedQuestion {
        val questionId = raw.requiredString("id")
        val metadata = raw.requiredObject("metadata")
        val source = metadata.requiredObject("source")
        val german = raw.requiredObject("german")
        val study = raw.requiredObject("study")
        val germanAnswers = german.requiredObjectList("answers")
        val studyAnswers = study.requiredObjectList("answers")
        val questionImage = german.optionalObject("questionImage")
        val memoryKey = german.optionalObject("memoryKey")
        val memoryQuestionCue = memoryKey?.requiredString("questionCue")
        val memoryAnswerCue = memoryKey?.requiredString("answerCue")
        val memoryKeyStatus = memoryKey?.requiredString("status")

        syncRequire(study.requiredString("languageCode") == languageCode) {
            "$questionId study language does not match package"
        }
        if (memoryKey != null) {
            validateMemoryKey(
                questionId = questionId,
                questionCue = requireNotNull(memoryQuestionCue),
                answerCue = requireNotNull(memoryAnswerCue),
                status = requireNotNull(memoryKeyStatus),
            )
            val correctAnswerId = raw.requiredString("correctAnswerId")
            val correctAnswer = germanAnswers.singleOrNull { answer ->
                answer.requiredString("id") == correctAnswerId
            }
            syncRequire(correctAnswer != null) {
                "$questionId memory key has no matching correct answer"
            }
            syncRequire(correctAnswer?.optionalObject("image") == null) {
                "$questionId image answers must not contain a memory key"
            }
        }

        val question = QuestionEntity(
            releaseId = releaseId,
            questionId = questionId,
            sourceId = metadata.requiredString("sourceId"),
            officialNumber = metadata.requiredInt("officialNumber"),
            scope = metadata.requiredString("scope"),
            stateCode = metadata.optionalString("stateCode"),
            category = metadata.optionalString("category"),
            categoryStatus = metadata.requiredString("categoryStatus"),
            sourceVersion = metadata.requiredString("sourceVersion"),
            sourcePdfSha256 = metadata.requiredString("sourcePdfSha256"),
            sourcePage = source.requiredInt("page"),
            bamfInternalId = source.requiredString("bamfInternalId"),
            correctAnswerId = raw.requiredString("correctAnswerId"),
            germanQuestionText = german.requiredString("questionText"),
            questionImageSourcePath = questionImage?.optionalString("sourcePath"),
            questionImageSourceUrl = questionImage?.optionalString("sourceUrl"),
            questionImageStoragePath = questionImage?.optionalString("storagePath"),
            questionImageSha256 = questionImage?.optionalString("sha256"),
            questionImageWidth = questionImage?.optionalInt("width"),
            questionImageHeight = questionImage?.optionalInt("height"),
            questionImageMimeType = questionImage?.optionalString("mimeType"),
            memoryQuestionCue = memoryQuestionCue,
            memoryAnswerCue = memoryAnswerCue,
            memoryKeyStatus = memoryKeyStatus,
        )

        val answers = germanAnswers.mapIndexed { index, answer ->
            val image = answer.optionalObject("image")
            AnswerEntity(
                releaseId = releaseId,
                questionId = questionId,
                answerId = answer.requiredString("id"),
                displayOrder = index,
                germanText = answer.requiredString("text"),
                imageSourcePath = image?.optionalString("sourcePath"),
                imageSourceUrl = image?.optionalString("sourceUrl"),
                imageStoragePath = image?.optionalString("storagePath"),
                imageSha256 = image?.optionalString("sha256"),
                imageWidth = image?.optionalInt("width"),
                imageHeight = image?.optionalInt("height"),
                imageMimeType = image?.optionalString("mimeType"),
            )
        }

        val mappedStudyAnswers = studyAnswers.mapIndexed { index, answer ->
            StudyAnswerEntity(
                releaseId = releaseId,
                questionId = questionId,
                languageCode = languageCode,
                answerId = answer.requiredString("id"),
                displayOrder = index,
                text = answer.requiredString("text"),
            )
        }

        val audit = study.optionalObject("audit")
        val translationAudit = audit?.let { value ->
            TranslationAuditEntity(
                releaseId = releaseId,
                questionId = questionId,
                languageCode = languageCode,
                provider = value.optionalString("provider"),
                sourceRecordNumber = value.optionalScalarString("sourceRecordNumber"),
                sourceRecordId = value.optionalString("sourceRecordId"),
                sourceCommit = value.optionalString("sourceCommit"),
                customId = value.optionalString("customId"),
                model = value.optionalString("model"),
                generatedAt = value.optionalString("generatedAt"),
                questionMatchScore = value.optionalDouble("questionMatchScore"),
                answerSetMatchScore = value.optionalDouble("answerSetMatchScore"),
            )
        }

        val answerMatchScores = audit?.optionalNumberList("answerMatchScores").orEmpty()
        val fallbackAnswerIds = audit?.optionalStringList("fallbackAnswerIds")?.toSet().orEmpty()
        if (answerMatchScores.isNotEmpty()) {
            syncRequire(answerMatchScores.size == mappedStudyAnswers.size) {
                "$questionId audit answerMatchScores do not match study answers"
            }
        }
        val answerAudits = if (translationAudit != null && answerMatchScores.isNotEmpty()) {
            mappedStudyAnswers.mapIndexed { index, answer ->
                TranslationAnswerAuditEntity(
                    releaseId = releaseId,
                    questionId = questionId,
                    languageCode = languageCode,
                    answerId = answer.answerId,
                    matchScore = answerMatchScores[index].toDouble(),
                    usedFallback = answer.answerId in fallbackAnswerIds,
                )
            }
        } else {
            emptyList()
        }

        return MappedQuestion(
            question = question,
            answers = answers,
            studyContent = StudyContentEntity(
                releaseId = releaseId,
                questionId = questionId,
                languageCode = languageCode,
                questionText = study.requiredString("questionText"),
                explanation = study.requiredString("explanation"),
                textStatus = study.optionalString("textStatus"),
                translationStatus = study.optionalString("translationStatus"),
                explanationStatus = study.requiredString("explanationStatus"),
                reviewed = study.requiredBoolean("reviewed"),
            ),
            studyAnswers = mappedStudyAnswers,
            translationAudit = translationAudit,
            translationAnswerAudits = answerAudits,
        )
    }

    private data class MappedQuestion(
        val question: QuestionEntity,
        val answers: List<AnswerEntity>,
        val studyContent: StudyContentEntity,
        val studyAnswers: List<StudyAnswerEntity>,
        val translationAudit: TranslationAuditEntity?,
        val translationAnswerAudits: List<TranslationAnswerAuditEntity>,
    )

    private fun validateMemoryKey(
        questionId: String,
        questionCue: String,
        answerCue: String,
        status: String,
    ) {
        listOf("questionCue" to questionCue, "answerCue" to answerCue).forEach { (label, cue) ->
            syncRequire(cue.length <= MEMORY_KEY_MAX_LENGTH) {
                "$questionId memory key $label is too long"
            }
            syncRequire(
                '\n' !in cue && '\r' !in cue &&
                    MEMORY_KEY_ARROW !in cue && MEMORY_KEY_ASCII_ARROW !in cue,
            ) {
                "$questionId memory key $label contains a forbidden separator"
            }
            syncRequire(cue.trim().split(Regex("\\s+")).size <= MEMORY_KEY_MAX_WORDS) {
                "$questionId memory key $label contains too many words"
            }
        }
        syncRequire(status in MEMORY_KEY_STATUSES) {
            "$questionId memory key status is not supported"
        }
    }

    private companion object {
        const val CHUNK_ID_WIDTH = 4
        const val GERMAN_LANGUAGE_CODE = "de"
        const val READY_STATUS = "READY"
        const val SUPPORTED_SCHEMA_VERSION = 3
        const val MEMORY_KEY_MAX_LENGTH = 80
        const val MEMORY_KEY_MAX_WORDS = 8
        const val MEMORY_KEY_ARROW = "→"
        const val MEMORY_KEY_ASCII_ARROW = "->"
        val MEMORY_KEY_STATUSES = setOf(
            "AI_GENERATED_NEEDS_REVIEW",
            "HUMAN_REVIEWED",
        )
    }
}

private fun Map<String, Any?>.requiredString(key: String): String =
    (this[key] as? String)?.takeIf(String::isNotBlank)
        ?: throw ContentSyncException("Missing or invalid string: $key")

private fun Map<String, Any?>.optionalString(key: String): String? = when (val value = this[key]) {
    null -> null
    is String -> value
    else -> throw ContentSyncException("Invalid optional string: $key")
}

private fun Map<String, Any?>.optionalScalarString(key: String): String? = when (
    val value = this[key]
) {
    null -> null
    is String -> value
    is Number -> value.toString()
    else -> throw ContentSyncException("Invalid optional scalar string: $key")
}

private fun Map<String, Any?>.requiredInt(key: String): Int =
    this[key].exactLongOrNull()
        ?.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }
        ?.toInt()
        ?: throw ContentSyncException("Missing or invalid integer: $key")

private fun Map<String, Any?>.optionalInt(key: String): Int? = when (val value = this[key]) {
    null -> null
    else -> value.exactLongOrNull()
        ?.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }
        ?.toInt()
        ?: throw ContentSyncException("Invalid optional integer: $key")
}

private fun Map<String, Any?>.requiredLong(key: String): Long =
    this[key].exactLongOrNull()
        ?: throw ContentSyncException("Missing or invalid long: $key")

private fun Map<String, Any?>.optionalDouble(key: String): Double? = when (val value = this[key]) {
    null -> null
    is Number -> value.toDouble()
    else -> throw ContentSyncException("Invalid optional number: $key")
}

private fun Map<String, Any?>.requiredBoolean(key: String): Boolean =
    this[key] as? Boolean
        ?: throw ContentSyncException("Missing or invalid boolean: $key")

private fun Map<String, Any?>.requiredObject(key: String): Map<String, Any?> =
    this[key].asStringObject(key)

private fun Map<String, Any?>.optionalObject(key: String): Map<String, Any?>? = when (
    val value = this[key]
) {
    null -> null
    else -> value.asStringObject(key)
}

private fun Map<String, Any?>.requiredObjectList(key: String): List<Map<String, Any?>> {
    val values = this[key] as? List<*>
        ?: throw ContentSyncException("Missing or invalid object list: $key")
    return values.mapIndexed { index, value -> value.asStringObject("$key[$index]") }
}

private fun Map<String, Any?>.requiredStringList(key: String): List<String> {
    val values = this[key] as? List<*>
        ?: throw ContentSyncException("Missing or invalid string list: $key")
    return values.mapIndexed { index, value ->
        value as? String
            ?: throw ContentSyncException("Invalid string at $key[$index]")
    }
}

private fun Map<String, Any?>.optionalStringList(key: String): List<String>? = when (
    val value = this[key]
) {
    null -> null
    is List<*> -> value.mapIndexed { index, item ->
        item as? String
            ?: throw ContentSyncException("Invalid string at $key[$index]")
    }
    else -> throw ContentSyncException("Invalid optional string list: $key")
}

private fun Map<String, Any?>.optionalNumberList(key: String): List<Number>? = when (
    val value = this[key]
) {
    null -> null
    is List<*> -> value.mapIndexed { index, item ->
        item as? Number
            ?: throw ContentSyncException("Invalid number at $key[$index]")
    }
    else -> throw ContentSyncException("Invalid optional number list: $key")
}

private fun Map<String, Any?>.requiredStringMap(key: String): Map<String, String> {
    val values = this[key].asStringObject(key)
    return values.mapValues { (nestedKey, value) ->
        value as? String
            ?: throw ContentSyncException("Invalid string at $key.$nestedKey")
    }
}

private fun Any?.asStringObject(label: String): Map<String, Any?> {
    val value = this as? Map<*, *>
        ?: throw ContentSyncException("Missing or invalid object: $label")
    return value.entries.associate { entry ->
        val key = entry.key as? String
            ?: throw ContentSyncException("Invalid object key in $label")
        key to entry.value
    }
}

private fun Any?.exactLongOrNull(): Long? = when (this) {
    is Byte -> toLong()
    is Short -> toLong()
    is Int -> toLong()
    is Long -> this
    else -> null
}

private inline fun syncRequire(condition: Boolean, lazyMessage: () -> String) {
    if (!condition) throw ContentSyncException(lazyMessage())
}
