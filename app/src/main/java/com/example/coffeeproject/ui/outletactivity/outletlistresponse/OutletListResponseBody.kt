package com.example.coffeeproject.ui.outletactivity.outletlistresponse

data class OutletListResponseBody(
    val message: String,
    val outletResult: List<OutletResult>,
    val success: Boolean
)