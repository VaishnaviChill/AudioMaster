package com.example.audiomaster

import android.app.VoiceInteractor
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.audiomaster.Model.AudioSettings
import com.example.audiomaster.ViewModel.AudioViewModel
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel : AudioViewModel
    private lateinit var bassSeekBar :SeekBar
    private lateinit var trebleSeekBar : SeekBar
    private lateinit var mediaPlayer : MediaPlayer
    private var bassBoost : BassBoost? = null
    private var equalizer: Equalizer? = null
    private var virtualizer: Virtualizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(AudioViewModel::class.java)
        mediaPlayer = MediaPlayer.create(this,R.raw.song)

        bassSeekBar = findViewById<SeekBar>(R.id.seekBarBass)
        trebleSeekBar = findViewById<SeekBar>(R.id.seekBarTreble)
        val surroundSwitch = findViewById<Switch>(R.id.switchSurround)
        val bassTextView = findViewById<TextView>(R.id.textViewBass)
        val trebleTextView = findViewById<TextView>(R.id.textViewTreble)
        val voiceCommandButton = findViewById<Button>(R.id.voiceCommandButton)
        val playButton = findViewById<Button>(R.id.playButton)

      //  trebleSeekBar.progress = 100


        playButton.setOnClickListener {
            if (mediaPlayer.isPlaying){
                mediaPlayer.pause()
                playButton.setText("Play")
            }else {
                mediaPlayer.start()
                playButton.setText("Pause")
            }
        }

        checkPermission()


        voiceCommandButton.setOnClickListener {
            initVoiceCommands()
        }

        viewModel.audioSettings.observe(this, Observer { settings ->
            bassTextView.text = "Bass Boost ${settings.bassBoost}"
            trebleTextView.text = "Treble Boost ${settings.trebleBoost}"
            surroundSwitch.isChecked = settings.surroundSuound
            applyDSPSettings(settings)

        })

        bassSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {

                viewModel.setBassBoost(p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                Toast.makeText(this@MainActivity, "${p0?.progress?.toShort()}", Toast.LENGTH_SHORT).show()
            }
        })

        trebleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {

                viewModel.setTrebleSound(p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })

        mediaPlayer.setOnCompletionListener {
            playButton.setText("Play")
        }

        surroundSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleSurroundSound(isChecked)
        }

        // initialize voice commands
        initVoiceCommands()
    }

    private fun initVoiceCommands(){
       // val voiceInteraction = VoiceInteractionService()

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}

            override fun onResults(results: Bundle?) {
                val command = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                handleVoiceCommand(command)
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // Start listening
        speechRecognizer.startListening(intent)

    }

    private fun handleVoiceCommand(command: String?) {
        when (command?.toLowerCase(Locale.ROOT)) {
            "enhance audio effect" -> viewModel.setBassBoost(100)
            "reduce audio effect" -> viewModel.setBassBoost(10)
            "reduce treble" -> viewModel.setTrebleSound(0)
            "activate surround sound" -> viewModel.toggleSurroundSound(true)
            "deactivate surround sound" -> viewModel.toggleSurroundSound(true)
            // Add more commands here
            else -> Toast.makeText(this, "Command not recognized", Toast.LENGTH_SHORT).show()
            // mediaPlayer.audioSessionId
        }
    }

    private fun applyDSPSettings(audioSettings: AudioSettings){
        bassBoost = BassBoost(0,mediaPlayer.audioSessionId).apply {
          //  setStrength(audioSettings.bassBoost?.toShort() ?:0)
            setStrength((audioSettings.bassBoost * 10).toShort())
        }
        bassBoost!!.enabled = true

        bassSeekBar.progress = audioSettings.bassBoost
        trebleSeekBar.progress = audioSettings.trebleBoost
       // mediaPlayer.start()
       // Toast.makeText(this, "${audioSettings.surroundSuound}", Toast.LENGTH_SHORT).show()

        // Initialize Equalizer if not already done (for Treble)
        if (equalizer == null) {
            equalizer = Equalizer(0, mediaPlayer.audioSessionId).apply {
                enabled = true
            }
        }
        // Apply Treble Boost (assuming last band is for treble)
        val numberOfBands = equalizer!!.numberOfBands
        val trebleBand = numberOfBands - 1 // Typically, the last band represents the highest frequencies
        val trebleLevel = (audioSettings.trebleBoost * 100).toShort() // Adjust according to your preference
        equalizer?.setBandLevel(trebleBand.toShort(), trebleLevel)

        // Initialize Virtualizer if not already done (for Surround Sound)
        if (virtualizer == null) {
            virtualizer = Virtualizer(0, mediaPlayer.audioSessionId).apply {
                enabled = true
            }
        }
        // Apply Surround Sound settings
        virtualizer?.setStrength(if (audioSettings.surroundSuound) 1000.toShort() else 0.toShort())


    }

    private fun checkPermission() {
       /* if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.MODIFY_AUDIO_SETTINGS), 1)
        }*/
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 2)
        } else {
            initVoiceCommands()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 2 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initVoiceCommands()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        // Release MediaPlayer and BassBoost when done
        mediaPlayer.release()
        bassBoost?.release()
        equalizer?.release()
        virtualizer?.release()
    }

}