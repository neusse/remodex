package com.remodex.mobile.data

import android.content.Context
import com.remodex.mobile.core.model.PetCompanion
import com.remodex.mobile.core.model.PetCompanionPosition
import com.remodex.mobile.core.model.PetCompanionStatusSnapshot
import com.remodex.mobile.core.transport.ConnectionState
import com.remodex.mobile.services.CodexService
import com.remodex.mobile.services.listPets
import com.remodex.mobile.services.readPet
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PetCompanionStore(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val prefs =
        appContext.getSharedPreferences(
            "remodex_pet_companion",
            Context.MODE_PRIVATE,
        )
    private val cacheDir = File(appContext.filesDir, "pet-companion-cache")
    private val initialSelectedPetId = prefs.getString(KEY_SELECTED_ID, null)
    private val initialCachedPet = initialSelectedPetId?.let(::readCachedPet)
    private val selectedPetLoadMutex = Mutex()

    private val _isEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _selectedPetId = MutableStateFlow(initialSelectedPetId)
    val selectedPetId: StateFlow<String?> = _selectedPetId.asStateFlow()

    private val _availablePets = MutableStateFlow(initialCachedPet?.let(::listOf).orEmpty())
    val availablePets: StateFlow<List<PetCompanion>> = _availablePets.asStateFlow()

    private val _renderedPet = MutableStateFlow(initialCachedPet)
    val renderedPet: StateFlow<PetCompanion?> = _renderedPet.asStateFlow()

    private val _position =
        MutableStateFlow(
            if (prefs.contains(KEY_POSITION_X) && prefs.contains(KEY_POSITION_Y)) {
                PetCompanionPosition(
                    normalizedX = prefs.getFloat(KEY_POSITION_X, 0.82f).toDouble(),
                    normalizedY = prefs.getFloat(KEY_POSITION_Y, 0.72f).toDouble(),
                )
            } else {
                PetCompanionPosition.Default
            },
        )
    val position: StateFlow<PetCompanionPosition> = _position.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _statusSnapshot = MutableStateFlow(PetCompanionStatusSnapshot.idle)
    val statusSnapshot: StateFlow<PetCompanionStatusSnapshot> = _statusSnapshot.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun selectPet(id: String?) {
        if (_selectedPetId.value == id) return
        _selectedPetId.value = id
        prefs.edit().putString(KEY_SELECTED_ID, id).apply()
        val cachedPet = id?.let(::readCachedPet)
        _renderedPet.value = cachedPet
        cachedPet?.let(::appendAvailablePetIfMissing)
    }

    suspend fun loadPetsIfNeeded(codex: CodexService) {
        if (_availablePets.value.isNotEmpty()) return
        refreshPets(codex)
    }

    suspend fun refreshPets(codex: CodexService) {
        if (!codex.isReadyForPetLoading()) {
            _errorMessage.value = null
            return
        }
        if (_isLoading.value) return

        _isLoading.value = true
        try {
            val pets = codex.listPets(includeData = false)
            setAvailablePets(pets)
            _errorMessage.value = null
            val selectedId = _selectedPetId.value
            if (selectedId != null && pets.any { it.id == selectedId }) {
                loadSelectedPet(codex)
                return
            }
            selectPet(pets.firstOrNull()?.id)
            loadSelectedPet(codex)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _errorMessage.value = error.message ?: "Could not load pets."
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun loadSelectedPet(codex: CodexService) {
        if (!codex.isReadyForPetLoading()) return
        val selected = selectedPet()
            ?: run {
                _renderedPet.value = null
                return
            }
        if (_renderedPet.value?.id == selected.id && !_renderedPet.value?.spritesheetDataUrl.isNullOrBlank()) {
            return
        }

        selectedPetLoadMutex.withLock {
            val requestedPetId = selected.id
            if (_renderedPet.value?.id == requestedPetId && !_renderedPet.value?.spritesheetDataUrl.isNullOrBlank()) {
                return
            }
            try {
                val loadedPet = codex.readPet(requestedPetId)
                if (_selectedPetId.value != null && _selectedPetId.value != requestedPetId) return
                setRenderedPet(loadedPet)
                withContext(Dispatchers.IO) {
                    cachePet(loadedPet)
                }
                _errorMessage.value = null
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (_selectedPetId.value != null && _selectedPetId.value != requestedPetId) return
                _renderedPet.value = null
                _errorMessage.value = error.message ?: "Could not load ${selected.displayName}."
            }
        }
    }

    fun updatePosition(position: PetCompanionPosition) {
        _position.value = position
        prefs.edit()
            .putFloat(KEY_POSITION_X, position.normalizedX.toFloat())
            .putFloat(KEY_POSITION_Y, position.normalizedY.toFloat())
            .apply()
    }

    fun setAvailablePets(pets: List<PetCompanion>) {
        val rendered = _renderedPet.value
        _availablePets.value =
            if (rendered != null && pets.none { it.id == rendered.id }) {
                listOf(rendered) + pets
            } else {
                pets
            }
    }

    fun setRenderedPet(pet: PetCompanion?) {
        _renderedPet.value = pet
        pet?.let(::appendAvailablePetIfMissing)
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun updateStatus(snapshot: PetCompanionStatusSnapshot) {
        if (_statusSnapshot.value != snapshot) {
            _statusSnapshot.value = snapshot
        }
    }

    fun selectedPet(): PetCompanion? {
        val selectedId = _selectedPetId.value
        if (selectedId != null) {
            _availablePets.value.firstOrNull { it.id == selectedId }?.let { return it }
            readCachedPet(selectedId)?.let { cachedPet ->
                appendAvailablePetIfMissing(cachedPet)
                _renderedPet.value = cachedPet
                return cachedPet
            }
        }
        return _availablePets.value.firstOrNull()
    }

    fun cachePet(pet: PetCompanion) {
        val dataUrl = pet.spritesheetDataUrl?.takeIf { it.isNotBlank() } ?: return
        val key = cacheKey(pet.id)
        cacheDir.mkdirs()
        File(cacheDir, "$key.dataurl").writeText(dataUrl)
        prefs.edit()
            .putString("$KEY_CACHE_PREFIX.$key.id", pet.id)
            .putString("$KEY_CACHE_PREFIX.$key.folderName", pet.folderName)
            .putString("$KEY_CACHE_PREFIX.$key.displayName", pet.displayName)
            .putString("$KEY_CACHE_PREFIX.$key.description", pet.description)
            .putString("$KEY_CACHE_PREFIX.$key.mimeType", pet.spritesheetMimeType)
            .putInt("$KEY_CACHE_PREFIX.$key.byteLength", pet.spritesheetByteLength ?: 0)
            .apply()
    }

    fun restoreCachedPet(id: String?): Boolean {
        val pet = id?.let(::readCachedPet) ?: return false
        _renderedPet.value = pet
        appendAvailablePetIfMissing(pet)
        return true
    }

    private fun appendAvailablePetIfMissing(pet: PetCompanion) {
        if (_availablePets.value.none { it.id == pet.id }) {
            _availablePets.value = listOf(pet) + _availablePets.value
        }
    }

    private fun readCachedPet(id: String): PetCompanion? {
        val key = cacheKey(id)
        return runCatching {
            val dataUrl = File(cacheDir, "$key.dataurl").takeIf { it.isFile }?.readText() ?: return null
            val cachedId = prefs.getString("$KEY_CACHE_PREFIX.$key.id", id) ?: id
            PetCompanion(
                id = cachedId,
                folderName = prefs.getString("$KEY_CACHE_PREFIX.$key.folderName", null) ?: cachedId,
                displayName = prefs.getString("$KEY_CACHE_PREFIX.$key.displayName", null) ?: cachedId,
                description = prefs.getString("$KEY_CACHE_PREFIX.$key.description", null),
                spritesheetDataUrl = dataUrl,
                spritesheetMimeType = prefs.getString("$KEY_CACHE_PREFIX.$key.mimeType", null),
                spritesheetByteLength = prefs.getInt("$KEY_CACHE_PREFIX.$key.byteLength", 0).takeIf { it > 0 },
            )
        }.getOrNull()
    }

    private fun cacheKey(id: String): String =
        id.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun CodexService.isReadyForPetLoading(): Boolean =
        connectionState.value is ConnectionState.Connected && isSessionReady.value

    private companion object {
        const val KEY_ENABLED = "codex.pet.isEnabled"
        const val KEY_SELECTED_ID = "codex.pet.selectedID"
        const val KEY_POSITION_X = "codex.pet.positionX"
        const val KEY_POSITION_Y = "codex.pet.positionY"
        const val KEY_CACHE_PREFIX = "codex.pet.cache"
    }
}
