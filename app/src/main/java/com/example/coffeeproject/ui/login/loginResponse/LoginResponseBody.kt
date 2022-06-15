package com.example.coffeeproject.ui.login.loginResponse

data class LoginResponseBody(
    val message: String,
    val sessionData: SessionData,
    val success: Boolean
)