package aakash.com.voiceandmapassignment.common.util

import aakash.com.voiceandmapassignment.R
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable
import java.util.*

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {
    val compositeDisposable = CompositeDisposable()
    lateinit var speechRecognizer: SpeechRecognizer
    lateinit var notificationManager : NotificationManager
    lateinit var speechRecognizerIntent: Intent
    lateinit var shrinkAnimation: Animation
    lateinit var expandAnimation: Animation

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun setupAnimation() {
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

    fun setupSpeechRecognizer(onSpeechRecognized : (speech: String) -> Unit) {
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
                        onSpeechRecognized(matches[0])
                    }
                }
            }
        })
    }

}