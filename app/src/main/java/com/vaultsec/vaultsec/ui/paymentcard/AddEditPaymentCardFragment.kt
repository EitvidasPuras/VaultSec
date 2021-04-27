package com.vaultsec.vaultsec.ui.paymentcard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
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
import com.vaultsec.vaultsec.databinding.FragmentAddEditPaymentCardBinding
import com.vaultsec.vaultsec.util.SupportedPaymentCardTypes
import com.vaultsec.vaultsec.util.hideKeyboard
import com.vaultsec.vaultsec.util.setProgressBarDrawable
import com.vaultsec.vaultsec.util.showKeyboard
import com.vaultsec.vaultsec.viewmodel.AddEditPaymentCardViewModel
import com.vaultsec.vaultsec.viewmodel.CARD_NUMBER_CAMERA_BUTTON
import com.vaultsec.vaultsec.viewmodel.CARD_PIN_CAMERA_BUTTON
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import java.sql.Timestamp

@AndroidEntryPoint
class AddEditPaymentCardFragment : Fragment(R.layout.fragment_add_edit_payment_card) {
    private var _binding: FragmentAddEditPaymentCardBinding? = null
    private val binding get() = _binding!!

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var scanTextResult: String? = null

    companion object {
        private const val TAG = "com.vaultsec.vaultsec.ui.paymentcard.AddEditPaymentCardFragment"
    }

    private val addEditPaymentCardViewModel: AddEditPaymentCardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditPaymentCardBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    addEditPaymentCardViewModel.onOpenCamera()
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
        setProgressBarDrawable(binding.progressbarAddEditCard)

        binding.apply {
            textviewDateEdited.text = getString(
                R.string.add_edit_edit_time_text,
                addEditPaymentCardViewModel.paymentCardDateUpdated.toString()
                    .substringBeforeLast(":")
            )
            textfieldCardTitle.setText(addEditPaymentCardViewModel.paymentCardTitle)
            textfieldCardNumber.setText(addEditPaymentCardViewModel.paymentCardNumber)
            textfieldCardMm.setText(addEditPaymentCardViewModel.paymentCardMM)
            textfieldCardYy.setText(addEditPaymentCardViewModel.paymentCardYY)
            textfieldCardCvv.setText(addEditPaymentCardViewModel.paymentCardCVV)
            textfieldCardPin.setText(addEditPaymentCardViewModel.paymentCardPIN)
            when (addEditPaymentCardViewModel.paymentCardType) {
                SupportedPaymentCardTypes.VISA -> {
                    imageviewCardType.isVisible = true
                    imageviewCardType.setImageResource(R.drawable.ic_visa)
                }
                SupportedPaymentCardTypes.MasterCard -> {
                    imageviewCardType.isVisible = true
                    imageviewCardType.setImageResource(R.drawable.ic_mc_symbol)
                }
                else -> {
                    if (addEditPaymentCardViewModel.card != null) {
                        imageviewCardType.isVisible = true
                    }
                    imageviewCardType.setImageResource(R.drawable.ic_credit_card_bigger)
                }
            }
        }

        setFragmentResultListener("com.vaultsec.vaultsec.ui.CameraFragment.recognizedText") { _, bundle ->
            scanTextResult = bundle.getString("Text")
            if (scanTextResult?.isNotEmpty() == true) {
                if (addEditPaymentCardViewModel.whichTextScanned == CARD_NUMBER_CAMERA_BUTTON) {
                    binding.textfieldCardNumber.setText(scanTextResult)
                } else {
                    binding.textfieldCardPin.setText(scanTextResult)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            addEditPaymentCardViewModel.addEditPaymentCardEvent.collect { event ->
                when (event) {
                    is AddEditPaymentCardViewModel.AddEditPaymentCardEvent.ShowInvalidInputMessage -> {
                        hideKeyboard(requireActivity())
                        binding.textfieldCardTitle.clearFocus()
                        binding.textfieldCardNumber.clearFocus()
                        binding.textfieldCardMm.clearFocus()
                        binding.textfieldCardYy.clearFocus()
                        binding.textfieldCardCvv.clearFocus()
                        binding.textfieldCardPin.clearFocus()
                        Snackbar.make(requireView(), event.message, Snackbar.LENGTH_LONG)
                            .show()
                    }
                    is AddEditPaymentCardViewModel.AddEditPaymentCardEvent.NavigateBackWithResult -> {
                        hideKeyboard(requireActivity())
                        setFragmentResult(
                            "com.vaultsec.vaultsec.ui.paymentcard.AddEditPaymentCardFragment",
                            bundleOf(
                                "AddEditResult" to event.result
                            )
                        )
                        findNavController().popBackStack()
                    }
                    is AddEditPaymentCardViewModel.AddEditPaymentCardEvent.NavigateBackWithoutResult -> {
                        hideKeyboard(requireActivity())
                        findNavController().popBackStack()
                    }
                    is AddEditPaymentCardViewModel.AddEditPaymentCardEvent.DoShowLoading -> {
                        hideKeyboard(requireActivity())
                        binding.progressbarAddEditCard.isVisible = event.visible
                    }
                    is AddEditPaymentCardViewModel.AddEditPaymentCardEvent.NavigateToCameraFragment -> {
                        hideKeyboard(requireActivity())
                        requireActivity().supportFragmentManager.setFragmentResult(
                            "com.vaultsec.vaultsec.ui.*.AddEditFragment.openCamera",
                            bundleOf(
                                "OpenCamera" to true
                            )
                        )
                        val action =
                            AddEditPaymentCardFragmentDirections.actionFragmentAddEditPaymentCardToFragmentCamera()
                        findNavController().navigate(action)
                    }
                }
            }
        }

        binding.imageviewCardNumberCopy.setOnClickListener {
            hideKeyboard(requireActivity())
            if (!binding.textfieldCardNumber.text?.isBlank()!!) {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(
                    binding.textfieldCardNumber.text.toString(),
                    binding.textfieldCardNumber.text.toString()
                )
                clipboard.setPrimaryClip(clip)
                Snackbar.make(
                    requireView(),
                    R.string.add_edit_card_number_copied,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        binding.imageviewExpirationCopy.setOnClickListener {
            hideKeyboard(requireActivity())
            if (!binding.textfieldCardMm.text?.isBlank()!! &&
                !binding.textfieldCardYy.text?.isBlank()!!
            ) {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(
                    "${binding.textfieldCardMm.text}/${binding.textfieldCardYy.text}",
                    "${binding.textfieldCardMm.text}/${binding.textfieldCardYy.text}"
                )
                clipboard.setPrimaryClip(clip)
                Snackbar.make(
                    requireView(),
                    R.string.add_edit_card_expiration_copied,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        binding.imageviewCardCvvCopy.setOnClickListener {
            hideKeyboard(requireActivity())
            if (!binding.textfieldCardCvv.text?.isBlank()!!) {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(
                    binding.textfieldCardCvv.text.toString(),
                    binding.textfieldCardCvv.text.toString()
                )
                clipboard.setPrimaryClip(clip)
                Snackbar.make(
                    requireView(),
                    R.string.add_edit_card_cvv_copied,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        binding.imageviewCardPinCopy.setOnClickListener {
            hideKeyboard(requireActivity())
            if (!binding.textfieldCardPin.text?.isBlank()!!) {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(
                    binding.textfieldCardPin.text.toString(),
                    binding.textfieldCardPin.text.toString()
                )
                clipboard.setPrimaryClip(clip)
                Snackbar.make(
                    requireView(),
                    R.string.add_edit_card_pin_copied,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        binding.imageviewCardNumberCamera.setOnClickListener {
            hideKeyboard(requireActivity())
            addEditPaymentCardViewModel.whichTextScanned = CARD_NUMBER_CAMERA_BUTTON
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    addEditPaymentCardViewModel.onOpenCamera()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    MaterialAlertDialogBuilder(
                        requireContext(),
                        R.style.ThemeOverlay_App_MaterialAlertDialog
                    ).setTitle(R.string.add_edit_camera_permission_title)
                        .setMessage(R.string.add_edit_camera_permission_message)
                        .setNegativeButton("Cancel") { dialog, _ ->
                            addEditPaymentCardViewModel.whichTextScanned = -1
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

        binding.imageviewCardPinCamera.setOnClickListener {
            hideKeyboard(requireActivity())
            addEditPaymentCardViewModel.whichTextScanned = CARD_PIN_CAMERA_BUTTON
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    addEditPaymentCardViewModel.onOpenCamera()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    MaterialAlertDialogBuilder(
                        requireContext(),
                        R.style.ThemeOverlay_App_MaterialAlertDialog
                    ).setTitle(R.string.add_edit_camera_permission_title)
                        .setMessage(R.string.add_edit_camera_permission_message)
                        .setNegativeButton("Cancel") { dialog, _ ->
                            addEditPaymentCardViewModel.whichTextScanned = -1
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

        binding.textfieldCardNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {
                if (p0.isNotEmpty()) {
                    binding.imageviewCardType.isVisible = true
                    when (addEditPaymentCardViewModel.determineCardType(
                        p0.toString().replace("\\s".toRegex(), "")
                    )) {
                        SupportedPaymentCardTypes.VISA -> {
                            binding.imageviewCardType.setImageResource(R.drawable.ic_visa)
                        }
                        SupportedPaymentCardTypes.MasterCard -> {
                            binding.imageviewCardType.setImageResource(R.drawable.ic_mc_symbol)
                        }
                        else -> {
                            binding.imageviewCardType.setImageResource(R.drawable.ic_credit_card_bigger)
                        }
                    }
                } else {
                    binding.imageviewCardType.isVisible = false
                }
            }

            override fun afterTextChanged(p0: Editable) {
                if (p0.isNotEmpty() && (p0.length % 5) == 0) {
                    val char = p0[p0.length - 1]
                    if (char == ' ')
                        p0.delete(p0.length - 1, p0.length)
                }
                if (p0.isNotEmpty() && (p0.length % 5) == 0) {
                    val char = p0[p0.length - 1]
                    if (Character.isDigit(char) && TextUtils.split(
                            p0.toString(),
                            ' '.toString()
                        ).size <= 3
                    )
                        p0.insert(p0.length - 1, ' '.toString())
                }
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_save_card -> {
                addEditPaymentCardViewModel.paymentCardTitle =
                    if (binding.textfieldCardTitle.text.toString().trim()
                            .isBlank()
                    ) null else binding.textfieldCardTitle.text.toString().trim()
                addEditPaymentCardViewModel.paymentCardNumber =
                    binding.textfieldCardNumber.text.toString().trim().replace("\\s".toRegex(), "")
                addEditPaymentCardViewModel.paymentCardMM =
                    binding.textfieldCardMm.text.toString()
                addEditPaymentCardViewModel.paymentCardYY =
                    binding.textfieldCardYy.text.toString()
                addEditPaymentCardViewModel.paymentCardCVV =
                    binding.textfieldCardCvv.text.toString()
                addEditPaymentCardViewModel.paymentCardPIN =
                    binding.textfieldCardPin.text.toString()
                if (addEditPaymentCardViewModel.card == null) {
                    addEditPaymentCardViewModel.paymentCardDateCreated =
                        Timestamp(System.currentTimeMillis())
                    addEditPaymentCardViewModel.paymentCardDateUpdated =
                        addEditPaymentCardViewModel.paymentCardDateCreated
                } else {
                    addEditPaymentCardViewModel.paymentCardDateUpdated =
                        Timestamp(System.currentTimeMillis())
                }
                addEditPaymentCardViewModel.onSavePaymentCardClick()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.add_edit_card_menu, menu)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (addEditPaymentCardViewModel.card == null && scanTextResult == null) {
            binding.textfieldCardNumber.requestFocus()
            showKeyboard(requireActivity())
        }
        scanTextResult = null
    }

    override fun onStop() {
        super.onStop()
        binding.textfieldCardTitle.clearFocus()
        binding.textfieldCardNumber.clearFocus()
        binding.textfieldCardMm.clearFocus()
        binding.textfieldCardYy.clearFocus()
        binding.textfieldCardCvv.clearFocus()
        binding.textfieldCardPin.clearFocus()
    }
}