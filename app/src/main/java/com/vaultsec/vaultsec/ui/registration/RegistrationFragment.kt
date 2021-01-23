package com.vaultsec.vaultsec.ui.registration

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.databinding.FragmentRegistrationBinding
import com.vaultsec.vaultsec.util.hasInternetConnection
import com.vaultsec.vaultsec.util.hideKeyboard
import com.vaultsec.vaultsec.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_start.*
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class RegistrationFragment : Fragment(R.layout.fragment_registration) {
    private var _binding: FragmentRegistrationBinding? = null
    private val binding get() = _binding!!

    private val tokenViewModel: TokenViewModel by viewModels()

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
            if (hasInternetConnection(requireActivity())) {
                tokenViewModel.onRegisterClick(
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
            tokenViewModel.tokenEvent.collect { event ->
                /*
                * The cases that belong to LoginFragment won't be handled here
                * */
                when (event) {
                    TokenViewModel.TokenEvent.ClearErrorsFirstName -> {
                        binding.textfieldRegistrationFirstnameLayout.error = null
                    }
                    TokenViewModel.TokenEvent.ClearErrorsLastName -> {
                        binding.textfieldRegistrationLastnameLayout.error = null
                    }
                    TokenViewModel.TokenEvent.ClearErrorsEmail -> {
                        binding.textfieldRegistrationEmailLayout.error = null
                    }
                    TokenViewModel.TokenEvent.ClearErrorsPassword -> {
                        binding.textfieldRegistrationPasswordLayout.error = null
                    }
                    TokenViewModel.TokenEvent.ClearErrorsPasswordRetype -> {
                        binding.textfieldRegistrationPasswordRetypeLayout.error = null
                    }
                    TokenViewModel.TokenEvent.ShowProgressBar -> {
                        requireActivity().progressbar_in_start.visibility = View.VISIBLE
                    }
                    TokenViewModel.TokenEvent.HideProgressBar -> {
                        requireActivity().progressbar_in_start.visibility = View.INVISIBLE
                    }
                    TokenViewModel.TokenEvent.SuccessfulRegistration -> {
                        setFragmentResult(
                            "com.vaultsec.vaultsec.ui.registration.RegistrationFragment",
                            bundleOf(
                                "RegistrationResult" to true
                            )
                        )
                        findNavController().popBackStack()
                    }
                    is TokenViewModel.TokenEvent.ShowFirstNameInputError -> {
                        binding.textfieldRegistrationFirstnameLayout.error =
                            getString(event.message)
                    }
                    is TokenViewModel.TokenEvent.ShowLastNameInputError -> {
                        binding.textfieldRegistrationLastnameLayout.error = getString(event.message)
                    }
                    is TokenViewModel.TokenEvent.ShowEmailInputError -> {
                        binding.textfieldRegistrationEmailLayout.error = getString(event.message)
                    }
                    is TokenViewModel.TokenEvent.ShowPasswordInputError -> {
                        binding.textfieldRegistrationPasswordLayout.error = getString(event.message)
                    }
                    is TokenViewModel.TokenEvent.ShowPasswordRetypeInputError -> {
                        binding.textfieldRegistrationPasswordRetypeLayout.error =
                            getString(event.message)
                    }
                    is TokenViewModel.TokenEvent.ShowHttpError -> {
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
                    is TokenViewModel.TokenEvent.ShowRequestError -> {
                        Snackbar.make(requireView(), event.message, Snackbar.LENGTH_LONG)
                            .setBackgroundTint(requireContext().getColor(R.color.color_error_snackbar))
                            .show()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        clearFocus()
    }

    private fun clearFocus() {
        binding.textfieldRegistrationFirstname.clearFocus()
        binding.textfieldRegistrationLastname.clearFocus()
        binding.textfieldRegistrationEmail.clearFocus()
        binding.textfieldRegistrationPassword.clearFocus()
        binding.textfieldRegistrationPasswordRetype.clearFocus()
    }
}