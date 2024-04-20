package net.mjstudio.rnkakao.core.util

import android.util.Log
import net.mjstudio.rnkakao.core.BuildConfig

private fun debugE(tag: String, message: Any?) {
    if (BuildConfig.DEBUG) Log.e(tag, "⭐️" + message.toString())
}

internal fun debugE(vararg message: Any?) {
    var str = ""
    for (i in message) {
        str += i.toString() + ", "
    }
    debugE("RNCKakao", str)
}