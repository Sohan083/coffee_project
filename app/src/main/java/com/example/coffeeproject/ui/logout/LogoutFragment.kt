package com.example.coffeeproject.ui.logout

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.coffeeproject.R


import com.example.coffeeproject.model.User

class LogoutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_logout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var user: User? = null
        user = User.instance
        val log = SweetAlertDialog(requireContext(), SweetAlertDialog.WARNING_TYPE)
        log.titleText = "Are you sure to Sign Out?"
        log.setConfirmClickListener {
            val editor = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE).edit()
            //editor.clear();
            //editor.apply();
            user?.clear(editor)
            log.dismissWithAnimation()
            requireActivity().finish()
        }
        log.setCancelClickListener {
            log.dismissWithAnimation()
            //FragmentManager fm = getParentFragmentManager();
            //fm.popBackStack();
            requireActivity().onBackPressed()
        }
        log.cancelText = "No"
        log.confirmText = "Ok"
        log.show()
    }
}