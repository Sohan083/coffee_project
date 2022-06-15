package com.example.coffeeproject.ui.outletactivity.followup

import com.example.coffeeproject.ui.outletactivity.followup.followupresponse.GetSampleDropResponseBody
import com.example.coffeeproject.ui.outletactivity.followup.followupresponse.InsertFollowupResponseBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface FollowupApiInterface {
    @FormUrlEncoded
    @POST("sample_dropping/get_sample_dropping.php")
    fun getSampleDropping(
        @Field("UserId") UserId: String,
        @Field("OutletId") OutletId: String,
        @Field("HasCompletedFollowup") HasCompletedFollowup: String,
        @Field("DropId") DropId: String
    ): retrofit2.Call<GetSampleDropResponseBody>

    @FormUrlEncoded
    @POST("outlet_followup/insert_outlet_followup.php")
    fun uploadOutletFollowup(
        @Field("UserId") UserId: String,
        @Field("OutletId") OutletId: String,
        @Field("FollowupData") FollowupData: String,
        @Field("DropId") DropId: String
    ): retrofit2.Call<InsertFollowupResponseBody>
}