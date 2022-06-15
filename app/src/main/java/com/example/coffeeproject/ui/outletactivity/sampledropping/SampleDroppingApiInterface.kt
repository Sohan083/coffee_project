package com.example.coffeeproject.ui.outletactivity.sampledropping

import com.example.coffeeproject.ui.outletactivity.sampledropping.sampledroppingresponse.SampleDroppingResponseBody
import com.example.coffeeproject.ui.outletactivity.singleoutletdetails.SingleOutletDetailsResponseBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SampleDroppingApiInterface {

    @FormUrlEncoded
    @POST("product_sku/get_product_sku.php")
    fun getProductSku(
        @Field("UserId") UserId: String
    ): retrofit2.Call<SampleDroppingResponseBody>

    @FormUrlEncoded
    @POST("outlet/get_outlet.php")
    fun getOutletDetails(
        @Field("UserId") UserId: String,
        @Field("OutletId") OutletId: String
    ): retrofit2.Call<SingleOutletDetailsResponseBody>
}