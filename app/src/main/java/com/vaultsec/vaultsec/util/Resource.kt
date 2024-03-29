package com.vaultsec.vaultsec.util

sealed class Resource<T>(
    val data: T? = null,
    val type: ErrorTypes? = ErrorTypes.GENERAL,
    val message: String? = null,
) {
    class Success<T>(data: T? = null) : Resource<T>(data)
    class Loading<T>(data: T? = null) : Resource<T>(data)
    class Error<T>(type: ErrorTypes? = null, message: String? = null, data: T? = null) :
        Resource<T>(data, type, message)

    class Empty<T> : Resource<T>()
}

enum class ErrorTypes {
    HTTP, SOCKET_TIMEOUT, SOCKET, CONNECTION, GENERAL
}
