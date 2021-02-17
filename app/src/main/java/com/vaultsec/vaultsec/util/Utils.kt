package com.vaultsec.vaultsec.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.ui.StartActivity
import kotlinx.android.synthetic.main.activity_bottom_navigation.*
import kotlinx.android.synthetic.main.activity_start.*
import java.security.MessageDigest

//private val HEX_CHARS = "0123456789abcdef".toCharArray()

fun hideKeyboard(activity: Activity) {
    val inputManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    activity.currentFocus?.let {
        inputManager.hideSoftInputFromWindow(
            activity.currentFocus?.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }
}

fun playSlidingAnimation(reveal: Boolean, activity: Activity) {
    val navbar = activity.findViewById<BottomNavigationView>(R.id.bottom_nav_view)
    val shadow = activity.findViewById<View>(R.id.bottom_nav_shadow)
    if (reveal) {
        if (navbar.visibility == View.GONE) {

            navbar.translationX = -navbar.width.toFloat()
            shadow.translationX = -navbar.width.toFloat()
            navbar.visibility = View.VISIBLE
            shadow.visibility = View.VISIBLE
            navbar.animate().translationX(0f).setDuration(320).setListener(null)
            shadow.animate().translationX(0f).setDuration(320).setListener(null)
        }
    } else {
        shadow.animate().translationX(-shadow.width.toFloat()).setDuration(400)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(p0: Animator?) {
                    shadow.visibility = View.GONE
                }
            })
        navbar.animate().translationX(-navbar.width.toFloat()).setDuration(400)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(p0: Animator?) {
                    navbar.visibility = View.GONE
                }
            })
    }
}

fun hasInternetConnection(activity: Activity): Boolean {
    val connectivityManager =
        activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        else -> false
    }
}

fun hashString(input: String): String {
    val bytes = MessageDigest
        .getInstance("SHA-256")
        .digest(input.toByteArray())
    return bytes.toHex()
}

private fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}

/*
* This is needed, because when setting indeterminateDrawable in the XML the animation gets messed up
* */
fun setProgressBarDrawable(progressBar: ProgressBar) {
    val drawable = CircularProgressDrawable(progressBar.context).apply {
        strokeWidth = 16f
        centerRadius = 88f
        setColorSchemeColors(progressBar.context.getColor(R.color.color_accent))
    }
    progressBar.indeterminateDrawable = drawable
}
/*
* A different way to implement ByteArray to hex conversion
* */
//private fun printHexBinary(data: ByteArray): String {
//    val r = StringBuilder(data.size * 2)
//    data.forEach { b ->
//        val i = b.toInt()
//        r.append(HEX_CHARS[i shr 4 and 0xF])
//        r.append(HEX_CHARS[i and 0xF])
//    }
//    return r.toString()
//}
