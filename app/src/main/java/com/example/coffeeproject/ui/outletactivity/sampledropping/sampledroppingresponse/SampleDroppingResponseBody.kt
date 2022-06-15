package com.example.coffeeproject.ui.outletactivity.sampledropping.sampledroppingresponse

data class SampleDroppingResponseBody(
    val message: String,
    val resultList: List<Result>,
    val success: Boolean
)