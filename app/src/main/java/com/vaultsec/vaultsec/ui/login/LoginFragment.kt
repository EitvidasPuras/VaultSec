package com.vaultsec.vaultsec.ui.login

import android.content.Intent
import android.os.Bundle
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
import com.vaultsec.vaultsec.ui.BottomNavigationActivity
import com.vaultsec.vaultsec.util.hasInternetConnection
import com.vaultsec.vaultsec.util.hideKeyboard
import com.vaultsec.vaultsec.viewmodel.HTTP_EMAIL_ERROR
import com.vaultsec.vaultsec.viewmodel.HTTP_PASSWORD_ERROR
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
//        Remove these comments to prevent taking screenshots
//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_SECURE,
//            WindowManager.LayoutParams.FLAG_SECURE
//        )

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
            hideKeyboard(requireActivity())
            if (hasInternetConnection(requireActivity())) {
                tokenViewModel.onLoginClick(
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
            tokenViewModel.onCreateAccountClick()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            tokenViewModel.tokenEvent.collect { event ->
                /*
                * The cases that belong to RegistrationFragment won't be handled here
                * */
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
                    is TokenViewModel.TokenEvent.ShowEmailInputError -> {
                        binding.textfieldLoginEmailLayout.error = getString(event.message)
                    }
                    is TokenViewModel.TokenEvent.ShowPasswordInputError -> {
                        binding.textfieldLoginPasswordLayout.error = getString(event.message)
                    }
                    is TokenViewModel.TokenEvent.ClearErrorsEmail -> {
                        binding.textfieldLoginEmailLayout.error = null
                    }
                    is TokenViewModel.TokenEvent.ClearErrorsPassword -> {
                        binding.textfieldLoginPasswordLayout.error = null
                    }
                    is TokenViewModel.TokenEvent.ShowProgressBar -> {
                        requireActivity().progressbar_in_start.visibility = View.VISIBLE
                    }
                    is TokenViewModel.TokenEvent.HideProgressBar -> {
                        requireActivity().progressbar_in_start.visibility = View.INVISIBLE
                    }
                    is TokenViewModel.TokenEvent.ShowHttpError -> {
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
                    is TokenViewModel.TokenEvent.ShowRequestError -> {
                        Snackbar.make(requireView(), event.message, Snackbar.LENGTH_LONG)
                            .setBackgroundTint(requireContext().getColor(R.color.color_error_snackbar))
                            .show()
                    }
                    TokenViewModel.TokenEvent.SuccessfulLogin -> {
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