package com.vaultsec.vaultsec.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.databinding.FragmentLoginBinding
import com.vaultsec.vaultsec.network.entity.ApiUser
import com.vaultsec.vaultsec.network.entity.ErrorTypes
import com.vaultsec.vaultsec.ui.BottomNavigationActivity
import com.vaultsec.vaultsec.util.hasInternetConnection
import com.vaultsec.vaultsec.util.hideKeyboard
import com.vaultsec.vaultsec.viewmodel.TokenViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_start.*
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val tokenViewModel: TokenViewModel by viewModels()

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
        // Remove these comments to prevent taking screenshots
//        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
//            WindowManager.LayoutParams.FLAG_SECURE)

        setFragmentResultListener(
            "com.vaultsec.vaultsec.ui.registration.RegistrationFragment"
        ) { _, bundle ->
            val result = bundle.getBoolean("RegistrationResult")
            tokenViewModel.onRegistrationResult(result)
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
            val emailInput: String = binding.textfieldLoginEmail.text.toString()
            val passInput: String = binding.textfieldLoginPassword.text.toString()
            hideKeyboard(requireActivity())
            if (hasInternetConnection(requireActivity())) {
                if (isLoginDataValid(emailInput, passInput)) {
                    requireActivity().progressbar_in_start.visibility = View.VISIBLE
                    val user = ApiUser(
                        email = emailInput,
                        password = passInput
                    )
                    tokenViewModel.postLogin(user).observe(viewLifecycleOwner) {
                        requireActivity().progressbar_in_start.visibility = View.INVISIBLE
                        if (!it.isError) {
                            openBottomNavigationActivity()
                        } else {
                            when (it.type) {
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

        binding.textviewLoginCreate.setOnClickListener {
            tokenViewModel.onCreateAccountClick()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            tokenViewModel.tokenEvent.collect { event ->
                when (event) {
                    is TokenViewModel.TokenEvent.NavigateToRegistrationFragment -> {
                        val action =
                            LoginFragmentDirections.actionFragmentLoginToFragmentRegistration()
                        findNavController().navigate(action)
                    }
                    is TokenViewModel.TokenEvent.ShowSuccessfulRegistrationMessage -> {
                        Snackbar.make(requireView(), event.message, Snackbar.LENGTH_LONG)
                            .setBackgroundTint(requireContext().getColor(R.color.color_successful_snackbar))
                            .show()
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

    private fun isLoginDataValid(emailInput: String, passInput: String): Boolean {
        if (emailInput.isEmpty()) {
            binding.textfieldLoginEmailLayout.error = getString(R.string.error_email_required)
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            binding.textfieldLoginEmailLayout.error = getString(R.string.error_email_format)
        } else {
            binding.textfieldLoginEmailLayout.error = null
        }
        if (passInput.isEmpty()) {
            binding.textfieldLoginPasswordLayout.error = getString(R.string.error_password_required)
        } else {
            binding.textfieldLoginPasswordLayout.error = null
        }
        return binding.textfieldLoginEmailLayout.error.isNullOrEmpty() &&
                binding.textfieldLoginPasswordLayout.error.isNullOrEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}