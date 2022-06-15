package com.example.coffeeproject.ui.outletactivity.purchaseorder.purchaseorderresponse

data class Result(
    val outlet_id: String,
    val outlet_name: String,
    val outlet_sku_id: String,
    val price_bdt: Any,
    val price_bdt_new: String,
    val product_name: String,
    val product_sku_id: String,
    val product_sku_name: String,
    val weight_in_gm: String,
    var selected: Boolean = false,
){
    override fun toString(): String {
        return "$product_name($weight_in_gm gm)"
    }
}