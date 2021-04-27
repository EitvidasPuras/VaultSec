package com.vaultsec.vaultsec.ui.registration

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.databinding.FragmentRegistrationBinding
import com.vaultsec.vaultsec.util.NetworkUtil
import com.vaultsec.vaultsec.util.OnConnectionStatusChange
import com.vaultsec.vaultsec.util.hideKeyboard
import com.vaultsec.vaultsec.util.isNetworkAvailable
import com.vaultsec.vaultsec.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class RegistrationFragment : Fragment(R.layout.fragment_registration) {
    private var _binding: FragmentRegistrationBinding? = null
    private val binding get() = _binding!!

    private val authenticationViewModel: AuthenticationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrationBinding.inflate(inflater, container, false)
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

//        populateTestingData()

        binding.textviewRegistrationLogin.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.textfieldRegistrationPasswordRetype.setOnEditorActionListener(object :
            TextView.OnEditorActionListener {
            override fun onEditorAction(p0: TextView?, p1: Int, p2: KeyEvent?): Boolean {
                if (p1 == EditorInfo.IME_ACTION_DONE) {
                    binding.buttonRegistration.performClick()
                    return true
                }
                return false
            }
        })

        binding.buttonRegistration.setOnClickListener {
            hideKeyboard(requireActivity())
            if (isNetworkAvailable) {
                authenticationViewModel.onRegisterClick(
                    binding.textfieldRegistrationFirstname.text.toString(),
                    binding.textfieldRegistrationLastname.text.toString(),
                    binding.textfieldRegistrationEmail.text.toString(),
                    binding.textfieldRegistrationPassword.text.toString(),
                    binding.textfieldRegistrationPasswordRetype.text.toString()
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.error_no_internet_connection,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            authenticationViewModel.authenticationEvent.collect { event ->
                /*
                * The cases that belong to LoginFragment won't be handled here
                * */
                when (event) {
                    AuthenticationViewModel.SessionEvent.ClearErrorsFirstName -> {
                        binding.textfieldRegistrationFirstnameLayout.error = null
                    }
                    AuthenticationViewModel.SessionEvent.ClearErrorsLastName -> {
                        binding.textfieldRegistrationLastnameLayout.error = null
                    }
                    AuthenticationViewModel.SessionEvent.ClearErrorsEmail -> {
                        binding.textfieldRegistrationEmailLayout.error = null
                    }
                    AuthenticationViewModel.SessionEvent.ClearErrorsPassword -> {
                        binding.textfieldRegistrationPasswordLayout.error = null
                    }
                    AuthenticationViewModel.SessionEvent.ClearErrorsPasswordRetype -> {
                        binding.textfieldRegistrationPasswordRetypeLayout.error = null
                    }
                    is AuthenticationViewModel.SessionEvent.ShowProgressBar -> {
                        val progressbar =
                            requireActivity().findViewById<View>(R.id.progressbar_start)
                        progressbar.isVisible = event.doShow
                    }
                    AuthenticationViewModel.SessionEvent.SuccessfulRegistration -> {
                        setFragmentResult(
                            "com.vaultsec.vaultsec.ui.registration.RegistrationFragment",
                            bundleOf(
                                "RegistrationResult" to true
                            )
                        )
                        findNavController().popBackStack()
                    }
                    is AuthenticationViewModel.SessionEvent.ShowFirstNameInputError -> {
                        binding.textfieldRegistrationFirstnameLayout.error =
                            getString(event.message)
                    }
                    is AuthenticationViewModel.SessionEvent.ShowLastNameInputError -> {
                        binding.textfieldRegistrationLastnameLayout.error = getString(event.message)
                    }
                    is AuthenticationViewModel.SessionEvent.ShowEmailInputError -> {
                        binding.textfieldRegistrationEmailLayout.error = getString(event.message)
                    }
                    is AuthenticationViewModel.SessionEvent.ShowPasswordInputError -> {
                        binding.textfieldRegistrationPasswordLayout.error = getString(event.message)
                    }
                    is AuthenticationViewModel.SessionEvent.ShowPasswordRetypeInputError -> {
                        binding.textfieldRegistrationPasswordRetypeLayout.error =
                            getString(event.message)
                    }
                    is AuthenticationViewModel.SessionEvent.ShowHttpError -> {
                        when (event.whereToDisplay) {
                            HTTP_FIRST_NAME_ERROR -> {
                                binding.textfieldRegistrationFirstnameLayout.error = event.message
                            }
                            HTTP_LAST_NAME_ERROR -> {
                                binding.textfieldRegistrationLastnameLayout.error = event.message
                            }
                            HTTP_EMAIL_ERROR -> {
                                binding.textfieldRegistrationEmailLayout.error = event.message
                            }
                            HTTP_PASSWORD_ERROR -> {
                                binding.textfieldRegistrationPasswordLayout.error = event.message
                            }
                            HTTP_PASSWORD_RE_ERROR -> {
                                binding.textfieldRegistrationPasswordRetypeLayout.error =
                                    event.message
                            }
                            else -> {
                                Snackbar.make(requireView(), event.message, Snackbar.LENGTH_LONG)
                                    .setBackgroundTint(requireContext().getColor(R.color.color_error_snackbar))
                                    .show()
                            }
                        }
                    }
                    is AuthenticationViewModel.SessionEvent.ShowRequestError -> {
                        Snackbar.make(requireView(), event.message, Snackbar.LENGTH_LONG)
                            .setBackgroundTint(requireContext().getColor(R.color.color_error_snackbar))
                            .show()
                    }
                }
            }
        }
    }

//    private fun populateTestingData() {
//        binding.textfieldRegistrationFirstname.setText("First")
//        binding.textfieldRegistrationLastname.setText("Last")
//        binding.textfieldRegistrationEmail.setText("email@gmail.com")
//        binding.textfieldRegistrationPassword.setText("123456789*aA")
//        binding.textfieldRegistrationPasswordRetype.setText("123456789*aA")
//    }

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

    private fun clearFocus() {
        binding.textfieldRegistrationFirstname.clearFocus()
        binding.textfieldRegistrationLastname.clearFocus()
        binding.textfieldRegistrationEmail.clearFocus()
        binding.textfieldRegistrationPassword.clearFocus()
        binding.textfieldRegistrationPasswordRetype.clearFocus()
    }
}