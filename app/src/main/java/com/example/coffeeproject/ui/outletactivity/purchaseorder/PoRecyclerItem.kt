package com.example.coffeeproject.ui.outletactivity.purchaseorder

data class PoRecyclerItem(
    val id: String,
    val name: String,
    val product_name: String,
    val weight_in_gm: String,
    var price: String,
    var quantity: String,
    var totalPrice: String,
)
