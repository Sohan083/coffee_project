package com.example.coffeeproject.ui.outletactivity.purchaseorder

import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.coffeeproject.R
import com.example.coffeeproject.databinding.FragmentPurchaseOrderBinding
import com.example.coffeeproject.model.User
import com.example.coffeeproject.model.User.Companion.instance
import com.example.coffeeproject.model.User.Companion.user
import com.example.coffeeproject.ui.outletactivity.OutletUpdateViewModel
import com.example.coffeeproject.utils.CustomUtility
import com.example.coffeeproject.utils.StaticTags
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.coffeeproject.ui.outletactivity.purchaseorder.purchaseorderresponse.OutlettSkuResponseBody
import com.example.coffeeproject.ui.outletactivity.purchaseorder.purchaseorderresponse.Result
import com.example.coffeeproject.ui.outletactivity.singleoutletdetails.OutletResult
import com.example.coffeeproject.utils.StaticTags.Companion.dateFormat
import com.example.coffeeproject.utils.StaticTags.Companion.dateFormatShow
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class PurchaseOrderFragment() : Fragment() {

    var sharedPreferences: SharedPreferences? = null

    lateinit var sweetAlertDialog: SweetAlertDialog
    var fullList: ArrayList<Result> = ArrayList()
    var recyclerView: RecyclerView? = null
    lateinit var mAdapter: BrandListDataAdapter
    var brandList = ArrayList<Result>()
    var outLetViewModel = OutletUpdateViewModel()
    private val dataList: ArrayList<PoRecyclerItem> = ArrayList<PoRecyclerItem>()
    var currentBrand: Result? = null
    var outletId: String? = null
    lateinit var binding: FragmentPurchaseOrderBinding
    val modeOfDeliveryList = arrayListOf("Cash","Cheque","Bank Transfer") // id 1 to 3
    val creditTermsList = arrayListOf("Bill to Bill", "15 Day's Payment Window","Cash on Delivery")
    var modeOfDeliveryId = ""
    var creditTermsId = ""
    var deliveryTimeCalendar: Calendar = Calendar.getInstance()
    var deliveryTime: String? = null
    val myFormat1 = "dd-MM-yyyy"
    var tempPositin: Int = -1
    var autoAdapter: AutoCompleteAdapter?=null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPurchaseOrderBinding.inflate(
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
            getBrandList()
        }

        outLetViewModel.getOutlet().observe(requireActivity(),observer)

        recyclerView = binding.brandListRecycler
        mAdapter = BrandListDataAdapter(dataList, requireContext())
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


        binding.autoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            tempPositin = position
            val temp = binding.autoCompleteTextView.adapter.getItem(position) as Result?
            if (temp != null) {
                if(temp.selected){
                    Toast.makeText(requireContext(),"Already selected",Toast.LENGTH_SHORT).show()
                }
                else {
                    currentBrand = binding.autoCompleteTextView.adapter.getItem(position) as Result?
                }

            }
        }

        binding.addBtn.setOnClickListener {
            if(currentBrand != null)
            {
                fullList[tempPositin].selected = true
                autoAdapter!!.notifyDataSetChanged()
                dataList.add(
                    PoRecyclerItem(currentBrand!!.outlet_sku_id,currentBrand!!.product_sku_name,currentBrand!!.product_name,
                currentBrand!!.weight_in_gm,currentBrand!!.price_bdt_new,"0","0"))
                mAdapter.notifyDataSetChanged()
                binding.autoCompleteTextView.setText("")
                currentBrand = null
            }
            /*else{
                Toast.makeText(requireContext(),"Please select one product first",Toast.LENGTH_SHORT).show()
            }*/
        }


        binding.spinnerModeOfPayment.adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, modeOfDeliveryList)
        binding.spinnerModeOfPayment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                modeOfDeliveryId = (position + 1).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }
        binding.spinnerCreditTerms.adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, creditTermsList)
        binding.spinnerCreditTerms.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                creditTermsId = (position + 1).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        binding.deliveryDateBtn.text = CustomUtility.getDeviceDate()
        deliveryTime = CustomUtility.getDeviceDate()
        val deliveryDateCalendarListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth -> // TODO Auto-generated method stub
                deliveryTimeCalendar.set(Calendar.YEAR, year)
                deliveryTimeCalendar.set(Calendar.MONTH, monthOfYear)
                deliveryTimeCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDeliveryTime()
            }

        binding.deliveryDateBtn.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                DatePickerDialog.THEME_HOLO_DARK,deliveryDateCalendarListener, deliveryTimeCalendar
                    .get(Calendar.YEAR), deliveryTimeCalendar.get(Calendar.MONTH),
                deliveryTimeCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

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
                Toast.makeText(requireContext(),"No Internet",Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun updateDeliveryTime() {
        val showFormat = SimpleDateFormat(dateFormatShow, Locale.US)
        val sdf = SimpleDateFormat(dateFormat, Locale.US)
        deliveryTime = sdf.format(deliveryTimeCalendar.time)
        binding.deliveryDateBtn.error = null
        binding.deliveryDateBtn.text = showFormat.format(deliveryTimeCalendar.time)
    }
    private fun getBrandList()
    {
        brandList.clear()
        if (CustomUtility.haveNetworkConnection(requireActivity())) {
            sweetAlertDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
            sweetAlertDialog.titleText = "Loading"
            sweetAlertDialog.show()
            sweetAlertDialog.setCancelable(false)
            val retrofit = Retrofit.Builder()
                .baseUrl(StaticTags.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(PurchaseOrderApiInterface::class.java)
            val call = service.getOutletSku(User.user!!.userId!!,outletId!!)

            call.enqueue(object : Callback<OutlettSkuResponseBody> {
                override fun onResponse(
                    call: Call<OutlettSkuResponseBody>?,
                    response: retrofit2.Response<OutlettSkuResponseBody>?
                ) {
                    sweetAlertDialog.dismiss()
                    Log.d("response", response?.body().toString())
                    if (response != null) {
                        if (response.code() == 200) {
                            val outlettSkuResponseBody = response.body()!!
                            if (outlettSkuResponseBody.success) {
                                for (i in outlettSkuResponseBody.result){
                                    if(i.outlet_sku_id != null && i.price_bdt_new != null && i.price_bdt_new.toFloat()>0.00){
                                        brandList.add(i)
                                    }
                                }
                                fullList.addAll(brandList)
                                autoAdapter = AutoCompleteAdapter(
                                    requireContext(),
                                    R.layout.fragment_sample_dropping,
                                    R.id.autocomplete_item_place_label,
                                    fullList
                                )
                                binding.autoCompleteTextView.setAdapter(autoAdapter)

                            }
                            else {
                                val s = SweetAlertDialog(requireContext(),SweetAlertDialog.ERROR_TYPE)
                                s.titleText = "Failed"
                                s.contentText = outlettSkuResponseBody.message
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

                override fun onFailure(call: Call<OutlettSkuResponseBody>?, t: Throwable?) {
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



    private fun upload() {
        val sweetAlertDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        sweetAlertDialog.titleText = "Loading"
        sweetAlertDialog.setCancelable(false)
        sweetAlertDialog.show()
        val queue = Volley.newRequestQueue(requireContext())
        val url = StaticTags.BASE_URL + "po/insert_po.php"

        val sr: StringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener {
                sweetAlertDialog.dismiss()
                try {
                    Log.d("response:", it)
                    val jsonObject = JSONObject(it)
                    if (jsonObject.getBoolean("success")) {
                        CustomUtility.showSuccess(requireContext(),"","Success")
                        dataList.clear()
                        mAdapter.notifyDataSetChanged()
                        currentBrand = null
                        binding.inTotalPrice.text = "0"
                    } else {
                        CustomUtility.showError(
                            requireContext(),
                            "Failed!",
                            jsonObject.getString("message")
                        )
                    }
                } catch (e: JSONException) {
                    CustomUtility.showError(requireContext(), e.message, "Failed")
                }

            },
            Response.ErrorListener {
                sweetAlertDialog.dismiss()
                CustomUtility.showError(requireContext(), "Network problem, try again", "Failed")
            }) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["UserId"] = user!!.userId!!
                params["OutletId"] = outletId!!
                params["DeliveryDateEstimated"] = deliveryTime!!
                params["TermOfDelivery"] = "Free"
                params["ModeOfPaymentId"] = modeOfDeliveryId

                Log.d("CreditId: ", creditTermsId)
                params["CreditTermId"] = creditTermsId

                var f = PurchaseOrderList()
                for (i in dataList)
                {
                    f.add(PurchaseOrderListItem(i.quantity, i.id,i.totalPrice,i.price))
                }
                Log.d("polist",Gson().toJson(f))
                params["PoSkuDetail"] = Gson().toJson(f)


                return params
            }

            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }
        }
        queue.add(sr)
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
        for(i in dataList)
        {
            if(i.totalPrice == "0")
            {
                Toast.makeText(requireContext(),"Please add price and quantity correctly!", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }



    inner class AutoCompleteAdapter(
        context: Context?, resource: Int,
        textViewResourceId: Int, fullList: ArrayList<Result>
    ) : ArrayAdapter<Result>(context!!, resource, textViewResourceId, fullList),
        Filterable {
        private var fullList: ArrayList<Result>
        private var mOriginalValues: ArrayList<Result>?
        private var mFilter: ArrayFilter? = null
        override fun getCount(): Int {
            return fullList.size
        }

        override fun getItem(position: Int): Result {
            return fullList[position]
        }



        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            if (convertView == null) {
                val inflater =
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                view = inflater.inflate(
                    R.layout.autocomplete_row_layout,
                    parent,
                    false
                )
            }
            val placeLabel: TextView = view!!.findViewById(R.id.autocomplete_item_place_label)
            val rowLayout: LinearLayout = view!!.findViewById(R.id.row_layout)
            val brand: Result = getItem(position)
            placeLabel.text = brand.product_name + "(${brand.weight_in_gm}gm)"

            return view
        }

        override fun getFilter(): Filter {
            if (mFilter == null) {
                mFilter = ArrayFilter()
            }
            return mFilter as ArrayFilter
        }

        private open inner class ArrayFilter : Filter() {
            private val lock: Any? = null
            protected override fun performFiltering(prefix: CharSequence?): FilterResults {
                val results = FilterResults()
                if (mOriginalValues == null) {
                    synchronized(lock!!) { mOriginalValues = ArrayList<Result>(fullList) }
                }
                if (prefix == null || prefix.length == 0) {
                    synchronized(lock!!) {
                        val list: ArrayList<Result> = ArrayList<Result>(
                            mOriginalValues
                        )
                        results.values = list
                        results.count = list.size
                    }
                } else {
                    val prefixString = prefix.toString().toLowerCase()
                    val values: ArrayList<Result>? = mOriginalValues
                    val count: Int = values!!.size
                    val newValues: ArrayList<Result> = ArrayList<Result>(count)
                    for (i in 0 until count) {
                        val item: Result = values[i]
                        if (item.product_name.toLowerCase().contains(prefixString) ||
                            item.product_sku_name.toLowerCase().contains(prefixString)) {
                            newValues.add(item)
                        }
                    }
                    results.values = newValues
                    results.count = newValues.size
                }
                return results
            }

            protected override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults
            ) {
                if (results.values != null) {
                    fullList = results.values as ArrayList<Result>
                } else {
                    fullList = ArrayList<Result>()
                }
                /*if (results.count > 0) {
            notifyDataSetChanged();
        } else {
            notifyDataSetInvalidated();
        }*/notifyDataSetChanged()
                clear()
                var i = 0
                val l: Int = fullList.size
                while (i < l) {
                    add(fullList[i])
                    i++
                }
                notifyDataSetInvalidated()
            }
        }

        init {
            this.fullList = fullList
            mOriginalValues = ArrayList<Result>(fullList)
        }
    }

    inner class BrandListDataAdapter(dataList: ArrayList<PoRecyclerItem>, context: Context) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var mc = context

        private val dataList: ArrayList<PoRecyclerItem> = dataList

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val itemView: View = LayoutInflater.from(parent.context).inflate(R.layout.purchase_order_row_layout, parent, false)
            return MyViewHolder(itemView)
        }


        override fun getItemCount(): Int {
            return dataList.size
        }


        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val data = dataList[position]

            var holder: MyViewHolder = holder as MyViewHolder
            holder.brandText.text = dataList[position].product_name
            holder.skuText.text = dataList[position].weight_in_gm
            holder.price.text = dataList[position].price
            /*holder.price.addTextChangedListener(object : TextWatcher {
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
                    if(!s.isNullOrEmpty() && !holder.quantity.text.isNullOrEmpty())
                    {
                        dataList[holder.adapterPosition].price = s.toString()

                        val total = (s.toString().toInt() * holder.quantity.text.toString().toInt()).toString()
                        holder.total.text = total
                        dataList[holder.adapterPosition].totalPrice = total
                    }
                    else
                    {
                        holder.total.text = "0"
                        dataList[holder.adapterPosition].totalPrice = "0"
                    }
                }
            })*/
            holder.quantity.addTextChangedListener(object : TextWatcher {
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
                    if(!s.isNullOrEmpty())
                    {
                        dataList[holder.adapterPosition].quantity = s.toString()
                        val total = (s.toString().toInt() * dataList[holder.adapterPosition].price.toFloat()).toString()
                        holder.total.text = total
                        dataList[holder.adapterPosition].totalPrice = total
                    }
                    else
                    {
                        holder.total.text = "0"
                        dataList[holder.adapterPosition].totalPrice = "0"
                    }
                    updateTotalPrice()
                }
            })
        }


        inner class MyViewHolder(convertView: View) : RecyclerView.ViewHolder(convertView) {
            var brandText: TextView = convertView.findViewById(R.id.item_description)
            var skuText: TextView = convertView.findViewById(R.id.pack_size)
            var price: TextView = convertView.findViewById(R.id.price)
            var quantity: EditText = convertView.findViewById(R.id.quantity)
            var total: TextView = convertView.findViewById(R.id.total)
        }


    }

    private fun updateTotalPrice() {
        var total = 0.0
        for (i in dataList){
            total += i.totalPrice.toFloat()
        }
        binding.inTotalPrice.visibility = View.VISIBLE
        binding.inTotalPrice.text = "Total: "+total.toString()
    }

    override fun onDetach() {
        super.onDetach()
        //requireContext().stopService(intent);
    }




}
