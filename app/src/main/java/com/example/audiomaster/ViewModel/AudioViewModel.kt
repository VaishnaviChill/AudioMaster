package com.example.audiomaster.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.audiomaster.Model.AudioSettings

class AudioViewModel : ViewModel() {
    private val _audioSettings = MutableLiveData<AudioSettings>().apply {
        value = AudioSettings()
    }
    val audioSettings : LiveData<AudioSettings> = _audioSettings

    fun setBassBoost(level: Int){
        _audioSettings.value = _audioSettings.value?.copy(bassBoost = level)
    }

    fun setTrebleSound(level: Int){
        _audioSettings.value = _audioSettings.value?.copy(trebleBoost = level)
    }

    fun toggleSurroundSound(enabled: Boolean){
        _audioSettings.value = _audioSettings.value?.copy(surroundSuound = enabled)
    }
}