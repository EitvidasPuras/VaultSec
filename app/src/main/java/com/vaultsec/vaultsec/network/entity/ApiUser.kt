package com.vaultsec.vaultsec.network.entity

data class ApiUser(
    val first_name: String? = null,
    val last_name: String? = null,
    val email: String,
    val password: String,
    val password_confirmation: String? = null
)