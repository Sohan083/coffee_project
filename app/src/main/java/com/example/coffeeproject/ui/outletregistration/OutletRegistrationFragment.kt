package com.example.coffeeproject.ui.outletregistration

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.*
import cn.pedant.SweetAlert.SweetAlertDialog
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.coffeeproject.MainActivity.Companion.isInActivity
import com.example.coffeeproject.MainActivity.Companion.menuNav
import com.example.coffeeproject.MainActivity.Companion.presentAcc
import com.example.coffeeproject.MainActivity.Companion.presentLat
import com.example.coffeeproject.MainActivity.Companion.presentLon
import com.example.coffeeproject.R
import com.example.coffeeproject.databinding.FragmentOutletRegistrationBinding
import com.example.coffeeproject.model.User.Companion.user
import com.example.coffeeproject.ui.login.LoginActivity
import com.example.coffeeproject.ui.outletactivity.OutletUpdateViewModel
import com.example.coffeeproject.ui.outletactivity.singleoutletdetails.OutletResult
import com.example.coffeeproject.ui.outletregistration.responses.areaapiresponse.AreaResponseBody
import com.example.coffeeproject.ui.outletregistration.responses.initiallistapiresponse.InitialListResponseBody
import com.example.coffeeproject.ui.outletregistration.responses.initiallistapiresponse.Syrup
import com.example.coffeeproject.utils.CustomUtility
import com.example.coffeeproject.utils.StaticTags
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class OutletRegistrationFragment() : Fragment() {
    var sharedPreferences: SharedPreferences? = null
    lateinit var binding: FragmentOutletRegistrationBinding
    var area: String? = "";
    var zone: String? = ""

    var takeGps = false

    var operatorList = arrayOf("017", "013", "019", "014", "016", "018", "015")
    var isCorrectPrimaryNumber: Boolean = false
    var isCorrectOwnerNumber: Boolean = false
    var isCorrectEmail: Boolean = false
    var outletContactNumber: String? = ""
    var outletOwnerNumber: String? = ""
    var sweetAlertDialog: SweetAlertDialog? = null

    var outletType: String? = ""
    var outletTypeId: String? = ""

    lateinit var areaList: ArrayList<String>;
    var areaId: String? = null;
    val areaIdMap = mutableMapOf<Int, String?>()

    var cpDesignation = ""
    var cpDesignationList: ArrayList<String> = ArrayList();
    var cpDesignationId: String? = null;
    val cpDesignationIdMap = mutableMapOf<Int, String?>()
    var ownerDesignation = ""
    var ownerDesignationList: ArrayList<String> = ArrayList();
    var ownerDesignationId: String? = null;
    val ownerDesignationIdMap = mutableMapOf<Int, String?>()

    var currentBrand = ""
    var currentBrandList: ArrayList<String> = ArrayList();
    var currentBrandId: String? = null;
    val currentBrandIdMap = mutableMapOf<Int, String?>()

    var machineBrand = ""
    var machineBrandList: ArrayList<String> = ArrayList();
    var machineBrandId: String? = null;
    val machineBrandIdMap = mutableMapOf<Int, String?>()

    var statusList = arrayListOf<String>("Hooked", "Customer")
    var statusId: String? = null;
    val statusIdMap = mutableMapOf<Int, String?>()

    var syrupBrandIdMap = mutableMapOf<String, String?>()
    var syrupBrandList = ArrayList<Syrup>();

    lateinit var outlet: OutletResult

    var outletId = ""

    var outLetViewModel = OutletUpdateViewModel()

    private var recyclerView: RecyclerView? = null
    private var mAdapter: SyrupDataAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOutletRegistrationBinding.inflate(layoutInflater, container, false)
        if (arguments != null) {
            requireActivity().actionBar!!.title = "Outlet Update"
            var s = requireArguments().getString("outlet")
            outlet = Gson().fromJson(s, OutletResult::class.java)
            Log.d("name", outlet.name)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val sharedPreferences =
            requireActivity().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("formDone", false)) {
            requireActivity().supportFragmentManager.commit {
                setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                replace(R.id.nav_host_fragment, FragmentPicture())
                addToBackStack(null)
            }
        } else {

            initialize()
        }

    }


    private fun initialize() {

        checkForUpdate()

        ownerDesignationList.clear()
        cpDesignationList.clear()
        machineBrandList.clear()
        currentBrandList.clear()

        user!!.setValuesFromSharedPreference(
            requireActivity().getSharedPreferences(
                "user",
                AppCompatActivity.MODE_PRIVATE
            )
        )
        if (user!!.areaList != null && user!!.areaList != null) {
            val areaResponseBody =
                Gson().fromJson(user!!.areaList, AreaResponseBody::class.java)
            val itemList = areaResponseBody.resultList
            Log.d("ind", 143.toString())
            areaList = ArrayList()
            for ((ind, i) in itemList.withIndex()) {
                areaList.add(i.name)
                areaIdMap[ind] = i.id
                Log.d("ind", ind.toString())
            }
            binding.spinnerArea.adapter =
                ArrayAdapter(requireContext(), R.layout.spinner_item, areaList)
            getInitialList()
        } else {
            getAreaList()
        }



        recyclerView = binding.syrupsRecycler
        syrupBrandList.clear()
        mAdapter = SyrupDataAdapter(syrupBrandList, requireContext())
        recyclerView!!.setItemViewCacheSize(20)
        recyclerView!!.setDrawingCacheEnabled(true)
        recyclerView!!.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH)
        recyclerView!!.setHasFixedSize(true)
        val mLayoutManager: RecyclerView.LayoutManager =
            GridLayoutManager(requireContext(), 3)
        recyclerView!!.layoutManager = mLayoutManager
        recyclerView!!.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        recyclerView!!.adapter = mAdapter


        binding.typeRadioGroup.setOnCheckedChangeListener { radioGroup: RadioGroup, i: Int ->
            when (i) {
                binding!!.chain.id -> {
                    outletType = "Chain"
                    outletTypeId = "1"
                }
                binding!!.single.id -> {
                    outletType = "Single"
                    outletTypeId = "2"
                }
            }
        }

        binding.spinnerArea.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                areaId = areaIdMap[position]
                area = areaList[position]
                //getZoneList(cityId!!)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }



        binding.spinnerContactDesignation.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    cpDesignation = cpDesignationList[position]
                    cpDesignationId = cpDesignationIdMap[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }

            }


        binding.contactPersonMobileNumber.addTextChangedListener(object : TextWatcher {
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
                if (!isCorrectPhoneNumber(s.toString())) {
                    binding.contactPersonMobileNumber.error = "Number must be correct and unique"
                    isCorrectPrimaryNumber = false
                    //setCallGone()
                } else {
                    isCorrectPrimaryNumber = true
                    outletContactNumber = s.toString()
                    //checkDuplicate(outletContactNumber!!)

                }

            }
        })



        binding.sameAsChk.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                if (binding.outletContactPerson.text.toString().isNullOrEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Please insert contact person name!",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.sameAsChk.isChecked = false
                } else {
                    binding.outletDecisionMakerPerson.setText(binding.outletContactPerson.text.toString())
                }
                if (binding.contactPersonMobileNumber.text.toString().isNullOrEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Please insert contact person mobile number!",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.sameAsChk.isChecked = false
                } else {
                    binding.decisionMakerMobileNumber.setText(binding.contactPersonMobileNumber.text.toString())
                }
                if (cpDesignationId.isNullOrEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Please insert contact person designation!",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.sameAsChk.isChecked = false
                } else {
                    binding.spinnerDecisionDesignation.setSelection(
                        getKey(
                            cpDesignationIdMap,
                            cpDesignationId
                        )!!
                    )
                }
            }
        }

        binding.spinnerDecisionDesignation.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    ownerDesignation = ownerDesignationList[position]
                    ownerDesignationId = ownerDesignationIdMap[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }

            }


        binding.decisionMakerMobileNumber.addTextChangedListener(object : TextWatcher {
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
                if (!isCorrectPhoneNumber(s.toString())) {
                    binding!!.decisionMakerMobileNumber.error = "Number must be correct and unique"
                    isCorrectOwnerNumber = false
                    //setCallGone()
                } else {
                    isCorrectOwnerNumber = true
                    outletOwnerNumber = s.toString()
                    //checkDuplicate(outletContactNumber!!)

                }

            }
        })

        binding!!.contactEmail.addTextChangedListener(object : TextWatcher {
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
                if (!isValidEmail(s.toString())) {
                    binding!!.contactEmail.error = "Email must be in correct format"
                    isCorrectEmail = false
                    //setCallGone()
                } else {
                    isCorrectEmail = true
                    //checkDuplicate(outletContactNumber!!)

                }

            }
        })
        binding!!.spinnerCurrentBrand.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    currentBrand = currentBrandList[position]
                    currentBrandId = currentBrandIdMap[position]
                    if (currentBrandId == "101") {
                        binding.otherBrand.visibility = View.VISIBLE
                    }
                    else
                    {
                        binding.otherBrand.visibility = View.GONE
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }

            }


        binding!!.spinnerMachine.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    machineBrand = machineBrandList[position]
                    machineBrandId = machineBrandIdMap[position]
                    if (machineBrandId == "101") {
                        binding.otherMachine.visibility = View.VISIBLE
                    }
                    else{
                        binding.otherMachine.visibility = View.GONE
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }

            }
        binding.spinnerStatus.adapter =
            ArrayAdapter(requireContext(), R.layout.spinner_item, statusList)
        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                statusId = (position + 2).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        binding.submitBtn.setOnClickListener()
        {
            if (checkFields()) {
                val confirm = SweetAlertDialog(requireContext(), SweetAlertDialog.WARNING_TYPE)
                confirm.setTitle("Are you sure?")
                confirm.setConfirmButton("Yes") {
                    confirm.dismissWithAnimation()
                    upload()
                }
                confirm.setCancelButton("No") { confirm.dismissWithAnimation() }
                confirm.show()
            }
        }

        binding.takeGpsChk.setOnCheckedChangeListener { compoundButton, b ->
            takeGps = b
        }

    }


    private fun isCorrectPhoneNumber(phone: String): Boolean {
        if ((phone == "") || (phone.length != 11)) {
            return false
        }
        val code2 = phone.substring(0, 3)
        for (op: String in operatorList) {
            if ((op == code2)) {
                return true
            }
        }
        return false
    }

    private fun checkFields(): Boolean {
        if (CustomUtility.haveNetworkConnection(requireContext())) {
            if (!CustomUtility.haveNetworkConnection(requireContext())) {
                Toast.makeText(requireContext(), "No internet connection!", Toast.LENGTH_SHORT)
                    .show()
                return false
            } else if (binding!!.outletName.text.toString() == "") {
                Toast.makeText(requireContext(), "Please fill required fields!", Toast.LENGTH_SHORT)
                    .show()
                binding!!.outletName.error = "Required Field!"
                return false
            } else if (outletType == "") {
                Toast.makeText(requireContext(), "Please fill required fields!", Toast.LENGTH_SHORT)
                    .show()
                CustomUtility.showError(
                    requireContext(),
                    "Please select a outlet's type",
                    "Required Field!"
                )
                return false
            } else if (binding!!.outletAddress.text.toString() == "") {
                Toast.makeText(requireContext(), "Please fill required fields!", Toast.LENGTH_SHORT)
                    .show()
                binding!!.outletAddress.error = "Required Field!"
                return false
            } else if (binding!!.outletContactPerson.text.toString() == "") {
                Toast.makeText(requireContext(), "Please fill required fields!", Toast.LENGTH_SHORT)
                    .show()
                binding!!.outletContactPerson.error = "Required Field!"
                return false
            } else if (!isCorrectPrimaryNumber) {
                Toast.makeText(requireContext(), "Please fill required fields!", Toast.LENGTH_SHORT)
                    .show()
                binding.contactPersonMobileNumber.error = "Required Field!"
                return false
            } else if (cpDesignationId == "") {
                Toast.makeText(requireContext(), "Please fill required fields!", Toast.LENGTH_SHORT)
                    .show()
                CustomUtility.showError(
                    requireContext(),
                    "Please select contact person's designation",
                    "Required Field!"
                )
                return false
            } else if (ownerDesignationId == "") {
                Toast.makeText(requireContext(), "Please fill required fields!", Toast.LENGTH_SHORT)
                    .show()
                CustomUtility.showError(
                    requireContext(),
                    "Please select contact person's designation",
                    "Required Field!"
                )
                return false
            } else if (!isCorrectEmail) {
                Toast.makeText(requireContext(), "Please fill required fields!", Toast.LENGTH_SHORT)
                    .show()
                binding.contactEmail.error = "Required Field!"
                return false
            } else if (area == "") {
                Toast.makeText(requireContext(), "Please fill required fields!", Toast.LENGTH_SHORT)
                    .show()
                CustomUtility.showError(
                    requireContext(),
                    "Please select a thana",
                    "Required Field!"
                )
                return false
            }   else if (currentBrandId == "") {
                Toast.makeText(requireContext(), "Please fill required fields!", Toast.LENGTH_SHORT)
                    .show()
                CustomUtility.showError(
                    requireContext(),
                    "Please select current brand using",
                    "Required Field!"
                )
                return false
            } else if (currentBrandId == "101" && binding.otherBrand.text.toString() == "") {
                Toast.makeText(requireContext(), "Please fill required fields!", Toast.LENGTH_SHORT)
                    .show()
                binding.otherBrand.error = "Required Field!"
                CustomUtility.showError(
                    requireContext(),
                    "Please specify other brand using",
                    "Required Field!"
                )
                return false
            } else if (machineBrandId == "") {
                Toast.makeText(requireContext(), "Please fill required fields!", Toast.LENGTH_SHORT)
                    .show()
                CustomUtility.showError(
                    requireContext(),
                    "Please select current machine using",
                    "Required Field!"
                )
                return false
            } else if (machineBrandId == "101" && binding.otherMachine.text.toString() == "") {
                Toast.makeText(requireContext(), "Please fill required fields!", Toast.LENGTH_SHORT)
                    .show()
                binding.otherMachine.error = "Required Field!"
                CustomUtility.showError(
                    requireContext(),
                    "Please specify other machine using",
                    "Required Field!"
                )
                return false
            } else if (statusId == "") {
                Toast.makeText(requireContext(), "Please fill required fields!", Toast.LENGTH_SHORT)
                    .show()
                CustomUtility.showError(
                    requireContext(),
                    "Please select outlet status",
                    "Required Field!"
                )
                return false
            } else if (presentAcc.equals(null)) {
                CustomUtility.showWarning(
                    requireContext(),
                    "Wait for gps location",
                    "Mandatory Field"
                )
                return false
            }

            return true
        } else {
            Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show()
            return false
        }

    }


    private fun getAreaList() {
        if (CustomUtility.haveNetworkConnection(requireContext())) {
            sweetAlertDialog =
                SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
            sweetAlertDialog!!.titleText = "Loading"
            sweetAlertDialog!!.show()
            sweetAlertDialog!!.setCancelable(false)
            val retrofit = Retrofit.Builder()
                .baseUrl(StaticTags.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(OutletApiInterface::class.java)
            val call = service.getAreaList(user!!.zoneId!!, user!!.userId!!)

            call.enqueue(object : Callback<AreaResponseBody> {
                override fun onResponse(
                    call: Call<AreaResponseBody>?,
                    response: retrofit2.Response<AreaResponseBody>?
                ) {
                    sweetAlertDialog?.dismiss()
                    if (response != null) {
                        if (response.code() == 200) {
                            val areaResponseBody = response.body()!!
                            if (areaResponseBody.success) {
                                val itemList = areaResponseBody.resultList
                                sharedPreferences = activity?.getSharedPreferences(
                                    "user",
                                    AppCompatActivity.MODE_PRIVATE
                                )
                                val editor: SharedPreferences.Editor = sharedPreferences!!.edit()
                                user?.setAreaList(
                                    editor,
                                    "areaList",
                                    Gson().toJson(areaResponseBody)
                                )
                                areaList = ArrayList()
                                for ((ind, i) in itemList.withIndex()) {
                                    areaList.add(i.name)
                                    areaIdMap[ind] = i.id
                                    //Log.d("ind", i.id)
                                }
                                binding.spinnerArea.adapter =
                                    ArrayAdapter(requireContext(), R.layout.spinner_item, areaList)
                                getInitialList()

                            } else {
                                val s =
                                    SweetAlertDialog(requireContext(), SweetAlertDialog.ERROR_TYPE)
                                s.titleText = "Failed!"
                                s.contentText = areaResponseBody.message
                                s.setConfirmButton("Ok", SweetAlertDialog.OnSweetClickListener {
                                    s.dismissWithAnimation()
                                })
                                s.setCancelable(false)
                                s.show()
                                Log.d("null", "session expired")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<AreaResponseBody>?, t: Throwable?) {
                    sweetAlertDialog?.dismiss()
                    //Log.e("res", error.toString())
                    CustomUtility.showError(
                        requireContext(),
                        "Network Error, try again!",
                        "Failed"
                    )
                }
            })
        } else {
            CustomUtility.showError(
                requireContext(),
                "Please Check your internet connection",
                "Network Warning !!!"
            )
        }
    }


    private fun getInitialList() {
        if (CustomUtility.haveNetworkConnection(requireContext())) {
            sweetAlertDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
            sweetAlertDialog!!.titleText = "Loading"
            sweetAlertDialog!!.show()
            sweetAlertDialog!!.setCancelable(false)
            val retrofit = Retrofit.Builder()
                .baseUrl(StaticTags.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(OutletApiInterface::class.java)
            val call = service.getInitialList(user!!.userId!!)

            call.enqueue(object : Callback<InitialListResponseBody> {
                override fun onResponse(
                    call: Call<InitialListResponseBody>?,
                    response: retrofit2.Response<InitialListResponseBody>?
                ) {
                    sweetAlertDialog?.dismiss()
                    Log.d("response", response?.body().toString())
                    if (response != null) {
                        if (response.code() == 200) {
                            val initialListResponseBody = response.body()!!
                            if (initialListResponseBody.success) {
                                val itemList = initialListResponseBody.allList
                                for ((ind, i) in itemList.DesignationList.withIndex()) {
                                    cpDesignationList.add(i.name)
                                    cpDesignationIdMap[ind] = i.id
                                    ownerDesignationList.add(i.name)
                                    ownerDesignationIdMap[ind] = i.id
                                }

                                for ((ind, i) in itemList.BrandList.withIndex()) {
                                    currentBrandList.add(i.name)
                                    currentBrandIdMap[ind] = i.id
                                }

                                for ((ind, i) in itemList.MachineList.withIndex()) {
                                    machineBrandList.add(i.name)
                                    machineBrandIdMap[ind] = i.id
                                }

                                for ((ind, i) in itemList.SyrupList.withIndex()) {
                                    syrupBrandList.add(Syrup(i.id, i.name))
                                }
                                binding.spinnerContactDesignation.adapter = ArrayAdapter(
                                    requireContext(),
                                    R.layout.spinner_item,
                                    cpDesignationList
                                )
                                binding.spinnerDecisionDesignation.adapter = ArrayAdapter(
                                    requireContext(),
                                    R.layout.spinner_item,
                                    ownerDesignationList
                                )

                                binding.spinnerCurrentBrand.adapter = ArrayAdapter(
                                    requireContext(),
                                    R.layout.spinner_item,
                                    currentBrandList
                                )
                                binding.spinnerMachine.adapter = ArrayAdapter(
                                    requireContext(),
                                    R.layout.spinner_item,
                                    machineBrandList
                                )

                                mAdapter!!.notifyDataSetChanged()

                                setupViewModel()
                            } else {
                                val s =
                                    SweetAlertDialog(requireContext(), SweetAlertDialog.ERROR_TYPE)
                                s.titleText = "Session Expired!"
                                s.contentText = "Please login again"
                                s.setConfirmButton("Ok", SweetAlertDialog.OnSweetClickListener {
                                    s.dismissWithAnimation()
                                    val editor = requireActivity().getSharedPreferences(
                                        "user",
                                        Context.MODE_PRIVATE
                                    ).edit()
                                    user?.clear(editor)
                                    activity!!.finish()
                                    val intent = Intent(requireContext(), LoginActivity::class.java)
                                    startActivity(intent)
                                })
                                s.setCancelable(false)
                                s.show()
                                Log.d("null", "session expired")
                            }
                        }
                    } else {
                        val s = SweetAlertDialog(requireContext(), SweetAlertDialog.ERROR_TYPE)
                        s.titleText = "Session Expired!"
                        s.contentText = "Please login again"
                        s.setConfirmButton("Ok", SweetAlertDialog.OnSweetClickListener {
                            s.dismissWithAnimation()
                            val editor =
                                requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE)
                                    .edit()
                            user?.clear(editor)
                            activity!!.finish()
                            val intent = Intent(requireContext(), LoginActivity::class.java)
                            startActivity(intent)
                        })
                        s.setCancelable(false)
                        s.show()
                        Log.d("null", "session expired")
                    }
                }

                override fun onFailure(call: Call<InitialListResponseBody>?, t: Throwable?) {
                    sweetAlertDialog?.dismiss()
                    //Log.e("res", error.toString())
                    CustomUtility.showError(
                        requireContext(),
                        "Network Error, try again!",
                        "Failed"
                    )
                }
            })
        } else {
            CustomUtility.showError(
                requireContext(),
                "Please Check your internet connection",
                "Network Warning !!!"
            )
        }
    }


    private fun isValidEmail(email: String): Boolean {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }


    private fun setupViewModel() {
        outLetViewModel =
            ViewModelProvider(requireActivity()).get(OutletUpdateViewModel::class.java)
        if(isInActivity)
        {
            requireActivity().viewModelStore.clear()
            isInActivity = false
        }
        val observer =
            Observer<com.example.coffeeproject.ui.outletactivity.singleoutletdetails.OutletResult> { data ->
                outletId = data.id!!
                if (!data.name.isNullOrEmpty())
                    binding.outletName.setText(data.name)
                if (!data.outlet_type_id.isNullOrEmpty()) {
                    if (data.outlet_type_id == "1")
                        binding.chain.isChecked = true
                    else
                        binding.single.isChecked = true
                }
                if (!data.area_id.isNullOrEmpty()) {
                    val temp = getKey(areaIdMap,data.area_id)
                    if(temp != null)
                    binding.spinnerArea.setSelection(getKey(areaIdMap, data.area_id)!!)
                }

                if (!data.address.isNullOrEmpty()) {
                    binding.outletAddress.setText(data.address)
                }


                if (!data.contact_person_name.isNullOrEmpty()) {
                    binding.outletContactPerson.setText(data.contact_person_name)
                }
                if (!data.contact_person_designation_id.isNullOrEmpty()) {
                    binding.spinnerContactDesignation.setSelection(
                        getKey(
                            cpDesignationIdMap,
                            data.contact_person_designation_id
                        )!!
                    )
                }
                if (!data.contact_person_mobile.isNullOrEmpty()) {
                    binding.contactPersonMobileNumber.setText(data.contact_person_mobile)
                }


                if (!data.decision_maker_name.isNullOrEmpty()) {
                    binding.outletDecisionMakerPerson.setText(data.decision_maker_name)
                }
                if (!data.decision_maker_designation_id.isNullOrEmpty()) {
                    binding.spinnerDecisionDesignation.setSelection(
                        getKey(
                            ownerDesignationIdMap,
                            data.decision_maker_designation_id
                        )!!
                    )
                }
                if (!data.decision_maker_mobile.isNullOrEmpty()) {
                    binding.decisionMakerMobileNumber.setText(data.decision_maker_mobile)
                }
                if (!data.coms_email.isNullOrEmpty()) {
                    binding.contactEmail.setText(data.coms_email)
                }


                /* if(!data.saturday.isNullOrEmpty() && data.saturday == "Closed")
                 {
                     binding.chkSaturday.isChecked = true
                 }
                 if(!data.sunday.isNullOrEmpty() && data.sunday == "Closed")
                 {
                     binding.chkSunday.isChecked = true
                 }
                 if(!data.monday.isNullOrEmpty() && data.monday == "Closed")
                 {
                     binding.chkMonday.isChecked = true
                 }
                 if(!data.tuesday.isNullOrEmpty() && data.tuesday == "Closed")
                 {
                     binding.chkTuesday.isChecked = true
                 }
                 if(!data.wednesday.isNullOrEmpty() && data.wednesday == "Closed")
                 {
                     binding.chkWednesday.isChecked = true
                 }
                 if(!data.thursday.isNullOrEmpty() && data.thursday == "Closed")
                 {
                     binding.chkThursday.isChecked = true
                 }
                 if(!data.friday.isNullOrEmpty() && data.friday == "Closed")
                 {
                     binding.chkFriday.isChecked = true
                 }
     */
                if (!data.seat_capacity.isNullOrEmpty()) {
                    binding.seatingCapacity.setText(data.seat_capacity)
                }
                if (!data.current_coffee_brands_id.isNullOrEmpty()) {
                    binding.spinnerCurrentBrand.setSelection(
                        getKey(
                            currentBrandIdMap,
                            data.current_coffee_brands_id
                        )!!
                    )
                    if (!data.current_coffee_brands_other.isNullOrEmpty()) {
                        binding.otherBrand.setText(data.current_coffee_brands_other)
                    }
                }

                if (!data.current_machine_using.isNullOrEmpty()) {
                    binding.spinnerMachine.setSelection(
                        getKey(
                            machineBrandIdMap,
                            data.current_machine_using
                        )!!
                    )
                    if (!data.current_machine_other.isNullOrEmpty()) {
                        binding.otherMachine.setText(data.current_machine_other)
                    }
                }

                if (!data.sourcing_price.isNullOrEmpty()) {
                    binding.sourcingPrice.setText(data.sourcing_price)
                }
                if (!data.monthly_sourcing_in_kg.isNullOrEmpty()) {
                    binding.sourcingQuantity.setText(data.monthly_sourcing_in_kg)
                }

                if (data.usingSyrups != null) {
                    for (new in data.usingSyrups!!) {
                        for ((i, old) in syrupBrandList.withIndex()) {
                            if (new.outlet_syrup_id == old.id) {
                                syrupBrandList[i].isChecked = true
                            }
                        }
                    }
                    mAdapter!!.notifyDataSetChanged()
                }

                if (!data.daily_average_sales_in_cup.isNullOrEmpty()) {
                    binding.salesPerDay.setText(data.daily_average_sales_in_cup)
                }
                if (!data.outlet_status_id.isNullOrEmpty()) {
                    binding.spinnerStatus.setSelection(data.outlet_status_id.toInt() - 2)
                }
                if (!data.remarks.isNullOrEmpty()) {
                    binding.remark.setText(data.remarks)
                }

            }


        outLetViewModel.getOutlet().observe(requireActivity(), observer)
    }


    private fun <K, V> getKey(map: Map<K, V>, target: V): K? {
        for ((key, value) in map) {
            if (target == value) {
                return key
            }
        }
        return null
    }

    //{"success":true,"message":"Data updated in outlet","outletId":"11"}

    private fun upload() {

        val sweetAlertDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        sweetAlertDialog.titleText = "Loading"
        sweetAlertDialog.setCancelable(false)
        sweetAlertDialog.show()
        val queue = Volley.newRequestQueue(requireContext())
        val url = StaticTags.BASE_URL + "outlet/insert_outlet.php "

        val sr: StringRequest = object : StringRequest(Method.POST, url,
            Response.Listener {
                sweetAlertDialog.dismiss()
                try {
                    Log.d("response:", it)
                    val jsonObject = JSONObject(it)
                    if (jsonObject.getBoolean("success")) {
                        val sharedPreferences = requireActivity().getSharedPreferences(
                            "user",
                            AppCompatActivity.MODE_PRIVATE
                        )
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("formDone", true)
                        editor.putString("outletId", jsonObject.getString("outletId"))
                        editor.putString("outletName", binding.outletName.text.toString())
                        editor.putString("outletAddress", "$area, $zone")
                        editor.apply()

                        /* requireActivity().supportFragmentManager.commit {
                            setCustomAnimations(
                                R.anim.slide_in_right,
                                R.anim.slide_out_left,
                                R.anim.slide_in_left,
                                R.anim.slide_out_right
                            )
                            replace(R.id.nav_host_fragment, FragmentPicture())
                            addToBackStack(null)
                        }*/

                        findNavController().navigate(R.id.action_outletRegistrationFragment_to_pictureFragment)

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
                params["AppVersion"] = getString(R.string.version)
                params["OutletName"] = binding.outletName.text.toString()
                params["Address"] = binding.outletAddress.text.toString()
                params["OutletTypeId"] = outletTypeId!!
                params["AreaId"] = areaId!!

                Log.d("outletId: ", outletId)
                if (outletId != "")
                    params["OutletId"] = outletId

                params["ContactPersonMobile"] = outletContactNumber!!
                params["ContactPersonName"] = binding.outletContactPerson.text.toString()
                params["ContactPersonDesignationId"] = cpDesignationId!!
                params["ComsEmail"] = binding.contactEmail.text.toString()
                params["DecisionMakerMobile"] = outletOwnerNumber!!
                params["DecisionMakerName"] = binding.outletDecisionMakerPerson.text.toString()
                params["DecisionMakerDesignationId"] = ownerDesignationId!!

                params["SeatCapacity"] = binding.seatingCapacity.text.toString()
                params["CurrentCoffeeBrandId"] = currentBrandId!!
                if (binding.otherBrand.text.toString() != "") {
                    params["CurrentCoffeeBrandOther"] = binding.otherBrand.text.toString()
                }



                params["SourcingPrice"] = binding.sourcingPrice.text.toString()
                params["MonthlySourcingInKg"] = binding.sourcingQuantity.text.toString()

                params["CurrentMachineUsing"] = machineBrandId!!
                if (binding.otherMachine.text.toString() != "") {
                    params["CurrentMachineOther"] = binding.otherMachine.text.toString()
                }
                var id = ""
                var syrupId = ""
                for ((key, value) in syrupBrandIdMap) {
                    id += "$key,"
                }
                //syrupId.dropLast(1)
                val l = id.length
                for (i in 0 until l) {
                    if (i != l - 1)
                        syrupId += id[i]
                }
                if(syrupId != "")
                params["CurrentSyrupUsing"] = syrupId

                Log.d("machine syrup id", machineBrandId.toString() + "     " + syrupId)

                params["OutletStatusId"] = statusId!!
                params["DailyAverageSalesInCup"] = binding.salesPerDay.text.toString()

                if(takeGps)
                {
                    params["LatValue"] = presentLat!!
                    params["LonValue"] = presentLon!!
                    params["Accuracy"] = presentAcc!!
                }
                if (binding.remark.text.toString() != "")
                    params["Remarks"] = binding.remark.text.toString()

                /*
                var offDays = ""
                if(binding.chkFriday.isChecked) offDays += "\'Friday\',"
                if(binding.chkSaturday.isChecked) offDays += "\'Saturday\',"
                if(binding.chkSunday.isChecked) offDays += "\'Sunday\',"
                if(binding.chkMonday.isChecked) offDays += "\'Monday\',"
                if(binding.chkTuesday.isChecked) offDays += "\'Tuesday\',"
                if(binding.chkWednesday.isChecked) offDays += "\'Wednesday\',"
                if(binding.chkThursday.isChecked) offDays += "\'Thursday\',"

                if(offDays != "")
                {
                    offDays.dropLast(1)
                }
                params["OffDays"] = offDays

                 */


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

    private fun checkForUpdate() {

        val queue = Volley.newRequestQueue(requireContext())
        val url = StaticTags.BASE_URL + "app_version/version_check.php "

        val sr: StringRequest = object : StringRequest(Method.POST, url,
            Response.Listener {
                Log.d("response:", it)
                val jsonObject = JSONObject(it)
                if (jsonObject.getBoolean("success")) {
                    val s = SweetAlertDialog(requireContext(), SweetAlertDialog.WARNING_TYPE)
                    s.titleText = "New Update"
                    s.contentText = "Please update the \n application"
                    s.setCancelable(false)
                    s.setConfirmButton("Ok", SweetAlertDialog.OnSweetClickListener {
                        val editor = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE).edit()
                        user?.clear(editor)
                        s.dismissWithAnimation()
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                jsonObject.getJSONObject("appInfo").getString("app_location_url")
                            )
                        )
                        startActivity(browserIntent)
                    })
                    s.show()

                    disableAllMenuItem()
                } else {
                    //CustomUtility.showError(requireContext(), "Failed!", jsonObject.getString("message"))
                }

            },
            Response.ErrorListener {
                CustomUtility.showError(requireContext(), "Network problem, try again", "Failed")
            }) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["UserId"] = user!!.userId!!
                params["CurrentVersion"] = getString(R.string.version)
                params["AppName"] = "bp"
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

    inner class SyrupDataAdapter(dataList: java.util.ArrayList<Syrup>, context: Context) :
        RecyclerView.Adapter<SyrupDataAdapter.MyViewHolder>() {
        var mc = context

        private val dataList: java.util.ArrayList<Syrup> = dataList
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.syrup_brand_row_layout, parent, false)
            return MyViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val data = dataList[position]


            holder.chk.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    syrupBrandIdMap[data.id] = data.name
                } else {
                    syrupBrandIdMap.remove(data.id)
                }
            }
            if (data.isChecked) {
                holder.chk.isChecked = true
            }

            holder.chk.text = data.name
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        inner class MyViewHolder(convertView: View) : RecyclerView.ViewHolder(convertView) {
            var chk: CheckBox = convertView.findViewById(R.id.checkbox)

        }

    }

    private fun disableAllMenuItem() {
        menuNav?.findItem(R.id.attendanceFragment)?.isEnabled = false
        menuNav?.findItem(R.id.logoutFragment)?.isEnabled = false
        menuNav?.findItem(R.id.outletUpdateFragment)?.isEnabled = false
        binding.submitBtn.isEnabled = false
    }

    override fun onStop() {
        super.onStop()
        //requireActivity().viewModelStore.clear()
    }
}