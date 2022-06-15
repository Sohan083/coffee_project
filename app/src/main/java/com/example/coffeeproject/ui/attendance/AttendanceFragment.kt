package com.example.coffeeproject.ui.attendance

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import cn.pedant.SweetAlert.SweetAlertDialog
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.example.coffeeproject.MainActivity.Companion.presentAcc
import com.example.coffeeproject.MainActivity.Companion.presentLat
import com.example.coffeeproject.MainActivity.Companion.presentLon
import com.example.coffeeproject.R
import com.example.coffeeproject.databinding.FragmentAttendanceBinding
import com.example.coffeeproject.model.User
import com.example.coffeeproject.model.User.Companion.user
import com.example.coffeeproject.ui.attendance.attendanceresponse.AttendanceStatusResponseBody
import com.example.coffeeproject.utils.CustomUtility
import com.example.coffeeproject.utils.CustomUtility.deleteFile
import com.example.coffeeproject.utils.GPSLocation
import com.example.coffeeproject.utils.MySingleton
import com.example.coffeeproject.utils.StaticTags
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AttendanceFragment : Fragment() {
    lateinit var binding: FragmentAttendanceBinding
    var imageString = ""
    var photoFlag = false
    var attendanceInFlag = false
    var attendanceOutFlag = false
    var network = false
    var pDialog: SweetAlertDialog? = null
    var activeBtn = ""
    var currentPath: String? = null
    private var gpsLocation: GPSLocation? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAttendanceBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize()
    }

    fun initialize() {
        user = User.instance


        gpsLocation = GPSLocation(requireContext())
        gpsLocation!!.GPS_Start()
        gpsLocation!!.setLocationChangedListener(object : GPSLocation.LocationChangedListener {
            override fun locationChangeCallback(lat: String?, lon: String?, acc: String?) {
                presentLat = lat
                presentLon = lon
                presentAcc = acc
                binding.gpsText.text = presentAcc
            }

        })

        getAttendance()

        binding.inbtn.setOnClickListener(View.OnClickListener {
            activeBtn = "in"
            binding.submitbtn.visibility = View.VISIBLE
            binding.inbtn.setBackgroundResource(R.drawable.ic_attendance_active_btn)
            binding.inbtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            binding.outbtn.setBackgroundResource(R.drawable.ic_attendance_inactive_btn)
            binding.outbtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.takeSelfieText.text = ""
            crossfadeVisible(binding.inOutLayout)
            //crossfadeVisible(binding.lateRemark)
            photoFlag = false
            if (currentPath != null) {
                val file = File(currentPath!!)
                if (file.exists()) {
                    deleteFile(file)
                }
            }

            binding.takeSelfieText.resources.getColor(R.color.text_color)
        })
        binding.outbtn.setOnClickListener(View.OnClickListener {
            activeBtn = "out"
            binding.submitbtn.visibility = View.VISIBLE
            if (!attendanceInFlag) {
                binding.inbtn.setBackgroundResource(R.drawable.ic_attendance_inactive_btn)
                binding.inbtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }

            binding.outbtn.setBackgroundResource(R.drawable.ic_attendance_active_btn)
            binding.outbtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            binding.lateRemark.visibility = View.GONE
            binding.attendanceInfoLayout.visibility = View.GONE
            binding.takeSelfieText.text = ""
            crossfadeVisible(binding.inOutLayout)
            photoFlag = false
            if (currentPath != null) {
                val file = File(currentPath!!)
                if (file.exists()) {
                    deleteFile(file)
                }
            }
            binding.takeSelfieText.resources.getColor(R.color.text_color)
        })


        binding.camera.setOnClickListener(View.OnClickListener {
            dispatchTakePictureIntent(StaticTags.IMAGE_CAPTURE_CODE)
        })


        binding.submitbtn.setOnClickListener(View.OnClickListener {
            network = CustomUtility.haveNetworkConnection(requireContext())
            val flag = chekFeilds()
            if (flag) {
                val confirm = SweetAlertDialog(requireContext(), SweetAlertDialog.WARNING_TYPE)
                confirm.setTitle("Are you sure?")
                confirm.setConfirmButton("Yes") {
                    confirm.dismissWithAnimation()
                    upload()
                }
                confirm.setCancelButton("No") { confirm.dismissWithAnimation() }
                confirm.show()
            }
        })
    }

    private fun disableAll() {
        binding.inbtn.isEnabled = false
        binding.outbtn.isEnabled = false
    }

    private fun enableAll() {
        binding.inbtn.isEnabled = true
        binding.outbtn.isEnabled = true
    }


    //after finishing camera intent whether the picture was save or not
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == StaticTags.IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            val file = File(currentPath!!)
            if (file.exists()) {
                val bitmap = MediaStore.Images.Media.getBitmap(
                    requireActivity().contentResolver,
                    Uri.fromFile(file)
                )
                imageString = CustomUtility.imageToString(bitmap)
                binding.takeSelfieText.text = getString(R.string.take_image_done)
                photoFlag = true
            }

        }
    }


    private fun chekFeilds(): Boolean {
        if (!network) {
            CustomUtility.showWarning(
                requireContext(),
                "Please turn on internet connection",
                "No internet connection!"
            )
            return false
        }
        if (activeBtn == "") {
            CustomUtility.showWarning(
                requireContext(),
                "Select attendance type In, Out or Leave",
                "Required field!"
            )
            return false
        } else if (presentAcc == "") {
            CustomUtility.showWarning(
                requireContext(),
                "Please wait for the gps",
                "Required fields"
            )
            return false
        } else if ((activeBtn == "in") or (activeBtn == "out") and !photoFlag) {
            CustomUtility.showWarning(requireContext(), "Take selfie", "Required field!")
            return false
        }
        return true
    }


    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? =
            requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPath = absolutePath
        }
    }

    private fun dispatchTakePictureIntent(code: Int) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.example.coffeeproject.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, code)
                }
            }
        }
    }


    private fun upload() {
        pDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        pDialog!!.progressHelper.barColor = Color.parseColor("#08839b")
        pDialog!!.titleText = "Loading"
        pDialog!!.setCancelable(false)
        pDialog!!.show()
        val upLoadServerUri: String = if (activeBtn == "in") {
            StaticTags.BASE_URL + "attendance/insert_daily_attendance_in.php"
        } else {
            StaticTags.BASE_URL + "attendance/insert_daily_attendance_out.php"
        }
        val stringRequest: StringRequest = object : StringRequest(Method.POST, upLoadServerUri,
            Response.Listener { response ->
                pDialog!!.dismiss()
                Log.e("response", response!!)
                try {
                    val jsonObject = JSONObject(response)
                    var code = jsonObject.getString("success")
                    val message = jsonObject.getString("message")
                    if (code == "true") {
                        deleteFile(File(currentPath!!))
                        code = "Successful"
                        SweetAlertDialog(requireContext(), SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Successful")
                            .setContentText("")
                            .setConfirmText("Ok")
                            .setConfirmClickListener { sDialog ->
                                sDialog.dismissWithAnimation()
                                startActivity(requireActivity().intent)
                                requireActivity().finish()
                            }
                            .show()
                    } else {
                        code = "Failed"
                        CustomUtility.showError(requireContext(), message, code)
                        //CustomUtility.showError(AttendanceActivity.this,"You allready submitted in",code);
                    }
                } catch (e: JSONException) {
                    CustomUtility.showError(requireContext(), e.message, "Failed")
                }
            }, Response.ErrorListener { error ->
                pDialog!!.dismiss()
                Log.e("response", error.toString())
                CustomUtility.showError(requireContext(), "Network slow, try again", "Failed")
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["UserId"] = user!!.userId!!
                params["LatValue"] = presentLat!!
                params["LonValue"] = presentLon!!
                params["Accuracy"] = presentAcc!!
                params["ImageData"] = imageString
                return params
            }
        }
        stringRequest.retryPolicy =
            DefaultRetryPolicy(50000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        MySingleton.getInstance(requireContext())?.addToRequestQue(stringRequest)
    }


    private fun getAttendance() {

        pDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        pDialog!!.progressHelper.barColor = Color.parseColor("#08839b")
        pDialog!!.titleText = "Loading"
        pDialog!!.setCancelable(false)
        pDialog!!.show()
        val retrofit = Retrofit.Builder()
            .baseUrl(StaticTags.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(AttendanceApiInterface::class.java)
        val call = service.getAttendanceStatus(user!!.userId!!, user!!.employeeId!!, "current")

        call.enqueue(object : Callback<AttendanceStatusResponseBody> {
            override fun onResponse(
                call: Call<AttendanceStatusResponseBody>?,
                response: retrofit2.Response<AttendanceStatusResponseBody>?
            ) {
                pDialog?.dismiss()
                Log.d("response", response?.body().toString())
                if (response != null) {
                    if (response.code() == 200) {
                        val attendanceStatusResponseBody = response.body()!!
                        if (attendanceStatusResponseBody.success) {
                            val s = attendanceStatusResponseBody.resultList[0]
                            crossfadeVisible(binding.attendanceInfoLayout)
                            binding.inbtn.setBackgroundResource(R.drawable.ic_attendance_active_btn)
                            binding.inbtn.isEnabled = false
                            binding.inTime.text = "In time: " + s.in_time
                            attendanceInFlag = true
                            if (!s.out_time.isNullOrEmpty()) {
                                binding.outbtn.setBackgroundResource(R.drawable.ic_attendance_active_btn)
                                binding.outbtn.isEnabled = false
                                binding.outTime.text = "Out time: " + s.out_time
                                attendanceOutFlag = true
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<AttendanceStatusResponseBody>?, t: Throwable?) {
                pDialog?.dismiss()
                Log.e("res", t.toString())
                CustomUtility.showError(
                    requireContext(),
                    "Network Error, try again!",
                    "Failed"
                )
            }
        })

    }

    private fun crossfadeVisible(view: View) {

        view.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            duration = 1000
            start()
        }
    }


    companion object {
        const val REQUEST_IMAGE_CAPTURE = 6
    }
}