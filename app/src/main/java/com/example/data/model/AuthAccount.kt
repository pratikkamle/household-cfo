package com.example.data.model

data class AuthAccount(
    val uid: String,
    val email: String,
    val displayName: String,
    val isGoogle: Boolean = false
)
