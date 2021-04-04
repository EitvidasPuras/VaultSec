package com.vaultsec.vaultsec.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.databinding.FragmentLoginBinding
import com.vaultsec.vaultsec.ui.BottomNavigationActivity
import com.vaultsec.vaultsec.util.NetworkUtil
import com.vaultsec.vaultsec.util.OnConnectionStatusChange
import com.vaultsec.vaultsec.util.hideKeyboard
import com.vaultsec.vaultsec.util.isNetworkAvailable
import com.vaultsec.vaultsec.viewmodel.HTTP_EMAIL_ERROR
import com.vaultsec.vaultsec.viewmodel.HTTP_PASSWORD_ERROR
import com.vaultsec.vaultsec.viewmodel.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        Remove these comments to prevent taking screenshots
//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_SECURE,
//            WindowManager.LayoutParams.FLAG_SECURE
//        )

        setFragmentResultListener(
            "com.vaultsec.vaultsec.ui.registration.RegistrationFragment"
        ) { _, bundle ->
            val result = bundle.getBoolean("RegistrationResult")
            sessionViewModel.onRegistrationResult(result)
        }

        binding.textfieldLoginPassword.setOnEditorActionListener(object :
            TextView.OnEditorActionListener {
            override fun onEditorAction(p0: TextView?, p1: Int, p2: KeyEvent?): Boolean {
                if (p1 == EditorInfo.IME_ACTION_DONE) {
                    binding.buttonLogin.performClick()
                    return true
                }
                return false
            }
        })
        binding.buttonLogin.setOnClickListener {
            hideKeyboard(requireActivity())
            if (isNetworkAvailable) {
                sessionViewModel.onLoginClick(
                    binding.textfieldLoginEmail.text.toString(),
                    binding.textfieldLoginPassword.text.toString()
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.error_no_internet_connection,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.textviewLoginCreate.setOnClickListener {
            sessionViewModel.onCreateAccountClick()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            sessionViewModel.sessionEvent.collect { event ->
                /*
                * The cases that belong to RegistrationFragment won't be handled here
                * */
                when (event) {
                    is SessionViewModel.SessionEvent.NavigateToRegistrationFragment -> {
                        val action =
                            LoginFragmentDirections.actionFragmentLoginToFragmentRegistration()
                        findNavController().navigate(action)
                    }
                    is SessionViewModel.SessionEvent.ShowSuccessfulRegistrationMessage -> {
                        Snackbar.make(requireView(), event.message, Snackbar.LENGTH_LONG)
                            .setBackgroundTint(requireContext().getColor(R.color.color_successful_snackbar))
                            .show()
                    }
                    is SessionViewModel.SessionEvent.ShowEmailInputError -> {
                        binding.textfieldLoginEmailLayout.error = getString(event.message)
                    }
                    is SessionViewModel.SessionEvent.ShowPasswordInputError -> {
                        binding.textfieldLoginPasswordLayout.error = getString(event.message)
                    }
                    is SessionViewModel.SessionEvent.ClearErrorsEmail -> {
                        binding.textfieldLoginEmailLayout.error = null
                    }
                    is SessionViewModel.SessionEvent.ClearErrorsPassword -> {
                        binding.textfieldLoginPasswordLayout.error = null
                    }
                    is SessionViewModel.SessionEvent.ShowProgressBar -> {
                        val progressbar =
                            requireActivity().findViewById<View>(R.id.progressbar_start)
                        progressbar.isVisible = event.doShow
                    }
                    is SessionViewModel.SessionEvent.ShowHttpError -> {
                        when (event.whereToDisplay) {
                            HTTP_EMAIL_ERROR -> {
                                binding.textfieldLoginEmailLayout.error = event.message
                            }
                            HTTP_PASSWORD_ERROR -> {
                                binding.textfieldLoginPasswordLayout.error = event.message
                            }
                            else -> {
                                Snackbar.make(requireView(), event.message, Snackbar.LENGTH_LONG)
                                    .setBackgroundTint(requireContext().getColor(R.color.color_error_snackbar))
                                    .show()
                            }
                        }
                    }
                    is SessionViewModel.SessionEvent.ShowRequestError -> {
                        Snackbar.make(requireView(), event.message, Snackbar.LENGTH_LONG)
                            .setBackgroundTint(requireContext().getColor(R.color.color_error_snackbar))
                            .show()
                    }
                    SessionViewModel.SessionEvent.SuccessfulLogin -> {
                        openBottomNavigationActivity()
                    }
                }
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        clearInputs()
        clearInputErrors()
    }

    override fun onStop() {
        super.onStop()
        clearFocus()
    }

    override fun onStart() {
        super.onStart()
        NetworkUtil().checkNetworkInfo(requireContext(), object : OnConnectionStatusChange {
            override fun onChange(isAvailable: Boolean) {
                if (isAvailable) {
                    Log.e("$isNetworkAvailable", "AVAILABLE")
                } else {
                    Log.e("$isNetworkAvailable", "UNAVAILABLE")
                }
            }
        })
    }

    private fun clearInputs() {
        binding.textfieldLoginEmail.setText("")
        binding.textfieldLoginPassword.setText("")
    }

    private fun clearInputErrors() {
        binding.textfieldLoginEmailLayout.error = null
        binding.textfieldLoginPasswordLayout.error = null
    }

    private fun clearFocus() {
        binding.textfieldLoginPassword.clearFocus()
        binding.textfieldLoginEmail.clearFocus()
    }

    private fun openBottomNavigationActivity() {
        val bottomNavIntent = Intent(requireContext(), BottomNavigationActivity::class.java)
        startActivity(bottomNavIntent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}