package com.example.coffeeproject.ui.outletactivity.checkin

import com.example.coffeeproject.ui.outletactivity.followup.followupresponse.GetSampleDropResponseBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface CheckInAPiInterface {
    @FormUrlEncoded
    @POST("checkin/insert_checkin.php")
    fun insertCheckIn(
        @Field("UserId") UserId: String,
        @Field("OutletId") OutletId: String,
        @Field("LatValue") LatValue: String,
        @Field("LonValue") LonValue: String,
        @Field("Accuracy") Accuracy: String,
        @Field("Remark") Remark: String,
    ): retrofit2.Call<CheckInResponseBody>
}