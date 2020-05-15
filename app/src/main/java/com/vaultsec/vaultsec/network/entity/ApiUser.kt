package com.vaultsec.vaultsec.network.entity

data class ApiUser(
    val first_name: String,
    val last_name: String,
    val email: String,
    val password: String,
    val password_confirmation: String
)