package aakash.com.voiceandmapassignment

import aakash.com.voiceandmapassignment.mapView.MapsActivity
import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.jakewharton.rxbinding3.view.touches
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Build
import android.media.AudioManager
import android.app.NotificationManager
import com.jakewharton.rxbinding3.view.clicks


class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent
    private lateinit var shrinkAnimation: Animation
    private lateinit var expandAnimation: Animation
    private lateinit var notificationManager: NotificationManager
    private val mCompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()
        setupSpeechRecognizer()
        setupAnimation()
        setupVoiceButtonEvent()
        setupClick()
    }

    private fun setupClick() {
        mCompositeDisposable.add(btnNavigate.clicks().subscribe(
            {
                startActivity(Intent(this@MainActivity, MapsActivity::class.java))
            }, {
                it.printStackTrace()
            }
        ))
    }

    private fun setupAnimation() {
        shrinkAnimation = AnimationUtils.loadAnimation(
            this,
            R.anim.shrink_fade_in_animation
        )
        shrinkAnimation.fillAfter = true
        expandAnimation = AnimationUtils.loadAnimation(
            this,
            R.anim.expand_fade_out_animation
        )
        expandAnimation.fillAfter = true
    }

    override fun onDestroy() {
        super.onDestroy()
        mCompositeDisposable.dispose()
        speechRecognizer.destroy()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 100
                )
            }
        }
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {

            val intent = Intent(
                android.provider.Settings
                    .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
            )

            startActivity(intent)
        }
    }

    private var isStarted: Boolean = false

    private fun setupVoiceButtonEvent() {
        mCompositeDisposable.add(ivVoiceInput.touches().subscribe(
            {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (!isStarted) {
                            isStarted = true
                            ivVoiceInput.startAnimation(shrinkAnimation)
                            speechRecognizer.startListening(speechRecognizerIntent)
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isStarted) {
                            isStarted = false
                            ivVoiceInput.startAnimation(expandAnimation)
                            speechRecognizer.stopListening()
                        }
                    }
                }

            }, {
                it.printStackTrace()
            }
        ))
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault()
        )
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {}

            override fun onRmsChanged(p0: Float) {}

            override fun onBufferReceived(p0: ByteArray?) {}

            override fun onPartialResults(bundle: Bundle?) {}

            override fun onEvent(p0: Int, bundle: Bundle?) {}

            override fun onBeginningOfSpeech() {}

            override fun onEndOfSpeech() {}

            override fun onError(p0: Int) {}

            override fun onResults(bundle: Bundle?) {
                bundle?.let {
                    val matches = bundle
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                    if (matches != null) {
                        tvOutputText.text = matches[0]
                        when (matches[0].toLowerCase()) {
                            "close app" -> {
                                mCompositeDisposable.dispose()
                                System.exit(0)
                            }
                            "silent device" -> {
                                val mode = this@MainActivity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                                } else {
                                    mode.ringerMode = AudioManager.RINGER_MODE_SILENT
                                }

                            }
                            "enable sound" -> {
                                val mode = this@MainActivity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                mode.ringerMode = AudioManager.RINGER_MODE_NORMAL
                            }
                            "navigate to map" -> {
                                startActivity(Intent(this@MainActivity, MapsActivity::class.java))
                            }
                        }
                    }
                }
            }
        })
    }
}
