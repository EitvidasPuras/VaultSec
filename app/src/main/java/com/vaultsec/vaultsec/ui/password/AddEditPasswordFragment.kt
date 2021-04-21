package com.vaultsec.vaultsec.ui.password

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.databinding.FragmentAddEditPasswordBinding
import com.vaultsec.vaultsec.util.hideKeyboard
import com.vaultsec.vaultsec.util.setProgressBarDrawable
import com.vaultsec.vaultsec.util.showKeyboard
import com.vaultsec.vaultsec.viewmodel.AddEditPasswordViewModel
import com.vaultsec.vaultsec.viewmodel.LOGIN_CAMERA_BUTTON
import com.vaultsec.vaultsec.viewmodel.PASS_CAMERA_BUTTON
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import java.sql.Timestamp

@AndroidEntryPoint
class AddEditPasswordFragment : Fragment(R.layout.fragment_add_edit_password) {
    private var _binding: FragmentAddEditPasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var scanTextResult: String? = null

    companion object {
        private const val TAG = "com.vaultsec.vaultsec.ui.password.AddEditPasswordFragment"
    }

    private val addEditPasswordViewModel: AddEditPasswordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditPasswordBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    addEditPasswordViewModel.onOpenCamera()
                } else {
                    Log.e(
                        TAG,
                        "Permission not granted"
                    )
                }

            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setProgressBarDrawable(binding.progressbarAddEditPassword)

        val urlAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_list_item_1,
            addEditPasswordViewModel.URLS
        )
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            addEditPasswordViewModel.CATEGORIES
        )
        binding.apply {
            autocompleteUrl.setAdapter(urlAdapter)
            autocompleteCategory.setAdapter(categoryAdapter)
            textviewDateEdited.text = getString(
                R.string.add_edit_edit_time_text,
                addEditPasswordViewModel.passwordDateUpdated.toString().substringBeforeLast(":")
            )
            textfieldPasswordTitle.setText(addEditPasswordViewModel.passwordTitle)
            textfieldPasswordLogin.setText(addEditPasswordViewModel.passwordLogin)
            textfieldPasswordPassword.setText(addEditPasswordViewModel.passwordPassword)
            autocompleteUrl.setText(addEditPasswordViewModel.passwordURL)
            autocompleteCategory.setText(addEditPasswordViewModel.passwordCategory, false)
        }

        setFragmentResultListener("com.vaultsec.vaultsec.ui.CameraFragment.recognizedText") { _, bundle ->
            scanTextResult = bundle.getString("Text")
            if (scanTextResult?.isNotEmpty() == true) {
                if (addEditPasswordViewModel.whichTextScanned == LOGIN_CAMERA_BUTTON) {
                    binding.textfieldPasswordLogin.setText(scanTextResult)
                } else {
                    binding.textfieldPasswordPassword.setText(scanTextResult)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            addEditPasswordViewModel.addEditTaskEvent.collect { event ->
                when (event) {
                    is AddEditPasswordViewModel.AddEditPasswordEvent.ShowInvalidInputMessage -> {
                        hideKeyboard(requireActivity())
                        binding.textfieldPasswordTitle.clearFocus()
                        binding.textfieldPasswordLogin.clearFocus()
                        binding.textfieldPasswordPassword.clearFocus()
                        binding.autocompleteUrl.clearFocus()
                        binding.autocompleteCategory.clearFocus()
                        Snackbar.make(requireView(), event.message, Snackbar.LENGTH_LONG)
                            .show()
                    }
                    is AddEditPasswordViewModel.AddEditPasswordEvent.NavigateBackWithResult -> {
                        hideKeyboard(requireActivity())
                        setFragmentResult(
                            "com.vaultsec.vaultsec.ui.password.AddEditPasswordFragment",
                            bundleOf(
                                "AddEditResult" to event.result
                            )
                        )
                        findNavController().popBackStack()
                    }
                    is AddEditPasswordViewModel.AddEditPasswordEvent.NavigateBackWithoutResult -> {
                        hideKeyboard(requireActivity())
                        findNavController().popBackStack()
                    }
                    is AddEditPasswordViewModel.AddEditPasswordEvent.DoShowLoading -> {
                        hideKeyboard(requireActivity())
                        binding.progressbarAddEditPassword.isVisible = event.visible
                    }
                    is AddEditPasswordViewModel.AddEditPasswordEvent.NavigateToCameraFragment -> {
                        hideKeyboard(requireActivity())
                        requireActivity().supportFragmentManager.setFragmentResult(
                            "com.vaultsec.vaultsec.ui.*.AddEditFragment.openCamera",
                            bundleOf(
                                "OpenCamera" to true
                            )
                        )
                        val action =
                            AddEditPasswordFragmentDirections.actionFragmentAddEditPasswordToFragmentCamera()
                        findNavController().navigate(action)
                    }
                }
            }
        }

        binding.imageviewPasswordLoginCopy.setOnClickListener {
            hideKeyboard(requireActivity())
            if (!binding.textfieldPasswordLogin.text?.isBlank()!!) {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(
                    binding.textfieldPasswordLogin.text.toString(),
                    binding.textfieldPasswordLogin.text.toString()
                )
                clipboard.setPrimaryClip(clip)
                Snackbar.make(
                    requireView(),
                    R.string.add_edit_password_login_copied,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
        binding.imageviewPasswordPasswordCopy.setOnClickListener {
            hideKeyboard(requireActivity())
            if (!binding.textfieldPasswordPassword.text?.isBlank()!!) {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(
                    binding.textfieldPasswordPassword.text.toString(),
                    binding.textfieldPasswordPassword.text.toString()
                )
                clipboard.setPrimaryClip(clip)
                Snackbar.make(
                    requireView(),
                    R.string.add_edit_password_password_copied,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        binding.autocompleteCategory.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                hideKeyboard(requireActivity())
            }
        }

        binding.imageviewPasswordLoginCamera.setOnClickListener {
            hideKeyboard(requireActivity())
            addEditPasswordViewModel.whichTextScanned = LOGIN_CAMERA_BUTTON
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    addEditPasswordViewModel.onOpenCamera()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    MaterialAlertDialogBuilder(
                        requireContext(),
                        R.style.ThemeOverlay_App_MaterialAlertDialog
                    ).setTitle(R.string.add_edit_camera_permission_title)
                        .setMessage(R.string.add_edit_camera_permission_message)
                        .setNegativeButton("Cancel") { dialog, _ ->
                            addEditPasswordViewModel.whichTextScanned = -1
                            dialog.cancel()
                        }
                        .setPositiveButton("OK") { dialog, _ ->
                            requestPermissionLauncher.launch(
                                Manifest.permission.CAMERA
                            )
                            dialog.cancel()
                        }.show()
                }
                else -> {
                    requestPermissionLauncher.launch(
                        Manifest.permission.CAMERA
                    )
                }
            }
        }

        binding.imageviewPasswordPasswordCamera.setOnClickListener {
            hideKeyboard(requireActivity())
            addEditPasswordViewModel.whichTextScanned = PASS_CAMERA_BUTTON
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    addEditPasswordViewModel.onOpenCamera()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    MaterialAlertDialogBuilder(
                        requireContext(),
                        R.style.ThemeOverlay_App_MaterialAlertDialog
                    ).setTitle(R.string.add_edit_camera_permission_title)
                        .setMessage(R.string.add_edit_camera_permission_message)
                        .setNegativeButton("Cancel") { dialog, _ ->
                            addEditPasswordViewModel.whichTextScanned = -1
                            dialog.cancel()
                        }
                        .setPositiveButton("OK") { dialog, _ ->
                            requestPermissionLauncher.launch(
                                Manifest.permission.CAMERA
                            )
                            dialog.cancel()
                        }.show()
                }
                else -> {
                    requestPermissionLauncher.launch(
                        Manifest.permission.CAMERA
                    )
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_save_password -> {
                addEditPasswordViewModel.passwordTitle =
                    if (binding.textfieldPasswordTitle.text.toString().trim()
                            .isBlank()
                    ) null else binding.textfieldPasswordTitle.text.toString().trim()
                addEditPasswordViewModel.passwordLogin =
                    binding.textfieldPasswordLogin.text.toString()
                addEditPasswordViewModel.passwordPassword =
                    binding.textfieldPasswordPassword.text.toString()
                addEditPasswordViewModel.passwordURL = binding.autocompleteUrl.text.toString()
                addEditPasswordViewModel.passwordCategory =
                    binding.autocompleteCategory.text.toString()
                if (addEditPasswordViewModel.password == null) {
                    addEditPasswordViewModel.passwordDateCreated =
                        Timestamp(System.currentTimeMillis())
                    addEditPasswordViewModel.passwordDateUpdated =
                        addEditPasswordViewModel.passwordDateCreated
                } else {
                    addEditPasswordViewModel.passwordDateUpdated =
                        Timestamp(System.currentTimeMillis())
                }

                addEditPasswordViewModel.onSavePasswordClick()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.add_edit_password_menu, menu)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        /*
        * This is required, because for some reason when returning from camera fragment
        * all the selection options disappear
        * */
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            addEditPasswordViewModel.CATEGORIES
        )
        binding.autocompleteCategory.setAdapter(categoryAdapter)
    }

    override fun onResume() {
        super.onResume()
        if (addEditPasswordViewModel.password == null && scanTextResult == null) {
            binding.textfieldPasswordPassword.requestFocus()
            showKeyboard(requireActivity())
        }
        scanTextResult = null
    }

    override fun onStop() {
        super.onStop()
        binding.textfieldPasswordTitle.clearFocus()
        binding.textfieldPasswordLogin.clearFocus()
        binding.textfieldPasswordPassword.clearFocus()
        binding.autocompleteUrl.clearFocus()
        binding.autocompleteCategory.clearFocus()
    }
}