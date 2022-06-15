package com.example.coffeeproject.ui.outletactivity.checkin

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.coffeeproject.MainActivity
import com.example.coffeeproject.databinding.FragmentCheckInBinding
import com.example.coffeeproject.model.User
import com.example.coffeeproject.ui.outletactivity.OutletUpdateViewModel
import com.example.coffeeproject.ui.outletactivity.singleoutletdetails.OutletResult
import com.example.coffeeproject.utils.CustomUtility
import com.example.coffeeproject.utils.StaticTags
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class CheckInFragment() : Fragment() {

    var sharedPreferences: SharedPreferences? = null
    var outletId: String? = null
    var sweetAlertDialog: SweetAlertDialog? = null
    lateinit var binding: FragmentCheckInBinding

    var outLetViewModel = OutletUpdateViewModel()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCheckInBinding.inflate(
            layoutInflater, container, false
        )

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {

        }
        super.onViewCreated(view, savedInstanceState)

        initialize()
    }

    private fun initialize() {
        User.user = User.instance
        if (User.user!!.userId == null) {
            User.user!!.setValuesFromSharedPreference(
                requireActivity().getSharedPreferences(
                    "user",
                    Context.MODE_PRIVATE
                )
            )
        }

        outLetViewModel = ViewModelProvider(requireActivity()).get(OutletUpdateViewModel::class.java)
        val observer = Observer<OutletResult> {
            outletId = it.id
            binding.outletName.text = it.name
            binding.outletId.text = "Outlet Id: "+ it.id
            binding.outletAddress.text = it.area_name + ", " + it.zone_name
        }

        outLetViewModel.getOutlet().observe(requireActivity(),observer)

        binding.submitBtn.setOnClickListener{
            if(CustomUtility.haveNetworkConnection(requireContext()))
            {
                if(checkFields())
                {
                    upload()
                }
            }
            else
            {
                Toast.makeText(requireContext(),"No Internet!",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun upload() {
        sweetAlertDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        sweetAlertDialog!!.titleText = "Loading"
        sweetAlertDialog!!.show()
        sweetAlertDialog!!.setCancelable(false)
        val retrofit = Retrofit.Builder()
            .baseUrl(StaticTags.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(CheckInAPiInterface::class.java)

        val call = service.insertCheckIn(User.user!!.userId!!,outletId!!,
            MainActivity.presentLat!!,MainActivity.presentLon!!, MainActivity.presentAcc!!, binding.reason.text.toString())

        call.enqueue(object : Callback<CheckInResponseBody> {
            override fun onResponse(
                call: Call<CheckInResponseBody>?,
                response: retrofit2.Response<CheckInResponseBody>?
            ) {
                sweetAlertDialog!!.dismiss()
                Log.d("response", response?.body().toString())
                if (response != null) {
                    if (response.code() == 200) {
                        val checkInResponseBody = response.body()
                        if(checkInResponseBody.success)
                        {
                            CustomUtility.showSuccess(requireContext(),"Checked In Successfully","Success")
                            binding.reason.setText("")
                        }
                        else
                        {
                            CustomUtility.showError(requireContext(),checkInResponseBody.message,"Failed")
                        }
                    }
                }
            }

            override fun onFailure(call: Call<CheckInResponseBody>?, t: Throwable?) {
                sweetAlertDialog!!.dismiss()
                //Log.e("res", error.toString())
                CustomUtility.showError(
                    requireContext(),
                    "Network Error, try again!",
                    "Failed"
                )
            }
        })
    }

    private fun checkFields():Boolean{
        if(outletId == null)
        {
            requireActivity().finish()
            requireActivity().startActivity(requireActivity().intent)
        }
        if (binding.reason.text.toString() == "")
        {
            CustomUtility.showError(requireContext(),"Please write the reason", "Required!")
            return false
        }
        else if(MainActivity.presentAcc == null)
        {
            CustomUtility.showError(requireContext(),"Please wait for gps", "Required!")
            return false
        }
        return true
    }


}
