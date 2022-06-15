package com.example.coffeeproject.ui.outletactivity.followup.followupresponse

data class GetSampleDropResponseBody(
    val message: String,
    val result: List<Result>,
    val success: Boolean
)