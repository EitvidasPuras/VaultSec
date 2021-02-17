package com.vaultsec.vaultsec.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.util.Log
import kotlin.properties.Delegates

var isNetworkConnected: Boolean by Delegates.observable(false) { _, _, _ -> }

class NetworkMonitor constructor(private val activity: Activity) {

    fun startNetworkCallback() {
        val cm: ConnectivityManager =
            activity.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager
        val builder: NetworkRequest.Builder = NetworkRequest.Builder()

        cm.registerNetworkCallback(
            builder.build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    isNetworkConnected = true
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    isNetworkConnected = false
                }
            }
        )
    }

    fun stopNetworkCallback() {
        val cm: ConnectivityManager =
            activity.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager
        cm.unregisterNetworkCallback(ConnectivityManager.NetworkCallback())
    }
}