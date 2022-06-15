package com.example.coffeeproject.ui.outletactivity.activityselection

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.coffeeproject.R
import com.example.coffeeproject.databinding.FragmentSelectActivityBinding
import com.example.coffeeproject.model.User.Companion.instance
import com.example.coffeeproject.model.User.Companion.user
import com.example.coffeeproject.ui.outletactivity.OutletUpdateViewModel
import com.example.coffeeproject.ui.outletactivity.singleoutletdetails.OutletResult


class ActivitySelectionFragment() : Fragment() {

    var sharedPreferences: SharedPreferences? = null

    var sweetAlertDialog: SweetAlertDialog? = null
    lateinit var binding: FragmentSelectActivityBinding

    var outLetViewModel = OutletUpdateViewModel()
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSelectActivityBinding.inflate(
                layoutInflater, container, false
        )
        return binding!!.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {

        }
        super.onViewCreated(view, savedInstanceState)

        initialize()
    }

    private fun initialize() {
        user = instance
        if (user!!.userId == null) {
            user!!.setValuesFromSharedPreference(
                    requireActivity().getSharedPreferences(
                            "user",
                            Context.MODE_PRIVATE
                    )
            )
        }

        outLetViewModel = ViewModelProvider(requireActivity()).get(OutletUpdateViewModel::class.java)
        val observer = Observer<OutletResult> {
            binding.outletName.text = it.name
            binding.outletId.text = "Outlet Id: "+ it.id
            binding.outletAddress.text = it.area_name + ", " + it.zone_name
        }

        outLetViewModel.getOutlet().observe(requireActivity(),observer)

        binding.outletUpdateBtn.setOnClickListener{
            findNavController().navigate(R.id.action_activitySelectionFragment_to_outletRegistrationFragment)
        }

        binding.sampleDroppingBtn.setOnClickListener {
           findNavController().navigate(R.id.action_activitySelectionFragment_to_sampleDroppingFragment)
            //Toast.makeText(requireContext(), "Under Development!",Toast.LENGTH_SHORT).show()
        }

        binding.followUpBtn.setOnClickListener {
            findNavController().navigate(R.id.action_activitySelectionFragment_to_followUpListFragment)
            //Toast.makeText(requireContext(), "Under Development!",Toast.LENGTH_SHORT).show()
        }
        binding.purchaseOrderBtn.setOnClickListener {
            findNavController().navigate(R.id.action_activitySelectionFragment_to_purchaseOrderFragment)
            //Toast.makeText(requireContext(), "Under Development!",Toast.LENGTH_SHORT).show()
        }

        binding.checkInBtn.setOnClickListener {
            findNavController().navigate(R.id.action_activitySelectionFragment_to_checkInFragment)
            //Toast.makeText(requireContext(), "Under Development!",Toast.LENGTH_SHORT).show()
        }
    }


}
