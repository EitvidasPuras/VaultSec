package com.vaultsec.vaultsec.ui.registration

import android.os.Bundle
import android.util.Patterns
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
import androidx.navigation.fragment.findNavController
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.databinding.FragmentRegistrationBinding
import com.vaultsec.vaultsec.network.entity.ApiUser
import com.vaultsec.vaultsec.network.entity.ErrorTypes
import com.vaultsec.vaultsec.util.hasInternetConnection
import com.vaultsec.vaultsec.util.hideKeyboard
import com.vaultsec.vaultsec.viewmodel.TokenViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_start.*
import java.util.regex.Pattern

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
        // Remove these comments to prevent taking screenshots
//        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
//            WindowManager.LayoutParams.FLAG_SECURE)

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
            val firstNameInput: String = binding.textfieldRegistrationFirstname.text.toString()
            val lastNameInput: String = binding.textfieldRegistrationLastname.text.toString()
            val emailInput: String = binding.textfieldRegistrationEmail.text.toString()
            val passInput: String = binding.textfieldRegistrationPassword.text.toString()
            val passRetypeInput: String =
                binding.textfieldRegistrationPasswordRetype.text.toString()
            hideKeyboard(requireActivity())
            if (hasInternetConnection(requireActivity())) {
                if (isRegistrationDataValid(
                        firstNameInput,
                        lastNameInput,
                        emailInput,
                        passInput,
                        passRetypeInput
                    )
                ) {
                    requireActivity().progressbar_in_start.visibility = View.VISIBLE
                    val user = ApiUser(
                        firstNameInput,
                        lastNameInput,
                        emailInput,
                        passInput,
                        passRetypeInput
                    )
                    tokenViewModel.postRegister(user).observe(viewLifecycleOwner) {
                        requireActivity().progressbar_in_start.visibility = View.INVISIBLE
                        if (!it.isError) {
                            hideKeyboard(requireActivity())
                            setFragmentResult(
                                "com.vaultsec.vaultsec.ui.registration.RegistrationFragment",
                                bundleOf(
                                    "RegistrationResult" to true
                                )
                            )
                            findNavController().popBackStack()
                        } else {
                            when (it.type) {
                                // TODO: 2021-01-22 Set server errors to materialtextinputs
                                ErrorTypes.HTTP_ERROR -> Toast.makeText(
                                    requireContext(),
                                    it.message,
                                    Toast.LENGTH_LONG
                                ).show()
                                ErrorTypes.SOCKET_TIMEOUT -> Toast.makeText(
                                    requireContext(),
                                    getString(R.string.error_connection_timed_out),
                                    Toast.LENGTH_LONG
                                ).show()
                                ErrorTypes.CONNECTION -> Toast.makeText(
                                    requireContext(),
                                    getString(R.string.error_connection_timed_out),
                                    Toast.LENGTH_LONG
                                ).show()
                                ErrorTypes.GENERAL -> Toast.makeText(
                                    requireContext(),
                                    getString(R.string.error_generic_connection),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.error_no_internet_connection,
                    Toast.LENGTH_SHORT
                ).show()
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

    private fun isRegistrationDataValid(
        firstNameInput: String, lastNameInput: String,
        emailInput: String, passInput: String,
        passRetypeInput: String
    ): Boolean {
        val letterLowercase = Pattern.compile("[a-z]")
        val letterUppercase = Pattern.compile("[A-Z]")
        val digit = Pattern.compile("[0-9]")
        val specialChar = Pattern.compile("[!@#$%^&*.,()_+=|<>?{}\\[\\]~`-]")

        val lastNameRegex = Pattern.compile("([A-Z][-,a-z. ']+[ ]*)+")

        if (firstNameInput.isEmpty()) {
            binding.textfieldRegistrationFirstnameLayout.error =
                getString(R.string.error_first_name_required)
        } else if (!firstNameInput.chars().allMatch(Character::isLetter)) {
            binding.textfieldRegistrationFirstnameLayout.error =
                getString(R.string.error_first_name_format)
        } else if (firstNameInput.first().isLowerCase()) {
            binding.textfieldRegistrationFirstnameLayout.error =
                getString(R.string.error_name_first_letter)
        } else if (firstNameInput.length > 30) {
            binding.textfieldRegistrationFirstnameLayout.error =
                getString(R.string.error_first_name_length)
        } else {
            binding.textfieldRegistrationFirstnameLayout.error = null
        }

        if (lastNameInput.isEmpty()) {
            binding.textfieldRegistrationLastnameLayout.error =
                getString(R.string.error_last_name_required)
        } else if (lastNameInput.length > 30) {
            binding.textfieldRegistrationLastnameLayout.error =
                getString(R.string.error_last_name_length)
        } else if (lastNameInput.first().isLowerCase()) {
            binding.textfieldRegistrationLastnameLayout.error =
                getString(R.string.error_name_first_letter)
        } else if (!lastNameInput.matches(lastNameRegex.toRegex())) {
            binding.textfieldRegistrationLastnameLayout.error =
                getString(R.string.error_last_name_format)
        } else {
            binding.textfieldRegistrationLastnameLayout.error = null
        }

        if (emailInput.isEmpty()) {
            binding.textfieldRegistrationEmailLayout.error =
                getString(R.string.error_email_required)
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            binding.textfieldRegistrationEmailLayout.error =
                getString(R.string.error_email_format)
        } else {
            binding.textfieldRegistrationEmailLayout.error = null
        }

        if (passInput.isEmpty()) {
            binding.textfieldRegistrationPasswordLayout.error =
                getString(R.string.error_password_required)
        } else if (passInput.length < 10) {
            binding.textfieldRegistrationPasswordLayout.error =
                getString(R.string.error_password_length_short)
        } else if (!letterLowercase.matcher(passInput).find()
            || !letterUppercase.matcher(passInput).find()
            || !digit.matcher(passInput).find()
            || !specialChar.matcher(passInput).find()
        ) {
            binding.textfieldRegistrationPasswordLayout.error =
                getString(R.string.error_password_format)
        } else if (passInput.length > 50) {
            binding.textfieldRegistrationPasswordLayout.error =
                getString(R.string.error_password_length_long)
        } else {
            binding.textfieldRegistrationPasswordLayout.error = null
        }

        if (passRetypeInput != passInput && passInput.isNotEmpty()) {
            binding.textfieldRegistrationPasswordRetypeLayout.error =
                getString(R.string.error_password_match)
        } else {
            binding.textfieldRegistrationPasswordRetypeLayout.error = null
        }

        return binding.textfieldRegistrationFirstnameLayout.error.isNullOrEmpty()
                && binding.textfieldRegistrationLastnameLayout.error.isNullOrEmpty()
                && binding.textfieldRegistrationEmailLayout.error.isNullOrEmpty()
                && binding.textfieldRegistrationPasswordLayout.error.isNullOrEmpty()
                && binding.textfieldRegistrationPasswordRetypeLayout.error.isNullOrEmpty()
    }
}