package com.vaultsec.vaultsec.network.entity

sealed class ApiResponse<T>(
    val data: T? = null,
    val type: ErrorTypes? = ErrorTypes.GENERAL,
    val message: String? = null,
) {
    class Success<T>(data: T? = null) : ApiResponse<T>(data)
    class Error<T>(type: ErrorTypes, message: String? = null, data: T? = null) :
        ApiResponse<T>(data, type, message)

    class Loading<T>(data: T? = null) : ApiResponse<T>(data)
}

enum class ErrorTypes {
    HTTP, SOCKET_TIMEOUT, SOCKET, CONNECTION, GENERAL
}