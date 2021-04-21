package com.vaultsec.vaultsec.ui.masterpassword

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.databinding.FragmentMasterPasswordBinding
import com.vaultsec.vaultsec.ui.BottomNavigationActivity
import com.vaultsec.vaultsec.util.hideKeyboard
import com.vaultsec.vaultsec.viewmodel.MasterPasswordViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class MasterPasswordFragment : Fragment(R.layout.fragment_master_password) {
    private var _binding: FragmentMasterPasswordBinding? = null
    private val binding get() = _binding!!

    private val masterPasswordViewModel: MasterPasswordViewModel by viewModels()

    private var cancellationSignal: CancellationSignal? = null

    private val authenticationCallback: BiometricPrompt.AuthenticationCallback
        get() = @RequiresApi(Build.VERSION_CODES.P)
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)
                openBottomNavigationActivity()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMasterPasswordBinding.inflate(inflater, container, false)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireActivity().finish()
        }
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textfieldMasterPassword.setOnEditorActionListener(object :
            TextView.OnEditorActionListener {
            override fun onEditorAction(p0: TextView?, p1: Int, p2: KeyEvent?): Boolean {
                if (p1 == EditorInfo.IME_ACTION_DONE) {
                    binding.buttonUnlock.performClick()
                    return true
                }
                return false
            }
        })

        binding.buttonUnlock.setOnClickListener {
            masterPasswordViewModel.onMasterUnlockClick(
                binding.textfieldMasterPassword.text.toString()
            )
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            masterPasswordViewModel.masterPasswordEvent.collect { event ->
                when (event) {
                    is MasterPasswordViewModel.MasterPasswordEvent.ShowMasterPasswordInputError -> {
                        binding.textfieldMasterPasswordLayout.error = getString(event.message)
                    }
                    is MasterPasswordViewModel.MasterPasswordEvent.ClearErrors -> {
                        binding.textfieldMasterPasswordLayout.error = null
                    }
                    is MasterPasswordViewModel.MasterPasswordEvent.SuccessfulUnlock -> {
                        hideKeyboard(requireActivity())
                        openBottomNavigationActivity()
                    }
                    is MasterPasswordViewModel.MasterPasswordEvent.ShowProgressBar -> {
                        val progressbar =
                            requireActivity().findViewById<View>(R.id.progressbar_start)
                        progressbar.isVisible = event.doShow
                    }
                }
            }
        }

        checkBiometricSupport()

        val biometricPrompt = BiometricPrompt.Builder(requireContext())
            .setTitle(getString(R.string.biometric_fingerprint_dialog_title))
            .setSubtitle(getString(R.string.biometric_fingerprint_dialog_subtitle))
            .setNegativeButton("Cancel", requireActivity().mainExecutor, { _, _ ->
            }).build()

        biometricPrompt.authenticate(
            getCancellationSignal(),
            requireActivity().mainExecutor,
            authenticationCallback
        )
    }

    private fun getCancellationSignal(): CancellationSignal {
        cancellationSignal = CancellationSignal()
        cancellationSignal?.setOnCancelListener {
            Snackbar.make(
                requireView(),
                R.string.biometric_fingerprint_cancellation_signal,
                Snackbar.LENGTH_LONG
            ).show()
        }
        return cancellationSignal as CancellationSignal
    }

    private fun checkBiometricSupport(): Boolean {
        val keyguardManager =
            requireActivity().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (!keyguardManager.isKeyguardSecure) {
            return false
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.USE_BIOMETRIC)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return if (requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            true
        } else true
    }

    private fun openBottomNavigationActivity() {
        val bottomNavIntent = Intent(requireContext(), BottomNavigationActivity::class.java)
        startActivity(bottomNavIntent)
        requireActivity().finish()
    }
}