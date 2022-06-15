package com.example.coffeeproject.ui.outletactivity.purchaseorder

import com.example.coffeeproject.ui.outletactivity.purchaseorder.purchaseorderresponse.OutlettSkuResponseBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface PurchaseOrderApiInterface {
    @FormUrlEncoded
    @POST("outlet_sku/get_outlet_sku.php")
    fun getOutletSku(
        @Field("UserId") UserId: String,
        @Field("OutletId") OutletId: String
    ): retrofit2.Call<OutlettSkuResponseBody>

}