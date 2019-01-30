package aakash.com.voiceandmapassignment

import android.Manifest
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

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent
    private lateinit var shrinkAnimation : Animation
    private lateinit var expandAnimation: Animation
    private val mCompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()
        setupSpeechRecognizer()
        setupAnimation()
        setupVoiceButtonEvent()
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
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            }
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

                    if (matches != null)
                        tvOutputText.text = matches[0]
                }
            }
        })
    }
}
