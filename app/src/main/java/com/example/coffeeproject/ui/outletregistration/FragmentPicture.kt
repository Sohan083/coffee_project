package com.example.coffeeproject.ui.outletregistration

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.coffeeproject.MainActivity
import com.example.coffeeproject.R
import com.example.coffeeproject.databinding.FragmentPictureBinding
import com.example.coffeeproject.model.User
import com.example.coffeeproject.model.User.Companion.user
import com.example.coffeeproject.ui.outletactivity.OutletListApiInterface
import com.example.coffeeproject.ui.outletactivity.OutletUpdateViewModel
import com.example.coffeeproject.ui.outletactivity.singleoutletdetails.OutletResult
import com.example.coffeeproject.ui.outletactivity.singleoutletdetails.SingleOutletDetailsResponseBody
import com.example.coffeeproject.utils.CustomUtility
import com.example.coffeeproject.utils.StaticTags
import com.google.gson.Gson
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FragmentPicture: Fragment() {
    private var binding: FragmentPictureBinding? = null
    var sweetAlertDialog: SweetAlertDialog? = null
    var imageString: String? = null; var imageType: String? = null
    var selfieImageDone = false
    var outletFrontImageDone = false
    var interior1ImageDone = false
    var interior2ImageDone = false
    var outletId:String? = null
    lateinit var outletUpdateViewModel: OutletUpdateViewModel
    var currentPath: String? = null
    var sharedPreferences: SharedPreferences? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(savedInstanceState != null)
        {
            currentPath = savedInstanceState.getString("currentPath", null)
        }
        binding = FragmentPictureBinding.inflate(inflater, container, false)
        outletUpdateViewModel = ViewModelProvider(requireActivity()).get(OutletUpdateViewModel::class.java)
        return  binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        disableAllMenuItem()

        sharedPreferences = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE)
        user = User.instance
        if (user!!.isUserInSharedpreference(sharedPreferences!!, "id")) {
            user!!.setValuesFromSharedPreference(sharedPreferences!!)
        }
        val sharedPreferences = requireActivity().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)

        selfieImageDone = sharedPreferences.getBoolean("selfieImageDone", false)
        outletFrontImageDone = sharedPreferences.getBoolean("outletFrontImageDone", false)
        interior1ImageDone = sharedPreferences.getBoolean("interior1ImageDone", false)
        interior2ImageDone = sharedPreferences.getBoolean("interior2ImageDone", false)

        outletId = sharedPreferences.getString("outletId",null)
        binding!!.outletName.text = sharedPreferences.getString("outletName","")
        binding!!.outletId.text = "Outlet Id: " +outletId
        binding!!.outletAddress.text = sharedPreferences.getString("outletAddress","")

        getOutletDetails(outletId!!)

        binding!!.selfieContactPerson.setOnClickListener {
            dispatchTakePictureIntent(StaticTags.SELFIE_IMAGE_CAPTURE_CODE)

        }
        binding!!.outletFront.setOnClickListener {
            dispatchTakePictureIntent(StaticTags.OUTLET_FRONT_IMAGE_CAPTURE_CODE)

        }
        binding!!.interior1.setOnClickListener {
            dispatchTakePictureIntent(StaticTags.INTERIOR1_IMAGE_CAPTURE_CODE)

        }
        binding!!.interior2.setOnClickListener {
            dispatchTakePictureIntent(StaticTags.INTERIOR2_IMAGE_CAPTURE_CODE)

        }

        binding!!.endBtn.setOnClickListener{


            if(checkFields())
            {


                val confirm = SweetAlertDialog(requireContext(), SweetAlertDialog.WARNING_TYPE)
                confirm.setTitle("Are you sure?")
                confirm.setConfirmButton("Yes") {
                    confirm.dismissWithAnimation()
                    val sharedPreferences:SharedPreferences = requireActivity().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("formDone",false)
                    editor.putBoolean("selfieImageDone",false)
                    editor.putBoolean("outletFrontImageDone",false)
                    editor.putBoolean("interior1ImageDone",false)
                    editor.putBoolean("interior2ImageDone",false)
                    editor.remove("outletDetails")
                    editor.apply()
                    requireActivity().finish()
                    requireActivity().startActivity(requireActivity().intent)

                }
                confirm.setCancelButton("No") { confirm.dismissWithAnimation() }
                confirm.show()

            }

        }

    }

    private fun checkFields(): Boolean {
       /* if(!outletFrontImageDone) {
            CustomUtility.showWarning(requireContext(), "Outlet's front image missing","Mandatory Image")
            return false
        }*/


        return true
    }
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
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
                    // Error occurred while creating the File
                    //...
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentPath", currentPath)
    }

    private fun getOutletDetails(outletId: String)
    {
        if (CustomUtility.haveNetworkConnection(requireContext())) {
            sweetAlertDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
            sweetAlertDialog!!.titleText = "Loading"
            sweetAlertDialog!!.show()
            sweetAlertDialog!!.setCancelable(false)
            val retrofit = Retrofit.Builder()
                .baseUrl(StaticTags.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(OutletListApiInterface::class.java)
            val call = service.getOutletDetails(User.user!!.userId!!, outletId)

            call.enqueue(object : Callback<SingleOutletDetailsResponseBody> {
                override fun onResponse(
                    call: Call<SingleOutletDetailsResponseBody>?,
                    response: retrofit2.Response<SingleOutletDetailsResponseBody>?
                ) {
                    sweetAlertDialog!!.dismiss()
                    Log.d("response", response?.body().toString())
                    if (response != null) {
                        if (response.code() == 200) {
                            val outletResponseBody = response.body()!!
                            if (outletResponseBody.success) {
                                val outletDetails = outletResponseBody.outletResult

                                val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)
                                val editor = sharedPreferences.edit()
                                editor.putString("outletDetails",Gson().toJson(outletDetails))
                                editor.apply()

                                outletUpdateViewModel.setOutlet(outletDetails)
                                getImageStatus()
                            }
                            else {
                                val s = SweetAlertDialog(requireContext(),SweetAlertDialog.ERROR_TYPE)
                                s.titleText = "Failed"
                                s.contentText = outletResponseBody.message
                                s.setConfirmButton("Ok", SweetAlertDialog.OnSweetClickListener {
                                    s.dismissWithAnimation()
                                    /* val editor = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE).edit()
                                     User.user?.clear(editor)
                                     activity!!.finish()
                                     val intent = Intent(requireContext(), LoginActivity::class.java)
                                     startActivity(intent)*/
                                })
                                s.setCancelable(false)
                                s.show()
                                Log.d("null","session expired")
                            }
                        }
                    }

                }

                override fun onFailure(call: Call<SingleOutletDetailsResponseBody>?, t: Throwable?) {
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

   private fun getImageStatus()
    {
        val sharedPreferences = requireActivity().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)
        if(sharedPreferences.getString("outletDetails",null) != null)
        {
            val outletResult = Gson().fromJson(sharedPreferences.getString("outletDetails",null), OutletResult::class.java)
            if(outletResult.outletImage != null)
            {
                Log.d("image: ",outletResult.outletImage.toString())
                for(i in outletResult.outletImage)
                {
                    when (i.image_type_id) {
                        "2" -> {
                            selfieImageDone = true
                        }
                        "4" -> {
                            outletFrontImageDone = true
                        }
                        "5" -> {
                            interior1ImageDone = true
                        }
                        "6" -> {
                            interior2ImageDone = true
                        }
                    }
                }
            }


        }
        val editor = sharedPreferences.edit()

        if(selfieImageDone)
        {
            editor.putBoolean("selfieImageDone",true)
            binding!!.selfieContactPerson.setBackgroundResource(R.drawable.ic_camera_done)
            binding!!.selfieContactUploadBtn.setBackgroundResource(R.drawable.ic_upload_done)
        }
        if(outletFrontImageDone)
        {
            editor.putBoolean("outletFrontImageDone",true)
            binding!!.outletFront.setBackgroundResource(R.drawable.ic_camera_done)
            binding!!.outletFrontUploadBtn.setBackgroundResource(R.drawable.ic_upload_done)
        }
        if(interior1ImageDone)
        {
            editor.putBoolean("interior1ImageDone",true)
            binding!!.interior1.setBackgroundResource(R.drawable.ic_camera_done)
            binding!!.interior1UploadBtn.setBackgroundResource(R.drawable.ic_upload_done)
        }
        if(interior2ImageDone)
        {
            editor.putBoolean("interior2ImageDone",true)
            binding!!.interior2.setBackgroundResource(R.drawable.ic_camera_done)
            binding!!.interior2UploadBtn.setBackgroundResource(R.drawable.ic_upload_done)
        }
        editor.apply()

    }



    //after finishing camera intent whether the picture was save or not
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == StaticTags.SELFIE_IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            //photoFlag = true
            //binding!!..setText(R.string.take_image_done)
            //val extras = data!!.extras
            //val imageBitmap = extras!!["data"] as Bitmap?
            //imageString = CustomUtility.imageToString(imageBitmap!!)
            val file = File(currentPath)
            if (file.exists())
            {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, Uri.fromFile(file))
                imageString = CustomUtility.imageToString(bitmap)
                binding!!.selfieContactPerson.setBackgroundResource(R.drawable.ic_camera_done)
                uploadPicture(outletId!! , imageString!!, StaticTags.SELFIE_IMAGE_CAPTURE_CODE, "outlet/insert_outlet_picture.php")
            }

        }
        else if (requestCode == StaticTags.OUTLET_FRONT_IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            //photoFlag = true
            //binding!!..setText(R.string.take_image_done)
            //val extras = data!!.extras
            //val imageBitmap = extras!!["data"] as Bitmap?
            //imageString = CustomUtility.imageToString(imageBitmap!!)
            val file = File(currentPath)
            if (file.exists())
            {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, Uri.fromFile(file))
                imageString = CustomUtility.imageToString(bitmap)
                binding!!.outletFront.setBackgroundResource(R.drawable.ic_camera_done)
                uploadPicture(outletId!! , imageString!!, StaticTags.OUTLET_FRONT_IMAGE_CAPTURE_CODE, "outlet/insert_outside_picture.php")
            }

        }
        else if (requestCode == StaticTags.INTERIOR1_IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            //photoFlag = true
            //binding!!..setText(R.string.take_image_done)
            //val extras = data!!.extras
            //val imageBitmap = extras!!["data"] as Bitmap?
            //imageString = CustomUtility.imageToString(imageBitmap!!)
            val file = File(currentPath)
            if (file.exists())
            {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, Uri.fromFile(file))
                imageString = CustomUtility.imageToString(bitmap)
                binding!!.interior1.setBackgroundResource(R.drawable.ic_camera_done)
                uploadPicture(outletId!! , imageString!!, StaticTags.INTERIOR1_IMAGE_CAPTURE_CODE, "outlet/insert_inside_01_picture.php")
            }

        }
        else if (requestCode == StaticTags.INTERIOR2_IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            //photoFlag = true
            //binding!!..setText(R.string.take_image_done)
            //val extras = data!!.extras
            //val imageBitmap = extras!!["data"] as Bitmap?
            //imageString = CustomUtility.imageToString(imageBitmap!!)
            val file = File(currentPath)
            if (file.exists())
            {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, Uri.fromFile(file))
                imageString = CustomUtility.imageToString(bitmap)
                binding!!.interior2.setBackgroundResource(R.drawable.ic_camera_done)
                uploadPicture(outletId!! , imageString!!, StaticTags.INTERIOR2_IMAGE_CAPTURE_CODE, "outlet/insert_inside_02_picture.php")
            }

        }

    }

    private fun uploadPicture(s: String, s1: String, code: Int, url_last: String ) {


        val sweetAlertDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        sweetAlertDialog.titleText = "Loading"
        sweetAlertDialog.setCancelable(false)
        sweetAlertDialog.show()
        val queue = Volley.newRequestQueue(requireContext())
        val url = StaticTags.BASE_URL + url_last

        val sr: StringRequest = object : StringRequest(Method.POST, url,
                Response.Listener {
                    sweetAlertDialog.dismiss()
                    Log.d("response:", it)
                    val jsonObject = JSONObject(it)
                    if(jsonObject.getBoolean("success"))
                    {
                        val ss = SweetAlertDialog(requireContext(),SweetAlertDialog.SUCCESS_TYPE)
                        ss.titleText = "Success"
                        ss.setCancelable(false)
                        ss.setConfirmButton("Ok") {
                            ss.dismiss()
                        }
                        ss.show()
                        val sharedPreferences = requireActivity().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        when (code) {
                            StaticTags.SELFIE_IMAGE_CAPTURE_CODE -> {
                                binding!!.selfieContactUploadBtn.setBackgroundResource(R.drawable.ic_upload_done)
                                editor.putBoolean("selfieImageDone", true)
                                selfieImageDone = true
                            }
                            StaticTags.OUTLET_FRONT_IMAGE_CAPTURE_CODE -> {
                                binding!!.outletFrontUploadBtn.setBackgroundResource(R.drawable.ic_upload_done)
                               editor.putBoolean("outletFrontImageDone", true)
                                outletFrontImageDone = true
                            }
                            StaticTags.INTERIOR1_IMAGE_CAPTURE_CODE -> {
                                binding!!.interior1UploadBtn.setBackgroundResource(R.drawable.ic_upload_done)
                                editor.putBoolean("interior1ImageDone", true)
                                interior1ImageDone = true
                            }
                            StaticTags.INTERIOR2_IMAGE_CAPTURE_CODE -> {
                                binding!!.interior2UploadBtn.setBackgroundResource(R.drawable.ic_upload_done)
                                editor.putBoolean("interior2ImageDone", true)
                                interior2ImageDone = true
                            }
                        }
                        editor.apply()
                    }

                    else
                    {
                        CustomUtility.showError(requireContext(), "Failed, try again", jsonObject.getString("message"))
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
                params["ImageData"] = imageString!!

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


    private fun disableAllMenuItem() {
        MainActivity.menuNav?.findItem(R.id.outletUpdateFragment)?.isEnabled = false
    }
}