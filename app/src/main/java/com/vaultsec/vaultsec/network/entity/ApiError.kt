package com.vaultsec.vaultsec.network.entity

data class ApiError(
    val error: Map<String, List<String>>
)