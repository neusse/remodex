package com.remodex.mobile.ui.pet

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remodex.mobile.core.model.PetCompanion
import com.remodex.mobile.core.model.PetCompanionLayout
import com.remodex.mobile.core.model.PetCompanionPhase
import com.remodex.mobile.core.model.PetCompanionPosition
import com.remodex.mobile.core.model.PetCompanionSpritesheetLimits
import com.remodex.mobile.core.model.PetCompanionStatusSnapshot
import com.remodex.mobile.core.model.isConnectedForPet
import com.remodex.mobile.core.transport.ConnectionState
import com.remodex.mobile.data.CodexRepository
import com.remodex.mobile.data.PetCompanionStore
import com.remodex.mobile.services.CodexService
import com.remodex.mobile.core.model.PetCompanionStatusLogic
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val SpriteDisplaySize = 92.dp to 100.dp
private val CompanionDisplaySize = 176.dp to 150.dp

@Composable
fun PetCompanionOverlay(
    repository: CodexRepository,
    store: PetCompanionStore,
    isInteractionEnabled: Boolean,
    bottomExclusionHeight: androidx.compose.ui.unit.Dp = 16.dp,
    modifier: Modifier = Modifier,
) {
    val isEnabled by store.isEnabled.collectAsStateWithLifecycle()
    val renderedPet by store.renderedPet.collectAsStateWithLifecycle()
    val selectedPetId by store.selectedPetId.collectAsStateWithLifecycle()
    val position by store.position.collectAsStateWithLifecycle()
    val statusSnapshot by store.statusSnapshot.collectAsStateWithLifecycle()
    val connectionState by repository.connectionState.collectAsStateWithLifecycle()
    val isSessionReady by repository.isSessionReady.collectAsStateWithLifecycle()
    val isConnected = connectionState.isConnectedForPet()
    val codexService = repository as? CodexService

    LaunchedEffect(isConnected, isSessionReady, isEnabled, codexService) {
        if (isConnected && isSessionReady && isEnabled && codexService != null) {
            store.loadPetsIfNeeded(codexService)
            store.loadSelectedPet(codexService)
        }
    }

    LaunchedEffect(isConnected, isSessionReady, isEnabled, selectedPetId, codexService) {
        if (isConnected && isSessionReady && isEnabled && codexService != null) {
            store.loadSelectedPet(codexService)
        }
    }

    LaunchedEffect(isEnabled, isConnected, isSessionReady) {
        if (!isEnabled || !isConnected || !isSessionReady) {
            store.updateStatus(PetCompanionStatusSnapshot.idle)
            return@LaunchedEffect
        }
        while (isActive) {
            store.updateStatus(PetCompanionStatusLogic.snapshot(repository))
            delay(1_000)
        }
    }

    if (!isEnabled || renderedPet == null) return

    val density = LocalDensity.current
    val petWidthPx = with(density) { CompanionDisplaySize.first.toPx() }
    val petHeightPx = with(density) { CompanionDisplaySize.second.toPx() }
    val bottomExclusionPx = with(density) { bottomExclusionHeight.toPx() }

    var containerWidthPx by remember { mutableStateOf(0f) }
    var containerHeightPx by remember { mutableStateOf(0f) }
    var dragStartX by remember { mutableStateOf<Float?>(null) }
    var dragStartY by remember { mutableStateOf<Float?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPhase by remember { mutableStateOf(PetCompanionPhase.RunningRight) }
    var transientPhase by remember { mutableStateOf<PetCompanionPhase?>(null) }

    val displayPhase =
        when {
            isDragging -> dragPhase
            transientPhase != null -> transientPhase!!
            else -> statusSnapshot.phase
        }

    val basePoint =
        remember(position, containerWidthPx, containerHeightPx, petWidthPx, petHeightPx, bottomExclusionPx) {
            if (containerWidthPx <= 0f || containerHeightPx <= 0f) {
                0f to 0f
            } else {
                PetCompanionLayout.pointForPosition(
                    position = position,
                    containerWidth = containerWidthPx,
                    containerHeight = containerHeightPx,
                    petWidth = petWidthPx,
                    petHeight = petHeightPx,
                    leftExclusionWidth = 0f,
                    bottomExclusionHeight = bottomExclusionPx,
                )
            }
        }

    val dragPoint =
        remember(basePoint, dragStartX, dragStartY, dragOffsetX, dragOffsetY, isDragging) {
            val startX = dragStartX ?: basePoint.first
            val startY = dragStartY ?: basePoint.second
            if (!isDragging) {
                basePoint
            } else {
                PetCompanionLayout.clampedPoint(
                    startX + dragOffsetX,
                    startY + dragOffsetY,
                    containerWidthPx,
                    containerHeightPx,
                    petWidthPx,
                    petHeightPx,
                    0f,
                    bottomExclusionPx,
                )
            }
        }

    val animatedX by animateFloatAsState(dragPoint.first, label = "petX")
    val animatedY by animateFloatAsState(dragPoint.second, label = "petY")

    LaunchedEffect(transientPhase) {
        val phase = transientPhase ?: return@LaunchedEffect
        delay(850)
        if (transientPhase == phase) {
            transientPhase = null
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    containerWidthPx = size.width.toFloat()
                    containerHeightPx = size.height.toFloat()
                },
    ) {
        PetCompanionBody(
            pet = renderedPet!!,
            status = statusSnapshot.copy(phase = displayPhase),
            modifier =
                Modifier
                    .offset {
                        IntOffset(
                            (animatedX - petWidthPx / 2f).roundToInt(),
                            (animatedY - petHeightPx / 2f).roundToInt(),
                        )
                    }
                    .size(CompanionDisplaySize.first, CompanionDisplaySize.second)
                    .align(Alignment.TopStart)
                    .pointerInput(isInteractionEnabled, containerWidthPx, containerHeightPx) {
                        if (!isInteractionEnabled) return@pointerInput
                        detectTapGestures {
                            transientPhase = PetCompanionPhase.Jumping
                        }
                    }
                    .pointerInput(isInteractionEnabled, containerWidthPx, containerHeightPx, basePoint) {
                        if (!isInteractionEnabled) return@pointerInput
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragStartX = basePoint.first
                                dragStartY = basePoint.second
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetX += dragAmount.x
                                dragOffsetY += dragAmount.y
                                if (kotlin.math.abs(dragOffsetX) >= 4f) {
                                    dragPhase =
                                        if (dragOffsetX >= 0f) {
                                            PetCompanionPhase.RunningRight
                                        } else {
                                            PetCompanionPhase.RunningLeft
                                        }
                                }
                            },
                            onDragEnd = {
                                val point =
                                    PetCompanionLayout.clampedPoint(
                                        (dragStartX ?: basePoint.first) + dragOffsetX,
                                        (dragStartY ?: basePoint.second) + dragOffsetY,
                                        containerWidthPx,
                                        containerHeightPx,
                                        petWidthPx,
                                        petHeightPx,
                                        0f,
                                        bottomExclusionPx,
                                    )
                                store.updatePosition(
                                    PetCompanionLayout.normalizedPositionForPoint(
                                        point.first,
                                        point.second,
                                        containerWidthPx,
                                        containerHeightPx,
                                        petWidthPx,
                                        petHeightPx,
                                        0f,
                                        bottomExclusionPx,
                                    ),
                                )
                                isDragging = false
                                dragStartX = null
                                dragStartY = null
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                dragStartX = null
                                dragStartY = null
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                            },
                        )
                    },
        )
    }
}

@Composable
private fun PetCompanionBody(
    pet: PetCompanion,
    status: PetCompanionStatusSnapshot,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PetSpriteFrame(
            pet = pet,
            phase = status.phase,
            modifier = Modifier.size(SpriteDisplaySize.first, SpriteDisplaySize.second),
        )
        if (status.showsLabel) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    status.title?.takeIf { it.isNotBlank() }?.let { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                    status.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PetSpriteFrame(
    pet: PetCompanion,
    phase: PetCompanionPhase,
    modifier: Modifier = Modifier,
) {
    val atlas =
        remember(pet.spritesheetDataUrl, pet.spritesheetByteLength, pet.id) {
            PetSpriteDecoder.decodeAtlas(pet.spritesheetDataUrl, pet.spritesheetByteLength)
        }
    var frameIndex by remember(phase) { mutableStateOf(0) }

    LaunchedEffect(phase, atlas) {
        frameIndex = 0
        if (atlas == null) return@LaunchedEffect
        while (isActive) {
            val durations = phase.frameDurationsMs
            val delayMs = durations.getOrElse(frameIndex % durations.size) { 140L }
            delay(delayMs)
            frameIndex = (frameIndex + 1) % phase.frameCount.coerceAtLeast(1)
        }
    }

    val frameBitmap =
        remember(atlas, phase, frameIndex) {
            atlas?.frame(phase, frameIndex) ?: atlas?.frame(PetCompanionPhase.Idle, 0)
        }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (frameBitmap != null) {
            Image(
                bitmap = frameBitmap.asImageBitmap(),
                contentDescription = pet.displayName,
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

private object PetSpriteDecoder {
    fun decodeAtlas(
        dataUrl: String?,
        byteLength: Int?,
    ): PetSpriteAtlas? {
        if (!PetCompanionSpritesheetLimits.allowsByteLength(byteLength)) return null
        val bitmap = decodeBitmap(dataUrl) ?: return null
        return PetSpriteAtlas(bitmap)
    }

    private fun decodeBitmap(dataUrl: String?): Bitmap? {
        val raw = dataUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val commaIndex = raw.indexOf(',')
        if (commaIndex < 0) return null
        val base64 = raw.substring(commaIndex + 1)
        val bytes =
            runCatching { Base64.decode(base64, Base64.DEFAULT) }
                .getOrNull()
                ?: return null
        if (!PetCompanionSpritesheetLimits.allowsByteLength(bytes.size)) return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}

private class PetSpriteAtlas(
    private val bitmap: Bitmap,
) {
    private val frameCache = mutableMapOf<String, Bitmap>()

    fun frame(
        phase: PetCompanionPhase,
        index: Int,
    ): Bitmap? {
        val normalizedIndex = index % phase.frameCount.coerceAtLeast(1)
        val key = "${phase.name}-$normalizedIndex"
        return frameCache.getOrPut(key) {
            val left = normalizedIndex * PetCompanionLayout.CELL_WIDTH
            val top = phase.rowIndex * PetCompanionLayout.CELL_HEIGHT
            if (left + PetCompanionLayout.CELL_WIDTH > bitmap.width ||
                top + PetCompanionLayout.CELL_HEIGHT > bitmap.height
            ) {
                bitmap
            } else {
                Bitmap.createBitmap(
                    bitmap,
                    left,
                    top,
                    PetCompanionLayout.CELL_WIDTH,
                    PetCompanionLayout.CELL_HEIGHT,
                )
            }
        }
    }
}
