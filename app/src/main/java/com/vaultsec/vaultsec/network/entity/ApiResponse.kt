package com.vaultsec.vaultsec.network.entity

data class ApiResponse(
    val isError: Boolean,
    val type: ErrorTypes? = ErrorTypes.GENERAL,
    val message: String? = null
)

enum class ErrorTypes {
    HTTP_ERROR, SOCKET_TIMEOUT, GENERAL
}