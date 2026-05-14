package no.secret24h.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val secrets: List<Secret> = emptyList(),
    val sort: Sort = Sort.Recent,
    val moodFilter: String? = null,
    val reactionSort: String? = null,
    val distanceKm: Double? = null,   // null = everywhere
    val lastLocation: LatLng? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class SecretsViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init { load() }

    fun setSort(sort: Sort) {
        _state.value = _state.value.copy(sort = sort, moodFilter = null, reactionSort = null)
        load()
    }

    fun setMoodFilter(mood: String?) {
        _state.value = _state.value.copy(moodFilter = mood)
        load()
    }

    fun setReactionSort(reactionSort: String?) {
        _state.value = _state.value.copy(reactionSort = reactionSort)
        load()
    }

    fun setDistanceFilter(km: Double?) {
        if (km == null) {
            _state.value = _state.value.copy(distanceKm = null, lastLocation = null)
            load()
        } else {
            val cached = _state.value.lastLocation
            if (cached != null) {
                // Reuse cached location — instant switch between distances
                _state.value = _state.value.copy(distanceKm = km)
                load()
            } else {
                viewModelScope.launch {
                    val loc = LocationHelper.getLocation(ctx)
                    _state.value = _state.value.copy(distanceKm = km, lastLocation = loc)
                    load()
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val s = _state.value
                val secrets = Api.getSecrets(
                    sort         = s.sort,
                    moodFilter   = if (s.sort == Sort.Top) s.moodFilter else null,
                    reactionSort = if (s.sort == Sort.Top) s.reactionSort else null,
                    distanceKm   = s.distanceKm,
                    location     = s.lastLocation,
                )
                _state.value = _state.value.copy(secrets = secrets, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun postSecret(text: String, mood: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val location = LocationHelper.getLocation(ctx) // attach location if permission granted
                val secret = Api.postSecret(text, mood, location)
                if (_state.value.sort == Sort.Recent) {
                    _state.value = _state.value.copy(
                        secrets = listOf(secret) + _state.value.secrets
                    )
                }
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
                onDone()
            }
        }
    }

    fun react(secretId: String, type: String) {
        val col = when (type) {
            "me_too"   -> "reaction_me_too"
            "wild"     -> "reaction_wild"
            "doubtful" -> "reaction_doubtful"
            else -> return
        }
        _state.value = _state.value.copy(
            secrets = _state.value.secrets.map { s ->
                if (s.id != secretId) s else when (type) {
                    "me_too"   -> s.copy(reactionMeToo = s.reactionMeToo + 1)
                    "wild"     -> s.copy(reactionWild = s.reactionWild + 1)
                    "doubtful" -> s.copy(reactionDoubtful = s.reactionDoubtful + 1)
                    else -> s
                }
            }
        )
        viewModelScope.launch {
            try { Api.react(secretId, col) } catch (_: Exception) {}
        }
    }
}
