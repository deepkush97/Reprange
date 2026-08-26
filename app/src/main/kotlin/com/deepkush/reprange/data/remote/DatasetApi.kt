package com.deepkush.reprange.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class DatasetInstructionsDto(
    val en: String = "",
)

@Serializable
data class DatasetExerciseDto(
    val id: String,
    val name: String,
    val category: String = "",
    @SerialName("body_part") val bodyPart: String = "",
    val equipment: String = "",
    val instructions: DatasetInstructionsDto? = null,
    @SerialName("instruction_steps") val instructionSteps: Map<String, List<String>>? = null,
    @SerialName("muscle_group") val muscleGroup: String = "",
    @SerialName("secondary_muscles") val secondaryMuscles: List<String> = emptyList(),
    val target: String = "",
    @SerialName("media_id") val mediaId: String = "",
    val image: String = "",
    @SerialName("gif_url") val gifUrl: String = "",
    val attribution: String = "",
)

/** Retrofit ingest of the raw JSON from hasaneyldrm/exercises-dataset. */
interface DatasetApi {
    @GET("hasaneyldrm/exercises-dataset/main/data/exercises.json")
    suspend fun exercises(): List<DatasetExerciseDto>
}

object DatasetMedia {
    private const val BASE = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/"

    fun thumbnailUrl(relativePath: String): String =
        if (relativePath.startsWith("http")) relativePath else BASE + relativePath.removePrefix("/")

    fun gifUrl(relativePath: String): String = thumbnailUrl(relativePath)
}
