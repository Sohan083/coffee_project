package com.example.coffeeproject.ui.outletregistration

import com.example.coffeeproject.ui.outletregistration.responses.areaapiresponse.AreaResponseBody
import com.example.coffeeproject.ui.outletregistration.responses.initiallistapiresponse.InitialListResponseBody
import com.example.coffeeproject.ui.outletregistration.responses.zoneapiresponsebody.ZoneResponseBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OutletApiInterface {
    @FormUrlEncoded
    @POST("zone/get_zone.php")
    fun getZoneList(
            @Field("UserId") TokenId: String
    ): retrofit2.Call<ZoneResponseBody>

    @FormUrlEncoded
    @POST("area/get_area.php")
    fun getAreaList(
            @Field("ZoneId") ZoneId: String,
            @Field("UserId") UserId: String
    ): retrofit2.Call<AreaResponseBody>

    @FormUrlEncoded
    @POST("initial_lists/get_lists.php")
    fun getInitialList(
        @Field("UserId") TokenId: String
    ): retrofit2.Call<InitialListResponseBody>
}