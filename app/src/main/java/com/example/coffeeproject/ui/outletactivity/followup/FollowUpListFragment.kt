package com.example.coffeeproject.ui.outletactivity.followup

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.*
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.coffeeproject.R

import com.example.coffeeproject.databinding.FragmentFollowUpListBinding
import com.example.coffeeproject.model.User
import com.example.coffeeproject.model.User.Companion.instance
import com.example.coffeeproject.model.User.Companion.user
import com.example.coffeeproject.ui.outletactivity.OutletUpdateViewModel
import com.example.coffeeproject.ui.outletactivity.followup.followupdata.FollowUpListItem
import com.example.coffeeproject.ui.outletactivity.followup.followupdata.FollowupData
import com.example.coffeeproject.ui.outletactivity.followup.followupdata.FollowupDataItem
import com.example.coffeeproject.ui.outletactivity.followup.followupresponse.GetSampleDropResponseBody
import com.example.coffeeproject.ui.outletactivity.followup.followupresponse.InsertFollowupResponseBody
import com.example.coffeeproject.ui.outletactivity.followup.followupresponse.Result
import com.example.coffeeproject.ui.outletactivity.singleoutletdetails.OutletResult
import com.example.coffeeproject.utils.CustomUtility
import com.example.coffeeproject.utils.StaticTags
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class FollowUpListFragment() : Fragment() {

    var sharedPreferences: SharedPreferences? = null

    lateinit var sweetAlertDialog: SweetAlertDialog
    lateinit var binding: FragmentFollowUpListBinding
    var recyclerView: RecyclerView? = null
    lateinit var mAdapter: FollowUpListDataAdapter
    private val dataList: java.util.ArrayList<FollowUpListItem> = java.util.ArrayList<FollowUpListItem>()
    var outLetViewModel = OutletUpdateViewModel()
    var outletId: String? = null
    var followupIdMark = mutableMapOf<String, String?>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFollowUpListBinding.inflate(
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
            outletId = it.id
            binding.outletName.text = it.name
            binding.outletId.text = "Outlet Id: "+ it.id
            binding.outletAddress.text = it.area_name + ", " + it.zone_name
            getFollowUpList()
        }

        outLetViewModel.getOutlet().observe(requireActivity(),observer)

        dataList.clear()
        recyclerView = binding.followUpListRecycler
        mAdapter = FollowUpListDataAdapter(dataList, requireContext())
        recyclerView!!.setItemViewCacheSize(10)
        recyclerView!!.isDrawingCacheEnabled = true
        recyclerView!!.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        recyclerView!!.setHasFixedSize(true)
        val mLayoutManager: RecyclerView.LayoutManager =
            GridLayoutManager(requireActivity().applicationContext, 1)
        recyclerView!!.layoutManager = mLayoutManager
        recyclerView!!.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        recyclerView!!.adapter = mAdapter



    }



    private fun getFollowUpList()
    {
        dataList.clear()
        if (CustomUtility.haveNetworkConnection(requireContext())) {
            sweetAlertDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
            sweetAlertDialog.titleText = "Loading"
            sweetAlertDialog.show()
            sweetAlertDialog.setCancelable(false)
            val retrofit = Retrofit.Builder()
                .baseUrl(StaticTags.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(FollowupApiInterface::class.java)
            val call = service.getSampleDropping(User.user!!.userId!!,outletId!!,"No","")

            call.enqueue(object : Callback<GetSampleDropResponseBody> {
                override fun onResponse(
                    call: Call<GetSampleDropResponseBody>?,
                    response: retrofit2.Response<GetSampleDropResponseBody>?
                ) {
                    sweetAlertDialog.dismiss()
                    Log.d("response", response?.body().toString())
                    if (response != null) {
                        if (response.code() == 200) {
                            val getSampleDropResponseBody = response.body()!!
                            if (getSampleDropResponseBody.success) {
                                for(item in getSampleDropResponseBody.result)
                                {
                                    if(dataList.size>0)
                                    {
                                        if(dataList[dataList.size-1].dropId == item.drop_id)
                                        {
                                            dataList[dataList.size-1].total++
                                        }
                                        else
                                        {
                                            dataList.add(FollowUpListItem(item.drop_id,item.create_time, item.product_name+" "+item.weight_in_gm,
                                                1))
                                        }
                                    }
                                    else
                                    {
                                        dataList.add(FollowUpListItem(item.drop_id,item.create_time, item.product_name+" "+item.weight_in_gm,
                                        1))
                                    }

                                }
                                mAdapter.notifyDataSetChanged()
                            }
                            else {
                                val s = SweetAlertDialog(requireContext(),SweetAlertDialog.ERROR_TYPE)
                                s.titleText = "Failed"
                                s.contentText = getSampleDropResponseBody.message
                                s.setConfirmButton("Ok", SweetAlertDialog.OnSweetClickListener {
                                    s.dismissWithAnimation()
                                })
                                s.setCancelable(false)
                                s.show()
                                Log.d("null","session expired")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<GetSampleDropResponseBody>?, t: Throwable?) {
                    sweetAlertDialog?.dismiss()
                    //Log.e("res", error.toString())
                    CustomUtility.showError(
                        requireContext(),
                        "Network Error, try again!",
                        "Failed"
                    )
                }
            })
        }
        else {
            CustomUtility.showError(
                requireContext(),
                "Please Check your internet connection",
                "Network Warning !!!"
            )
        }
    }



    inner class FollowUpListDataAdapter(dataList: java.util.ArrayList<FollowUpListItem>, context: Context) :
        RecyclerView.Adapter<FollowUpListDataAdapter.MyViewHolder>() {
        var mc = context

        private val dataList: java.util.ArrayList<FollowUpListItem> = dataList
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.follow_up_list_fragment_row_layout, parent, false)
            return MyViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val data = dataList[position]

            holder.date.text = "Date: " + data.date.slice(0..9)
            holder.droppedItems.text = "Items Dropped: " + data.total.toString()
            holder.details.text = data.name

            holder.rowLayout.setOnClickListener{
                dropId = data.dropId
                findNavController().navigate(R.id.action_followUpListFragment_to_followUp)
            }
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        inner class MyViewHolder(convertView: View) : RecyclerView.ViewHolder(convertView) {

            var date: TextView = convertView.findViewById(R.id.drop_date)
            var details: TextView = convertView.findViewById(R.id.details)
            var droppedItems: TextView = convertView.findViewById(R.id.dropped_items)
            var rowLayout: ConstraintLayout = convertView.findViewById(R.id.row_layout)
        }

    }

    companion object {
        var dropId = ""
    }
}
