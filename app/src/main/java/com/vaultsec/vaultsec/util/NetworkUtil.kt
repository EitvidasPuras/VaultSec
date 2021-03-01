package com.vaultsec.vaultsec.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlin.properties.Delegates

var isNetworkAvailable: Boolean by Delegates.observable(false) { _, _, _ -> }

class NetworkUtil {
    fun checkNetworkInfo(
        context: Context,
        onConnectionStatusChange: OnConnectionStatusChange
    ) {
        val cm: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager
        val capabilities: NetworkCapabilities? = cm.getNetworkCapabilities(cm.activeNetwork)
        if (capabilities == null) {
            isNetworkAvailable = false
            onConnectionStatusChange.onChange(false)
        }
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                isNetworkAvailable = true
                onConnectionStatusChange.onChange(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                isNetworkAvailable = false
                onConnectionStatusChange.onChange(false)
            }
        })
    }
}

interface OnConnectionStatusChange {
    fun onChange(isAvailable: Boolean)
}