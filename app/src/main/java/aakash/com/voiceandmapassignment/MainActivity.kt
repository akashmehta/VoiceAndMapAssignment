package aakash.com.voiceandmapassignment

import aakash.com.voiceandmapassignment.common.util.BaseActivity
import aakash.com.voiceandmapassignment.mapView.MapsActivity
import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.touches
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity() {

    private val mCompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()
        setupSpeechRecognizer {
            tvOutputText.text = it
            when (it.toLowerCase()) {
                "close app" -> {
                    compositeDisposable.dispose()
                    System.exit(0)
                }
                "silent device" -> {
                    val mode = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                    } else {
                        mode.ringerMode = AudioManager.RINGER_MODE_SILENT
                    }

                }
                "enable sound" -> {
                    val mode = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    mode.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
                "navigate to map" -> {
                    startActivity(Intent(this, MapsActivity::class.java))
                }
            }
        }
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

}
