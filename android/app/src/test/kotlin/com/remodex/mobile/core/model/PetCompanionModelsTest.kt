package com.remodex.mobile.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PetCompanionModelsTest {
    @Test
    fun layout_clampsAwayFromSidebarAndBottomControls() {
        val (x, y) =
            PetCompanionLayout.clampedPoint(
                pointX = 10f,
                pointY = 900f,
                containerWidth = 400f,
                containerHeight = 800f,
                petWidth = 176f,
                petHeight = 150f,
                leftExclusionWidth = 120f,
                bottomExclusionHeight = 180f,
            )
        assertTrue(x >= 120f)
        assertTrue(y <= 800f - 180f)
    }

    @Test
    fun frameSelection_usesPhaseRowAndCount() {
        assertEquals(6, PetCompanionPhase.Idle.frameCount)
        assertEquals(0, PetCompanionPhase.Idle.rowIndex)
        assertEquals(8, PetCompanionPhase.RunningRight.frameCount)
        assertEquals(1, PetCompanionPhase.RunningRight.rowIndex)
    }

    @Test
    fun petListFromRpcResult_readsAvatarsArray() {
        val pets =
            petListFromRpcResult(
                JSONValue.Obj(
                    mapOf(
                        "avatars" to
                            JSONValue.Arr(
                                listOf(
                                    JSONValue.Obj(
                                        mapOf(
                                            "id" to JSONValue.Str("pet-1"),
                                            "displayName" to JSONValue.Str("Codex Cat"),
                                        ),
                                    ),
                                ),
                            ),
                    ),
                ),
            )
        assertEquals(1, pets.size)
        assertEquals("pet-1", pets.first().id)
    }

    @Test
    fun petCompanionFromJson_rejectsMissingSpritesheetWhenRequired() {
        val pet =
            petCompanionFromJson(
                JSONValue.Obj(
                    mapOf(
                        "id" to JSONValue.Str("pet-1"),
                        "displayName" to JSONValue.Str("Codex Cat"),
                    ),
                ),
                requiresSpritesheetData = true,
        )
        assertEquals(null, pet)
    }

    @Test
    fun spritesheetLimits_rejectOversizedPayloads() {
        assertTrue(PetCompanionSpritesheetLimits.allowsByteLength(null))
        assertTrue(PetCompanionSpritesheetLimits.allowsByteLength(PetCompanionSpritesheetLimits.MAX_SPRITESHEET_BYTES))
        assertEquals(
            false,
            PetCompanionSpritesheetLimits.allowsByteLength(PetCompanionSpritesheetLimits.MAX_SPRITESHEET_BYTES + 1),
        )
    }
}
