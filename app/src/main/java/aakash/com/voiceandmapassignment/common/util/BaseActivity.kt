package aakash.com.voiceandmapassignment.common.util

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {
    val compositeDisposable = CompositeDisposable()

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }
}