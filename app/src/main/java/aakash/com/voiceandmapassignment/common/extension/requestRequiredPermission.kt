package aakash.com.voiceandmapassignment.common.extension

import android.app.Activity
import androidx.core.app.ActivityCompat

fun Activity.requestRequiredPermission(permissions: Array<String>, requestCode: Int) {
    ActivityCompat.requestPermissions(this, permissions, requestCode)
}
