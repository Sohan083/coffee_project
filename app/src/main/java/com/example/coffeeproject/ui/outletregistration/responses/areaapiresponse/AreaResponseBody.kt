package com.example.coffeeproject.ui.outletregistration.responses.areaapiresponse

data class AreaResponseBody(
    val message: String,
    val resultList: List<Result>,
    val success: Boolean
)