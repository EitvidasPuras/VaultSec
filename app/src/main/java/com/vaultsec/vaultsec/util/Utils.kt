package com.vaultsec.vaultsec.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.vaultsec.vaultsec.R

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