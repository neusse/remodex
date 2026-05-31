package com.remodex.mobile.core.model

data class PetCompanion(
    val id: String,
    val folderName: String,
    val displayName: String,
    val description: String? = null,
    val spritesheetDataUrl: String? = null,
    val spritesheetMimeType: String? = null,
    val spritesheetByteLength: Int? = null,
)

object PetCompanionSpritesheetLimits {
    const val MAX_SPRITESHEET_BYTES = 5 * 1024 * 1024

    fun allowsByteLength(byteLength: Int?): Boolean =
        byteLength == null || byteLength in 0..MAX_SPRITESHEET_BYTES
}

data class PetCompanionStatusSnapshot(
    val phase: PetCompanionPhase,
    val title: String? = null,
    val detail: String? = null,
) {
    companion object {
        val idle = PetCompanionStatusSnapshot(phase = PetCompanionPhase.Idle)
    }

    val showsLabel: Boolean get() = !title.isNullOrBlank() || !detail.isNullOrBlank()
}

enum class PetCompanionPhase {
    Idle,
    RunningRight,
    RunningLeft,
    Waving,
    Jumping,
    Failed,
    Waiting,
    Running,
    Review,
    ;

    val rowIndex: Int
        get() =
            when (this) {
                Idle -> 0
                RunningRight -> 1
                RunningLeft -> 2
                Waving -> 3
                Jumping -> 4
                Failed -> 5
                Waiting -> 6
                Running -> 7
                Review -> 8
            }

    val frameCount: Int
        get() =
            when (this) {
                Idle, Waiting, Running, Review -> 6
                RunningRight, RunningLeft, Failed -> 8
                Waving -> 4
                Jumping -> 5
            }

    val frameDurationsMs: List<Long>
        get() =
            when (this) {
                Idle -> listOf(280, 110, 110, 140, 140, 320)
                RunningRight, RunningLeft -> List(7) { 120L } + 220L
                Waving -> listOf(140, 140, 140, 280)
                Jumping -> listOf(140, 140, 140, 140, 280)
                Failed -> List(7) { 140L } + 240L
                Waiting -> List(5) { 150L } + 260L
                Running -> List(5) { 120L } + 220L
                Review -> List(5) { 150L } + 280L
            }
}

data class PetCompanionPosition(
    val normalizedX: Double,
    val normalizedY: Double,
) {
    companion object {
        val Default = PetCompanionPosition(normalizedX = 0.82, normalizedY = 0.72)
    }
}

object PetCompanionLayout {
    const val CELL_WIDTH = 192
    const val CELL_HEIGHT = 208
    const val ATLAS_COLUMNS = 8

    fun clampedPoint(
        pointX: Float,
        pointY: Float,
        containerWidth: Float,
        containerHeight: Float,
        petWidth: Float,
        petHeight: Float,
        leftExclusionWidth: Float,
        bottomExclusionHeight: Float,
    ): Pair<Float, Float> {
        if (containerWidth <= 0f || containerHeight <= 0f) return 0f to 0f
        val horizontalMargin = maxOf(12f, petWidth / 2f)
        val verticalMargin = maxOf(12f, petHeight / 2f)
        val minX = minOf(containerWidth - horizontalMargin, leftExclusionWidth + horizontalMargin)
        val maxX = maxOf(minX, containerWidth - horizontalMargin)
        val minY = verticalMargin
        val maxY = maxOf(minY, containerHeight - bottomExclusionHeight - verticalMargin)
        return minOf(maxOf(pointX, minX), maxX) to minOf(maxOf(pointY, minY), maxY)
    }

    fun pointForPosition(
        position: PetCompanionPosition,
        containerWidth: Float,
        containerHeight: Float,
        petWidth: Float,
        petHeight: Float,
        leftExclusionWidth: Float,
        bottomExclusionHeight: Float,
    ): Pair<Float, Float> {
        val rawX = containerWidth * position.normalizedX.toFloat()
        val rawY = containerHeight * position.normalizedY.toFloat()
        return clampedPoint(
            rawX,
            rawY,
            containerWidth,
            containerHeight,
            petWidth,
            petHeight,
            leftExclusionWidth,
            bottomExclusionHeight,
        )
    }

    fun normalizedPositionForPoint(
        pointX: Float,
        pointY: Float,
        containerWidth: Float,
        containerHeight: Float,
        petWidth: Float,
        petHeight: Float,
        leftExclusionWidth: Float,
        bottomExclusionHeight: Float,
    ): PetCompanionPosition {
        val clamped =
            clampedPoint(
                pointX,
                pointY,
                containerWidth,
                containerHeight,
                petWidth,
                petHeight,
                leftExclusionWidth,
                bottomExclusionHeight,
            )
        if (containerWidth <= 0f || containerHeight <= 0f) return PetCompanionPosition.Default
        return PetCompanionPosition(
            normalizedX = (clamped.first / containerWidth).toDouble(),
            normalizedY = (clamped.second / containerHeight).toDouble(),
        )
    }
}

fun petCompanionFromJson(
    value: JSONValue,
    requiresSpritesheetData: Boolean,
): PetCompanion? {
    val obj = value.objectValue ?: return null
    val id = obj["id"]?.stringValue?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val displayName = obj["displayName"]?.stringValue?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val dataUrl = obj["spritesheetDataUrl"]?.stringValue?.trim()
    if (requiresSpritesheetData && dataUrl.isNullOrEmpty()) return null
    return PetCompanion(
        id = id,
        folderName = obj["folderName"]?.stringValue ?: id,
        displayName = displayName,
        description = obj["description"]?.stringValue,
        spritesheetDataUrl = dataUrl,
        spritesheetMimeType = obj["spritesheetMimeType"]?.stringValue,
        spritesheetByteLength = obj["spritesheetByteLength"]?.intValue,
    )
}

fun petListFromRpcResult(result: JSONValue?): List<PetCompanion> {
    val obj = result?.objectValue ?: return emptyList()
    val rawPets = obj["avatars"]?.arrayValue ?: obj["pets"]?.arrayValue ?: return emptyList()
    return rawPets.mapNotNull { element -> petCompanionFromJson(element, requiresSpritesheetData = false) }
}
