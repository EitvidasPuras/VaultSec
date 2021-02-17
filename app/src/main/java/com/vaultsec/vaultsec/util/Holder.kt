package com.vaultsec.vaultsec.util

sealed class Holder<T>(
    val data: T? = null,
    val error: Exception? = null
) {
    class Success<T>(data: T) : Holder<T>(data)
    class Loading<T>(data: T? = null) : Holder<T>(data)
    class Error<T>(e: Exception, data: T? = null) : Holder<T>(data, e)
}
