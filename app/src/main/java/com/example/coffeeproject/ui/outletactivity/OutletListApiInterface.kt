package com.example.coffeeproject.ui.outletactivity

import com.example.coffeeproject.ui.outletactivity.outletlistresponse.OutletListResponseBody
import com.example.coffeeproject.ui.outletactivity.singleoutletdetails.SingleOutletDetailsResponseBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OutletListApiInterface {

    @FormUrlEncoded
    @POST("outlet/get_outlet.php")
    fun getOutletList(
        @Field("UserId") UserId: String
    ): retrofit2.Call<OutletListResponseBody>

    @FormUrlEncoded
    @POST("outlet/get_outlet.php")
    fun getOutletDetails(
        @Field("UserId") UserId: String,
        @Field("OutletId") OutletId: String
    ): retrofit2.Call<SingleOutletDetailsResponseBody>
}