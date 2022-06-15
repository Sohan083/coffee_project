package com.example.coffeeproject.ui.outletactivity

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.coffeeproject.ui.outletactivity.singleoutletdetails.OutletResult

class OutletUpdateViewModel: ViewModel() {

    private var selectedOutlet: MutableLiveData<OutletResult> = MutableLiveData()

    fun getOutlet(): LiveData<OutletResult> {
        return selectedOutlet
    }

    fun setOutlet(outlet: OutletResult) {
        Log.d("data",outlet.toString())
        selectedOutlet.value = outlet
    }
}