package com.example.coffeeproject.ui.outletactivity.sampledropping

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import cn.pedant.SweetAlert.SweetAlertDialog
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.coffeeproject.R
import com.example.coffeeproject.databinding.FragmentSampleDroppingBinding
import com.example.coffeeproject.model.User
import com.example.coffeeproject.model.User.Companion.instance
import com.example.coffeeproject.model.User.Companion.user
import com.example.coffeeproject.ui.outletactivity.OutletUpdateViewModel
import com.example.coffeeproject.ui.outletactivity.sampledropping.sampledroppingresponse.Result
import com.example.coffeeproject.ui.outletactivity.sampledropping.sampledroppingresponse.SampleDroppingResponseBody
import com.example.coffeeproject.ui.outletactivity.singleoutletdetails.OutletResult
import com.example.coffeeproject.utils.CustomUtility
import com.example.coffeeproject.utils.StaticTags
import org.json.JSONException
import org.json.JSONObject

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*


/**
 * This fragment for sample dropping activity
 * there is autocomplete text view and on custom adapter for that. FullList of Result is the main product list from api
 * dataList for showing the product in the recycler
 */


class SampleDroppingFragment() : Fragment() {


    lateinit var sweetAlertDialog: SweetAlertDialog
    lateinit var binding: FragmentSampleDroppingBinding
    var fullList: ArrayList<Result> = ArrayList()
    var recyclerView: RecyclerView? = null
    lateinit var mAdapter: BrandListDataAdapter
    var brandList = ArrayList<Result>()
    var outLetViewModel = OutletUpdateViewModel()
    private val dataList: ArrayList<Result> = ArrayList<Result>()
    var currentBrand: Result? = null
    var outletId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSampleDroppingBinding.inflate(
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
        }

        outLetViewModel.getOutlet().observe(requireActivity(),observer)

        dataList.clear()
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

        getBrandList()

        binding.addBtn.setOnClickListener {
            if(currentBrand != null)
            {
                dataList.add(currentBrand!!)
                mAdapter.notifyDataSetChanged()
                binding.autoCompleteTextView.setText("")
                binding.addBtn.visibility == View.GONE
                currentBrand = null
            }
            else{
                Toast.makeText(requireContext(),"Please select one product first",Toast.LENGTH_SHORT).show()
            }
        }
        binding.autoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            currentBrand = binding.autoCompleteTextView.adapter.getItem(position) as Result?
            binding.addBtn.visibility = View.VISIBLE
        }
        binding.submitBtn.setOnClickListener{
            if(CustomUtility.haveNetworkConnection(requireContext()))
            {
                if(checkFields())
                {
                    val s = SweetAlertDialog(requireContext(),SweetAlertDialog.WARNING_TYPE)
                    s.cancelText = "No"
                    s.titleText = "Are you sure"
                    s.setConfirmButton("Yes",SweetAlertDialog.OnSweetClickListener {
                        s.dismissWithAnimation()
                        upload()
                    })
                    s.show()
                }
            }
            else
            {
                Toast.makeText(requireContext(),"No internet!", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun upload() {
        val sweetAlertDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        sweetAlertDialog.titleText = "Loading"
        sweetAlertDialog.setCancelable(false)
        sweetAlertDialog.show()
        val queue = Volley.newRequestQueue(requireContext())
        val url = StaticTags.BASE_URL + "sample_dropping/insert_sample_dropping.php"

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

                var id = ""
                var pId = ""
                for (item in dataList) {
                    id += "${item.id},"
                }
                val l = id.length
                for (i in 0 until l) {
                    if (i != l - 1)
                        pId += id[i]
                }
                if(pId != "")
                    params["SampleSKUIdList"] = pId



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
        return true
    }


    /*inner class BrandListDataAdapter(dataList: ArrayList<Result>, context: Context) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var mc = context

        private val dataList: ArrayList<Result> = dataList

        override fun getItemViewType(position: Int): Int {
            // based on you list you will return the ViewType
            return if (dataList[position].isAdd) {
                1
            } else {
                2
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            if(viewType == 1)
            {
                val itemView: View = LayoutInflater.from(parent.context).inflate(R.layout.add_row_layout, parent, false)
                return MyViewHolderAdd(itemView)
            }
            else
            {
                val itemView: View = LayoutInflater.from(parent.context).inflate(R.layout.sample_dropping_row_layout, parent, false)
                return MyViewHolder(itemView)
            }
        }


        override fun getItemCount(): Int {
            return dataList.size
        }


        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val data = dataList[position]

            if(getItemViewType(position) == 1) {
                var holder: MyViewHolderAdd = holder as MyViewHolderAdd
                holder.addBtn.setOnClickListener {
                    dataList.add(BrandCustomClass(isAdd = false))
                    var l = ArrayList<BrandCustomClass> ()
                    for(i in dataList)
                    {
                        if(!i.isAdd)
                        {
                            l.add(i)
                        }
                    }
                    l.add(BrandCustomClass())
                    dataList.clear()
                    dataList.addAll(l)
                    notifyDataSetChanged()
                }
            }
            else
            {
                var holder: MyViewHolder = holder as MyViewHolder
               *//* holder.brandBtn.setOnClickListener{
                    //Creating the instance of PopupMenu
                    //Creating the instance of PopupMenu
                    val menu = PopupMenu(mc, it)
                    for((i,j) in brandList.withIndex())
                    {
                        menu.menu.add(Menu.NONE, i, i, j)
                    }

                    menu.show()

                    menu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                        holder.brandText.text = item.title
                        true
                    })

                }*//*
                holder.brandText.text = dataList[position].brandName
                holder.skuText.text = dataList[position].sku
                holder.quantity.text = dataList[position].quantity
            }

        }

        inner class MyViewHolder(convertView: View) : RecyclerView.ViewHolder(convertView) {
            var brandBtn: CardView = convertView.findViewById(R.id.selectBrandBtn)
            var skuBtn: CardView = convertView.findViewById(R.id.selectQuantityBtn)
            var quantity: TextView = convertView.findViewById(R.id.quantity)
            var brandText: TextView = convertView.findViewById(R.id.brandText)
            var skuText: TextView = convertView.findViewById(R.id.skuText)
        }

        inner class MyViewHolderAdd(convertView: View) : RecyclerView.ViewHolder(convertView) {
            var addBtn: CardView = convertView.findViewById(R.id.addBtn)
        }


    }
*/


/*    class CompareBrandCustomClass {
        companion object : Comparator<BrandCustomClass> {
            override fun compare(a: BrandCustomClass, b: BrandCustomClass): Int  {
               if(a.isAdd)
               {
                   return -1
               }
                else{
                    return 1
               }
            }
        }
    }*/

    inner class BrandListDataAdapter(dataList: ArrayList<Result>, context: Context) :
           RecyclerView.Adapter<RecyclerView.ViewHolder>() {
           var mc = context

           private val dataList: ArrayList<Result> = dataList

           override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
               val itemView: View = LayoutInflater.from(parent.context).inflate(R.layout.sample_dropping_row_layout, parent, false)
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
               //holder.quantity.text = "1"
        }

        inner class MyViewHolder(convertView: View) : RecyclerView.ViewHolder(convertView) {
            var brandText: TextView = convertView.findViewById(R.id.brandText)
            var skuText: TextView = convertView.findViewById(R.id.skuText)
        }


    }


    private fun getBrandList()
    {
        brandList.clear()
        if (CustomUtility.haveNetworkConnection(requireContext())) {
            sweetAlertDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
            sweetAlertDialog.titleText = "Loading"
            sweetAlertDialog.show()
            sweetAlertDialog.setCancelable(false)
            val retrofit = Retrofit.Builder()
                .baseUrl(StaticTags.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(SampleDroppingApiInterface::class.java)
            val call = service.getProductSku(User.user!!.userId!!)

            call.enqueue(object : Callback<SampleDroppingResponseBody> {
                override fun onResponse(
                    call: Call<SampleDroppingResponseBody>?,
                    response: retrofit2.Response<SampleDroppingResponseBody>?
                ) {
                    sweetAlertDialog.dismiss()
                    Log.d("response", response?.body().toString())
                    if (response != null) {
                        if (response.code() == 200) {
                            val sampleDroppingResponseBody = response.body()!!
                            if (sampleDroppingResponseBody.success) {
                                brandList.addAll(sampleDroppingResponseBody.resultList)
                                fullList.addAll(brandList)
                                val adapter = AutoCompleteAdapter(
                                    requireContext(),
                                    R.layout.fragment_sample_dropping,
                                    R.id.autocomplete_item_place_label,
                                    fullList
                                )
                                binding.autoCompleteTextView.setAdapter(adapter)

                            }
                            else {
                                val s = SweetAlertDialog(requireContext(),SweetAlertDialog.ERROR_TYPE)
                                s.titleText = "Failed"
                                s.contentText = sampleDroppingResponseBody.message
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

                override fun onFailure(call: Call<SampleDroppingResponseBody>?, t: Throwable?) {
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
                                item.name.toLowerCase().contains(prefixString)) {
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

    override fun onDetach() {
        super.onDetach()
        //requireContext().stopService(intent);
    }




}
