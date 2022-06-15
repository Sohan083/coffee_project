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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.coffeeproject.R
import com.example.coffeeproject.databinding.FragmentFollowUpBinding
import com.example.coffeeproject.model.User
import com.example.coffeeproject.model.User.Companion.instance
import com.example.coffeeproject.model.User.Companion.user
import com.example.coffeeproject.ui.outletactivity.OutletUpdateViewModel
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


class FollowUpFragment() : Fragment() {

    var sharedPreferences: SharedPreferences? = null

    lateinit var sweetAlertDialog: SweetAlertDialog
    lateinit var binding: FragmentFollowUpBinding
    var recyclerView: RecyclerView? = null
    lateinit var mAdapter: FollowUpListDataAdapter
    private val dataList: java.util.ArrayList<Result> = java.util.ArrayList<Result>()
    var outLetViewModel = OutletUpdateViewModel()
    var outletId: String? = null
    var dropId = ""
    var followupIdMark = mutableMapOf<String, String?>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFollowUpBinding.inflate(
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
                Toast.makeText(requireContext(),"No internet!", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun upload() {
        sweetAlertDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        sweetAlertDialog.titleText = "Loading"
        sweetAlertDialog.show()
        sweetAlertDialog.setCancelable(false)
        val retrofit = Retrofit.Builder()
            .baseUrl(StaticTags.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(FollowupApiInterface::class.java)
        var f = FollowupData()
        for (i in dataList)
        {
            f.add(FollowupDataItem(followupIdMark[i.product_sku_id]!!,"No",i.product_sku_id))
        }
        val call = service.uploadOutletFollowup(User.user!!.userId!!,outletId!!, Gson().toJson(f),dropId)

        call.enqueue(object : Callback<InsertFollowupResponseBody> {
            override fun onResponse(
                call: Call<InsertFollowupResponseBody>?,
                response: retrofit2.Response<InsertFollowupResponseBody>?
            ) {
                sweetAlertDialog.dismiss()
                Log.d("response", response?.body().toString())
                if (response != null) {
                    if (response.code() == 200) {
                        val insertFollowupResponseBody = response.body()
                        if(insertFollowupResponseBody.success)
                        {
                            CustomUtility.showSuccess(requireContext(),"Followup Insert Successful","Success")
                            dataList.clear()
                            mAdapter.notifyDataSetChanged()
                        }
                        else
                        {
                            CustomUtility.showError(requireContext(),insertFollowupResponseBody.message,"Failed")
                        }
                    }
                }
            }

            override fun onFailure(call: Call<InsertFollowupResponseBody>?, t: Throwable?) {
                sweetAlertDialog.dismiss()
                //Log.e("res", error.toString())
                CustomUtility.showError(
                    requireContext(),
                    "Network Error, try again!",
                    "Failed"
                )
            }
        })
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
            dropId = FollowUpListFragment.dropId
            val call = service.getSampleDropping(User.user!!.userId!!,outletId!!,"No", dropId)

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
                                    dataList.add(item)
                                    followupIdMark[item.product_sku_id] = "5"
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
    private fun checkFields(): Boolean {
        if(outletId == null)
        {
            requireActivity().finish()
            requireActivity().startActivity(requireActivity().intent)
        }
        if(dataList.size == 0)
        {
            Toast.makeText(requireContext(),"No product added!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }


    inner class FollowUpListDataAdapter(dataList: java.util.ArrayList<Result>, context: Context) :
        RecyclerView.Adapter<FollowUpListDataAdapter.MyViewHolder>() {
        var mc = context

        private val dataList: java.util.ArrayList<Result> = dataList
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.follow_up_row_layout, parent, false)
            return MyViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val data = dataList[position]
            holder.plusBtn.setOnClickListener {
                if(holder.markText.text.toString() != "")
                {
                    var m = holder.markText.text.toString().toInt()
                    if(m<=0)
                    {
                        Toast.makeText(requireContext(),"Value can't be more than 0!",Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        m += 1
                        followupIdMark[data.product_sku_id] = m.toString()
                        notifyItemChanged(position)
                    }
                }
                else
                {
                    Toast.makeText(requireContext(),"Empty value", Toast.LENGTH_SHORT).show()
                    followupIdMark[data.drop_id] = "5"
                    holder.markText.setText(followupIdMark[data.product_sku_id])
                }

            }
            holder.minusBtn.setOnClickListener {
                if(holder.markText.text.toString() != "")
                {
                    var m = holder.markText.text.toString().toInt()
                    if(m<=0)
                    {
                        Toast.makeText(requireContext(),"Value can't be less than 0!",Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        m -= 1
                        followupIdMark[data.product_sku_id] = m.toString()
                        notifyItemChanged(position)
                    }
                }
                else
                {
                    Toast.makeText(requireContext(),"Empty value", Toast.LENGTH_SHORT).show()
                    followupIdMark[data.drop_id] = "5"
                    holder.markText.setText(followupIdMark[data.product_sku_id])
                }

            }

            holder.brandName.text = data.product_name
            holder.sku.text = data.weight_in_gm
            holder.quantity.text = "1"

            holder.markText.setText(followupIdMark[data.product_sku_id])
            holder.markText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
                ) {

                    if(s.toString() != "" && (s.toString().toInt() > 10 || s.toString().toInt() < 0))
                    {
                        Toast.makeText(requireContext(),"Value must be between 0 and 10", Toast.LENGTH_SHORT).show()
                        followupIdMark[data.drop_id] = "5"
                        holder.markText.setText(followupIdMark[data.product_sku_id])
                    }
                }
            })
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        inner class MyViewHolder(convertView: View) : RecyclerView.ViewHolder(convertView) {
            var plusBtn: CardView = convertView.findViewById(R.id.plusBtn)
            var minusBtn: CardView = convertView.findViewById(R.id.minusBtn)
            var markText: EditText = convertView.findViewById(R.id.mark)
            var brandName: TextView = convertView.findViewById(R.id.brandText)
            var sku: TextView = convertView.findViewById(R.id.skuText)
            var quantity: TextView = convertView.findViewById(R.id.quantity)
        }

    }


}
