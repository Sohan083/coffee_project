package com.example.coffeeproject.ui.outletactivity.purchaseorder.purchaseorderresponse

data class OutlettSkuResponseBody(
    val message: String,
    val result: List<Result>,
    val success: Boolean
)