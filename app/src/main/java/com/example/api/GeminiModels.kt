package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseFormat") val responseFormat: ResponseFormat? = null,
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null,
    @Json(name = "thinkingConfig") val thinkingConfig: ThinkingConfig? = null,
    @Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @Json(name = "speechConfig") val speechConfig: SpeechConfig? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    @Json(name = "type") val type: String? = null, // "application/json"
    @Json(name = "responseSchema") val responseSchema: ResponseSchema? = null
)

@JsonClass(generateAdapter = true)
data class ResponseSchema(
    @Json(name = "type") val type: String,
    @Json(name = "properties") val properties: Map<String, SchemaProperty>? = null,
    @Json(name = "required") val required: List<String>? = null,
    @Json(name = "items") val items: ResponseSchema? = null
)

@JsonClass(generateAdapter = true)
data class SchemaProperty(
    @Json(name = "type") val type: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "properties") val properties: Map<String, SchemaProperty>? = null,
    @Json(name = "required") val required: List<String>? = null,
    @Json(name = "items") val items: ResponseSchema? = null
)

@JsonClass(generateAdapter = true)
data class ThinkingConfig(
    @Json(name = "thinkingLevel") val thinkingLevel: String
)

@JsonClass(generateAdapter = true)
data class SpeechConfig(
    @Json(name = "voiceConfig") val voiceConfig: VoiceConfig
)

@JsonClass(generateAdapter = true)
data class VoiceConfig(
    @Json(name = "prebuiltVoiceConfig") val prebuiltVoiceConfig: PrebuiltVoiceConfig
)

@JsonClass(generateAdapter = true)
data class PrebuiltVoiceConfig(
    @Json(name = "voiceName") val voiceName: String
)

// --- Response Mapping ---
@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

// --- Specific Structured Models ---
@JsonClass(generateAdapter = true)
data class EvaluationReport(
    @Json(name = "overallBand") val overallBand: Double,
    @Json(name = "fluencyBand") val fluencyBand: Double,
    @Json(name = "fluencyFeedback") val fluencyFeedback: String,
    @Json(name = "lexicalBand") val lexicalBand: Double,
    @Json(name = "lexicalFeedback") val lexicalFeedback: String,
    @Json(name = "grammarBand") val grammarBand: Double,
    @Json(name = "grammarFeedback") val grammarFeedback: String,
    @Json(name = "pronunciationBand") val pronunciationBand: Double,
    @Json(name = "pronunciationFeedback") val pronunciationFeedback: String,
    @Json(name = "strengths") val strengths: List<String>,
    @Json(name = "weaknesses") val weaknesses: List<String>,
    @Json(name = "coachPrescription") val coachPrescription: String,
    @Json(name = "topImprovementPoints") val topImprovementPoints: List<String>,
    @Json(name = "modelAnswers") val modelAnswers: List<ModelAnswerItem>
)

@JsonClass(generateAdapter = true)
data class ModelAnswerItem(
    @Json(name = "question") val question: String,
    @Json(name = "modelAnswer") val modelAnswer: String
)
