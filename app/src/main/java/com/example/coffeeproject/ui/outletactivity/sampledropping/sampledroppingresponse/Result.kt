package com.example.coffeeproject.ui.outletactivity.sampledropping.sampledroppingresponse

data class Result(
    val id: String,
    val name: String,
    val product_name: String,
    val short_name: String,
    val weight_in_gm: String


) {
    override fun toString(): String {
        return "$product_name($weight_in_gm gm)"
    }
}