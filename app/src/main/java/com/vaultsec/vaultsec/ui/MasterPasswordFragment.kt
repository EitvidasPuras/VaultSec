package com.vaultsec.vaultsec.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.fragment.findNavController
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.databinding.FragmentMasterPasswordBinding
import com.vaultsec.vaultsec.util.hideKeyboard

class MasterPasswordFragment : Fragment(R.layout.fragment_master_password) {
    private var _binding: FragmentMasterPasswordBinding? = null
    private val binding get() = _binding!!

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
            hideKeyboard(requireActivity())
            openBottomNavigationActivity()
        }
    }

    private fun openBottomNavigationActivity() {
        val bottomNavIntent = Intent(requireContext(), BottomNavigationActivity::class.java)
        startActivity(bottomNavIntent)
        requireActivity().finish()
    }
}