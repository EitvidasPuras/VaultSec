package com.vaultsec.vaultsec.ui.masterpassword

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
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
    }

    private fun openBottomNavigationActivity() {
        val bottomNavIntent = Intent(requireContext(), BottomNavigationActivity::class.java)
        startActivity(bottomNavIntent)
        requireActivity().finish()
    }
}