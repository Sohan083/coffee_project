package com.example.coffeeproject.ui.outletregistration.responses.zoneapiresponsebody

data class ZoneResponseBody(
    val message: String,
    val resultList: List<Result>,
    val success: Boolean
)